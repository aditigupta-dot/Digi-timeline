package com.digitaltimeline.service;

import java.util.UUID;

public record ActivityLinkedEvent(UUID userId, UUID activityId, int links) {}
