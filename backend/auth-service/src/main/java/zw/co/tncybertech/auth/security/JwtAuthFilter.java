package zw.co.tncybertech.auth.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

  private final JwtVerifier verifier;

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    String auth = request.getHeader(HttpHeaders.AUTHORIZATION);
    if (auth != null && auth.startsWith("Bearer ") && SecurityContextHolder.getContext().getAuthentication() == null) {
      try {
        var claims = verifier.verify(auth.substring(7));
        String role = String.valueOf(claims.get("role"));
        var authorities = role == null
            ? List.<SimpleGrantedAuthority>of()
            : List.of(
                new SimpleGrantedAuthority(role),
                new SimpleGrantedAuthority(role.startsWith("ROLE_") ? role : "ROLE_" + role)
            );
        var authentication = new UsernamePasswordAuthenticationToken(
            claims.getSubject(),
            null,
            authorities
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
      } catch (Exception e) {
        response.setStatus(401);
        response.setContentType("application/json");
        response.getWriter().write("{\"error\":\"invalid_token\"}");
        return;
      }
    }

    filterChain.doFilter(request, response);
  }
}
