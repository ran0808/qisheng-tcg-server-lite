package com.game.network.gameSet.battle.rule.impl;

import com.game.network.gameSet.battle.rule.ElementReactionRule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ReactionEngine {
    private final List<ElementReactionRule> reactionRules;

    @Autowired
    public ReactionEngine(List<ElementReactionRule> reactionRules) {
        this.reactionRules = reactionRules;
    }

    // 查找匹配的元素反应规则
    public ElementReactionRule findMatchingRule(String element1, String element2) {
        if (element1 == null || element2 == null) return null;
        for (ElementReactionRule rule : reactionRules) {
            if (rule.canReact(element1, element2)) {
                return rule;
            }
        }
        return null;
    }
}