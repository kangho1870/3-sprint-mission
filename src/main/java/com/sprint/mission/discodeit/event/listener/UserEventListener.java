package com.sprint.mission.discodeit.event.listener;

import com.sprint.mission.discodeit.dto.data.UserDto;
import com.sprint.mission.discodeit.entity.User;
import com.sprint.mission.discodeit.event.UserEvent;
import com.sprint.mission.discodeit.exception.user.UserNotFoundException;
import com.sprint.mission.discodeit.mapper.UserMapper;
import com.sprint.mission.discodeit.repository.UserRepository;
import com.sprint.mission.discodeit.service.SseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@RequiredArgsConstructor
@Slf4j
public class UserEventListener {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final SseService sseService;

    @Async("notificationTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleUserEvent(UserEvent userEvent) {
        UserDto dto = null;
        String eventName = "users." + userEvent.type();

        if (!userEvent.type().equals("deleted")) {
            User user = userRepository.findById(userEvent.userId()).orElseThrow(() -> new UserNotFoundException(userEvent.userId()));
            dto = userMapper.toDto(user);
            sseService.broadcast(eventName, dto);
        } else {
            dto = new  UserDto(userEvent.userId(), null, null, null, null, null);
            sseService.broadcast(eventName, dto);
        }

    }
}
