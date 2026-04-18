package com.sns.app.dev;

import com.sns.chat.domain.ChatMessageEntity;
import com.sns.chat.repo.ChatMessageRepository;
import com.sns.event.app.QrCodeService;
import com.sns.event.domain.EventEntity;
import com.sns.event.domain.ParticipationEntity;
import com.sns.event.repo.EventRepository;
import com.sns.event.repo.ParticipationRepository;
import com.sns.identity.domain.Role;
import com.sns.identity.domain.UserEntity;
import com.sns.identity.repo.UserRepository;
import com.sns.interest.app.KeywordExtractor;
import com.sns.interest.domain.InterestEntity;
import com.sns.interest.domain.InterestType;
import com.sns.interest.repo.InterestRepository;
import com.sns.matching.app.MatchingService;
import com.sns.profile.domain.ProfileEntity;
import com.sns.profile.repo.ProfileRepository;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Populates Postgres with the same demo dataset the MSW mocks ship under
 * {@code web/lib/fixtures/} — 21 users with portraits, NeurIPS 2026 + ACL 2026 participations
 * with venue-clustered PostGIS positions, a few interests per user (so matching has real input),
 * recomputed similarity matches, and the 6 chat threads from the mock fixtures.
 *
 * <p>Opt-in via {@code sns.dev.seed-demo-data=true}. Default off so a vanilla dev boot keeps the
 * lightweight events-only seed. Idempotent via the sentinel user {@code you@example.com}: if it
 * already exists the entire seed short-circuits, so re-runs cost a single SELECT.
 *
 * <p>Demo login: {@code you@example.com} / {@code Demo!2026} — Alex Chen, ETH Zurich.
 */
