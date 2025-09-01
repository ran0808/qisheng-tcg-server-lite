package com.game.network.server;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import com.game.network.codec.GameProtocolDecoder;
import com.game.network.codec.GameProtocolEncoder;
import com.game.network.handler.*;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


import java.util.concurrent.TimeUnit;


//管道初始化
@Component
public class GatewayInitializer extends ChannelInitializer<SocketChannel> {
    @Autowired
    private LoginHandler loginHandler;
    @Autowired
    private LogoutHandler logoutHandler;
    @Autowired
    private AutoMatchServerHandler autoMatchServerHandler;
    @Autowired
    private AESGCMDecryptHandler aesGcmDecryptHandler;
    @Autowired
    private AESGCMEncryptHandler aesGcmEncryptHandler;
    @Autowired
    private GamePlayHandler gamePlayHandler;
    @Override
    protected void initChannel(SocketChannel socketChannel)  {
        ChannelPipeline pipeline = socketChannel.pipeline();
        pipeline.addLast(new IdleStateHandler(30, 0, 0, TimeUnit.MINUTES));
        //协议层
        pipeline.addLast("protocolDecoder",new GameProtocolDecoder());
        pipeline.addLast("protocolEncoder",new GameProtocolEncoder());
        pipeline.addLast(new ServerIdleStateHandler());
        //安全层
        pipeline.addLast("encryptHandler", aesGcmDecryptHandler);
        pipeline.addLast("decryptHandler",aesGcmEncryptHandler);
        //业务层
        pipeline.addLast("LoginHandler",loginHandler);
        pipeline.addLast("LogoutHandler",logoutHandler);
        pipeline.addLast("autoMatchServerHandler",autoMatchServerHandler);
        pipeline.addLast("gamePlayerHandler",gamePlayHandler);
    }
    public static class ServerIdleStateHandler extends ChannelInboundHandlerAdapter {
        @Override
        public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
            if (evt instanceof IdleStateEvent event) {
                if (event.state() == IdleState.READER_IDLE) {
                    // 长时间无操作，主动关闭连接
                    System.out.println("连接超时，关闭通道: " + ctx.channel());
                    ctx.close();
                }
            } else {
                super.userEventTriggered(ctx, evt);
            }
        }
}
}
