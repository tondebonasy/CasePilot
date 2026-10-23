package zw.co.tncybertech.dispute.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import zw.co.tncybertech.dispute.domain.Evidence;

import java.util.List;

public interface EvidenceRepository extends JpaRepository<Evidence, Long> {
  List<Evidence> findByDisputeId(Long disputeId);
  long countByDisputeId(Long disputeId);
}
