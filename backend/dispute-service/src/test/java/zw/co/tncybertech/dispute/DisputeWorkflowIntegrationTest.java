package zw.co.tncybertech.dispute;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.annotation.Transactional;
import zw.co.tncybertech.dispute.api.dto.DisputeDtos;
import zw.co.tncybertech.dispute.domain.Dispute;
import zw.co.tncybertech.dispute.domain.DisputeStatus;
import zw.co.tncybertech.dispute.domain.DisputeType;
import zw.co.tncybertech.dispute.domain.SlaRule;
import zw.co.tncybertech.dispute.repo.SlaRuleRepository;
import zw.co.tncybertech.dispute.service.DisputeService;

import java.math.BigDecimal;
import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional
class DisputeWorkflowIntegrationTest {

  private static final String TEST_IP = "127.0.0.1";

  @Autowired
  private DisputeService disputeService;

  @Autowired
  private SlaRuleRepository slaRuleRepository;

  @Test
  void createDisputeUsesConfiguredSlaDays() {
    SlaRule rule = SlaRule.builder()
        .network("INT_NET_A")
        .disputeType(DisputeType.OTHER)
        .internalDays(3)
        .networkDays(6)
        .build();
    slaRuleRepository.save(rule);

    Instant txDate = Instant.now().minus(4, ChronoUnit.DAYS);
    Dispute dispute = disputeService.create(101L, buildRequest("INT_NET_A", txDate), TEST_IP);

    assertEquals(3, ChronoUnit.DAYS.between(txDate, dispute.getDueAt()));
    assertEquals(6, ChronoUnit.DAYS.between(txDate, dispute.getNetworkDueAt()));
  }

  @Test
  void updateStatusEnforcesRulesAndTransitions() throws IOException {
    SlaRule rule = SlaRule.builder()
        .network("INT_NET_B")
        .disputeType(DisputeType.OTHER)
        .internalDays(2)
        .networkDays(5)
        .build();
    slaRuleRepository.save(rule);

    Dispute dispute = disputeService.create(202L, buildRequest("INT_NET_B", Instant.now()), TEST_IP);
    dispute = disputeService.claim(dispute.getId(), 202L, "INTAKE_OFFICER", TEST_IP);
    final Long disputeId = dispute.getId();

    assertThrows(IllegalStateException.class, () ->
        disputeService.updateStatus(disputeId, 202L, "INTAKE_OFFICER",
            new DisputeDtos.UpdateStatusRequest(DisputeStatus.SUBMITTED, "Attempt invalid jump"), TEST_IP));

    Dispute underReview = disputeService.updateStatus(disputeId, 202L, "INTAKE_OFFICER",
        new DisputeDtos.UpdateStatusRequest(DisputeStatus.UNDER_REVIEW, "Starting review"), TEST_IP);
    assertEquals(DisputeStatus.UNDER_REVIEW, underReview.getStatus());

    disputeService.claim(disputeId, 202L, "REVIEWER", TEST_IP);
    Dispute needsInfo = disputeService.updateStatus(disputeId, 202L, "REVIEWER",
        new DisputeDtos.UpdateStatusRequest(DisputeStatus.NEEDS_INFO, "Need more support"), TEST_IP);
    assertEquals(DisputeStatus.NEEDS_INFO, needsInfo.getStatus());

    disputeService.claim(disputeId, 202L, "EVIDENCE_OFFICER", TEST_IP);
    Dispute readyForSubmission = disputeService.updateStatus(disputeId, 202L, "EVIDENCE_OFFICER",
        new DisputeDtos.UpdateStatusRequest(DisputeStatus.READY_FOR_SUBMISSION, "Evidence pack complete"), TEST_IP);
    assertEquals(DisputeStatus.READY_FOR_SUBMISSION, readyForSubmission.getStatus());

    disputeService.claim(disputeId, 202L, "NETWORK_LIAISON", TEST_IP);
    assertThrows(IllegalStateException.class, () ->
        disputeService.updateStatus(disputeId, 202L, "NETWORK_LIAISON",
            new DisputeDtos.UpdateStatusRequest(DisputeStatus.SUBMITTED, "Attempt without evidence"), TEST_IP));

    disputeService.uploadEvidence(disputeId, 202L, "NETWORK_LIAISON",
        new MockMultipartFile("file", "evidence.txt", "text/plain", "proof".getBytes()), TEST_IP);

    Dispute submitted = disputeService.updateStatus(disputeId, 202L, "NETWORK_LIAISON",
        new DisputeDtos.UpdateStatusRequest(DisputeStatus.SUBMITTED, "Submitted to network"), TEST_IP);
    assertEquals(DisputeStatus.SUBMITTED, submitted.getStatus());

    disputeService.claim(disputeId, 202L, "RESOLUTION_OFFICER", TEST_IP);
    assertThrows(IllegalStateException.class, () ->
        disputeService.updateStatus(disputeId, 202L, "RESOLUTION_OFFICER",
            new DisputeDtos.UpdateStatusRequest(DisputeStatus.OPEN, "Invalid revert"), TEST_IP));
  }

