package com.game.network.gameSet.room;
import com.game.network.player.PlayerSessionManager;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
@Component
public class RoomManager {
    private final Map<String, Room> rooms = new ConcurrentHashMap<>();
    private final PlayerSessionManager sessionManager; // 玩家会话管理器
    public RoomManager(PlayerSessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }
    //创建房间，保证房间id唯一
    public Room createRoom(String roomId){
        return rooms.computeIfAbsent(roomId, k -> new Room(roomId,sessionManager));
    }
    public boolean joinRoom(String roomId, String playerId) {
        Room room = rooms.get(roomId);
        if (room == null) return false;
        synchronized (room) {
            return room.join(playerId);
        }
    }
    //玩家离开房间
    public void leaveRoom(Room room, String playerId) {
        synchronized (room){
            room.exit(playerId);
            //如果房间为空，删除房间释放资源
            if (room.isEmpty()) {
                rooms.remove(room.getRoomId());
                System.out.println("房间[" + room.getRoomId() + "]已空，自动删除");
            }
        }
    }
    public Room findRoomByPlayer(String playerId){
        for (Room room :rooms.values()){
            if (room.getRoomMembers().contains(playerId)){
                return room;
            }
        }
        return null;
    }
}
