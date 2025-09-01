package com.game.network.gameSet.skill;

import com.game.network.gameSet.battle.ElementReaction;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class SkillFactory {
    @Autowired
    private List<SkillStrategy> allStrategies;
    @Autowired
    private ElementReaction elementReaction;
    private Map<String, Map<Skill.SkillType, SkillStrategy>> skillMap;
    @PostConstruct
    public void init() {
        skillMap = new HashMap<>();
        for (SkillStrategy strategy : allStrategies) {
            String characterName = getCharacterName(strategy);// 从策略获取所属角色
            if (!characterName.isEmpty()) {
                Skill.SkillType type = strategy.getSkillType();
                skillMap.computeIfAbsent(characterName, k -> new HashMap<>())
                        .put(type, strategy);
            }
        }
    }
    public Skill createSkill(String characterName, Skill.SkillType type) {
        SkillStrategy strategy = skillMap.getOrDefault(characterName, new HashMap<>())
                .get(type);
        if (strategy == null) {
            throw new SkillNotFoundException(characterName, type);
        }
        return new Skill(
                type,
                strategy.getSkillName(),
                strategy.getBaseDamage(),
                strategy.getDiceCosts(),
                strategy.getEnergyProvide(),
                strategy.getEffects(),
                strategy.getAdditionCardId(),
                strategy,
                elementReaction
        );
    }

    // 从策略类获取所属角色
    private String getCharacterName(SkillStrategy strategy) {
        Character annotation = strategy.getClass().getAnnotation(Character.class);
        return annotation != null ? annotation.name() : "";
    }
    public class SkillNotFoundException extends RuntimeException {
        public SkillNotFoundException(String characterName, Skill.SkillType type) {
            super("角色[" + characterName + "]的技能类型[" + type + "]未找到");
        }
    }
}