  @Test
  void metricsUseResolvedOutcomesAndHistoricalDueDates() throws IOException {
    SlaRule rule = SlaRule.builder()
        .network("INT_NET_C")
        .disputeType(DisputeType.OTHER)
        .internalDays(3)
        .networkDays(6)
        .build();
    slaRuleRepository.save(rule);
    Instant from = Instant.now().minus(1, ChronoUnit.MINUTES);

    Dispute won = disputeService.create(301L, buildRequest("INT_NET_C", Instant.now().minus(2, ChronoUnit.DAYS)), TEST_IP);
    progressToOutcome(won.getId(), 301L, DisputeStatus.WON);

    Dispute lost = disputeService.create(302L, buildRequest("INT_NET_C", Instant.now().minus(2, ChronoUnit.DAYS)), TEST_IP);
    progressToOutcome(lost.getId(), 302L, DisputeStatus.LOST);

    disputeService.create(303L, buildRequest("INT_NET_C", Instant.now().minus(5, ChronoUnit.DAYS)), TEST_IP);
    Instant to = Instant.now().plus(1, ChronoUnit.MINUTES);

    DisputeDtos.DashboardMetrics metrics = disputeService.metrics(from, to);

    assertEquals(1, metrics.overdueDisputes());
    assertEquals(0.5, metrics.winRate());
    assertEquals(0.5, metrics.lossRate());
    assertEquals(1, disputeService.rbzReport(from, to).winCount());
    assertEquals(1, disputeService.rbzReport(from, to).lossCount());
  }

  private DisputeDtos.CreateDisputeRequest buildRequest(String network, Instant txDate) {
    return new DisputeDtos.CreateDisputeRequest(
        null,
        "4444",
        "Integration Merchant",
        "M-001",
        new BigDecimal("140.00"),
        "USD",
        txDate,
        network,
        "INT-REF",
        "RRN-001",
        "AUTH01",
        "POS",
        "TERM-001",
        "Harare",
        "customer-id",
        false,
        null,
        null,
        DisputeType.OTHER,
        "Integration test focus"
    );
  }

  private void progressToOutcome(Long disputeId, Long actorId, DisputeStatus outcome) throws IOException {
    disputeService.claim(disputeId, actorId, "INTAKE_OFFICER", TEST_IP);
    disputeService.updateStatus(disputeId, actorId, "INTAKE_OFFICER",
        new DisputeDtos.UpdateStatusRequest(DisputeStatus.UNDER_REVIEW, "Starting review"), TEST_IP);

    disputeService.claim(disputeId, actorId, "REVIEWER", TEST_IP);
    disputeService.updateStatus(disputeId, actorId, "REVIEWER",
        new DisputeDtos.UpdateStatusRequest(DisputeStatus.NEEDS_INFO, "Need support"), TEST_IP);

    disputeService.claim(disputeId, actorId, "EVIDENCE_OFFICER", TEST_IP);
    disputeService.updateStatus(disputeId, actorId, "EVIDENCE_OFFICER",
        new DisputeDtos.UpdateStatusRequest(DisputeStatus.READY_FOR_SUBMISSION, "Evidence complete"), TEST_IP);

    disputeService.claim(disputeId, actorId, "NETWORK_LIAISON", TEST_IP);
    disputeService.uploadEvidence(disputeId, actorId, "NETWORK_LIAISON",
        new MockMultipartFile("file", "evidence.txt", "text/plain", "proof".getBytes()), TEST_IP);
    disputeService.updateStatus(disputeId, actorId, "NETWORK_LIAISON",
        new DisputeDtos.UpdateStatusRequest(DisputeStatus.SUBMITTED, "Submitted to network"), TEST_IP);

    disputeService.claim(disputeId, actorId, "RESOLUTION_OFFICER", TEST_IP);
    disputeService.updateStatus(disputeId, actorId, "RESOLUTION_OFFICER",
        new DisputeDtos.UpdateStatusRequest(outcome, "Resolved"), TEST_IP);
    disputeService.updateStatus(disputeId, actorId, "RESOLUTION_OFFICER",
        new DisputeDtos.UpdateStatusRequest(DisputeStatus.CLOSED, "Closed after resolution"), TEST_IP);
  }
}
