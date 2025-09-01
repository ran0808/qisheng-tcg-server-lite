package com.game.dto;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("battles")
public class Battle {
    @TableId(type = IdType.AUTO)
    private Integer battleId;
    private String roomId;
    private String playerId1;
    private String playerId2;
    private Integer turn;
    private int ourCard1;
    private int ourCard2;
    private int ourCard3;
    private int enemyCard1;
    private int enemyCard2;
    private int enemyCard3;
    private String winner;
    private String loser;
    private BattleState status;

}