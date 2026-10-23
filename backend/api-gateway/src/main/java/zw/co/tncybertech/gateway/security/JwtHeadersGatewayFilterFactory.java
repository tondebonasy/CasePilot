package zw.co.tncybertech.gateway.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

@Component("JwtHeaders")
public class JwtHeadersGatewayFilterFactory extends AbstractGatewayFilterFactory<Object> {

  private final SecretKey key;
  private final String issuer;

  public JwtHeadersGatewayFilterFactory(@Value("${app.jwt.secret}") String secret,
                                       @Value("${app.jwt.issuer}") String issuer) {
    this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    this.issuer = issuer;
  }

  @Override
  public GatewayFilter apply(Object config) {
    return (exchange, chain) -> {
      String path = exchange.getRequest().getURI().getPath();

      // Allow all auth endpoints without token
      if (path.startsWith("/api/v1/auth/")) {
        return chain.filter(exchange);
      }

      String auth = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
      if (auth == null || !auth.startsWith("Bearer ")) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        return exchange.getResponse().setComplete();
      }

      try {
        String token = auth.substring(7);
        Claims c = Jwts.parser()
            .setSigningKey(key)
            .requireIssuer(issuer)
            .build()
            .parseClaimsJws(token)
            .getBody();

        String userId = c.getSubject();
        String role = normalizeRole(c.get("role"));
        String email = String.valueOf(c.get("email"));
        String name = String.valueOf(c.get("name"));

        var requestBuilder = exchange.getRequest().mutate()
            .header("X-User-Id", userId)
            .header("X-User-Email", email)
            .header("X-User-Name", name);
        if (role != null && !role.isBlank()) {
          requestBuilder.header("X-User-Role", role);
        }

        var mutated = requestBuilder.build();

        return chain.filter(exchange.mutate().request(mutated).build());
      } catch (Exception e) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        return exchange.getResponse().setComplete();
      }
    };
  }

  private static String normalizeRole(Object roleValue) {
    if (roleValue == null) {
      return "";
    }
    String role = String.valueOf(roleValue).trim();
    if (role.isEmpty() || "null".equalsIgnoreCase(role)) {
      return "";
    }
    if (role.startsWith("ROLE_")) {
      role = role.substring(5);
    }
    return role.toUpperCase();
  }
}
