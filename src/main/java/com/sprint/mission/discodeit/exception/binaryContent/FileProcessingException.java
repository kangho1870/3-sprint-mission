package com.sprint.mission.discodeit.exception.binaryContent;

import com.sprint.mission.discodeit.dto.ErrorCode;
import com.sprint.mission.discodeit.dto.ResponseMessage;
import com.sprint.mission.discodeit.exception.DiscodeitException;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.Map;

public class FileProcessingException extends BinaryContentException {
    public FileProcessingException() {
        super(
                Instant.now(),
                ErrorCode.FILE_PROCESSING_ERROR,
                Map.of("message", ResponseMessage.FILE_PROCESSING_ERROR)
        );
    }
}
