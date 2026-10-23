package zw.co.tncybertech.dispute.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import zw.co.tncybertech.dispute.domain.FraudFlag;

import java.util.List;

public interface FraudFlagRepository extends JpaRepository<FraudFlag, Long> {
  List<FraudFlag> findByDisputeIdOrderByCreatedAtDesc(Long disputeId);

  List<FraudFlag> findByCreatedAtBetween(java.time.Instant from, java.time.Instant to);
}
