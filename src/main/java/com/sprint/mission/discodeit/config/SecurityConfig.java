package com.sprint.mission.discodeit.config;

import com.sprint.mission.discodeit.handler.CustomAccessDeniedHandler;
import com.sprint.mission.discodeit.handler.CustomSessionExpiredStrategy;
import com.sprint.mission.discodeit.handler.LoginFailureHandler;
import com.sprint.mission.discodeit.handler.LoginSuccessHandler;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

import java.util.List;
import java.util.stream.IntStream;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           LoginSuccessHandler loginSuccessHandler,
                                           LoginFailureHandler loginFailureHandler,
                                           CustomAccessDeniedHandler accessDeniedHandler,
                                           SessionRegistry sessionRegistry) throws Exception {

        http
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
                )
                .formLogin(login -> login
                        .loginProcessingUrl("/api/auth/login")
                        .successHandler(loginSuccessHandler)
                        .failureHandler(loginFailureHandler))
                .logout(logout -> logout
                        .logoutUrl("/api/auth/logout")
                        .logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler(HttpStatus.NO_CONTENT)))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/csrf-token").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/users").permitAll()
                        .requestMatchers("/api/auth/login").permitAll()
                        .requestMatchers("/api/auth/logout").permitAll()
                        // Actuator 헬스 체크 및 메트릭 요청 허용
                        .requestMatchers("/actuator/**").permitAll()
                        // Swagger UI 및 문서 요청 허용
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/swagger-ui.html"
                        ).permitAll()

                        .requestMatchers("/api/**").authenticated()

                        .anyRequest().permitAll())
                .exceptionHandling(ex -> ex
                        .accessDeniedHandler(accessDeniedHandler))
                .sessionManagement(management -> management
                        .sessionFixation().migrateSession()
                        .sessionConcurrency(concurrency -> concurrency
                                .maximumSessions(1)
                                .maxSessionsPreventsLogin(false)
                                .sessionRegistry(sessionRegistry)
                                .expiredSessionStrategy(new CustomSessionExpiredStrategy())
                        )
                );

        return http.build();
    }

    @Bean
    public CommandLineRunner debugFilterChain(SecurityFilterChain filterChain) {

        return args -> {
            int filterSize = filterChain.getFilters().size();

            List<String> filterNames = IntStream.range(0, filterSize)
                    .mapToObj(idx -> String.format("\t[%s/%s] %s", idx + 1, filterSize,
                            filterChain.getFilters().get(idx).getClass()))
                    .toList();

            System.out.println("현재 적용된 필터 체인 목록:");
            filterNames.forEach(System.out::println);
        };
    }

    @Bean
    public RoleHierarchy roleHierarchy() {
        RoleHierarchy roleHierarchy = RoleHierarchyImpl.fromHierarchy(
                "ROLE_ADMIN > ROLE_CHANNEL_MANAGER > ROLE_USER"
        );
        return roleHierarchy;
    }

    @Bean
    static MethodSecurityExpressionHandler methodSecurityExpressionHandler(
            RoleHierarchy roleHierarchy) {
        DefaultMethodSecurityExpressionHandler handler = new DefaultMethodSecurityExpressionHandler();
        handler.setRoleHierarchy(roleHierarchy);
        return handler;
    }

    @Bean
    public SessionRegistry sessionRegistry() {

        // 세션 레지스트리 구현체를 상속받아 커스터마이징
        SessionRegistryImpl sessionRegistry = new SessionRegistryImpl() {

            // 새 세션 등록 시 추가 로깅
            @Override
            public void registerNewSession(String sessionId, Object principal) {
                System.out.println("[SessionRegistry] 새 세션 등록 - 사용자: " + principal + ", 세션ID: " + sessionId);
                super.registerNewSession(sessionId, principal);
                System.out.println("[SessionRegistry] 현재 활성 세션 수: " + getAllSessions(principal, false).size());
            }

            // 세션 제거 시 추가 로깅
            @Override
            public void removeSessionInformation(String sessionId) {
                System.out.println("[SessionRegistry] 세션 제거 - 세션ID: " + sessionId);
                super.removeSessionInformation(sessionId);
            }

            // 세션 정보 조회 시 추가 로깅
            @Override
            public SessionInformation getSessionInformation(String sessionId) {
                SessionInformation info = super.getSessionInformation(sessionId);
                if (info != null) {
                    System.out.println("[SessionRegistry] 세션 정보 조회 - 세션ID: " + sessionId + ", 만료됨: " + info.isExpired());
                }
                return info;
            }
        };

        return sessionRegistry;
    }
}
