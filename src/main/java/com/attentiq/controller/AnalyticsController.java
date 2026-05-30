package com.attentiq.controller;

import com.attentiq.dto.response.HostOverviewResponse;
import com.attentiq.dto.response.MeetingAnalyticsResponse;
import com.attentiq.dto.response.ParticipantAttendanceDto;
import com.attentiq.entity.User;
import com.attentiq.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    // GET /api/analytics/host/overview
    @GetMapping("/host/overview")
    public ResponseEntity<HostOverviewResponse> hostOverview(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(analyticsService.getHostOverview(user));
    }

    @GetMapping("/userOverview/{userId}")
    public ResponseEntity<List<ParticipantAttendanceDto>> getUserOverview(@PathVariable Long userId) {
        try {
            List<ParticipantAttendanceDto> attendanceData = analyticsService.findParticipantAttendanceOverview(userId);
            return ResponseEntity.ok(attendanceData);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    // GET /api/analytics/meeting/{id}
    @GetMapping("/meeting/{id}")
    public ResponseEntity<MeetingAnalyticsResponse> meetingAnalytics(
            @PathVariable Long id) {
        return ResponseEntity.ok(analyticsService.getMeetingAnalytics(id));
    }

    // GET /api/analytics/meeting/{id}/timeline  (attention over time)
    @GetMapping("/meeting/{id}/timeline")
    public ResponseEntity<MeetingAnalyticsResponse> timeline(@PathVariable Long id) {
        return ResponseEntity.ok(analyticsService.getMeetingAnalytics(id));
    }
}
