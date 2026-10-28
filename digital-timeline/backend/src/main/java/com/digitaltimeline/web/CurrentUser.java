package com.digitaltimeline.web;

import java.time.DateTimeException;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.UUID;
import org.springframework.security.oauth2.jwt.Jwt;

final class CurrentUser {
    private CurrentUser() {}

    static UUID id(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }

    static ZoneId zone(String tz) {
        try {
            return tz == null || tz.isBlank() ? ZoneOffset.UTC : ZoneId.of(tz);
        } catch (DateTimeException e) {
            return ZoneOffset.UTC;
        }
    }
}
