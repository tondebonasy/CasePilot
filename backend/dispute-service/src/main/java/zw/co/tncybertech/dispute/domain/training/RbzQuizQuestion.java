package zw.co.tncybertech.dispute.domain.training;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "rbz_quiz_questions")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class RbzQuizQuestion {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "quiz_id")
  private RbzQuiz quiz;

  @Column(columnDefinition = "TEXT", nullable = false)
  private String question;

  @Column(columnDefinition = "TEXT")
  private String explanation;
}
