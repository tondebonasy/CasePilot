package zw.co.tncybertech.dispute.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "evidence")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class Evidence {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "dispute_id")
  private Dispute dispute;

  @Column(name = "file_name", nullable = false, length = 255)
  private String fileName;

  @Column(name = "content_type", nullable = false, length = 120)
  private String contentType;

  @Column(name = "storage_path", nullable = false, length = 500)
  private String storagePath;

  @Column(name = "uploaded_by_user_id", nullable = false)
  private Long uploadedByUserId;

  @Column(name = "uploaded_at", nullable = false)
  private Instant uploadedAt;
}
