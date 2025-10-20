package com.sprint.mission.discodeit.filter;

import com.sprint.mission.discodeit.provider.JwtTokenProvider;
import com.sprint.mission.discodeit.repository.registry.JwtRegistry;
import com.sprint.mission.discodeit.service.DiscodeitUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.util.Collection;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationChannelInterceptor implements ChannelInterceptor {

    private final JwtTokenProvider jwtTokenProvider;
    private final DiscodeitUserDetailsService userDetailsService;
    private final JwtRegistry jwtRegistry;
    private final GrantedAuthoritiesMapper authoritiesMapper;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            String authorizationHeader = accessor.getFirstNativeHeader("Authorization");

            if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
                throw new AuthorizationDeniedException("Authorization header not found");
            }

            String token = authorizationHeader.substring(7);

            // JWT 토큰 자체 유효성 검증
            if (!jwtTokenProvider.validateAccessToken(token)) {
                throw new AuthorizationDeniedException("Invalid JWT token");
            }

            // Redis에서 토큰 존재 여부 검증 (분산환경 동기화)
            if (!jwtRegistry.hasActiveJwtInformationByAccessToken(token)) {
                throw new AuthorizationDeniedException("Token not found in Redis registry");
            }

            String username = jwtTokenProvider.getUsernameFromToken(token);

            UserDetails userDetails = userDetailsService.loadUserByUsername(username);

            Collection<? extends GrantedAuthority> mapped = authoritiesMapper.mapAuthorities(
                    userDetails.getAuthorities());

            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(userDetails, null, mapped);
            accessor.setUser(authentication);
        }
        return message;
    }
}
