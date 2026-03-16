package com.vibe.sports.news.api;

import com.vibe.sports.news.domain.NewsIssueSummary;
import com.vibe.sports.news.repository.NewsIssueSummaryRepository;
import com.vibe.sports.news.service.NewsIngestService;
import com.vibe.sports.news.service.NewsSummaryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/news")
public class NewsSummaryController {

    private final NewsIssueSummaryRepository newsIssueSummaryRepository;
    private final NewsIngestService newsIngestService;
    private final NewsSummaryService newsSummaryService;

    public NewsSummaryController(NewsIssueSummaryRepository newsIssueSummaryRepository,
                                 NewsIngestService newsIngestService,
                                 NewsSummaryService newsSummaryService) {
        this.newsIssueSummaryRepository = newsIssueSummaryRepository;
        this.newsIngestService = newsIngestService;
        this.newsSummaryService = newsSummaryService;
    }

    /**
     * 예: GET /api/news/summary?category=SOCCER&keyword=축구
     */
    @GetMapping("/summary")
    public ResponseEntity<?> getLatestSummary(
            @RequestParam String category,
            @RequestParam String keyword
    ) {
        // 1) 요약이 없거나, 1시간 이상 지난 경우 → 데이터 수집 및 요약 보장
        newsIngestService.fetchAndSave(category, keyword);
        NewsIssueSummary ensured = newsSummaryService.ensureFreshSummary(category, keyword);
        return toResponse(ensured);
    }

    private ResponseEntity<?> toResponse(NewsIssueSummary summary) {
        Map<String, Object> body = Map.of(
                "category", summary.getCategory(),
                "keyword", summary.getKeyword(),
                "fromDatetime", summary.getFromDatetime(),
                "toDatetime", summary.getToDatetime(),
                "summaryTitle", summary.getSummaryTitle(),
                "summaryText", summary.getSummaryText(),
                "relatedArticles", summary.getRelatedArticles()
        );
        return ResponseEntity.ok(body);
    }
}

