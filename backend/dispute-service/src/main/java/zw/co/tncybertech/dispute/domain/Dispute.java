package zw.co.tncybertech.dispute.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "disputes")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class Dispute {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "case_code", nullable = false, length = 40)
  private String caseCode;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "transaction_id")
  private Transaction transaction;

  @Enumerated(EnumType.STRING)
  @Column(name = "dispute_type", nullable = false, length = 60)
  private DisputeType disputeType;

  @Column(name = "scheme_category", length = 60)
  private String schemeCategory;

  @Column(name = "scheme_reason_code", length = 20)
  private String schemeReasonCode;

  @Column(nullable = false, columnDefinition = "TEXT")
  private String reason;

  @Enumerated(EnumType.STRING)
  @Column(name = "eligibility_status", nullable = false, length = 40)
  private EligibilityStatus eligibilityStatus;

  @Column(name = "eligibility_notes", columnDefinition = "TEXT")
  private String eligibilityNotes;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 60)
  private DisputeStatus status;

  @Column(name = "opened_by_user_id", nullable = false)
  private Long openedByUserId;

  @Column(name = "assigned_to_user_id")
  private Long assignedToUserId;

  @Column(name = "opened_at", nullable = false)
  private Instant openedAt;

  @Column(name = "due_at", nullable = false)
  private Instant dueAt;

  @Column(name = "network_due_at")
  private Instant networkDueAt;

  @Column(name = "submitted_at")
  private Instant submittedAt;

  @Column(name = "submission_ref", length = 80)
  private String submissionRef;

  @Column(name = "internal_alerted_at")
  private Instant internalAlertedAt;

  @Column(name = "network_alerted_at")
  private Instant networkAlertedAt;

  @Column(name = "internal_breach_at")
  private Instant internalBreachAt;

  @Column(name = "network_breach_at")
  private Instant networkBreachAt;

  @Column(name = "breach_flag", nullable = false)
  @Builder.Default
  private boolean breachFlag = false;

  @Column(name = "closed_at")
  private Instant closedAt;
}
