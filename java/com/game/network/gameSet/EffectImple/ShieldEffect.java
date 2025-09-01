package com.game.network.gameSet.EffectImple;

import com.game.network.gameSet.status.BattleStatus;
import com.game.network.gameSet.card.CharacterCard;
import com.game.network.player.PlayerSession;
import com.game.network.protocol.Opcode;
import com.game.network.util.SendMessage;

//给自己加护盾
public class ShieldEffect extends SkillEffect {
    public ShieldEffect(int shieldValue) {
        super(SkillEffect.EffectType.SHIELD, shieldValue);
    }
    @Override
    public void execute(CharacterCard caster, CharacterCard target,
                        PlayerSession casterSession, PlayerSession targetSession) {
        // 给施法者添加护盾状态,持续到盾破
        caster.getBattleStatues().add(new BattleStatus(BattleStatus.Type.SHIELD_SKILL, value,-1));
        SendMessage.sendMessage(casterSession.getPlayerChannel(),
                caster.getName() + "获得" + value + "点护盾！",
                Opcode.BROADCAST_OPCODE, casterSession.getPlayerId());
        SendMessage.sendMessage(targetSession.getPlayerChannel(),
                caster.getName() + "获得" + value + "点护盾！",
                Opcode.BROADCAST_OPCODE, targetSession.getPlayerId());
    }
}
