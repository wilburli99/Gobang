package cn.iocoder.gobang.game;

import cn.iocoder.gobang.model.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.LinkedList;
import java.util.Queue;
// 匹配器-- 匹配对列
@Component
public class Matcher {
    private Queue<User> normalQueue = new LinkedList<>();
    private Queue<User> highQueue = new LinkedList<>();
    private Queue<User> veryHighQueue = new LinkedList<>();

    @Autowired
    private OnlineUserManage  onlineUserManage;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private RoomManager roomManager;

    // 添加用户到匹配队列
    public void add(User user){
        if (user.getScore() < 2000){
            // 通过synchronized关键字加锁，防止线程安全问题
            synchronized (normalQueue){
                normalQueue.offer(user);
            }
            System.out.println("用户" + user.getUsername() + "进入普通队列");
        } else if (user.getScore() >= 2000 && user.getScore() < 3000) {
            synchronized (highQueue){
                highQueue.offer(user);
            }
            System.out.println("用户" + user.getUsername() + "进入高级队列");
        } else {
            synchronized (veryHighQueue){
                veryHighQueue.offer(user);
            }
            System.out.println("用户" + user.getUsername() + "进入超高级队列");
        }
    }
    // 从匹配对列中移除用户
    public void remove(User user){
        if (user.getScore() < 2000){
            synchronized (normalQueue){
                normalQueue.remove(user);
            }
            System.out.println("用户" + user.getUsername() + "离开普通队列");
        } else if (user.getScore() >= 2000 && user.getScore() < 3000) {
            synchronized (highQueue){
                highQueue.remove(user);
            }
            System.out.println("用户" + user.getUsername() + "离开高级队列");
        } else {
            synchronized (veryHighQueue){
                veryHighQueue.remove(user);
            }
            System.out.println();
        }
    }

    // 针对三个对列创建线程
    public void match(){
        Thread thread1 = new Thread() {
            @Override
            public void run() {
                while (true) {
                    handlerMatch(normalQueue);
                    // notify可以唤醒wait，让线程继续执行
                    normalQueue.notify();
                }
            }
        };
        thread1.start();

        Thread thread2 = new Thread() {
            @Override
            public void run() {
                while (true) {
                    handlerMatch(highQueue);
                    highQueue.notify();
                }
            }
        };
        thread2.start();

        Thread thread3 = new Thread() {
            @Override
            public void run() {
                while (true) {
                    handlerMatch(veryHighQueue);
                    veryHighQueue.notify();
                }
            }
        };
        thread3.start();
    }

    private void handlerMatch(Queue<User> matchQueue) {
        synchronized (matchQueue){
            try {
                // 1. 检测对列中的玩家数量是否满足条件
                while (matchQueue.size() < 2) {
                    // wait可以释放锁，然后进入等待队列，等待被notify
                    matchQueue.wait();
                }
                // 2. 尝试从对列中获取两个玩家
                User player1 = matchQueue.poll();
                User player2 = matchQueue.poll();
                System.out.println("开始匹配玩家：" + player1.getUsername() + " 和 " + player2.getUsername());
                // 3. 获取两个玩家的 WebSocket会话，告诉它们匹配成功，并把对方信息告诉它
                WebSocketSession session1 = onlineUserManage.getFromGameHall(player1.getUserId());
                WebSocketSession session2 = onlineUserManage.getFromGameHall(player2.getUserId());
                // 理论上来说，匹配队列中的玩家一定是在线的状态.
                // 因为前面的逻辑里进行了处理，当玩家断开连接的时候就把玩家从匹配队列中移除了
                // 但是此处仍然进行一次判定~~(二次判定)
                if (session1 == null){
                    // 如果玩家1断开连接，那么就把玩家2放回匹配队列中
                    matchQueue.offer(player2);
                    return;
                }
                if (session2 == null){
                    // 如果玩家2断开连接，那么就把玩家1放回匹配队列中
                    matchQueue.offer(player1);
                    return;
                }
                // 当前能否排到两个玩家是同一个用户的情况嘛?一个玩家入队列了两次??
                // 理论上也不会存在~~
                // 1)如果玩家下线，就会对玩家移出匹配队列
                // 2)又禁止了玩家多开.
                //但是仍然在这里多进行一次判定，以免前面的逻辑出现 bug 时带来严重的后果
                if (session1 == session2){
                    // 把其中一个玩家移出匹配队列
                    matchQueue.offer(player1);
                    return;
                }
                // 4. 把两个玩家放到同一个游戏房间中
                Room room = new Room();
                roomManager.add(room, player1.getUserId(), player2.getUserId());

                // 5. 给玩家反馈信息：匹配成功
                // 通过WebSocketSession.sendMessage()，发送消息给玩家"matchSuccess"
                MatchResponse response1 = new MatchResponse();
                response1.setOk(true);
                response1.setMessage("matchSuccess");
                String json1 = objectMapper.writeValueAsString(response1);
                session1.sendMessage(new TextMessage(json1));

                MatchResponse response2 = new MatchResponse();
                response2.setOk(true);
                response2.setMessage("matchSuccess");
                String json2 = objectMapper.writeValueAsString(response2);
                session2.sendMessage(new TextMessage(json2));
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
