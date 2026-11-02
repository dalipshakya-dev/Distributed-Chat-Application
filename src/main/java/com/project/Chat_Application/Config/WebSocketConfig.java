package com.project.Chat_Application.Config;

import com.project.Chat_Application.Observability.StompCorrelationInterceptor;
import com.project.Chat_Application.Security.StompAuthInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final StompAuthInterceptor stompAuthInterceptor;
    private final StompCorrelationInterceptor stompCorrelationInterceptor;

    public WebSocketConfig(StompAuthInterceptor stompAuthInterceptor,
                           StompCorrelationInterceptor stompCorrelationInterceptor) {
        this.stompAuthInterceptor = stompAuthInterceptor;
        this.stompCorrelationInterceptor = stompCorrelationInterceptor;
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("http://localhost:5000")
                .withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.setApplicationDestinationPrefixes("/app");
        registry.enableSimpleBroker("/topic");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(stompCorrelationInterceptor, stompAuthInterceptor);
    }
}