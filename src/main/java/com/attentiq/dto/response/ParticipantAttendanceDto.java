package com.attentiq.dto.response;

import lombok.*;

@Getter
@Setter
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParticipantAttendanceDto {
    private String meetingTitle;
    private String hostName;
    private String meetingCode;
    private Long eyesClosedCount;
    private Long faceMissingCount;
    private Long phoneDetectedCount;
}