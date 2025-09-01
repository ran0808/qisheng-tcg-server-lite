package com.game.network.handler;

import com.game.network.gameSet.room.RoomManager;
import com.game.network.gameSet.status.BattleStatus;
import com.game.network.gameSet.card.CharacterCard;
import com.game.network.gameSet.dice.DiceService;
import com.game.network.gameSet.status.Status;
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

import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;

import static com.game.network.util.SendMessage.sendMessage;
@Component
@ChannelHandler.Sharable
public class CharacterSwitchHandler extends BaseHandler {
    @Autowired
    private GameStateManager gameStateManager;
    private DiceHandler diceHandler;
    @Autowired
    @Qualifier("cardExecutor")
    private ExecutorService cardExecutor;

    @Autowired
    public CharacterSwitchHandler(RoomManager roomManager) {
        super(roomManager);
    }

    // 主动换人方法
    public void handleActiveSwitchCharacter(ChannelHandlerContext ctx, GameProtocol protocol) {
        cardExecutor.execute(() -> {
            if (gameStateManager.isGameOver()) {
                return; // 游戏已结束，不处理任何操作
            }
            String playerId = String.valueOf(protocol.getPlayerId());
            Channel channel = ctx.channel();
            PlayerSession currentPlayer = getCurrentPlayer(playerId);
            String currentTurnPlayerId = gameStateManager.getCurrentTurnPlayerId();
            // 检查是否是当前回合
            if (!playerId.equals(currentTurnPlayerId)) {
                sendMessage(channel, "不是你的回合，无法行动", Opcode.ALERT_OPCODE, playerId);
                return;
            }

            // 检查是否有骰子可以消耗
            if (currentPlayer.getDices().isEmpty()) {
                sendMessage(channel, "没有可用骰子，无法执行换人操作", Opcode.ALERT_OPCODE, playerId);
                return;
            }

            // 生成可选角色列表（所有存活且非当前活跃角色）
            List<String> remainCards = currentPlayer.getCharacterCards().stream()
                    .filter(CharacterCard::isAlive)
                    .filter(card -> !card.equals(currentPlayer.getActiveCharacter()))
                    .map(CharacterCard::getName)
                    .toList();

            // 发送可选角色列表给客户端
            sendMessage(channel, "请选择以下角色卡牌(主动换人):" + remainCards,
                    Opcode.SEND_REMAINING_CARD_OPCODE, playerId);

            // 消耗一个骰子
            DiceService.consumeAnyDice(currentPlayer, 1);

            // 发送更新后的骰子状态
            diceHandler = new DiceHandler(roomManager);
            diceHandler.sendDiceSituation(currentPlayer, channel);
        });
    }

