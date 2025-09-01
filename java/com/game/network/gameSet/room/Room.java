package com.game.network.gameSet.room;

import com.game.network.util.SendMessage;
import io.netty.channel.Channel;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import com.game.network.player.PlayerSession;
import com.game.network.player.PlayerSessionManager;
import com.game.network.protocol.GameProtocol;
import com.game.network.protocol.Opcode;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Data
//创建聊天室，每个聊天室仅限两人入内
public class Room {
    private final String roomId;//每个room对应一个roomId
    private final List<String> roomMembers = new ArrayList<>(2);
    private final PlayerSessionManager playerSessionManager ;
    private String firstHandPlayerId;//判断本轮先手
    private String currentTurnPlayerId;//当前回合玩家Id
   //构造函数
    public Room(String roomId,PlayerSessionManager playerSessionManager){
        this.roomId=roomId;
        this.playerSessionManager =playerSessionManager;
    }
    //玩家加入房间
    public boolean join(String playerId){
        if (roomMembers.size()>=2){
            System.out.println("房间["+roomId+"]已满（最多两人）");
            return false;
        }
        if (roomMembers.contains(playerId)){
            System.out.println("玩家[" + playerId + "]已在房间[" + roomId + "]内");
            return false;
        }
        roomMembers.add(playerId);
        System.out.println("玩家[" + playerId + "]加入房间[" + roomId + "]，当前成员：" + roomMembers);
        return true;
    }
    //玩家退出房间
    public void exit(String playerId){
        if (roomMembers.remove(playerId)){
            System.out.println("玩家[" + playerId + "]离开房间[" + roomId + "]");
        }
        if (roomMembers.size()==1){
            String reminingPlayerId = roomMembers.get(0);
            PlayerSession playerSession = playerSessionManager.getByPlayerId(reminingPlayerId);
            if (playerSession == null)
                return;
            Channel remainingchannel = playerSession.getPlayerChannel();
            //发送对方已下线
            sendOfflineTip(remainingchannel, reminingPlayerId);
            //30秒后自动退出
            scheduleAutoExit(remainingchannel, reminingPlayerId);
        }
    }
    //安排30秒后自动退出
    private void scheduleAutoExit(Channel channel, String playerId) {
        // 启动延时任务（使用Netty的EventLoop确保线程安全）
        channel.eventLoop().schedule(() -> {
            // 发送退出通知
            SendMessage.sendMessage(channel,"时间到，自动退出房间",Opcode.AUTO_EXIT,playerId);
            // 关闭通道
            channel.close();
        }, 30, TimeUnit.SECONDS);
    }
    //发送对方已下线提示
    private void sendOfflineTip(Channel remainingchannel, String remainingPlayerId) {
        SendMessage.sendMessage(remainingchannel,"对方已下线，30秒后自动退出",Opcode.USER_OFFLINE,remainingPlayerId);
    }

    //向房间内其他玩家广播消息
    public void broadcast(String senderId, GameProtocol protocol){
        for (String roomMemberId : roomMembers) {
            if (!roomMemberId.equals(senderId)){
               playerSessionManager.sendToPlayerId(roomMemberId,protocol);
            }
        }
    }
    //判断房间是否为空
    public boolean isEmpty() {
        return roomMembers.isEmpty();
    }

}
