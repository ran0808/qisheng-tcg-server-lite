package com.game.network.gameSet.skill;

import com.game.network.gameSet.EffectImple.SkillEffect;
import com.game.network.gameSet.card.Card;
import com.game.network.gameSet.card.CharacterCard;
import com.game.network.gameSet.dice.DiceCost;
import com.game.network.player.PlayerSession;

import java.util.List;
import java.util.Map;

public interface SkillStrategy {
    // 执行技能逻辑（返回额外伤害）
    int execute(CharacterCard caster, CharacterCard target, PlayerSession currentPlayer, PlayerSession oppositePlayer, Map<Integer, Card> cardLibrary);
    //获取技能类型
    Skill.SkillType getSkillType();

    // 获取技能名称
    String getSkillName();

    // 获取骰子消耗
    List<DiceCost> getDiceCosts();

    // 获取基础伤害
    int getBaseDamage();

    // 获取能量提供值
    int getEnergyProvide();

    // 获取附加效果（如元素附着、护盾）
    List<SkillEffect> getEffects();

    // 获取关联附加卡ID（如召唤物）
    default int getAdditionCardId() {
        return 0; // 默认无附加卡
    }

    default void adjustDamage(Skill skill, CharacterCard caster) {
    }

}
