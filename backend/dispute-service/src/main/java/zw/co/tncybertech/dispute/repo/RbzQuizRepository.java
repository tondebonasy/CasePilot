package zw.co.tncybertech.dispute.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import zw.co.tncybertech.dispute.domain.training.RbzQuiz;

import java.util.List;

public interface RbzQuizRepository extends JpaRepository<RbzQuiz, Long> {
  List<RbzQuiz> findByPolicyId(Long policyId);
}
