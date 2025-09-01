package com.game.network.gameSet.skill.impl;

import com.game.network.gameSet.EffectImple.ElementAttachEffect;
import com.game.network.gameSet.EffectImple.SkillEffect;
import com.game.network.gameSet.card.*;
import com.game.network.gameSet.dice.DiceCost;
import com.game.network.gameSet.skill.Skill;
import com.game.network.gameSet.skill.SkillStrategy;
import com.game.network.gameSet.status.Status;
import com.game.network.player.PlayerSession;
import org.springframework.stereotype.Component;
import com.game.network.gameSet.skill.Character;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
@Component
@Character(name = "迪卢克")
public class DirukNormalAttack implements SkillStrategy {
    private static final Skill.SkillType TYPE = Skill.SkillType.NORMAL_ATTACK;
    private static final String NAME = "淬炼之剑";
    private static final int BASE_DAMAGE = 2;
    private static final int ENERGY_PROVIDE = 1;
    @Override
    public int execute(CharacterCard caster, CharacterCard target, PlayerSession currentPlayer, PlayerSession oppositePlayer, Map<Integer, Card> cardLibrary) {
        // 检查是否有"普通攻击变火元素"状态
        boolean hasFireAttack = caster.getStatuses().stream()
                .anyMatch(s -> s.getType() == Status.Type.NORMAL_ATTACK_FIRE && s.getRemainingTurns() > 0);
        if (hasFireAttack) {
            // 添加火元素附着效果
            Skill skill = caster.getSkillByName("淬炼之剑");
            if ( skill!=null&&skill.getEffects() == null) {
                skill.setEffects(new ArrayList<>());
            }
            if (skill != null) {
                skill.getEffects().add(new ElementAttachEffect("火"));
            }
            // 减少持续回合（每使用一次普通攻击减少1回合）
            caster.getStatuses().forEach(s -> {
                if (s.getType() == Status.Type.NORMAL_ATTACK_FIRE) {
                    s.setRemainingTurns(s.getRemainingTurns() - 1);
                }
            });
            // 移除过期状态
            caster.getStatuses().removeIf(s -> s.getType() == Status.Type.NORMAL_ATTACK_FIRE && s.getRemainingTurns() <= 0);
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
        DiceCost diceCost1 = new DiceCost(1, "火");
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
