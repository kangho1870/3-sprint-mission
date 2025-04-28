package com.sprint.mission.discodeit.entity.dto.userStatus;

import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
public class UserStatusUpdateRequestDto {

    private UUID userStatusId;
    private Instant nowTime;
}
