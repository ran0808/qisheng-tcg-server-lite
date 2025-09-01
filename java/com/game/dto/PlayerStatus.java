package com.game.dto;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;


public enum PlayerStatus {
    EXITING(0, "离线"),
    LOGIN (1, "登录"),
    MATCHING(2,"匹配中" ),
    IN_ROOM(3, "在房间中"),
    PREPARE(4, "准备中"),
    FIGHTING(5,"战斗中");
    @Getter
    @EnumValue
    private final int code;
    private final String desc;

    PlayerStatus(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
