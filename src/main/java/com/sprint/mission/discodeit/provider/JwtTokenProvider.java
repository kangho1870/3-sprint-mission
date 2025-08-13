package com.sprint.mission.discodeit.provider;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.sprint.mission.discodeit.service.DiscodeitUserDetails;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.Date;
import java.util.UUID;

@Component
@Slf4j
public class JwtTokenProvider {

    // 토큰 만료 시간
    private final int accessTokenExpirationMs;
    // 리프레시 토큰 만료 시간
    private final int refreshTokenExpirationMs;

    // 토큰을 서명하기 위한 서명자
    private final JWSSigner accessTokenSigner;
    // 토큰의 서명을 검증하기 위한 검증자
    private final JWSVerifier accessTokenVerifier;
    // 리프레시 토큰을 서명하기 위한 서명자
    private final JWSSigner refreshTokenSigner;
    // 리프레시 토큰의 서명을 검증하기 위한 검증자
    private final JWSVerifier refreshTokenVerifier;

    public JwtTokenProvider(
            @Value("${jwt.access-token.secret}") String accessTokenSecret,
            @Value("${jwt.access-token.exp}") int accessTokenExpirationMs,
            @Value("${jwt.refresh-token.secret}") String refreshTokenSecret,
            @Value("${jwt.refresh-token.exp}") int refreshTokenExpirationMs
    ) throws JOSEException {
        this.accessTokenExpirationMs = accessTokenExpirationMs;
        this.refreshTokenExpirationMs = refreshTokenExpirationMs;

        byte[] accessSecretBytes = accessTokenSecret.getBytes(StandardCharsets.UTF_8);
        this.accessTokenSigner = new MACSigner(accessSecretBytes);
        this.accessTokenVerifier = new MACVerifier(accessSecretBytes);

        byte[] refreshSecretBytes = refreshTokenSecret.getBytes(StandardCharsets.UTF_8);
        this.refreshTokenSigner = new MACSigner(refreshSecretBytes);
        this.refreshTokenVerifier = new MACVerifier(refreshSecretBytes);
    }

    public String generateAccessToken(DiscodeitUserDetails discodeitUserDetails) throws JOSEException {

        log.info("[TokenProvider] generateAccessToken 호출됨 엑세스 토큰 생성 호출. 호출자 {}", discodeitUserDetails.getUsername());

        // 생성할 토큰의 타입이 "access"인 경우 액세스 토큰을 생성한다.
        return generateToken(discodeitUserDetails, accessTokenExpirationMs, accessTokenSigner, "access");
    }

    public String generateRefreshToken(DiscodeitUserDetails discodeitUserDetails) throws JOSEException {

        System.out.println("[TokenProvider] generateRefreshToken 호출됨: " + discodeitUserDetails.getUsername() + "의 리프레시 토큰 생성");

        // 생성할 토큰의 타입이 "refresh"인 경우 리프레시 토큰을 생성한다.
        return generateToken(discodeitUserDetails, refreshTokenExpirationMs, refreshTokenSigner, "refresh");
    }

    public boolean validateAccessToken(String accessToken) {
        log.info("[TokenProvider] 토큰 검증 호출 : {}", accessToken);

        return verifyToken(accessToken, accessTokenVerifier, "access");
    }

    public boolean validateRefreshToken(String accessToken) {
        log.info("[TokenProvider] 리프레시 토큰 검증 호출 : {}", accessToken);

        return verifyToken(accessToken, refreshTokenVerifier, "refresh");
    }

    private boolean verifyToken(String token, JWSVerifier verifier, String expectedType) {

        try {
            // 토큰 파싱
            SignedJWT signedJWT = SignedJWT.parse(token);

            // 서명 검증
            if (!signedJWT.verify(verifier)) {
                log.error("[TokenProvider] 서명 검증 실패");
                return false;
            }

            // 토큰 타입 검증
            String tokenType = (String) signedJWT.getJWTClaimsSet().getClaim("type");
            if (!expectedType.equals(tokenType)) {
                log.error("[TokenProvider] verifyToken: 타입 불일치 - expected= {}, actual={}", expectedType, tokenType);
                return false;
            }

            // 만료 시간 검증
            Date exp = signedJWT.getJWTClaimsSet().getExpirationTime();

            return exp != null && exp.after(new Date());
        } catch (ParseException | JOSEException e) {
            throw new RuntimeException(e);
        }
    }

    private String generateToken(DiscodeitUserDetails discodeitUserDetails, int expirationMs, JWSSigner signer, String type ) throws JOSEException {

        // 토큰 식별자 생성
        String tokenId = UUID.randomUUID().toString();

        // 현재 시간을 기준으로 만료 시간 설정
        Date now = new Date();
        Date expiration = new Date(now.getTime() + expirationMs);

        // 토큰의 클레임 설정
        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                // 토큰 주체(subject)
                .subject(discodeitUserDetails.getUsername())
                // 토큰 식별자(jti)
                .jwtID(tokenId)
                // userId
                .claim("userId", discodeitUserDetails.getUserDto().id())
                // 토큰 타입
                .claim("type", type)
                // 사용자 권한
                .claim("roles",
                        discodeitUserDetails.getAuthorities()
                                .stream()
                                .map(GrantedAuthority::getAuthority)
                                .toList()
                )
                // 토큰 발급 시간(iat)
                .issueTime(now)
                .expirationTime(expiration)
                .build();

        // 토큰 생성 (아직 서명은 안됨)
        SignedJWT signedJWT = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claimsSet);

        // 이제 토큰 서명
        signedJWT.sign(signer);

        // 토큰 직렬화 : JWT 토큰을 생성한 후 URL 안전한 문자열(Base64 인코딩)로 직렬화
        String completedJWT = signedJWT.serialize();

        log.info("[TokenProvider] 토큰 생성 완료 {}", completedJWT);
        return completedJWT;
    }

    public String getUsernameFromToken(String token) {
        try {
            log.info("[TokenProvider] getUsernameFromToken 호출됨: subject 추출 시작");

            SignedJWT signedJWT = SignedJWT.parse(token);
            String subject = signedJWT.getJWTClaimsSet().getSubject();

            log.info("[TokenProvider] getUsernameFromToken 결과: subject= {}", subject);

            return subject;
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid JWT token", e);
        }
    }

    public void expireRefreshCookie(HttpServletResponse response) {

        System.out.println("[TokenProvider] expireRefreshCookie 호출됨: 만료 쿠키 응답에 추가");
        Cookie cookie = generateRefreshTokenExpirationCookie();

        response.addCookie(cookie);
    }

    public Cookie generateRefreshTokenExpirationCookie() {

        System.out.println("[TokenProvider] generateRefreshTokenExpirationCookie 호출됨: Refresh Token 만료 쿠키 생성");

        Cookie cookie = new Cookie("REFRESH-TOKEN", "");

        cookie.setHttpOnly(true);
        cookie.setSecure(false);
        cookie.setPath("/");
        cookie.setMaxAge(0);		// 쿠키 만료 시간을 0으로 설정하여 즉시 만료시킨다

        System.out.println("[TokenProvider] generateRefreshTokenExpirationCookie 완료");

        return cookie;
    }
}
