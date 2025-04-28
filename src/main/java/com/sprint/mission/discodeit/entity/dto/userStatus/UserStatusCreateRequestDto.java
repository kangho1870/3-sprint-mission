package com.sprint.mission.discodeit.entity.dto.userStatus;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class UserStatusCreateRequestDto {
    private UUID userId;
    private Instant nowTime;

}
