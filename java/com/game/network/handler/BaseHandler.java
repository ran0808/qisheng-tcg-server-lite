package com.game.network.handler;

import com.game.network.gameSet.room.Room;
import com.game.network.gameSet.room.RoomManager;
import io.netty.channel.Channel;
import com.game.network.player.PlayerSession;
import com.game.network.player.PlayerSessionManager;
import com.game.network.protocol.GameProtocol;
import com.game.network.protocol.Opcode;
import com.game.network.util.SendMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

import static com.game.network.util.SendMessage.sendMessage;
@Component
public abstract class BaseHandler {

    protected final RoomManager roomManager;
    @Autowired
    protected PlayerSessionManager playerSessionManager;
    @Autowired
    public BaseHandler(RoomManager roomManager) {
        this.roomManager = roomManager;
    }

    protected PlayerSession getCurrentPlayer(String playerId) {
        return playerSessionManager.getByPlayerId(playerId);
    }

    protected PlayerSession getOppositePlayer(PlayerSession currentPlayer) {
        Room currentRoom = roomManager.findRoomByPlayer(currentPlayer.getPlayerId());
        List<String> roomMembers = currentRoom.getRoomMembers();
        String opponentId = currentPlayer.getPlayerId().equals(roomMembers.get(0)) ? roomMembers.get(1) : roomMembers.get(0);
        return playerSessionManager.getByPlayerId(opponentId);
    }

    protected void broadcastBothSelections(String content, short opcode, PlayerSession currentPlayer, PlayerSession oppositePlayer) {
        sendMessage(currentPlayer.getPlayerChannel(), content, opcode, currentPlayer.getPlayerId());
        GameProtocol protocol = SendMessage.makeMessage(content, opcode, oppositePlayer.getPlayerId());
        Room currentRoom = roomManager.findRoomByPlayer(currentPlayer.getPlayerId());
        currentRoom.broadcast(currentPlayer.getPlayerId(), protocol);
    }

    protected void sendDiceSituation(PlayerSession player, Channel channel) {
        String diceStr = player.getDices().isEmpty() ? "空" : String.join(",", player.getDices());
        sendMessage(channel, "我方骰子状况：" + diceStr, Opcode.BROADCAST_OPCODE, player.getPlayerId());
    }
}
