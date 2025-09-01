package com.game.service;

import com.game.dao.PlayerDao;
import com.game.dto.PlayerDO;
import com.game.dto.PlayerStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.alibaba.fastjson.JSON;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

//玩家持久化数据与业务逻辑层
@Service
public class PlayerService {
    @Autowired
    PlayerDao playerDao;
    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    //缓存key前缀
    private static final String USER_AUTH_CACHE_KEY = "user:auth";

    //创建用户
    public void createPlayer(String playerId, String playerName, String password) {
        PlayerDO playerDO = new PlayerDO();
        playerDO.setPlayerId(playerId);
        playerDO.setPlayerName(playerName);
        playerDO.setRegistrationTime(LocalDateTime.now());
        playerDO.setStatus(PlayerStatus.EXITING);
        playerDO.setLastLoginTime(LocalDateTime.now());
        playerDO.setPassword(password);
        playerDao.insertPlayer(playerDO);

        // 修正：缓存完整PlayerDO的JSON字符串
        String cacheKey = USER_AUTH_CACHE_KEY + playerName;
        redisTemplate.opsForValue().set(cacheKey, JSON.toJSONString(playerDO), 1, TimeUnit.HOURS); }

    //通过姓名获取玩家
    public PlayerDO getByName(String playerName) {
        //查询缓存
        String cacheKey = USER_AUTH_CACHE_KEY + playerName;
        String cachePlayerJson = redisTemplate.opsForValue().get(cacheKey);
        if (cachePlayerJson != null) {
            return JSON.parseObject(cachePlayerJson, PlayerDO.class);
        }
        PlayerDO playerDO = playerDao.getByName(playerName);
        if (playerDO != null) {
            redisTemplate.opsForValue().set(cacheKey, JSON.toJSONString(playerDO), 1, TimeUnit.HOURS);
        }
        return playerDO;
    }

    public PlayerDO getByPlayerId(String playerId) {
        return playerDao.getPlayerById(playerId);
    }

    //修改玩家最近活跃时间
    @Transactional
    public void updateStatus(String playerId, PlayerStatus playerStatus) {
        LocalDateTime time = LocalDateTime.now();
        int rows = playerDao.updateStatus(time, playerId, playerStatus);
        if (rows == 0) {
            throw new RuntimeException("更新玩家状态失败，玩家ID: " + playerId);
        }
    }

    //判断是否登录
    public boolean isStatus(String playerId, PlayerStatus playerStatus) {
        return getByPlayerId(playerId).getStatus() == playerStatus;
    }

    public void updateRoomId(String id, String roomId) {
        playerDao.updateRoomId(id, roomId);
    }

    //离开房间
    @Transactional
    public void exit(String playerId) {
        updateRoomId(playerId, null);
        updateStatus(playerId, PlayerStatus.EXITING);
    }

    public void resetAllPlayersToOffline() {
        playerDao.resetAllPlayersToOffline();
    }

}
