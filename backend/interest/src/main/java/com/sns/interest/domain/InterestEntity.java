package com.sns.interest.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Type;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "interests")
public class InterestEntity {

    @Id
    @Column(name = "interest_id")
    private UUID interestId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private InterestType type;

    @Column(name = "content", nullable = false, columnDefinition = "text")
    private String content;

    @Column(name = "article_url")
    private String articleUrl;

    @Column(name = "article_object_key")
    private String articleObjectKey;

    @Column(name = "extracted_keywords", columnDefinition = "text[]")
    @JdbcTypeCode(SqlTypes.ARRAY)
    private String[] extractedKeywords = new String[0];

    @Type(JsonType.class)
    @Column(name = "keyword_vector", columnDefinition = "jsonb", nullable = false)
    private Map<String, Double> keywordVector = Map.of();

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    void prePersist() {
        if (interestId == null) interestId = UUID.randomUUID();
        if (createdAt == null) createdAt = OffsetDateTime.now();
    }

    public UUID getInterestId() { return interestId; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public InterestType getType() { return type; }
    public void setType(InterestType type) { this.type = type; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getArticleUrl() { return articleUrl; }
    public void setArticleUrl(String articleUrl) { this.articleUrl = articleUrl; }
    public String getArticleObjectKey() { return articleObjectKey; }
    public void setArticleObjectKey(String articleObjectKey) { this.articleObjectKey = articleObjectKey; }
    public String[] getExtractedKeywords() { return extractedKeywords; }
    public void setExtractedKeywords(String[] extractedKeywords) { this.extractedKeywords = extractedKeywords; }

    @JsonIgnore
    public Map<String, Double> getKeywordVector() { return keywordVector; }
    public void setKeywordVector(Map<String, Double> keywordVector) { this.keywordVector = keywordVector; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}
