package com.game.network.handler;

import com.game.network.gameSet.card.CharacterCard;
import com.game.network.gameSet.room.Room;
import com.game.network.gameSet.room.RoomManager;
import com.game.service.BattleService;
import io.netty.channel.ChannelHandler;
import lombok.Data;
import com.game.network.player.PlayerSession;
import com.game.network.protocol.Opcode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static com.game.network.util.SendMessage.sendMessage;
@Data
@Component
@ChannelHandler.Sharable
public class GameStateManager {
    @Autowired
    BattleService battleService;
    @Autowired
    RoomManager roomManager;
    public enum TurnState {
        ACTION_PHASE,       // 行动阶段，双方都可以行动
        ONE_PLAYER_ENDED,   // 一方已结束回合
        WAITING_FOR_SWITCH, BOTH_PLAYERS_ENDED  // 双方都已结束回合
    }
    private final AtomicReference<TurnState> currentTurnState = new AtomicReference<>(TurnState.ACTION_PHASE);
    private final AtomicReference<String> currentTurnPlayerId = new AtomicReference<>();
    private final AtomicInteger turn = new AtomicInteger(1);
    private final AtomicBoolean isGameOver = new AtomicBoolean(false);
    private static final int MAX_TURNS = 15;
    public void incrementTurn() {
        this.turn.incrementAndGet();
    }
    public int getTurn() {
        return turn.get();
    }
    public static int getMaxTurns() {
        return MAX_TURNS;
    }
    //实现平局逻辑
    public void gameOverWithDraw(PlayerSession currentPlayer,PlayerSession oppositePlayer) {
        Room room = roomManager.findRoomByPlayer(currentPlayer.getPlayerId());
        battleService.withDraw(room.getRoomId());
       setGameOver(true);
        sendMessage(
                currentPlayer.getPlayerChannel(),
                "15回合已结束，未分胜负，本局平局！",
                Opcode.GAME_OVER_OPCODE,
                currentPlayer.getPlayerId()
        );
        sendMessage(
                oppositePlayer.getPlayerChannel(),
                "15回合已结束，未分胜负，本局平局！",
                Opcode.GAME_OVER_OPCODE,
                oppositePlayer.getPlayerId()
        );
    }
    //实现游戏结束逻辑
    public void gameOver(PlayerSession winner, PlayerSession loser) {
        //记录
        Room room = roomManager.findRoomByPlayer(winner.getPlayerId());
        battleService.gameOver(loser.getPlayerId(), winner.getPlayerId(),room.getRoomId());
       setGameOver(true);
        sendMessage(
                winner.getPlayerChannel(),
                "恭喜你获胜！",
                Opcode.GAME_OVER_OPCODE,
                winner.getPlayerId()
        );
        sendMessage(
                loser.getPlayerChannel(),
                "很遗憾，你输了！",
                Opcode.GAME_OVER_OPCODE,
                loser.getPlayerId()
        );
    }
    //实现角色被击败逻辑
    public void handleCharacterDefeat(PlayerSession defeatedPlayer, PlayerSession winningPlayer) {
        CharacterCard defeatedChar = defeatedPlayer.getActiveCharacter();
        sendMessage(
                defeatedPlayer.getPlayerChannel(),
                "你的" + defeatedChar.getName() + "已被击败！",
                Opcode.BROADCAST_OPCODE,
                defeatedPlayer.getPlayerId()
        );
        sendMessage(
                winningPlayer.getPlayerChannel(),
                "你击败了对方的" + defeatedChar.getName() + "！",
                Opcode.BROADCAST_OPCODE,
                winningPlayer.getPlayerId()
        );
        // 检查是否还有可切换的角色
        List<CharacterCard> remainingChars = defeatedPlayer.getCharacterCards().stream()
                .filter(CharacterCard::isAlive)
                .toList();

        if (remainingChars.isEmpty()) {
            // 游戏结束，判定胜负
            gameOver(winningPlayer, defeatedPlayer);
        }
        else {
            // 提示切换角色
            List<String> names = remainingChars.stream()
                    .map(CharacterCard::getName)
                    .collect(Collectors.toList());
            String content = String.join(",", names);
            sendMessage(
                    defeatedPlayer.getPlayerChannel(),
                    "请从剩余角色中选择新出战角色：[" + content + "]",
                    Opcode.SEND_REMAINING_CARD_OPCODE,
                    defeatedPlayer.getPlayerId()
            );
            changeTurnState(TurnState.WAITING_FOR_SWITCH);
        }
    }

    public void changeDefeatedTurn(PlayerSession defeatedPlayer, PlayerSession winningPlayer) {
        if (getCurrentTurnState()!=TurnState.WAITING_FOR_SWITCH) {
            // 根据回合状态决定行动权
            if (getCurrentTurnState() == TurnState.ACTION_PHASE) {
                // 行动阶段，击败角色后切换行动权给被击败方
                changeCurrentTurnPlayer(defeatedPlayer.getPlayerId());
                sendMessage(defeatedPlayer.getPlayerChannel(), "请切换角色后继续行动",
                        Opcode.ATTACK_CONTINUE_OPCODE, defeatedPlayer.getPlayerId());
                sendMessage(winningPlayer.getPlayerChannel(), "对方角色被击败，请等待对方切换角色",
                        Opcode.ALERT_OPCODE, winningPlayer.getPlayerId());
            } else if (getCurrentTurnState() ==TurnState.ONE_PLAYER_ENDED) {
                // 一方已结束回合，击败角色后不切换行动权
                sendMessage(winningPlayer.getPlayerChannel(), "对方角色被击败，请继续你的操作",
                        Opcode.ATTACK_CONTINUE_OPCODE, winningPlayer.getPlayerId());
                sendMessage(defeatedPlayer.getPlayerChannel(), "你的角色被击败，请切换角色，对方继续行动",
                        Opcode.ALERT_OPCODE, defeatedPlayer.getPlayerId());
            }
        }
    }

    // 回合状态变更方法
    public  void changeTurnState(TurnState newState) {
        currentTurnState.set(newState);
    }
    // 当前玩家变更方法
    public void changeCurrentTurnPlayer(String playerId) {
        currentTurnPlayerId.set(playerId);
    }
    public TurnState getCurrentTurnState() {
        return currentTurnState.get();
    }

    public String getCurrentTurnPlayerId() {
        return currentTurnPlayerId.get();
    }

    public boolean isGameOver() {
        return isGameOver.get();
    }

    public void setGameOver(boolean gameOver) {
        isGameOver.set(gameOver);
    }

}
