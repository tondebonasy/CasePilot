package zw.co.tncybertech.dispute.api;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import jakarta.servlet.http.HttpServletRequest;
import zw.co.tncybertech.dispute.api.dto.DisputeDtos;
import zw.co.tncybertech.dispute.domain.Dispute;
import zw.co.tncybertech.dispute.domain.DisputeStatus;
import zw.co.tncybertech.dispute.repo.ActivityLogRepository;
import zw.co.tncybertech.dispute.repo.EvidenceRepository;
import zw.co.tncybertech.dispute.security.JwtClaims;
import zw.co.tncybertech.dispute.service.DisputeService;
import zw.co.tncybertech.dispute.service.StorageService;
import zw.co.tncybertech.dispute.util.DateRange;
import zw.co.tncybertech.dispute.util.RequestUtils;

import java.time.LocalDate;
import java.time.Instant;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class DisputeController {

  private final DisputeService service;
  private final EvidenceRepository evidenceRepo;
  private final ActivityLogRepository activityRepo;
  private final StorageService storageService;
  private final zw.co.tncybertech.dispute.repo.FinancialPostingRepository postingRepo;
  private final zw.co.tncybertech.dispute.repo.FraudFlagRepository fraudRepo;

  @PostMapping("/disputes")
  public ResponseEntity<?> create(@AuthenticationPrincipal JwtClaims claims,
                                  Authentication authentication,
                                  HttpServletRequest request,
                                  @Valid @RequestBody DisputeDtos.CreateDisputeRequest req) {
    ResolvedUser user = resolveUser(claims, authentication);
    Dispute d = service.create(user.userId(), req, RequestUtils.resolveIp(request));
    return ResponseEntity.ok(java.util.Map.of("id", d.getId()));
  }

  @GetMapping("/disputes")
  public ResponseEntity<?> list(@AuthenticationPrincipal JwtClaims claims,
                                Authentication authentication,
                                @RequestParam(defaultValue = "0") int page,
                                @RequestParam(defaultValue = "20") int size) {
    ResolvedUser user = resolveUser(claims, authentication);
    var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "openedAt"));
    var p = service.list(user.role(), user.userId(), pageable);

    var summaries = p.getContent().stream()
        .map(this::toSummary)
        .toList();

    return ResponseEntity.ok(java.util.Map.of(
        "items", summaries,
        "page", p.getNumber(),
        "size", p.getSize(),
        "totalElements", p.getTotalElements()
    ));
  }

  @GetMapping("/disputes/{id}")
  public ResponseEntity<?> get(@AuthenticationPrincipal JwtClaims claims,
                               Authentication authentication,
                               @PathVariable Long id) {
    ResolvedUser user = resolveUser(claims, authentication);
    Dispute d = service.get(id, user.role(), user.userId());

    var ev = evidenceRepo.findByDisputeId(d.getId()).stream()
        .map(e -> new DisputeDtos.EvidenceDto(e.getId(), e.getFileName(), e.getContentType(), e.getUploadedAt()))
        .toList();

    var act = activityRepo.findByDisputeIdOrderByAtTimeAsc(d.getId()).stream()
        .map(a -> new DisputeDtos.ActivityDto(a.getId(), a.getActorUserId(), a.getAction(), a.getDetail(), a.getIpAddress(), a.getAtTime()))
        .toList();

    var postings = postingRepo.findByDisputeIdOrderByCreatedAtDesc(d.getId()).stream()
        .map(p -> new DisputeDtos.FinancialPostingDto(
            p.getId(),
            p.getPostingType().name(),
            p.getDirection().name(),
            p.getAmount().toPlainString(),
            p.getCurrency(),
            p.getStatus().name(),
            p.getReference(),
            p.getNotes(),
            p.getCreatedAt(),
            p.getPostedAt(),
            p.getReversedAt()
        ))
        .toList();

    var fraudFlags = fraudRepo.findByDisputeIdOrderByCreatedAtDesc(d.getId()).stream()
        .map(f -> new DisputeDtos.FraudFlagDto(
            f.getId(),
            f.getFlagType().name(),
            f.getSeverity().name(),
            f.getDetail(),
            f.getCreatedAt()
        ))
        .toList();

    boolean opened = hasAction(act, "DISPUTE_OPENED") || d.getOpenedAt() != null;
    boolean assigned = hasAction(act, "ASSIGNED") || d.getStatus() != DisputeStatus.OPEN;
    boolean evidence = hasAction(act, "EVIDENCE_UPLOADED") || !ev.isEmpty();
    boolean underReview = hasStatus(act, "UNDER_REVIEW") || hasStatus(act, "READY_FOR_SUBMISSION")
        || d.getStatus() != DisputeStatus.OPEN;
    boolean submitted = hasStatus(act, "SUBMITTED")
        || Set.of(DisputeStatus.SUBMITTED, DisputeStatus.WON, DisputeStatus.LOST, DisputeStatus.CLOSED).contains(d.getStatus());
    boolean resolved = hasStatus(act, "WON") || hasStatus(act, "LOST") || hasStatus(act, "CLOSED")
        || d.getClosedAt() != null;

    var checklist = List.of(
        new DisputeDtos.ProtocolStep("Case opened", opened),
        new DisputeDtos.ProtocolStep("Assigned to reviewer", assigned),
        new DisputeDtos.ProtocolStep("Evidence collected", evidence),
        new DisputeDtos.ProtocolStep("Under review", underReview),
        new DisputeDtos.ProtocolStep("Submitted to network", submitted),
        new DisputeDtos.ProtocolStep("Resolved/Closed", resolved)
    );

    var tx = d.getTransaction();
    var detail = new DisputeDtos.DisputeDetail(
        d.getId(),
        d.getCaseCode(),
        new DisputeDtos.TransactionDto(
            tx.getId(),
            tx.getMerchantName(),
            tx.getMerchantId(),
            tx.getCardLast4(),
            tx.getCurrency(),
            tx.getAmount().toPlainString(),
            tx.getTxDate(),
            tx.getStatus().name(),
            tx.getNetwork(),
            tx.getChannel(),
            tx.getTransactionRef(),
            tx.getRrn(),
            tx.getAuthCode(),
            tx.getTerminalId(),
            tx.getTerminalLocation(),
            tx.getCustomerIdentifier(),
            tx.isMerchantRefunded(),
            tx.getRefundRef(),
            tx.getRefundAt()
        ),
        d.getDisputeType(),
        d.getSchemeCategory(),
        d.getSchemeReasonCode(),
        d.getReason(),
        d.getEligibilityStatus() == null ? null : d.getEligibilityStatus().name(),
        d.getEligibilityNotes(),
        d.getStatus(),
        d.getOpenedByUserId(),
        d.getAssignedToUserId(),
        d.getOpenedAt(),
        d.getDueAt(),
        d.getNetworkDueAt(),
        d.getSubmittedAt(),
        d.getSubmissionRef(),
        d.getInternalAlertedAt(),
        d.getNetworkAlertedAt(),
        d.getInternalBreachAt(),
        d.getNetworkBreachAt(),
        d.isBreachFlag(),
        d.getClosedAt(),
        d.getClosedAt() == null && d.getDueAt().isBefore(Instant.now()),
        resolutionOutcome(d, act),
        checklist,
        ev,
        act,
        postings,
        fraudFlags
    );

    return ResponseEntity.ok(detail);
  }

  @PutMapping("/disputes/{id}/assign")
  public ResponseEntity<?> assign(@AuthenticationPrincipal JwtClaims claims,
                                  Authentication authentication,
                                  HttpServletRequest request,
                                  @PathVariable Long id,
                                  @Valid @RequestBody DisputeDtos.AssignRequest req) {
    ResolvedUser user = resolveUser(claims, authentication);
    service.assign(id, user.userId(), user.role(), req.assignedToUserId(), RequestUtils.resolveIp(request));
    return ResponseEntity.ok().build();
  }

  @PutMapping("/disputes/{id}/status")
  public ResponseEntity<?> updateStatus(@AuthenticationPrincipal JwtClaims claims,
                                        Authentication authentication,
                                        HttpServletRequest request,
                                        @PathVariable Long id,
                                        @Valid @RequestBody DisputeDtos.UpdateStatusRequest req) {
    ResolvedUser user = resolveUser(claims, authentication);
    service.updateStatus(id, user.userId(), user.role(), req, RequestUtils.resolveIp(request));
    return ResponseEntity.ok().build();
  }

  @PutMapping("/disputes/{id}/claim")
  public ResponseEntity<?> claim(@AuthenticationPrincipal JwtClaims claims,
                                 Authentication authentication,
                                 HttpServletRequest request,
                                 @PathVariable Long id) {
    ResolvedUser user = resolveUser(claims, authentication);
    service.claim(id, user.userId(), user.role(), RequestUtils.resolveIp(request));
    return ResponseEntity.ok().build();
  }

  @PutMapping("/disputes/{id}/reopen")
  public ResponseEntity<?> reopen(@AuthenticationPrincipal JwtClaims claims,
                                  Authentication authentication,
                                  HttpServletRequest request,
                                  @PathVariable Long id) {
    requireAdmin(claims, authentication);
    ResolvedUser user = resolveUser(claims, authentication);
    service.reopen(id, user.userId(), "ADMIN", RequestUtils.resolveIp(request));
    return ResponseEntity.ok().build();
  }

  @GetMapping("/disputes/tasks")
  public ResponseEntity<?> tasks(@AuthenticationPrincipal JwtClaims claims,
                                 Authentication authentication) {
    ResolvedUser user = resolveUser(claims, authentication);
    var items = service.tasks(user.userId(), user.role()).stream()
        .map(this::toSummary)
        .toList();
    return ResponseEntity.ok(items);
  }

  @GetMapping("/disputes/queue")
  public ResponseEntity<?> queue(@AuthenticationPrincipal JwtClaims claims,
                                 Authentication authentication) {
    ResolvedUser user = resolveUser(claims, authentication);
    var items = service.queue(user.role()).stream()
        .map(this::toSummary)
        .toList();
    return ResponseEntity.ok(items);
  }

  @PostMapping("/disputes/{id}/evidence")
  public ResponseEntity<?> upload(@AuthenticationPrincipal JwtClaims claims,
                                  Authentication authentication,
                                  HttpServletRequest request,
                                  @PathVariable Long id,
                                  @RequestParam("file") MultipartFile file) throws Exception {
    ResolvedUser user = resolveUser(claims, authentication);
    var e = service.uploadEvidence(id, user.userId(), user.role(), file, RequestUtils.resolveIp(request));
    return ResponseEntity.ok(java.util.Map.of("evidenceId", e.getId()));
  }

  @GetMapping("/disputes/{id}/evidence/{evidenceId}/download")
  public ResponseEntity<Resource> downloadEvidence(@AuthenticationPrincipal JwtClaims claims,
                                                   Authentication authentication,
                                                   @PathVariable Long id,
                                                   @PathVariable Long evidenceId) {
    return evidenceResponse(claims, authentication, id, evidenceId, true);
  }

  @GetMapping("/disputes/{id}/evidence/{evidenceId}/view")
  public ResponseEntity<Resource> viewEvidence(@AuthenticationPrincipal JwtClaims claims,
                                               Authentication authentication,
                                               @PathVariable Long id,
                                               @PathVariable Long evidenceId) {
    return evidenceResponse(claims, authentication, id, evidenceId, false);
  }

  private ResponseEntity<Resource> evidenceResponse(JwtClaims claims,
                                                    Authentication authentication,
                                                    Long id,
                                                    Long evidenceId,
                                                    boolean attachment) {
    ResolvedUser user = resolveUser(claims, authentication);
    service.get(id, user.role(), user.userId());
    var evidence = evidenceRepo.findById(evidenceId)
        .filter(e -> e.getDispute().getId().equals(id))
        .orElseThrow(() -> new IllegalArgumentException("Evidence not found"));

    var path = storageService.resolveStoredPath(evidence.getStoragePath());
    Resource resource = new FileSystemResource(path);
    MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;
    if (evidence.getContentType() != null && !evidence.getContentType().isBlank()) {
      try {
        mediaType = MediaType.parseMediaType(evidence.getContentType());
      } catch (Exception ignored) {
        mediaType = MediaType.APPLICATION_OCTET_STREAM;
      }
    }

    ContentDisposition disposition = (attachment ? ContentDisposition.attachment() : ContentDisposition.inline())
        .filename(evidence.getFileName(), StandardCharsets.UTF_8)
        .build();

    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
        .contentType(mediaType)
        .body(resource);
  }

  @GetMapping("/metrics/dashboard")
  public ResponseEntity<?> dashboard(@RequestParam(required = false) String from,
                                     @RequestParam(required = false) String to) {
    DateRange range = DateRange.resolve(from, to);
    return ResponseEntity.ok(service.metrics(range.from(), range.to()));
  }

  @GetMapping({"/metrics/bank", "/metrics/rbz"})
  public ResponseEntity<?> rbzReport(@RequestParam(required = false) String from,
                                     @RequestParam(required = false) String to) {
    DateRange range = DateRange.resolve(from, to);
    return ResponseEntity.ok(service.rbzReport(range.from(), range.to()));
  }

  @GetMapping(value = "/metrics/dashboard/export", produces = "text/csv")
  public ResponseEntity<String> exportDashboard(@RequestParam(required = false) String from,
                                                @RequestParam(required = false) String to) {
    DateRange range = DateRange.resolve(from, to);
    var m = service.metrics(range.from(), range.to());

    StringBuilder sb = new StringBuilder();
    sb.append("Metric,Value\n");
    sb.append("From,").append(range.from()).append("\n");
    sb.append("To,").append(range.to()).append("\n");
    sb.append("Total Disputes,").append(m.totalDisputes()).append("\n");
    sb.append("Open Disputes,").append(m.openDisputes()).append("\n");
    sb.append("Overdue Disputes,").append(m.overdueDisputes()).append("\n");
    sb.append("SLA Breaches,").append(m.slaBreaches()).append("\n");
    sb.append("Avg Resolution Hours,").append(m.avgResolutionHours()).append("\n");
    sb.append("Win Rate,").append(m.winRate()).append("\n");
    sb.append("Loss Rate,").append(m.lossRate()).append("\n");
    sb.append("Overdue Loss Rate,").append(m.overdueLossRate()).append("\n");
    sb.append("Avg Overdue Hours,").append(m.avgOverdueHours()).append("\n");
    sb.append("Avg Overdue Closure Hours,").append(m.avgOverdueClosureHours()).append("\n");
    sb.append("Fraud Flag Count,").append(m.fraudFlagCount()).append("\n");

    sb.append("\nTop Causes\n");
    sb.append("Cause,Count\n");
    m.topCauses().forEach(c -> sb.append(csv(c.label())).append(",").append(c.count()).append("\n"));

    String filename = "dashboard-metrics-" + LocalDate.now() + ".csv";
    return ResponseEntity.ok()
        .header("Content-Disposition", "attachment; filename=" + filename)
        .body(sb.toString());
  }

  @GetMapping(value = {"/metrics/bank/export", "/metrics/rbz/export"}, produces = "text/csv")
  public ResponseEntity<String> exportRbz(@RequestParam(required = false) String from,
                                          @RequestParam(required = false) String to) {
    DateRange range = DateRange.resolve(from, to);
    var m = service.rbzReport(range.from(), range.to());

    StringBuilder sb = new StringBuilder();
    sb.append("Metric,Value\n");
    sb.append("From,").append(m.from()).append("\n");
    sb.append("To,").append(m.to()).append("\n");
    sb.append("Total Disputes,").append(m.totalDisputes()).append("\n");
    sb.append("Open Disputes,").append(m.openDisputes()).append("\n");
    sb.append("Closed Disputes,").append(m.closedDisputes()).append("\n");
    sb.append("SLA Breaches,").append(m.slaBreaches()).append("\n");
    sb.append("Avg Resolution Hours,").append(m.avgResolutionHours()).append("\n");
    sb.append("Win Count,").append(m.winCount()).append("\n");
    sb.append("Loss Count,").append(m.lossCount()).append("\n");

    sb.append("\nDisputes By Type\n");
    sb.append("Type,Count\n");
    m.byType().forEach(c -> sb.append(csv(c.label())).append(",").append(c.count()).append("\n"));

    sb.append("\nDisputes By Channel\n");
    sb.append("Channel,Count\n");
    m.byChannel().forEach(c -> sb.append(csv(c.label())).append(",").append(c.count()).append("\n"));

    sb.append("\nDisputes By Merchant\n");
    sb.append("Merchant,Count\n");
    m.byMerchant().forEach(c -> sb.append(csv(c.label())).append(",").append(c.count()).append("\n"));

    sb.append("\nFraud Flags By Type\n");
    sb.append("Type,Count\n");
    m.fraudByType().forEach(c -> sb.append(csv(c.label())).append(",").append(c.count()).append("\n"));

    String filename = "bank-report-" + LocalDate.now() + ".csv";
    return ResponseEntity.ok()
        .header("Content-Disposition", "attachment; filename=" + filename)
        .body(sb.toString());
  }

  private static String csv(String value) {
    if (value == null) return "";
    String v = value.replace("\"", "\"\"");
    return v.contains(",") || v.contains("\n") ? "\"" + v + "\"" : v;
  }

  private static boolean hasAction(List<DisputeDtos.ActivityDto> activities, String action) {
    return activities.stream().anyMatch(a -> action.equalsIgnoreCase(a.action()));
  }

  private static boolean hasStatus(List<DisputeDtos.ActivityDto> activities, String status) {
    return activities.stream().anyMatch(a -> {
      if (a.detail() == null) return false;
      String d = a.detail().trim();
      return d.equalsIgnoreCase(status) || d.startsWith(status + " ") || d.startsWith(status + "•") || d.startsWith(status + " •");
    });
  }

  private DisputeDtos.DisputeSummary toSummary(Dispute d) {
    return new DisputeDtos.DisputeSummary(
        d.getId(),
        d.getCaseCode(),
        d.getTransaction().getId(),
        d.getTransaction().getMerchantName(),
        d.getTransaction().getCardLast4(),
        d.getTransaction().getCurrency(),
        d.getTransaction().getAmount().toPlainString(),
        d.getDisputeType(),
        d.getStatus(),
        d.getAssignedToUserId(),
        d.getOpenedAt(),
        d.getDueAt(),
        d.getNetworkDueAt(),
        d.getTransaction().getNetwork(),
        d.getClosedAt() == null && d.getDueAt().isBefore(Instant.now()),
        resolutionOutcome(d, activityRepo.findByDisputeIdOrderByAtTimeAsc(d.getId()).stream()
            .map(a -> new DisputeDtos.ActivityDto(a.getId(), a.getActorUserId(), a.getAction(), a.getDetail(), a.getIpAddress(), a.getAtTime()))
            .toList())
    );
  }

  private String resolutionOutcome(Dispute d, List<DisputeDtos.ActivityDto> activities) {
    if (d.getStatus() == DisputeStatus.WON) {
      return "WON";
    }
    if (d.getStatus() == DisputeStatus.LOST) {
      return "LOST";
    }

    for (int i = activities.size() - 1; i >= 0; i--) {
      var activity = activities.get(i);
      if (!"STATUS_CHANGED".equalsIgnoreCase(activity.action()) || activity.detail() == null) {
        continue;
      }
      String detail = activity.detail().toUpperCase();
      if (detail.startsWith("WON")) {
        return "WON";
      }
      if (detail.startsWith("LOST")) {
        return "LOST";
      }
    }
    return null;
  }

  private void requireAdmin(JwtClaims claims, Authentication authentication) {
    if (claims != null && "ADMIN".equalsIgnoreCase(claims.role())) {
      return;
    }
    if (authentication != null) {
      for (GrantedAuthority authority : authentication.getAuthorities()) {
        String value = authority.getAuthority();
        if ("ADMIN".equals(value) || "ROLE_ADMIN".equals(value)) {
          return;
        }
      }
    }
    throw new SecurityException("Forbidden");
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
