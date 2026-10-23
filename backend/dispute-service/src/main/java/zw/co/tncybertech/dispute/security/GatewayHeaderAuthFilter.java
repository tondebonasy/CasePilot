package zw.co.tncybertech.dispute.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class GatewayHeaderAuthFilter extends OncePerRequestFilter {

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    String userId = request.getHeader("X-User-Id");
    String role = request.getHeader("X-User-Role");
    String email = request.getHeader("X-User-Email");
    String name = request.getHeader("X-User-Name");

    var existing = SecurityContextHolder.getContext().getAuthentication();
    boolean replace = existing == null || existing instanceof AnonymousAuthenticationToken;

    if (userId != null && role != null && replace) {
      JwtClaims claims = new JwtClaims(Long.valueOf(userId), email, name, role);
      var authorities = List.of(
          new SimpleGrantedAuthority(role),
          new SimpleGrantedAuthority(role.startsWith("ROLE_") ? role : "ROLE_" + role)
      );
      var auth = new UsernamePasswordAuthenticationToken(
          claims,
          null,
          authorities
      );
      SecurityContextHolder.getContext().setAuthentication(auth);
    }

    filterChain.doFilter(request, response);
  }
}
