package com.game.network.gameSet.battle.rule.impl;

import com.game.network.gameSet.battle.rule.ElementReactionRule;
import com.game.network.gameSet.card.CharacterCard;
import com.game.network.player.PlayerSession;
import com.game.network.protocol.Opcode;
import com.game.network.util.SendMessage;
import com.game.service.CardService;
import org.springframework.stereotype.Component;

@Component
public class OverloadReaction implements ElementReactionRule {
    @Override
    public boolean canReact(String e1, String e2) {
        return ("火".equals(e1) && "雷".equals(e2)) || ("雷".equals(e1) && "火".equals(e2));
    }

    @Override
    public int calculateDamage(CharacterCard attacker, CharacterCard target,
                               PlayerSession currentPlayer, PlayerSession oppositePlayer,
                               CardService cardService) {
        int damage = 2;
        // 检查目标是否为当前出战角色
        if (oppositePlayer.getActiveCharacter() == target) {
            // 获取上一个使用的角色并切换
            CharacterCard lastCharacter = oppositePlayer.getLastActiveCharacter();
            if (lastCharacter != null && lastCharacter != target) {
                oppositePlayer.setActiveCharacter(lastCharacter);
                SendMessage.sendMessage(
                        currentPlayer.getPlayerChannel(),
                        "超载反应触发！敌方" + target.getName() + "被强制切换为" + lastCharacter.getName(),
                        Opcode.BROADCAST_OPCODE,
                        currentPlayer.getPlayerId()
                );
                SendMessage.sendMessage(
                        oppositePlayer.getPlayerChannel(),
                        "超载反应触发！你的" + target.getName() + "被强制切换为" + lastCharacter.getName(),
                        Opcode.BROADCAST_OPCODE,
                        oppositePlayer.getPlayerId()
                );
            }
        }
        return damage;
    }

    @Override
    public String getReactionName() {
        return "超载";
    }
}