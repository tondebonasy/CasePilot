package zw.co.tncybertech.dispute.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import zw.co.tncybertech.dispute.domain.DisputeType;
import zw.co.tncybertech.dispute.domain.SlaRule;

import java.util.Optional;

public interface SlaRuleRepository extends JpaRepository<SlaRule, Long> {
  Optional<SlaRule> findByNetworkIgnoreCaseAndDisputeType(String network, DisputeType disputeType);
}
