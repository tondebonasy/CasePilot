package zw.co.tncybertech.dispute.service;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import zw.co.tncybertech.dispute.api.dto.AdminDtos;
import zw.co.tncybertech.dispute.domain.DisputeType;
import zw.co.tncybertech.dispute.domain.AdminAuditLog;
import zw.co.tncybertech.dispute.domain.SlaRule;
import zw.co.tncybertech.dispute.domain.training.RbzPolicy;
import zw.co.tncybertech.dispute.domain.training.RbzQuiz;
import zw.co.tncybertech.dispute.domain.training.RbzQuizOption;
import zw.co.tncybertech.dispute.domain.training.RbzQuizQuestion;
import zw.co.tncybertech.dispute.repo.AdminAuditLogRepository;
import zw.co.tncybertech.dispute.repo.RbzPolicyRepository;
import zw.co.tncybertech.dispute.repo.RbzQuizOptionRepository;
import zw.co.tncybertech.dispute.repo.RbzQuizQuestionRepository;
import zw.co.tncybertech.dispute.repo.RbzQuizRepository;
import zw.co.tncybertech.dispute.repo.SlaRuleRepository;

import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AdminContentService {

  private final SlaRuleRepository slaRules;
  private final AdminAuditLogRepository auditRepo;
  private final RbzPolicyRepository policyRepo;
  private final RbzQuizRepository quizRepo;
  private final RbzQuizQuestionRepository questionRepo;
  private final RbzQuizOptionRepository optionRepo;

  @Transactional(readOnly = true)
  public List<AdminDtos.SlaRuleResponse> listSlaRules() {
    return slaRules.findAll(Sort.by(Sort.Direction.DESC, "createdAt")).stream()
        .map(this::toSlaRuleResponse)
        .toList();
  }

  @Transactional(readOnly = true)
  public List<AdminDtos.AuditLogResponse> listAuditLogs() {
    return auditRepo.findAll(Sort.by(Sort.Direction.DESC, "atTime")).stream()
        .map(a -> new AdminDtos.AuditLogResponse(
            a.getId(),
            a.getActorUserId(),
            a.getActorRole(),
            a.getAction(),
            a.getDetail(),
            a.getIpAddress(),
            a.getAtTime()
        ))
        .toList();
  }

  @Transactional
  public AdminDtos.SlaRuleResponse createSlaRule(@Valid AdminDtos.SlaRuleRequest req,
                                                 Long actorUserId,
                                                 String actorRole,
                                                 String ipAddress) {
    DisputeType disputeType = parseDisputeType(req.disputeType());
    String network = req.network().trim();
    ensureUniqueRule(null, network, disputeType);

    SlaRule rule = SlaRule.builder()
        .network(network)
        .disputeType(disputeType)
        .internalDays(req.internalDays())
        .networkDays(req.networkDays())
        .build();

    AdminDtos.SlaRuleResponse response = toSlaRuleResponse(slaRules.save(rule));
    logAudit(actorUserId, actorRole, ipAddress,
        "SLA_CREATED",
        "network=" + response.network() + ", disputeType=" + response.disputeType()
            + ", internalDays=" + response.internalDays() + ", networkDays=" + response.networkDays());
    return response;
  }

  @Transactional
  public AdminDtos.SlaRuleResponse updateSlaRule(Long id,
                                                 @Valid AdminDtos.SlaRuleRequest req,
                                                 Long actorUserId,
                                                 String actorRole,
                                                 String ipAddress) {
    SlaRule rule = slaRules.findById(id).orElseThrow(() -> new IllegalArgumentException("SLA rule not found"));
    DisputeType disputeType = parseDisputeType(req.disputeType());
    String network = req.network().trim();
    ensureUniqueRule(rule, network, disputeType);

    rule.setNetwork(network);
    rule.setDisputeType(disputeType);
    rule.setInternalDays(req.internalDays());
    rule.setNetworkDays(req.networkDays());

    AdminDtos.SlaRuleResponse response = toSlaRuleResponse(slaRules.save(rule));
    logAudit(actorUserId, actorRole, ipAddress,
        "SLA_UPDATED",
        "id=" + response.id() + ", network=" + response.network() + ", disputeType=" + response.disputeType()
            + ", internalDays=" + response.internalDays() + ", networkDays=" + response.networkDays());
    return response;
  }

  @Transactional(readOnly = true)
  public List<AdminDtos.PolicyResponse> listPolicies() {
    return policyRepo.findAll(Sort.by(Sort.Direction.DESC, "createdAt")).stream()
        .map(this::toPolicyResponse)
        .toList();
  }

  @Transactional
  public AdminDtos.PolicyResponse createPolicy(@Valid AdminDtos.PolicyRequest req) {
    RbzPolicy policy = RbzPolicy.builder()
        .title(req.title().trim())
        .circularReference(req.circularReference().trim())
        .summary(req.summary().trim())
        .effectiveDate(req.effectiveDate())
        .keyRequirements(req.keyRequirements().trim())
        .active(req.active())
        .build();

    return toPolicyResponse(policyRepo.save(policy));
  }

  @Transactional
  public AdminDtos.PolicyResponse updatePolicy(Long id, @Valid AdminDtos.PolicyRequest req) {
    RbzPolicy policy = policyRepo.findById(id)
        .orElseThrow(() -> new IllegalArgumentException("Policy not found"));

    policy.setTitle(req.title().trim());
    policy.setCircularReference(req.circularReference().trim());
    policy.setSummary(req.summary().trim());
    policy.setEffectiveDate(req.effectiveDate());
    policy.setKeyRequirements(req.keyRequirements().trim());
    policy.setActive(req.active());

    return toPolicyResponse(policyRepo.save(policy));
  }

  @Transactional(readOnly = true)
  public List<AdminDtos.QuizResponse> listQuizzes() {
    return quizRepo.findAll(Sort.by(Sort.Direction.DESC, "createdAt")).stream()
        .map(this::toQuizResponse)
        .toList();
  }

  @Transactional
  public AdminDtos.QuizResponse createQuiz(@Valid AdminDtos.QuizRequest req) {
    RbzPolicy policy = policyRepo.findById(req.policyId())
        .orElseThrow(() -> new IllegalArgumentException("Policy not found"));

    RbzQuiz quiz = RbzQuiz.builder()
        .policy(policy)
        .title(req.title().trim())
        .description(req.description().trim())
        .build();

    quiz = quizRepo.save(quiz);
    for (AdminDtos.QuestionRequest questionReq : req.questions()) {
      RbzQuizQuestion question = RbzQuizQuestion.builder()
          .quiz(quiz)
          .question(questionReq.question().trim())
          .explanation(questionReq.explanation() == null ? null : questionReq.explanation().trim())
          .build();
      question = questionRepo.save(question);
      if (questionReq.options().stream().noneMatch(AdminDtos.OptionRequest::correct)) {
        throw new IllegalArgumentException("Each question must have at least one correct option");
      }

      for (AdminDtos.OptionRequest optionReq : questionReq.options()) {
        optionRepo.save(RbzQuizOption.builder()
            .question(question)
            .text(optionReq.text().trim())
            .correct(optionReq.correct())
            .build());
      }
    }

    return toQuizResponse(quiz);
  }

  private void ensureUniqueRule(SlaRule current, String network, DisputeType disputeType) {
    slaRules.findByNetworkIgnoreCaseAndDisputeType(network, disputeType)
        .filter(existing -> current == null || !existing.getId().equals(current.getId()))
        .ifPresent(existing -> {
          throw new IllegalArgumentException("A rule already exists for " + network + " / " + disputeType);
        });
  }

  private DisputeType parseDisputeType(String value) {
    try {
      return DisputeType.valueOf(value.trim().toUpperCase(Locale.ROOT));
    } catch (Exception e) {
      throw new IllegalArgumentException("Unknown dispute type: " + value);
    }
  }

  private AdminDtos.SlaRuleResponse toSlaRuleResponse(SlaRule rule) {
    return new AdminDtos.SlaRuleResponse(
        rule.getId(),
        rule.getNetwork(),
        rule.getDisputeType().name(),
        rule.getInternalDays(),
        rule.getNetworkDays(),
        rule.getCreatedAt()
    );
  }

  private AdminDtos.PolicyResponse toPolicyResponse(RbzPolicy policy) {
    return new AdminDtos.PolicyResponse(
        policy.getId(),
        policy.getTitle(),
        policy.getCircularReference(),
        policy.getSummary(),
        policy.getEffectiveDate(),
        policy.getKeyRequirements(),
        policy.isActive(),
        policy.getCreatedAt()
    );
  }

  private AdminDtos.QuizResponse toQuizResponse(RbzQuiz quiz) {
    List<AdminDtos.QuestionResponse> questions = questionRepo.findByQuizId(quiz.getId()).stream()
        .map(q -> new AdminDtos.QuestionResponse(
            q.getId(),
            q.getQuestion(),
            q.getExplanation(),
            optionRepo.findByQuestionId(q.getId()).stream()
                .map(opt -> new AdminDtos.OptionResponse(opt.getId(), opt.getText(), opt.isCorrect()))
                .toList()
        )).toList();

    return new AdminDtos.QuizResponse(
        quiz.getId(),
        quiz.getTitle(),
        quiz.getDescription(),
        quiz.getPolicy().getId(),
        quiz.getCreatedAt(),
        questions
    );
  }

  private void logAudit(Long actorUserId, String actorRole, String ipAddress, String action, String detail) {
    if (actorUserId == null || actorRole == null) {
      return;
    }
    auditRepo.save(AdminAuditLog.builder()
        .actorUserId(actorUserId)
        .actorRole(actorRole)
        .action(action)
        .detail(detail)
        .ipAddress(ipAddress)
        .build());
  }
}
