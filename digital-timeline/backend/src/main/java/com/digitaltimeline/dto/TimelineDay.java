package com.digitaltimeline.dto;

import java.time.LocalDate;
import java.util.List;

public record TimelineDay(LocalDate date, List<ActivityResponse> items) {}
