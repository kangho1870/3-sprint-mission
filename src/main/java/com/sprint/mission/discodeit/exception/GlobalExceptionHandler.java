package com.sprint.mission.discodeit.exception;

import com.sprint.mission.discodeit.dto.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@ResponseBody
public class GlobalExceptionHandler {

  @ExceptionHandler(DiscodeitException.class)
  public ResponseEntity<ErrorResponse> handleBaseException(DiscodeitException ex) {

    return ResponseEntity.status(ex.getErrorCode().getStatus())
            .body(new ErrorResponse(
                    ex.getTimestamp(),
                    ex.getErrorCode().toString(),
                    ex.getErrorCode().getMessage(),
                    ex.getDetails(),
                    ex.getClass().getSimpleName(),
                    ex.getErrorCode().getStatus().value()
            ));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<String> handleException(Exception e) {
    return ResponseEntity
        .status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(e.getMessage());
  }
}
