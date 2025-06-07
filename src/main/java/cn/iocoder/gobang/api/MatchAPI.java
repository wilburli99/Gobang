package cn.iocoder.gobang.api;

import cn.iocoder.gobang.game.MatchRequest;
import cn.iocoder.gobang.game.MatchResponse;
import cn.iocoder.gobang.game.Matcher;
import cn.iocoder.gobang.game.OnlineUserManage;
import cn.iocoder.gobang.model.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
@Component
public class MatchAPI extends TextWebSocketHandler {
    @Autowired
    private OnlineUserManage onlineUserManage;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private Matcher matcher;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        //玩家上线，加入到0nlineUserManager 中
        //1.先获取到当前用户的身份信息(谁在游戏大厅中，建立的连接)
        //  此处的代码，之所以能够 getAttributes，全靠了在注册 Websocket 的时候,
        //  加上的 .addInterceptors(new HttpSessionHandshakeInterceptor());
        //  这个逻辑就把 HttpSession 中的 Attribute 都给拿到 WebSocketSession 中了
        //  在 Http 登录逻辑中，往 Httpsession 中存了 User 数据:httpSession.setAttribute("user"，user);
        //  此时就可以在 WebSocketSession 中把之前 HttpSession 里存的 User 对象给拿到了.
        //  但是，此处拿到的user可能是空的
        //  如果之前用户没有通过http进行登录，而是直接通过访问./game_hall.html 的方式进入游戏大厅, 那么此处的user就会为空
        //  此处的user就会是null的情况
        try  {
            User user = (User) session.getAttributes().get("user");
            //2. 应先判断用户是否在线，如果在线，则不允许重复登录
            WebSocketSession temSession = onlineUserManage.getFromGameHall(user.getUserId());
            if (temSession != null){
                MatchRequest response = new MatchRequest();
                response.setOk(false);
                response.setReason("禁止多开！");
                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(response)));
                session.close();
                return;
            }
            //3. 拿到身份信息后，可以把玩家设置为在线状态
            onlineUserManage.enterGameHall(user.getUserId(), session);
            System.out.println("用户" + user.getUsername() + "上线了");
        } catch (NullPointerException e) {
            e.printStackTrace();
            // 出现空指针异常，说明用户的身份信息为空，说明用户没有登录
            // 需要把该信息返回
            MatchRequest request = new MatchRequest();
            request.setOk(false);
            request.setReason("用户未登录，无法进行后续匹配功能");
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(request)));
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        // 实现处理开始匹配请求和停止匹配请求
        User user = (User)session.getAttributes().get("user");
        // 获取到用户发送的匹配请求
        String payload = message.getPayload();
        // 将用户发送的匹配请求---Json格式，转换为 MatchRequest （java对象）
        MatchRequest request = objectMapper.readValue(payload, MatchRequest.class);
        MatchResponse response = new MatchResponse();
        if (request.getMessage().equals("startMatch")){
            // 进入匹配对列
            matcher.add(user);
            // 把玩家信息放入匹配对列后，就可以返回一个响应给客户端了
            response.setOk(true);
            response.setMessage("startMatch");
        } else if (request.getMessage().equals("stopMatch")) {
            // 退出匹配对列
            matcher.remove(user);
            // 把玩家信息从匹配对列中移除后，就可以返回一个响应给客户端了
            response.setOk(true);
            response.setMessage("stopMatch");
        } else {
            // 非法情况
            response.setOk(false);
            response.setMessage("非法的匹配请求");
        }
        // 把响应发送给给客户端
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(response)));
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        //玩家下线
        try {
            User user = (User) session.getAttributes().get("user");
            WebSocketSession tmpSession = onlineUserManage.getFromGameHall(user.getUserId());
            // 确保当前用户是当前连接的,如果不是当前连接的，则不处理下线操作
            if (tmpSession ==  session){
                onlineUserManage.exitGameHall(user.getUserId());
            }
            // 如果玩家遭遇异常而断开websocket连接, 则需要把玩家从匹配对列中移除
            matcher.remove(user);
        } catch (NullPointerException e) {
            e.printStackTrace();
            MatchRequest request = new MatchRequest();
            request.setOk(false);
            request.setReason("用户未登录，无法进行后续匹配功能");
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(request)));
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        //玩家下线
        try {
            User user = (User) session.getAttributes().get("user");
            WebSocketSession tmpSession = onlineUserManage.getFromGameHall(user.getUserId());
            // 确保当前用户是当前连接的,如果不是当前连接的，则不处理下线操作
            if (tmpSession ==  session){
                onlineUserManage.exitGameHall(user.getUserId());
            }
            // 如果玩家遭遇异常而断开websocket连接, 则需要把玩家从匹配对列中移除
            matcher.remove(user);
        } catch (NullPointerException e) {
            e.printStackTrace();
            MatchRequest request = new MatchRequest();
            request.setOk(false);
            request.setReason("用户未登录，无法进行后续匹配功能");
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(request)));
        }
    }
}
