package zw.co.tncybertech.dispute.api.dto;

import java.time.Instant;
import java.util.List;

public class MetricsDtos {

  public record MetricItem(String label, long count) {}

  public record RbzReport(
      Instant from,
      Instant to,
      long totalDisputes,
      long openDisputes,
      long closedDisputes,
      long slaBreaches,
      double avgResolutionHours,
      long winCount,
      long lossCount,
      List<MetricItem> byType,
      List<MetricItem> byChannel,
      List<MetricItem> byMerchant,
      List<MetricItem> fraudByType
  ) {}
}
