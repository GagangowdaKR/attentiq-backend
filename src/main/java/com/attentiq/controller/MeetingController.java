package com.attentiq.controller;

import com.attentiq.dto.request.MeetingRequest;
import com.attentiq.dto.response.JoinMeetingResponse;
import com.attentiq.dto.response.MeetingResponse;
import com.attentiq.entity.User;
import com.attentiq.service.MeetingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/meetings")
@RequiredArgsConstructor
public class MeetingController {

    private final MeetingService meetingService;

    // POST /api/meetings/create
    @PostMapping("/create")
    public ResponseEntity<JoinMeetingResponse> create(
            @Valid @RequestBody MeetingRequest.Create req,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(meetingService.createMeeting(req, user));
    }

    // POST /api/meetings/join
    @PostMapping("/join")
    public ResponseEntity<JoinMeetingResponse> join(
            @Valid @RequestBody MeetingRequest.Join req,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(meetingService.joinMeeting(req, user));
    }

    // POST /api/meetings/{id}/leave
    @PostMapping("/{id}/leave")
    public ResponseEntity<?> leave(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {
        meetingService.leaveMeeting(id, user);
        return ResponseEntity.ok("{\"message\": \"Left meeting\"}");
    }

    // POST /api/meetings/{id}/end
    @PostMapping("/{id}/end")
    public ResponseEntity<?> end(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {
        meetingService.endMeeting(id, user);
        return ResponseEntity.ok("{\"message\": \"Meeting ended\"}");
    }

    // GET /api/meetings/history
    @GetMapping("/history")
    public ResponseEntity<List<MeetingResponse>> history(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(meetingService.getMeetingHistory(user));
    }

    // GET /api/meetings/{id}
    @GetMapping("/{id}")
    public ResponseEntity<MeetingResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(meetingService.getMeetingById(id));
    }

    // PUT /api/meetings/{id}/thresholds
    @PutMapping("/{id}/thresholds")
    public ResponseEntity<?> updateThresholds(
            @PathVariable Long id,
            @RequestBody MeetingRequest.UpdateThresholds req,
            @AuthenticationPrincipal User user) {
        meetingService.updateThresholds(id, req, user);
        return ResponseEntity.ok("{\"message\": \"Thresholds updated\"}");
    }
}
