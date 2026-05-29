package com.attentiq.service;

import com.attentiq.dto.request.EventLogRequest;
import com.attentiq.dto.response.EventResponse;
import com.attentiq.entity.AttentionEvent;
import com.attentiq.entity.Meeting;
import com.attentiq.entity.Participant;
import com.attentiq.entity.User;
import com.attentiq.enums.EventType;
import com.attentiq.enums.Role;
import com.attentiq.repository.AttentionEventRepository;
import com.attentiq.repository.MeetingRepository;
import com.attentiq.repository.ParticipantRepository;
import com.attentiq.repository.UserRepository;
import com.attentiq.websocket.SocketIOService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.FileOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventService {

    private final AttentionEventRepository eventRepository;
    private final MeetingRepository meetingRepository;
    private final UserRepository userRepository;
    private final ParticipantRepository participantRepository;
    private final SocketIOService socketIOService;

    @Value("${app.screenshot.upload-dir}")
    private String uploadDir;

    // ─── Log Event (called by Python AI service) ──────────────────────────────
    @Transactional
    public EventResponse logEvent(EventLogRequest req) {
        Meeting meeting = meetingRepository.findById(Long.parseLong(req.getMeetingId()))
                .orElseThrow(() -> new RuntimeException("Meeting not found"));

        User user = userRepository.findById(req.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Save screenshot if provided
        String screenshotPath = null;
        if (req.getScreenshotBase64() != null && !req.getScreenshotBase64().isEmpty()) {
            screenshotPath = saveScreenshot(req.getScreenshotBase64(), user.getId(), meeting.getId());
        }

        AttentionEvent event = AttentionEvent.builder()
                .meeting(meeting)
                .user(user)
                .eventType(EventType.valueOf(req.getEventType()))
                .screenshotPath(screenshotPath)
                .timestamp(LocalDateTime.now())
                .acknowledged(false)
                .build();

        if (!user.getRole().equals(Role.PARTICIPANT)){
            log.info("Host event type = {} is detected : IGNORE Host Event", event.getEventType());
            EventResponse response = toEventResponse(event);
            socketIOService.sendAlertToHost(meeting.getId(), response);
            return response;
        } else {
            event = eventRepository.save(event);
            log.info("{} : {}-{} : {Event Saved - {}}", Role.PARTICIPANT, user.getId(), user.getName(),event.getEventType());
        }

        // Update participant attention score
        updateAttentionScore(meeting, user);

        // Push real-time alert to host via Socket.IO
        EventResponse response = toEventResponse(event);
        socketIOService.sendAlertToHost(meeting.getId(), response);

        return response;
    }

    // ─── Get Events For Meeting ───────────────────────────────────────────────
    public List<EventResponse> getEventsForMeeting(Long meetingId) {
        return eventRepository.findByMeetingIdOrderByTimestampDesc(meetingId)
                .stream()
                .map(this::toEventResponse)
                .collect(Collectors.toList());
    }

    // ─── Update Attention Score ────────────────────────────────────────────────
    private void updateAttentionScore(Meeting meeting, User user) {
        participantRepository.findByMeetingAndUser(meeting, user).ifPresent(participant -> {
            long totalEvents = eventRepository.findByMeetingIdOrderByTimestampDesc(meeting.getId())
                    .stream().filter(e -> e.getUser().getId().equals(user.getId())).count();

            // Simple scoring: each event reduces score by 5, minimum 0
            double score = Math.max(0, 100 - (totalEvents * 5));
            participant.setAttentionScore(score);
            participantRepository.save(participant);
        });
    }

    // ─── Save Screenshot ──────────────────────────────────────────────────────
    private String saveScreenshot(String base64, Long userId, Long meetingId) {
        try {
            File dir = new File(uploadDir);
            if (!dir.exists()) dir.mkdirs();

            String filename = String.format("user%d_meeting%d_%s.png",
                    userId, meetingId, UUID.randomUUID().toString().substring(0, 8));

            byte[] data = Base64.getDecoder().decode(
                    base64.contains(",") ? base64.split(",")[1] : base64
            );

            File file = new File(dir, filename);
            try (FileOutputStream fos = new FileOutputStream(file)) {
                fos.write(data);
            }
            return "/screenshots/" + filename;
        } catch (Exception e) {
            log.error("Failed to save screenshot: {}", e.getMessage());
            return null;
        }
    }

    // ─── Mapper ───────────────────────────────────────────────────────────────
    private EventResponse toEventResponse(AttentionEvent e) {
        return EventResponse.builder()
                .id(e.getId())
                .userName(e.getUser().getName())
                .userId(e.getUser().getId())
                .meetingId(String.valueOf(e.getMeeting().getId()))
                .eventType(e.getEventType().name())
                .screenshotUrl(e.getScreenshotPath())
                .timestamp(e.getTimestamp())
                .acknowledged(e.getAcknowledged())
                .build();
    }
}
