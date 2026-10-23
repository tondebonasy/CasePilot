package zw.co.tncybertech.dispute.api.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public class TrainingDtos {

  public record PolicySummary(
      Long id,
      String title,
      String circularReference,
      String summary,
      LocalDate effectiveDate,
      String keyRequirements
  ) {}

  public record QuizDetail(
      Long id,
      String title,
      String description,
      Long policyId,
      List<QuestionDto> questions
  ) {}

  public record QuestionDto(
      Long id,
      String question,
      List<QuizOptionDto> options
  ) {}

  public record QuizOptionDto(
      Long id,
      String text
  ) {}

  public record SubmitAttemptRequest(
      Long quizId,
      List<Answer> answers
  ) {}

  public record Answer(
      Long questionId,
      Long optionId
  ) {}

  public record AttemptResult(
      long score,
      long maxScore,
      List<Weakness> weaknesses
  ) {}

  public record Weakness(
      Long questionId,
      String question,
      String explanation
  ) {}

  public record AwarenessMetrics(
      double averageScore,
      long completionCount,
      List<PolicyAwareness> byPolicy,
      List<RoleAwareness> byRole
  ) {}

  public record PolicyAwareness(
      Long policyId,
      String policyTitle,
      double averageScore,
      long attempts
  ) {}

  public record RoleAwareness(
      String role,
      double averageScore,
      long attempts
  ) {}

  public record AttemptSummary(
      Long id,
      String quizTitle,
      long score,
      long maxScore,
      Instant createdAt
  ) {}
}
