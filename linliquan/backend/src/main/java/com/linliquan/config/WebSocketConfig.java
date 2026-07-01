package com.linliquan.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket消息代理配置
 * - /ws STOMP连接端点，前端通过此端点建立长连接
 * - /topic 服务端推送消息前缀（如 /topic/notifications/{userId}）
 * - /app 客户端发送消息前缀
 *
 * 启用后可使用 SimpMessagingTemplate 向订阅了 /topic/notifications/{userId} 的客户端实时推送通知
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*");
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // 服务端推送目的地前缀：客户端订阅 /topic/** 即可接收推送
        registry.enableSimpleBroker("/topic");
        // 客户端发送消息前缀：发送到 /app/** 的消息会被路由到 @MessageMapping 方法
        registry.setApplicationDestinationPrefixes("/app");
    }
}
