package com.vibe.sports.news.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.vibe.sports.news.domain.NewsArticle;
import com.vibe.sports.news.domain.NewsIssueSummary;
import com.vibe.sports.news.repository.NewsArticleRepository;
import com.vibe.sports.news.repository.NewsIssueSummaryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

@Service
public class NewsSummaryService {

    private static final Logger log = LoggerFactory.getLogger(NewsSummaryService.class);

    private final NewsArticleRepository newsArticleRepository;
    private final NewsIssueSummaryRepository newsIssueSummaryRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public NewsSummaryService(NewsArticleRepository newsArticleRepository,
                              NewsIssueSummaryRepository newsIssueSummaryRepository) {
        this.newsArticleRepository = newsArticleRepository;
        this.newsIssueSummaryRepository = newsIssueSummaryRepository;
    }

    public void buildTwoDaySummary(String category, String keyword) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        OffsetDateTime from = now.minusDays(2);

        List<NewsArticle> articles = newsArticleRepository
                .findByCategoryAndPubDateBetweenOrderByPubDateDesc(category, from, now);

        if (articles.isEmpty()) {
            log.info("요약 생성 스킵 - 최근 2일 기사 없음. category={}, keyword={}", category, keyword);
            return;
        }

        // 간단한 규칙 기반 요약: 상위 5개 기사 제목/description을 문단으로 이어 붙임
        StringBuilder sb = new StringBuilder();
        int limit = Math.min(5, articles.size());
        for (int i = 0; i < limit; i++) {
            NewsArticle a = articles.get(i);
            sb.append("- ").append(a.getTitle());
            if (a.getDescription() != null && !a.getDescription().isBlank()) {
                sb.append(" : ").append(a.getDescription());
            }
            sb.append("\n");
        }

        String summaryText = sb.toString().trim();

        // 관련 기사 JSON 배열 구성
        ArrayNode related = objectMapper.createArrayNode();
        for (int i = 0; i < limit; i++) {
            NewsArticle a = articles.get(i);
            ObjectNode node = objectMapper.createObjectNode();
            node.put("article_id", a.getId());
            node.put("title", a.getTitle());
            node.put("link", a.getLink());
            node.put("originallink", a.getOriginallink());
            node.put("pub_date", a.getPubDate().toString());
            related.add(node);
        }

        NewsIssueSummary summary = new NewsIssueSummary();
        summary.setCategory(category);
        summary.setKeyword(keyword);
        summary.setFromDatetime(from);
        summary.setToDatetime(now);
        summary.setSummaryTitle(keyword + " 최근 2일 이슈 요약");
        summary.setSummaryText(summaryText);
        summary.setRelatedArticles(related.toString());

        newsIssueSummaryRepository.save(summary);
        log.info("요약 생성 완료 - category={}, keyword={}, articles={}", category, keyword, articles.size());
    }

    /**
     * 요약이 없으면 생성하고, 이미 있다면 생성 시간을 기준으로
     * 1시간이 지났으면 기존 데이터를 삭제 후 다시 생성.
     */
    public NewsIssueSummary ensureFreshSummary(String category, String keyword) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        Optional<NewsIssueSummary> latestOpt =
                newsIssueSummaryRepository.findFirstByCategoryAndKeywordOrderByGeneratedAtDesc(category, keyword);

        if (latestOpt.isEmpty()) {
            log.info("요약 없음 → 새로 생성. category={}, keyword={}", category, keyword);
            buildTwoDaySummary(category, keyword);
            return newsIssueSummaryRepository
                    .findFirstByCategoryAndKeywordOrderByGeneratedAtDesc(category, keyword)
                    .orElseThrow();
        }

        NewsIssueSummary latest = latestOpt.get();
        OffsetDateTime generatedAt = latest.getGeneratedAt();
        if (generatedAt != null && generatedAt.isAfter(now.minusHours(1))) {
            log.info("요약이 최신 상태(1시간 이내). category={}, keyword={}", category, keyword);
            return latest;
        }

        log.info("요약이 1시간 이상 지남 → 기존 기사/요약 삭제 후 재생성. category={}, keyword={}", category, keyword);
        // 오래된 요약은 그대로 두고 새 요약만 추가해도 되지만,
        // 요구사항에 맞춰 최신 데이터만 쓰고 싶다면 여기에서 정리 로직을 추가할 수 있습니다.

        buildTwoDaySummary(category, keyword);
        return newsIssueSummaryRepository
                .findFirstByCategoryAndKeywordOrderByGeneratedAtDesc(category, keyword)
                .orElseThrow();
    }
}

