package com.game.network.handler;

import com.game.network.gameSet.room.RoomManager;
import com.game.network.gameSet.card.CharacterCard;
import com.game.network.gameSet.dice.DiceService;
import com.game.network.gameSet.skill.Skill;
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
import java.util.concurrent.ExecutorService;

import static com.game.network.util.SendMessage.sendMessage;
@Component
@ChannelHandler.Sharable
public class BattleHandler extends BaseHandler {
    @Autowired
    private GameStateManager gameStateManager;
    @Autowired
    public BattleHandler(RoomManager roomManager) {
        super(roomManager);
    }
    @Autowired
    @Qualifier("battleExecutor")
    private ExecutorService battleExecutor;
    //实现攻击处理逻辑
    public void handleAttack(ChannelHandlerContext ctx, GameProtocol protocol) {
        battleExecutor.execute(()->  {
            if (gameStateManager.isGameOver()) {
                return; // 游戏已结束，不处理任何操作
            }
            String playerId = String.valueOf(protocol.getPlayerId());
            Channel channel = ctx.channel();
            PlayerSession currentPlayer = getCurrentPlayer(playerId);
            PlayerSession oppositePlayer = getOppositePlayer(currentPlayer);
            String currentTurnPlayerId = gameStateManager.getCurrentTurnPlayerId();
            if (!playerId.equals(currentTurnPlayerId)) {
                sendMessage(ctx.channel(), "不是你的回合！", Opcode.ALERT_OPCODE, playerId);
                return;
            }

            int skillIndex = Integer.parseInt(new String(protocol.getBody(), StandardCharsets.UTF_8)) - 1; // 转为0基索引
            CharacterCard curCharCard = currentPlayer.getActiveCharacter();
            CharacterCard opoCharCard = oppositePlayer.getActiveCharacter();
            Skill skill = curCharCard.getSkills().get(skillIndex);

            if (!opoCharCard.isAlive()) {
               gameStateManager.handleCharacterDefeat(oppositePlayer, currentPlayer);
                return;
            }
            // 判断是否能执行技能（通过CharacterCard的onUse触发）
            if (curCharCard.hasStatus(Status.Type.FREEZE)) {
                SendMessage.sendMessage(
                        currentPlayer.getPlayerChannel(),
                        curCharCard.getName()+ "处于冻结状态，无法使用技能！",
                        Opcode.ATTACK_CONTINUE_OPCODE,
                        currentPlayer.getPlayerId()
                );
                return;
            }
            if (curCharCard.hasStatus(Status.Type.PETRIFY)) {
                SendMessage.sendMessage(
                        currentPlayer.getPlayerChannel(),
                        curCharCard.getName()+ "处于石化状态，无法使用技能！",
                        Opcode.ATTACK_CONTINUE_OPCODE,
                        currentPlayer.getPlayerId()
                );
                return;
            }
            if (DiceService.canCastSkill(currentPlayer, skill.getDiceCosts())) {
                if (skillIndex == 2 && curCharCard.canUseBurst()) {
                    curCharCard.onUse(currentPlayer, oppositePlayer, 2);
                } else if (skillIndex == 0 || skillIndex == 1) {
                    curCharCard.onUse(currentPlayer, oppositePlayer, skillIndex);
                } else {
                    sendMessage(channel, "充能不足，无法释放元素爆发，请重新选择战技",
                            Opcode.ATTACK_CONTINUE_OPCODE, playerId);
                    return;
                }
                if (gameStateManager.isGameOver()) {
                    return; // 游戏已结束，不处理任何操作
                }
                String content1 = SendMessage.broadcastCard(currentPlayer);
                String content2 = SendMessage.broadcastCard(oppositePlayer);
                String content = content1 + content2;
                broadcastBothSelections(content, Opcode.BROADCAST_OPCODE,currentPlayer,oppositePlayer);

                sendDiceSituation(currentPlayer,channel);

                // 根据回合状态决定下一步
                GameStateManager.TurnState currentTurnState = gameStateManager.getCurrentTurnState();
                if (currentTurnState == GameStateManager.TurnState.ACTION_PHASE) {
                    // 行动阶段，切换行动权
                    gameStateManager.changeCurrentTurnPlayer(oppositePlayer.getPlayerId());
                    sendMessage(currentPlayer.getPlayerChannel(), "请等待对方操作",
                            Opcode.ACTION_OPTION_OPCODE, playerId);
                    sendMessage(oppositePlayer.getPlayerChannel(), "该您进行操作",
                            Opcode.ATTACK_CONTINUE_OPCODE, oppositePlayer.getPlayerId());
                } else if (currentTurnState ==GameStateManager.TurnState.ONE_PLAYER_ENDED) {
                    // 一方已结束回合，不切换行动权
                    sendMessage(channel, "请您继续操作", Opcode.ATTACK_CONTINUE_OPCODE, playerId);
                }
            } else {
                sendMessage(channel, "骰子数量不满足技能释放条件，请重新选择技能,结束本回合或退出对局",
                        Opcode.ACTION_OPTION_OPCODE, playerId);
            }
        });
    }

}
