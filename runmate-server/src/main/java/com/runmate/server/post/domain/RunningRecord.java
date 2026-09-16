package com.runmate.server.post.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "running_records")
public class RunningRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "running_record_id")
    private Long runningRecordId;
    @Column(name = "post_id", nullable = false, unique = true)
    private Long postId;
    @Column(name = "running_date", nullable = false)
    private LocalDate runningDate;
    @Column(name = "distance_km", nullable = false, precision = 7, scale = 3)
    private BigDecimal distanceKm;
    @Column(name = "duration_seconds", nullable = false)
    private Integer durationSeconds;
    @Column(name = "location_name", length = 200)
    private String locationName;
    @Column(name = "region", length = 100)
    private String region;
    @Enumerated(EnumType.STRING)
    @Column(name = "intensity", nullable = false)
    private RunningIntensity intensity;
    @Column(name = "weather", length = 50)
    private String weather;
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public static RunningRecord create(Long postId, LocalDate runningDate, BigDecimal distanceKm,
            int durationSeconds, String locationName, String region,
            RunningIntensity intensity, String weather) {
        RunningRecord record = new RunningRecord();
        record.postId = postId;
        record.runningDate = runningDate;
        record.distanceKm = distanceKm;
        record.durationSeconds = durationSeconds;
        record.locationName = locationName;
        record.region = region;
        record.intensity = intensity == null ? RunningIntensity.NORMAL : intensity;
        record.weather = weather;
        return record;
    }
}
