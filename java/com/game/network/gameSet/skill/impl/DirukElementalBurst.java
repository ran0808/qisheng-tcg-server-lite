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
import org.springframework.stereotype.Component;
import com.game.network.gameSet.skill.Character;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
@Component
@Character(name = "迪卢克")
public class DirukElementalBurst implements SkillStrategy {
    private static final Skill.SkillType TYPE = Skill.SkillType.ELEMENTAL_BURST;
    private static final String NAME = "黎明";
    private static final int BASE_DAMAGE = 8;
    private static final int ENERGY_PROVIDE = 0;
    @Override
    public int execute(CharacterCard caster, CharacterCard target, PlayerSession currentPlayer, PlayerSession oppositePlayer, Map<Integer, Card> cardLibrary) {
        // 添加"普通攻击变火元素"状态（持续2回合）
        Status fireAttackStatus = new Status(Status.Type.NORMAL_ATTACK_FIRE, 0, 2, "火");
        caster.getStatuses().add(fireAttackStatus);
        SendMessage.sendMessage(
                currentPlayer.getPlayerChannel(),
                caster.getName() + "释放大招后，普通攻击变为火元素伤害，持续2回合！",
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
        costs.add(new DiceCost(3, "火"));
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
        effects.add(new ElementAttachEffect("火"));
        return effects;
    }
}
