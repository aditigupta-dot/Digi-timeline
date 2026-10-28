package com.digitaltimeline.security;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/** Stateless JWT security: BCrypt passwords, HS256 tokens, per-user rate limiting, strict CORS. */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    SecretKey jwtKey(@Value("${dt.security.jwt-secret}") String secret) {
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException("dt.security.jwt-secret must be at least 32 bytes (set JWT_SECRET)");
        }
        return new SecretKeySpec(bytes, "HmacSHA256");
    }

    @Bean
    JwtEncoder jwtEncoder(SecretKey key) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(key));
    }

    @Bean
    JwtDecoder jwtDecoder(SecretKey key) {
        return NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(@Value("${dt.security.allowed-origins}") String origins) {
        CorsConfiguration cfg = new CorsConfiguration();
        cfg.setAllowedOrigins(Arrays.stream(origins.split(",")).map(String::trim).toList());
        cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        cfg.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        cfg.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", cfg);
        return source;
    }

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http,
                                    @Value("${dt.rate-limit.general-burst:80}") double generalBurst,
                                    @Value("${dt.rate-limit.general-per-second:20}") double generalPerSecond,
                                    @Value("${dt.rate-limit.ai-burst:8}") double aiBurst,
                                    @Value("${dt.rate-limit.ai-per-second:0.2}") double aiPerSecond) throws Exception {
        // Browsers cannot set headers on EventSource, so ONLY the live stream may carry ?access_token=
        DefaultBearerTokenResolver headerResolver = new DefaultBearerTokenResolver();
        DefaultBearerTokenResolver streamResolver = new DefaultBearerTokenResolver();
        streamResolver.setAllowUriQueryParameter(true);
        BearerTokenResolver resolver = request ->
                request.getRequestURI().startsWith("/api/stream") ? streamResolver.resolve(request) : headerResolver.resolve(request);

        http.csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .headers(h -> h.frameOptions(f -> f.sameOrigin()))
                .authorizeHttpRequests(a -> a
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/api/auth/**", "/api/system", "/actuator/health", "/v3/api-docs/**",
                                "/swagger-ui/**", "/swagger-ui.html", "/h2-console/**").permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(o -> o.bearerTokenResolver(resolver).jwt(Customizer.withDefaults()))
                .addFilterAfter(new RateLimitFilter(generalBurst, generalPerSecond, aiBurst, aiPerSecond),
                        BearerTokenAuthenticationFilter.class);
        return http.build();
    }
}
