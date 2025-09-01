package com.game.network.gameSet.battle.rule.impl;

import com.game.network.gameSet.battle.rule.ElementReactionRule;
import com.game.network.gameSet.card.CharacterCard;
import com.game.network.player.PlayerSession;
import com.game.network.util.SendMessage;
import com.game.service.CardService;
import org.springframework.stereotype.Component;

@Component
public class MeltReaction implements ElementReactionRule {
    @Override
    public boolean canReact(String e1, String e2) {
        return ("火".equals(e1) && "冰".equals(e2)) || ("冰".equals(e1) && "火".equals(e2));
    }

    @Override
    public int calculateDamage(CharacterCard attacker, CharacterCard target,
                               PlayerSession currentPlayer, PlayerSession oppositePlayer,
                               CardService cardService) {
        int damage = 2;
        SendMessage.damageBroadcast(
                currentPlayer,
                oppositePlayer,
                "融化反应伤害",
                damage,
                target.getCurrentHp(),
                target.getName()
        );
        return damage;
    }

    @Override
    public String getReactionName() {
        return "融化";
    }
}