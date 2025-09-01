package com.game.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.game.dto.PlayerDO;
import com.game.dto.PlayerStatus;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface PlayerMapper extends BaseMapper<PlayerDO> {
    //更新玩家上次活跃时间
    int updateStatus(LocalDateTime time, String playerId, PlayerStatus playerStatus);
    @Select("select * from player where player_name=#{playerName}")
    PlayerDO getByName(String playerName);
    @Update("update player set current_room_id =#{roomId} where player_id = #{id}")
    void updateRoomId(String id,String roomId);
    @Update("update player set status = 0 where status != 0")
    void resetAllPlayersToOffline();
}
