package com.sprint.mission.discodeit.event.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JOSEException;
import com.sprint.mission.discodeit.dto.data.ChannelDto;
import com.sprint.mission.discodeit.dto.data.JwtInformation;
import com.sprint.mission.discodeit.dto.data.NotificationDto;
import com.sprint.mission.discodeit.dto.data.UserDto;
import com.sprint.mission.discodeit.entity.Channel;
import com.sprint.mission.discodeit.entity.Notification;
import com.sprint.mission.discodeit.entity.ReadStatus;
import com.sprint.mission.discodeit.entity.User;
import com.sprint.mission.discodeit.event.*;
import com.sprint.mission.discodeit.exception.channel.ChannelNotFoundException;
import com.sprint.mission.discodeit.exception.user.UserNotFoundException;
import com.sprint.mission.discodeit.mapper.ChannelMapper;
import com.sprint.mission.discodeit.mapper.UserMapper;
import com.sprint.mission.discodeit.provider.JwtTokenProvider;
import com.sprint.mission.discodeit.repository.ChannelRepository;
import com.sprint.mission.discodeit.repository.NotificationRepository;
import com.sprint.mission.discodeit.repository.ReadStatusRepository;
import com.sprint.mission.discodeit.repository.UserRepository;
import com.sprint.mission.discodeit.repository.registry.JwtRegistry;
import com.sprint.mission.discodeit.service.SseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Component
public class NotificationRequiredTopicListener {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final ChannelRepository channelRepository;
    private final ReadStatusRepository readStatusRepository;
    private final ObjectMapper objectMapper;
    private final CacheManager cacheManager;
    private final SseService sseService;
    private final JwtRegistry jwtRegistry;
    private final UserMapper userMapper;
    private final ChannelMapper channelMapper;
    private final JwtTokenProvider jwtTokenProvider;
    private final SimpMessagingTemplate simpMessagingTemplate;

