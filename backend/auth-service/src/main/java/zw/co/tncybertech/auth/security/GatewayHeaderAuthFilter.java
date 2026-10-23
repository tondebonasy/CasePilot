package zw.co.tncybertech.auth.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

@Component
public class GatewayHeaderAuthFilter extends OncePerRequestFilter {

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    String userId = request.getHeader("X-User-Id");
    String role = normalizeRole(request.getHeader("X-User-Role"));

    if (userId != null && role != null && !role.isBlank() && SecurityContextHolder.getContext().getAuthentication() == null) {
      var authorities = List.of(
          new SimpleGrantedAuthority(role),
          new SimpleGrantedAuthority("ROLE_" + role)
      );
      var auth = new UsernamePasswordAuthenticationToken(
          userId,
          null,
          authorities
      );
      SecurityContextHolder.getContext().setAuthentication(auth);
    }

    filterChain.doFilter(request, response);
  }

  private String normalizeRole(String role) {
    if (role == null) {
      return "";
    }
    String normalized = role.trim();
    if (normalized.isEmpty() || "null".equalsIgnoreCase(normalized)) {
      return "";
    }
    if (normalized.startsWith("ROLE_")) {
      normalized = normalized.substring(5);
    }
    return normalized.toUpperCase(Locale.ROOT);
  }
}
