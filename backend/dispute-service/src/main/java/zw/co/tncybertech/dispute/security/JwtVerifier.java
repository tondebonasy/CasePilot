package zw.co.tncybertech.dispute.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

@Component
public class JwtVerifier {

  private final SecretKey key;
  private final String issuer;

  public JwtVerifier(@Value("${app.jwt.secret}") String secret,
                     @Value("${app.jwt.issuer}") String issuer) {
    this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    this.issuer = issuer;
  }

  public JwtClaims verify(String token) {
    Claims c = Jwts.parser()
        .setSigningKey(key)
        .requireIssuer(issuer)
        .build()
        .parseClaimsJws(token)
        .getBody();

    Long userId = Long.valueOf(c.getSubject());
    String role = String.valueOf(c.get("role"));
    String email = String.valueOf(c.get("email"));
    String name = String.valueOf(c.get("name"));

    return new JwtClaims(userId, email, name, role);
  }
}
