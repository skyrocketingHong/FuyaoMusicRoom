package ninja.skyrocketing.fuyao.musicroom.configuration;

import ninja.skyrocketing.fuyao.musicroom.handler.FuyaoMusicRoomWebSocketHandlerDecoratorFactory;
import ninja.skyrocketing.fuyao.musicroom.interceptor.FuyaoMusicRoomWebSocketHandshakeInterceptor;
import ninja.skyrocketing.fuyao.musicroom.interceptor.FuyaoMusicRoomWebSocketInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;

/**
 * @author skyrocketing Hong
 */
@Configuration
@EnableWebSocketMessageBroker
public class FuyaoMusicRoomWebSocketConfiguration implements WebSocketMessageBrokerConfigurer {
    @Autowired
    private FuyaoMusicRoomWebSocketInterceptor fuyaoMusicRoomWebSocketInterceptor;
    @Autowired
    private FuyaoMusicRoomWebSocketHandshakeInterceptor fuyaoMusicRoomWebSocketHandshakeInterceptor;
    @Autowired
    private FuyaoMusicRoomWebSocketHandlerDecoratorFactory fuyaoMusicRoomWebSocketHandlerDecoratorFactory;

    /**
     * 有关客户端建立连接的部分
     *
     * @param registry StompEndpointRegistry
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry
                // 配置连接前缀，客户端建立连接时：localhost:port/server
                .addEndpoint("/server")
                // 添加拦截器
                .addInterceptors(fuyaoMusicRoomWebSocketHandshakeInterceptor)
                // 允许所有域
                .setAllowedOriginPatterns("*")
                // 支持以 SockJs 的方式建立连接，这是一个备选方案，在 WebSocket 不可用的时候启用
                .withSockJS();
    }

    /**
     * 消息入口通道相关配置
     *
     * @param registration
     */
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(fuyaoMusicRoomWebSocketInterceptor);
    }

    @Override
    public void configureWebSocketTransport(WebSocketTransportRegistration registry) {
        // 加入自定义的 handler
        registry.addDecoratorFactory(fuyaoMusicRoomWebSocketHandlerDecoratorFactory);
    }
}
