package com.attentiq.service;

import com.attentiq.dto.response.HostOverviewResponse;
import com.attentiq.dto.response.MeetingAnalyticsResponse;
import com.attentiq.dto.response.MeetingResponse;
import com.attentiq.entity.User;
import com.attentiq.enums.EventType;
import com.attentiq.repository.AttentionEventRepository;
import com.attentiq.repository.MeetingRepository;
import com.attentiq.repository.ParticipantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final MeetingRepository meetingRepository;
    private final ParticipantRepository participantRepository;
    private final AttentionEventRepository eventRepository;
    private final MeetingService meetingService;
    private final EventService eventService;

    // ─── Host Overview ────────────────────────────────────────────────────────
    public HostOverviewResponse getHostOverview(User host) {
        List<MeetingResponse> meetings = meetingService.getMeetingHistory(host);

        long totalMeetings      = meetings.size();
        long totalParticipants  = meetings.stream().mapToLong(m -> m.getParticipantCount() != null ? m.getParticipantCount() : 0).sum();

        double avgAttention = meetings.stream()
                .filter(m -> m.getAvgAttention() != null)
                .mapToDouble(MeetingResponse::getAvgAttention)
                .average()
                .orElse(100.0);

        // Alert counts across all host's meetings
        List<Object[]> grouped = eventRepository.countGroupedByEventTypeForHost(host.getId());
        List<HostOverviewResponse.AlertCount> topAlerts = grouped.stream()
                .map(row -> HostOverviewResponse.AlertCount.builder()
                        .type(row[0].toString())
                        .count((Long) row[1])
                        .build())
                .collect(Collectors.toList());

        // Ensure all 3 types present even if 0
        for (EventType type : EventType.values()) {
            boolean exists = topAlerts.stream().anyMatch(a -> a.getType().equals(type.name()));
            if (!exists) {
                topAlerts.add(HostOverviewResponse.AlertCount.builder()
                        .type(type.name()).count(0L).build());
            }
        }

        // Weekly attention (last 7 meetings avg, or mock 7 values)
        List<Integer> weeklyAttention = new ArrayList<>();
        List<MeetingResponse> last7 = meetings.stream().limit(7).collect(Collectors.toList());
        for (int i = 6; i >= 0; i--) {
            if (i < last7.size() && last7.get(i).getAvgAttention() != null) {
                weeklyAttention.add(last7.get(i).getAvgAttention().intValue());
            } else {
                weeklyAttention.add(75); // default placeholder
            }
        }

        // Recent 4 meetings
        List<MeetingResponse> recentMeetings = meetings.stream().limit(4).collect(Collectors.toList());

        return HostOverviewResponse.builder()
                .totalMeetings(totalMeetings)
                .totalParticipants(totalParticipants)
                .avgAttentionScore((int) Math.round(avgAttention))
                .topAlerts(topAlerts)
                .weeklyAttention(weeklyAttention)
                .recentMeetings(recentMeetings)
                .build();
    }

    // ─── Per Meeting Analytics ────────────────────────────────────────────────
    public MeetingAnalyticsResponse getMeetingAnalytics(Long meetingId) {
        var meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new RuntimeException("Meeting not found"));

        long eyeCount   = eventRepository.countByMeetingIdAndEventType(meetingId, EventType.EYES_CLOSED);
        long faceCount  = eventRepository.countByMeetingIdAndEventType(meetingId, EventType.FACE_MISSING);
        long phoneCount = eventRepository.countByMeetingIdAndEventType(meetingId, EventType.PHONE_DETECTED);

        Double avgAttention = participantRepository.avgAttentionScoreByMeeting(meetingId);
        Long totalAttendees = participantRepository.countByMeetingId(meetingId);

        return MeetingAnalyticsResponse.builder()
                .meetingId(String.valueOf(meetingId))
                .title(meeting.getTitle())
                .totalAttendees(totalAttendees != null ? totalAttendees.intValue() : 0)
                .avgAttentionScore(avgAttention != null ? avgAttention : 100.0)
                .eyesClosedCount(eyeCount)
                .faceMissingCount(faceCount)
                .phoneDetectedCount(phoneCount)
                .events(eventService.getEventsForMeeting(meetingId))
                .build();
    }
}
