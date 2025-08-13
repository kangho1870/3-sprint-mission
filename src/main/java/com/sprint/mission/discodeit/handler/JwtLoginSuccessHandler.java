package com.sprint.mission.discodeit.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JOSEException;
import com.sprint.mission.discodeit.dto.data.JwtDto;
import com.sprint.mission.discodeit.dto.data.UserDto;
import com.sprint.mission.discodeit.provider.JwtTokenProvider;
import com.sprint.mission.discodeit.service.DiscodeitUserDetails;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtLoginSuccessHandler implements AuthenticationSuccessHandler {

    private final ObjectMapper objectMapper;
    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        log.info("[JwtLoginSuccessHandler] onAuthenticationSuccess 호출");


        // response encoding / contentType 설정 및 응답구조 설정
        response.setCharacterEncoding("UTF-8");
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        // Principal 유효성 확인 및 캐스팅
        if (authentication.getPrincipal() instanceof DiscodeitUserDetails discodeitUserDetails) {
            try {
                UserDto userDto = discodeitUserDetails.getUserDto();

                // 새로운 accessToken, refreshToken 발급
                String accessToken = jwtTokenProvider.generateAccessToken(discodeitUserDetails);
                String refreshToken = jwtTokenProvider.generateRefreshToken(discodeitUserDetails);

                JwtDto jwtDto = new JwtDto(userDto, accessToken);
                response.setStatus(HttpServletResponse.SC_OK);
                response.getWriter().write(objectMapper.writeValueAsString(jwtDto));
                response.addCookie(new Cookie("REFRESH-TOKEN", refreshToken));
            } catch (JOSEException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
