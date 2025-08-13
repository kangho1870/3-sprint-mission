package com.sprint.mission.discodeit.service.basic;

import com.nimbusds.jose.JOSEException;
import com.sprint.mission.discodeit.dto.ErrorCode;
import com.sprint.mission.discodeit.dto.data.JwtDto;
import com.sprint.mission.discodeit.dto.data.UserDto;
import com.sprint.mission.discodeit.entity.Role;
import com.sprint.mission.discodeit.entity.User;
import com.sprint.mission.discodeit.exception.NotValidTokenException;
import com.sprint.mission.discodeit.exception.user.UserNotFoundException;
import com.sprint.mission.discodeit.mapper.UserMapper;
import com.sprint.mission.discodeit.provider.JwtTokenProvider;
import com.sprint.mission.discodeit.repository.UserRepository;
import com.sprint.mission.discodeit.service.AuthService;
import com.sprint.mission.discodeit.service.DiscodeitUserDetails;
import com.sprint.mission.discodeit.service.DiscodeitUserDetailsService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class BasicAuthService implements AuthService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final SessionRegistry sessionRegistry;
    private final JwtTokenProvider jwtTokenProvider;
    private final DiscodeitUserDetailsService discodeitUserDetailsService;

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    @Override
    public UserDto updateRole(UUID uuid, Role role) {
        User user = userRepository.findById(uuid)
                .orElseThrow(() -> new UserNotFoundException(uuid));

        user.updateRole(role);
        userRepository.save(user);

        List<Object> allPrincipals = sessionRegistry.getAllPrincipals();

        try {
            for (Object principal : allPrincipals) {

                DiscodeitUserDetails userDetails = (DiscodeitUserDetails) principal;
                String principalName = userDetails.getUsername();

                if (user.getUsername().equals(principalName)) {
                    List<SessionInformation> sessions = sessionRegistry.getAllSessions(principal, false);

                    for (SessionInformation session : sessions) {
                        session.expireNow();
                    }

                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return userMapper.toDto(user);
    }

    @Override
    public JwtDto refreshToken(String refreshToken, HttpServletResponse response) {

        if (refreshToken == null || !jwtTokenProvider.validateRefreshToken(refreshToken)) {
            throw new NotValidTokenException(
                    Instant.now(),
                    ErrorCode.TOKEN_NOT_VALID,
                    Map.of("details", "유효하지 않은 토큰입니다.")
            );
        }

        String username = jwtTokenProvider.getUsernameFromToken(refreshToken);

        DiscodeitUserDetails userDetails = (DiscodeitUserDetails) discodeitUserDetailsService.loadUserByUsername(username);

        JwtDto jwtDto = null;
        try {
            String newAccessToken = jwtTokenProvider.generateAccessToken(userDetails);
            String newRefreshToken = jwtTokenProvider.generateRefreshToken(userDetails);

            jwtTokenProvider.expireRefreshCookie(response);

            response.addCookie(new Cookie("REFRESH-TOKEN", newRefreshToken));
            jwtDto = new JwtDto(userDetails.getUserDto(), newAccessToken);
        } catch (JOSEException e) {
            throw new RuntimeException(e);
        }
        return jwtDto;
    }
}
