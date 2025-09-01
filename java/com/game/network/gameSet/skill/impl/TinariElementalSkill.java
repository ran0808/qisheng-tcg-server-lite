package com.game.network.gameSet.skill.impl;

import com.game.network.gameSet.EffectImple.ElementAttachEffect;
import com.game.network.gameSet.EffectImple.SkillEffect;
import com.game.network.gameSet.card.Card;
import com.game.network.gameSet.card.CharacterCard;
import com.game.network.gameSet.dice.DiceCost;
import com.game.network.gameSet.skill.Skill;
import com.game.network.gameSet.skill.SkillStrategy;
import com.game.network.gameSet.status.Status;
import com.game.network.player.PlayerSession;
import com.game.network.protocol.Opcode;
import com.game.network.util.SendMessage;
import com.game.network.gameSet.skill.Character;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
@Component
@Character(name = "缇纳里")
public class TinariElementalSkill implements SkillStrategy {
    private static final Skill.SkillType TYPE = Skill.SkillType.ELEMENTAL_SKILL;
    private static final String NAME = "识果种雷";
    private static final int BASE_DAMAGE = 2;
    private static final int ENERGY_PROVIDE = 1;

    @Override
    public int execute(CharacterCard caster, CharacterCard target, PlayerSession currentPlayer, PlayerSession oppositePlayer, Map<Integer, Card> cardLibrary) {
        Status TongSaiShi = new Status(Status.Type.TONGSAISHI,0,2);
        caster.getStatuses().add(TongSaiShi);
        SendMessage.sendMessage(currentPlayer.getPlayerChannel(),caster.getName()+"释放元素战技后，重击时物理伤害，变成草元素伤害，并召唤藏蕴花矢", Opcode.BROADCAST_OPCODE,currentPlayer.getPlayerId());
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
        List<SkillEffect> effects = new ArrayList<>();
        effects.add(new ElementAttachEffect("草"));
        return effects;
    }
}
