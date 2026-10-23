package zw.co.tncybertech.dispute.api;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;
import zw.co.tncybertech.dispute.api.dto.TrainingDtos;
import zw.co.tncybertech.dispute.security.JwtClaims;
import zw.co.tncybertech.dispute.service.TrainingService;
import zw.co.tncybertech.dispute.util.DateRange;

import java.util.List;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/training")
@RequiredArgsConstructor
public class TrainingController {

  private final TrainingService trainingService;

  @GetMapping("/policies")
  public List<TrainingDtos.PolicySummary> policies() {
    return trainingService.listPolicies();
  }

  @GetMapping("/quizzes")
  public List<TrainingDtos.QuizDetail> quizzes(@RequestParam(required = false) Long policyId) {
    return trainingService.listQuizzes(policyId);
  }

  @GetMapping("/quizzes/{id}")
  public TrainingDtos.QuizDetail quiz(@PathVariable Long id) {
    return trainingService.getQuiz(id);
  }

  @GetMapping("/metrics")
  public TrainingDtos.AwarenessMetrics metrics(@RequestParam(required = false) String from,
                                               @RequestParam(required = false) String to) {
    DateRange range = DateRange.resolve(from, to);
    return trainingService.awarenessMetrics(range.from(), range.to());
  }

  @GetMapping(value = "/metrics/export", produces = "text/csv")
  public org.springframework.http.ResponseEntity<String> exportMetrics(@RequestParam(required = false) String from,
                                                                       @RequestParam(required = false) String to) {
    DateRange range = DateRange.resolve(from, to);
    TrainingDtos.AwarenessMetrics metrics = trainingService.awarenessMetrics(range.from(), range.to());

    StringBuilder sb = new StringBuilder();
    sb.append("Metric,Value\n");
    sb.append("From,").append(range.from()).append("\n");
    sb.append("To,").append(range.to()).append("\n");
    sb.append("Average Score,").append(metrics.averageScore()).append("\n");
    sb.append("Completion Count,").append(metrics.completionCount()).append("\n");

    sb.append("\nAwareness By Policy\n");
    sb.append("Policy,Average,Attempts\n");
    metrics.byPolicy().forEach(p -> sb.append(csv(p.policyTitle())).append(",")
        .append(p.averageScore()).append(",")
        .append(p.attempts()).append("\n"));

    sb.append("\nAwareness By Role\n");
    sb.append("Role,Average,Attempts\n");
    metrics.byRole().forEach(r -> sb.append(csv(r.role())).append(",")
        .append(r.averageScore()).append(",")
        .append(r.attempts()).append("\n"));

    String filename = "training-metrics-" + LocalDate.now() + ".csv";
    return org.springframework.http.ResponseEntity.ok()
        .header("Content-Disposition", "attachment; filename=" + filename)
        .body(sb.toString());
  }

  private static String csv(String value) {
    if (value == null) return "";
    String v = value.replace("\"", "\"\"");
    return v.contains(",") || v.contains("\n") ? "\"" + v + "\"" : v;
  }

  @GetMapping("/attempts/me")
  public List<TrainingDtos.AttemptSummary> myAttempts(@AuthenticationPrincipal JwtClaims claims,
                                                      Authentication authentication) {
    ResolvedUser user = resolveUser(claims, authentication);
    return trainingService.myAttempts(user.userId());
  }

  @PostMapping("/attempts")
  public TrainingDtos.AttemptResult submit(@AuthenticationPrincipal JwtClaims claims,
                                           Authentication authentication,
                                           @Valid @RequestBody TrainingDtos.SubmitAttemptRequest req) {
    ResolvedUser user = resolveUser(claims, authentication);
    return trainingService.submitAttempt(user.userId(), user.role(), req);
  }

  private ResolvedUser resolveUser(JwtClaims claims, Authentication authentication) {
    if (claims != null) {
      return new ResolvedUser(claims.userId(), claims.role());
    }
    if (authentication == null) {
      throw new SecurityException("Forbidden");
    }

    Long userId = null;
    Object principal = authentication.getPrincipal();
    if (principal instanceof JwtClaims jwtClaims) {
      userId = jwtClaims.userId();
    } else if (principal instanceof String s) {
      try {
        userId = Long.valueOf(s);
      } catch (NumberFormatException ignored) {
        userId = null;
      }
    } else if (principal instanceof Long l) {
      userId = l;
    }

    String role = resolveRole(authentication);
    if (userId == null) {
      throw new SecurityException("Forbidden");
    }
    return new ResolvedUser(userId, role);
  }

  private String resolveRole(Authentication authentication) {
    String role = null;
    for (GrantedAuthority authority : authentication.getAuthorities()) {
      String value = authority.getAuthority();
      if ("ADMIN".equals(value) || "ROLE_ADMIN".equals(value)) {
        return "ADMIN";
      }
      if (role == null && value != null && !value.isBlank()) {
        role = value.startsWith("ROLE_") ? value.substring(5) : value;
      }
    }
    return role == null ? "" : role;
  }

  private record ResolvedUser(Long userId, String role) {}
}
