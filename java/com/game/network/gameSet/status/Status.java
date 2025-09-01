package com.game.network.gameSet.status;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

//记录临时效果
@Data
@AllArgsConstructor
public class Status {
    public boolean isExpired() {
        return remainingTurns <= 0;
    }

    public enum Type {ELEMENT_ATTACH, PETRIFY, FREEZE, TONGSAISHI, NORMAL_ATTACK_FIRE}

    private final Type type;
    private int value;//效果值
    private int remainingTurns;//剩余回合
    private String element;//元素类型

    //构造方法
    public Status(Type type, int value, int remainingTurns) {
        this(type, value, remainingTurns, null);
    }

    public Status(Type type, String element) {
        this.type = type;
        this.element = element;
        this.remainingTurns = -1;
    }

    @Override
    public String toString() {
        if (element != null) {
            return "element='" + element + '\'' +
                    ", type=" + type;
        }
        return "type=" + type + ", value=" + value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Status status = (Status) o;
        return Objects.equals(element, status.element) && Objects.equals(type, status.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(element, type);
    }

    public String getDescription() {
        if (Type.ELEMENT_ATTACH.equals(type)) {
            return element + "附着";
        } else if (Type.FREEZE.equals(type)) {
            return "冻结";
        } else if (Type.PETRIFY.equals(type)) {
            return "石化";
        } else if (Type.NORMAL_ATTACK_FIRE.equals(type))
        {
            return "火附魔";
        }else if (Type.TONGSAISHI.equals(type)){
            return "通塞识";
        }
        // 其他状态类型...
        return "";
    }
}