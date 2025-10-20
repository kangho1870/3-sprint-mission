package com.sprint.mission.discodeit.repository.registry;

import com.sprint.mission.discodeit.dto.data.JwtInformation;
import com.sprint.mission.discodeit.event.UserLogInOutEvent;
import com.sprint.mission.discodeit.provider.JwtTokenProvider;
import com.sprint.mission.discodeit.redis.RedisLockProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisJwtRegistry implements JwtRegistry {

    private static final String USER_JWT_KEY_PREFIX = "jwt:user:";
    private static final String ACCESS_TOKEN_INDEX_KEY = "jwt:access_tokens";
    private static final String REFRESH_TOKEN_INDEX_KEY = "jwt:refresh_tokens";
    private static final Duration DEFAULT_TTL = Duration.ofMinutes(30);

    private final int maxActiveJwtCount = 1;
    private final JwtTokenProvider jwtTokenProvider;
    private final ApplicationEventPublisher eventPublisher;
    private final RedisTemplate<String, Object> redisTemplate;
    private final RedisTemplate<String, String> customStringRedisTemplate;
    private final RedisLockProvider redisLockProvider;

    @CacheEvict(value = "users", key = "'all'")
    @Retryable(retryFor = RedisLockProvider.RedisLockAcquisitionException.class, maxAttempts = 10,
            backoff = @Backoff(delay = 100, multiplier = 2))
    @Override
    public void register(JwtInformation jwtInformation) {
        String userKey = getUserKey(jwtInformation.userDto().id());
        String lockKey = jwtInformation.userDto().id().toString();

        redisLockProvider.acquireLock(lockKey);
        try {
            Long currentSize = redisTemplate.opsForList().size(userKey);

            while (currentSize != null && currentSize >= maxActiveJwtCount) {
                Object oldestTokenObj = redisTemplate.opsForList().leftPop(userKey);
                if (oldestTokenObj instanceof JwtInformation oldestToken) {
                    removeTokenIndex(oldestToken.accessToken(), oldestToken.refreshToken());
                }
                currentSize = redisTemplate.opsForList().size(userKey);
            }

            redisTemplate.opsForList().rightPush(userKey, jwtInformation);
            redisTemplate.expire(userKey, DEFAULT_TTL);
            addTokenIndex(jwtInformation.accessToken(), jwtInformation.refreshToken());

        } finally {
            redisLockProvider.releaseLock(lockKey);
        }

        eventPublisher.publishEvent(
                new UserLogInOutEvent(jwtInformation.userDto().id(), true)
        );
    }

    @CacheEvict(value = "users", key = "'all'")
    @Override
    public void inValidJwtInformationByUserId(UUID userId) {
        String userKey = getUserKey(userId);

        List<Object> tokens = redisTemplate.opsForList().range(userKey, 0, -1);
        if (tokens != null) {
            tokens.forEach(tokenObj -> {
                if (tokenObj instanceof JwtInformation jwtInfo) {
                    removeTokenIndex(jwtInfo.accessToken(), jwtInfo.refreshToken());
                }
            });
        }

        redisTemplate.delete(userKey);
        eventPublisher.publishEvent(new UserLogInOutEvent(userId, false));
    }

    @Override
    public boolean hasActiveJwtInformationByUserId(UUID userId) {
        String userKey = getUserKey(userId);
        Long size = redisTemplate.opsForList().size(userKey);
        return size != null && size > 0;
    }

    @Override
    public boolean hasActiveJwtInformationByAccessToken(String accessToken) {
        boolean existsInRedis = Boolean.TRUE.equals(
            customStringRedisTemplate.opsForSet().isMember(ACCESS_TOKEN_INDEX_KEY, accessToken)
        );

        if (!existsInRedis) return false;

        boolean isValidJwt = jwtTokenProvider.validateAccessToken(accessToken);
        if (!isValidJwt) {
            customStringRedisTemplate.opsForSet().remove(ACCESS_TOKEN_INDEX_KEY, accessToken);
        }
        return isValidJwt;
    }

    @Override
    public boolean hasActiveJwtInformationByRefreshToken(String refreshToken) {
        boolean existsInRedis = Boolean.TRUE.equals(
                customStringRedisTemplate.opsForSet().isMember(REFRESH_TOKEN_INDEX_KEY, refreshToken)
        );

        if (!existsInRedis) return false;

        boolean isValidJwt = jwtTokenProvider.validateRefreshToken(refreshToken);
        if (!isValidJwt) {
            customStringRedisTemplate.opsForSet().remove(REFRESH_TOKEN_INDEX_KEY, refreshToken);
        }
        return isValidJwt;
    }

    @Retryable(retryFor = RedisLockProvider.RedisLockAcquisitionException.class, maxAttempts = 10,
            backoff = @Backoff(delay = 100, multiplier = 2))
    @Override
    public JwtInformation rotateJwtInformation(String refreshToken, JwtInformation newJwtInformation) {
        String userKey = getUserKey(newJwtInformation.userDto().id());
        String lockKey = newJwtInformation.userDto().id().toString();

        redisLockProvider.acquireLock(lockKey);
        try {
            List<Object> tokens = redisTemplate.opsForList().range(userKey, 0, -1);

            if (tokens != null) {
                for (int i = 0; i < tokens.size(); i++) {
                    if (tokens.get(i) instanceof JwtInformation jwtInfo &&
                            jwtInfo.refreshToken().equals(refreshToken)) {

                        removeTokenIndex(jwtInfo.accessToken(), jwtInfo.refreshToken());

                        jwtInfo.rotate(newJwtInformation.accessToken(),
                                newJwtInformation.refreshToken());
                        redisTemplate.opsForList().set(userKey, i, jwtInfo);

                        addTokenIndex(newJwtInformation.accessToken(),
                                newJwtInformation.refreshToken());
                        redisTemplate.expire(userKey, DEFAULT_TTL);
                        break;
                    }
                }
            }
        } finally {
            redisLockProvider.releaseLock(lockKey);
        }
        return newJwtInformation;
    }

    @Scheduled(fixedDelay = 1000 * 60 * 5)
    @Override
    public void clearExpiredJwtInformation() {
        Set<String> userKeys = redisTemplate.keys(USER_JWT_KEY_PREFIX + "*");

        for (String userKey : userKeys) {
            List<Object> tokens = redisTemplate.opsForList().range(userKey, 0, -1);

            if (tokens != null) {
                boolean hasValidTokens = false;

                for (int i = tokens.size() - 1; i >= 0; i--) {
                    if (tokens.get(i) instanceof JwtInformation jwtInfo) {
                        boolean isExpired =
                                !jwtTokenProvider.validateAccessToken(jwtInfo.accessToken()) ||
                                        !jwtTokenProvider.validateRefreshToken(jwtInfo.refreshToken());

                        if (isExpired) {
                            redisTemplate.opsForList().set(userKey, i, "EXPIRED");
                            redisTemplate.opsForList().remove(userKey, 1, "EXPIRED");
                            removeTokenIndex(jwtInfo.accessToken(), jwtInfo.refreshToken());
                        } else {
                            hasValidTokens = true;
                        }
                    }
                }

                if (!hasValidTokens) {
                    redisTemplate.delete(userKey);
                }
            }
        }
    }

    private String getUserKey(UUID userId) {
        return USER_JWT_KEY_PREFIX + userId.toString();
    }

    private void addTokenIndex(String accessToken, String refreshToken) {
        customStringRedisTemplate.opsForSet().add(ACCESS_TOKEN_INDEX_KEY, accessToken);
        customStringRedisTemplate.opsForSet().add(REFRESH_TOKEN_INDEX_KEY, refreshToken);

        customStringRedisTemplate.expire(ACCESS_TOKEN_INDEX_KEY, DEFAULT_TTL);
        customStringRedisTemplate.expire(REFRESH_TOKEN_INDEX_KEY, DEFAULT_TTL);
    }

    private void removeTokenIndex(String accessToken, String refreshToken) {
        customStringRedisTemplate.opsForSet().remove(ACCESS_TOKEN_INDEX_KEY, accessToken);
        customStringRedisTemplate.opsForSet().remove(REFRESH_TOKEN_INDEX_KEY, refreshToken);
    }
}
