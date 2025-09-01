package com.game.network.handler;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import com.game.network.protocol.GameProtocol;
import com.game.network.protocol.Opcode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutorService;

@Component
@ChannelHandler.Sharable
public class GamePlayHandler extends ChannelInboundHandlerAdapter {
    @Autowired
    @Qualifier("battleExecutor")
    private ExecutorService battleExecutor;
    @Autowired
    @Qualifier("cardExecutor")
    private ExecutorService cardExecutor;

    private final CardSelectionHandler cardSelectionHandler;
    private final DiceHandler diceHandler;
    private final BattleHandler battleHandler;
    private final TurnHandler turnHandler;
    private final CharacterSwitchHandler characterSwitchHandler;
    private final ActionCardHandler actionCardHandler;
    @Autowired
    public GamePlayHandler(CardSelectionHandler cardSelectionHandler,
                           DiceHandler diceHandler,
                           BattleHandler battleHandler,
                           TurnHandler turnHandler,
                           CharacterSwitchHandler characterSwitchHandler,
                           ActionCardHandler actionCardHandler) {
        this.cardSelectionHandler = cardSelectionHandler;
        this.diceHandler = diceHandler;
        this.battleHandler = battleHandler;
        this.turnHandler = turnHandler;
        this.characterSwitchHandler = characterSwitchHandler;
        this.actionCardHandler = actionCardHandler;

    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (msg instanceof GameProtocol protocol) {
            // 根据操作码路由到不同的处理器
            switch (protocol.getOpcode()) {
                case Opcode.SELECT_CARD_OPCODE:
                    cardExecutor.execute(() -> cardSelectionHandler.handleCardSelection(ctx, protocol));
                    break;
                case Opcode.SELECT_CARD_ON_OPCODE:
                    cardExecutor.execute(()->cardSelectionHandler.handlePlayOnSelection(ctx, protocol));
                    break;
                case Opcode.DICE_RESELECT_SUBMIT:
                    diceHandler.handleDiceReselectSubmit(ctx, protocol);
                    break;
                case Opcode.ATTACK_OPCODE:
                    battleExecutor.execute(() -> battleHandler.handleAttack(ctx, protocol));
                    break;
                case Opcode.TURN_END:
                    battleExecutor.execute(()->turnHandler.handleEndTurn(ctx, protocol));
                    break;
                case Opcode.SELECT_NEW_ACTIVE_CHARACTER_OPCODE:
                   cardExecutor.execute(()-> characterSwitchHandler.handleNewActiveCharacterSelection(ctx, protocol, false));
                    break;
                case Opcode.SELECT_NEW_ACTIVE_CHARACTER_QUICK_OPCODE:
                    cardExecutor.execute(()->  characterSwitchHandler.handleNewActiveCharacterSelection(ctx, protocol, true));
                    break;
                case Opcode.ACTION_CARD_OPCODE:
                    cardExecutor.execute(()->actionCardHandler.handleActionCard(ctx, protocol));
                    break;
                case Opcode.SWITCH_CHARACTER_OPCODE:
                   cardExecutor.execute(()-> characterSwitchHandler.handleActiveSwitchCharacter(ctx, protocol));
                    break;
                default:
                    ctx.fireChannelRead(protocol);
                    break;
            }
        } else {
            ctx.fireChannelRead(msg);
        }
    }
}
