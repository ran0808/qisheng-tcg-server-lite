package com.game.network.gameSet.battle;

import com.game.network.gameSet.battle.rule.ElementReactionRule;
import com.game.network.gameSet.battle.rule.impl.ReactionEngine;
import com.game.network.gameSet.card.*;
import com.game.network.player.PlayerSession;
import com.game.service.CardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.*;
@Component
//元素反应服务类
public class ElementReaction {
    @Autowired
    private ReactionEngine reactionEngine;
    @Autowired
    @Lazy
    private CardService cardService;
    //判断是否能反应
    public  boolean canReact(String element1, String element2) {
        return reactionEngine.findMatchingRule(element1, element2) != null;
    }
    //元素反应的效果
    public  int calculateReactionDamage(CharacterCard attacker, PlayerSession currentPlayer, PlayerSession oppositePlayer, CharacterCard target) {

        String attackerElement = attacker.getElement();
        //检查目标是否有元素附着
        String attachedElement = target.getAttachedElement();
        ElementReactionRule rule = reactionEngine.findMatchingRule(attackerElement, attachedElement);
        System.out.println(rule);
        if (rule != null) {
            return rule.calculateDamage(attacker, target, currentPlayer, oppositePlayer, cardService);
        }
        return 0;
    }
}
