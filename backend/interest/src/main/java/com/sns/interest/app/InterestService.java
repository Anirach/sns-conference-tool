package com.sns.interest.app;

import com.sns.common.events.UserInterestsChanged;
import com.sns.interest.api.dto.InterestDtos;
import com.sns.interest.domain.InterestEntity;
import com.sns.interest.domain.InterestType;
import com.sns.interest.repo.InterestRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class InterestService {

    private final InterestRepository repo;
    private final KeywordExtractor extractor;
    private final ArticleStorageService storage;
    private final ApplicationEventPublisher publisher;

    public InterestService(
        InterestRepository repo,
        KeywordExtractor extractor,
        ArticleStorageService storage,
        ApplicationEventPublisher publisher
    ) {
        this.repo = repo;
        this.extractor = extractor;
        this.storage = storage;
        this.publisher = publisher;
    }

    @Transactional(readOnly = true)
    public List<InterestDtos.InterestDto> listFor(UUID userId) {
        return repo.findByUserIdOrderByCreatedAtDesc(userId).stream()
            .map(InterestService::toDto)
            .toList();
    }

    @Transactional
    public InterestDtos.InterestDto create(UUID userId, InterestDtos.CreateRequest req) {
        KeywordExtractor.Extraction ex = extractor.extract(req.content());
        InterestEntity e = new InterestEntity();
        e.setUserId(userId);
        e.setType(req.type());
        e.setContent(req.content());
        e.setArticleUrl(req.articleUrl());
        e.setExtractedKeywords(ex.keywordsArray());
        e.setKeywordVector(ex.vector());
        var saved = repo.save(e);
        publisher.publishEvent(new UserInterestsChanged(userId));
        return toDto(saved);
    }

    @Transactional
    public InterestDtos.InterestDto createWithUpload(UUID userId, String filename, byte[] content, String contentType) {
        String text = new String(content, java.nio.charset.StandardCharsets.UTF_8); // TODO Phase 2.5: extract PDF text
        KeywordExtractor.Extraction ex = extractor.extract(text);
        String objectKey = storage.store(userId, filename, content, contentType);
        InterestEntity e = new InterestEntity();
        e.setUserId(userId);
        e.setType(InterestType.ARTICLE_LOCAL);
        e.setContent(filename == null ? "article" : filename);
        e.setArticleObjectKey(objectKey);
        e.setExtractedKeywords(ex.keywordsArray());
        e.setKeywordVector(ex.vector());
        var saved = repo.save(e);
        publisher.publishEvent(new UserInterestsChanged(userId));
        return toDto(saved);
    }

    @Transactional
    public void delete(UUID userId, UUID interestId) {
        long removed = repo.deleteByInterestIdAndUserId(interestId, userId);
        if (removed == 0) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        publisher.publishEvent(new UserInterestsChanged(userId));
    }

    private static InterestDtos.InterestDto toDto(InterestEntity e) {
        return new InterestDtos.InterestDto(
            e.getInterestId(),
            e.getUserId(),
            e.getType(),
            e.getContent(),
            List.of(e.getExtractedKeywords()),
            e.getCreatedAt()
        );
    }
}
