package cn.iocoder.gobang.api;

import cn.iocoder.gobang.game.*;
import cn.iocoder.gobang.mapper.UserMapper;
import cn.iocoder.gobang.model.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;

@Component
public class GameAPI extends TextWebSocketHandler {
    @Autowired
    private ObjectMapper objectMapper;

    public GameAPI(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Autowired
    private OnlineUserManage onlineUserManage;
    @Autowired
    private RoomManager roomManager;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private Matcher matcher;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        GameReadyResponse resp = new GameReadyResponse();
        // 1. 先获取到用户的身份信息（从httpsession里面拿到当前用户的对象）
        User user = (User) session.getAttributes().get("user");
        if (user == null) {
            resp.setOk(false);
            resp.setReason("用户尚未登录！");
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(resp)));
            return;
        }

        // 2. 判定当前用户是否已经进入房间（拿着房间管理器进行查询）
        Room room = roomManager.getRoomByUserId(user.getUserId());
        if (room == null) {
            resp.setOk(false);
            resp.setReason("用户尚未匹配到！");
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(resp)));
            return;
        }

        // 3. 判定是不是多开（用户是不是已经在其他地方进入游戏了）
        if (onlineUserManage.getFromGameHall(user.getUserId()) != null
                || onlineUserManage.getFromGameRoom(user.getUserId()) != null) {
            resp.setOk(true);
            resp.setReason("禁止多开！");
            resp.setMessage("repeatConnection");
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(resp)));
            return;
        }

        // 4. 设置当前玩家上线
        onlineUserManage.enterGameRoom(user.getUserId(), session);

        // 5. 把两个玩家加入房间中
        // 前面的创建房间匹配过程，是在 game_hall.htm1 页面中完成的
        // 因此前面匹配到对手之后，需要经过页面跳转，来到game_room.html 才算正式进入房间
        // 当前这个逻辑是在 game_room.htm1 页面加载的时候进行的.
        // 执行到当前逻辑，说明玩家已经页面跳转成功了
        synchronized (room) { 
            // 加锁是为了保证房间状态的一致性，如果不加锁，可能会出现竞态条件，导致房间状态混乱：
            // 两个玩家都被设置成 user1，user2 永远为 null。
            // 两个玩家都被设置成 user2，user1 永远为 null。
            // 两个玩家都被设置成同一个 user，房间只进了一个人。
            if (room.getUser1() == null) {
                // 第一个玩家还尚未加入房间，就把当前连上websocket的玩家作为user1，加入到房间中
                room.setUser1(user);
                // 把先连接进房间的玩家设置为先手方
                room.setWhiteUser(user.getUserId());
                System.out.println("用户" + user.getUsername() + "已准备就绪");
                return;
            }
            if (room.getUser2() == null) {
                room.setUser2(user);
                System.out.println("用户" + user.getUsername() + "已准备就绪");

                // 当两个玩家都加入成功后，就让服务器给两个玩家返回websocket的连接信息
                // 通知两个玩家，双方已准备好
                // 通知玩家1
                noticeGameReady(room, room.getUser1(), room.getUser2());
                // 通知玩家2
                noticeGameReady(room, room.getUser2(), room.getUser1());
                return;
            }
        }

        // 6. 此处如果又有玩家尝试连接同一个房间, 就提示报错.
        //    这种情况理论上是不存在的, 为了让程序更加的健壮, 还是做一个判定和提示.
        resp.setOk(false);
        resp.setReason("当前房间已满, 您不能加入房间");
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(resp)));
    }

    private void noticeGameReady(Room room, User thisUser, User thatUser) throws IOException {
        GameReadyResponse resp = new GameReadyResponse();
        resp.setMessage("gameReady");
        resp.setOk(true);
        resp.setReason("");
        resp.setRoomId(room.getRoomId());
        resp.setThisUserId(thisUser.getUserId());
        resp.setThatUserId(thatUser.getUserId());
        resp.setWhiteUser(room.getWhiteUser());
        // 把当前的响应数据传回给玩家.
        WebSocketSession webSocketSession = onlineUserManage.getFromGameRoom(thisUser.getUserId());
        webSocketSession.sendMessage(new TextMessage(objectMapper.writeValueAsString(resp)));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        // 1. 先从 session 里拿到当前用户的身份信息
        User user = (User) session.getAttributes().get("user");
        if (user == null) {
            System.out.println("[handleTextMessage] 当前玩家尚未登录! ");
            return;
        }

        // 2. 根据玩家 id 获取到房间对象
        Room room = roomManager.getRoomByUserId(user.getUserId());
        // 3. 通过 room 对象来处理这次具体的请求
        room.putChess(message.getPayload());
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        User user = (User) session.getAttributes().get("user");
        if (user == null) return;

        // 1. 获取当前用户所在的房间
        Room room = roomManager.getRoomByUserId(user.getUserId());

        // 2. 清理在线用户管理器的状态
        // 用户离开游戏房间
        onlineUserManage.exitGameRoom(user.getUserId());
        // 如果用户在大厅中，也需要清理
        onlineUserManage.exitGameHall(user.getUserId());

        // 3. 处理房间和对手逻辑
        if (room != null) {
            // 如果房间里有对手，通知对手获胜并清理房间
            User otherUser = (user == room.getUser1()) ? room.getUser2() : room.getUser1();
            if (otherUser != null && onlineUserManage.getFromGameRoom(otherUser.getUserId()) != null) {
                // 只有对手还在房间里，才通知对手获胜
                noticeThatUserWin(user); // 这个方法会处理通知对手和 roomManager.remove
            } else {
                // 如果房间里只有自己，或者对手已经不在了，直接移除房间
                // 避免 noticeThatUserWin 内部因为找不到对手而提前 return 导致房间未释放
                roomManager.remove(room.getRoomId(), room.getUser1().getUserId(), room.getUser2().getUserId());
            }
        }
        // 4. 从匹配队列中移除用户（确保不会有残留用户在匹配队列中）
        // 你需要确保 Matche`r 实例能够被获取到
        // Matcher matcher = GobangApplication.context.getBean(Matcher.class); // 如果是静态获取
        // matcher.remove(user); // 确保 Matcher.remove 能正确处理
        System.out.println("当前用户" + user.getUserId() + "离开游戏房间并清理状态");
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        // // 玩家下线
        // User user = (User) session.getAttributes().get("user");
        // if (user == null) {
        //     // 这里简单处理， 在断开连接的时候就不给客户端返回响应了。
        //     return;
        // }
        // WebSocketSession exitSession = onlineUserManage.getFromGameHall(user.getUserId());
        // if (session == exitSession) {
        //     // 这个判定，为了避免在多开的情况下第二个用户退出链接动作，导致第一个用户的会话被删除
        //     onlineUserManage.exitGameHall(user.getUserId());
        // }
        // System.out.println("当前用户" + user.getUserId() + "离开游戏房间");
        // // 通知对手获胜
        // noticeThatUserWin(user);
        User user = (User) session.getAttributes().get("user");
        if (user == null) return;

        // 1. 获取当前用户所在的房间
        Room room = roomManager.getRoomByUserId(user.getUserId());

        // 2. 清理在线用户管理器的状态
        // 用户离开游戏房间
        onlineUserManage.exitGameRoom(user.getUserId());
        // 如果用户在大厅中，也需要清理
        onlineUserManage.exitGameHall(user.getUserId());

        // 3. 处理房间和对手逻辑
        if (room != null) {
            // 如果房间里有对手，通知对手获胜并清理房间
            User otherUser = (user == room.getUser1()) ? room.getUser2() : room.getUser1();
            if (otherUser != null && onlineUserManage.getFromGameRoom(otherUser.getUserId()) != null) {
                // 只有对手还在房间里，才通知对手获胜
                noticeThatUserWin(user); // 这个方法会处理通知对手和 roomManager.remove
            } else {
                // 如果房间里只有自己，或者对手已经不在了，直接移除房间
                // 避免 noticeThatUserWin 内部因为找不到对手而提前 return 导致房间未释放
                roomManager.remove(room.getRoomId(), room.getUser1().getUserId(), room.getUser2().getUserId());
            }
        }
        // 4. 从匹配队列中移除用户（确保不会有残留用户在匹配队列中）
        // 你需要确保 Matche`r 实例能够被获取到
        // 如果是静态获取
        matcher.remove(user); // 确保 Matcher.remove 能正确处理
        System.out.println("当前用户" + user.getUserId() + "离开游戏房间并清理状态");
    }

    private void noticeThatUserWin(User user) throws IOException {
        // 1. 根据当前玩家, 找到玩家所在的房间
        Room room = roomManager.getRoomByUserId(user.getUserId());
        if (room == null) {
            // 这个情况意味着房间已经被释放了, 也就没有 "对手" 了
            System.out.println("当前房间已经释放, 无需通知对手!");
            return;
        }
        // 2. 根据房间找到对手
        User thatUser = (user == room.getUser1()) ? room.getUser2() : room.getUser1();
        // 3. 找到对手的在线状态
        WebSocketSession webSocketSession = onlineUserManage.getFromGameRoom(thatUser.getUserId());
        if (webSocketSession == null) {
            // 这就意味着对手也掉线了!
            System.out.println("对手也已经掉线了, 无需通知!");
            return;
        }
        // 4. 构造一个响应, 来通知对手, 你是获胜方
        GameResponse resp = new GameResponse();
        resp.setMessage("putChess");
        resp.setUserId(thatUser.getUserId());
        resp.setWinner(thatUser.getUserId());
        webSocketSession.sendMessage(new TextMessage(objectMapper.writeValueAsString(resp)));

        // 5. 更新玩家的分数信息
        int winUserId = thatUser.getUserId();
        int loseUserId = user.getUserId();
        userMapper.userWin(winUserId);
        userMapper.userLose(loseUserId);

        // 6. 释放房间对象
        roomManager.remove(room.getRoomId(), room.getUser1().getUserId(), room.getUser2().getUserId());
    }
}
