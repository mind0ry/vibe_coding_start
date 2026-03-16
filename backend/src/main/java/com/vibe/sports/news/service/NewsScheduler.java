package com.vibe.sports.news.service;

import com.vibe.sports.news.repository.NewsArticleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Component
public class NewsScheduler {

    private static final Logger log = LoggerFactory.getLogger(NewsScheduler.class);

    private final NewsArticleRepository newsArticleRepository;
    private final NewsIngestService newsIngestService;
    private final NewsSummaryService newsSummaryService;

    public NewsScheduler(NewsArticleRepository newsArticleRepository,
                         NewsIngestService newsIngestService,
                         NewsSummaryService newsSummaryService) {
        this.newsArticleRepository = newsArticleRepository;
        this.newsIngestService = newsIngestService;
        this.newsSummaryService = newsSummaryService;
    }

    @Scheduled(cron = "0 0/30 * * * *") // 30분마다
    public void fetchLatestNews() {
        log.info("fetchLatestNews 스케줄러 실행 - 네이버 뉴스 수집 시작");
        // 기본 4개 카테고리/키워드
        newsIngestService.fetchAndSave("SOCCER", "축구");
        newsIngestService.fetchAndSave("BASEBALL", "야구");
        newsIngestService.fetchAndSave("BASKETBALL", "농구");
        newsIngestService.fetchAndSave("VOLLEYBALL", "배구");
        log.info("fetchLatestNews 스케줄러 종료");
    }

    /**
     * 2일 이전 뉴스 정리 (매일 새벽 3시)
     */
    @Scheduled(cron = "0 0 3 * * *")
    public void cleanupOldNews() {
        OffsetDateTime cutoff = OffsetDateTime.now(ZoneOffset.UTC).minusDays(2);
        long deleted = newsArticleRepository.deleteByPubDateBefore(cutoff);
        log.info("cleanupOldNews 완료 - {}건 삭제 (cutoff={})", deleted, cutoff);
    }

    /**
     * 요약 미리 생성 (예: 1시간마다)
     */
    @Scheduled(cron = "0 5 * * * *")
    public void buildSummaries() {
        log.info("buildSummaries 스케줄러 실행 - 최근 2일 요약 생성 시작");
        newsSummaryService.buildTwoDaySummary("SOCCER", "축구");
        newsSummaryService.buildTwoDaySummary("BASEBALL", "야구");
        newsSummaryService.buildTwoDaySummary("BASKETBALL", "농구");
        newsSummaryService.buildTwoDaySummary("VOLLEYBALL", "배구");
        log.info("buildSummaries 스케줄러 종료");
    }
}

