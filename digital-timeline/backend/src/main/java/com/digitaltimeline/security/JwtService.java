package com.digitaltimeline.security;

import com.digitaltimeline.domain.User;
import java.time.Duration;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    public record Token(String value, Instant expiresAt) {}

    private final JwtEncoder encoder;
    private final Duration ttl;

    public JwtService(JwtEncoder encoder, @Value("${dt.security.jwt-ttl:PT12H}") Duration ttl) {
        this.encoder = encoder;
        this.ttl = ttl;
    }

    public Token issue(User user) {
        Instant now = Instant.now();
        Instant expires = now.plus(ttl);
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("digital-timeline")
                .issuedAt(now)
                .expiresAt(expires)
                .subject(user.getId().toString())
                .claim("email", user.getEmail())
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return new Token(encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue(), expires);
    }
}
