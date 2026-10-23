package zw.co.tncybertech.dispute.domain.training;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "rbz_quiz_options")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class RbzQuizOption {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "question_id")
  private RbzQuizQuestion question;

  @Column(columnDefinition = "TEXT", nullable = false)
  private String text;

  @Column(name = "is_correct", nullable = false)
  private boolean correct;
}
