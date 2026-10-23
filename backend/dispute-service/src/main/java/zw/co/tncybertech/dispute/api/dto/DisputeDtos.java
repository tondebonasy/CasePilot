package zw.co.tncybertech.dispute.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import zw.co.tncybertech.dispute.domain.DisputeStatus;
import zw.co.tncybertech.dispute.domain.DisputeType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public class DisputeDtos {

  public record CreateDisputeRequest(
      Long transactionId,
      String cardLast4,
      String merchantName,
      String merchantId,
      BigDecimal amount,
      String currency,
      Instant txDate,
      String network,
      String transactionRef,
      String rrn,
      String authCode,
      String channel,
      String terminalId,
      String terminalLocation,
      String customerIdentifier,
      Boolean merchantRefunded,
      String refundRef,
      Instant refundAt,
      @NotNull DisputeType disputeType,
      @NotBlank String reason
  ) {}

  public record DisputeSummary(
      Long id,
      String caseCode,
      Long transactionId,
      String merchantName,
      String cardLast4,
      String currency,
      String amount,
      DisputeType disputeType,
      DisputeStatus status,
      Long assignedToUserId,
      Instant openedAt,
      Instant dueAt,
      Instant networkDueAt,
      String network,
      boolean overdue,
      String resolutionOutcome
  ) {}

  public record DisputeDetail(
      Long id,
      String caseCode,
      TransactionDto transaction,
      DisputeType disputeType,
      String schemeCategory,
      String schemeReasonCode,
      String reason,
      String eligibilityStatus,
      String eligibilityNotes,
      DisputeStatus status,
      Long openedByUserId,
      Long assignedToUserId,
      Instant openedAt,
      Instant dueAt,
      Instant networkDueAt,
      Instant submittedAt,
      String submissionRef,
      Instant internalAlertedAt,
      Instant networkAlertedAt,
      Instant internalBreachAt,
      Instant networkBreachAt,
      boolean breachFlag,
      Instant closedAt,
      boolean overdue,
      String resolutionOutcome,
      List<ProtocolStep> protocolChecklist,
      List<EvidenceDto> evidence,
      List<ActivityDto> activity,
      List<FinancialPostingDto> postings,
      List<FraudFlagDto> fraudFlags
  ) {}

  public record TransactionDto(
      Long id,
      String merchantName,
      String merchantId,
      String cardLast4,
      String currency,
      String amount,
      Instant txDate,
      String status,
      String network,
      String channel,
      String transactionRef,
      String rrn,
      String authCode,
      String terminalId,
      String terminalLocation,
      String customerIdentifier,
      boolean merchantRefunded,
      String refundRef,
      Instant refundAt
  ) {}

  public record EvidenceDto(
      Long id,
      String fileName,
      String contentType,
      Instant uploadedAt
  ) {}

  public record ActivityDto(
      Long id,
      Long actorUserId,
      String action,
      String detail,
      String ipAddress,
      Instant atTime
  ) {}

  public record FinancialPostingDto(
      Long id,
      String postingType,
      String direction,
      String amount,
      String currency,
      String status,
      String reference,
      String notes,
      Instant createdAt,
      Instant postedAt,
      Instant reversedAt
  ) {}

  public record FraudFlagDto(
      Long id,
      String flagType,
      String severity,
      String detail,
      Instant createdAt
  ) {}

  public record ProtocolStep(
      String label,
      boolean completed
  ) {}

  public record AssignRequest(@NotNull Long assignedToUserId) {}

  public record UpdateStatusRequest(@NotNull DisputeStatus status, String detail) {}

  public record DashboardMetrics(
      long totalDisputes,
      long openDisputes,
      long overdueDisputes,
      long slaBreaches,
      double avgResolutionHours,
      double winRate,
      double lossRate,
      double overdueLossRate,
      double avgOverdueHours,
      double avgOverdueClosureHours,
      long fraudFlagCount,
      List<TopItem> topCauses
  ) {}

  public record TopItem(String label, long count) {}
}
