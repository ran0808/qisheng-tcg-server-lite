package com.game.network.gameSet.card;

import lombok.AllArgsConstructor;
import lombok.Data;
import com.game.network.player.PlayerSession;


@AllArgsConstructor
@Data
public abstract class Card {
    protected final int id;//唯一ID,不可以修改
    protected final String name;//卡牌名称
    //当角色卡被使用的时候，已经设置了出战角色，根据角色的技能发起攻击
    public abstract boolean onUse(PlayerSession currentPlayer, PlayerSession oppositePlayer, int i);
}