package zw.co.tncybertech.dispute.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import zw.co.tncybertech.dispute.domain.Notification;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
  List<Notification> findByRecipientUserIdOrderByCreatedAtDesc(Long recipientUserId);
}
