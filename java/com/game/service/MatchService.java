package com.game.service;

import com.game.dto.PlayerDO;
import com.game.dto.PlayerStatus;
import com.game.network.gameSet.room.Room;
import com.game.network.gameSet.room.RoomManager;
import com.game.network.protocol.Opcode;
import com.game.network.util.SendMessage;
import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import com.game.network.player.PlayerSessionManager;
@Slf4j
@Service
public class MatchService {
    // 缓冲队列
    private final BlockingQueue<PlayerMatchInfo> matchQueue = new LinkedBlockingQueue<>(10000);
    // 定时任务线程池
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(
            r -> new Thread(r, "match-scheduler")
    );
    // 记录正在匹配的玩家ID
    private final Set<String> matchingPlayers = ConcurrentHashMap.newKeySet();
    @Autowired
    private PlayerService playerService;
    @Autowired
    private RoomManager roomManager;
    @Autowired
    private PlayerSessionManager sessionManager;

    // 匹配信息封装
    private static class PlayerMatchInfo {
        String playerId;
        Channel channel;
        long joinTime; // 用于超时处理
        PlayerStatus status;

        PlayerMatchInfo(String playerId, Channel channel) {
            this.playerId = playerId;
            this.channel = channel;
            this.joinTime = System.currentTimeMillis(); // 记录入队时间，确保"先来先服务"
            this.status = PlayerStatus.LOGIN;
        }
    }

    @PostConstruct
    public void init() {
        scheduler.scheduleAtFixedRate(
                this::batchMatch,
                0, 100, TimeUnit.MILLISECONDS
        );
        log.info("匹配服务初始化完成，批量处理间隔：100ms（先来先服务模式）");
    }

    /**
     * 玩家加入匹配队列（先来后到顺序）
     */
    public boolean joinMatch(String playerId, Channel channel) {
        // 校验玩家状态（必须是登录状态）
        PlayerDO player = playerService.getByPlayerId(playerId);
        if (player == null || !PlayerStatus.LOGIN.equals(player.getStatus())) {
            SendMessage.sendMessage(channel, "请先登录后再匹配", Opcode.ALERT_OPCODE, playerId);
            return false;
        }
        // 入队（非阻塞，队列满时返回失败）
        boolean added = matchQueue.offer(new PlayerMatchInfo(playerId, channel));

        if (added) {
            matchingPlayers.add(playerId);
            playerService.updateStatus(playerId, PlayerStatus.MATCHING);
            SendMessage.sendMessage(channel, "已加入匹配队列，当前排队人数：" + matchQueue.size(), Opcode.ALERT_OPCODE, playerId);
            log.info("玩家[{}]加入匹配队列，当前队列长度：{}", playerId, matchQueue.size());
            return true;
        } else {
            SendMessage.sendMessage(channel, "当前匹配人数过多，请稍后再试", Opcode.ALERT_OPCODE, playerId);
            log.warn("玩家[{}]入队失败，队列已满", playerId);
            return false;
        }
    }
    /**
     * 批量匹配核心逻辑（按入队顺序两两匹配）
     */
    private void batchMatch() {
        if (matchQueue.size() < 2) {
            return;
        }

        List<PlayerMatchInfo> batch = new ArrayList<>(32);
        matchQueue.drainTo(batch, 32);

        // 过滤无效玩家
        List<PlayerMatchInfo> validPlayers = batch.stream()
                .filter(this::isValidPlayer)
                .collect(Collectors.toList());

        if (validPlayers.size() < 2) {
            if (validPlayers.size() == 1) {
                matchQueue.offer(validPlayers.get(0));
            }
            return;
        }

        // 按入队时间排序
        validPlayers.sort((p1, p2) -> Long.compare(p1.joinTime, p2.joinTime));

        // 顺序两两匹配
        matchInOrder(validPlayers);
    }

