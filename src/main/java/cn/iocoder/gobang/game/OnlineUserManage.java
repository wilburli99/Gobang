package cn.iocoder.gobang.game;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class OnlineUserManage {
    //此哈希表来表示在线用户的信息
    //但是如果多个用户的上线和下线的时候，这个哈希表会存在并发问题。所以需要用到线程安全的哈希表ConcurrentHashMap
    private ConcurrentHashMap<Integer, WebSocketSession> gameHall = new ConcurrentHashMap<>();
    private ConcurrentHashMap<Integer, WebSocketSession> gameRoom = new ConcurrentHashMap<>();
    public void enterGameHall(int userId, WebSocketSession session) {
        gameHall.put(userId, session);
    }

    public void exitGameHall(int userId) {
        gameHall.remove(userId);
    }

    public WebSocketSession getFromGameHall(int userId) {
        return gameHall.get(userId);
    }

    public void enterGameRoom(int userId, WebSocketSession session) {
        gameRoom.put(userId, session);
    }

    public void exitGameRoom(int userId) {
        gameRoom.remove(userId);
    }

    public WebSocketSession getFromGameRoom(int userId) {
        return gameRoom.get(userId);
    }
}
