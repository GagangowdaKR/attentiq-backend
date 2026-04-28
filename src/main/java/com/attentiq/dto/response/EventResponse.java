package com.attentiq.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class EventResponse {
    private Long id;
    private String userName;
    private Long userId;
    private String meetingId;
    private String eventType;
    private String screenshotUrl;
    private LocalDateTime timestamp;
    private Boolean acknowledged;
}
