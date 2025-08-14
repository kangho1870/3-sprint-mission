package com.sprint.mission.discodeit.handler;

import com.sprint.mission.discodeit.provider.JwtTokenProvider;
import com.sprint.mission.discodeit.repository.registry.JwtRegistry;
import com.sprint.mission.discodeit.service.DiscodeitUserDetails;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtLogoutHandler implements LogoutHandler {

    private final JwtTokenProvider jwtTokenProvider;
    private final JwtRegistry jwtRegistry;

    @Override
    public void logout(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        log.info("[JwtLogoutHandler] logout 호출 됨");
        
        // 1. SecurityContext 초기화
        SecurityContextHolder.clearContext();
        
        // 2. Access 토큰을 블랙리스트에 추가
        String authorization = request.getHeader("Authorization");
        if (authorization != null && authorization.startsWith("Bearer ")) {
            String accessToken = authorization.substring(7);
            jwtTokenProvider.blacklistAccessToken(accessToken);
            log.info("[JwtLogoutHandler] Access 토큰을 블랙리스트에 추가");
        }
        
        // 3. Refresh 토큰을 블랙리스트에 추가
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("REFRESH_TOKEN".equals(cookie.getName()) && cookie.getValue() != null && !cookie.getValue().isEmpty()) {
                    jwtTokenProvider.blacklistRefreshToken(request.getHeader("REFRESH_TOKEN"));
                    break;
                }
            }
        }
        
        // 4. Refresh 토큰 쿠키 만료
        jwtTokenProvider.expireRefreshCookie(response);
        
        // 5. Registry에서 사용자 정보 무효화
        if (authentication != null && authentication.getPrincipal() instanceof DiscodeitUserDetails user) {
            jwtRegistry.inValidJwtInformationByUserId(user.getUserDto().id());
        }
        
        log.info("[JwtLogoutHandler] 로그아웃 처리 완료 - 모든 인증 정보 정리됨");
    }
}
