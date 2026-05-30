package com.attentiq.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class StudentAlertDTO {
    private Long eventId;
    private String userName;
    private String eventType; // "EYES_CLOSED", "PHONE_DETECTED", "FACE_MISSING"
    private String screenshotPath;
    private String timestamp;
}