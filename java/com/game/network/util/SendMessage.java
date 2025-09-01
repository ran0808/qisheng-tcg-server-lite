package com.game.network.util;

import com.game.network.gameSet.card.AddictionCard;
import com.game.network.gameSet.status.BattleStatus;
import com.game.network.gameSet.card.CharacterCard;
import com.game.network.gameSet.status.Status;
import io.netty.channel.Channel;
import com.game.network.player.PlayerSession;
import com.game.network.protocol.GameProtocol;
import com.game.network.protocol.Opcode;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class SendMessage {
    public static void sendMessage(Channel channel, String content, short opcode, String playerId) {
        try {
            // 关键消息之间添加延迟（如500ms），根据消息类型调整
            if (opcode == Opcode.LOGIN_RESPONSE_OPCODE || opcode == Opcode.MATCH_SUCCESS_OPCODE) {
                Thread.sleep(1000); // 登录成功、匹配成功前延迟500ms
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        GameProtocol protocol = new GameProtocol();
        protocol.setOpcode(opcode);
        protocol.setPlayerId(Short.parseShort(playerId));
        protocol.setBody(content.getBytes(StandardCharsets.UTF_8));
        channel.writeAndFlush(protocol);
    }

    public static GameProtocol makeMessage(String content, short opcode, String playerId) {
        GameProtocol protocol = new GameProtocol();
        protocol.setOpcode(opcode);
        protocol.setPlayerId(Short.parseShort(playerId));
        protocol.setBody(content.getBytes(StandardCharsets.UTF_8));
        return protocol;
    }
    public static String broadcastCard(PlayerSession player){
        CharacterCard activeCharacter = player.getActiveCharacter();
        String status;
        String battleStatus;
        if (activeCharacter.getStatuses().isEmpty()) {
            status="状态正常";
        }
        else {
            List<String> statusDescriptions = new ArrayList<>();
            for (Status s : activeCharacter.getStatuses()) {
                statusDescriptions.add(s.getDescription());
            }
            status = String.join("，", statusDescriptions);
        }
        if (activeCharacter.getBattleStatues().isEmpty()) {
            battleStatus="出战状态正常";
        }
        else {
            List<String> battleStatusDescriptions = new ArrayList<>();
            for (BattleStatus s : activeCharacter.getBattleStatues()) {
                battleStatusDescriptions.add(s.getDescription());
            }
            battleStatus = String.join("，", battleStatusDescriptions);
        }
        return String.format(
                "玩家%-6s 骰子数量：%02d  当前出战角色：%-6s  血量：%02d  充能：%d/%d  状态：%s%n  出战状态：%s%n",
                player.getPlayerId(),
                player.getDices().size(),
                activeCharacter.getName(),
                activeCharacter.getCurrentHp(),
                activeCharacter.getEnergy(),
                activeCharacter.getEnergyNeeded(),
                status,
                battleStatus
        );
    }
    public static void damageBroadcast(PlayerSession currentPlayer,PlayerSession oppositePlayer,String skillName,int damage,int currentHp,String cardName){
        SendMessage.sendMessage(currentPlayer.getPlayerChannel(),"玩家"+oppositePlayer.getPlayerId()+"的"+skillName+"造成敌方"+damage+"点伤害", Opcode.BROADCAST_OPCODE,currentPlayer.getPlayerId());
        SendMessage.sendMessage(oppositePlayer.getPlayerChannel(),"玩家"+oppositePlayer.getPlayerId()+"的"+cardName+"受到"+damage+"点伤害"+"当前血量为"+currentHp, Opcode.BROADCAST_OPCODE,oppositePlayer.getPlayerId());
    }
    public static void additionCardBroadcast(PlayerSession currentPlayer, PlayerSession oppositePlayer, AddictionCard card){
        SendMessage.sendMessage(currentPlayer.getPlayerChannel(),"玩家"+currentPlayer.getPlayerId()+"创造附加卡"+card.getName()+"使用轮数"+card.getTurn(), Opcode.BROADCAST_OPCODE,currentPlayer.getPlayerId());
        SendMessage.sendMessage(oppositePlayer.getPlayerChannel(),"玩家"+currentPlayer.getPlayerId()+"创造附加卡"+card.getName(), Opcode.BROADCAST_OPCODE,oppositePlayer.getPlayerId());
    }
}
