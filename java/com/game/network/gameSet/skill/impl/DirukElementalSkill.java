package com.game.network.gameSet.skill.impl;

import com.game.network.gameSet.EffectImple.ElementAttachEffect;
import com.game.network.gameSet.EffectImple.SkillEffect;
import com.game.network.gameSet.card.Card;
import com.game.network.gameSet.card.CharacterCard;
import com.game.network.gameSet.dice.DiceCost;
import com.game.network.gameSet.skill.Skill;
import com.game.network.gameSet.skill.SkillStrategy;
import com.game.network.player.PlayerSession;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import com.game.network.gameSet.skill.Character;
@Component
@Character(name = "迪卢克")
public class DirukElementalSkill implements SkillStrategy {

    private static final Skill.SkillType TYPE = Skill.SkillType.ELEMENTAL_SKILL;
    private static final String NAME = "逆焰之刃";
    private static final int BASE_DAMAGE = 3;
    private static final int ENERGY_PROVIDE = 1;

    @Override
    public int execute(CharacterCard caster, CharacterCard target, PlayerSession currentPlayer, PlayerSession oppositePlayer, Map<Integer, Card> cardLibrary) {
        // 战技使用次数+1
        caster.incrementSkillCount();
        // 第三次使用伤害+2
        return caster.getElementalSkillUseCount() == 3 ? 2 : 0;
    }

    @Override
    public Skill.SkillType getSkillType() {
        return TYPE;
    }

    @Override
    public String getSkillName() {
        return NAME;
    }

    @Override
    public List<DiceCost> getDiceCosts() {
        List<DiceCost> costs = new ArrayList<>();
        costs.add(new DiceCost(3, "火"));
        return costs;
    }

    @Override
    public int getBaseDamage() {
        return BASE_DAMAGE;
    }

    @Override
    public int getEnergyProvide() {
        return ENERGY_PROVIDE;
    }

    @Override
    public List<SkillEffect> getEffects() {
        List<SkillEffect> effects = new ArrayList<>();
        effects.add(new ElementAttachEffect("火"));
        return effects;
    }

    @Override
    public void adjustDamage(Skill skill, CharacterCard caster) {
        if (caster.getElementalSkillUseCount() == 3) {
            skill.setDamage(skill.getDamage() - 2);
        }
    }
}
