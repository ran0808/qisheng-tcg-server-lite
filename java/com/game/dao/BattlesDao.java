package com.game.dao;

import com.game.dto.Battle;
import com.game.mapper.BattleMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class BattlesDao {
    @Autowired
    BattleMapper battleMapper;
    public void insert(Battle battle) {
        battleMapper.insert(battle);
    }

    public Battle selectByRoomId(String roomId) {
        return battleMapper.selectByRoomId(roomId);
    }

    public Battle getBattle(String playerId, String opponentId, String roomId) {
        return battleMapper.getBattle(playerId,opponentId,roomId);
    }

    public void updateBattleCards(int ourCard1, int ourCard2, int ourCard3, int enemyCard1, int enemyCard2, int enemyCard3, String roomId) {
        battleMapper.updateBattleCards(ourCard1,ourCard2,ourCard3,enemyCard1,enemyCard2,enemyCard3,roomId);
    }

    public void updateBattleGameOver(String playerId1, String playerId2,String roomId) {
        battleMapper.updateBattleGameOver(playerId1,playerId2,roomId);
    }

    public void updateBattleWithDraw(String roomId) {
        battleMapper.updateBattleWithDraw(roomId);
    }

    public void updateBattleTurn(String roomId, int turn) {
        battleMapper.updateBattleTurn(roomId,turn);
    }

    public void updateBattleExcept(String roomId) {battleMapper.updateBattleExcept(roomId);
    }
}
