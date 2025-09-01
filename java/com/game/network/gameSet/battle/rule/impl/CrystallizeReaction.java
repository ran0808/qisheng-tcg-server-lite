package com.game.network.gameSet.battle.rule.impl;

import com.game.network.gameSet.status.BattleStatus;
import com.game.network.gameSet.card.CharacterCard;
import com.game.network.gameSet.battle.rule.ElementReactionRule;
import com.game.network.player.PlayerSession;
import com.game.network.protocol.Opcode;
import com.game.network.util.SendMessage;
import com.game.service.CardService;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

@Component
public class CrystallizeReaction implements ElementReactionRule {
    private static final List<String> REACTABLE_ELEMENTS = Arrays.asList("火", "冰", "水", "雷");

    @Override
    public boolean canReact(String e1, String e2) {
        // 岩元素与其他基础元素反应
        return ("岩".equals(e1) && REACTABLE_ELEMENTS.contains(e2))
                || ("岩".equals(e2) && REACTABLE_ELEMENTS.contains(e1));
    }

    @Override
    public int calculateDamage(CharacterCard attacker, CharacterCard target,
                               PlayerSession currentPlayer, PlayerSession oppositePlayer,
                               CardService cardService) {
        if (!attacker.isAlive()) return 0;

        // 计算新护盾值（最多2层）
        int currentShield = 0;
        Iterator<BattleStatus> shieldIterator = attacker.getBattleStatues().iterator();
        while (shieldIterator.hasNext()) {
            BattleStatus status = shieldIterator.next();
            if (status.getType() == BattleStatus.Type.SHIELD_CRYSTALLIZE) {
                currentShield += status.getValue();
                shieldIterator.remove(); // 移除旧护盾
            }
        }
        int newShield = Math.min(currentShield + 1, 2);

        // 添加新护盾
        if (newShield > 0) {
            attacker.getBattleStatues().add(new BattleStatus(
                    BattleStatus.Type.SHIELD_CRYSTALLIZE, newShield, -1
            ));
            SendMessage.sendMessage(
                    currentPlayer.getPlayerChannel(),
                    "结晶反应！我方" + attacker.getName() + "获得1点护盾，当前护盾：" + newShield,
                    Opcode.BROADCAST_OPCODE,
                    currentPlayer.getPlayerId()
            );
            SendMessage.sendMessage(
                    oppositePlayer.getPlayerChannel(),
                    "对方" + attacker.getName() + "触发结晶反应，获得1点护盾，当前护盾：" + newShield,
                    Opcode.BROADCAST_OPCODE,
                    oppositePlayer.getPlayerId()
            );
        }
        return 1;
    }

    @Override
    public String getReactionName() {
        return "结晶";
    }
}