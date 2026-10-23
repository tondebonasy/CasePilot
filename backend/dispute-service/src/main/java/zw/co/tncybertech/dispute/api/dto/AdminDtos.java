package zw.co.tncybertech.dispute.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public class AdminDtos {

  public record SlaRuleRequest(
      @NotBlank String network,
      @NotBlank String disputeType,
      @Min(1) int internalDays,
      @Min(1) int networkDays
  ) {}

  public record SlaRuleResponse(
      Long id,
      String network,
      String disputeType,
      int internalDays,
      int networkDays,
      Instant createdAt
  ) {}

  public record PolicyRequest(
      @NotBlank String title,
      @NotBlank String circularReference,
      @NotBlank String summary,
      @NotNull LocalDate effectiveDate,
      @NotBlank String keyRequirements,
      boolean active
  ) {}

  public record PolicyResponse(
      Long id,
      String title,
      String circularReference,
      String summary,
      LocalDate effectiveDate,
      String keyRequirements,
      boolean active,
      Instant createdAt
  ) {}

  public record QuizRequest(
      @NotNull Long policyId,
      @NotBlank String title,
      @NotBlank String description,
      @NotEmpty List<@Valid QuestionRequest> questions
  ) {}

  public record QuestionRequest(
      @NotBlank String question,
      String explanation,
      @NotEmpty List<@Valid OptionRequest> options
  ) {}

  public record OptionRequest(
      @NotBlank String text,
      boolean correct
  ) {}

  public record QuizResponse(
      Long id,
      String title,
      String description,
      Long policyId,
      Instant createdAt,
      List<QuestionResponse> questions
  ) {}

  public record QuestionResponse(
      Long id,
      String question,
      String explanation,
      List<OptionResponse> options
  ) {}

  public record OptionResponse(
      Long id,
      String text,
      boolean correct
  ) {}

  public record AuditLogResponse(
      Long id,
      Long actorUserId,
      String actorRole,
      String action,
      String detail,
      String ipAddress,
      Instant atTime
  ) {}
}
