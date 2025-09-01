package com.game.network.codec;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import lombok.extern.slf4j.Slf4j;
import com.game.network.protocol.GameProtocol;
@Slf4j
public class GameProtocolDecoder extends LengthFieldBasedFrameDecoder {
    //协议头长度为16
    private static final int HEADER_LENGTH = 16;
    public GameProtocolDecoder(int maxFrameLength, int lengthFieldOffset, int lengthFieldLength) {
        super(maxFrameLength, lengthFieldOffset, lengthFieldLength);
    }
    public GameProtocolDecoder(){
        super(10*1024*1024,7,4,5,0);
    }
    @Override
    protected Object decode(ChannelHandlerContext ctx, ByteBuf in) throws Exception {
        //用父类方法获取完整帧
        ByteBuf frame = (ByteBuf) super.decode(ctx, in);
        if (frame==null){
            return null;//帧不完整，等待后续操作
        }
        try {
            //解析协议头部
            GameProtocol gameProtocol = new GameProtocol();
            //验证魔数
            int magic = frame.readInt();
            if (magic!=GameProtocol.MAGIC_NUMBER){
                return new IllegalArgumentException("协议校验失败,无效数据包"+magic);
            }
            //获取版本信息
            gameProtocol.setVersion(frame.readByte());
            //获取操作码
            gameProtocol.setOpcode(frame.readShort());
            //获取字节长度
            int length  = frame.readInt();
            gameProtocol.setLength(length);
            //获取校验和
            short checkSum = frame.readShort();
            gameProtocol.setChecksum(checkSum);
            //获取玩家ID
            short playerId = frame.readShort();
            gameProtocol.setPlayerId(playerId);
            //读指针后移一个字节
            frame.readByte();
            //获取正文数据
            if (length>0&& frame.readableBytes()>=length) {
                byte[] body = new byte[length] ;
                frame.readBytes(body);
                gameProtocol.setBody(body);
            }
            //校验数据完整性
            short calculatedCheckSum = gameProtocol.calculateCheckSum();
            if (calculatedCheckSum!=checkSum){
                return new IllegalArgumentException("数据校验失败");
            }
            log.debug("{}{}", gameProtocol);
            return gameProtocol;
        }
        finally
        {
            frame.release();
        }
    }
}

