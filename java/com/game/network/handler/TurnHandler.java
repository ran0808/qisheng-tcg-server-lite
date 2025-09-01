package com.game.network.handler;

import com.game.network.gameSet.room.Room;
import com.game.network.gameSet.room.RoomManager;
import com.game.network.gameSet.card.AddictionCard;
import com.game.network.gameSet.card.CharacterCard;
import com.game.network.gameSet.status.Status;
import com.game.service.BattleService;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import com.game.network.player.PlayerSession;
import com.game.network.protocol.GameProtocol;
import com.game.network.protocol.Opcode;
import com.game.network.util.SendMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;

import static com.game.network.util.SendMessage.sendMessage;
@Component
@ChannelHandler.Sharable
public class TurnHandler extends BaseHandler{
    @Autowired
    private  GameStateManager gameStateManager;
    @Autowired
    BattleService battleService;
    @Autowired
    @Qualifier("battleExecutor")
    private ExecutorService battleExecutor;
    @Autowired
    public TurnHandler(RoomManager roomManager) {
        super(roomManager);
    }

    public void handleEndTurn(ChannelHandlerContext ctx, GameProtocol protocol) {
        battleExecutor.execute(()->{
            if (gameStateManager.isGameOver()) {
                return; // 游戏已结束，不处理任何操作
            }
            String playerId = String.valueOf(protocol.getPlayerId());
            Channel channel = ctx.channel();
            PlayerSession currentPlayer = getCurrentPlayer(playerId);
            PlayerSession oppositePlayer = getOppositePlayer(currentPlayer);
            String opponentId = oppositePlayer.getPlayerId();
            Room currentRoom = roomManager.findRoomByPlayer(playerId);
            GameStateManager.TurnState currentTurnState = gameStateManager.getCurrentTurnState();
            int turn = gameStateManager.getTurn();

            if (currentTurnState== GameStateManager.TurnState.ONE_PLAYER_ENDED&&playerSessionManager.getTurnOver().get(0).equals(opponentId)) {
                // 对方宣布回合结束，则移除冻结，石化状态
                // 重置玩家的角色回合计数器
                handleStatusExpiration(currentPlayer, channel);
                handleStatusExpiration(oppositePlayer, oppositePlayer.getPlayerChannel());
            }
            // 更新回合状态
            if (currentTurnState == GameStateManager.TurnState.ACTION_PHASE) {
                gameStateManager.changeTurnState(GameStateManager.TurnState.ONE_PLAYER_ENDED);
                playerSessionManager.getTurnOver().add(0,playerId);
                // 切换行动权给对手
                gameStateManager.changeCurrentTurnPlayer(opponentId);
                sendMessage(oppositePlayer.getPlayerChannel(), "对方已结束回合，现在是你的回合",
                        Opcode.ATTACK_CONTINUE_OPCODE, oppositePlayer.getPlayerId());
                sendMessage(channel, "你已结束回合，等待对方操作", Opcode.ALERT_OPCODE, playerId);
            }
            else if (currentTurnState == GameStateManager.TurnState.ONE_PLAYER_ENDED) {
                gameStateManager.changeTurnState(GameStateManager.TurnState.BOTH_PLAYERS_ENDED);
                // 双方都结束回合，进行回合结算
                broadcastBothSelections("第" + turn + "回合结束，正在结算，请等待", Opcode.BROADCAST_OPCODE,currentPlayer,oppositePlayer);
                playerSessionManager.getTurnOver().add(1,playerId);
                // 处理回合结束时的卡牌效果
                if (currentPlayer.getAddictionCards() != null) {
                    for (AddictionCard addictionCard : currentPlayer.getAddictionCards().values()) {
                        addictionCard.onUse(currentPlayer, oppositePlayer, 0);
                        if (!oppositePlayer.getActiveCharacter().isAlive()) {
                            gameStateManager.handleCharacterDefeat(oppositePlayer, currentPlayer);
                            return;
                        }
                    }
                }
                if (currentPlayer.getReactionCard() != null) {
                    currentPlayer.getReactionCard().onUse(currentPlayer, oppositePlayer, 0);
                    if (!oppositePlayer.getActiveCharacter().isAlive()) {
                        gameStateManager.handleCharacterDefeat(oppositePlayer, currentPlayer);
                        return;
                    }
                }
                if (oppositePlayer.getAddictionCards() != null) {
                    for (AddictionCard addictionCard : oppositePlayer.getAddictionCards().values()) {
                        addictionCard.onUse(oppositePlayer, currentPlayer, 0);
                        if (!currentPlayer.getActiveCharacter().isAlive()) {
                            gameStateManager.handleCharacterDefeat(currentPlayer, oppositePlayer);
                            return;
                        }
                    }
                }
                if (oppositePlayer.getReactionCard() != null) {
                    oppositePlayer.getReactionCard().onUse(oppositePlayer, currentPlayer, 0);
                    if (!currentPlayer.getActiveCharacter().isAlive()) {
                        gameStateManager.handleCharacterDefeat(currentPlayer, oppositePlayer);
                        return;
                    }
                }
                if (turn == GameStateManager.getMaxTurns()) {
                    // 15回合结束，未分胜负，判定平局
                    gameStateManager.gameOverWithDraw(currentPlayer,oppositePlayer);
                    return; // 终止后续逻辑
                }
                gameStateManager.incrementTurn();
                battleService.incrementTurn(currentRoom.getRoomId(),gameStateManager.getTurn());
                // 进行状态更新
                String content1 = SendMessage.broadcastCard(currentPlayer);
                String content2 = SendMessage.broadcastCard(oppositePlayer);
                String content = content1 + content2;
                broadcastBothSelections(content, Opcode.BROADCAST_OPCODE,currentPlayer,oppositePlayer);

                // 记录先手信息
                String firstHandPlayerId = currentRoom.getFirstHandPlayerId();
                if (firstHandPlayerId == null) {
                    firstHandPlayerId = new Random().nextBoolean() ? playerId : opponentId;
                    currentRoom.setFirstHandPlayerId(firstHandPlayerId);
                }

                currentPlayer.setFirstHand(firstHandPlayerId.equals(playerId));
                oppositePlayer.setFirstHand(firstHandPlayerId.equals(opponentId));

                // 清除回合结束玩家
                gameStateManager.changeTurnState(GameStateManager.TurnState.ACTION_PHASE);
                // 重掷骰子
                DiceHandler diceHandler = new DiceHandler(roomManager);
                diceHandler.initDiceAndPromptReselect(currentPlayer,oppositePlayer);
            }
        });
    }
    private void handleStatusExpiration(PlayerSession player, Channel channel) {
        for (CharacterCard card : player.getCharacterCards()) {
            card.resetTurnCounter();
            if (card.isAlive()) {
                card.getStatuses().forEach(status -> {
                    if (status.getType() == Status.Type.PETRIFY || status.getType() == Status.Type.FREEZE) {
                        status.setRemainingTurns(status.getRemainingTurns() - 1);
                    }
                });
                // 收集并移除已过期的状态
                List<Status> expiredStatuses = card.getStatuses().stream()
                        .filter(s -> (s.getType() == Status.Type.PETRIFY || s.getType() == Status.Type.FREEZE) && s.isExpired())
                        .toList();
                card.getStatuses().removeAll(expiredStatuses);
                // 仅对已解除的状态发送消息
                expiredStatuses.forEach(s -> {
                    sendMessage(channel, card.getName() + s.getType().name() + "状态已解除",
                            Opcode.BROADCAST_OPCODE, player.getPlayerId());
                });
            }
        }
    }
}
