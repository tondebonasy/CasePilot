package zw.co.tncybertech.dispute.service;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import zw.co.tncybertech.dispute.domain.ActivityLog;
import zw.co.tncybertech.dispute.domain.Dispute;
import zw.co.tncybertech.dispute.domain.Notification;
import zw.co.tncybertech.dispute.domain.NotificationChannel;
import zw.co.tncybertech.dispute.domain.NotificationStatus;
import zw.co.tncybertech.dispute.repo.ActivityLogRepository;
import zw.co.tncybertech.dispute.repo.DisputeRepository;
import zw.co.tncybertech.dispute.repo.NotificationRepository;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DeadlineMonitorService {

  private static final long ALERT_WINDOW_HOURS = 72;
  private static final Long SYSTEM_ACTOR = 0L;

  private final DisputeRepository disputes;
  private final ActivityLogRepository activityRepo;
  private final NotificationRepository notifications;

  @Transactional
  @Scheduled(fixedDelayString = "${app.deadlines.scan-ms:3600000}")
  public void scanDeadlines() {
    Instant now = Instant.now();
    Instant alertBefore = now.plus(ALERT_WINDOW_HOURS, ChronoUnit.HOURS);

    List<Dispute> toSave = new ArrayList<>();

    for (Dispute d : disputes.findByClosedAtIsNullAndDueAtBetween(now, alertBefore)) {
      if (d.getInternalAlertedAt() == null) {
        d.setInternalAlertedAt(now);
        log(d, "DEADLINE_ALERT", "Internal deadline within 72 hours.");
        notifyParties(d, "Internal deadline approaching",
            "Dispute " + d.getCaseCode() + " is due within 72 hours.");
        toSave.add(d);
      }
    }

    for (Dispute d : disputes.findByClosedAtIsNullAndDueAtBefore(now)) {
      if (d.getInternalBreachAt() == null) {
        d.setInternalBreachAt(now);
        d.setBreachFlag(true);
        log(d, "DEADLINE_BREACH", "Internal deadline missed.");
        notifyParties(d, "Internal deadline breached",
            "Dispute " + d.getCaseCode() + " has missed the internal deadline.");
        toSave.add(d);
      }
    }

    for (Dispute d : disputes.findByClosedAtIsNullAndNetworkDueAtBetween(now, alertBefore)) {
      if (d.getNetworkAlertedAt() == null) {
        d.setNetworkAlertedAt(now);
        log(d, "NETWORK_DEADLINE_ALERT", "Network deadline within 72 hours.");
        notifyParties(d, "Network deadline approaching",
            "Dispute " + d.getCaseCode() + " is due with the network within 72 hours.");
        toSave.add(d);
      }
    }

    for (Dispute d : disputes.findByClosedAtIsNullAndNetworkDueAtBefore(now)) {
      if (d.getNetworkBreachAt() == null) {
        d.setNetworkBreachAt(now);
        d.setBreachFlag(true);
        log(d, "NETWORK_DEADLINE_BREACH", "Network deadline missed.");
        notifyParties(d, "Network deadline breached",
            "Dispute " + d.getCaseCode() + " has missed the network deadline.");
        toSave.add(d);
      }
    }

    if (!toSave.isEmpty()) {
      disputes.saveAll(toSave);
    }
  }

  private void log(Dispute dispute, String action, String detail) {
    activityRepo.save(ActivityLog.builder()
        .dispute(dispute)
        .actorUserId(SYSTEM_ACTOR)
        .action(action)
        .detail(detail)
        .ipAddress("SYSTEM")
        .atTime(Instant.now())
        .build());
  }

  private void notifyParties(Dispute dispute, String title, String message) {
    List<Long> recipients = new ArrayList<>();
    if (dispute.getAssignedToUserId() != null) {
      recipients.add(dispute.getAssignedToUserId());
    }
    if (dispute.getOpenedByUserId() != null && !recipients.contains(dispute.getOpenedByUserId())) {
      recipients.add(dispute.getOpenedByUserId());
    }

    for (Long recipient : recipients) {
      notifications.save(Notification.builder()
          .recipientUserId(recipient)
          .dispute(dispute)
          .channel(NotificationChannel.IN_APP)
          .status(NotificationStatus.SENT)
          .title(title)
          .message(message)
          .sentAt(Instant.now())
          .build());
    }
  }
}
