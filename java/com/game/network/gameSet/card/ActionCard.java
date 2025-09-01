package com.game.network.gameSet.card;

import com.game.network.gameSet.dice.DiceCost;
import lombok.Getter;
import com.game.network.gameSet.dice.DiceService;
import com.game.network.player.PlayerSession;
import com.game.network.protocol.Opcode;
import com.game.network.util.SendMessage;

import java.util.List;
//作用在当前玩家身上
public class ActionCard extends Card{
    public enum ActionType{HEAL,DICE_CHANGE,QUICKLY_ACTION}//行动卡类型
    @Getter
    private final ActionType actionType;//功能类型
    @Getter
    private final List<DiceCost> diceCost;//所需筛子类型
    private final int effectValue;//功能参数

    public ActionCard(int id, String name,ActionType actionType,List diceCosts,int effectValue) {
        super(id, name);
        this.actionType = actionType;
        this.diceCost = diceCosts;
        this.effectValue = effectValue;
    }
    //行动卡被使用时对应的职能
    @Override
    public boolean onUse(PlayerSession currentPlayer, PlayerSession oppositePlayer, int i) {
        CharacterCard currentCard = currentPlayer.getActiveCharacter();
        // 检查骰子是否满足
        if (diceCost!=null&&!DiceService.canCastSkill(currentPlayer, diceCost)) {
            SendMessage.sendMessage(
                    currentPlayer.getPlayerChannel(),
                    "骰子不足，无法使用" + getName(),
                    Opcode.ALERT_OPCODE,
                    currentPlayer.getPlayerId()
            );
            return false;
        }
        // 消耗骰子
        if (diceCost!=null)
         DiceService.consumeDice(currentPlayer, diceCost);
        // 执行行动效果
        switch (actionType) {
            case HEAL:
                currentCard.heal(effectValue);
                SendMessage.sendMessage(
                        currentPlayer.getPlayerChannel(),
                        "使用" + getName() + "，" + currentCard.getName() + "恢复" + effectValue + "点生命值,当前生命为"+currentCard.getCurrentHp(),
                        Opcode.BROADCAST_OPCODE,
                        currentPlayer.getPlayerId()
                );
                break;
            case DICE_CHANGE:
                for (int i1 = 0; i1 < effectValue; i1++) {
                    currentPlayer.getDices().add("万能");
                }
                SendMessage.sendMessage(
                        currentPlayer.getPlayerChannel(),
                        "使用" + getName() + "，获得新骰子：" + currentPlayer.getDices(),
                        Opcode.BROADCAST_OPCODE,
                        currentPlayer.getPlayerId()
                );
                break;
            case QUICKLY_ACTION:
                // 快速行动不结束回合
                SendMessage.sendMessage(
                        currentPlayer.getPlayerChannel(),
                        "使用" + getName() + "切换角色后，可继续行动",
                        Opcode.BROADCAST_OPCODE,
                        currentPlayer.getPlayerId()
                );
                break;
        }
        return true;
    }
}
