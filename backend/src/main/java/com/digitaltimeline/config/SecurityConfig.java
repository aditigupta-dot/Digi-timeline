package com.digitaltimeline.config;
import com.digitaltimeline.security.JwtAuthFilter;
import java.util.List;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
@Configuration
@EnableConfigurationProperties({AppProperties.class, GithubProperties.class, GoogleProperties.class, AiProperties.class})
public class SecurityConfig {
  @Bean PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }
  @Bean
  SecurityFilterChain chain(HttpSecurity http, JwtAuthFilter jwtFilter, AppProperties props) throws Exception {
    return http.csrf(c -> c.disable()) // stateless bearer-token API, no cookies
        .cors(c -> c.configurationSource(r -> {
          var cfg = new CorsConfiguration();
          cfg.setAllowedOrigins(List.of(props.corsOrigin()));
          cfg.setAllowedMethods(List.of("GET", "POST", "PATCH", "DELETE", "OPTIONS"));
          cfg.setAllowedHeaders(List.of("Authorization", "Content-Type"));
          return cfg;
        }))
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(a -> a.requestMatchers("/api/auth/**", "/api/integrations/*/callback").permitAll().anyRequest().authenticated())
        .exceptionHandling(e -> e.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
        .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class).build();
  }
}
