package cn.iocoder.gobang.config;

import cn.iocoder.gobang.api.GameAPI;
import cn.iocoder.gobang.api.MatchAPI;
import cn.iocoder.gobang.api.TestAPI;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.support.HttpSessionHandshakeInterceptor;

/**
 *  WebSocket 配置类
 */
@Configuration
@EnableWebSocket
public class JavaGobangApplication implements WebSocketConfigurer {
    @Autowired
    private TestAPI testAPI;
    @Autowired
    private MatchAPI matchAPI;
    @Autowired
    private GameAPI gameAPI;
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(testAPI, "/test");
        registry.addHandler(matchAPI, "/findMatch")
                .addInterceptors(new HttpSessionHandshakeInterceptor());
        registry.addHandler(gameAPI, "/game")
                .addInterceptors(new HttpSessionHandshakeInterceptor());
    }

}
