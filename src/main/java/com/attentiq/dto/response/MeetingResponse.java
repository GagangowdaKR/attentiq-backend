package com.attentiq.dto.response;

import com.attentiq.entity.Meeting;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class MeetingResponse {
    private String meetingId;
    private String code;
    private String title;
    private String status;
    private Long hostId;
    private String hostName;
    private LocalDateTime createdAt;
    private LocalDateTime endedAt;
    private Integer participantCount;
    private Double avgAttention;
    private Integer alertCount;
    private String date;

    public static MeetingResponse from(Meeting meeting) {
        return  builder()
                .code(meeting.getCode())
                .status(meeting.getStatus().toString())
                .createdAt(meeting.getCreatedAt())
                .title(meeting.getTitle())
                .hostName(meeting.getHost().getName())
                .date(meeting.getCreatedAt().toLocalDate().toString())
                .build();
    }
}
