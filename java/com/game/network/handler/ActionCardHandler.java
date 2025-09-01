package com.game.network.handler;

import com.game.network.gameSet.room.RoomManager;
import com.game.network.gameSet.card.ActionCard;
import com.game.network.gameSet.card.CharacterCard;
import com.game.service.CardService;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import com.game.network.player.PlayerSession;
import com.game.network.protocol.GameProtocol;
import com.game.network.protocol.Opcode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.game.network.util.SendMessage.sendMessage;
@Component
@ChannelHandler.Sharable
public class ActionCardHandler extends BaseHandler{
    @Autowired
    public ActionCardHandler(RoomManager roomManager){
        super(roomManager);
    }
    @Autowired
    private CardService cardService;
    @Autowired
    @Qualifier("cardExecutor")
    private ExecutorService cardExecutor;
    public void handleActionCard(ChannelHandlerContext ctx, GameProtocol protocol) {
       cardExecutor.execute(()->{
           Channel channel = ctx.channel();
           String playerId = String.valueOf(protocol.getPlayerId());
           PlayerSession currentPlayer = getCurrentPlayer(playerId);
           PlayerSession oppositePlayer = getOppositePlayer(currentPlayer);
           if (protocol.getBody() == null) {
               sendMessage(channel, "所选功能卡不存在，请重新输入", Opcode.ALERT_OPCODE, playerId);
               return;
           }

           String actionCardNames = new String(protocol.getBody(), StandardCharsets.UTF_8);
           List<String> actionCards = Arrays.stream(actionCardNames.split(",")).toList();

           for (String actionCard : actionCards) {
               ActionCard card = (ActionCard) cardService.getCardByName(actionCard);
               if (card == null) {
                   sendMessage(channel, "功能卡不存在" + actionCard, Opcode.ALERT_OPCODE, playerId);
                   continue;
               }

               boolean useSuccess = card.onUse(currentPlayer, oppositePlayer, 0);
               if (useSuccess) {
                   currentPlayer.getActionCards().remove(actionCard);
               } else {
                   sendMessage(channel, "功能卡使用失败", Opcode.ALERT_OPCODE, playerId);
               }

               if (card.getActionType() == ActionCard.ActionType.QUICKLY_ACTION) {
                   List<String> remainCards = currentPlayer.getCharacterCards().stream()
                           .filter(CharacterCard::isAlive) // 仅保留存活角色
                           .filter(card1 -> !card1.equals(currentPlayer.getActiveCharacter())) // 排除当前活跃角色
                           .map(CharacterCard::getName)
                           .toList();
                   sendMessage(channel, "选择你想切换的角色(快速行动)" + remainCards, Opcode.SEND_REMAINING_CARD_OPCODE, playerId);
                   return;
               }
           }

           String actionMsg = currentPlayer.getActionCards().isEmpty() ?
                   "您的行动卡为空" : "您的行动卡为：" + currentPlayer.getActionCards();
           sendMessage(channel, actionMsg, Opcode.ATTACK_CONTINUE_OPCODE, playerId);
       });
    }
}
