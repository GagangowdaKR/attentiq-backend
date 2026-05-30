package com.attentiq.repository;

import com.attentiq.dto.response.ParticipantAttendanceDto;
import com.attentiq.entity.AttentionEvent;
import com.attentiq.enums.EventType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AttentionEventRepository extends JpaRepository<AttentionEvent, Long> {

    List<AttentionEvent> findByMeetingIdOrderByTimestampDesc(Long meetingId);

    List<AttentionEvent> findByMeetingIdAndEventType(Long meetingId, EventType eventType);

    @Query("SELECT COUNT(e) FROM AttentionEvent e WHERE e.meeting.id = :meetingId AND e.eventType = :type")
    Long countByMeetingIdAndEventType(@Param("meetingId") Long meetingId, @Param("type") EventType type);

    @Query("SELECT COUNT(e) FROM AttentionEvent e WHERE e.meeting.host.id = :hostId")
    Long countTotalEventsByHost(@Param("hostId") Long hostId);

    @Query("""
        SELECT e.eventType, COUNT(e) FROM AttentionEvent e
        WHERE e.meeting.id = :meetingId
        GROUP BY e.eventType
        """)
    List<Object[]> countGroupedByEventType(@Param("meetingId") Long meetingId);

    @Query("""
        SELECT e.eventType, COUNT(e) FROM AttentionEvent e
        WHERE e.meeting.host.id = :hostId
        GROUP BY e.eventType
        """)
    List<Object[]> countGroupedByEventTypeForHost(@Param("hostId") Long hostId);

    // Add to EventRepository.java or equivalent
    @Query(value = "SELECT " +
            "  m.title AS meetingTitle, " +
            "  h.name AS hostName, " +
            "  m.code AS meetingCode, " +
            "  SUM(CASE WHEN e.event_type = 'EYES_CLOSED' THEN 1 ELSE 0 END) AS eyesClosedCount, " +
            "  SUM(CASE WHEN e.event_type = 'FACE_MISSING' THEN 1 ELSE 0 END) AS faceMissingCount, " +
            "  SUM(CASE WHEN e.event_type = 'PHONE_DETECTED' THEN 1 ELSE 0 END) AS phoneDetectedCount " +
            "FROM attention_events e " +   // Verify your exact DB table name for events
            "JOIN meetings m ON e.meeting_id = m.id " + // Verify your exact join columns
            "JOIN users h ON m.host_id = h.id " +       // Verify your exact host relation columns
            "WHERE e.user_id = :userId " +
            "GROUP BY m.id, m.title, h.name, m.code",
            nativeQuery = true)
    List<Object[]> findParticipantAttendanceOverviewRaw(@Param("userId") Long userId);
}
