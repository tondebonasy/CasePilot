package zw.co.tncybertech.dispute.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import zw.co.tncybertech.dispute.domain.training.RbzQuizQuestion;

import java.util.List;

public interface RbzQuizQuestionRepository extends JpaRepository<RbzQuizQuestion, Long> {
  List<RbzQuizQuestion> findByQuizId(Long quizId);
}
