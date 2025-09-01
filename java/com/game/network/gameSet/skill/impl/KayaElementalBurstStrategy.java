package com.game.network.gameSet.skill.impl;

import com.game.network.gameSet.EffectImple.SkillEffect;
import com.game.network.gameSet.status.BattleStatus;
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
@Character(name = "凯亚")
public class KayaElementalBurstStrategy implements SkillStrategy {
    private static final Skill.SkillType TYPE = Skill.SkillType.ELEMENTAL_BURST;
    private static final String NAME = "凛冽轮舞";
    private static final int BASE_DAMAGE = 1;
    private static final int ENERGY_PROVIDE = 0;
    @Override
    public int execute(CharacterCard caster, CharacterCard target, PlayerSession currentPlayer, PlayerSession oppositePlayer, Map<Integer, Card> cardLibrary) {
        // 添加"切换角色造成伤害"状态（3次使用次数，每次造成2点伤害）
        BattleStatus switchDamageStatus = new BattleStatus(BattleStatus.Type.SWITCH_DAMAGE_TRIGGER, 2, 3);
        currentPlayer.getBattleStatuses().add(switchDamageStatus);
        caster.getBattleStatues().add(switchDamageStatus);
        SendMessage.sendMessage(
                currentPlayer.getPlayerChannel(),
                caster.getName() + "释放大招后，切换角色将对敌人造成2点伤害（可使用3次）！",
                Opcode.BROADCAST_OPCODE,
                currentPlayer.getPlayerId()
        );
        SendMessage.sendMessage(
                oppositePlayer.getPlayerChannel(),
                "对方凯亚释放了大招「凛冽轮舞」！对方切换角色时将对你造成2点伤害（剩余3次）",
                Opcode.BROADCAST_OPCODE,
                oppositePlayer.getPlayerId()
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
        costs.add(new DiceCost(3, "冰"));
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
