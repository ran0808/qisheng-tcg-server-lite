package com.game.network.gameSet.skill.impl;

import com.game.network.gameSet.EffectImple.ElementAttachEffect;
import com.game.network.gameSet.EffectImple.SkillEffect;
import com.game.network.gameSet.card.Card;
import com.game.network.gameSet.card.CharacterCard;
import com.game.network.gameSet.dice.DiceCost;
import com.game.network.gameSet.skill.Skill;
import com.game.network.gameSet.skill.SkillStrategy;
import com.game.network.player.PlayerSession;
import com.game.network.protocol.Opcode;
import com.game.network.util.SendMessage;
import org.springframework.stereotype.Component;
import com.game.network.gameSet.skill.Character;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
@Component
@Character(name = "砂糖")
public class SugarElementalSkill implements SkillStrategy {
    private static final Skill.SkillType TYPE = Skill.SkillType.ELEMENTAL_SKILL;
    private static final String NAME = "风灵作成·陆叁零捌";
    private static final int BASE_DAMAGE = 3; // 伤害通过穿透实现
    private static final int ENERGY_PROVIDE = 1;
    @Override
    public int execute(CharacterCard caster, CharacterCard target, PlayerSession currentPlayer, PlayerSession oppositePlayer, Map<Integer, Card> cardLibrary) {
        if (oppositePlayer.getActiveCharacter() == target) {
            //获取上一个使用的角色
            CharacterCard lastCharacter = oppositePlayer.getLastActiveCharacter();
            if (lastCharacter != null && lastCharacter != target) {
                oppositePlayer.setActiveCharacter(lastCharacter);
                //广播切换消息
                SendMessage.sendMessage(
                        currentPlayer.getPlayerChannel(),
                        "敌方" + target.getName() + "被强制切换为" + lastCharacter.getName(),
                        Opcode.BROADCAST_OPCODE,
                        currentPlayer.getPlayerId()
                );
                SendMessage.sendMessage(
                        oppositePlayer.getPlayerChannel(),
                        "你的" + target.getName() + "被强制切换为" + lastCharacter.getName(),
                        Opcode.BROADCAST_OPCODE,
                        oppositePlayer.getPlayerId()
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
        costs.add(new DiceCost(3, "风"));
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
        ElementAttachEffect skillEffect = new ElementAttachEffect("风");
        List<SkillEffect> effects = new ArrayList<>();
        effects.add(skillEffect);
        return effects;
    }
}
