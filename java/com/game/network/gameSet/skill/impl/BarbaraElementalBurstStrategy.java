package com.game.network.gameSet.skill.impl;

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
@Character(name = "芭芭拉")
public class BarbaraElementalBurstStrategy implements SkillStrategy {
    private static final Skill.SkillType TYPE = Skill.SkillType.ELEMENTAL_BURST;
    private static final String NAME = "闪耀奇迹";
    private static final int BASE_DAMAGE = 4;
    private static final int ENERGY_PROVIDE = 0;
    @Override
    public int execute(CharacterCard caster, CharacterCard target, PlayerSession currentPlayer, PlayerSession oppositePlayer, Map<Integer, Card> cardLibrary) {
        // 治疗当前所有存活卡牌
        for (CharacterCard characterCard : currentPlayer.getCharacterCards()) {
            if (characterCard.isAlive()) {
                characterCard.heal(4);
            }
        }
        caster.setEnergy(0);
        SendMessage.sendMessage(
                currentPlayer.getPlayerChannel(),
                caster.getName() + "获得四点治疗",
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
        List<DiceCost> costs = new ArrayList<>();
        costs.add(new DiceCost(3, "水"));
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
        return null;
    }
}
