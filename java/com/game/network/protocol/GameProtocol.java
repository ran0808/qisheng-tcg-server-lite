package com.game.network.protocol;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 幻影协议结构（16字节头 + 变长体）
 * +------------+----------+----------+-----------+
 * | 魔数(4B)   | 版本(1B) | 操作码(2B)|字节长度(1B)/
 * +------------+----------+----------+-----------+
 * 字节长度(3B)|校验和(2B)| 玩家ID(2B) |invaild(1B)
 * +---------------------------------+-----------+
 * |             数据体(NB)                    |
 * +-------------------------------------------+
 */
@Data
@AllArgsConstructor
public class GameProtocol {
    //四个字节的魔数
    public static final int MAGIC_NUMBER = 0x504847;//'P','H','G'
    //一个字节的版本号
    private byte version;
    //两个字节的操作码 01:登录，02：退出登录，03：移动，04：技能释放
    private short opcode;
    //四个字节的数据长度
    private int length;
    //两个字节的校验和
    private short checksum;
    //两个字节的玩家ID
    private int playerId = 0;;
    //进行无效填充
    private  byte inValid ;
    //数据体
    private byte[] body;
    public GameProtocol(){}
    //构造协议
    public GameProtocol(String playerId,byte[] body){
        this.playerId = (int) Short.parseShort(playerId);
        this.body = body;
    }
    //计算校验和
    public short calculateCheckSum() {
        short checksum = 0;
        // 校验版本号
        checksum ^= getVersion();
        // 校验操作码
        checksum ^= getOpcode();
        // 校验数据长度
        checksum ^= (short) (getLength() & 0xFFFF);
        checksum ^= (short) ((getLength() >> 16) & 0xFFFF);
        // 校验玩家ID
        checksum ^= (short) (getPlayerId() & 0xFFFF);
        checksum ^= (short) ((getPlayerId() >> 16) & 0xFFFF);
        // 校验数据体
        if (getBody() != null) {
            for (byte b : getBody()) {
                checksum ^= b;
            }
        }

        return checksum;
    }
}
