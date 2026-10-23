package zw.co.tncybertech.dispute.api;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import zw.co.tncybertech.dispute.api.dto.NotificationDtos;
import zw.co.tncybertech.dispute.repo.NotificationRepository;
import zw.co.tncybertech.dispute.security.JwtClaims;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

  private final NotificationRepository notifications;

  @GetMapping
  public ResponseEntity<?> list(@AuthenticationPrincipal JwtClaims claims,
                                Authentication authentication) {
    ResolvedUser user = resolveUser(claims, authentication);
    var items = notifications.findByRecipientUserIdOrderByCreatedAtDesc(user.userId()).stream()
        .map(n -> new NotificationDtos.NotificationDto(
            n.getId(),
            n.getDispute() == null ? null : n.getDispute().getId(),
            n.getTitle(),
            n.getMessage(),
            n.getChannel().name(),
            n.getStatus().name(),
            n.getCreatedAt(),
            n.getSentAt()
        ))
        .toList();
    return ResponseEntity.ok(items);
  }

  private ResolvedUser resolveUser(JwtClaims claims, Authentication authentication) {
    if (claims != null) {
      return new ResolvedUser(claims.userId(), claims.role());
    }
    if (authentication == null) {
      throw new SecurityException("Forbidden");
    }

    Long userId = null;
    Object principal = authentication.getPrincipal();
    if (principal instanceof JwtClaims jwtClaims) {
      userId = jwtClaims.userId();
    } else if (principal instanceof String s) {
      try {
        userId = Long.valueOf(s);
      } catch (NumberFormatException ignored) {
        userId = null;
      }
    } else if (principal instanceof Long l) {
      userId = l;
    }

    String role = resolveRole(authentication);
    if (userId == null) {
      throw new SecurityException("Forbidden");
    }
    return new ResolvedUser(userId, role);
  }

  private String resolveRole(Authentication authentication) {
    String role = null;
    for (GrantedAuthority authority : authentication.getAuthorities()) {
      String value = authority.getAuthority();
      if ("ADMIN".equals(value) || "ROLE_ADMIN".equals(value)) {
        return "ADMIN";
      }
      if (role == null && value != null && !value.isBlank()) {
        role = value.startsWith("ROLE_") ? value.substring(5) : value;
      }
    }
    return role == null ? "" : role;
  }

  private record ResolvedUser(Long userId, String role) {}
}
