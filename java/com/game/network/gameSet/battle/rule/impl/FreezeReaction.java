package com.game.network.gameSet.battle.rule.impl;

import com.game.network.gameSet.battle.rule.ElementReactionRule;
import com.game.network.gameSet.card.CharacterCard;
import com.game.network.gameSet.status.Status;
import com.game.network.player.PlayerSession;
import com.game.network.protocol.Opcode;
import com.game.network.util.SendMessage;
import com.game.service.CardService;
import org.springframework.stereotype.Component;

@Component
public class FreezeReaction implements ElementReactionRule {
    @Override
    public boolean canReact(String e1, String e2) {
        return ("水".equals(e1) && "冰".equals(e2)) || ("冰".equals(e1) && "水".equals(e2));
    }

    @Override
    public int calculateDamage(CharacterCard attacker, CharacterCard target,
                               PlayerSession currentPlayer, PlayerSession oppositePlayer,
                               CardService cardService) {
        int damage = 1;
        // 添加冻结状态（持续1回合）
        Status freezeStatus = new Status(Status.Type.FREEZE, 0, 1, null);
        target.getStatuses().add(freezeStatus);

        SendMessage.sendMessage(
                currentPlayer.getPlayerChannel(),
                "冻结反应触发！目标" + target.getName() + "被冻结，无法使用技能！",
                Opcode.BROADCAST_OPCODE,
                currentPlayer.getPlayerId()
        );
        SendMessage.sendMessage(
                oppositePlayer.getPlayerChannel(),
                "你的" + target.getName() + "被冻结，本回合无法使用技能！",
                Opcode.BROADCAST_OPCODE,
                oppositePlayer.getPlayerId()
        );
        return damage;
    }

    @Override
    public String getReactionName() {
        return "冻结";
    }
}