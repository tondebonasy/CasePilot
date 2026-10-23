package zw.co.tncybertech.dispute.api.dto;

import java.time.Instant;

public class NotificationDtos {
  public record NotificationDto(
      Long id,
      Long disputeId,
      String title,
      String message,
      String channel,
      String status,
      Instant createdAt,
      Instant sentAt
  ) {}
}
