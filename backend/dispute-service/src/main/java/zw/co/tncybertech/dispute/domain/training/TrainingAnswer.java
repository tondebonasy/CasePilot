package zw.co.tncybertech.dispute.domain.training;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "training_answers")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class TrainingAnswer {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "attempt_id")
  private TrainingAttempt attempt;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "question_id")
  private RbzQuizQuestion question;

  @Column(name = "selected_option_id", nullable = false)
  private Long selectedOptionId;

  @Column(nullable = false)
  private boolean correct;
}
