package zw.co.tncybertech.auth.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import zw.co.tncybertech.auth.domain.User;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
  Optional<User> findByEmail(String email);
  boolean existsByEmail(String email);

  @Query("select u from User u where lower(trim(u.email)) = lower(trim(:email))")
  Optional<User> findByEmailNormalized(@Param("email") String email);
}
