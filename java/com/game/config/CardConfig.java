package com.game.config;

import com.game.network.gameSet.EffectImple.ElementAttachEffect;
import com.game.network.gameSet.card.*;
import com.game.network.gameSet.dice.DiceCost;
import com.game.network.gameSet.skill.Skill;
import com.game.network.gameSet.skill.SkillFactory;
import com.game.network.handler.GameStateManager;
import com.game.network.player.PlayerSessionManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@Configuration
public class CardConfig {

    @Autowired
    private GameStateManager gameStateManager;

    @Autowired
    private PlayerSessionManager playerSessionManager;

    @Autowired
    private SkillFactory skillFactory;
    // 卡牌ID生成器
    private final AtomicInteger characterCardIdGenerator = new AtomicInteger(0);
    private final AtomicInteger otherCardIdGenerator = new AtomicInteger(10);

    // ****************************** 钟离相关卡牌和技能 ******************************
    @Bean
    public AddictionCard yanji() {
        return new AddictionCard(
                otherCardIdGenerator.getAndIncrement(),
                "岩脊",
                AddictionCard.Type.POISON,
                2,
                1,
                new ElementAttachEffect("岩"),null
        );
    }

    @Bean
    public CharacterCard Zhongli(
    ) {
        int id = characterCardIdGenerator.getAndIncrement();
        CharacterCard Zhongli = new CharacterCard(
                id,
                "钟离",
                "岩",
                12,
                3,
                gameStateManager,
                playerSessionManager,
                null
        );
        Zhongli.addSkill(skillFactory.createSkill("钟离", Skill.SkillType.NORMAL_ATTACK));
        Zhongli.addSkill(skillFactory.createSkill("钟离",Skill.SkillType.ELEMENTAL_SKILL));
        Zhongli.addSkill(skillFactory.createSkill("钟离", Skill.SkillType.ELEMENTAL_BURST));
        return Zhongli;
    }

    // ****************************** 芭芭拉相关卡牌和技能 ******************************
    @Bean
    public AddictionCard songRing() {
        return new AddictionCard(
                otherCardIdGenerator.getAndIncrement(),
                "歌声之环",
                AddictionCard.Type.HEAL,
                2,
                1,
                null
        );
    }

    @Bean
        public CharacterCard Barbara() {
        int id = characterCardIdGenerator.getAndIncrement();
        CharacterCard Barbara = new CharacterCard(
                id,
                "芭芭拉",
                "水",
                10,
                3,
                gameStateManager,
                playerSessionManager,
                null
        );
        Barbara.addSkill(skillFactory.createSkill("芭芭拉", Skill.SkillType.NORMAL_ATTACK));
        Barbara.addSkill(skillFactory.createSkill("芭芭拉",Skill.SkillType.ELEMENTAL_SKILL));
        Barbara.addSkill(skillFactory.createSkill("芭芭拉", Skill.SkillType.ELEMENTAL_BURST));
        return Barbara;
    }

    // ****************************** 迪卢克相关卡牌和技能 ******************************
    @Bean
    public CharacterCard Diruk(
    ) {
        int id = characterCardIdGenerator.getAndIncrement();
        CharacterCard Diruk = new CharacterCard(
                id,
                "迪卢克",
                "火",
                10,
                2,
                gameStateManager,
                playerSessionManager,
                null
        );
        Diruk.addSkill(skillFactory.createSkill("迪卢克", Skill.SkillType.NORMAL_ATTACK));
        Diruk.addSkill(skillFactory.createSkill("迪卢克",Skill.SkillType.ELEMENTAL_SKILL));
        Diruk.addSkill(skillFactory.createSkill("迪卢克", Skill.SkillType.ELEMENTAL_BURST));
        return Diruk;
    }

    // ****************************** 凯亚相关卡牌和技能 ******************************
    @Bean
    public CharacterCard Kaya() {
        int id = characterCardIdGenerator.getAndIncrement();
        CharacterCard Kaya = new CharacterCard(
                id,
                "凯亚",
                "冰",
                10,
                2,
                gameStateManager,
                playerSessionManager,
                null
        );
        Kaya.addSkill(skillFactory.createSkill("凯亚", Skill.SkillType.NORMAL_ATTACK));
        Kaya.addSkill(skillFactory.createSkill("凯亚",Skill.SkillType.ELEMENTAL_SKILL));
        Kaya.addSkill(skillFactory.createSkill("凯亚", Skill.SkillType.ELEMENTAL_BURST));
        return Kaya;
    }

    // ****************************** 砂糖相关卡牌和技能 ******************************
    @Bean
    public AddictionCard fengling() {
        return new AddictionCard(
                otherCardIdGenerator.getAndIncrement(),
                "大型风灵",
                AddictionCard.Type.POISON,
                3,
                2,
                new ElementAttachEffect("风"),null
        );
    }

    @Bean
    public CharacterCard Sugar() {
        int id = characterCardIdGenerator.getAndIncrement();
        CharacterCard Sugar = new CharacterCard(
                id,
                "砂糖",
                "风",
                10,
                2,
                gameStateManager,
                playerSessionManager,
                null
        );
        Sugar.addSkill(skillFactory.createSkill("砂糖", Skill.SkillType.NORMAL_ATTACK));
        Sugar.addSkill(skillFactory.createSkill("砂糖",Skill.SkillType.ELEMENTAL_SKILL));
        Sugar.addSkill(skillFactory.createSkill("砂糖", Skill.SkillType.ELEMENTAL_BURST));

        return Sugar;
    }

    // ****************************** 缇纳里相关卡牌和技能 ******************************
    @Bean
    public AddictionCard cangYunHuaShi() {
        return new AddictionCard(
                otherCardIdGenerator.getAndIncrement(),
                "藏蕴花矢",
                AddictionCard.Type.POISON,
                1,
                1,
                new ElementAttachEffect("草"),null
        );
    }

