package com.game.dto;

public enum ElementType {
    ROCK("岩"), WATER("水"), FIRE("火"), THUNDER("雷"), WIND("风"), ICE("冰"), GRASS("草");

    private final String desc;
    ElementType(String desc) { this.desc = desc; }
}