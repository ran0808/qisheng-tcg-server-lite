package com.game.network.gameSet.dice;

import com.game.network.gameSet.card.CharacterCard;
import com.game.network.player.PlayerSession;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class DiceService {
    private static final Map<String,String> CHARACTER_TO_ELEMENT= new HashMap<>();
    static {
        CHARACTER_TO_ELEMENT.put("芭芭拉","水");
        CHARACTER_TO_ELEMENT.put("钟离","岩");
        CHARACTER_TO_ELEMENT.put("迪卢克","火");
        CHARACTER_TO_ELEMENT.put("凯亚","冰");
        CHARACTER_TO_ELEMENT.put("砂糖","风");
        CHARACTER_TO_ELEMENT.put("缇纳里","草");
        CHARACTER_TO_ELEMENT.put("菲谢尔","雷");
    }
    static AtomicInteger wildCount = new AtomicInteger();
    private static String getCurrentCharacterElement(PlayerSession playerSession) {
        return playerSession.getActiveCharacter().getElement(); // 示例方法
    }

    public static boolean canCastSkill(PlayerSession playerSession, List<DiceCost> diceCosts) {
        List<String> dices = playerSession.getDices();
        if (dices == null || dices.isEmpty()) {
            return false;
        }
        int totalRequired = 0;
        int anyRequired = 0;
        Map<String, Integer> fixedCosts = new HashMap<>();
        for (DiceCost cost : diceCosts) {
            int count = cost.getCount();
            totalRequired += count;
            if ("任意".equals(cost.getElement())) {
                anyRequired += count;
            } else {
                fixedCosts.put(cost.getElement(), fixedCosts.getOrDefault(cost.getElement(), 0) + count);
            }
        }
        if (totalRequired > dices.size()) {
            return false;
        }
        Map<String, Integer> elementCount = getElementCount(dices);
        int remainingWild = wildCount.get();
        int fixedTotal = 0;
        for (Map.Entry<String, Integer> fixed : fixedCosts.entrySet()) {
            String element = fixed.getKey();
            int required = fixed.getValue();
            int existingNormal = elementCount.getOrDefault(element, 0);
            int needWild = Math.max(0, required - existingNormal);
            if (needWild > remainingWild) {
                return false;
            }
            remainingWild -= needWild;
            fixedTotal += required;
        }
        int remainingDices = dices.size() - fixedTotal;
        return remainingDices >= anyRequired;
    }

    public static void consumeDice(PlayerSession playerSession, List<DiceCost> diceCosts) {
        List<String> dices = new ArrayList<>(playerSession.getDices());
        if (dices.isEmpty()) {
            return;
        }
        String currentElement = getCurrentCharacterElement(playerSession);
        int anyNeed = 0;
        Map<String, Integer> fixedNeed = new HashMap<>();

        for (DiceCost cost : diceCosts) {
            if ("任意".equals(cost.getElement())) {
                anyNeed += cost.getCount();
            } else {
                fixedNeed.put(cost.getElement(), fixedNeed.getOrDefault(cost.getElement(), 0) + cost.getCount());
            }
        }

        // 1. 消耗固定需求：非当前元素 → 当前元素 → 万能
        for (Map.Entry<String, Integer> entry : new HashMap<>(fixedNeed).entrySet()) {
            String targetElement = entry.getKey();
            int need = entry.getValue();
            if (need <= 0) continue;

            // 先消耗非当前元素的目标骰子
            int nonCurrentConsumed = consumeSpecificDice(dices, targetElement, need, currentElement, false);
            need -= nonCurrentConsumed;

            // 再消耗当前元素的目标骰子
            if (need > 0) {
                int currentConsumed = consumeSpecificDice(dices, targetElement, need, currentElement, true);
                need -= currentConsumed;
            }

            // 最后用万能骰子补充
            if (need > 0) {
                consumeSpecificDice(dices, "万能", need, currentElement, false);
            }
        }

        // 2. 消耗任意需求：调整为 非当前普通 → 当前普通 → 万能 的固定顺序
        if (anyNeed > 0) {
            // 分类骰子：非当前普通/当前普通/万能
            List<String> nonCurrentNormal = new ArrayList<>();
            List<String> currentNormal = new ArrayList<>();
            List<String> wilds = new ArrayList<>();
            for (String dice : dices) {
                if ("万能".equals(dice)) {
                    wilds.add(dice);
                } else if (dice.equals(currentElement)) {
                    currentNormal.add(dice);
                } else {
                    nonCurrentNormal.add(dice);
                }
            }

            // 2.1 先消耗非当前普通骰子
            int takeNonCurrent = Math.min(anyNeed, nonCurrentNormal.size());
            if (takeNonCurrent > 0) {
                removeDices(dices, nonCurrentNormal.subList(0, takeNonCurrent));
                anyNeed -= takeNonCurrent;
            }

            // 2.2 再消耗当前普通骰子
            if (anyNeed > 0) {
                int takeCurrent = Math.min(anyNeed, currentNormal.size());
                if (takeCurrent > 0) {
                    removeDices(dices, currentNormal.subList(0, takeCurrent));
                    anyNeed -= takeCurrent;
                }
            }

            // 2.3 最后消耗万能骰子
            if (anyNeed > 0) {
                int takeWild = Math.min(anyNeed, wilds.size());
                if (takeWild > 0) {
                    removeDices(dices, wilds.subList(0, takeWild));
                    anyNeed -= takeWild;
                }
            }
        }

        playerSession.setDices(dices);
    }

    // 移除指定的骰子列表
    private static void removeDices(List<String> dices, List<String> toRemove) {
        Map<String, Integer> removeCount = new HashMap<>();
        for (String dice : toRemove) {
            removeCount.put(dice, removeCount.getOrDefault(dice, 0) + 1);
        }
        Iterator<String> iterator = dices.iterator();
        while (iterator.hasNext()) {
            String dice = iterator.next();
            int count = removeCount.getOrDefault(dice, 0);
            if (count > 0) {
                iterator.remove();
                removeCount.put(dice, count - 1);
            }
        }
    }

    private static int consumeSpecificDice(List<String> dices, String targetElement, int need, String currentElement, boolean isCurrent) {
        // 保持原有逻辑：优先消耗指定类型的非当前/当前骰子
        if (need <= 0) return 0;
        int consumed = 0;
        Iterator<String> iterator = dices.iterator();
        while (iterator.hasNext() && consumed < need) {
            String dice = iterator.next();
            boolean isTarget = dice.equals(targetElement);
            boolean isCurrentDice = dice.equals(currentElement);
            if (isTarget && (isCurrent == isCurrentDice)) {
                iterator.remove();
                consumed++;
            }
        }
        return consumed;
    }

    private static Map<String, Integer> getElementCount(List<String> dices) {
        Map<String, Integer> countMap = new ConcurrentHashMap<>();
        wildCount.set(0);
        for (String dice : dices) {
            if ("万能".equals(dice)){
                wildCount.incrementAndGet();
            }
            countMap.put(dice, countMap.getOrDefault(dice, 0) + 1);
        }
        return countMap;
    }

    public static void consumeAnyDice(PlayerSession currentPlayer, int count) {
        // 原有逻辑已符合 非当前 → 当前 → 万能
        List<String> dices = new ArrayList<>(currentPlayer.getDices());
        if (dices.size() < count) {
            return;
        }
        String currentElement = getCurrentCharacterElement(currentPlayer);
        List<String> nonCurrentDices = new ArrayList<>();
        List<String> currentDices = new ArrayList<>();
        List<String> wildDices = new ArrayList<>();
        for (String dice : dices) {
                if ("万能".equals(dice)) {
                    wildDices.add(dice);
                } else if (dice.equals(currentElement)) {
                    currentDices.add(dice);
                } else {
                    nonCurrentDices.add(dice);
                }
        }
        int remaining = count;
        List<String> consumed = new ArrayList<>();

        // 先消耗非当前
        int takeFromNonCurrent = Math.min(remaining, nonCurrentDices.size());
        if (takeFromNonCurrent > 0) {
            consumed.addAll(nonCurrentDices.subList(0, takeFromNonCurrent));
            nonCurrentDices = nonCurrentDices.subList(takeFromNonCurrent, nonCurrentDices.size());
            remaining -= takeFromNonCurrent;
        }

        // 再消耗当前
        if (remaining > 0) {
            int takeFromCurrent = Math.min(remaining, currentDices.size());
            if (takeFromCurrent > 0) {
                consumed.addAll(currentDices.subList(0, takeFromCurrent));
                currentDices = currentDices.subList(takeFromCurrent, currentDices.size());
                remaining -= takeFromCurrent;
            }
        }

        // 最后消耗万能
        if (remaining > 0) {
            int takeFromWild = Math.min(remaining, wildDices.size());
            if (takeFromWild > 0) {
                consumed.addAll(wildDices.subList(0, takeFromWild));
                wildDices = wildDices.subList(takeFromWild, wildDices.size());
                remaining -= takeFromWild;
            }
        }

        if (remaining > 0) {
            return;
        }

        List<String> remainingDices = new ArrayList<>();
        remainingDices.addAll(nonCurrentDices);
        remainingDices.addAll(currentDices);
        remainingDices.addAll(wildDices);
        currentPlayer.setDices(remainingDices);

    }
    public static List<String> generateInitialDices(int num,PlayerSession playerSession){
        Random random = new Random();
        List<String> result = new ArrayList<>();
        //1.获取队伍中所有角色的元素
        Set<String> teamElements = new HashSet<>();
        for (CharacterCard card : playerSession.getCharacterCards() ){
            String element = CHARACTER_TO_ELEMENT.get(card.getName().trim());
            if (element!=null){
                teamElements.add(element);
            }
        }
        //过滤掉不在骰子列表的元素，避免无效元素
        List<String> validTeamElements = teamElements.stream()
                .filter(elem ->Arrays.asList(DiceCost.ALL_ELEMENTS).contains(elem))
                .toList();
        //2.生成骰子
        for (int i=0;i<num;i++){
            String dice;
            if (!validTeamElements.isEmpty()){
                double probability = random.nextDouble();
                if (probability<0.7){
                    int index = random.nextInt(validTeamElements.size());
                    dice = validTeamElements.get(index);
                }
                else {
                    List<String> otherElements = new ArrayList<>();
                    for (String elem :DiceCost.ALL_ELEMENTS){
                        if (!validTeamElements.contains(elem)){
                            otherElements.add(elem);
                        }
                    }
                    dice = otherElements.get(random.nextInt(otherElements.size()));
                }
            }
            else {
                int index = random.nextInt(DiceCost.ALL_ELEMENTS.length);
                dice = DiceCost.ALL_ELEMENTS[index];
            }
            result.add(dice);
        }
        return sort(result);
    }
    //进行骰子的排序
    public static List<String> sort(List<String> result) {
        result.sort((s, t1) -> {
            int index1 = DiceCost.SORT_ORDER.indexOf(s);
            int index2 = DiceCost.SORT_ORDER.indexOf(t1);
            // 如果元素不在排序列表中，放在最后
            if (index1 == -1) index1 = Integer.MAX_VALUE;
            if (index2 == -1) index2 = Integer.MAX_VALUE;
            return Integer.compare(index1, index2);
        });
        return result;
    }
}