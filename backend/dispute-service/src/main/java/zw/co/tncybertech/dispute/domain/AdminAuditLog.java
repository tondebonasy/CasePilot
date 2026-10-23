package zw.co.tncybertech.dispute.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "admin_audit_log")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class AdminAuditLog {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "actor_user_id", nullable = false, updatable = false)
  @Setter(AccessLevel.NONE)
  private Long actorUserId;

  @Column(name = "actor_role", nullable = false, length = 40, updatable = false)
  @Setter(AccessLevel.NONE)
  private String actorRole;

  @Column(nullable = false, length = 140, updatable = false)
  @Setter(AccessLevel.NONE)
  private String action;

  @Column(columnDefinition = "TEXT", updatable = false)
  @Setter(AccessLevel.NONE)
  private String detail;

  @Column(name = "ip_address", length = 64, updatable = false)
  @Setter(AccessLevel.NONE)
  private String ipAddress;

  @Column(name = "at_time", nullable = false, updatable = false)
  @Builder.Default
  @Setter(AccessLevel.NONE)
  private Instant atTime = Instant.now();

  @PrePersist
  void ensureAtTime() {
    if (atTime == null) {
      atTime = Instant.now();
    }
  }
}