    @KafkaListener(
            topics = "discodeit.MessageCreatedEvent",
            groupId = "discodeit-broadcast-${random.uuid}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onMessageCreatedEvent(String kafkaEvent) {
        log.info("[NotificationRequiredTopicListener] Kafka 메시지 수신: {}", kafkaEvent);
        try {
            MessageCreatedEvent event = objectMapper.readValue(kafkaEvent, MessageCreatedEvent.class);
            log.info("[NotificationRequiredTopicListener] MessageCreatedEvent 파싱 완료: channelId={}, authorId={}", event.channelId(), event.authorId());

            Channel channel = channelRepository.findById(event.channelId()).orElseThrow(() -> new ChannelNotFoundException(event.channelId()));

            User user = userRepository.findById(event.authorId()).orElseThrow(() -> new UserNotFoundException(event.authorId()));

            String title = (channel.getName() == null)
                    ? user.getUsername()
                    : user.getUsername() + " (#" + channel.getName() + ")";

            List<ReadStatus> readStatuses = readStatusRepository.findAllByChannelIdAndNotificationEnabled(channel.getId(), true);

            log.info("[알림 생성 이벤트 리스너] 찾은 채팅방 알림 true User 수: {}", readStatuses.size());
            List<Notification> notifications = readStatuses.stream()
                    .filter(readStatus -> !readStatus.getUser().getId().equals(user.getId()))
                    .map(readStatus -> new Notification(title, event.message().content(), readStatus.getUser()))
                    .toList();

            log.info("[알림 생성 이벤트 리스너] 생성한 알림 수: {}", notifications.size());
            notificationRepository.saveAll(notifications);

            // 캐시 무효화
            Cache cache = cacheManager.getCache("userNotifications");
            if (cache != null) {
                notifications.forEach(n -> cache.evict(n.getUser().getId()));
            }

            for (Notification n : notifications) {
                NotificationDto dto = new NotificationDto(n.getId(), n.getCreatedAt(), n.getUser().getId(), n.getTitle(), n.getContent());
                log.info("userId: {}", n.getUser().getId());
                sseService.send(List.of(n.getUser().getId()), "notifications.created", dto);
            }

            // WebSocket 브로드캐스트는 WebsocketRequiredEventListener에서 처리
            log.info("[NotificationRequiredTopicListener] 알림 처리 완료: channelId={}, authorId={}", event.channelId(), event.authorId());
        } catch (JsonMappingException e) {
            throw new RuntimeException(e);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }


    @KafkaListener(
            topics = "discodeit.RoleUpdatedEvent",
            groupId = "discodeit-broadcast",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onRoleUpdatedEvent(String kafkaEvent) {

        try {
            RoleUpdatedEvent event = objectMapper.readValue(kafkaEvent, RoleUpdatedEvent.class);

            User user = userRepository.findById(event.changedId()).orElseThrow(() -> new UserNotFoundException(event.changedId()));
            String title = "권한이 변경되었습니다.";

            String content = event.oldRole().name() + " -> " + event.newRole().name();

            Notification notification = new Notification(title, content, user);
            notificationRepository.save(notification);

            // 캐시 무효화
            Cache cache = cacheManager.getCache("users");
            if (cache != null) {
                cache.evict(notification.getUser().getId());
            }

            NotificationDto dto = new NotificationDto(notification.getId(), notification.getCreatedAt(), notification.getUser().getId(), notification.getTitle(), notification.getContent());
            sseService.send(List.of(notification.getUser().getId()), "notifications.created", dto);
        } catch (JsonMappingException e) {
            throw new RuntimeException(e);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @KafkaListener(
            topics = "discodeit.S3UploadFailedEvent",
            groupId = "discodeit-broadcast",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onS3UploadFailedEvent(String kafkaEvent) {
        try {
            S3UploadFailedEvent event = objectMapper.readValue(kafkaEvent, S3UploadFailedEvent.class);

            List<Notification> notifications = event.users().stream()
                    .map(user -> {
                        return new Notification(
                                "S3 파일 업로드 실패",
                                "RequestsId: " + event.requestId() + "BinaryContentId: " + event.binaryContentId() + "Error: " + event.errorMessage(),
                                user
                        );
                    })
                    .toList();
            notificationRepository.saveAll(notifications);
        } catch (JsonMappingException e) {
            throw new RuntimeException(e);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @KafkaListener(
            topics = "discodeit.UserLogInOutEvent",
            groupId = "discodeit-broadcast",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onUserLogInOutEvent(String kafkaEvent) {
        try {
            UserLogInOutEvent userLogInOutEvent = objectMapper.readValue(kafkaEvent, UserLogInOutEvent.class);

            User user = userRepository.findById(userLogInOutEvent.userId()).orElseThrow(() -> new UserNotFoundException(userLogInOutEvent.userId()));
            UserDto dto = userMapper.toDto(user);
            if (userLogInOutEvent.isLogin()) {
                jwtRegistry.register(
                        new JwtInformation(
                                dto,
                                jwtTokenProvider.generateAccessToken(dto),
                                jwtTokenProvider.generateRefreshToken(dto)
                        )
                );

                sseService.broadcast("users.login", userLogInOutEvent);

            } else {
                jwtRegistry.inValidJwtInformationByUserId(userLogInOutEvent.userId());

                sseService.broadcast("users.logout", userLogInOutEvent);
            }

        } catch (JsonMappingException e) {
            throw new RuntimeException(e);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        } catch (JOSEException e) {
            throw new RuntimeException(e);
        }
    }

    @KafkaListener(
            topics = "discodeit.UserEvent",
            groupId = "discodeit-broadcast-${random.uuid}", // 각 앱마다 고유한 그룹 ID
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeUserEvent(String kafkaEvent) {
        log.info("[NotificationRequiredTopicListener] UserEvent Kafka 메시지 수신: {}", kafkaEvent);
        try {
            UserEvent userEvent = objectMapper.readValue(kafkaEvent, UserEvent.class);
            System.out.println("카프카 이벤트 리스너 동작(유저생성): " + userEvent);
            log.info("[NotificationRequiredTopicListener] 사용자 이벤트 수신: type={}, userId={}", userEvent.type(), userEvent.userId());

            UserDto dto = null;
            String eventName = "users." + userEvent.type();

            if (!userEvent.type().equals("deleted")) {
                UUID userId = userEvent.userId();
                User user = userRepository.findById(userId)
                        .orElseThrow(() -> new UserNotFoundException(userId));
                dto = userMapper.toDto(user);
                log.info("[NotificationRequiredTopicListener] 사용자 정보 조회 완료: {}", dto);
            } else {
                dto = new UserDto(userEvent.userId(), null, null, null, null, null);
            }

            // 로컬 SSE 연결에 전송
            sseService.broadcast(eventName, dto);
            log.info("[NotificationRequiredTopicListener] SSE 이벤트 전송 완료: eventName={}, dto={}", eventName, dto);
            log.info("[NotificationRequiredTopicListener] 현재 앱 인스턴스에서 SSE 브로드캐스트 실행됨");
        } catch (JsonMappingException e) {
            log.error("[NotificationRequiredTopicListener] UserEvent JSON 매핑 오류: {}", e.getMessage(), e);
        } catch (JsonProcessingException e) {
            log.error("[NotificationRequiredTopicListener] UserEvent JSON 처리 오류: {}", e.getMessage(), e);
        } catch (Exception e) {
            log.error("[NotificationRequiredTopicListener] 이벤트 처리 실패: {}", e.getMessage(), e);
        }
    }

    @KafkaListener(
            topics = "discodeit.ChannelEvent",
            groupId = "discodeit-broadcast-${random.uuid}", // 각 앱마다 고유한 그룹 ID
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleChannelEvent(String kafkaEvent) {
        log.info("[NotificationRequiredTopicListener] ChannelEvent Kafka 메시지 수신: {}", kafkaEvent);
        try {
            ChannelEvent event = objectMapper.readValue(kafkaEvent, ChannelEvent.class);
            log.info("[NotificationRequiredTopicListener] ChannelEvent 수신: type={}, channelId={}", event.type(), event.channelId());
            
            ChannelDto dto = null;
            String eventName = "channels." + event.type();

            if (!event.type().equals("deleted")) {
                Channel channel = channelRepository.findById(event.channelId()).orElseThrow(() -> new ChannelNotFoundException(event.channelId()));
                dto = channelMapper.toDto(channel);
                log.info("[NotificationRequiredTopicListener] Channel 정보 조회 완료: {}", dto);
            } else {
                dto = new ChannelDto(event.channelId(), null, null, null, null, null);
            }
            
            sseService.broadcast(eventName, dto);
            log.info("[NotificationRequiredTopicListener] ChannelEvent SSE 전송 완료: eventName={}, dto={}", eventName, dto);
        } catch (JsonMappingException e) {
            log.error("[NotificationRequiredTopicListener] ChannelEvent JSON 매핑 오류: {}", e.getMessage(), e);
        } catch (JsonProcessingException e) {
            log.error("[NotificationRequiredTopicListener] ChannelEvent JSON 처리 오류: {}", e.getMessage(), e);
        } catch (Exception e) {
            log.error("[NotificationRequiredTopicListener] ChannelEvent 처리 실패: {}", e.getMessage(), e);
        }
    }

}

