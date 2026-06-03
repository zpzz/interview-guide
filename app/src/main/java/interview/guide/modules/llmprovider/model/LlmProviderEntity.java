package interview.guide.modules.llmprovider.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Index;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "llm_provider_config", indexes = {
    @Index(name = "idx_llm_provider_owner", columnList = "owner_user_id,id")
})
public class LlmProviderEntity {

  @Id
  @Column(length = 64)
  private String id;

  @Column(name = "owner_user_id")
  private Long ownerUserId;

  @Column(name = "display_id", length = 64)
  private String displayId;

  @Column(name = "base_url", nullable = false, length = 512)
  private String baseUrl;

  @Column(name = "api_key_ciphertext", nullable = false, length = 4096)
  private String apiKeyCiphertext;

  @Column(name = "api_key_nonce", nullable = false, length = 64)
  private String apiKeyNonce;

  @Column(nullable = false, length = 128)
  private String model;

  @Column(name = "embedding_model", length = 128)
  private String embeddingModel;

  @Column(name = "embedding_dimensions")
  private Integer embeddingDimensions;

  @Column(name = "supports_embedding", nullable = false)
  private boolean supportsEmbedding;

  private Double temperature;

  @Column(nullable = false)
  private boolean enabled;

  @Column(nullable = false)
  private boolean builtin;

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  @PrePersist
  void prePersist() {
    LocalDateTime now = LocalDateTime.now();
    createdAt = now;
    updatedAt = now;
    if (displayId == null || displayId.isBlank()) {
      displayId = id;
    }
  }

  @PreUpdate
  void preUpdate() {
    updatedAt = LocalDateTime.now();
  }
}
