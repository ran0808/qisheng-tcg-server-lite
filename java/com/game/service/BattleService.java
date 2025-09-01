package com.game.service;

import com.game.dao.BattlesDao;
import com.game.dto.Battle;
import com.game.dto.BattleState;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BattleService {
    @Autowired
    BattlesDao battlesDao;
    //创建对局
    public void createBattle(String playerId1,String playerId2,String roomId){
        Battle battle = new Battle();
        battle.setPlayerId1(playerId1);
        battle.setPlayerId2(playerId2);
        battle.setRoomId(roomId);
        battle.setTurn(0);
        battle.setStatus(BattleState.ON);
        insert(battle);
    }
    public void insert(Battle battle){
        battlesDao.insert(battle);
    }
    //修改对局卡牌
    public void updateCards(String playerId1,String playerId2,String roomId,List<Integer> cardIds){
        Battle battle = getBattle(playerId1,playerId2,roomId);
        if (battle == null) {
            throw new RuntimeException("未找到对局信息");
        }
        if (playerId1.equals(battle.getPlayerId1())) {
            // 当前玩家是player1，更新ourCard1-3
            battle.setOurCard1(!cardIds.isEmpty() ? cardIds.get(0) : 0);
            battle.setOurCard2(cardIds.size() >= 2 ? cardIds.get(1) : 0);
            battle.setOurCard3(cardIds.size() >= 3 ? cardIds.get(2) : 0);
        } else if (playerId1.equals(battle.getPlayerId2())) {
            // 当前玩家是player2，更新enemyCard1-3
            battle.setEnemyCard1(!cardIds.isEmpty() ? cardIds.get(0) : 0);
            battle.setEnemyCard2(cardIds.size() >= 2 ? cardIds.get(1) : 0);
            battle.setEnemyCard3(cardIds.size() >= 3 ? cardIds.get(2) : 0);
        }
        // 3. 更新数据库
        battlesDao.updateBattleCards(battle.getOurCard1(),battle.getOurCard2(),battle.getOurCard3()
        ,battle.getEnemyCard1(),battle.getEnemyCard2(),battle.getEnemyCard3(),roomId);
    }
    public void gameOver(String playerId1, String playerId2,String roomId) {
        battlesDao.updateBattleGameOver(playerId1,playerId2,roomId);
    }

    public void withDraw(String roomId) {
        battlesDao.updateBattleWithDraw(roomId);
    }

    public void incrementTurn(String roomId, int turn) {
        battlesDao.updateBattleTurn(roomId,turn);
    }

    public void ExceptEnd(String roomId) {
        battlesDao.updateBattleExcept(roomId);

    }

    public Battle getBattle(String playerId, String opponentId, String roomId) {
        return battlesDao.getBattle(playerId,opponentId,roomId);
    }
}
