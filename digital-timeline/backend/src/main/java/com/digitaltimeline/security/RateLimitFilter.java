package com.digitaltimeline.security;

import com.digitaltimeline.core.TokenBucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Per-user token-bucket rate limiting (concurrent, lock-light). Two buckets: normal API traffic and the
 * expensive AI endpoints, so a runaway client cannot burn the LLM budget. Not a @Component on purpose:
 * it is added inside the security chain AFTER authentication so it can key on the user id.
 */
public class RateLimitFilter extends OncePerRequestFilter {

    private final ConcurrentHashMap<String, TokenBucket> general = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, TokenBucket> ai = new ConcurrentHashMap<>();
    private final double generalBurst, generalPerSecond, aiBurst, aiPerSecond;

    public RateLimitFilter(double generalBurst, double generalPerSecond, double aiBurst, double aiPerSecond) {
        this.generalBurst = generalBurst;
        this.generalPerSecond = generalPerSecond;
        this.aiBurst = aiBurst;
        this.aiPerSecond = aiPerSecond;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return !path.startsWith("/api/") || path.startsWith("/api/stream");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String key = auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())
                ? auth.getName() : request.getRemoteAddr();
        if (general.size() > 20_000) general.clear();
        if (ai.size() > 20_000) ai.clear();

        TokenBucket bucket = general.computeIfAbsent(key, k -> new TokenBucket(generalBurst, generalPerSecond));
        if (!bucket.tryAcquire()) {
            reject(response, bucket.retryAfterMillis());
            return;
        }
        String path = request.getRequestURI();
        if (path.equals("/api/ask") || path.startsWith("/api/digest")) {
            TokenBucket aiBucket = ai.computeIfAbsent(key, k -> new TokenBucket(aiBurst, aiPerSecond));
            if (!aiBucket.tryAcquire()) {
                reject(response, aiBucket.retryAfterMillis());
                return;
            }
        }
        chain.doFilter(request, response);
    }

    private void reject(HttpServletResponse response, long retryAfterMillis) throws IOException {
        response.setStatus(429);
        response.setHeader("Retry-After", String.valueOf(Math.max(1, retryAfterMillis / 1000)));
        response.setContentType("application/json");
        response.getWriter().write("{\"title\":\"Too many requests\",\"status\":429,\"detail\":\"Slow down and retry in "
                + Math.max(1, retryAfterMillis / 1000) + "s\"}");
    }
}
