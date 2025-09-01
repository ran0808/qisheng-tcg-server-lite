package com.game.network.server;

import com.game.network.player.PlayerSessionManager;
import com.game.service.PlayerService;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.Collection;

@Component
public class GatewayServer {
        private EventLoopGroup bossGroup;
        private EventLoopGroup workerGroup;
        private Channel serverChannel;
        @Autowired
        GatewayInitializer gatewayInitializer;
        @Autowired
        PlayerSessionManager playerSessionManager;
        @Autowired
        PlayerService playerService;
        @PostConstruct
        public void start() throws Exception{
            playerService.resetAllPlayersToOffline();
             bossGroup = new NioEventLoopGroup(1);
             workerGroup = new NioEventLoopGroup();
             ServerBootstrap serverBootstrap = new ServerBootstrap()
                    .group(bossGroup,workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(gatewayInitializer);//核心初始化

             ChannelFuture channelFuture = serverBootstrap.bind(8087).sync();
             serverChannel = channelFuture.channel();
             System.out.println("Netty服务器启动，端口：8087");
             registerShutdownHook();
        }
        private void registerShutdownHook(){
            Runtime.getRuntime().addShutdownHook(new Thread(()->{
                System.out.println("服务器开始优雅关闭");
                if (serverChannel!=null&&serverChannel.isActive()){
                    serverChannel.close().syncUninterruptibly();
                    System.out.println("服务器监听通道已关闭");
                }
                Collection<Channel> allChannels = playerSessionManager.getAllChannels();
                for (Channel channel : allChannels) {
                    if (channel.isActive()){
                        channel.close().addListener(
                                future -> {
                                    if (future.isSuccess()){
                                        System.out.println("客户端通道关闭成功: " + channel.remoteAddress());
                                    }
                                    else {
                                        System.err.println("客户端通道关闭失败: " + future.cause());
                                    }
                                }
                        );
                    }
                }
                bossGroup.shutdownGracefully().syncUninterruptibly();
                workerGroup.shutdownGracefully().syncUninterruptibly();
                System.out.println("Netty线程组已关闭，服务器优雅关闭完成");
            }, "ShutdownHook-Thread"));
        }
        @PreDestroy
        public void shutdown() {
            if (serverChannel != null) {
                serverChannel.close();
            }
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
            System.out.println("Netty 服务器关闭");
        }

}
