package com.game.dao;
import com.game.dto.PlayerDO;
import com.game.dto.PlayerStatus;
import com.game.mapper.PlayerMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;


@Service
public class PlayerDao {
    @Autowired
    PlayerMapper playerMapper;
    //新增玩家
    public void insertPlayer(PlayerDO playerDO){
            playerMapper.insert(playerDO);
    }
    //根据玩家ID查询
    public PlayerDO getPlayerById(String playerId){
           return playerMapper.selectById(playerId);
    }
    //更新玩家状态(上次活跃时间)
    public int updateStatus(LocalDateTime time, String playerId, PlayerStatus playerStatus){
        return playerMapper.updateStatus(time,playerId,playerStatus);
    }

    public PlayerDO getByName(String playerName) {
        return playerMapper.getByName(playerName);
    }

    public void updateRoomId(String id, String roomId) {
        playerMapper.updateRoomId(id,roomId);
    }
    public void resetAllPlayersToOffline() {
        playerMapper.resetAllPlayersToOffline();
    }
}
