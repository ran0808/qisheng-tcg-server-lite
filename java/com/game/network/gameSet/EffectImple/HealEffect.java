package com.game.network.gameSet.EffectImple;

import com.game.network.gameSet.card.CharacterCard;
import com.game.network.player.PlayerSession;

public class HealEffect extends SkillEffect {
    public HealEffect(EffectType type, int value) {
        super(type, value);
    }

    @Override
    public void execute(CharacterCard caster, CharacterCard target, PlayerSession casterSession, PlayerSession targetSession) {
        caster.heal(value);
    }
}
