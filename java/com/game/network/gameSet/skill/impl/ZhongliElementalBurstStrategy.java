package com.game.network.gameSet.skill.impl;

import com.game.network.gameSet.EffectImple.ElementAttachEffect;
import com.game.network.gameSet.EffectImple.SkillEffect;
import com.game.network.gameSet.card.*;
import com.game.network.gameSet.dice.DiceCost;
import com.game.network.gameSet.skill.Skill;
import com.game.network.gameSet.skill.SkillStrategy;
import com.game.network.gameSet.status.Status;
import com.game.network.player.PlayerSession;
import com.game.network.protocol.Opcode;
import com.game.network.util.SendMessage;
import org.springframework.stereotype.Component;
import com.game.network.gameSet.skill.Character;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
@Component
@Character(name = "钟离")
public class ZhongliElementalBurstStrategy implements SkillStrategy {
    private static final Skill.SkillType TYPE = Skill.SkillType.ELEMENTAL_BURST;
    private static final String NAME = "天星";
    private static final int BASE_DAMAGE = 4;
    private static final int ENERGY_PROVIDE = 0;
    @Override
    public int execute(CharacterCard caster, CharacterCard target, PlayerSession currentPlayer, PlayerSession oppositePlayer, Map<Integer, Card> cardLibrary) {
        //石化目标，本回合无法行动
        target.getStatuses().add(new Status(Status.Type.PETRIFY, 0, 1));
        SendMessage.sendMessage(
                oppositePlayer.getPlayerChannel(),
                "目标" + target.getName() + "被石化，本回合无法行动",
                Opcode.BROADCAST_OPCODE,
                oppositePlayer.getPlayerId()
        );
        SendMessage.sendMessage(
                currentPlayer.getPlayerChannel(),
                "石化目标" + target.getName() + "本回合无法行动",
                Opcode.BROADCAST_OPCODE,
                currentPlayer.getPlayerId()
        );
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
        // 封装骰子消耗（3点岩元素）
        List<DiceCost> costs = new ArrayList<>();
        costs.add(new DiceCost(3, "岩"));
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
        // 封装元素附着效果
        List<SkillEffect> effects = new ArrayList<>();
        effects.add(new ElementAttachEffect("岩"));
        return effects;
    }
}
