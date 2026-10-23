package zw.co.tncybertech.dispute.api;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;
import zw.co.tncybertech.dispute.api.dto.AdminDtos;
import zw.co.tncybertech.dispute.security.JwtClaims;
import zw.co.tncybertech.dispute.service.AdminContentService;
import zw.co.tncybertech.dispute.util.RequestUtils;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminContentController {

  private final AdminContentService adminService;

  @GetMapping("/sla")
  public ResponseEntity<List<AdminDtos.SlaRuleResponse>> listSlaRules(@AuthenticationPrincipal JwtClaims claims,
                                                                      Authentication authentication) {
    requireAdmin(claims, authentication);
    return ResponseEntity.ok(adminService.listSlaRules());
  }

  @GetMapping("/audit")
  public ResponseEntity<List<AdminDtos.AuditLogResponse>> listAuditLogs(@AuthenticationPrincipal JwtClaims claims,
                                                                        Authentication authentication) {
    requireAdmin(claims, authentication);
    return ResponseEntity.ok(adminService.listAuditLogs());
  }

  @PostMapping("/sla")
  public ResponseEntity<AdminDtos.SlaRuleResponse> createSlaRule(@AuthenticationPrincipal JwtClaims claims,
                                                                 Authentication authentication,
                                                                 HttpServletRequest request,
                                                                 @Valid @RequestBody AdminDtos.SlaRuleRequest req) {
    requireAdmin(claims, authentication);
    return ResponseEntity.ok(
        adminService.createSlaRule(req, claims.userId(), claims.role(), RequestUtils.resolveIp(request))
    );
  }

  @PutMapping("/sla/{id}")
  public ResponseEntity<AdminDtos.SlaRuleResponse> updateSlaRule(@AuthenticationPrincipal JwtClaims claims,
                                                                 Authentication authentication,
                                                                 HttpServletRequest request,
                                                                 @PathVariable Long id,
                                                                 @Valid @RequestBody AdminDtos.SlaRuleRequest req) {
    requireAdmin(claims, authentication);
    return ResponseEntity.ok(
        adminService.updateSlaRule(id, req, claims.userId(), claims.role(), RequestUtils.resolveIp(request))
    );
  }

  @GetMapping("/training/policies")
  public ResponseEntity<List<AdminDtos.PolicyResponse>> listPolicies(@AuthenticationPrincipal JwtClaims claims,
                                                                     Authentication authentication) {
    requireAdmin(claims, authentication);
    return ResponseEntity.ok(adminService.listPolicies());
  }

  @PostMapping("/training/policies")
  public ResponseEntity<AdminDtos.PolicyResponse> createPolicy(@AuthenticationPrincipal JwtClaims claims,
                                                               Authentication authentication,
                                                               @Valid @RequestBody AdminDtos.PolicyRequest req) {
    requireAdmin(claims, authentication);
    return ResponseEntity.ok(adminService.createPolicy(req));
  }

  @PutMapping("/training/policies/{id}")
  public ResponseEntity<AdminDtos.PolicyResponse> updatePolicy(@AuthenticationPrincipal JwtClaims claims,
                                                               Authentication authentication,
                                                               @PathVariable Long id,
                                                               @Valid @RequestBody AdminDtos.PolicyRequest req) {
    requireAdmin(claims, authentication);
    return ResponseEntity.ok(adminService.updatePolicy(id, req));
  }

  @GetMapping("/training/quizzes")
  public ResponseEntity<List<AdminDtos.QuizResponse>> listQuizzes(@AuthenticationPrincipal JwtClaims claims,
                                                                  Authentication authentication) {
    requireAdmin(claims, authentication);
    return ResponseEntity.ok(adminService.listQuizzes());
  }

  @PostMapping("/training/quizzes")
  public ResponseEntity<AdminDtos.QuizResponse> createQuiz(@AuthenticationPrincipal JwtClaims claims,
                                                           Authentication authentication,
                                                           @Valid @RequestBody AdminDtos.QuizRequest req) {
    requireAdmin(claims, authentication);
    return ResponseEntity.ok(adminService.createQuiz(req));
  }

  private void requireAdmin(JwtClaims claims, Authentication authentication) {
    if (claims != null && "ADMIN".equals(claims.role())) {
      return;
    }
    if (authentication != null) {
      for (GrantedAuthority authority : authentication.getAuthorities()) {
        String a = authority.getAuthority();
        if ("ADMIN".equals(a) || "ROLE_ADMIN".equals(a)) {
          return;
        }
      }
    }
    throw new SecurityException("Forbidden");
  }
}
