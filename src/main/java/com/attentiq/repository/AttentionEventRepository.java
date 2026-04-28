package com.attentiq.repository;

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
}
