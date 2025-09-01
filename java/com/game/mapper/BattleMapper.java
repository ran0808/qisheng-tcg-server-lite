package com.game.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.game.dto.Battle;
import org.apache.ibatis.annotations.Select;

public interface BattleMapper extends BaseMapper<Battle> {
    @Select("select * from battles where room_id=#{room_id} and status='1'")
    Battle selectByRoomId(String roomId);
    @Select("select * from battles where " +
            "( player_id1= #{playerId1} and player_id2 = #{playerId2} and status = '1') " +
            "or " +
            "(player_id2= #{playerId1} and player_id1 = #{playerId2} and status = '1')")
    Battle selectByIds(String playerId1, String playerId2);
    @Select("select * from battles where " +
            "(player_id1 = #{playerId} and player_id2 = #{playerId2} and room_id = #{roomId}) " +
            "or " +
            "(player_id1 = #{playerId2} and player_id2 = #{playerId} and room_id = #{roomId})")
    Battle getBattle(String playerId, String playerId2, String roomId);

    void updateBattleCards(int ourCard1, int ourCard2, int ourCard3, int enemyCard1, int enemyCard2, int enemyCard3, String roomId);

    void updateBattleGameOver(String playerId1, String playerId2,String roomId);

    void updateBattleWithDraw(String roomId);

    void updateBattleTurn(String roomId, int turn);

    void updateBattleExcept(String roomId);
}
