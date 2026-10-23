package zw.co.tncybertech.dispute.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "sla_rules", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"network", "dispute_type"})
})
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class SlaRule {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 40)
  private String network;

  @Enumerated(EnumType.STRING)
  @Column(name = "dispute_type", nullable = false, length = 60)
  private DisputeType disputeType;

  @Column(name = "internal_days", nullable = false)
  private int internalDays;

  @Column(name = "network_days", nullable = false)
  private int networkDays;

  @Column(name = "created_at", nullable = false)
  @Builder.Default
  private Instant createdAt = Instant.now();

  @PrePersist
  void ensureCreatedAt() {
    if (createdAt == null) {
      createdAt = Instant.now();
    }
  }
}
