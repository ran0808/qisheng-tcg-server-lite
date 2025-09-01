package com.game.network.handler;

import com.game.network.util.SendMessage;
import com.game.service.PlayerService;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import com.game.network.player.PlayerSessionManager;
import com.game.network.protocol.GameProtocol;
import com.game.network.protocol.Opcode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@ChannelHandler.Sharable
@Component
//处理用户主动退出操作
public class LogoutHandler extends ChannelInboundHandlerAdapter {
    @Autowired
    private PlayerSessionManager sessionManager;
    @Autowired
    PlayerService playerService;
    @Override
    public void channelRead(ChannelHandlerContext channelHandlerContext, Object msg) {
            if (msg instanceof GameProtocol protocol){
                if (protocol.getOpcode()== Opcode.LOGOUT_OPCODE){
                    HandleLogoutRequest(channelHandlerContext,protocol);
                }
                else {
                    channelHandlerContext.fireChannelRead(protocol);
                }
            }
            else {
                channelHandlerContext.fireChannelRead(msg);
            }

    }
    private void HandleLogoutRequest(ChannelHandlerContext ctx,GameProtocol protocol ) {
        String playerId =String.valueOf(protocol.getPlayerId());
        SendMessage.sendMessage(ctx.channel(),"退出登录成功",Opcode.LOGOUT_RESPONSE_OPCODE,playerId);
        //发送响应给客户端并关闭连接
        ctx.close();
    }
    //处理连接被动断开
    @Override
    public void channelInactive(ChannelHandlerContext channelHandlerContext){
       channelHandlerContext.fireChannelInactive();
    }
    @Override
    public void exceptionCaught(ChannelHandlerContext channelHandlerContext, Throwable throwable) {
        System.err.println("退出登录处理异常"+throwable.getMessage());
        channelHandlerContext.close();
    }
}
