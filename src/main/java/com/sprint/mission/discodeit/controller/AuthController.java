package com.sprint.mission.discodeit.controller;

import com.sprint.mission.discodeit.dto.data.JwtDto;
import com.sprint.mission.discodeit.dto.data.UserDto;
import com.sprint.mission.discodeit.dto.request.RoleUpdateRequest;
import com.sprint.mission.discodeit.service.AuthService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

@Tag(
        name = "Auth",
        description = "Auth API"
)
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    @GetMapping("/csrf-token")
    public ResponseEntity<Void> getCsrfToken(CsrfToken csrfToken) {
        String token = csrfToken.getToken();
        log.debug("CSRF 토큰 요청: {}", token);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/role")
    public ResponseEntity<UserDto> updateRole(@RequestBody RoleUpdateRequest roleUpdateRequest) {
        UserDto userDto = authService.updateRole(roleUpdateRequest.userId(), roleUpdateRequest.newRole());

        return ResponseEntity.status(HttpStatus.OK).body(userDto);
    }

    @PostMapping("/refresh")
    public ResponseEntity<JwtDto> refreshToken(
            @CookieValue(name = "REFRESH_TOKEN", required = false) String refreshToken,
            HttpServletResponse response) {
        JwtDto jwtDto = authService.refreshToken(refreshToken, response);

        return ResponseEntity.status(HttpStatus.OK).body(jwtDto);
    }
}
