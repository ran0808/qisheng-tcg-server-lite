package com.game.network.player;
import com.game.network.gameSet.dice.DiceCost;
import com.game.network.gameSet.status.Status;
import io.netty.channel.Channel;
import lombok.Data;
import com.game.network.gameSet.status.BattleStatus;
import com.game.network.gameSet.card.*;
import com.game.network.protocol.Opcode;
import com.game.network.util.SendMessage;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Data
//玩家会话类，存储玩家连接相关信息
public class PlayerSession {
    //角色到元素的映射

    //玩家唯一标识
    private String playerId;
    //网络连接通道
    private Channel playerChannel;
    //登录时间
    private LocalDateTime loginTime;
    //离线时间
    private static LocalDateTime lastActiveTime;
    //登录状态标识
    private boolean LoggedIn = false;//默认未登录
    //游戏准备状态标识
    private boolean ready = false;
    //判断是否选好卡牌
    private boolean selected = false;
    //是否已经完成骰子重投
    private boolean diceReselected = false;
    //判断是否为先手
    private boolean firstHand = false;
    //存储所选择的卡牌，保存最初的卡牌选择
    private String selectedCard;
    //所持有的卡牌，用来进行关于当前出战角色的业务
    private final List<CharacterCard> characterCards = Collections.synchronizedList(new ArrayList<>());
    //存储所持有的功能卡牌
    private List<String> actionCards = new ArrayList<>();
    //元素反应附加卡
    private AddictionCard reactionCard;
    //保存每轮的骰子
    private List<String> dices = Collections.synchronizedList(new ArrayList<>());
    // 活跃角色实例（玩家专属）
    private CharacterCard activeCharacter;
    //上一个使用的角色
    private int  lastActiveCharacterId;
    //是否是快速行动
    private String switchType;
    //角色的召唤物
    private Map<CharacterCard,AddictionCard> addictionCards= new ConcurrentHashMap<>();
    //玩家级状态
    private final List<Status> statuses = new ArrayList<>();
    //玩家出战状态
    private final List<BattleStatus>battleStatuses = new ArrayList<>();
    public PlayerSession(String playerId, Channel playerChannel){
        this.playerId = playerId;
        this.playerChannel=playerChannel;
        this.loginTime = LocalDateTime.now();
        lastActiveTime = LocalDateTime.now();
    }
    public void setActiveCharacter(CharacterCard card){
        if (activeCharacter!=null)
        {
        this.lastActiveCharacterId = this.activeCharacter.getId();
        }
        this.activeCharacter = card;
        if (card != null) {
            characterCards.removeIf(c -> c.getName().equals(card.getName()));
            characterCards.add(0, card);
        }
    }
    //获取上一个使用的角色
    public CharacterCard getLastActiveCharacter(){
        for (CharacterCard card:characterCards){
            if (card.getId() == lastActiveCharacterId&&card.isAlive()){
                return card;
            }
        }
        return characterCards.stream()
                .filter(CharacterCard::isAlive)
                .findFirst()
                .orElse(null);
    }
    //通过附属卡ID找到角色卡牌
    public CharacterCard findByAdditionCardId(int cardId){
        return characterCards.stream()
                .filter(
                card ->
                    card.getSkills()
                            .stream()
                            .anyMatch(skill -> skill.getAddition_cardId() == cardId))
                .findFirst()
                .orElse(null);
    }
    //添加角色的召唤物
    public  void addAdditionCard(CharacterCard characterCard,AddictionCard addictionCard){
        // 检查角色是否已存在召唤物
        AddictionCard existingCard = addictionCards.get(characterCard);
        if (existingCard != null) {
            // 情况1：同一召唤物，刷新轮数
            if (existingCard.getName().equals(addictionCard.getName())) {
                existingCard.setTurn(addictionCard.getTurn());
                SendMessage.sendMessage(playerChannel,
                        characterCard.getName() + "的召唤物" + existingCard.getName() + "轮数已刷新为" + addictionCard.getTurn(),
                        Opcode.BROADCAST_OPCODE,
                       playerId);
            }
            // 情况2：不同召唤物，替换旧召唤物
            else {
                addictionCards.put(characterCard, addictionCard);
                SendMessage.sendMessage(playerChannel,
                        characterCard.getName() + "的召唤物" + existingCard.getName() + "已替换为" + addictionCard.getName(),
                        Opcode.BROADCAST_OPCODE,
                        playerId);
            }
        }
        // 情况3：首次添加召唤物
        else {
            addictionCards.put(characterCard, addictionCard);
            SendMessage.sendMessage(playerChannel,
                    characterCard.getName() + "获得召唤物：" + addictionCard.getName(),
                    Opcode.BROADCAST_OPCODE,
                    playerId);
        }
    }
    //移除角色的召唤物
    public  void removeAdditionCard(AddictionCard addictionCard){
        if (addictionCard == null) return;
        addictionCards.entrySet().removeIf(
                entry-> entry.getValue().getName().equals(addictionCard.getName())
        );
    }
}
