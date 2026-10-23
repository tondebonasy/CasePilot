package zw.co.tncybertech.dispute.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import zw.co.tncybertech.dispute.domain.training.RbzPolicy;

import java.util.List;

public interface RbzPolicyRepository extends JpaRepository<RbzPolicy, Long> {
  List<RbzPolicy> findByActiveTrue();
}
