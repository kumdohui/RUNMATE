package com.runmate.server.post.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RunningRecord {
    private Long runningRecordId;
    private Long postId;
    private LocalDate runningDate;
    private BigDecimal distanceKm;
    private Integer durationSeconds;
    private String locationName;
    private String region;
    private RunningIntensity intensity;
    private String weather;
    private LocalDateTime createdAt;
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
