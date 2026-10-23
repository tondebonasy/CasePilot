package zw.co.tncybertech.dispute.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "financial_postings")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class FinancialPosting {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "dispute_id")
  private Dispute dispute;

  @Enumerated(EnumType.STRING)
  @Column(name = "posting_type", nullable = false, length = 40)
  private PostingType postingType;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 10)
  private PostingDirection direction;

  @Column(nullable = false, precision = 19, scale = 2)
  private BigDecimal amount;

  @Column(nullable = false, length = 10)
  private String currency;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private PostingStatus status;

  @Column(name = "reference", length = 80)
  private String reference;

  @Column(name = "notes", columnDefinition = "TEXT")
  private String notes;

  @Column(name = "created_at", nullable = false)
  @Builder.Default
  private Instant createdAt = Instant.now();

  @Column(name = "posted_at")
  private Instant postedAt;

  @Column(name = "reversed_at")
  private Instant reversedAt;

  @PrePersist
  void ensureCreatedAt() {
    if (createdAt == null) {
      createdAt = Instant.now();
    }
  }
}
