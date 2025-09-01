package com.game.network.handler;

import com.game.dto.PlayerStatus;
import com.game.network.gameSet.battle.ElementReaction;
import com.game.network.gameSet.room.Room;
import com.game.network.gameSet.room.RoomManager;
import com.game.network.gameSet.card.Card;
import com.game.network.gameSet.card.CharacterCard;
import com.game.network.gameSet.skill.Skill;
import com.game.service.BattleService;
import com.game.service.CardService;
import com.game.service.PlayerService;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import com.game.network.player.PlayerSession;
import com.game.network.protocol.GameProtocol;
import com.game.network.protocol.Opcode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

import static com.game.network.util.SendMessage.sendMessage;
@Component
@ChannelHandler.Sharable
public class CardSelectionHandler extends BaseHandler {
    private static final ConcurrentHashMap<String, Boolean> selectionStatus = new ConcurrentHashMap<>();
    @Autowired
    private BattleService battleService;
    @Autowired
    private GameStateManager gameStateManager;
    @Autowired
    private PlayerService playerService;
    @Autowired
    private RoomManager roomManager;
    @Autowired
    private CardService cardService;
    @Autowired
    private ElementReaction elementReaction;
    @Autowired
    public CardSelectionHandler(RoomManager roomManager) {
        super(roomManager);
    }
    @Autowired
    @Qualifier("cardExecutor")
    private ExecutorService cardExecutor;
    public void handleCardSelection(ChannelHandlerContext ctx, GameProtocol protocol) {
       cardExecutor.execute(()-> {
           try {
               String playerId = String.valueOf(protocol.getPlayerId());
               PlayerSession currentPlayer = getCurrentPlayer(playerId);
               PlayerSession oppositePlayer = getOppositePlayer(currentPlayer);
               Room room = roomManager.findRoomByPlayer(playerId);
               String currentCard = new String(protocol.getBody(), StandardCharsets.UTF_8);
               currentPlayer.getCharacterCards().clear();

               for (String cardName : currentCard.split(",")) {
                   cardName = cardName.trim();
                   Card card = cardService.getCardByName(cardName);
                   if (card instanceof CharacterCard templateCard) {
                       CharacterCard playerCard = new CharacterCard(
                               templateCard.getId(),
                               templateCard.getName(),
                               templateCard.getElement(),
                               templateCard.getMaxHp(),
                               templateCard.getEnergyNeeded(), gameStateManager, playerSessionManager, elementReaction
                       );
                       for (Skill skill : templateCard.getSkills()) {
                           playerCard.addSkill(skill);
                       }
                       currentPlayer.getCharacterCards().add(playerCard);
                   } else {
                       sendMessage(ctx.channel(), "无效的角色卡牌：" + cardName, Opcode.ALERT_OPCODE, playerId);
                   }
               }

               currentPlayer.setSelectedCard(currentCard);
               selectionStatus.put(playerId, true);

               // 检查双方卡牌是否选择完毕
               boolean bothSelected = selectionStatus.getOrDefault(playerId, false)
                       && selectionStatus.getOrDefault(oppositePlayer.getPlayerId(), false);

               if (bothSelected) {
                   String playerSelection = "玩家" + playerId + "选择: [" + currentPlayer.getSelectedCard() + "]";
                   String opponentSelection = "玩家" + oppositePlayer.getPlayerId() + "选择: [" + oppositePlayer.getSelectedCard() + "]";
                   String broadcastContent = playerSelection + "\n" + opponentSelection;
                   //修改对局卡牌
                   List<Integer> ourCardIds = currentPlayer.getCharacterCards().stream()
                           .map(CharacterCard::getId) // 获取卡牌ID
                           .collect(Collectors.toList());
                   battleService.updateCards(playerId, oppositePlayer.getPlayerId(), room.getRoomId(), ourCardIds);
                   // 保存敌方卡牌到数据库
                   List<Integer> enemyCardIds = oppositePlayer.getCharacterCards().stream()
                           .map(CharacterCard::getId)
                           .collect(Collectors.toList());
                   battleService.incrementTurn(room.getRoomId(), 1);
                   battleService.updateCards(oppositePlayer.getPlayerId(), playerId, room.getRoomId(), enemyCardIds);
                   broadcastBothSelections(broadcastContent, Opcode.BROADCAST_OPCODE, currentPlayer, oppositePlayer);
                   broadcastBothSelections("请选择您的出战角色:", Opcode.SELECT_CARD_ON_OPCODE, currentPlayer, oppositePlayer);
                   selectionStatus.remove(playerId);
                   selectionStatus.remove(oppositePlayer.getPlayerId());
               }
           } catch (Exception e) {
               // 添加错误处理
               System.err.println("卡牌选择处理错误: " + e.getMessage());
               e.printStackTrace();
           }
       });
    }

    public void handlePlayOnSelection(ChannelHandlerContext ctx, GameProtocol protocol) {
        try {
            String playerId = String.valueOf(protocol.getPlayerId());
            PlayerSession currentPlayer = getCurrentPlayer(playerId);
            PlayerSession oppositePlayer = getOppositePlayer(currentPlayer);

            String playOnCardName = new String(protocol.getBody(), StandardCharsets.UTF_8);
            CharacterCard playerCard = currentPlayer.getCharacterCards().stream().filter(characterCard -> characterCard.getName().equals(playOnCardName))
                    .findFirst()
                    .orElse(null);

            if (playerCard == null) {
                sendMessage(ctx.channel(), "当前所选卡牌不存在", Opcode.ALERT_OPCODE, playerId);
                return;
            }

            currentPlayer.setActiveCharacter(playerCard);
            selectionStatus.put(playerId, true);
            // 检查双方出战卡牌是否选择完毕
            boolean bothSelected = selectionStatus.getOrDefault(playerId, false)
                    && selectionStatus.getOrDefault(oppositePlayer.getPlayerId(), false);

            if (bothSelected) {
                String playerSelection = "玩家" + playerId + "选择: [" + playOnCardName + "]作为出战卡牌";
                String oppositeSelection = "玩家" + oppositePlayer.getPlayerId() + "选择: [" + oppositePlayer.getActiveCharacter().getName() + "]作为出战卡牌";
                String broadcastContent = playerSelection + "\n" + oppositeSelection;
                broadcastBothSelections(broadcastContent, Opcode.BROADCAST_OPCODE, currentPlayer, oppositePlayer);
                playerService.updateStatus(playerId, PlayerStatus.FIGHTING);
                playerService.updateStatus(oppositePlayer.getPlayerId(),PlayerStatus.FIGHTING);
                // 生成初始骰子序列
                DiceHandler diceHandler = new DiceHandler(roomManager);
                diceHandler.initDiceAndPromptReselect(currentPlayer, oppositePlayer);

                selectionStatus.remove(playerId);
                selectionStatus.remove(oppositePlayer.getPlayerId());
                gameStateManager.changeTurnState(GameStateManager.TurnState.ACTION_PHASE);
            } else {
                sendMessage(ctx.channel(), "请等待对方选择出战卡牌", Opcode.ALERT_OPCODE, playerId);
            }
        } catch (Exception e) {
            System.err.println("出战卡牌选择处理错误: " + e.getMessage());
            e.printStackTrace();
        }
    }
}