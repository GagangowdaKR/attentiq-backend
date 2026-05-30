package com.attentiq.repository;

import com.attentiq.entity.Meeting;
import com.attentiq.entity.User;
import com.attentiq.enums.MeetingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MeetingRepository extends JpaRepository<Meeting, Long> {

    Optional<Meeting> findByCode(String code);

    List<Meeting> findByHostOrderByCreatedAtDesc(User host);

    List<Meeting> findByStatus(MeetingStatus status);

    @Query("SELECT m FROM Meeting m WHERE m.host = :host ORDER BY m.createdAt DESC")
    List<Meeting> findAllByHostOrderByCreatedAtDesc(@Param("host") User host);

    @Query("SELECT COUNT(p) FROM Participant p WHERE p.meeting.id = :meetingId AND p.isActive = true")
    Long countActiveParticipants(@Param("meetingId") Long meetingId);

    List<Meeting> findByHostIdOrderByIdDesc(Long hostId);

    List<Meeting> findByHostIdAndStatus(Long userId, MeetingStatus status);
}
