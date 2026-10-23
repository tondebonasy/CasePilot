package zw.co.tncybertech.dispute.repo;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import zw.co.tncybertech.dispute.domain.Dispute;
import zw.co.tncybertech.dispute.domain.DisputeStatus;

import java.time.Instant;
import java.util.List;
import java.util.Set;

public interface DisputeRepository extends JpaRepository<Dispute, Long> {
  Page<Dispute> findByOpenedByUserId(Long userId, Pageable pageable);

  Page<Dispute> findByAssignedToUserIdOrStatusInAndAssignedToUserIdIsNull(Long userId,
                                                                           Set<DisputeStatus> statuses,
                                                                           Pageable pageable);

  List<Dispute> findByOpenedAtBetween(Instant from, Instant to);

  List<Dispute> findByAssignedToUserIdOrderByOpenedAtDesc(Long userId);

  List<Dispute> findByAssignedToUserIdIsNullOrderByOpenedAtDesc();

  List<Dispute> findByStatusInAndAssignedToUserIdIsNullOrderByOpenedAtDesc(Set<DisputeStatus> statuses);

  boolean existsByTransactionId(Long transactionId);

  List<Dispute> findByClosedAtIsNullAndDueAtBetween(Instant from, Instant to);

  List<Dispute> findByClosedAtIsNullAndDueAtBefore(Instant time);

  List<Dispute> findByClosedAtIsNullAndNetworkDueAtBetween(Instant from, Instant to);

  List<Dispute> findByClosedAtIsNullAndNetworkDueAtBefore(Instant time);

  @Query("select count(d) from Dispute d where d.transaction.customerIdentifierHash = :hash and d.openedAt >= :from")
  long countByCustomerHashSince(@Param("hash") String hash, @Param("from") Instant from);

  @Query("select count(d) from Dispute d where d.transaction.merchantName = :merchant and d.openedAt >= :from")
  long countByMerchantSince(@Param("merchant") String merchant, @Param("from") Instant from);

  @Query("select count(d) from Dispute d where d.transaction.merchantName = :merchant and d.transaction.amount = :amount and d.openedAt >= :from")
  long countByMerchantAmountSince(@Param("merchant") String merchant, @Param("amount") java.math.BigDecimal amount, @Param("from") Instant from);
}
