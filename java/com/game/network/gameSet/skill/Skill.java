package com.game.network.gameSet.skill;
import com.game.network.gameSet.EffectImple.SkillEffect;
import com.game.network.gameSet.card.*;
import com.game.network.gameSet.dice.DiceCost;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import com.game.network.gameSet.dice.DiceService;
import com.game.network.gameSet.battle.ElementReaction;
import com.game.network.player.PlayerSession;
import com.game.network.protocol.Opcode;
import com.game.network.util.SendMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;

import java.util.List;
import java.util.Map;

@AllArgsConstructor
@Data
public class Skill {
    private ElementReaction elementReaction;
    public enum SkillType { NORMAL_ATTACK, ELEMENTAL_SKILL, ELEMENTAL_BURST }
    private  SkillType type; // 技能类型
    private String name;//技能名称
    private int damage;//技能伤害
    @Getter
    private List<DiceCost> diceCosts;//骰子的消耗组合
    private int energyProvide;//所提供能量值
    public List<SkillEffect> effects;//附加效果（如挂元素，加护盾，产生附加产物）
    public int addition_cardId;
    private SkillStrategy skillStrategy;
    public Skill(SkillType type, String name, int damage, List<DiceCost> dieCosts, int energyProvide, List<SkillEffect> effects,SkillStrategy skillStrategy) {
        this.type = type;
        this.name = name;
        this.damage = damage;
        this.diceCosts = dieCosts;
        this.energyProvide = energyProvide;
        this.effects = effects;
        this.skillStrategy = skillStrategy;
    }
    public Skill(SkillType type, String name, int damage, List<DiceCost> dieCosts, int energyProvide, List<SkillEffect> effects,int addition_cardId,SkillStrategy skillStrategy,ElementReaction elementReaction) {
        this.type = type;
        this.name = name;
        this.damage = damage;
        this.diceCosts = dieCosts;
        this.energyProvide = energyProvide;
        this.effects = effects;
        this.addition_cardId=addition_cardId;
        this.skillStrategy = skillStrategy;
        this.elementReaction = elementReaction;
    }
   //执行技能效果
    public void execute(CharacterCard caster, CharacterCard target, PlayerSession currentPlayer, PlayerSession oppositePlayer, Map<Integer, Card> cardLibrary) {
        int extraDamage = 0;
        if (skillStrategy != null) {
            extraDamage = skillStrategy.execute(caster, target, currentPlayer, oppositePlayer, cardLibrary);
            // 2. 造成伤害
            int finalDamage = damage+extraDamage;
            target.takeDamage(finalDamage, caster, this, currentPlayer, oppositePlayer);
            SendMessage.damageBroadcast(
                    currentPlayer,
                    oppositePlayer,
                    this.name,
                    finalDamage,
                    target.getCurrentHp(),
                    target.getName()
            );
            //3.消耗骰子
            DiceService.consumeDice(currentPlayer, diceCosts);
            // 4. 能量处理（大招不产能量）
            if (type != SkillType.ELEMENTAL_BURST) {
                caster.gainEnergy(energyProvide);
                SendMessage.sendMessage(
                        currentPlayer.getPlayerChannel(),
                        caster.getName() + "获得" + energyProvide + "点能量，当前能量：" + caster.getEnergy(),
                        Opcode.BROADCAST_OPCODE,
                        currentPlayer.getPlayerId()
                );
            } else {
                caster.setEnergy(0);
            }
            // 5. 触发附加效果（元素附着、护盾等）
            if (effects != null) {
                for (SkillEffect effect : effects) {
                    effect.execute(caster, target, currentPlayer, oppositePlayer);
                }
            }
            // 6. 处理附加卡（如召唤物）
            if (addition_cardId > 0) {
                AddictionCard card = (AddictionCard) cardLibrary.get(addition_cardId);
                AddictionCard newCard = null;
                if (card != null) {
                    newCard = new AddictionCard(card.getId(), card.getName(), card.getType(), card.getTurn(), card.getValue(), card.getEffect(), elementReaction);
                }
                if (newCard != null) {
                    currentPlayer.addAdditionCard(caster, newCard);
                }
            }
        }
    }
}
