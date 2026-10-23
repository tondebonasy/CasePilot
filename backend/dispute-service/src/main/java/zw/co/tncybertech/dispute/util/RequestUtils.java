package zw.co.tncybertech.dispute.util;

import jakarta.servlet.http.HttpServletRequest;

public final class RequestUtils {

  private RequestUtils() {}

  public static String resolveIp(HttpServletRequest request) {
    String forwarded = request.getHeader("X-Forwarded-For");
    if (forwarded != null && !forwarded.isBlank()) {
      int comma = forwarded.indexOf(',');
      return comma > 0 ? forwarded.substring(0, comma).trim() : forwarded.trim();
    }
    return request.getRemoteAddr();
  }
}
