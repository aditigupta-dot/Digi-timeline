package com.digitaltimeline.insight;

import com.digitaltimeline.domain.ActivityType;
import com.digitaltimeline.dto.ActivityResponse;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public record InsightsReport(
        int days,
        long total,
        Map<ActivityType, Long> byType,
        Map<String, Long> byProject,
        List<DayCount> perDay,
        int peakHour,
        String workStyle,
        int activeDays,
        int longestStreak,
        int currentStreak,
        int focusScore,
        int contextSwitches,
        List<String> topTags,
        List<ActivityResponse> staleTasks,
        Double changeVsPrevious,
        String busiestDay) {

    public record DayCount(LocalDate day, long count) {}
}
