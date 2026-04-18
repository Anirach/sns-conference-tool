package com.sns.sns.app;

import com.sns.identity.app.AuditLogger;
import com.sns.sns.crypto.AesGcmCipher;
import com.sns.sns.domain.SnsLinkEntity;
import com.sns.sns.domain.SnsLinkEntity.Provider;
import com.sns.sns.repo.SnsLinkRepository;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

/**
 * Periodically refreshes {@code imported_data} on linked SNS accounts by calling the provider's
 * userinfo endpoint with the stored (encrypted) access token. Fields kept:
 * <ul>
 *   <li>Facebook: id, name, picture.data.url, email (if granted).</li>
 *   <li>LinkedIn: sub, name, picture, email.</li>
 * </ul>
 * Tokens that fail to decrypt, are expired, or are rejected by the provider are left in place —
 * the next scheduled sweep tries again. Persistent failures should be surfaced via the
 * {@code SnsPushDeliveryFailure}-style alerting (out of scope for this job).
 *
 * <p>Runs only when {@code sns.enrichment.enabled=true} (default false) so local dev doesn't
 * hammer provider APIs without secrets.
 */
@Component
@ConditionalOnProperty(name = "sns.enrichment.enabled", havingValue = "true")
public class SnsEnrichmentJob {

    private static final Logger log = LoggerFactory.getLogger(SnsEnrichmentJob.class);

    private final SnsLinkRepository repo;
    private final AesGcmCipher cipher;
    private final AuditLogger audit;
    private final int staleHours;

    public SnsEnrichmentJob(
        SnsLinkRepository repo,
        AesGcmCipher cipher,
        AuditLogger audit,
        @Value("${sns.enrichment.stale-hours:24}") int staleHours
    ) {
        this.repo = repo;
        this.cipher = cipher;
        this.audit = audit;
        this.staleHours = staleHours;
    }

    private static final int PAGE_SIZE = 100;

    @Scheduled(cron = "${sns.enrichment.cron:0 15 */6 * * *}")
    public void sweep() {
        OffsetDateTime cutoff = OffsetDateTime.now().minusHours(staleHours);
        // Page through stale rows only — never full-scan sns_links.
        while (true) {
            List<SnsLinkEntity> page = repo.findByLastFetchIsNullOrLastFetchBefore(
                cutoff,
                org.springframework.data.domain.PageRequest.of(0, PAGE_SIZE)
            );
            if (page.isEmpty()) return;
            for (SnsLinkEntity link : page) {
                try {
                    enrich(link);
                } catch (Exception e) {
                    log.warn("sns.enrich failed link={} provider={} err={}",
                        link.getSnsId(), link.getProvider(), e.toString());
                }
            }
            if (page.size() < PAGE_SIZE) return;
        }
    }

    @Transactional
    protected void enrich(SnsLinkEntity link) {
        if (link.getAccessTokenEnc() == null || link.getTokenIv() == null) return;
        String token = cipher.decryptToString(link.getTokenIv(), link.getAccessTokenEnc());
        Map<String, Object> fetched = switch (link.getProvider()) {
            case FACEBOOK -> fetchFacebook(token);
            case LINKEDIN -> fetchLinkedIn(token);
        };
        if (fetched != null) {
            link.setImportedData(fetched);
            link.setLastFetch(OffsetDateTime.now());
            repo.save(link);
            audit.log("sns.enrich", link.getUserId(), "sns_link", link.getProvider().name());
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private Map<String, Object> fetchFacebook(String accessToken) {
        RestClient rest = RestClient.create();
        try {
            Map json = rest.get()
                .uri("https://graph.facebook.com/v19.0/me?fields=id,name,email,picture")
                .header("Authorization", "Bearer " + accessToken)
                .retrieve().body(Map.class);
            if (json == null) return null;
            Map<String, Object> out = new HashMap<>();
            put(out, "id", json.get("id"));
            put(out, "name", json.get("name"));
            put(out, "email", json.get("email"));
            Object pic = json.get("picture");
            if (pic instanceof Map<?, ?> pm && pm.get("data") instanceof Map<?, ?> data) {
                put(out, "pictureUrl", data.get("url"));
            }
            return out;
        } catch (Exception e) {
            log.warn("fetchFacebook failed: {}", e.toString());
            return null;
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private Map<String, Object> fetchLinkedIn(String accessToken) {
        RestClient rest = RestClient.create();
        try {
            Map json = rest.get()
                .uri("https://api.linkedin.com/v2/userinfo")
                .header("Authorization", "Bearer " + accessToken)
                .retrieve().body(Map.class);
            if (json == null) return null;
            Map<String, Object> out = new HashMap<>();
            put(out, "id", json.getOrDefault("sub", json.get("id")));
            put(out, "name", json.get("name"));
            put(out, "email", json.get("email"));
            put(out, "pictureUrl", json.get("picture"));
            return out;
        } catch (Exception e) {
            log.warn("fetchLinkedIn failed: {}", e.toString());
            return null;
        }
    }

    private static void put(Map<String, Object> m, String k, Object v) {
        if (v != null) m.put(k, v);
    }
}
