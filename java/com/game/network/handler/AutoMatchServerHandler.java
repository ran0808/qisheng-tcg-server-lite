package com.game.network.handler;

import com.game.dto.PlayerDO;
import com.game.dto.PlayerStatus;
import com.game.network.gameSet.card.Card;
import com.game.network.gameSet.card.CharacterCard;
import com.game.network.player.PlayerSession;
import com.game.network.util.SendMessage;
import com.game.service.BattleService;
import com.game.service.CardService;
import com.game.service.MatchService;
import com.game.service.PlayerService;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import com.game.network.gameSet.room.Room;
import com.game.network.gameSet.room.RoomManager;

import com.game.network.player.PlayerSessionManager;
import com.game.network.protocol.GameProtocol;
import com.game.network.protocol.Opcode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static com.game.network.util.SendMessage.sendMessage;
@ChannelHandler.Sharable
@Component
public class AutoMatchServerHandler extends ChannelInboundHandlerAdapter {
    @Autowired
    PlayerService playerService;
    @Autowired
    BattleService battleService;
    @Autowired
    CardService cardService;
    @Autowired
    private MatchService matchService;
    private final RoomManager roomManager;
    @Autowired
    public AutoMatchServerHandler(RoomManager roomManager) {
        this.roomManager = roomManager;
    }
    @Autowired
    private PlayerSessionManager playerSessionManager;
    @Autowired
    @Qualifier("matchExecutor")
    private ExecutorService matchExecutor;
    public void onLoginSuccess(String playerId,Channel channel) {
        PlayerSession playerSession = playerSessionManager.getByPlayerId(playerId);
        if (playerSession!=null){
            Channel existingChannel = playerSession.getPlayerChannel();
            if (existingChannel != null && existingChannel.isActive()) {
                SendMessage.sendMessage(
                        existingChannel,
                        "您的账号在其他设备登录，已被迫下线",
                        Opcode.AUTO_EXIT,
                        playerId
                );
                cleanOldSessionState(playerId);
            }
            playerSessionManager.removeSession(playerId);
        }
        playerSessionManager.addSession(playerId, channel);
        playerService.updateStatus(playerId,PlayerStatus.LOGIN);
        boolean success = matchService.joinMatch(playerId,channel);
        if (success){
            sendMessage(channel, "登录成功，正在匹配对手...", Opcode.LOGIN_RESPONSE_OPCODE, playerId);
        }
        else {
            sendMessage(channel, "登录成功，但匹配有误，请稍后再试", Opcode.LOGIN_RESPONSE_OPCODE, playerId);
        }
    }



    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        //只处理登录协议（通过操作码判断）
        if (msg instanceof GameProtocol protocol) {
            if (protocol.getOpcode() == Opcode.GAME_START_OPCODE) {
                gameStart(ctx, protocol);
            }
            else if (protocol.getOpcode() == Opcode.HEARTBEAT_RESPONSE) {
                sendMessage(ctx.channel(),"pong",Opcode.HEARTBEAT_RESPONSE, String.valueOf(protocol.getPlayerId()));
            }
            else {
                ctx.fireChannelRead(msg);
            }
        } else {
            ctx.fireChannelRead(msg);
        }
    }
    protected void gameStart(ChannelHandlerContext ch, GameProtocol msg) {
        matchExecutor.execute(()->{
            String playerId = String.valueOf(msg.getPlayerId());
            //设置当前玩家准备完毕
            playerService.updateStatus(playerId, PlayerStatus.PREPARE);
            Room currentRoom = roomManager.findRoomByPlayer(playerId);
            List<String> roomMembers = currentRoom.getRoomMembers();
            String opponentId = playerId.equals(roomMembers.get(0)) ? roomMembers.get(1) : roomMembers.get(0);
            AtomicReference<ScheduledFuture<?>>taskRef = new AtomicReference<>();
            taskRef.set(ch.executor().scheduleAtFixedRate(()->{
                ScheduledFuture<?> checkTask = taskRef.get();
                if (checkTask != null && !checkTask.isCancelled()&&playerService.isStatus(playerId,PlayerStatus.PREPARE) && playerService.isStatus(opponentId,PlayerStatus.PREPARE)){
                    //向用户发送角色卡牌，让其选取三张
                    Map<Integer, Card> cardLibrary = cardService.getCardLibrary();
                    sendMessage(ch.channel(), "双方已准备完毕，请您从以下角色卡牌中选取三张，作为出战卡牌（用逗号分隔，例如：卡牌1,卡牌2,卡牌3）：", Opcode.SEND_CARD_OPCODE, playerId);
                    List<String> characterCards = new ArrayList<>();
                    for (Card card : cardLibrary.values())
                        //判断类型是否是角色卡牌,如果是，就将卡牌的信息发送给玩家
                        if (card instanceof CharacterCard) {
                            characterCards.add(card.getName());

                        }
                    String content = characterCards.toString();
                    //创建对局
                    synchronized (this) {
                        if (battleService.getBattle(playerId,opponentId,currentRoom.getRoomId())==null){
                            battleService.createBattle(playerId,opponentId,currentRoom.getRoomId());}
                    }
                    sendMessage(ch.channel(), content, Opcode.SEND_CARD_OPCODE, playerId);
                    checkTask.cancel(false);
                }
                else if (checkTask != null && !checkTask.isCancelled()) {
                    String content = "对方玩家尚未准备完毕，请等待";
                    sendMessage(ch.channel(), content, Opcode.ALERT_OPCODE, playerId);
                }
            },0,5,TimeUnit.SECONDS));
        });
    }
    @Override
    public void channelInactive(ChannelHandlerContext ctx){
        System.out.println("有玩家退出");
        try {
            PlayerSession playerSession = playerSessionManager.getByChannel(ctx.channel());
            if (playerSession!=null){
                String exitPlayerId = playerSession.getPlayerId();
                playerService.exit(exitPlayerId);
                playerSessionManager.removeSession(exitPlayerId);
                // 从房间移除逻辑保留
                Room currentRoom = roomManager.findRoomByPlayer(exitPlayerId);
                if (currentRoom != null) {
                    battleService.ExceptEnd(currentRoom.getRoomId());
                    roomManager.leaveRoom(currentRoom, exitPlayerId);
                    System.out.println("玩家[" + exitPlayerId + "]已下线，通知对方...");
                }
            }
        } catch (Exception e) {
            System.err.println("处理连接断开时发生异常: " + e.getMessage());
        } finally {
            ctx.fireChannelInactive();
        }
    }
    private void cleanOldSessionState(String playerId) {
        if (matchService.isMatching(playerId)) {
            matchService.cancelMatch(playerId);
        }
        Room room = roomManager.findRoomByPlayer(playerId);
        if (room != null) {
            roomManager.leaveRoom(room, playerId);
            battleService.ExceptEnd(room.getRoomId());
        }
        playerService.exit(playerId);
    }
}
