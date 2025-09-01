package com.game.network.gameSet.battle.rule.impl;

import com.game.network.gameSet.battle.rule.ElementReactionRule;
import com.game.network.gameSet.card.CharacterCard;
import com.game.network.player.PlayerSession;
import com.game.network.util.SendMessage;
import com.game.service.CardService;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ElectroChargedReaction implements ElementReactionRule {
    @Override
    public boolean canReact(String e1, String e2) {
        return ("雷".equals(e1) && "水".equals(e2)) || ("水".equals(e1) && "雷".equals(e2));
    }

    @Override
    public int calculateDamage(CharacterCard attacker, CharacterCard target,
                               PlayerSession currentPlayer, PlayerSession oppositePlayer,
                               CardService cardService) {
        int damage = 1;
        // 对敌方其他角色造成穿透伤害
        List<CharacterCard> enemyCharacters = oppositePlayer.getCharacterCards();
        for (CharacterCard enemyCharacter : enemyCharacters) {
            if (enemyCharacter != target && enemyCharacter.isAlive()) {
                enemyCharacter.takePiercingDamage(1);
                SendMessage.damageBroadcast(
                        currentPlayer,
                        oppositePlayer,
                        "感电穿透伤害",
                        1,
                        enemyCharacter.getCurrentHp(),
                        enemyCharacter.getName()
                );
            }
        }
        return damage;
    }

    @Override
    public String getReactionName() {
        return "感电";
    }
}