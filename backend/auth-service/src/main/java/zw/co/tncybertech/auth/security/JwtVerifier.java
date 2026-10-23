package zw.co.tncybertech.auth.security;

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

  public Claims verify(String token) {
    return Jwts.parser()
        .setSigningKey(key)
        .requireIssuer(issuer)
        .build()
        .parseClaimsJws(token)
        .getBody();
  }
}
