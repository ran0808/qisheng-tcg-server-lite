package com.game.dto;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

//与数据库进行映射
@Data
@TableName("player")
public class PlayerDO {
    @TableId(type = IdType.ASSIGN_UUID)
    private String playerId;
    private String playerName;
    private String password;
    private LocalDateTime registrationTime;
    private LocalDateTime lastLoginTime;
    private PlayerStatus status;
    private String currentRoomId;
}
