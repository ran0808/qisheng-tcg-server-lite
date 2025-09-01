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
public class BloomReaction implements ElementReactionRule {
    @Override
    public boolean canReact(String e1, String e2) {
        return ("水".equals(e1) && "草".equals(e2)) || ("草".equals(e1) && "水".equals(e2));
    }

    @Override
    public int calculateDamage(CharacterCard attacker, CharacterCard target,
                               PlayerSession currentPlayer, PlayerSession oppositePlayer,
                               CardService cardService) {
        int damage = 1;
        // 生成草原核创造物
        AddictionCard card = (AddictionCard) cardService.getCardByName("草原核");
        currentPlayer.setReactionCard(card);
        if (card != null) {
            SendMessage.additionCardBroadcast(currentPlayer, oppositePlayer, card);
        }

        SendMessage.sendMessage(
                currentPlayer.getPlayerChannel(),
                "绽放反应！" + target.getName() + "额外受到1点伤害",
                Opcode.BROADCAST_OPCODE,
                currentPlayer.getPlayerId()
        );
        SendMessage.sendMessage(
                oppositePlayer.getPlayerChannel(),
                "绽放反应！你的" + target.getName() + "额外受到1点伤害",
                Opcode.BROADCAST_OPCODE,
                oppositePlayer.getPlayerId()
        );
        return damage;
    }

    @Override
    public String getReactionName() {
        return "绽放";
    }
}