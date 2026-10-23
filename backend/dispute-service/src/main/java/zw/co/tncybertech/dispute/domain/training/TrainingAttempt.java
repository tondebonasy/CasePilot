package zw.co.tncybertech.dispute.domain.training;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "training_attempts")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class TrainingAttempt {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "user_id", nullable = false)
  private Long userId;

  @Column(name = "user_role", nullable = false, length = 40)
  private String userRole;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "quiz_id")
  private RbzQuiz quiz;

  @Column(nullable = false)
  private int score;

  @Column(name = "max_score", nullable = false)
  private int maxScore;

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
