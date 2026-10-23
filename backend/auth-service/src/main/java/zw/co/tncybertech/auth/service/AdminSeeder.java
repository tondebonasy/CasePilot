package zw.co.tncybertech.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import zw.co.tncybertech.auth.domain.User;
import zw.co.tncybertech.auth.domain.UserRole;
import zw.co.tncybertech.auth.repo.UserRepository;

@Component
@RequiredArgsConstructor
public class AdminSeeder implements ApplicationRunner {

  private final UserRepository users;
  private final PasswordEncoder passwordEncoder;

  @Value("${app.seed.admin.enabled:true}")
  private boolean enabled;

  @Value("${app.seed.admin.email:}")
  private String email;

  @Value("${app.seed.admin.password:}")
  private String password;

  @Value("${app.seed.admin.fullName:Default Admin}")
  private String fullName;

  @Override
  @Transactional
  public void run(ApplicationArguments args) {
    if (!enabled) {
      return;
    }
    if (email == null || email.isBlank() || password == null || password.isBlank()) {
      return;
    }

    String normalizedEmail = email.toLowerCase();
    if (users.existsByEmail(normalizedEmail)) {
      return;
    }

    User admin = User.builder()
        .email(normalizedEmail)
        .fullName(fullName)
        .passwordHash(passwordEncoder.encode(password))
        .role(UserRole.ADMIN)
        .enabled(true)
        .build();

    users.save(admin);
  }
}
