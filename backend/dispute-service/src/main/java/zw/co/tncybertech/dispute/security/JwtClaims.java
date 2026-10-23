package zw.co.tncybertech.dispute.security;

public record JwtClaims(
    Long userId,
    String email,
    String name,
    String role
) {}
