package com.digitaltimeline.security;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
/** Reads "Authorization: Bearer ..." and stores the user's UUID as the principal. */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {
  private final JwtService jwt;
  public JwtAuthFilter(JwtService jwt) { this.jwt = jwt; }
  @Override protected boolean shouldNotFilterAsyncDispatch() { return false; } // SSE streams finish via an async dispatch
  @Override
  protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
      throws ServletException, IOException {
    String h = req.getHeader("Authorization");
    if (h != null && h.startsWith("Bearer ")) {
      try {
        UUID id = jwt.parse(h.substring(7));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(id, null, List.of()));
      } catch (JwtException | IllegalArgumentException e) { /* invalid/expired: stay anonymous -> 401 */ }
    }
    chain.doFilter(req, res);
  }
}
