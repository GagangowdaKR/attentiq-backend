package com.attentiq.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.util.List;

@Data
@AllArgsConstructor
public class DetailedHistoryDTO {
    private Long meetingId;
    private String title;
    private String code;
    private String status; // "ACTIVE" or "ENDED"
    private List<StudentAlertDTO> flaggedStudents;
}