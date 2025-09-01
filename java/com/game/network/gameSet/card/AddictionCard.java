package com.game.network.gameSet.card;

import com.game.network.gameSet.EffectImple.SkillEffect;
import lombok.Getter;
import lombok.Setter;
import com.game.network.gameSet.battle.ElementReaction;
import com.game.network.player.PlayerSession;
import com.game.network.protocol.Opcode;
import com.game.network.util.SendMessage;
import org.springframework.beans.factory.annotation.Autowired;

//角色卡牌释放技能产生的召唤物
//作用在敌方身上
public class AddictionCard extends Card{
    private ElementReaction elementReaction;
    public enum Type {POISON,HEAL}
    @Getter
    private final Type type;
    @Getter
    @Setter
    //持续的回合数
    private int turn;
    @Getter
    private int value;
    @Getter
    private SkillEffect effect;
    public AddictionCard(int id, String name, Type type, int turn, int value, SkillEffect effect,ElementReaction elementReaction) {
        super(id, name);
        this.type = type;
        this.turn = turn;
        this.value = value;
        this.effect = effect;
        this.elementReaction = elementReaction;
    }
    public AddictionCard(int id, String name, Type type, int turn, int value,ElementReaction elementReaction) {
        super(id, name);
        this.type = type;
        this.turn = turn;
        this.value = value;
        this.elementReaction = elementReaction;
    }
    public boolean isDestroyed(){
        return turn<=0;
    }
    @Override
    public boolean onUse(PlayerSession playerSession1, PlayerSession playerSession2, int i) {
        //通过附属卡找到角色卡牌
        CharacterCard card = playerSession1.findByAdditionCardId(id);
        CharacterCard curCard = playerSession1.getActiveCharacter();
        CharacterCard opoCard = playerSession2.getActiveCharacter();
        if (type==Type.HEAL){
            curCard.heal(value);
            SendMessage.sendMessage(playerSession1.getPlayerChannel(),"治疗当前上场角色"+value+"点生命值",Opcode.BROADCAST_OPCODE,playerSession1.getPlayerId());
            turn--;
        }
        if (type==Type.POISON){
            int reactionDamage = elementReaction.calculateReactionDamage(curCard, playerSession1, playerSession2, opoCard);
            opoCard.takeDamage(value+reactionDamage,curCard,null,playerSession1,playerSession2);
            SendMessage.damageBroadcast(playerSession1,playerSession2,name,value, opoCard.getCurrentHp(),opoCard.getName());
            turn--;
        }
        if (effect!=null&& card !=null){
            effect.execute(card,opoCard,playerSession1,playerSession2);
        }
        if (isDestroyed()){
            SendMessage.sendMessage(playerSession1.getPlayerChannel(),"召唤物"+name+"下场", Opcode.BROADCAST_OPCODE,playerSession1.getPlayerId());
            SendMessage.sendMessage(playerSession2.getPlayerChannel(),"召唤物"+name+"下场", Opcode.BROADCAST_OPCODE,playerSession2.getPlayerId());
            //移除当前玩家的召唤物
            playerSession1.removeAdditionCard(this);
            return false;
        }
        return true;
    }
}
