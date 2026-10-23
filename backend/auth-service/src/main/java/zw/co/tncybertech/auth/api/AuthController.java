package zw.co.tncybertech.auth.api;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import zw.co.tncybertech.auth.api.dto.AuthDtos;
import zw.co.tncybertech.auth.repo.UserRepository;
import zw.co.tncybertech.auth.service.AuthService;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class AuthController {

  private final AuthService authService;
  private final UserRepository users;

  @PostMapping("/auth/register")
  public ResponseEntity<AuthDtos.AuthResponse> register(@Valid @RequestBody AuthDtos.RegisterRequest req) {
    String token = authService.registerCustomer(req);
    return ResponseEntity.ok(new AuthDtos.AuthResponse(token));
  }

  @PostMapping("/auth/login")
  public ResponseEntity<AuthDtos.AuthResponse> login(@Valid @RequestBody AuthDtos.LoginRequest req) {
    String token = authService.login(req);
    return ResponseEntity.ok(new AuthDtos.AuthResponse(token));
  }

  @GetMapping("/users/me")
  public ResponseEntity<?> me(@RequestHeader("X-User-Id") String userId,
                              @RequestHeader("X-User-Role") String role,
                              @RequestHeader(value = "X-User-Email", required = false) String email,
                              @RequestHeader(value = "X-User-Name", required = false) String name) {
    // When called through gateway, headers are injected from JWT.
    return ResponseEntity.ok(new java.util.LinkedHashMap<String, Object>() {{
      put("id", Long.valueOf(userId));
      put("role", role);
      put("email", email);
      put("fullName", name);
    }});
  }

  @GetMapping("/admin/users")
  @PreAuthorize("hasAnyAuthority('ADMIN','ROLE_ADMIN')")
  public ResponseEntity<?> listUsers() {
    var list = users.findAll().stream().map(u ->
        new AuthDtos.UserResponse(u.getId(), u.getEmail(), u.getFullName(), u.getRole().name(), u.isEnabled())
    ).toList();
    return ResponseEntity.ok(list);
  }

  @PostMapping("/admin/users")
  @PreAuthorize("hasAnyAuthority('ADMIN','ROLE_ADMIN')")
  public ResponseEntity<AuthDtos.UserResponse> createUser(@Valid @RequestBody AuthDtos.AdminCreateUserRequest req) {
    return ResponseEntity.ok(authService.createUser(req));
  }

  @PutMapping("/admin/users/{id}/status")
  @PreAuthorize("hasAnyAuthority('ADMIN','ROLE_ADMIN')")
  public ResponseEntity<AuthDtos.UserResponse> updateStatus(@PathVariable Long id,
                                                            @Valid @RequestBody AuthDtos.AdminUpdateUserStatusRequest req) {
    return ResponseEntity.ok(authService.updateUserStatus(id, req.enabled()));
  }

  @PutMapping("/admin/users/{id}/role")
  @PreAuthorize("hasAnyAuthority('ADMIN','ROLE_ADMIN')")
  public ResponseEntity<AuthDtos.UserResponse> updateRole(@PathVariable Long id,
                                                          @Valid @RequestBody AuthDtos.AdminUpdateUserRoleRequest req) {
    return ResponseEntity.ok(authService.updateUserRole(id, req.role()));
  }

  @PutMapping("/admin/users/{id}/password")
  @PreAuthorize("hasAnyAuthority('ADMIN','ROLE_ADMIN')")
  public ResponseEntity<AuthDtos.UserResponse> resetPassword(@PathVariable Long id,
                                                             @Valid @RequestBody AuthDtos.AdminResetPasswordRequest req) {
    return ResponseEntity.ok(authService.resetUserPassword(id, req.password()));
  }
}
