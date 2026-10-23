package zw.co.tncybertech.dispute.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import zw.co.tncybertech.dispute.domain.training.TrainingAttempt;

import java.time.Instant;
import java.util.List;

public interface TrainingAttemptRepository extends JpaRepository<TrainingAttempt, Long> {
  List<TrainingAttempt> findByUserIdOrderByCreatedAtDesc(Long userId);

  List<TrainingAttempt> findByCreatedAtBetween(Instant from, Instant to);
}
