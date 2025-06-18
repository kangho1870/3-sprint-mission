package com.sprint.mission.discodeit.exception.readStatus;

import com.sprint.mission.discodeit.dto.ErrorCode;
import com.sprint.mission.discodeit.dto.ResponseMessage;
import com.sprint.mission.discodeit.exception.DiscodeitException;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public class ReadStatusNotFoundException extends ReadStatusException {
    public ReadStatusNotFoundException(UUID readStatusId) {
        super(
                Instant.now(),
                ErrorCode.READ_STATUS_NOT_FOUND,
                Map.of("readStatusId", readStatusId)
        );
    }
}
