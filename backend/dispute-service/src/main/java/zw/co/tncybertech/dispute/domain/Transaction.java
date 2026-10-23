package zw.co.tncybertech.dispute.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "transactions")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class Transaction {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "card_last4", nullable = false, length = 4)
  private String cardLast4;

  @Column(name = "merchant_name", nullable = false, length = 190)
  private String merchantName;

  @Column(name = "merchant_id", length = 40)
  private String merchantId;

  @Column(nullable = false, precision = 19, scale = 2)
  private BigDecimal amount;

  @Column(nullable = false, length = 10)
  private String currency;

  @Column(name = "tx_date", nullable = false)
  private Instant txDate;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 40)
  private TransactionStatus status;

  @Column(nullable = false, length = 40)
  private String network;

  @Column(name = "transaction_ref", nullable = false, length = 80)
  private String transactionRef;

  @Column(name = "rrn", length = 40)
  private String rrn;

  @Column(name = "auth_code", length = 20)
  private String authCode;

  @Column(nullable = false, length = 80)
  private String channel;

  @Column(name = "terminal_id", length = 40)
  private String terminalId;

  @Column(name = "terminal_location", length = 190)
  private String terminalLocation;

  @Column(name = "customer_identifier", nullable = false, length = 100)
  private String customerIdentifier;

  @Column(name = "customer_identifier_hash", nullable = false, length = 64)
  private String customerIdentifierHash;

  @Column(name = "merchant_refunded", nullable = false)
  @Builder.Default
  private boolean merchantRefunded = false;

  @Column(name = "refund_ref", length = 80)
  private String refundRef;

  @Column(name = "refund_at")
  private Instant refundAt;

  @Column(name = "created_at", nullable = false)
  @Builder.Default
  private Instant createdAt = Instant.now();

  @PrePersist
  void ensureCreatedAt() {
    if (createdAt == null) {
      createdAt = Instant.now();
    }
  }
}
