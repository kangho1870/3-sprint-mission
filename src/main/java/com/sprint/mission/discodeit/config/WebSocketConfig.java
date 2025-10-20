package com.sprint.mission.discodeit.config;

import com.sprint.mission.discodeit.filter.JwtAuthenticationChannelInterceptor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.security.messaging.access.intercept.AuthorizationChannelInterceptor;
import org.springframework.security.messaging.access.intercept.MessageMatcherDelegatingAuthorizationManager;
import org.springframework.security.messaging.context.SecurityContextChannelInterceptor;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Slf4j
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtAuthenticationChannelInterceptor jwtAuthenticationChannelInterceptor;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/sub");
        config.setApplicationDestinationPrefixes("/pub");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
            .setAllowedOriginPatterns("*")  // 개발 환경용
            .withSockJS();
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(
            jwtAuthenticationChannelInterceptor,
            new SecurityContextChannelInterceptor(),
            authorizationChannelInterceptor()
        );
    }

    private AuthorizationChannelInterceptor authorizationChannelInterceptor() {
        log.info("[AuthorizationChannelInterceptor] 메시지 권한 검사 수행");
        return new AuthorizationChannelInterceptor(
            MessageMatcherDelegatingAuthorizationManager.builder()
//                .anyMessage().hasRole(Role.USER.name())
                // 메시지 타입 허용 추가
                .simpTypeMatchers(
                    SimpMessageType.CONNECT,
                    SimpMessageType.HEARTBEAT,
                    SimpMessageType.UNSUBSCRIBE,
                    SimpMessageType.DISCONNECT
                ).permitAll()
                .anyMessage().permitAll()
                .build()
        );
    }
}
