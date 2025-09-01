package com.game.network.gameSet.battle.rule;

import com.game.network.gameSet.card.CharacterCard;
import com.game.network.player.PlayerSession;
import com.game.service.CardService;

public interface ElementReactionRule {
    boolean canReact(String element1,String element2);
    int calculateDamage(CharacterCard attacker, CharacterCard target,
                        PlayerSession currentPlayer, PlayerSession oppositePlayer,
                        CardService cardService);
    String getReactionName();
}
