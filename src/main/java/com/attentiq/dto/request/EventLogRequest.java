package com.attentiq.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class EventLogRequest {
    @NotNull
    private Long userId;

    @NotBlank
    private String meetingId;

    @NotBlank
    private String eventType;   // EYES_CLOSED | FACE_MISSING | PHONE_DETECTED

    private String screenshotBase64;  // optional base64 image from Python AI service
}
