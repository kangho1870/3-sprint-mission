package com.sprint.mission.discodeit.controller;

import com.sprint.mission.discodeit.service.DiscodeitUserDetails;
import com.sprint.mission.discodeit.service.SseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class SseController {

    private final SseService service;

    @GetMapping(value = "/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter connect(
            Authentication authentication,
            @RequestParam(value = "lastEventId", required = false) UUID lastEventId
    ) {
        DiscodeitUserDetails user = (DiscodeitUserDetails) authentication.getPrincipal();
        UUID receiverId = user.getUserDto().id();

        return service.connect(receiverId, lastEventId);
    }
}