    /**
     * 校验玩家是否有效（在线、未超时、状态正常）
     */
    private boolean isValidPlayer(PlayerMatchInfo info) {
        // 超时校验（30秒未匹配则移除）
        if (System.currentTimeMillis() - info.joinTime > 30_000) {
            SendMessage.sendMessage(info.channel, "匹配超时，请重新尝试", Opcode.ALERT_OPCODE, info.playerId);
            cleanPlayer(info.playerId);
            return false;
        }

        // 状态校验
        PlayerDO player = playerService.getByPlayerId(info.playerId);
        if (player == null || !PlayerStatus.MATCHING.equals(player.getStatus())) {
            cleanPlayer(info.playerId);
            return false;
        }

        // 连接有效性校验
        if (!info.channel.isActive()) {
            cleanPlayer(info.playerId);
            return false;
        }

        return true;
    }

    /**
     * 按入队顺序两两匹配
     */
    private void matchInOrder(List<PlayerMatchInfo> validPlayers) {
        // 循环两两配对（i和i+1为一组）
        for (int i = 0; i < validPlayers.size() - 1; i += 2) {
            PlayerMatchInfo p1 = validPlayers.get(i);
            PlayerMatchInfo p2 = validPlayers.get(i + 1);

            // 避免自己匹配自己（理论上不会发生，做双重保障）
            if (p1.playerId.equals(p2.playerId)) {
                // 将当前玩家放回队列，继续下一组
                matchQueue.offer(p1);
                continue;
            }

            // 创建房间并通知双方
            createRoomAndNotify(p1, p2);
        }

        // 奇数个玩家时，最后1个放回队列（保留入队顺序）
        if (validPlayers.size() % 2 != 0) {
            PlayerMatchInfo remaining = validPlayers.get(validPlayers.size() - 1);
            matchQueue.offer(remaining);
        }
    }

    /**
     * 创建房间并通知玩家
     */
    private void createRoomAndNotify(PlayerMatchInfo p1, PlayerMatchInfo p2) {
        // 清理匹配状态
        cleanPlayer(p1.playerId);
        cleanPlayer(p2.playerId);

        // 创建房间
        String roomId = "Room-" + UUID.randomUUID().toString().substring(0, 8);
        roomManager.createRoom(roomId);
        roomManager.joinRoom(roomId, p1.playerId);
        roomManager.joinRoom(roomId, p2.playerId);

        // 更新玩家房间的信息
        playerService.updateRoomId(p1.playerId, roomId);
        playerService.updateRoomId(p2.playerId, roomId);
        playerService.updateStatus(p1.playerId, PlayerStatus.IN_ROOM);
        playerService.updateStatus(p2.playerId, PlayerStatus.IN_ROOM);

        // 通知匹配成功
        PlayerDO player1 = playerService.getByPlayerId(p1.playerId);
        PlayerDO player2 = playerService.getByPlayerId(p2.playerId);
        SendMessage.sendMessage(
                p1.channel,
                "匹配成功！已进入房间[" + roomId + "],对方是" + player2.getPlayerName(),
                Opcode.ALERT_OPCODE,
                p1.playerId
        );
        SendMessage.sendMessage(
                p2.channel,
                "匹配成功！已进入房间[" + roomId + "],对方是" + player1.getPlayerName(),
                Opcode.ALERT_OPCODE,
                p2.playerId
        );
       // 发送准备提示
        SendMessage.sendMessage(p1.channel, "准备开始：确定？取消", Opcode.MATCH_SUCCESS_OPCODE, p1.playerId);
        SendMessage.sendMessage(p2.channel, "准备开始：确定？取消", Opcode.MATCH_SUCCESS_OPCODE, p2.playerId);

        log.info("房间[{}]创建成功，成员：{}({})、{}({})（按入队顺序匹配）",
                roomId,
                p1.playerId, player1.getPlayerName(),
                p2.playerId, player2.getPlayerName()
        );
    }

    /**
     * 清理玩家匹配状态
     */
    private void cleanPlayer(String playerId) {
        matchingPlayers.remove(playerId);
    }

    /**
     * 服务销毁时关闭线程池
     */
    @PreDestroy
    public void destroy() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(1, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
        }
        log.info("匹配服务已关闭");
    }
    public boolean isMatching(String playerId) {
        return matchingPlayers.contains(playerId);
    }
    public void cancelMatch(String playerId) {
        matchingPlayers.remove(playerId);
        matchQueue.removeIf(info -> info.playerId.equals(playerId));
        log.info("玩家[{}]因重复登录，已取消匹配", playerId);
    }
}