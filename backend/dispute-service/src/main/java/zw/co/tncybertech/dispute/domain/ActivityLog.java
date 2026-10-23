package zw.co.tncybertech.dispute.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "activity_log")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class ActivityLog {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "dispute_id")
  private Dispute dispute;

  @Column(name = "actor_user_id", nullable = false)
  @Setter(AccessLevel.NONE)
  private Long actorUserId;

  @Column(nullable = false, length = 120, updatable = false)
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
