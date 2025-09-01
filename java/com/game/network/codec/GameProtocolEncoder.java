package com.game.network.codec;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import com.game.network.protocol.GameProtocol;

public class GameProtocolEncoder extends MessageToByteEncoder<GameProtocol> {
    @Override
    protected void encode(ChannelHandlerContext channelHandlerContext, GameProtocol gameProtocol, ByteBuf out) throws Exception {
        //1.计算数据长度
        byte[] body = gameProtocol.getBody();
        int bodyLength = body != null ? body.length:0;
        gameProtocol.setLength(bodyLength);
        gameProtocol.setVersion((byte)0x01);
        //2.计算校验和
        short checkSum = gameProtocol.calculateCheckSum();
        gameProtocol.setChecksum(checkSum);
        //3.设置无效填充值,版本号
        gameProtocol.setInValid((byte) 0xff);
        //3.进行数据封装打包,写入协议内容
        out.writeInt(GameProtocol.MAGIC_NUMBER);
        out.writeByte(gameProtocol.getVersion());
        out.writeShort(gameProtocol.getOpcode());
        out.writeInt(gameProtocol.getLength());
        out.writeShort(gameProtocol.getChecksum());
        out.writeShort(gameProtocol.getPlayerId());
        out.writeByte(gameProtocol.getInValid());
        //写入数据体
        if (bodyLength>0&& body !=null)
        {
            out.writeBytes(body);
        }


    }
}