@Configuration
@ConditionalOnProperty(name = "sns.dev.seed-demo-data", havingValue = "true")
public class DemoDataSeeder {

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);
    private static final String SENTINEL_EMAIL = "you@example.com";
    private static final String SEED_PASSWORD = "Demo!2026";
    private static final GeometryFactory GEO = new GeometryFactory(new PrecisionModel(), 4326);

    // Queen Sirikit National Convention Center, Bangkok (NeurIPS 2026 venue).
    private static final double NEURIPS_LAT = 13.7234;
    private static final double NEURIPS_LON = 100.5601;
    // Austria Center Vienna (ACL 2026 venue).
    private static final double ACL_LAT = 48.2336;
    private static final double ACL_LON = 16.4124;

    private record SeedUser(
        String mockId, String email, String first, String last,
        String title, String institution, String avatar, List<SeedInterest> interests
    ) {}

    private record SeedInterest(InterestType type, String content, String articleUrl) {}

    private record SeedChat(String fromMockId, String toMockId, String text, int minutesAgo) {}

    private static final List<SeedUser> USERS = List.of(
        // The demo "me" — fixture interests reproduced verbatim (with the ARTICLE_LOCAL pdf
        // demoted to a TEXT description, since seeding doesn't round-trip MinIO).
        new SeedUser("u-you-0001", "you@example.com", "Alex", "Chen",
            "PhD Candidate", "ETH Zurich", "/avatars/jordan.jpg",
            List.of(
                new SeedInterest(InterestType.TEXT,
                    "I work on graph neural networks for drug-target interaction prediction, "
                    + "focusing on heterogeneous graphs and explainability.", null),
                new SeedInterest(InterestType.ARTICLE_LINK,
                    "Efficient transformer attention for long-context language models, "
                    + "covering linear attention and sparse approximations.",
                    "https://arxiv.org/abs/2305.12345"),
                new SeedInterest(InterestType.TEXT,
                    "Federated learning survey: privacy, distributed training, "
                    + "differential privacy, and robust model aggregation.", null)
            )),
        new SeedUser("u-0002", "alice.smith@mit.edu", "Alice", "Smith",
            "Prof.", "MIT", "/avatars/alice.jpg",
            List.of(
                interest("Graph neural networks for drug-target interaction with attention pooling."),
                interest("Heterogeneous graph models, explainability, and benchmarking.")
            )),
        new SeedUser("u-0003", "somchai.r@chula.ac.th", "Somchai", "Ratanakul",
            "Assoc. Prof.", "Chulalongkorn University", "/avatars/hiro.jpg",
            List.of(
                interest("Transformer architectures with long-context attention and efficient attention variants."),
                interest("Federated learning and privacy-preserving distributed training.")
            )),
        new SeedUser("u-0004", "h.mueller@tum.de", "Hannah", "Müller",
            "Dr.", "TU Munich", "/avatars/elin.jpg",
            List.of(
                interest("Federated learning, differential privacy, and heterogeneous graphs."),
                interest("Multi-relational benchmarks for graph neural networks.")
            )),
        new SeedUser("u-0005", "yuki.tanaka@u-tokyo.ac.jp", "Yuki", "Tanaka",
            "PhD Candidate", "University of Tokyo", "/avatars/hiro.jpg",
            List.of(
                interest("Diffusion models, multi-modal learning, and vision-language models."),
                interest("Long-context transformers and efficient attention mechanisms.")
            )),
        new SeedUser("u-0006", "priya.sharma@iitb.ac.in", "Priya", "Sharma",
            "Prof.", "IIT Bombay", "/avatars/carla.jpg",
            List.of(
                interest("Reinforcement learning, knowledge graphs, and contrastive learning."),
                interest("Self-supervised representation learning over large knowledge bases.")
            )),
        new SeedUser("u-0007", "marco.rossi@polimi.it", "Marco", "Rossi",
            "Dr.", "Politecnico di Milano", "/avatars/farid.jpg",
            List.of(
                interest("Transformers with long-context attention and linear-attention variants."),
                interest("Retrieval-augmented generation and mixture-of-experts efficient inference.")
            )),
        new SeedUser("u-0008", "aisha.okonkwo@uct.ac.za", "Aisha", "Okonkwo",
            "Prof.", "University of Cape Town", "/avatars/grace.jpg",
            List.of(
                interest("Efficient attention, mixture-of-experts, and retrieval-augmented generation."),
                interest("Knowledge graphs, explainability, and diffusion models for science.")
            )),
        new SeedUser("u-0009", "jean.dupont@inria.fr", "Jean", "Dupont",
            "Research Scientist", "INRIA Paris", "/avatars/ben.jpg",
            List.of(
                interest("Self-supervised and contrastive learning over knowledge graphs."),
                interest("Diffusion models and multi-modal vision-language pretraining.")
            )),
        new SeedUser("u-0010", "lina.kowalski@ethz.ch", "Lina", "Kowalski",
            "Postdoc", "ETH Zurich", "/avatars/elin.jpg",
            List.of(
                interest("Diffusion models, multi-modal learning, and vision-language models."),
                interest("Self-supervised contrastive pretraining for scientific imagery.")
            )),
        new SeedUser("u-0011", "diego.fernandez@unam.mx", "Diego", "Fernández",
            "PhD Candidate", "UNAM", "/avatars/farid.jpg",
            List.of(
                interest("Heterogeneous graphs, explainability, and knowledge graph embeddings."),
                interest("Graph neural networks for drug-target interaction.")
            )),
        new SeedUser("u-0012", "emma.ohara@tcd.ie", "Emma", "O'Hara",
            "Dr.", "Trinity College Dublin", "/avatars/grace.jpg",
            List.of(
                interest("Federated learning, privacy, and differential privacy in healthcare."),
                interest("Distributed training and model aggregation across hospitals.")
            )),
        new SeedUser("u-0013", "rajesh.iyer@stanford.edu", "Rajesh", "Iyer",
            "Prof.", "Stanford University", "/avatars/dmitri.jpg",
            List.of(
                interest("Transformers, long-context attention, and efficient attention research."),
                interest("Graph neural networks and knowledge graph reasoning.")
            )),
        new SeedUser("u-0014", "nina.petrov@bsu.by", "Nina", "Petrov",
            "Assoc. Prof.", "Belarusian State University", "/avatars/alice.jpg",
            List.of(
                interest("Reinforcement learning with diffusion models and multi-modal observations."),
                interest("Vision-language models for embodied agents.")
            )),
        new SeedUser("u-0015", "kenji.ito@kyoto-u.ac.jp", "Kenji", "Ito",
            "Dr.", "Kyoto University", "/avatars/hiro.jpg",
            List.of(
                interest("Graph neural networks for drug-target interaction prediction."),
                interest("Heterogeneous graphs and explainability for life-science models.")
            )),
        new SeedUser("u-0016", "fatima.alghamdi@kaust.edu.sa", "Fatima", "Al-Ghamdi",
            "PhD Candidate", "KAUST", "/avatars/carla.jpg",
            List.of(
                interest("Long-context attention, efficient attention, and mixture-of-experts."),
                interest("Retrieval-augmented generation for scientific literature.")
            )),
        new SeedUser("u-0017", "lukas.svensson@kth.se", "Lukas", "Svensson",
            "Prof.", "KTH Stockholm", "/avatars/ben.jpg",
            List.of(
                interest("Self-supervised learning, contrastive learning, and knowledge graphs."),
                interest("Graph representation learning for industrial applications.")
            )),
        new SeedUser("u-0018", "ana.ferreira@usp.br", "Ana", "Ferreira",
            "Research Scientist", "University of São Paulo", "/avatars/grace.jpg",
            List.of(
                interest("Diffusion models, multi-modal learning, and vision-language models."),
                interest("Federated learning and privacy in agricultural sensor networks.")
            )),
        new SeedUser("u-0019", "michael.oconnor@unimelb.edu.au", "Michael", "O'Connor",
            "Dr.", "University of Melbourne", "/avatars/dmitri.jpg",
            List.of(
                interest("Reinforcement learning, knowledge graphs, and graph neural networks."),
                interest("Explainability and interpretability of large language models.")
            )),
        new SeedUser("u-0020", "chen.wei@pku.edu.cn", "Wei", "Chen",
            "Prof.", "Peking University", "/avatars/hiro.jpg",
            List.of(
                interest("Transformers, attention, long-context, and mixture-of-experts."),
                interest("Retrieval-augmented generation and efficient inference at scale.")
            ))
    );

    // ACL Vienna participants — matches the mock's otherUsers.slice(3, 9): u-0004..u-0009.
    // The demo user joins too (so they see the event in their joined list, like the mock).
    private static final List<String> ACL_PARTICIPANTS = List.of(
        "u-you-0001", "u-0004", "u-0005", "u-0006", "u-0007", "u-0008", "u-0009"
    );

    // 18 chat messages from web/lib/fixtures/chats.ts. All scoped to NeurIPS.
    private static final List<SeedChat> CHATS = List.of(
        new SeedChat("u-0002", "u-you-0001",
            "Hi Alex! Saw you work on GNNs for drug discovery — I gave a talk on that this morning.", 120),
        new SeedChat("u-you-0001", "u-0002",
            "Prof. Smith! I caught part of it online. The attention-pooling result was striking.", 118),
        new SeedChat("u-0002", "u-you-0001",
            "Thank you. Are you around for coffee after session 3?", 117),
        new SeedChat("u-you-0001", "u-0002",
            "Yes, where is good? Main hall?", 115),
        new SeedChat("u-0002", "u-you-0001",
            "Main hall espresso bar, see you at 3:30.", 114),

        new SeedChat("u-you-0001", "u-0003",
            "สวัสดีครับอาจารย์! You shared common keywords with me — federated learning and privacy.", 75),
        new SeedChat("u-0003", "u-you-0001",
            "Hello! Yes, we run a small FL group at Chula. Would love to compare notes.", 73),
        new SeedChat("u-you-0001", "u-0003",
            "Happy to. Are you presenting a poster?", 70),
        new SeedChat("u-0003", "u-you-0001",
            "Thursday afternoon, poster #127.", 69),

        new SeedChat("u-0004", "u-you-0001",
            "Your abstract on heterogeneous graphs was fascinating.", 50),
        new SeedChat("u-you-0001", "u-0004",
            "Thanks Hannah! Have you tried the multi-relational benchmarks from last year?", 48),
        new SeedChat("u-0004", "u-you-0001",
            "Briefly. Do you have comparisons?", 46),

        new SeedChat("u-you-0001", "u-0005",
            "こんにちは Yuki, nice to match!", 30),
        new SeedChat("u-0005", "u-you-0001",
            "Hello! Do you have a minute to chat about long-context models?", 29),
        new SeedChat("u-you-0001", "u-0005",
            "Of course, where are you standing?", 28),

        new SeedChat("u-0006", "u-you-0001",
            "Hi! Matched on transformers + attention. Quick question about your linear-attention variant.", 15),
        new SeedChat("u-you-0001", "u-0006",
            "Happy to — what's the question?", 14),

        new SeedChat("u-0009", "u-you-0001",
            "Bonjour! We share diffusion-models and multi-modal as keywords.", 8),
        new SeedChat("u-you-0001", "u-0009",
            "Bonjour Jean! Yes, I'm curious if INRIA's latest work is open-source?", 6)
    );

    @Bean
    @Order(20) // After DevSeedRunner (@Order(10)) so the demo events exist by the time we look them up.
    ApplicationRunner runDemoSeed(
        UserRepository users,
        ProfileRepository profiles,
        EventRepository events,
        ParticipationRepository participations,
        ChatMessageRepository chatRepo,
        QrCodeService qr,
        InterestRepository interestRepo,
        KeywordExtractor keywordExtractor,
        MatchingService matchingService,
        PasswordEncoder passwordEncoder,
        PlatformTransactionManager txMgr
    ) {
        TransactionTemplate tx = new TransactionTemplate(txMgr);
        return args -> {
            if (users.findByEmailIgnoreCase(SENTINEL_EMAIL).isPresent()) {
                log.info("DemoDataSeeder: sentinel {} already present — skipping demo seed", SENTINEL_EMAIL);
                return;
            }

            EventEntity neurips = events.findByQrCodeHash(qr.hash("NEURIPS2026"))
                .orElseThrow(() -> new IllegalStateException(
                    "DemoDataSeeder requires sns.dev.seed-events=true (NEURIPS2026 missing)"));
            EventEntity acl = events.findByQrCodeHash(qr.hash("ACL2026"))
                .orElseThrow(() -> new IllegalStateException(
                    "DemoDataSeeder requires sns.dev.seed-events=true (ACL2026 missing)"));

            // Phase 1: users + profiles + NeurIPS participations.
            // One transaction so a partial failure rolls back cleanly. The sentinel check above
            // guards against picking up a half-seeded DB on retry.
            String seedHash = passwordEncoder.encode(SEED_PASSWORD);
            int userCount = tx.execute(status -> {
                int n = 0;
                for (SeedUser u : USERS) {
                    UUID userId = seedUuid("user:" + u.mockId());
                    UserEntity ue = new UserEntity();
                    ue.setUserId(userId);
                    ue.setEmail(u.email());
                    ue.setEmailVerified(true);
                    ue.setPasswordHash(seedHash);
                    ue.setRole(roleFor(u.mockId()));
                    users.save(ue);

                    ProfileEntity pe = new ProfileEntity();
                    pe.setUserId(userId);
                    pe.setFirstName(u.first());
                    pe.setLastName(u.last());
                    pe.setAcademicTitle(u.title());
                    pe.setInstitution(u.institution());
                    pe.setProfilePictureUrl(u.avatar());
                    profiles.save(pe);

                    participations.save(participation(
                        userId, neurips.getEventId(), NEURIPS_LAT, NEURIPS_LON, u.mockId()));
                    n++;
                }
                return n;
            });

            // Phase 2: ACL participations for the mock subset.
            int aclCount = tx.execute(status -> {
                int n = 0;
                for (String mockId : ACL_PARTICIPANTS) {
                    UUID userId = seedUuid("user:" + mockId);
                    participations.save(participation(
                        userId, acl.getEventId(), ACL_LAT, ACL_LON, mockId));
                    n++;
                }
                return n;
            });

            // Phase 3: interests. We bypass InterestService.create on purpose — it publishes
            // UserInterestsChanged, and MatchingEventListener handles that with @Async, which
            // races with itself across 21 parallel users and trips the (event_id, user_id_a,
            // user_id_b) unique index. We still run the real KeywordExtractor so populated
            // extracted_keywords + keyword_vector look identical to a real interest creation;
            // the single recompute() in Phase 4 below is then the only writer to similarity_matches.
            int interestCount = tx.execute(status -> {
                int n = 0;
                for (SeedUser u : USERS) {
                    UUID userId = seedUuid("user:" + u.mockId());
                    for (SeedInterest si : u.interests()) {
                        KeywordExtractor.Extraction ex = keywordExtractor.extract(si.content());
                        InterestEntity e = new InterestEntity();
                        e.setUserId(userId);
                        e.setType(si.type());
                        e.setContent(si.content());
                        e.setArticleUrl(si.articleUrl());
                        e.setExtractedKeywords(ex.keywordsArray());
                        e.setKeywordVector(ex.vector());
                        interestRepo.save(e);
                        n++;
                    }
                }
                return n;
            });

            // Phase 4: trigger a full recompute on both active events. The
            // UserInterestsChanged listener already ran per-interest above (incremental), but a
            // final full sweep guarantees every pair has been considered.
            int neuripsMatches = matchingService.recompute(neurips.getEventId());
            int aclMatches = matchingService.recompute(acl.getEventId());

            // Phase 5: chat history — direct repo writes so we can pin createdAt to the same
            // "minutes-ago" deltas the mock uses, set readFlag=true in one shot, and avoid
            // generating push-outbox rows that would never reach a real device.
            int chatCount = tx.execute(status -> {
                int n = 0;
                OffsetDateTime now = OffsetDateTime.now();
                for (int i = 0; i < CHATS.size(); i++) {
                    SeedChat c = CHATS.get(i);
                    ChatMessageEntity m = new ChatMessageEntity();
                    m.setMessageId(seedUuid("chat:" + i + ":" + c.fromMockId() + ":" + c.toMockId()));
                    m.setEventId(neurips.getEventId());
                    m.setFromUserId(seedUuid("user:" + c.fromMockId()));
                    m.setToUserId(seedUuid("user:" + c.toMockId()));
                    m.setContent(c.text());
                    m.setReadFlag(true);
                    m.setClientMessageId("seed:" + i);
                    m.setCreatedAt(now.minusMinutes(c.minutesAgo()));
                    chatRepo.save(m);
                    n++;
                }
                return n;
            });

            log.info("DemoDataSeeder: seeded {} users / {} NeurIPS participants / {} ACL participants / "
                + "{} interests / {} NeurIPS matches / {} ACL matches / {} chat messages — "
                + "login as {} / {}",
                userCount, userCount, aclCount, interestCount,
                neuripsMatches, aclMatches, chatCount, SENTINEL_EMAIL, SEED_PASSWORD);
        };
    }

    private static SeedInterest interest(String content) {
        return new SeedInterest(InterestType.TEXT, content, null);
    }

    private static ParticipationEntity participation(
        UUID userId, UUID eventId, double centerLat, double centerLon, String mockId
    ) {
        ParticipationEntity p = new ParticipationEntity();
        p.setUserId(userId);
        p.setEventId(eventId);
        p.setSelectedRadius((short) 50);
        p.setLastPosition(scatterAround(centerLat, centerLon, mockId));
        p.setLastPositionAccM(8f);
        p.setLastUpdate(OffsetDateTime.now());
        return p;
    }

    /**
     * Deterministic ±~20 m jitter so all participants land inside each other's 50 m vicinity
     * radius. Using {@code mockId} as the seed keeps positions stable across re-seeds.
     */
    private static Point scatterAround(double centerLat, double centerLon, String mockId) {
        int seed = Math.abs(mockId.hashCode());
        // 0.00018° latitude ≈ 20 m anywhere on Earth.
        double dLat = ((seed % 401) - 200) / 1_000_000.0 * 0.9;
        double dLon = (((seed / 401) % 401) - 200) / 1_000_000.0 * 0.9;
        Point p = GEO.createPoint(new Coordinate(centerLon + dLon, centerLat + dLat));
        p.setSRID(4326);
        return p;
    }

    /**
     * Hands out demo roles so the admin user-list has something to filter on out of the box.
     * Alex Chen (the demo login) becomes SUPER_ADMIN; two senior fellows become ADMIN.
     */
    private static Role roleFor(String mockId) {
        return switch (mockId) {
            case "u-you-0001" -> Role.SUPER_ADMIN;
            case "u-0013", "u-0017" -> Role.ADMIN;
            default -> Role.USER;
        };
    }

    private static UUID seedUuid(String key) {
        UUID raw = UUID.nameUUIDFromBytes(("sns-seed:" + key).getBytes(StandardCharsets.UTF_8));
        // Java's UUID.compareTo treats the 64-bit halves as signed longs; Postgres' UUID
        // type compares them as unsigned bytes. Clearing the most-significant bit on every
        // seeded UUID makes both orderings agree, so MatchingService's canonical (a < b)
        // pair selection lines up with the `CHECK (user_id_a < user_id_b)` constraint.
        long msb = raw.getMostSignificantBits() & 0x7FFFFFFFFFFFFFFFL;
        return new UUID(msb, raw.getLeastSignificantBits());
    }
}
