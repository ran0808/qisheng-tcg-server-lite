package com.game.network.gameSet.skill.impl;

import com.game.network.gameSet.EffectImple.ElementAttachEffect;
import com.game.network.gameSet.EffectImple.SkillEffect;
import com.game.network.gameSet.card.Card;
import com.game.network.gameSet.card.CharacterCard;
import com.game.network.gameSet.dice.DiceCost;
import com.game.network.gameSet.skill.Skill;
import com.game.network.gameSet.skill.SkillStrategy;
import com.game.network.player.PlayerSession;
import com.game.network.util.SendMessage;
import org.springframework.stereotype.Component;
import com.game.network.gameSet.skill.Character;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
@Component
@Character(name = "缇纳里")
public class TinariElementalBurst implements SkillStrategy {
    private static final Skill.SkillType TYPE = Skill.SkillType.ELEMENTAL_BURST;
    private static final String NAME = "造生缠藤箭";
    private static final int BASE_DAMAGE = 4; // 伤害通过穿透实现
    private static final int ENERGY_PROVIDE = 0;
    @Override
    public int execute(CharacterCard caster, CharacterCard target, PlayerSession currentPlayer, PlayerSession oppositePlayer, Map<Integer, Card> cardLibrary) {
        for (CharacterCard enemyCharacter : oppositePlayer.getCharacterCards()) {
            if (enemyCharacter != target && enemyCharacter.isAlive()) {
                enemyCharacter.takePiercingDamage(1);
                //广播穿透伤害信息
                SendMessage.damageBroadcast(
                        currentPlayer,
                        oppositePlayer,
                        "缇纳里元素爆发造成1点穿透伤害",
                        1,
                        enemyCharacter.getCurrentHp(),
                        enemyCharacter.getName()
                );
            }
        }
        return 0;
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
        costs.add(new DiceCost(3, "草"));
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
        ElementAttachEffect skillEffect = new ElementAttachEffect("草");
        List<SkillEffect> effects = new ArrayList<>();
        effects.add(skillEffect);
        return effects;
    }
}
