package com.game.network.gameSet.status;

import lombok.Data;

@Data
public class BattleStatus {


    public enum Type {SHIELD_CRYSTALLIZE, SHIELD_SKILL, SWITCH_DAMAGE_TRIGGER}

    //使用次数
    private int times;
    private Type type;
    //护盾值或者伤害值
    private int value;
    public BattleStatus(Type type, int value, int times) {
        this.type = type;
        this.value = value;
        this.times = times;
    }
    public String getDescription() {
        if (Type.SHIELD_SKILL.equals(type)) {
            return "释放战技获得" + value + "点护盾";
        } else if (Type.SHIELD_CRYSTALLIZE.equals(type)) {
            return "结晶反应获得" + value + "点护盾";
        } else if (Type.SWITCH_DAMAGE_TRIGGER.equals(type)) {
            return "寒冰之棱";
        }
        return "";
    }
}