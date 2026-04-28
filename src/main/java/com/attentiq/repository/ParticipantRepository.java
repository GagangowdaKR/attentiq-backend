package com.attentiq.repository;

import com.attentiq.entity.Meeting;
import com.attentiq.entity.Participant;
import com.attentiq.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ParticipantRepository extends JpaRepository<Participant, Long> {

    Optional<Participant> findByMeetingAndUser(Meeting meeting, User user);

    List<Participant> findByMeetingAndIsActive(Meeting meeting, Boolean isActive);

    @Query("SELECT AVG(p.attentionScore) FROM Participant p WHERE p.meeting.id = :meetingId")
    Double avgAttentionScoreByMeeting(@Param("meetingId") Long meetingId);

    @Query("SELECT COUNT(p) FROM Participant p WHERE p.meeting.id = :meetingId")
    Long countByMeetingId(@Param("meetingId") Long meetingId);
}
