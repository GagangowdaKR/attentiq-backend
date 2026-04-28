package com.attentiq.controller;

import com.attentiq.dto.request.EventLogRequest;
import com.attentiq.dto.response.EventResponse;
import com.attentiq.service.EventService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;

    // POST /api/events/log  (called by Python AI service)
    @PostMapping("/log")
    public ResponseEntity<EventResponse> logEvent(
            @Valid @RequestBody EventLogRequest req) {
        return ResponseEntity.ok(eventService.logEvent(req));
    }

    // GET /api/events/meeting/{meetingId}
    @GetMapping("/meeting/{meetingId}")
    public ResponseEntity<List<EventResponse>> getByMeeting(
            @PathVariable Long meetingId) {
        return ResponseEntity.ok(eventService.getEventsForMeeting(meetingId));
    }
}
