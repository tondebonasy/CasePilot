package zw.co.tncybertech.dispute.util;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;

public record DateRange(Instant from, Instant to) {

  public static DateRange resolve(String fromParam, String toParam) {
    Instant to = parseParam(toParam, true);
    if (to == null) {
      to = Instant.now();
    }

    Instant from = parseParam(fromParam, false);
    if (from == null) {
      LocalDate toDate = LocalDate.ofInstant(to, ZoneOffset.UTC);
      from = toDate.minusYears(2).atStartOfDay(ZoneOffset.UTC).toInstant();
    }

    if (from.isAfter(to)) {
      LocalDate toDate = LocalDate.ofInstant(to, ZoneOffset.UTC);
      from = toDate.minusYears(2).atStartOfDay(ZoneOffset.UTC).toInstant();
    }

    return new DateRange(from, to);
  }

  private static Instant parseParam(String value, boolean endOfDay) {
    if (value == null || value.isBlank()) {
      return null;
    }
    try {
      return Instant.parse(value.trim());
    } catch (DateTimeParseException ignored) {
      try {
        LocalDate d = LocalDate.parse(value.trim());
        if (endOfDay) {
          return d.plusDays(1).atStartOfDay(ZoneOffset.UTC).minusNanos(1).toInstant();
        }
        return d.atStartOfDay(ZoneOffset.UTC).toInstant();
      } catch (DateTimeParseException ex) {
        throw new IllegalArgumentException("Invalid date value: " + value);
      }
    }
  }
}
