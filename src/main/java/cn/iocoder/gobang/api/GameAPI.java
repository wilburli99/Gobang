package cn.iocoder.gobang.api;

import cn.iocoder.gobang.game.GameReadyResponse;
import cn.iocoder.gobang.game.OnlineUserManage;
import cn.iocoder.gobang.game.Room;
import cn.iocoder.gobang.game.RoomManager;
import cn.iocoder.gobang.model.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
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
        if (room != null) {
            resp.setOk(false);
            resp.setReason("用户尚未匹配到！");
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(resp)));
            return;
        }

        // 3. 判定是不是多开（用户是不是已经在其他地方进入游戏了）
        if (onlineUserManage.getFromGameHall(user.getUserId()) != null
                || onlineUserManage.getFromGameRoom(user.getUserId()) != null) {
            resp.setOk(false);
            resp.setReason("禁止多开！");
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(resp)));
            return;
        }

        // 4. 设置当前玩家上线
        onlineUserManage.enterGameRoom(user.getUserId(), session);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
    }
}
