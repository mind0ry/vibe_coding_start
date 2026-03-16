package com.vibe.sports.news.api;

import com.vibe.sports.news.service.NewsIngestService;
import com.vibe.sports.news.service.NewsSummaryService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/news")
public class AdminNewsController {

    private final NewsIngestService newsIngestService;
    private final NewsSummaryService newsSummaryService;

    public AdminNewsController(NewsIngestService newsIngestService,
                               NewsSummaryService newsSummaryService) {
        this.newsIngestService = newsIngestService;
        this.newsSummaryService = newsSummaryService;
    }

    /**
     * 수동으로 네 개 카테고리의 뉴스 수집 + 최근 2일 요약을 한 번에 수행.
     *
     * POST /api/admin/news/fetch-and-summarize
     */
    @PostMapping("/fetch-and-summarize")
    public ResponseEntity<?> fetchAndSummarize() {
        try {
            newsIngestService.fetchAndSave("SOCCER", "축구");
            newsIngestService.fetchAndSave("BASEBALL", "야구");
            newsIngestService.fetchAndSave("BASKETBALL", "농구");
            newsIngestService.fetchAndSave("VOLLEYBALL", "배구");

            newsSummaryService.buildTwoDaySummary("SOCCER", "축구");
            newsSummaryService.buildTwoDaySummary("BASEBALL", "야구");
            newsSummaryService.buildTwoDaySummary("BASKETBALL", "농구");
            newsSummaryService.buildTwoDaySummary("VOLLEYBALL", "배구");

            return ResponseEntity.accepted().body(Map.of("ok", true));
        } catch (Exception e) {
            Throwable root = e;
            while (root.getCause() != null) {
                root = root.getCause();
            }
            String message = root.getMessage() != null ? root.getMessage() : e.toString();

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "ok", false,
                            "message", message
                    ));
        }
    }
}

