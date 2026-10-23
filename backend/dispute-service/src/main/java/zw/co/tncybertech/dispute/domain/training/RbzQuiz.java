package zw.co.tncybertech.dispute.domain.training;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "rbz_quizzes")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class RbzQuiz {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "policy_id")
  private RbzPolicy policy;

  @Column(nullable = false, length = 180)
  private String title;

  @Column(columnDefinition = "TEXT", nullable = false)
  private String description;

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
