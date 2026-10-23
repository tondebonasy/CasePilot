package zw.co.tncybertech.dispute.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "fraud_flags")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class FraudFlag {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "dispute_id")
  private Dispute dispute;

  @Enumerated(EnumType.STRING)
  @Column(name = "flag_type", nullable = false, length = 40)
  private FraudFlagType flagType;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private FraudSeverity severity;

  @Column(columnDefinition = "TEXT")
  private String detail;

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
