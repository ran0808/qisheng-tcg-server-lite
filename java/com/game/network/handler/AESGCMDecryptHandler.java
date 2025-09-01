package com.game.network.handler;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.util.List;
@ChannelHandler.Sharable
//对客户端传输的数据进行数据解密
public class AESGCMDecryptHandler extends MessageToMessageDecoder<ByteBuf> {
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;
    private final SecretKeySpec secretKey;
    public AESGCMDecryptHandler(byte[] key) {
        this.secretKey = new SecretKeySpec(key, "AES"); // 实例化当前处理器的密钥
    }

    @Override
    protected void decode(ChannelHandlerContext channelHandlerContext, ByteBuf encryptedData, List out) throws Exception {
        //1.检查最小数据长度（IV+最小密文）
        if (encryptedData.readableBytes()<GCM_IV_LENGTH+16){
            //数据不足，等待更多数据
            return;
        }
        //2.提取IV
        byte[] iv =new byte[GCM_IV_LENGTH];
        encryptedData.readBytes(iv);//读取并移动读指针
        //3.准备解密器
        Cipher cipher =Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH,iv);
        cipher.init(Cipher.DECRYPT_MODE,secretKey,gcmParameterSpec);
        //4.获取加密数据
        byte[] cipherText =  new byte[encryptedData.readableBytes()];
        encryptedData.readBytes(cipherText);
        try {
            //5.执行解密
            byte[] plainText = cipher.doFinal(cipherText);
            //6.将解密数据传递到下一个处理器
            out.add(Unpooled.wrappedBuffer(plainText));
        } catch (Exception e) {
          //7.处理解密失败
            System.out.println("解密失败");
           e.printStackTrace();
        }
    }

}
