package com.attentiq.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class MeetingAnalyticsResponse {
    private String meetingId;
    private String title;
    private Integer totalAttendees;
    private Double avgAttentionScore;
    private Long eyesClosedCount;
    private Long faceMissingCount;
    private Long phoneDetectedCount;
    private List<EventResponse> events;
}
