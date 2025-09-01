package com.game.network.player;

import io.netty.channel.Channel;
import lombok.Getter;
import com.game.network.protocol.GameProtocol;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

//会话管理器（单例模式）
//负责管理所有在线玩家的会话信息
@Service
public class PlayerSessionManager {
    private static final PlayerSessionManager INSTANCE = new PlayerSessionManager();
    //存储在线玩家
    @Getter
    private final Map<String,PlayerSession> onlineSession = new ConcurrentHashMap<>();
    //存储回合结束玩家
    @Getter
    private List<String> turnOver = new ArrayList<>();
    //私有化构造器，防止外部实例化
    private PlayerSessionManager(){}
    //获取单例
    public static PlayerSessionManager getInstance(){
        return INSTANCE;
    }
     //添加会话
    public void addSession(String playerId, Channel socketChannel){
        //防止重复记录会话
        removeSession(playerId);
        PlayerSession playerSession = new PlayerSession(playerId, socketChannel);
        onlineSession.put(playerId,playerSession);
    }
    //移除会话
    public void removeSession(String playerId){
        onlineSession.remove(playerId);
    }
    //根据玩家Id获取会话
    public PlayerSession getByPlayerId(String playerId){
        return onlineSession.get(playerId);
    }
    //根据通道获取会话
    public PlayerSession getByChannel(Channel socketChannel){
        for(PlayerSession session : onlineSession.values()){
            if (session.getPlayerChannel()==socketChannel){
                return session;
            }
        }
        return null;
    }
    //获取在线玩家数量
    public int getOnlineCount(){
        return onlineSession.size();
    }
    //对活跃玩家进行广播
    public void sendToPlayerId(String roomMemberId, GameProtocol protocol) {
        Channel channel = getByPlayerId(roomMemberId).getPlayerChannel();
        if (channel.isActive()){
            channel.writeAndFlush(protocol);
        }
    }

    public Collection<Channel> getAllChannels() {
        Collection<Channel> channels = new ArrayList<>();
        for (PlayerSession playerSession : onlineSession.values()) {
           channels.add( playerSession.getPlayerChannel());
        }
        return channels;
    }
}