    // 完成新角色切换
    public void handleNewActiveCharacterSelection(ChannelHandlerContext ctx, GameProtocol protocol, boolean isQuickAction) {
        cardExecutor.execute(() -> {
            Channel channel = ctx.channel();
            String playerId = String.valueOf(protocol.getPlayerId());
            PlayerSession currentPlayer = getCurrentPlayer(playerId);
            PlayerSession oppositePlayer = getOppositePlayer(currentPlayer);
            GameStateManager.TurnState currentTurnState = gameStateManager.getCurrentTurnState();
            List<CharacterCard> remainingChars = currentPlayer.getCharacterCards().stream()
                    .filter(CharacterCard::isAlive) // 仅保留存活角色
                    .filter(card -> !card.equals(currentPlayer.getActiveCharacter())) // 排除当前活跃角色
                    .toList();
            // 解析客户端选择的角色名
            String newCharName = new String(protocol.getBody(), StandardCharsets.UTF_8).trim();
            CharacterCard selectedChar = remainingChars.stream()
                    .filter(card -> card.getName().equals(newCharName))
                    .findFirst()
                    .orElse(null);
            if (selectedChar == null) {
                List<String> validNames = remainingChars.stream()
                        .map(CharacterCard::getName)
                        .toList();
                sendMessage(ctx.channel(), "无效的角色选择，请从以下角色中重新选择：" + validNames,
                        Opcode.ALERT_OPCODE, playerId);
                return;
            }
            //进行状态转移
            CharacterCard oldActive = currentPlayer.getActiveCharacter();
            if (oldActive != null && oldActive.isAlive()) {
                //收集旧角色的可转移状态
                List<BattleStatus> battleStatuses = oldActive.getBattleStatues();
                Iterator<BattleStatus> battleStatusIterator = battleStatuses.iterator();
                while (battleStatusIterator.hasNext()) {
                    BattleStatus battleStatus = battleStatusIterator.next();
                    selectedChar.getBattleStatues().add(battleStatus);
                    //从旧角色移除该状态
                    battleStatusIterator.remove();
                }
            }
            currentPlayer.setActiveCharacter(selectedChar);
            // 广播角色切换信息
            String broadcastContent = "玩家" + playerId + "切换出战角色为：" + newCharName;
            broadcastBothSelections(broadcastContent, Opcode.BROADCAST_OPCODE, currentPlayer, oppositePlayer);
            if (currentTurnState == GameStateManager.TurnState.WAITING_FOR_SWITCH) {
                if (playerSessionManager.getTurnOver().size() == 1) {
                    gameStateManager.changeTurnState(GameStateManager.TurnState.ONE_PLAYER_ENDED);
                } else {
                    gameStateManager.changeTurnState(GameStateManager.TurnState.ACTION_PHASE);
                }
                gameStateManager.changeDefeatedTurn(currentPlayer, oppositePlayer);
            }
            // 判断是否有凯亚的大招状态
            Iterator<BattleStatus> battleStatusIterator = currentPlayer.getBattleStatuses().iterator();
            while (battleStatusIterator.hasNext()) {
                BattleStatus battleStatus = battleStatusIterator.next();
                if (battleStatus.getType() == BattleStatus.Type.SWITCH_DAMAGE_TRIGGER) {
                    int damage = battleStatus.getValue(); // 2点伤害
                    CharacterCard enemyTarget = oppositePlayer.getActiveCharacter();
                    enemyTarget.takeDamage(damage, selectedChar, null, currentPlayer, oppositePlayer);
                    enemyTarget.applyElementStatus(enemyTarget, new Status(Status.Type.ELEMENT_ATTACH, "冰"));
                    SendMessage.damageBroadcast(
                            currentPlayer,
                            oppositePlayer,
                            "凯亚大招切换伤害",
                            damage,
                            enemyTarget.getCurrentHp(),
                            enemyTarget.getName()
                    );

                    if (!enemyTarget.isAlive()) {
                        gameStateManager.handleCharacterDefeat(oppositePlayer, currentPlayer);
                        return;
                    }
                    battleStatus.setTimes(battleStatus.getTimes() - 1);
                    if (battleStatus.getTimes() <= 0) {
                        battleStatusIterator.remove();
                        SendMessage.sendMessage(
                                currentPlayer.getPlayerChannel(),
                                "凯亚大招效果已耗尽！",
                                Opcode.BROADCAST_OPCODE,
                                currentPlayer.getPlayerId()
                        );
                    }
                    break;
                }
            }
            // 发送角色状态更新
            String content1 = SendMessage.broadcastCard(currentPlayer);
            String content2 = SendMessage.broadcastCard(oppositePlayer);
            String content = content1 + content2;
            broadcastBothSelections(content, Opcode.BROADCAST_OPCODE, currentPlayer, oppositePlayer);

            // 根据回合状态决定行动权
            if (isQuickAction) {
                // 快速行动不切换行动权
                sendMessage(ctx.channel(), "请继续你的操作", Opcode.ATTACK_CONTINUE_OPCODE, playerId);
                sendMessage(oppositePlayer.getPlayerChannel(), "对方已切换角色，正在行动，请等待...",
                        Opcode.ALERT_OPCODE, oppositePlayer.getPlayerId());
            } else {
                // 主动换人或战斗中被击败换人
                if (currentTurnState == GameStateManager.TurnState.ACTION_PHASE) {
                    // 行动阶段换人，切换行动权
                    gameStateManager.changeCurrentTurnPlayer(oppositePlayer.getPlayerId());
                    sendMessage(oppositePlayer.getPlayerChannel(), "对方已切换角色，请你进行操作",
                            Opcode.ATTACK_CONTINUE_OPCODE, oppositePlayer.getPlayerId());
                    sendMessage(ctx.channel(), "你已切换角色，对方进行行动", Opcode.ALERT_OPCODE, playerId);
                } else if (currentTurnState == GameStateManager.TurnState.ONE_PLAYER_ENDED) {
                    // 一方已结束回合，换人不切换行动权
                    sendMessage(ctx.channel(), "已消耗一个骰子完成换人，你的回合可继续操作",
                            Opcode.ATTACK_CONTINUE_OPCODE, playerId);
                    sendMessage(oppositePlayer.getPlayerChannel(), "对方已换人，仍在其回合中，请等待",
                            Opcode.ALERT_OPCODE, oppositePlayer.getPlayerId());
                }
            }

            // 发送当前骰子状态，方便玩家决策
            diceHandler = new DiceHandler(roomManager);
            diceHandler.sendDiceSituation(currentPlayer, channel);

        });
    }
}