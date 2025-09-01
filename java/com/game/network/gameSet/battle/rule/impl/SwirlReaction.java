package com.game.network.gameSet.battle.rule.impl;

import com.game.network.gameSet.battle.rule.ElementReactionRule;
import com.game.network.gameSet.card.CharacterCard;
import com.game.network.gameSet.status.Status;
import com.game.network.player.PlayerSession;
import com.game.network.protocol.Opcode;
import com.game.network.util.SendMessage;
import com.game.service.CardService;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
public class SwirlReaction implements ElementReactionRule {
    private static final List<String> REACTABLE_ELEMENTS = Arrays.asList("火", "冰", "水", "雷");
    @Override
    public boolean canReact(String e1, String e2) {
        return ("风".equals(e1) && REACTABLE_ELEMENTS.contains(e2))
                || ("风".equals(e2) && REACTABLE_ELEMENTS.contains(e1));
    }

    @Override
    public int calculateDamage(CharacterCard attacker, CharacterCard target, PlayerSession currentPlayer, PlayerSession oppositePlayer, CardService cardService) {
        String spreadElement = "风".equals(attacker.getElement()) ? target.getAttachedElement() : attacker.getElement();
        for (CharacterCard enemyChar : oppositePlayer.getCharacterCards()) {
            if (enemyChar == target || !enemyChar.isAlive()) continue;

            enemyChar.takeDamage(1, attacker, null, currentPlayer, oppositePlayer);
            Status spreadStatus = new Status(Status.Type.ELEMENT_ATTACH, spreadElement);
            enemyChar.applyElementStatus(enemyChar, spreadStatus);

            SendMessage.sendMessage(
                    currentPlayer.getPlayerChannel(),
                    "扩散反应！" + enemyChar.getName() + "受到1点" + spreadElement + "伤害并被附着" + spreadElement,
                    Opcode.BROADCAST_OPCODE,
                    currentPlayer.getPlayerId());
            SendMessage.sendMessage(
                    oppositePlayer.getPlayerChannel(),
                    "扩散反应！你的" + enemyChar.getName() + "受到1点" + spreadElement + "伤害并被附着" + spreadElement,
                    Opcode.BROADCAST_OPCODE,
                    oppositePlayer.getPlayerId()
            );

        }
        return 0;
    }

    @Override
    public String getReactionName() {
        return "扩散";
    }
}
