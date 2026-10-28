package com.digitaltimeline.insight;

import com.digitaltimeline.dto.ActivityResponse;

public record ForgottenWork(ActivityResponse task, double score, String reason) {}
