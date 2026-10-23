package zw.co.tncybertech.dispute.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import zw.co.tncybertech.dispute.api.dto.DisputeDtos;
import zw.co.tncybertech.dispute.domain.*;
import zw.co.tncybertech.dispute.repo.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DisputeService {

  private static final Set<String> STAFF_ROLES = Set.of(
      "ADMIN",
      "INTAKE_OFFICER",
      "REVIEWER",
      "EVIDENCE_OFFICER",
      "NETWORK_LIAISON",
      "RESOLUTION_OFFICER",
      "AGENT",
      "COMPLIANCE"
  );
  private static final Map<DisputeType, Integer> SCHEME_TIME_LIMIT_DAYS = Map.ofEntries(
      Map.entry(DisputeType.UNAUTHORIZED, 120),
      Map.entry(DisputeType.DUPLICATE, 120),
      Map.entry(DisputeType.NOT_RECEIVED, 120),
      Map.entry(DisputeType.DEFECTIVE_OR_NOT_AS_DESCRIBED, 120),
      Map.entry(DisputeType.CANCELLED_RECURRING, 120),
      Map.entry(DisputeType.PROCESSING_ERROR, 90),
      Map.entry(DisputeType.OTHER, 120)
  );
  private static final Map<DisputeType, Set<String>> ALLOWED_CHANNELS = Map.ofEntries(
      Map.entry(DisputeType.UNAUTHORIZED, Set.of("POS", "E-COMMERCE", "MOBILE", "BRANCH", "ATM")),
      Map.entry(DisputeType.DUPLICATE, Set.of("POS", "E-COMMERCE", "MOBILE", "BRANCH", "ATM")),
      Map.entry(DisputeType.NOT_RECEIVED, Set.of("E-COMMERCE", "MOBILE")),
      Map.entry(DisputeType.DEFECTIVE_OR_NOT_AS_DESCRIBED, Set.of("POS", "E-COMMERCE")),
      Map.entry(DisputeType.CANCELLED_RECURRING, Set.of("E-COMMERCE", "MOBILE")),
      Map.entry(DisputeType.PROCESSING_ERROR, Set.of("POS", "BRANCH", "ATM")),
      Map.entry(DisputeType.OTHER, Set.of("POS", "E-COMMERCE", "MOBILE", "BRANCH", "ATM"))
  );
  private static final Map<String, Map<DisputeType, SchemeReason>> SCHEME_REASON_MAP = Map.ofEntries(
      Map.entry("VISA", Map.ofEntries(
          Map.entry(DisputeType.UNAUTHORIZED, new SchemeReason("Fraud", "10.4")),
          Map.entry(DisputeType.DUPLICATE, new SchemeReason("Processing Errors", "12.6")),
          Map.entry(DisputeType.NOT_RECEIVED, new SchemeReason("Consumer Disputes", "13.1")),
          Map.entry(DisputeType.DEFECTIVE_OR_NOT_AS_DESCRIBED, new SchemeReason("Consumer Disputes", "13.3")),
          Map.entry(DisputeType.CANCELLED_RECURRING, new SchemeReason("Consumer Disputes", "13.2")),
          Map.entry(DisputeType.PROCESSING_ERROR, new SchemeReason("Processing Errors", "12.5")),
          Map.entry(DisputeType.OTHER, new SchemeReason("Other", "12.9"))
      )),
      Map.entry("MASTERCARD", Map.ofEntries(
          Map.entry(DisputeType.UNAUTHORIZED, new SchemeReason("Fraud", "4837")),
          Map.entry(DisputeType.DUPLICATE, new SchemeReason("Processing Errors", "4834")),
          Map.entry(DisputeType.NOT_RECEIVED, new SchemeReason("Consumer Disputes", "4855")),
          Map.entry(DisputeType.DEFECTIVE_OR_NOT_AS_DESCRIBED, new SchemeReason("Consumer Disputes", "4853")),
          Map.entry(DisputeType.CANCELLED_RECURRING, new SchemeReason("Consumer Disputes", "4841")),
          Map.entry(DisputeType.PROCESSING_ERROR, new SchemeReason("Processing Errors", "4831")),
          Map.entry(DisputeType.OTHER, new SchemeReason("Other", "4808"))
      )),
      Map.entry("ZIMSWITCH", Map.ofEntries(
          Map.entry(DisputeType.UNAUTHORIZED, new SchemeReason("Local Fraud", "ZSW-01")),
          Map.entry(DisputeType.DUPLICATE, new SchemeReason("Local Processing", "ZSW-02")),
          Map.entry(DisputeType.NOT_RECEIVED, new SchemeReason("Local Consumer", "ZSW-03")),
          Map.entry(DisputeType.DEFECTIVE_OR_NOT_AS_DESCRIBED, new SchemeReason("Local Consumer", "ZSW-04")),
          Map.entry(DisputeType.CANCELLED_RECURRING, new SchemeReason("Local Consumer", "ZSW-05")),
          Map.entry(DisputeType.PROCESSING_ERROR, new SchemeReason("Local Processing", "ZSW-06")),
          Map.entry(DisputeType.OTHER, new SchemeReason("Local Other", "ZSW-99"))
      ))
  );
  private static final int FALLBACK_INTERNAL_DAYS = 10;
  private static final int FALLBACK_NETWORK_DAYS = 16;
  private static final Map<DisputeStatus, Set<DisputeStatus>> TRANSITIONS = Map.ofEntries(
      Map.entry(DisputeStatus.INELIGIBLE, Set.of()),
      Map.entry(DisputeStatus.OPEN, Set.of(DisputeStatus.UNDER_REVIEW, DisputeStatus.CANCELLED)),
      Map.entry(DisputeStatus.UNDER_REVIEW, Set.of(DisputeStatus.NEEDS_INFO, DisputeStatus.CANCELLED)),
      Map.entry(DisputeStatus.NEEDS_INFO, Set.of(DisputeStatus.READY_FOR_SUBMISSION, DisputeStatus.CANCELLED)),
      Map.entry(DisputeStatus.READY_FOR_SUBMISSION, Set.of(DisputeStatus.SUBMITTED, DisputeStatus.CANCELLED)),
      Map.entry(DisputeStatus.SUBMITTED, Set.of(DisputeStatus.WON, DisputeStatus.LOST)),
      Map.entry(DisputeStatus.WON, Set.of(DisputeStatus.CLOSED)),
      Map.entry(DisputeStatus.LOST, Set.of(DisputeStatus.CLOSED)),
      Map.entry(DisputeStatus.CLOSED, Set.of()),
      Map.entry(DisputeStatus.CANCELLED, Set.of())
  );
  private static final Map<DisputeStatus, String> ROLE_BY_STATUS = Map.ofEntries(
      Map.entry(DisputeStatus.OPEN, "INTAKE_OFFICER"),
      Map.entry(DisputeStatus.UNDER_REVIEW, "REVIEWER"),
      Map.entry(DisputeStatus.NEEDS_INFO, "EVIDENCE_OFFICER"),
      Map.entry(DisputeStatus.READY_FOR_SUBMISSION, "NETWORK_LIAISON"),
      Map.entry(DisputeStatus.SUBMITTED, "RESOLUTION_OFFICER"),
      Map.entry(DisputeStatus.WON, "RESOLUTION_OFFICER"),
      Map.entry(DisputeStatus.LOST, "RESOLUTION_OFFICER"),
      Map.entry(DisputeStatus.CLOSED, "RESOLUTION_OFFICER")
  );

  private final TransactionRepository transactions;
  private final DisputeRepository disputes;
  private final EvidenceRepository evidenceRepo;
  private final ActivityLogRepository activityRepo;
  private final StorageService storage;
  private final SlaRuleRepository slaRules;
  private final FinancialPostingRepository postings;
  private final NotificationRepository notifications;
  private final FraudFlagRepository fraudFlags;

  @Transactional
  public Dispute create(Long userId, DisputeDtos.CreateDisputeRequest req, String ipAddress) {
    Transaction tx = resolveTransaction(req);
    Instant now = Instant.now();
    SlaRule rule = slaRules.findByNetworkIgnoreCaseAndDisputeType(tx.getNetwork(), req.disputeType()).orElse(null);
    int internalDays = rule != null ? rule.getInternalDays() : FALLBACK_INTERNAL_DAYS;
    int networkDays = rule != null ? rule.getNetworkDays() : FALLBACK_NETWORK_DAYS;
    Instant deadlineBase = tx.getTxDate() != null && tx.getTxDate().isBefore(now) ? tx.getTxDate() : now;
    Instant due = deadlineBase.plus(internalDays, ChronoUnit.DAYS);
    Instant networkDue = deadlineBase.plus(networkDays, ChronoUnit.DAYS);

    EligibilityResult eligibility = evaluateEligibility(tx, req.disputeType(), req.channel());
    SchemeReason schemeReason = resolveSchemeReason(tx.getNetwork(), req.disputeType());
    boolean eligible = eligibility.eligible();

    Dispute d = Dispute.builder()
        .caseCode(generateCaseCode())
        .transaction(tx)
        .disputeType(req.disputeType())
        .schemeCategory(schemeReason.category())
        .schemeReasonCode(schemeReason.code())
        .reason(req.reason())
        .eligibilityStatus(eligible ? EligibilityStatus.ELIGIBLE : EligibilityStatus.INELIGIBLE)
        .eligibilityNotes(eligibility.notes())
        .status(eligible ? DisputeStatus.OPEN : DisputeStatus.INELIGIBLE)
        .openedByUserId(userId)
        .openedAt(now)
        .dueAt(due)
        .networkDueAt(networkDue)
        .closedAt(eligible ? null : now)
        .build();

    disputes.save(d);
    if (eligible) {
      logActivity(d, userId, "DISPUTE_OPENED", "Dispute opened and awaiting review.", ipAddress);
      notifyUser(userId, d, "Dispute received", "Your dispute has been received and is pending review.");
    } else {
      logActivity(d, userId, "DISPUTE_INELIGIBLE", eligibility.notes(), ipAddress);
      notifyUser(userId, d, "Dispute ineligible", "Your dispute cannot proceed: " + eligibility.notes());
    }
    evaluateFraudFlags(d);
    return d;
  }

  @Transactional(readOnly = true)
  public Page<Dispute> list(String role, Long userId, Pageable pageable) {
    String normalized = normalizeRole(role);
    if ("CUSTOMER".equals(normalized)) {
      return disputes.findByOpenedByUserId(userId, pageable);
    }
    if ("ADMIN".equals(normalized)) {
      return disputes.findAll(pageable);
    }
    if ("COMPLIANCE".equals(normalized)) {
      return disputes.findAll(pageable);
    }
    Set<DisputeStatus> statuses = statusesForRole(normalized);
    if (statuses.isEmpty()) {
      return Page.empty(pageable);
    }
    return disputes.findByAssignedToUserIdOrStatusInAndAssignedToUserIdIsNull(userId, statuses, pageable);
  }

  @Transactional(readOnly = true)
  public Dispute get(Long id, String role, Long userId) {
    Dispute d = findById(id);
    String normalized = normalizeRole(role);
    if ("CUSTOMER".equals(normalized) && !d.getOpenedByUserId().equals(userId)) {
      throw new SecurityException("Forbidden");
    }
    if (!"ADMIN".equals(normalized) && !"COMPLIANCE".equals(normalized) && STAFF_ROLES.contains(normalized)) {
      if (d.getAssignedToUserId() != null) {
        if (!d.getAssignedToUserId().equals(userId)) {
          throw new SecurityException("Forbidden");
        }
      } else {
        Set<DisputeStatus> statuses = statusesForRole(normalized);
        if (!statuses.contains(d.getStatus())) {
          throw new SecurityException("Forbidden");
        }
      }
    }
    return d;
  }

  @Transactional
  public Dispute assign(Long id, Long actorId, String role, Long assignedToUserId, String ipAddress) {
    requireStaff(role);
    Dispute d = findById(id);
    if (d.getEligibilityStatus() == EligibilityStatus.INELIGIBLE) {
      throw new IllegalStateException("Ineligible disputes cannot proceed.");
    }
    requireRoleForStatus(d.getStatus(), role);
    if (assignedToUserId == null) {
      throw new IllegalArgumentException("assignedToUserId is required");
    }
    d.setAssignedToUserId(assignedToUserId);

    disputes.save(d);
    logActivity(d, actorId, "ASSIGNED", "Assigned to userId=" + assignedToUserId, ipAddress);
    notifyUser(assignedToUserId, d, "New dispute assigned", "A dispute has been assigned to you: " + d.getCaseCode());
    return d;
  }

  @Transactional
  public Dispute updateStatus(Long id, Long actorId, String role, DisputeDtos.UpdateStatusRequest req, String ipAddress) {
    requireStaff(role);
    Dispute d = findById(id);
    requireRoleForStatus(d.getStatus(), role);
    enforceAssignment(d, actorId, role);
    validateStatusTransition(d.getStatus(), req.status());
    if (req.status() == DisputeStatus.SUBMITTED && evidenceRepo.countByDisputeId(d.getId()) == 0) {
      throw new IllegalStateException("Evidence is required before submission.");
    }

    DisputeStatus previousStatus = d.getStatus();
    d.setStatus(req.status());
    if (req.status() == DisputeStatus.SUBMITTED && d.getSubmittedAt() == null) {
      d.setSubmittedAt(Instant.now());
      d.setSubmissionRef("NET-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT));
      createPosting(d, PostingType.TEMP_CREDIT, PostingDirection.CREDIT,
          d.getTransaction().getAmount(), d.getTransaction().getCurrency(),
          "Temporary credit pending network outcome.");
      notifyUser(d.getOpenedByUserId(), d, "Dispute submitted",
          "Your dispute has been submitted to the card network.");
    }
    if (req.status() == DisputeStatus.WON) {
      createPosting(d, PostingType.FINAL_CREDIT, PostingDirection.CREDIT,
          d.getTransaction().getAmount(), d.getTransaction().getCurrency(),
          "Final credit after dispute won.");
      notifyUser(d.getOpenedByUserId(), d, "Dispute won",
          "Your dispute has been resolved in your favor.");
    }
    if (req.status() == DisputeStatus.LOST) {
      createPosting(d, PostingType.REVERSAL, PostingDirection.DEBIT,
          d.getTransaction().getAmount(), d.getTransaction().getCurrency(),
          "Reversal after dispute lost.");
      notifyUser(d.getOpenedByUserId(), d, "Dispute lost",
          "Your dispute was not resolved in your favor.");
    }
    String currentRole = ROLE_BY_STATUS.get(previousStatus);
    String nextRole = ROLE_BY_STATUS.get(req.status());
    if (nextRole != null && !nextRole.equals(currentRole)) {
      d.setAssignedToUserId(null);
    }
    if (Set.of(DisputeStatus.WON, DisputeStatus.LOST, DisputeStatus.CLOSED, DisputeStatus.CANCELLED).contains(req.status())) {
      d.setClosedAt(Instant.now());
    }

    disputes.save(d);
    logActivity(d, actorId, "STATUS_CHANGED", req.status().name() + " • " + Objects.toString(req.detail(), ""), ipAddress);
    return d;
  }

  @Transactional
  public Evidence uploadEvidence(Long disputeId, Long actorId, String role, MultipartFile file, String ipAddress) throws IOException {
    Dispute d = findById(disputeId);
    String normalized = normalizeRole(role);
    enforceAssignment(d, actorId, role);
    if (!canUploadEvidenceAtStatus(d.getStatus())) {
      throw new IllegalStateException("Evidence can only be uploaded during the evidence collection stages.");
    }
    if ("CUSTOMER".equals(normalized) && !d.getOpenedByUserId().equals(actorId)) {
      throw new SecurityException("Forbidden");
    }

    String storedPath = storage.save(disputeId, file);

    Evidence e = Evidence.builder()
        .dispute(d)
        .fileName(file.getOriginalFilename() == null ? "evidence" : file.getOriginalFilename())
        .contentType(file.getContentType() == null ? "application/octet-stream" : file.getContentType())
        .storagePath(storedPath)
        .uploadedByUserId(actorId)
        .uploadedAt(Instant.now())
        .build();

    evidenceRepo.save(e);
    logActivity(d, actorId, "EVIDENCE_UPLOADED", e.getFileName(), ipAddress);
    return e;
  }

  @Transactional(readOnly = true)
  public DisputeDtos.DashboardMetrics metrics(Instant from, Instant to) {
    List<Dispute> all = disputes.findByOpenedAtBetween(from, to);
    Instant now = Instant.now();
    long total = all.size();
    long open = all.stream().filter(d -> d.getClosedAt() == null).count();
    long overdue = all.stream().filter(d -> d.getClosedAt() == null && d.getDueAt().isBefore(now)).count();
    long breaches = all.stream().filter(Dispute::isBreachFlag).count();
    long won = all.stream().filter(d -> resolutionOutcome(d) == DisputeStatus.WON).count();
    long lost = all.stream().filter(d -> resolutionOutcome(d) == DisputeStatus.LOST).count();
    long resolved = won + lost;
    long overdueLoss = all.stream()
        .filter(d -> resolutionOutcome(d) == DisputeStatus.LOST)
        .filter(d -> (d.getClosedAt() == null && d.getDueAt().isBefore(now))
            || (d.getClosedAt() != null && d.getClosedAt().isAfter(d.getDueAt())))
        .count();

    double avgHours = all.stream()
        .filter(d -> d.getClosedAt() != null)
        .mapToLong(d -> Duration.between(d.getOpenedAt(), d.getClosedAt()).toHours())
        .average()
        .orElse(0);

    double winRate = resolved > 0 ? (double) won / resolved : 0;
    double lossRate = resolved > 0 ? (double) lost / resolved : 0;
    double overdueLossRate = lost > 0 ? (double) overdueLoss / lost : 0;

    double avgOverdueHours = all.stream()
        .filter(d -> d.getClosedAt() == null && d.getDueAt().isBefore(now))
        .mapToLong(d -> Duration.between(d.getDueAt(), now).toHours())
        .average()
        .orElse(0);

    double avgOverdueClosureHours = all.stream()
        .filter(d -> d.getClosedAt() != null && d.getClosedAt().isAfter(d.getDueAt()))
        .mapToLong(d -> Duration.between(d.getDueAt(), d.getClosedAt()).toHours())
        .average()
        .orElse(0);

    var top = all.stream()
        .collect(Collectors.groupingBy(Dispute::getDisputeType, Collectors.counting()))
        .entrySet().stream()
        .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
        .limit(5)
        .map(e -> new DisputeDtos.TopItem(e.getKey().name(), e.getValue()))
        .toList();

    long fraudCount = fraudFlags.findByCreatedAtBetween(from, to).size();

    return new DisputeDtos.DashboardMetrics(
        total,
        open,
        overdue,
        breaches,
        avgHours,
        winRate,
        lossRate,
        overdueLossRate,
        avgOverdueHours,
        avgOverdueClosureHours,
        fraudCount,
        top
    );
  }

  @Transactional(readOnly = true)
  public zw.co.tncybertech.dispute.api.dto.MetricsDtos.RbzReport rbzReport(Instant from, Instant to) {
    List<Dispute> all = disputes.findByOpenedAtBetween(from, to);
    long total = all.size();
    long open = all.stream().filter(d -> d.getClosedAt() == null).count();
    long closed = total - open;
    long breaches = all.stream().filter(Dispute::isBreachFlag).count();
    long won = all.stream().filter(d -> resolutionOutcome(d) == DisputeStatus.WON).count();
    long lost = all.stream().filter(d -> resolutionOutcome(d) == DisputeStatus.LOST).count();

    double avgHours = all.stream()
        .filter(d -> d.getClosedAt() != null)
        .mapToLong(d -> Duration.between(d.getOpenedAt(), d.getClosedAt()).toHours())
        .average()
        .orElse(0);

    var byType = all.stream()
        .collect(Collectors.groupingBy(d -> d.getDisputeType().name(), Collectors.counting()))
        .entrySet().stream()
        .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
        .map(e -> new zw.co.tncybertech.dispute.api.dto.MetricsDtos.MetricItem(e.getKey(), e.getValue()))
        .toList();

    var byChannel = all.stream()
        .collect(Collectors.groupingBy(d -> d.getTransaction().getChannel(), Collectors.counting()))
        .entrySet().stream()
        .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
        .map(e -> new zw.co.tncybertech.dispute.api.dto.MetricsDtos.MetricItem(e.getKey(), e.getValue()))
        .toList();

    var byMerchant = all.stream()
        .collect(Collectors.groupingBy(d -> d.getTransaction().getMerchantName(), Collectors.counting()))
        .entrySet().stream()
        .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
        .limit(10)
        .map(e -> new zw.co.tncybertech.dispute.api.dto.MetricsDtos.MetricItem(e.getKey(), e.getValue()))
        .toList();

    var fraudByType = fraudFlags.findByCreatedAtBetween(from, to).stream()
        .collect(Collectors.groupingBy(f -> f.getFlagType().name(), Collectors.counting()))
        .entrySet().stream()
        .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
        .map(e -> new zw.co.tncybertech.dispute.api.dto.MetricsDtos.MetricItem(e.getKey(), e.getValue()))
        .toList();

    return new zw.co.tncybertech.dispute.api.dto.MetricsDtos.RbzReport(
        from,
        to,
        total,
        open,
        closed,
        breaches,
        avgHours,
        won,
        lost,
        byType,
        byChannel,
        byMerchant,
        fraudByType
    );
  }

  @Transactional(readOnly = true)
  public List<Dispute> tasks(Long userId, String role) {
    String normalized = normalizeRole(role);
    if (!STAFF_ROLES.contains(normalized)) {
      return List.of();
    }
    return disputes.findByAssignedToUserIdOrderByOpenedAtDesc(userId);
  }

  @Transactional(readOnly = true)
  public List<Dispute> queue(String role) {
    String normalized = normalizeRole(role);
    if ("ADMIN".equals(normalized)) {
      return disputes.findByAssignedToUserIdIsNullOrderByOpenedAtDesc();
    }
    Set<DisputeStatus> statuses = statusesForRole(normalized);
    if (statuses.isEmpty()) {
      return List.of();
    }
    return disputes.findByStatusInAndAssignedToUserIdIsNullOrderByOpenedAtDesc(statuses);
  }

  @Transactional
  public Dispute claim(Long id, Long actorId, String role, String ipAddress) {
    Dispute d = findById(id);
    requireStaff(role);
    requireRoleForStatus(d.getStatus(), role);
    if (d.getAssignedToUserId() != null && !d.getAssignedToUserId().equals(actorId)) {
      throw new SecurityException("Forbidden");
    }
    d.setAssignedToUserId(actorId);
    disputes.save(d);
    logActivity(d, actorId, "CLAIMED", "Claimed by userId=" + actorId, ipAddress);
    notifyUser(actorId, d, "Dispute claimed", "You have claimed dispute " + d.getCaseCode());
    return d;
  }

  @Transactional
  public Dispute reopen(Long id, Long actorId, String role, String ipAddress) {
    String normalized = normalizeRole(role);
    if (!"ADMIN".equals(normalized)) {
      throw new SecurityException("Forbidden");
    }

    Dispute d = findById(id);
    if (d.getStatus() != DisputeStatus.CLOSED) {
      throw new IllegalStateException("Only closed cases can be reopened.");
    }

    DisputeStatus previousStatus = previousStatusBeforeClose(d);
    d.setStatus(previousStatus);
    d.setClosedAt(null);
    disputes.save(d);
    logActivity(d, actorId, "REOPENED", "Case reopened to " + previousStatus.name(), ipAddress);
    notifyUser(d.getOpenedByUserId(), d, "Case reopened", "Your dispute case has been reopened for further handling.");
    return d;
  }

  private void requireStaff(String role) {
    String normalized = normalizeRole(role);
    if (!STAFF_ROLES.contains(normalized)) {
      throw new SecurityException("Forbidden");
    }
  }

  private void requireRoleForStatus(DisputeStatus status, String role) {
    String normalized = normalizeRole(role);
    if ("ADMIN".equals(normalized)) {
      return;
    }
    String required = ROLE_BY_STATUS.get(status);
    if (required == null || !required.equals(normalized)) {
      throw new SecurityException("Forbidden");
    }
  }

  private void enforceAssignment(Dispute d, Long actorId, String role) {
    String normalized = normalizeRole(role);
    if ("ADMIN".equals(normalized)) {
      return;
    }
    if (!STAFF_ROLES.contains(normalized)) {
      return;
    }
    if (d.getAssignedToUserId() == null) {
      throw new SecurityException("Forbidden");
    }
    if (!d.getAssignedToUserId().equals(actorId)) {
      throw new SecurityException("Forbidden");
    }
  }

  private String normalizeRole(String role) {
    if (role == null) {
      return "";
    }
    String normalized = role.trim().toUpperCase(Locale.ROOT);
    if (normalized.startsWith("ROLE_")) {
      normalized = normalized.substring(5);
    }
    return normalized;
  }

  private Set<DisputeStatus> statusesForRole(String role) {
    return ROLE_BY_STATUS.entrySet().stream()
        .filter(e -> e.getValue().equals(role))
        .map(Map.Entry::getKey)
        .collect(Collectors.toSet());
  }

  private boolean canUploadEvidenceAtStatus(DisputeStatus status) {
    return Set.of(DisputeStatus.NEEDS_INFO, DisputeStatus.READY_FOR_SUBMISSION).contains(status);
  }

  private void logActivity(Dispute d, Long actorId, String action, String detail, String ipAddress) {
    activityRepo.save(ActivityLog.builder()
        .dispute(d)
        .actorUserId(actorId)
        .action(action)
        .detail(detail)
        .ipAddress(ipAddress)
        .atTime(Instant.now())
        .build());
  }

  private DisputeStatus previousStatusBeforeClose(Dispute dispute) {
    List<ActivityLog> activities = activityRepo.findByDisputeIdOrderByAtTimeAsc(dispute.getId());
    for (int i = activities.size() - 1; i >= 0; i--) {
      ActivityLog activity = activities.get(i);
      if (!"STATUS_CHANGED".equalsIgnoreCase(activity.getAction()) || activity.getDetail() == null) {
        continue;
      }
      String detail = activity.getDetail().trim().toUpperCase(Locale.ROOT);
      if (detail.startsWith("CLOSED")) {
        continue;
      }
      for (DisputeStatus status : DisputeStatus.values()) {
        if (detail.startsWith(status.name())) {
          return status;
        }
      }
    }
    throw new IllegalStateException("Unable to determine the previous status for this closed case.");
  }

  private Dispute findById(Long id) {
    return disputes.findById(id).orElseThrow(() -> new IllegalArgumentException("Dispute not found"));
  }

  private void validateStatusTransition(DisputeStatus current, DisputeStatus next) {
    if (current == next) return;
    if (current == DisputeStatus.INELIGIBLE) {
      throw new IllegalStateException("Cannot change an ineligible dispute.");
    }
    if (next == DisputeStatus.CANCELLED) {
      if (current == DisputeStatus.CANCELLED) {
        throw new IllegalStateException("Dispute is already cancelled.");
      }
      return;
    }
    if (current == DisputeStatus.CANCELLED) {
      throw new IllegalStateException("Cannot change a cancelled dispute.");
    }

    Set<DisputeStatus> allowed = TRANSITIONS.getOrDefault(current, Set.of());
    if (!allowed.contains(next)) {
      throw new IllegalStateException("Invalid state transition from " + current + " to " + next);
    }
  }

  private Transaction resolveTransaction(DisputeDtos.CreateDisputeRequest req) {
    if (req.transactionId() != null) {
      return transactions.findById(req.transactionId())
          .orElseThrow(() -> new IllegalArgumentException("Transaction not found"));
    }

    Objects.requireNonNull(req.cardLast4(), "cardLast4 is required when creating a new transaction");
    Objects.requireNonNull(req.merchantName(), "merchantName is required when creating a new transaction");
    Objects.requireNonNull(req.merchantId(), "merchantId is required when creating a new transaction");
    Objects.requireNonNull(req.amount(), "amount is required when creating a new transaction");
    Objects.requireNonNull(req.currency(), "currency is required when creating a new transaction");
    Objects.requireNonNull(req.txDate(), "txDate is required when creating a new transaction");
    Objects.requireNonNull(req.network(), "network is required when creating a new transaction");
    Objects.requireNonNull(req.transactionRef(), "transactionRef is required when creating a new transaction");
    Objects.requireNonNull(req.rrn(), "rrn is required when creating a new transaction");
    Objects.requireNonNull(req.authCode(), "authCode is required when creating a new transaction");
    Objects.requireNonNull(req.channel(), "channel is required when creating a new transaction");
    Objects.requireNonNull(req.terminalId(), "terminalId is required when creating a new transaction");
    Objects.requireNonNull(req.terminalLocation(), "terminalLocation is required when creating a new transaction");
    Objects.requireNonNull(req.customerIdentifier(), "customerIdentifier is required when creating a new transaction");

    TransactionStatus status = Boolean.TRUE.equals(req.merchantRefunded()) ? TransactionStatus.REFUNDED : TransactionStatus.DISPUTED;

    Transaction tx = Transaction.builder()
        .cardLast4(req.cardLast4())
        .merchantName(req.merchantName())
        .merchantId(req.merchantId())
        .amount(req.amount())
        .currency(req.currency())
        .txDate(req.txDate())
        .status(status)
        .network(req.network())
        .transactionRef(req.transactionRef())
        .rrn(req.rrn())
        .authCode(req.authCode())
        .channel(req.channel())
        .terminalId(req.terminalId())
        .terminalLocation(req.terminalLocation())
        .customerIdentifier(maskIdentifier(req.customerIdentifier()))
        .customerIdentifierHash(hashIdentifier(req.customerIdentifier()))
        .merchantRefunded(Boolean.TRUE.equals(req.merchantRefunded()))
        .refundRef(req.refundRef())
        .refundAt(req.refundAt())
        .build();

    return transactions.save(tx);
  }

  private String maskIdentifier(String value) {
    if (value == null || value.isBlank()) {
      return "****";
    }
    String cleaned = value.trim();
    if (cleaned.length() <= 4) {
      return "*".repeat(cleaned.length());
    }
    int maskLength = Math.max(0, cleaned.length() - 4);
    return cleaned.substring(0, 2) + "*".repeat(maskLength) + cleaned.substring(cleaned.length() - 2);
  }

  private String hashIdentifier(String value) {
    try {
      MessageDigest md = MessageDigest.getInstance("SHA-256");
      byte[] digest = md.digest(value.trim().getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(digest);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("Unable to hash identifier", e);
    }
  }

  private String generateCaseCode() {
    return "CB-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
  }

  private EligibilityResult evaluateEligibility(Transaction tx, DisputeType type, String channel) {
    List<String> issues = new ArrayList<>();
    int limitDays = SCHEME_TIME_LIMIT_DAYS.getOrDefault(type, 120);
    long ageDays = Duration.between(tx.getTxDate(), Instant.now()).toDays();
    if (ageDays > limitDays) {
      issues.add("Outside scheme time limits (" + limitDays + " days).");
    }
    String normalizedChannel = normalizeChannel(channel);
    Set<String> allowed = ALLOWED_CHANNELS.getOrDefault(type, Set.of());
    if (!allowed.isEmpty() && !allowed.contains(normalizedChannel)) {
      issues.add("Dispute type " + type + " is not valid for channel " + normalizedChannel + ".");
    }
    if (tx.isMerchantRefunded() || tx.getStatus() == TransactionStatus.REFUNDED || tx.getStatus() == TransactionStatus.REVERSED) {
      issues.add("Merchant already refunded the transaction.");
    }
    if (tx.getId() != null && disputes.existsByTransactionId(tx.getId())) {
      issues.add("Duplicate dispute already exists for this transaction.");
    }
    if (issues.isEmpty()) {
      return new EligibilityResult(true, "Eligible");
    }
    return new EligibilityResult(false, String.join(" ", issues));
  }

  private SchemeReason resolveSchemeReason(String network, DisputeType type) {
    String normalized = network == null ? "" : network.trim().toUpperCase(Locale.ROOT);
    Map<DisputeType, SchemeReason> byType = SCHEME_REASON_MAP.get(normalized);
    if (byType != null && byType.containsKey(type)) {
      return byType.get(type);
    }
    return new SchemeReason("Other", "GENERIC");
  }

  private String normalizeChannel(String channel) {
    if (channel == null) {
      return "";
    }
    return channel.trim().toUpperCase(Locale.ROOT);
  }

  private void createPosting(Dispute d,
                             PostingType type,
                             PostingDirection direction,
                             java.math.BigDecimal amount,
                             String currency,
                             String notes) {
    postings.save(FinancialPosting.builder()
        .dispute(d)
        .postingType(type)
        .direction(direction)
        .amount(amount)
        .currency(currency)
        .status(PostingStatus.POSTED)
        .notes(notes)
        .postedAt(Instant.now())
        .build());
  }

  private void notifyUser(Long userId, Dispute dispute, String title, String message) {
    if (userId == null) {
      return;
    }
    notifications.save(Notification.builder()
        .recipientUserId(userId)
        .dispute(dispute)
        .channel(NotificationChannel.IN_APP)
        .status(NotificationStatus.SENT)
        .title(title)
        .message(message)
        .sentAt(Instant.now())
        .build());
  }

  private void evaluateFraudFlags(Dispute dispute) {
    Transaction tx = dispute.getTransaction();
    Instant now = Instant.now();
    Instant lastYear = now.minus(365, ChronoUnit.DAYS);
    Instant lastMonth = now.minus(30, ChronoUnit.DAYS);

    long customerCount = disputes.countByCustomerHashSince(tx.getCustomerIdentifierHash(), lastYear);
    if (customerCount >= 3) {
      fraudFlags.save(FraudFlag.builder()
          .dispute(dispute)
          .flagType(FraudFlagType.REPEAT_DISPUTER)
          .severity(FraudSeverity.MEDIUM)
          .detail("Customer has " + customerCount + " disputes in the last 12 months.")
          .build());
    }

    long merchantCount = disputes.countByMerchantSince(tx.getMerchantName(), lastMonth);
    if (merchantCount >= 5) {
      fraudFlags.save(FraudFlag.builder()
          .dispute(dispute)
          .flagType(FraudFlagType.MERCHANT_CLUSTER)
          .severity(FraudSeverity.MEDIUM)
          .detail("Merchant has " + merchantCount + " disputes in the last 30 days.")
          .build());
    }

    long merchantAmountCount = disputes.countByMerchantAmountSince(tx.getMerchantName(), tx.getAmount(), lastMonth);
    if (merchantAmountCount >= 3) {
      fraudFlags.save(FraudFlag.builder()
          .dispute(dispute)
          .flagType(FraudFlagType.AMOUNT_PATTERN)
          .severity(FraudSeverity.MEDIUM)
          .detail("Repeated disputes with same merchant and amount in last 30 days.")
          .build());
    }

    if (tx.isMerchantRefunded() || tx.getStatus() == TransactionStatus.REFUNDED || tx.getStatus() == TransactionStatus.REVERSED) {
      fraudFlags.save(FraudFlag.builder()
          .dispute(dispute)
          .flagType(FraudFlagType.DOUBLE_DIPPING)
          .severity(FraudSeverity.HIGH)
          .detail("Dispute filed after merchant refund/reversal.")
          .build());
    }

    if (tx.getAmount() != null && tx.getAmount().compareTo(java.math.BigDecimal.valueOf(1000)) >= 0) {
      fraudFlags.save(FraudFlag.builder()
          .dispute(dispute)
          .flagType(FraudFlagType.HIGH_RISK)
          .severity(FraudSeverity.HIGH)
          .detail("High value dispute amount: " + tx.getAmount())
          .build());
    }
  }

  private DisputeStatus resolutionOutcome(Dispute dispute) {
    if (dispute.getStatus() == DisputeStatus.WON || dispute.getStatus() == DisputeStatus.LOST) {
      return dispute.getStatus();
    }
    if (dispute.getStatus() != DisputeStatus.CLOSED) {
      return null;
    }

    List<ActivityLog> activities = activityRepo.findByDisputeIdOrderByAtTimeAsc(dispute.getId());
    for (int i = activities.size() - 1; i >= 0; i--) {
      ActivityLog activity = activities.get(i);
      if (!"STATUS_CHANGED".equalsIgnoreCase(activity.getAction()) || activity.getDetail() == null) {
        continue;
      }
      String detail = activity.getDetail().trim().toUpperCase(Locale.ROOT);
      if (detail.startsWith(DisputeStatus.WON.name())) {
        return DisputeStatus.WON;
      }
      if (detail.startsWith(DisputeStatus.LOST.name())) {
        return DisputeStatus.LOST;
      }
    }
    return null;
  }

  private record EligibilityResult(boolean eligible, String notes) {}

  private record SchemeReason(String category, String code) {}
}
