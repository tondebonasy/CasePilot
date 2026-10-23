package zw.co.tncybertech.dispute.domain.training;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "rbz_policies")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class RbzPolicy {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 200)
  private String title;

  @Column(name = "circular_reference", nullable = false, length = 80)
  private String circularReference;

  @Column(columnDefinition = "TEXT", nullable = false)
  private String summary;

  @Column(name = "effective_date", nullable = false)
  private LocalDate effectiveDate;

  @Column(name = "key_requirements", columnDefinition = "TEXT", nullable = false)
  private String keyRequirements;

  @Column(nullable = false)
  @Builder.Default
  private boolean active = true;

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
