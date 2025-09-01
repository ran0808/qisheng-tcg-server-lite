package com.game.network.gameSet.skill.impl;

import com.game.network.gameSet.EffectImple.ElementAttachEffect;
import com.game.network.gameSet.EffectImple.SkillEffect;
import com.game.network.gameSet.battle.ElementReaction;
import com.game.network.gameSet.card.AddictionCard;
import com.game.network.gameSet.card.Card;
import com.game.network.gameSet.card.CharacterCard;
import com.game.network.gameSet.dice.DiceCost;
import com.game.network.gameSet.skill.Skill;
import com.game.network.gameSet.skill.SkillStrategy;
import com.game.network.gameSet.status.Status;
import com.game.network.player.PlayerSession;
import com.game.network.gameSet.skill.Character;
import com.game.network.protocol.Opcode;
import com.game.network.util.SendMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
@Component
@Character(name = "缇纳里")
public class TinariNormalAttack implements SkillStrategy {
    private static final Skill.SkillType TYPE = Skill.SkillType.NORMAL_ATTACK;
    private static final String NAME = "藏蕴破障";
    private static final int BASE_DAMAGE = 2;
    private static final int ENERGY_PROVIDE = 1;
    private static final int ADDITION_CARD_ID = 13;
    @Autowired
    private ElementReaction elementReaction;
    @Override
    public int execute(CharacterCard caster, CharacterCard target, PlayerSession currentPlayer, PlayerSession oppositePlayer, Map<Integer, Card> cardLibrary) {
        //检查是否有通塞识状态以及是否是重击
        boolean hasTongSaiShi  = caster.getStatuses().stream()
                .anyMatch(status -> status.getType()== Status.Type.TONGSAISHI && status.getRemainingTurns()>0);
        if (hasTongSaiShi && currentPlayer.getDices().size()%2==0){
            SendMessage.sendMessage(currentPlayer.getPlayerChannel(),
                    caster.getName() + "的重击因通塞识效果转为草元素伤害！",
                    Opcode.BROADCAST_OPCODE, currentPlayer.getPlayerId());
            Skill skill = caster.getSkillByName("藏蕴破障");
            AddictionCard addictionCard = currentPlayer.getAddictionCards().get(caster);
            if (addictionCard !=null){
                if (addictionCard.getTurn()<2) {
                    SendMessage.sendMessage(currentPlayer.getPlayerChannel(),"附加卡藏蕴花矢的轮数加一，变成"+addictionCard.getTurn(),Opcode.BROADCAST_OPCODE,currentPlayer.getPlayerId());
                }
                else {
                    SendMessage.sendMessage(currentPlayer.getPlayerChannel(),"附加卡藏蕴花矢的轮数已达到最大值"+addictionCard.getTurn(),Opcode.BROADCAST_OPCODE,currentPlayer.getPlayerId());
                }
            }else {
                AddictionCard card = (AddictionCard) cardLibrary.get(ADDITION_CARD_ID);
                addictionCard = new AddictionCard(card.getId(),card.getName(),card.getType(),card.getTurn(),card.getValue(),card.getEffect(),elementReaction);
                currentPlayer.addAdditionCard(caster,addictionCard);
              }
            if ( skill!=null&&skill.getEffects() == null) {
                skill.setEffects(new ArrayList<>());
            }
            if (skill != null) {
                skill.getEffects().add(new ElementAttachEffect("草"));
            }
            // 减少持续回合
            caster.getStatuses().forEach(s -> {
                if (s.getType() == Status.Type.TONGSAISHI) {
                    s.setRemainingTurns(s.getRemainingTurns() - 1);
                }
            });
            // 移除过期状态
            caster.getStatuses().removeIf(s -> s.getType() == Status.Type.TONGSAISHI && s.getRemainingTurns() <= 0);
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
        DiceCost diceCost1 = new DiceCost(1, "草");
        DiceCost diceCost2 = new DiceCost(2, "任意");
        List<DiceCost> diceCosts = new ArrayList<>();
        diceCosts.add(diceCost1);
        diceCosts.add(diceCost2);
        return diceCosts;
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
