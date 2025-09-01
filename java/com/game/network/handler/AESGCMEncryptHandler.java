package com.game.network.handler;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.List;
//处理出站数据加密
@ChannelHandler.Sharable
public class AESGCMEncryptHandler extends MessageToMessageEncoder<ByteBuf> {
    private static final int AES_KEY_SIZE = 256;
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;//认证标签长度
    private final SecretKeySpec secretKey;
    private static SecureRandom secureRandom = new SecureRandom();//生成安全的随机数组
    public AESGCMEncryptHandler(byte[] key){
        //检验密钥长度是否为32位
        if (key.length != AES_KEY_SIZE/8){
            throw new IllegalArgumentException("无效密钥长度，需要"+(AES_KEY_SIZE/8)+"字节，实际收到"+key.length);
        }
        this.secretKey = new SecretKeySpec(key,"AES");
    }

    @Override
    public void encode(ChannelHandlerContext channelHandlerContext, ByteBuf message, List<Object> out) throws Exception {
        //1.生成12位随机初始化向量
        byte[] iv = new byte[GCM_IV_LENGTH];
        secureRandom.nextBytes(iv);
        //2.准备加密器,获取一个使用AES，GCM模式，无填充的加密实例
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH,iv);
        cipher.init(Cipher.ENCRYPT_MODE,secretKey,gcmParameterSpec);
        //3.获取原始数据的字节数组
        byte[] plainBytes = getBytesFromByteBuf(message);
        //4.执行加密
        byte[] cipherText  = cipher.doFinal(plainBytes);
        //5.组合IV和密文
        ByteBuf encryptedBuf = combineIvAndCipherText(iv,cipherText);
        //6.添加到输出列表，传递给下一个处理器
        out.add(encryptedBuf);
    }

    private ByteBuf combineIvAndCipherText(byte[] iv, byte[] cipherText) {
        return Unpooled.wrappedBuffer(iv,cipherText);
    }

    //从ByteBuf安全获取字节数组
    private byte[] getBytesFromByteBuf(ByteBuf message) {
       if (message.hasArray()){
           //堆缓冲区--直接访问底层数据
           return extractHeapBuffer(message);
       }
       else {
           //直接缓冲区
           return extractDirectBuffer(message);
       }
    }
//堆内存：数据在JVM堆上分配，可以直接通过array()方法获取底层字节数组，但要注意readerIndex和arrayOffset。
    private byte[] extractHeapBuffer(ByteBuf message) {
        //获取读指针
        int readerIndex = message.readerIndex();
        //返回可读字节数
        int readableBytes = message.readableBytes();
        //读取数据的起始位置=数据偏移量+读指针
        int arrayOffset = message.arrayOffset()+readerIndex;
        return Arrays.copyOfRange(message.array(),arrayOffset,arrayOffset+readableBytes);
    }
//直接内存：数据在堆外内存，我们无法直接获取一个数组，必须将数据复制到堆内存中。
    private byte[] extractDirectBuffer(ByteBuf message) {
        byte[] bytes = new byte[message.readableBytes()];
        message.getBytes(message.readerIndex(),bytes);
        return bytes;
    }
}
