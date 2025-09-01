package com.game.network.gameSet.skill.impl;

import com.game.network.gameSet.EffectImple.ElementAttachEffect;
import com.game.network.gameSet.EffectImple.ShieldEffect;
import com.game.network.gameSet.EffectImple.SkillEffect;
import com.game.network.gameSet.card.Card;
import com.game.network.gameSet.card.CharacterCard;
import com.game.network.gameSet.dice.DiceCost;
import com.game.network.gameSet.skill.Skill;
import com.game.network.gameSet.skill.SkillStrategy;
import com.game.network.player.PlayerSession;
import org.springframework.stereotype.Component;
import com.game.network.gameSet.skill.Character;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
@Component
@Character(name = "钟离")
public class ZhongliElementalSkill implements SkillStrategy{
        private static final Skill.SkillType TYPE = Skill.SkillType.ELEMENTAL_SKILL;
        private static final String NAME = "地心";
        private static final int BASE_DAMAGE = 3;
        private static final int ENERGY_PROVIDE = 1;
        private static final int ADDITION_CARD_ID = 10;
        @Override
        public int execute(CharacterCard caster, CharacterCard target, PlayerSession currentPlayer, PlayerSession oppositePlayer, Map<Integer, Card> cardLibrary) {
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
            DiceCost diceCost = new DiceCost(3, "岩");
            List<DiceCost> diceCosts = new ArrayList<>();
            diceCosts.add(diceCost);
            return diceCosts;
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
            ShieldEffect shieldEffect = new ShieldEffect(2);
            ElementAttachEffect elementEffect = new ElementAttachEffect("岩");
            List<SkillEffect> effects = new ArrayList<>();
            effects.add(shieldEffect);
            effects.add(elementEffect);
            return effects;
        }


        @Override
        public int getAdditionCardId() {
        return ADDITION_CARD_ID;
    }
}


