package com.game.network.gameSet.skill.impl;

import com.game.network.gameSet.EffectImple.ElementAttachEffect;
import com.game.network.gameSet.EffectImple.SkillEffect;
import com.game.network.gameSet.battle.ElementReaction;
import com.game.network.gameSet.card.Card;
import com.game.network.gameSet.card.CharacterCard;
import com.game.network.gameSet.dice.DiceCost;
import com.game.network.gameSet.skill.Skill;
import com.game.network.gameSet.skill.SkillStrategy;
import com.game.network.gameSet.status.Status;
import com.game.network.player.PlayerSession;
import com.game.network.protocol.Opcode;
import com.game.network.util.SendMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.game.network.gameSet.skill.Character;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
@Component
@Character(name = "芭芭拉")
public class BarbaraElementalSkillStrategy implements SkillStrategy {
    private static final Skill.SkillType TYPE = Skill.SkillType.ELEMENTAL_SKILL;
    private static final String NAME = "演唱，开始~";
    private static final int BASE_DAMAGE = 1;
    private static final int ENERGY_PROVIDE = 1;
    private static final int ADDITION_CARD_ID = 11;
    @Autowired
    ElementReaction elementReaction;
    @Override
    public int execute(CharacterCard caster, CharacterCard target, PlayerSession currentPlayer, PlayerSession oppositePlayer, Map<Integer, Card> cardLibrary) {
        for (CharacterCard character : currentPlayer.getCharacterCards()) {
            if (character.isAlive()) {
                character.heal(1);
            }
        }
        String attachedElement = caster.getAttachedElement();
        if (attachedElement != null && !attachedElement.equals(caster.getElement())) {
            int reactionDamage = elementReaction.calculateReactionDamage(caster, currentPlayer, oppositePlayer, caster);
            if (reactionDamage > 0) {
                caster.takePiercingDamage(reactionDamage); // 承受反应伤害
            }
        }
        Status status = new Status(Status.Type.ELEMENT_ATTACH, caster.getElement());
        caster.applyElementStatus(caster, status);
        SendMessage.sendMessage(
                currentPlayer.getPlayerChannel(),
                caster.getName() + "释放演唱，开始~全体回血1点，自身附着水元素！",
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
        List<SkillEffect> effects = new ArrayList<>();
        effects.add(new ElementAttachEffect("水"));
        return effects;
    }

    @Override
    public int getAdditionCardId() {
        return ADDITION_CARD_ID;
    }
}