    @Bean
    public CharacterCard Tinari(){
        int id = characterCardIdGenerator.getAndIncrement();
        CharacterCard Tinari = new CharacterCard(
                id,
                "缇纳里",
                "草",
                10,
                2,
                gameStateManager,
                playerSessionManager,null
        );
        Tinari.addSkill(skillFactory.createSkill("缇纳里", Skill.SkillType.NORMAL_ATTACK));
        Tinari.addSkill(skillFactory.createSkill("缇纳里",Skill.SkillType.ELEMENTAL_SKILL));
        Tinari.addSkill(skillFactory.createSkill("缇纳里", Skill.SkillType.ELEMENTAL_BURST));
        return Tinari;
    }

    // ****************************** 菲谢尔相关卡牌和技能 ******************************
    @Bean
    public AddictionCard oz() {
        return new AddictionCard(
                otherCardIdGenerator.getAndIncrement(),
                "奥兹",
                AddictionCard.Type.POISON,
                2,
                1,
                new ElementAttachEffect("雷"),null
        );
    }

    @Bean
    public CharacterCard Fischl() {
        int id = characterCardIdGenerator.getAndIncrement();
        CharacterCard Fischl = new CharacterCard(
                id,
                "菲谢尔",
                "雷",
                10,
                3,
                gameStateManager,
                playerSessionManager,null
        );
        Fischl.addSkill(skillFactory.createSkill("菲谢尔", Skill.SkillType.NORMAL_ATTACK));
        Fischl.addSkill(skillFactory.createSkill("菲谢尔",Skill.SkillType.ELEMENTAL_SKILL));
        Fischl.addSkill(skillFactory.createSkill("菲谢尔", Skill.SkillType.ELEMENTAL_BURST));

        return Fischl;
    }

    // ****************************** 其他卡牌 ******************************
    @Bean
    public ActionCard brewedChicken() {
        return new ActionCard(
                otherCardIdGenerator.getAndIncrement(),
                "甜甜花酿鸡",
                ActionCard.ActionType.HEAL,
                null,
                1
        );
    }

    @Bean
    public ActionCard catherine() {
        DiceCost diceCost = new DiceCost(1, "任意");
        List<DiceCost> list = new ArrayList<>();
        list.add(diceCost);

        return new ActionCard(
                otherCardIdGenerator.getAndIncrement(),
                "凯瑟琳",
                ActionCard.ActionType.QUICKLY_ACTION,
                list,
                0
        );
    }

    @Bean
    public ActionCard bestFriend() {
        DiceCost diceCost = new DiceCost(2, "任意");
        List<DiceCost> list = new ArrayList<>();
        list.add(diceCost);

        return new ActionCard(
                otherCardIdGenerator.getAndIncrement(),
                "最好的伙伴",
                ActionCard.ActionType.DICE_CHANGE,
                list,
                2
        );
    }

    @Bean
    public AddictionCard burn() {
        return new AddictionCard(
                otherCardIdGenerator.getAndIncrement(),
                "燃烧烈焰",
                AddictionCard.Type.POISON,
                1,
                1,
                new ElementAttachEffect("火"),null
        );
    }

    @Bean
    public AddictionCard grassCore() {
        return new AddictionCard(
                otherCardIdGenerator.getAndIncrement(),
                "草原核",
                null,
                1,
                0,
                null
        );
    }

    @Bean
    public AddictionCard aggravateField() {
        return new AddictionCard(
                otherCardIdGenerator.getAndIncrement(),
                "激化领域",
                null,
                2,
                0,
                null
        );
    }

    // ****************************** 卡牌库汇总 ******************************
    @Bean(name = "cardLibrary")
    public Map<Integer, Card> cardLibrary(
            // 角色卡
            CharacterCard Zhongli,
            CharacterCard Barbara,
            CharacterCard Diruk,
            CharacterCard Kaya,
            CharacterCard Sugar,
            CharacterCard Tinari,
            CharacterCard Fischl,
            // 附加卡
            AddictionCard yanji,
            AddictionCard songRing,
            AddictionCard fengling,
            AddictionCard cangYunHuaShi,
            AddictionCard oz,
            AddictionCard burn,
            AddictionCard grassCore,
            AddictionCard aggravateField,

            // 行动卡
            ActionCard brewedChicken,
            ActionCard catherine,
            ActionCard bestFriend
    ) {
        Map<Integer, Card> cardMap = new HashMap<>();

        // 添加角色卡
        cardMap.put(Zhongli.getId(), Zhongli);
        cardMap.put(Barbara.getId(), Barbara);
        cardMap.put(Diruk.getId(), Diruk);
        cardMap.put(Kaya.getId(), Kaya);
        cardMap.put(Sugar.getId(), Sugar);
        cardMap.put(Tinari.getId(), Tinari);
        cardMap.put(Fischl.getId(), Fischl);

        // 添加附加卡
        cardMap.put(yanji.getId(), yanji);
        cardMap.put(songRing.getId(), songRing);
        cardMap.put(fengling.getId(), fengling);
        cardMap.put(cangYunHuaShi.getId(), cangYunHuaShi);
        cardMap.put(oz.getId(), oz);
        cardMap.put(burn.getId(), burn);
        cardMap.put(grassCore.getId(), grassCore);
        cardMap.put(aggravateField.getId(), aggravateField);

        // 添加行动卡
        cardMap.put(brewedChicken.getId(), brewedChicken);
        cardMap.put(catherine.getId(), catherine);
        cardMap.put(bestFriend.getId(), bestFriend);

        return cardMap;
    }
}
