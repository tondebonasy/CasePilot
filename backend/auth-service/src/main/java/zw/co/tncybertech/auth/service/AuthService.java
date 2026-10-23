package zw.co.tncybertech.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import zw.co.tncybertech.auth.api.dto.AuthDtos;
import zw.co.tncybertech.auth.domain.User;
import zw.co.tncybertech.auth.domain.UserRole;
import zw.co.tncybertech.auth.repo.UserRepository;
import zw.co.tncybertech.auth.security.JwtService;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AuthService {

  private final UserRepository users;
  private final PasswordEncoder passwordEncoder;
  private final JwtService jwt;

  @Transactional
  public String registerCustomer(AuthDtos.RegisterRequest req) {
    String email = normalizeEmail(req.email());
    if (users.findByEmailNormalized(email).isPresent()) {
      throw new IllegalArgumentException("Email already registered");
    }

    User u = User.builder()
        .email(email)
        .fullName(normalizeName(req.fullName()))
        .passwordHash(passwordEncoder.encode(req.password()))
        .role(UserRole.CUSTOMER)
        .enabled(true)
        .build();

    users.save(u);
    return jwt.createAccessToken(u);
  }

  @Transactional(readOnly = true)
  public String login(AuthDtos.LoginRequest req) {
    String email = normalizeEmail(req.email());
    User u = users.findByEmailNormalized(email)
        .orElseGet(() -> users.findAll().stream()
            .filter(candidate -> normalizeEmailLoose(candidate.getEmail()).equals(normalizeEmailLoose(email)))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Invalid credentials")));

    if (!u.isEnabled() || !passwordEncoder.matches(req.password(), u.getPasswordHash())) {
      throw new IllegalArgumentException("Invalid credentials");
    }
    return jwt.createAccessToken(u);
  }

  @Transactional
  public AuthDtos.UserResponse createUser(AuthDtos.AdminCreateUserRequest req) {
    String email = normalizeEmail(req.email());
    if (users.findByEmailNormalized(email).isPresent()) {
      throw new IllegalArgumentException("Email already registered");
    }

    UserRole role = parseRole(req.role());
    User u = User.builder()
        .email(email)
        .fullName(normalizeName(req.fullName()))
        .passwordHash(passwordEncoder.encode(req.password()))
        .role(role)
        .enabled(true)
        .build();

    u = users.save(u);
    return new AuthDtos.UserResponse(u.getId(), u.getEmail(), u.getFullName(), u.getRole().name(), u.isEnabled());
  }

  @Transactional
  public AuthDtos.UserResponse updateUserStatus(Long id, boolean enabled) {
    User u = users.findById(id).orElseThrow(() -> new IllegalArgumentException("User not found"));
    u.setEnabled(enabled);
    u = users.save(u);
    return new AuthDtos.UserResponse(u.getId(), u.getEmail(), u.getFullName(), u.getRole().name(), u.isEnabled());
  }

  @Transactional
  public AuthDtos.UserResponse updateUserRole(Long id, String roleValue) {
    User u = users.findById(id).orElseThrow(() -> new IllegalArgumentException("User not found"));
    UserRole role = parseRole(roleValue);
    u.setRole(role);
    u = users.save(u);
    return new AuthDtos.UserResponse(u.getId(), u.getEmail(), u.getFullName(), u.getRole().name(), u.isEnabled());
  }

  @Transactional
  public AuthDtos.UserResponse resetUserPassword(Long id, String password) {
    User u = users.findById(id).orElseThrow(() -> new IllegalArgumentException("User not found"));
    u.setPasswordHash(passwordEncoder.encode(password));
    u = users.save(u);
    return new AuthDtos.UserResponse(u.getId(), u.getEmail(), u.getFullName(), u.getRole().name(), u.isEnabled());
  }

  private UserRole parseRole(String value) {
    if (value == null) {
      throw new IllegalArgumentException("Role is required");
    }
    try {
      return UserRole.valueOf(value.trim().toUpperCase(Locale.ROOT));
    } catch (Exception e) {
      throw new IllegalArgumentException("Invalid role: " + value);
    }
  }

  private String normalizeEmail(String email) {
    if (email == null) {
      return null;
    }
    return normalizeEmailLoose(email);
  }

  private String normalizeName(String name) {
    if (name == null) {
      return null;
    }
    return name.trim();
  }

  private String normalizeEmailLoose(String email) {
    if (email == null) {
      return "";
    }
    String lowered = email.trim().toLowerCase(Locale.ROOT);
    StringBuilder sb = new StringBuilder(lowered.length());
    for (int i = 0; i < lowered.length(); i++) {
      char c = lowered.charAt(i);
      if (!Character.isWhitespace(c)) {
        sb.append(c);
      }
    }
    return sb.toString();
  }
}
