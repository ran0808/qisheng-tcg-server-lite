package com.game.network.gameSet.EffectImple;

import com.game.network.gameSet.card.CharacterCard;
import com.game.network.player.PlayerSession;

// 抽象效果类
public abstract class SkillEffect {
    // 效果类型（枚举）
    public enum EffectType {
        SHIELD, ELEMENT_ATTACH,HEAL,POISON
    }

    protected EffectType type;
    protected int value; // 效果值（如护盾值、伤害值）

    public SkillEffect(EffectType type, int value) {
        this.type = type;
        this.value = value;
    }
    // 抽象执行方法：参数为施法者、目标、双方玩家会话
    public abstract void execute(CharacterCard caster, CharacterCard target,
                                 PlayerSession casterSession, PlayerSession targetSession);
}