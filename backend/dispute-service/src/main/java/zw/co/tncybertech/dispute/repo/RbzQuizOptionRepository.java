package zw.co.tncybertech.dispute.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import zw.co.tncybertech.dispute.domain.training.RbzQuizOption;

import java.util.List;

public interface RbzQuizOptionRepository extends JpaRepository<RbzQuizOption, Long> {
  List<RbzQuizOption> findByQuestionId(Long questionId);
}
