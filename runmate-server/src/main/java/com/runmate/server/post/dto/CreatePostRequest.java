package com.runmate.server.post.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.runmate.server.post.domain.PostStatus;
import com.runmate.server.post.domain.RunningIntensity;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreatePostRequest(
        @NotNull(message = "카테고리를 선택해 주세요.") Long categoryId,
        @Size(max = 255, message = "제목은 255자 이하로 입력해 주세요.") String title,
        @Size(max = 500, message = "요약은 500자 이하로 입력해 주세요.") String summary,
        String content,
        @Valid RunningRecordRequest runningRecord,
        @Size(max = 10, message = "태그는 최대 10개까지 등록할 수 있습니다.") List<String> tags,
        PostStatus status
) {
    public record RunningRecordRequest(
            @NotNull(message = "러닝 날짜를 입력해 주세요.")
            @PastOrPresent(message = "러닝 날짜는 오늘 이후일 수 없습니다.") LocalDate runningDate,
            @NotNull(message = "러닝 거리를 입력해 주세요.")
            @DecimalMin(value = "0.001", message = "러닝 거리는 0.001km 이상이어야 합니다.")
            @DecimalMax(value = "9999.999", message = "러닝 거리가 허용 범위를 초과했습니다.") BigDecimal distanceKm,
            @Positive(message = "운동 시간은 1초 이상이어야 합니다.") int durationSeconds,
            @Size(max = 200, message = "장소명은 200자 이하로 입력해 주세요.") String locationName,
            @Size(max = 100, message = "지역은 100자 이하로 입력해 주세요.") String region,
            RunningIntensity intensity,
            @Size(max = 50, message = "날씨는 50자 이하로 입력해 주세요.") String weather
    ) {
    }
}
