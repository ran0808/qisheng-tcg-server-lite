package com.game.network.gameSet.battle.rule.impl;

import com.game.network.gameSet.battle.rule.ElementReactionRule;
import com.game.network.gameSet.card.AddictionCard;
import com.game.network.gameSet.card.CharacterCard;
import com.game.network.player.PlayerSession;
import com.game.network.protocol.Opcode;
import com.game.network.util.SendMessage;
import com.game.service.CardService;
import org.springframework.stereotype.Component;

@Component
public class BurningReaction implements ElementReactionRule {
    @Override
    public boolean canReact(String e1, String e2) {
        return ("火".equals(e1) && "草".equals(e2)) || ("草".equals(e1) && "火".equals(e2));
    }

    @Override
    public int calculateDamage(CharacterCard attacker, CharacterCard target,
                               PlayerSession currentPlayer, PlayerSession oppositePlayer,
                               CardService cardService) {
        int damage = 1;
        // 生成燃烧烈焰创造物
        AddictionCard card = (AddictionCard) cardService.getCardByName("燃烧烈焰");
        currentPlayer.setReactionCard(card);
        if (card != null) {
            SendMessage.additionCardBroadcast(currentPlayer, oppositePlayer, card);
        }

        SendMessage.sendMessage(
                currentPlayer.getPlayerChannel(),
                "燃烧反应！" + target.getName() + "额外受到1点伤害",
                Opcode.BROADCAST_OPCODE,
                currentPlayer.getPlayerId()
        );
        SendMessage.sendMessage(
                oppositePlayer.getPlayerChannel(),
                "燃烧反应！你的" + target.getName() + "额外受到1点伤害",
                Opcode.BROADCAST_OPCODE,
                oppositePlayer.getPlayerId()
        );
        return damage;
    }

    @Override
    public String getReactionName() {
        return "燃烧";
    }
}