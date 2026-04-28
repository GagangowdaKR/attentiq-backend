package com.attentiq.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class HostOverviewResponse {
    private Long totalMeetings;
    private Long totalParticipants;
    private Integer avgAttentionScore;
    private List<AlertCount> topAlerts;
    private List<Integer> weeklyAttention;
    private List<MeetingResponse> recentMeetings;

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class AlertCount {
        private String type;
        private Long count;
    }
}
