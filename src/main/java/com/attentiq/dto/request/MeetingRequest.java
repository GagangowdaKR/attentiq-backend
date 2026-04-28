package com.attentiq.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

public class MeetingRequest {

    @Data
    public static class Create {
        @NotBlank
        private String title;
    }

    @Data
    public static class Join {
        @NotBlank
        private String code;
    }

    @Data
    public static class UpdateThresholds {
        private Integer eyeCloseDuration;
        private Integer faceMissingDuration;
        private Boolean phoneDetectionEnabled;
    }
}
