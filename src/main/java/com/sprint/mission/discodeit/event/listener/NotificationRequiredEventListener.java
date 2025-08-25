package com.sprint.mission.discodeit.event.listener;

import com.sprint.mission.discodeit.entity.Channel;
import com.sprint.mission.discodeit.entity.Notification;
import com.sprint.mission.discodeit.entity.ReadStatus;
import com.sprint.mission.discodeit.entity.User;
import com.sprint.mission.discodeit.event.MessageCreatedEvent;
import com.sprint.mission.discodeit.event.RoleUpdatedEvent;
import com.sprint.mission.discodeit.exception.channel.ChannelNotFoundException;
import com.sprint.mission.discodeit.exception.user.UserNotFoundException;
import com.sprint.mission.discodeit.repository.ChannelRepository;
import com.sprint.mission.discodeit.repository.NotificationRepository;
import com.sprint.mission.discodeit.repository.ReadStatusRepository;
import com.sprint.mission.discodeit.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CachePut;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationRequiredEventListener {

    private final NotificationRepository notificationRepository;
    private final ReadStatusRepository readStatusRepository;
    private final ChannelRepository channelRepository;
    private final UserRepository userRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @CachePut(value = "userNotifications", key = "#event.authorId")
    public void on(MessageCreatedEvent event) {
        Channel channel = channelRepository.findById(event.channelId()).orElseThrow(() -> new ChannelNotFoundException(event.channelId()));

        User user = userRepository.findById(event.authorId()).orElseThrow(() -> new UserNotFoundException(event.authorId()));

        String title = (channel.getName() == null)
                ? user.getUsername()
                : user.getUsername() + " (#" + channel.getName() + ")";

        List<ReadStatus> readStatuses = readStatusRepository.findAllByChannelIdAndNotificationEnabled(channel.getId(), true);

        log.info("[알림 생성 이벤트 리스너] 찾은 채팅방 알림 true User 수: {}", readStatuses.size());
        List<Notification> notifications = readStatuses.stream()
                .filter(readStatus -> !readStatus.getUser().equals(user))
                .map(readStatus -> new Notification(title, event.content(), readStatus.getUser()))
                .toList();

        log.info("[알림 생성 이벤트 리스너] 생성한 알림 수: {}", notifications.size());
        notificationRepository.saveAll(notifications);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @CachePut(value = "userNotifications", key = "#event.changedId()")
    public void on(RoleUpdatedEvent event) {
        User user = userRepository.findById(event.changedId()).orElseThrow(() -> new UserNotFoundException(event.changedId()));
        String title = "권한이 변경되었습니다.";

        String content = event.oldRole().name() + " -> " + event.newRole().name();

        Notification notification = new Notification(title, content, user);
        notificationRepository.save(notification);
    }
}
