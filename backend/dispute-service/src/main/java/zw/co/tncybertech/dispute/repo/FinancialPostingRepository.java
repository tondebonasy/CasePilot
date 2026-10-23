package zw.co.tncybertech.dispute.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import zw.co.tncybertech.dispute.domain.FinancialPosting;

import java.util.List;

public interface FinancialPostingRepository extends JpaRepository<FinancialPosting, Long> {
  List<FinancialPosting> findByDisputeIdOrderByCreatedAtDesc(Long disputeId);
}
