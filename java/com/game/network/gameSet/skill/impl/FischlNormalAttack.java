package com.game.network.gameSet.skill.impl;

import com.game.network.gameSet.EffectImple.SkillEffect;
import com.game.network.gameSet.card.Card;
import com.game.network.gameSet.card.CharacterCard;
import com.game.network.gameSet.dice.DiceCost;
import com.game.network.gameSet.skill.Skill;
import com.game.network.gameSet.skill.SkillStrategy;
import com.game.network.player.PlayerSession;
import com.game.network.gameSet.skill.Character;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
@Component
@Character(name = "菲谢尔")
public class FischlNormalAttack implements SkillStrategy{
        private static final Skill.SkillType TYPE = Skill.SkillType.NORMAL_ATTACK;
        private static final String NAME = "灭罪之矢";
        private static final int BASE_DAMAGE = 2;
        private static final int ENERGY_PROVIDE = 1;
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
            DiceCost diceCost1 = new DiceCost(1, "雷");
            DiceCost diceCost2 = new DiceCost(2, "任意");
            List<DiceCost> diceCosts = new ArrayList<>();
            diceCosts.add(diceCost1);
            diceCosts.add(diceCost2);
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
            return null;
        }
    }

