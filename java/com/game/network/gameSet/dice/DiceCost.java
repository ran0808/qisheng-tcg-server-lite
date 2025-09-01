package com.game.network.gameSet.dice;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Arrays;
import java.util.List;

//单个筛子消耗条件：包含元素类型和数量
@Data
@AllArgsConstructor
public class DiceCost {
    public static final String [] ALL_ELEMENTS = {"岩", "水", "雷", "风", "冰", "草", "火", "万能"};
    public static final List<String> SORT_ORDER = Arrays.asList("万能", "冰", "水", "火", "雷", "风", "岩", "草");
    private int count;
    private String element;
}
