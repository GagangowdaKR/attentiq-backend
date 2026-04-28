package com.attentiq.service;

import com.attentiq.dto.request.MeetingRequest;
import com.attentiq.dto.response.JoinMeetingResponse;
import com.attentiq.dto.response.MeetingResponse;
import com.attentiq.entity.Meeting;
import com.attentiq.entity.Participant;
import com.attentiq.entity.User;
import com.attentiq.enums.EventType;
import com.attentiq.enums.MeetingStatus;
import com.attentiq.repository.AttentionEventRepository;
import com.attentiq.repository.MeetingRepository;
import com.attentiq.repository.ParticipantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MeetingService {

    private final MeetingRepository meetingRepository;
    private final ParticipantRepository participantRepository;
    private final AttentionEventRepository eventRepository;

    // ─── Create Meeting (HOST only) ───────────────────────────────────────────
    @Transactional
    public JoinMeetingResponse createMeeting(MeetingRequest.Create req, User host) {
        Meeting meeting = Meeting.builder()
                .title(req.getTitle())
                .code(generateCode())
                .host(host)
                .status(MeetingStatus.ACTIVE)
                .eyeCloseThreshold(60)
                .faceMissingThreshold(30)
                .phoneDetectionEnabled(true)
                .build();

        meeting = meetingRepository.save(meeting);

        // Add host as participant too
        Participant hostParticipant = Participant.builder()
                .meeting(meeting)
                .user(host)
                .isActive(true)
                .build();
        participantRepository.save(hostParticipant);

        return toJoinResponse(meeting);
    }

    // ─── Join Meeting ─────────────────────────────────────────────────────────
    @Transactional
    public JoinMeetingResponse joinMeeting(MeetingRequest.Join req, User user) {
        Meeting meeting = meetingRepository.findByCode(req.getCode())
                .orElseThrow(() -> new RuntimeException("Meeting not found with code: " + req.getCode()));

        if (meeting.getStatus() != MeetingStatus.ACTIVE) {
            throw new RuntimeException("Meeting is not active");
        }

        // Add participant if not already in
        participantRepository.findByMeetingAndUser(meeting, user)
                .orElseGet(() -> {
                    Participant p = Participant.builder()
                            .meeting(meeting)
                            .user(user)
                            .isActive(true)
                            .build();
                    return participantRepository.save(p);
                });

        return toJoinResponse(meeting);
    }

    // ─── Leave Meeting ────────────────────────────────────────────────────────
    @Transactional
    public void leaveMeeting(Long meetingId, User user) {
        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new RuntimeException("Meeting not found"));

        participantRepository.findByMeetingAndUser(meeting, user)
                .ifPresent(p -> {
                    p.setIsActive(false);
                    p.setLeftAt(LocalDateTime.now());
                    participantRepository.save(p);
                });
    }

    // ─── End Meeting (HOST only) ──────────────────────────────────────────────
    @Transactional
    public void endMeeting(Long meetingId, User host) {
        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new RuntimeException("Meeting not found"));

        if (!meeting.getHost().getId().equals(host.getId())) {
            throw new RuntimeException("Only the host can end this meeting");
        }

        meeting.setStatus(MeetingStatus.ENDED);
        meeting.setEndedAt(LocalDateTime.now());
        meetingRepository.save(meeting);
    }

    // ─── Get History (HOST) ───────────────────────────────────────────────────
    public List<MeetingResponse> getMeetingHistory(User host) {
        return meetingRepository.findAllByHostOrderByCreatedAtDesc(host)
                .stream()
                .map(this::toMeetingResponse)
                .collect(Collectors.toList());
    }

    // ─── Get Meeting By ID ────────────────────────────────────────────────────
    public MeetingResponse getMeetingById(Long id) {
        Meeting meeting = meetingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Meeting not found"));
        return toMeetingResponse(meeting);
    }

    // ─── Update Thresholds ────────────────────────────────────────────────────
    @Transactional
    public void updateThresholds(Long meetingId, MeetingRequest.UpdateThresholds req, User host) {
        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new RuntimeException("Meeting not found"));

        if (!meeting.getHost().getId().equals(host.getId())) {
            throw new RuntimeException("Only the host can update thresholds");
        }

        if (req.getEyeCloseDuration()       != null) meeting.setEyeCloseThreshold(req.getEyeCloseDuration());
        if (req.getFaceMissingDuration()    != null) meeting.setFaceMissingThreshold(req.getFaceMissingDuration());
        if (req.getPhoneDetectionEnabled()  != null) meeting.setPhoneDetectionEnabled(req.getPhoneDetectionEnabled());

        meetingRepository.save(meeting);
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────
    private String generateCode() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        Random rnd = new Random();
        StringBuilder sb = new StringBuilder("ATQ-");
        for (int i = 0; i < 4; i++) sb.append(chars.charAt(rnd.nextInt(chars.length())));
        return sb.toString();
    }

    private JoinMeetingResponse toJoinResponse(Meeting m) {
        return JoinMeetingResponse.builder()
                .meetingId(String.valueOf(m.getId()))
                .code(m.getCode())
                .title(m.getTitle())
                .eyeCloseThreshold(m.getEyeCloseThreshold())
                .faceMissingThreshold(m.getFaceMissingThreshold())
                .phoneDetectionEnabled(m.getPhoneDetectionEnabled())
                .build();
    }

    private MeetingResponse toMeetingResponse(Meeting m) {
        Long participantCount = participantRepository.countByMeetingId(m.getId());
        Double avgAttention   = participantRepository.avgAttentionScoreByMeeting(m.getId());
        long alertCount = eventRepository.countByMeetingIdAndEventType(m.getId(), EventType.EYES_CLOSED)
                        + eventRepository.countByMeetingIdAndEventType(m.getId(), EventType.FACE_MISSING)
                        + eventRepository.countByMeetingIdAndEventType(m.getId(), EventType.PHONE_DETECTED);

        return MeetingResponse.builder()
                .meetingId(String.valueOf(m.getId()))
                .code(m.getCode())
                .title(m.getTitle())
                .status(m.getStatus().name())
                .hostId(m.getHost().getId())
                .hostName(m.getHost().getName())
                .createdAt(m.getCreatedAt())
                .endedAt(m.getEndedAt())
                .participantCount(participantCount != null ? participantCount.intValue() : 0)
                .avgAttention(avgAttention != null ? Math.round(avgAttention * 10.0) / 10.0 : 100.0)
                .alertCount((int) alertCount)
                .date(m.getCreatedAt() != null
                        ? m.getCreatedAt().format(DateTimeFormatter.ofPattern("MMM d")) : "")
                .build();
    }
}
