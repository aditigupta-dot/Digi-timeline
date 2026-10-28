package com.digitaltimeline.service;

import com.digitaltimeline.dto.ActivityResponse;
import java.util.UUID;

/** OBSERVER pattern via Spring events: linking, live streaming and anything else can react without coupling. */
public record ActivityCreatedEvent(UUID userId, UUID activityId, ActivityResponse activity) {}
