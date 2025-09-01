package com.game.dto;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

public enum BattleState {
    OVER(0, "已结束"),
    ON (1, "正在进行"),
    WITHDRAW(2,"平局"),
    ERROR(3,"异常结束");
    @Getter
    @EnumValue
    private final int code;
    private final String desc;
    BattleState(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
