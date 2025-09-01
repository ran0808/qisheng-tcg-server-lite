package com.game.network.handler;

import com.game.network.gameSet.dice.DiceService;
import com.game.network.gameSet.room.Room;
import com.game.network.gameSet.room.RoomManager;
import com.game.network.gameSet.card.ActionCard;
import com.game.network.gameSet.card.Card;
import com.game.service.CardService;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import com.game.network.player.PlayerSession;
import com.game.network.protocol.GameProtocol;
import com.game.network.protocol.Opcode;
import com.game.network.util.SendMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import static com.game.network.util.SendMessage.sendMessage;
@Component
@ChannelHandler.Sharable
public class DiceHandler extends BaseHandler {
    @Autowired
    private GameStateManager gameStateManager;
    @Autowired
    private CardService cardService;
    @Autowired
    public DiceHandler(RoomManager roomManager) {
        super(roomManager);
    }
    public void initDiceAndPromptReselect(PlayerSession currentPlayer, PlayerSession oppositePlayer) {
        currentPlayer.setDiceReselected(false);
        oppositePlayer.setDiceReselected(false);

        // 玩家1
        currentPlayer.setDices(DiceService.generateInitialDices(8,currentPlayer));
        String dices1 = String.join(",", currentPlayer.getDices());
        sendMessage(currentPlayer.getPlayerChannel(), "投掷阶段:" + "\n" + dices1, Opcode.DICE_RESELECT_PROMPT, currentPlayer.getPlayerId());

        // 玩家2
        oppositePlayer.setDices(DiceService.generateInitialDices( 8,oppositePlayer));
        String dices2 = String.join(",", oppositePlayer.getDices());
        sendMessage(oppositePlayer.getPlayerChannel(), "投掷阶段:" + "\n" + dices2, Opcode.DICE_RESELECT_PROMPT, oppositePlayer.getPlayerId());
    }

    public void handleDiceReselectSubmit(ChannelHandlerContext ctx, GameProtocol protocol) {
        try {
            String playerId = String.valueOf(protocol.getPlayerId());
            PlayerSession currentPlayer = getCurrentPlayer(playerId);
            PlayerSession oppositePlayer = getOppositePlayer(currentPlayer);

            if (currentPlayer.isDiceReselected()) {
                sendMessage(ctx.channel(), "已完成骰子重掷，无法再次操作", Opcode.ALERT_OPCODE, playerId);
                return;
            }

            if (protocol.getBody() == null) {
                String finalDiceStr = String.join(",", currentPlayer.getDices());
                sendMessage(ctx.channel(), "骰子重掷完成，最终骰子：" + finalDiceStr, Opcode.DICE_RESELECT_RESULT, playerId);
                currentPlayer.setDiceReselected(true);
                checkBothDiceReselected(currentPlayer, oppositePlayer);
                return;
            }

            String reselectStr = new String(protocol.getBody(), StandardCharsets.UTF_8).trim();
            List<Integer> reselectIndices = new ArrayList<>();

            if (!reselectStr.isEmpty()) {
                reselectIndices = Arrays.stream(reselectStr.split(","))
                        .map(String::trim)
                        .map(Integer::parseInt)
                        .toList();
            }

            // 执行重掷逻辑
            List<String> currentDices = currentPlayer.getDices();
            List<String> newDices = new ArrayList<>(currentDices);

            if (!reselectIndices.isEmpty()) {
                List<String> reRolled = DiceService.generateInitialDices(reselectIndices.size(),currentPlayer);
                for (int i = 0; i < reselectIndices.size(); i++) {
                    int idx = reselectIndices.get(i);
                    newDices.set(idx, reRolled.get(i));
                }
                newDices = DiceService.sort(newDices);
            }

            currentPlayer.setDices(newDices);
            currentPlayer.setDiceReselected(true);

            String finalDiceStr = String.join(",", newDices);
            sendMessage(ctx.channel(), "骰子重掷完成，最终骰子：" + finalDiceStr, Opcode.DICE_RESELECT_RESULT, playerId);
            checkBothDiceReselected(currentPlayer, oppositePlayer);
        } catch (Exception e) {
            System.err.println("骰子重掷处理错误: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void checkBothDiceReselected(PlayerSession currentPlayer, PlayerSession oppositePlayer) {
        if (currentPlayer.isDiceReselected() && oppositePlayer.isDiceReselected()) {
            determineFirstHand(currentPlayer, oppositePlayer);
            String content1 = SendMessage.broadcastCard(currentPlayer);
            String content2 = SendMessage.broadcastCard(oppositePlayer);
            String content = content1 + content2;

            broadcastBothSelections(content, Opcode.BROADCAST_OPCODE, currentPlayer, oppositePlayer);

            // 将功能卡牌发给玩家
            for (Card card :cardService.getCardLibrary().values()) {
                if (card instanceof ActionCard && currentPlayer.getActionCards().size() < 3) {
                    currentPlayer.getActionCards().add(card.getName());
                }
            }

            String action_content = currentPlayer.getActionCards().toString();
            sendMessage(currentPlayer.getPlayerChannel(), action_content, Opcode.SEND_ACTION_CARD_OPCODE, currentPlayer.getPlayerId());
            sendMessage(oppositePlayer.getPlayerChannel(), action_content, Opcode.SEND_ACTION_CARD_OPCODE, oppositePlayer.getPlayerId());
        }
    }

    private void determineFirstHand(PlayerSession currentPlayer, PlayerSession oppositePlayer) {
        String firstHandPlayerId;
        String secondHandPlayerId;

        // 获取房间的信息
        Room currentRoom = roomManager.findRoomByPlayer(currentPlayer.getPlayerId());

        if (currentRoom.getFirstHandPlayerId() == null) {
            firstHandPlayerId = new Random().nextBoolean() ? currentPlayer.getPlayerId() : oppositePlayer.getPlayerId();
        }
        else {
            firstHandPlayerId = playerSessionManager.getTurnOver().get(0);
            playerSessionManager.getTurnOver().clear();
        }

        gameStateManager.changeCurrentTurnPlayer(firstHandPlayerId);
        secondHandPlayerId = firstHandPlayerId.equals(currentPlayer.getPlayerId()) ? oppositePlayer.getPlayerId() : currentPlayer.getPlayerId();

        sendMessage(playerSessionManager.getByPlayerId(firstHandPlayerId).getPlayerChannel(),
                "你获得先手！", Opcode.FIRST_HAND_RESULT, firstHandPlayerId);
        sendMessage(playerSessionManager.getByPlayerId(secondHandPlayerId).getPlayerChannel(),
                "对方获得先手，你为后手", Opcode.FIRST_HAND_RESULT, secondHandPlayerId);

        currentPlayer.setFirstHand(firstHandPlayerId.equals(currentPlayer.getPlayerId()));
        oppositePlayer.setFirstHand(firstHandPlayerId.equals(oppositePlayer.getPlayerId()));
        currentRoom.setFirstHandPlayerId(firstHandPlayerId);
    }
}