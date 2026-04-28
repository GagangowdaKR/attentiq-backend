package com.attentiq.entity;

import com.attentiq.enums.MeetingStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "meetings")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Meeting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, unique = true, length = 10)
    private String code;  // e.g. ATQ-2841

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "host_id", nullable = false)
    private User host;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MeetingStatus status;

    // Detection thresholds (host configurable)
    @Column(name = "eye_close_threshold")
    private Integer eyeCloseThreshold = 60;

    @Column(name = "face_missing_threshold")
    private Integer faceMissingThreshold = 30;

    @Column(name = "phone_detection_enabled")
    private Boolean phoneDetectionEnabled = true;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    @OneToMany(mappedBy = "meeting", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Participant> participants = new ArrayList<>();

    @OneToMany(mappedBy = "meeting", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<AttentionEvent> events = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        startedAt = LocalDateTime.now();
        if (status == null) status = MeetingStatus.ACTIVE;
    }
}
