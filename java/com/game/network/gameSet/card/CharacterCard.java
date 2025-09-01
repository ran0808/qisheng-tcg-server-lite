package com.game.network.gameSet.card;

import com.game.network.gameSet.EffectImple.ElementAttachEffect;
import com.game.network.gameSet.EffectImple.SkillEffect;
import com.game.network.gameSet.skill.Skill;
import com.game.network.gameSet.status.BattleStatus;
import com.game.network.gameSet.status.Status;
import lombok.Getter;
import lombok.Setter;
import com.game.network.gameSet.battle.ElementReaction;
import com.game.network.handler.GameStateManager;
import com.game.network.player.PlayerSession;
import com.game.network.player.PlayerSessionManager;
import com.game.network.protocol.Opcode;
import com.game.network.util.SendMessage;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import java.util.*;
public class CharacterCard extends Card implements ApplicationContextAware {
    @Getter
    private ElementReaction elementReaction;
    private static ApplicationContext applicationContext;
    //包含角色特有的属性和技能体系
    @Getter
    private final PlayerSessionManager playerSessionManager;
    @Getter
    private final String element;//元素属性
    @Getter
    private final int maxHp;//最大生命值
    @Getter
    @Setter
    private int currentHp;//当前生命值
    @Getter
    @Setter
    private int energy;//元素战技提供能量
    @Getter
    private final int energyNeeded;//释放大招所需能量
    @Getter
    private final List<Skill> skills = new ArrayList<>(3);//技能列表（固定三个：普攻/战技/大招）
    //状态列表(元素附着等)
    @Getter
    private final List<Status> statuses = new ArrayList<>();
    //附着元素栈(用来处理元素反应)
    @Getter
    private final Deque<Status> elementStack = new ArrayDeque<>();
    //出战状态列表
    @Getter
    private final List<BattleStatus> battleStatues = new ArrayList<>();
    @Getter
    @Setter
    private AddictionCard addictionCard;//角色专属召唤物
    @Getter
    //本回合战技使用次数
    private int elementalSkillUseCount = 0;
    private final GameStateManager gameStateManager;
    // 重置回合计数器（在回合切换时调用）
    public void resetTurnCounter() {
        elementalSkillUseCount = 0;
    }
    // 战技使用次数自增
    public void incrementSkillCount() {
        elementalSkillUseCount++;
    }
    public CharacterCard(int id, String name,String element,int maxHp,int energyNeeded,GameStateManager gameStateManager,PlayerSessionManager playerSessionManager,ElementReaction elementReaction) {
        super(id, name);
        this.element=element;
        this.maxHp=maxHp;
        this.currentHp=maxHp;
        this.energy=0;
        this.energyNeeded=energyNeeded;
        this.gameStateManager = gameStateManager;
        this.playerSessionManager = playerSessionManager;
        this.elementReaction = elementReaction;
    }
    //添加技能(按顺序：0=普攻，1=战技，2=大招)
    public void addSkill(Skill skill){
        if (skills.size()<3) {
            skills.add(skill);
        }
    }
    @Override
    public void setApplicationContext(ApplicationContext context){
        applicationContext = context;
    }
    private Map<Integer, Card> getCardLibrary() {
        return applicationContext.getBean("cardLibrary", Map.class);
    }
    @Override
    public boolean onUse(PlayerSession currentPlayer, PlayerSession oppositePlayer, int skillIndex) {
        if (hasStatus(Status.Type.FREEZE)) {
            SendMessage.sendMessage(
                    currentPlayer.getPlayerChannel(),
                    this.getName() + "处于冻结状态，无法使用技能！",
                    Opcode.ALERT_OPCODE,
                    currentPlayer.getPlayerId()
            );
            return false;
        }
        if (hasStatus(Status.Type.PETRIFY)) {
            SendMessage.sendMessage(
                    currentPlayer.getPlayerChannel(),
                    this.getName() + "处于石化状态，无法使用技能！",
                    Opcode.ALERT_OPCODE,
                    currentPlayer.getPlayerId()
            );
            return false;
        }
        CharacterCard caster = this; // 当前卡牌为施法者
        //获取对手的当前出战卡牌
        CharacterCard target = oppositePlayer.getActiveCharacter();
        // 检查技能索引合法性
        if (skillIndex < 0 || skillIndex >= skills.size()) {
            SendMessage.sendMessage(currentPlayer.getPlayerChannel(),
                    "无效的技能索引！", Opcode.ALERT_OPCODE, currentPlayer.getPlayerId());
            return false;
        }
        //获取技能
        Skill skill = skills.get(skillIndex);
        // 执行技能（技能自身处理消耗、伤害、效果）
        skill.execute(caster, target, currentPlayer, oppositePlayer,getCardLibrary());
        //交换回合
        if (playerSessionManager.getTurnOver().isEmpty()||!playerSessionManager.getTurnOver().get(0).equals(oppositePlayer.getPlayerId())) {
            gameStateManager.changeCurrentTurnPlayer(oppositePlayer.getPlayerId());
        }
        // 检查目标是否被击败
        if (!oppositePlayer.getActiveCharacter().isAlive()) {
            gameStateManager.handleCharacterDefeat(oppositePlayer, currentPlayer);
            return true;
        }
        if (!caster.isAlive()) {
            gameStateManager.handleCharacterDefeat(currentPlayer, oppositePlayer);
            return true;
        }
        return true;
    }
    //受到伤害时(含护盾和元素反应)
    public int  takeDamage(int originalDamage, CharacterCard attacker, Skill skill, PlayerSession currentPlayer, PlayerSession oppositePlayer){
        //1.计算元素反应伤害
        int reactionDamage = 0;
        if (hasElementEffect(skill)) {
            reactionDamage = elementReaction.calculateReactionDamage(attacker,currentPlayer,oppositePlayer,this);
        }
        int finalDamage = originalDamage + reactionDamage;
        //2.处理冻结状态的伤害加成与解除
        boolean isFrozen = hasStatus(Status.Type.FREEZE);
        if (isFrozen){
            //判断是否是火元素伤害或普通攻击
            boolean isFireAttack = attacker.getElement().equals("火")&& hasElementEffect(skill);
            boolean isNormalAttack = skill.getType() == Skill.SkillType.NORMAL_ATTACK&&!hasElementEffect(skill);
            if (isNormalAttack||isFireAttack){
                finalDamage +=2;
                //解除冻结状态
                statuses.removeIf(status -> status.getType()==Status.Type.FREEZE);
                //广播去除消息
                SendMessage.sendMessage(
                        currentPlayer.getPlayerChannel(),
                        "目标" + this.getName() + "的冻结状态被解除，额外造成2点伤害！",
                        Opcode.BROADCAST_OPCODE,
                        currentPlayer.getPlayerId()
                );
                SendMessage.sendMessage(
                        oppositePlayer.getPlayerChannel(),
                        this.getName() + "的冻结状态被解除，受到额外2点伤害！",
                        Opcode.BROADCAST_OPCODE,
                        oppositePlayer.getPlayerId()
                );
            }
        }
        //3.处理绽放反应的伤害加成
        if (currentPlayer.getReactionCard()!=null){
            boolean isBlOOm = currentPlayer.getReactionCard().getName().equals("草原核");
            if (isBlOOm){
                boolean isContainAttack = (Arrays.asList("火","雷").contains(attacker.getElement()))&& hasElementEffect(skill);
                if (isContainAttack){
                    finalDamage+=2;
                    //将附加卡下场
                    SendMessage.sendMessage(currentPlayer.getPlayerChannel(),"卡牌"+"草原核"+"下场", Opcode.BROADCAST_OPCODE,currentPlayer.getPlayerId());
                    SendMessage.sendMessage(oppositePlayer.getPlayerChannel(),"卡牌"+"草原核"+"下场", Opcode.BROADCAST_OPCODE,oppositePlayer.getPlayerId());
                    currentPlayer.setReactionCard(null);
                    //对敌方进行火元素或雷元素附着
                    applyElementStatus(oppositePlayer.getActiveCharacter(),new Status(Status.Type.ELEMENT_ATTACH,attacker.getElement()));
                    //进行播报
                    SendMessage.sendMessage(
                            currentPlayer.getPlayerChannel(),
                            "草原核爆炸"+this.getName() + "受到额外2点伤害！",
                            Opcode.BROADCAST_OPCODE,
                            currentPlayer.getPlayerId()
                    );
                    SendMessage.sendMessage(
                            oppositePlayer.getPlayerChannel(),
                            "草原核爆炸"+this.getName() + "受到额外2点伤害！",
                            Opcode.BROADCAST_OPCODE,
                            oppositePlayer.getPlayerId()
                    );
                }
            }
        }
        //4.处理原激化反应的伤害加成
        if (currentPlayer.getReactionCard()!=null) {
            boolean isAggravated = currentPlayer.getReactionCard().getName().equals("激化领域");
            if (isAggravated){
                boolean isContainAttack = (Arrays.asList("草","雷").contains(attacker.getElement()))&& hasElementEffect(skill);
                if (isContainAttack){
                    finalDamage+=1;
                    //使用附加卡使用并且次数减1
                    currentPlayer.getReactionCard().setTurn(currentPlayer.getReactionCard().getTurn()-1);
                    //对敌方进行草元素或雷元素附着
                    applyElementStatus(oppositePlayer.getActiveCharacter(),new Status(Status.Type.ELEMENT_ATTACH,attacker.getElement()));
                    //进行播报
                    SendMessage.sendMessage(
                            currentPlayer.getPlayerChannel(),
                            "原激化反应！"+this.getName() + "受到额外1点伤害！",
                            Opcode.BROADCAST_OPCODE,
                            currentPlayer.getPlayerId()
                    );
                    SendMessage.sendMessage(
                            oppositePlayer.getPlayerChannel(),
                            "原激化反应！"+this.getName() + "受到额外1点伤害！",
                            Opcode.BROADCAST_OPCODE,
                            oppositePlayer.getPlayerId()
                    );
                }
            }
        }
        //5.优先消耗护盾
        int remainingDamage = finalDamage;
        //5.1先消耗结晶护盾
        Iterator<BattleStatus> battleStatusIterator1 = battleStatues.iterator();
        while (battleStatusIterator1.hasNext()&& remainingDamage >0){
            BattleStatus battleStatus = battleStatusIterator1.next();
            if (battleStatus.getType()== BattleStatus.Type.SHIELD_CRYSTALLIZE){
                int shieldAbsorb = Math.min(remainingDamage, battleStatus.getValue());
                remainingDamage -= shieldAbsorb;
                battleStatus.setValue(battleStatus.getValue()-shieldAbsorb);
                if (battleStatus.getValue()<=0){
                    battleStatusIterator1.remove();//护盾耗尽
                }
            }
        }
        //5.2消耗战技护盾
        Iterator<BattleStatus> battleStatusIterator2 = battleStatues.iterator();
        while (battleStatusIterator2.hasNext()&& remainingDamage >0){
            BattleStatus battleStatus = battleStatusIterator2.next();
            if (battleStatus.getType()== BattleStatus.Type.SHIELD_SKILL){
                int shieldAbsorb = Math.min(remainingDamage, battleStatus.getValue());
                remainingDamage -= shieldAbsorb;
                battleStatus.setValue(battleStatus.getValue()-shieldAbsorb);
                if (battleStatus.getValue()<=0){
                    battleStatusIterator2.remove();//护盾耗尽
                }
            }
        }
        //6.扣除剩余生命值
        this.currentHp = Math.max(0,this.currentHp-remainingDamage);
        return finalDamage;
    }
    //判断技能是否包含元素效果
    private boolean hasElementEffect(Skill skill){
        if (skill==null||skill.getEffects()==null){
            return false;
        }
        for (SkillEffect effect : skill.getEffects()){
            if (effect instanceof ElementAttachEffect){
                return true;
            }
        }
        return false;
    }
    // 获取当前附着的元素
    public String getAttachedElement() {
        return elementStack.isEmpty() ? null : elementStack.peek().getElement();
    }
    //进行元素附着
    public void applyElementStatus(CharacterCard card,Status newStatus){
        //进行判断元素栈中是否元素附着
        Status topElement = card.elementStack.peek();
        String newElement = newStatus.getElement();
        if (topElement==null&&!(newElement.equals("岩")||newElement.equals("风"))){
            card.elementStack.push(newStatus);
            card.getStatuses().add(newStatus);
            return;
        }
        String currentElement = null;
        if (topElement != null) {
            currentElement = topElement.getElement();
        }
        if (elementReaction.canReact(currentElement,newElement)){
            //产生元素反应后将栈中元素弹出，并清除元素附着状态
            card.elementStack.pop();
            card.getStatuses().remove(topElement);
        }
        else if (currentElement != null) {
            //草风，草岩不反应
            if ((!(currentElement.equals("草")&&(newElement.equals("岩")||newElement.equals("风"))))){
                card.elementStack.pop();
                card.getStatuses().remove(topElement);
                card.elementStack.push(newStatus);
                card.getStatuses().add(newStatus);
            }
        }
    }
    //回血
    public void heal(int amount){
        this.currentHp=Math.min(maxHp,this.currentHp+amount);
    }
    //加能量
    public void gainEnergy(int amount){
        this.energy = Math.min(energyNeeded,this.energy+amount);
    }
    //是否能使用大招
    public boolean canUseBurst(){
        return energy>=energyNeeded;
    }
    // 检查是否存活
    public boolean isAlive() {
        return currentHp > 0;
    }
    //受到穿透伤害
    public void takePiercingDamage(int amount){
        this.currentHp = Math.max(0,currentHp - amount);
    }
    public boolean hasStatus(Status.Type type) {
        for (Status status : statuses) {
            if (status.getType() == type && !status.isExpired()) {
                return true;
            }
        }
        return false;
    }

    public Skill getSkillByName(String skillName) {
        for (Skill skill : skills) {
            if (skill.getName().equals(skillName))
                return skill;
        }
        return null;
    }
}