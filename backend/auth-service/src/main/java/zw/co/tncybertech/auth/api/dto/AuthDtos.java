package zw.co.tncybertech.auth.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AuthDtos {

  public record RegisterRequest(
      @Email @NotBlank String email,
      @NotBlank @Size(min = 8, max = 72) String password,
      @NotBlank @Size(min = 2, max = 190) String fullName
  ) {}

  public record LoginRequest(
      @Email @NotBlank String email,
      @NotBlank String password
  ) {}

  public record AuthResponse(
      String accessToken
  ) {}

  public record UserResponse(
      Long id,
      String email,
      String fullName,
      String role,
      boolean enabled
  ) {}

  public record AdminCreateUserRequest(
      @Email @NotBlank String email,
      @NotBlank @Size(min = 8, max = 72) String password,
      @NotBlank @Size(min = 2, max = 190) String fullName,
      @NotBlank String role
  ) {}

  public record AdminUpdateUserStatusRequest(
      boolean enabled
  ) {}

  public record AdminUpdateUserRoleRequest(
      @NotBlank String role
  ) {}

  public record AdminResetPasswordRequest(
      @NotBlank @Size(min = 8, max = 72) String password
  ) {}
}
