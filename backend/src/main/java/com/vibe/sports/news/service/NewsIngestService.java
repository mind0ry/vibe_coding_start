package com.vibe.sports.news.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vibe.sports.news.domain.NewsArticle;
import com.vibe.sports.news.repository.NewsArticleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;
import org.springframework.web.client.RestClientResponseException;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Service
public class NewsIngestService {

    private static final Logger log = LoggerFactory.getLogger(NewsIngestService.class);

    private final NaverNewsClient naverNewsClient;
    private final NewsArticleRepository newsArticleRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final DateTimeFormatter NAVER_DATE_FORMAT =
            DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss Z", java.util.Locale.ENGLISH);

    public NewsIngestService(NaverNewsClient naverNewsClient, NewsArticleRepository newsArticleRepository) {
        this.naverNewsClient = naverNewsClient;
        this.newsArticleRepository = newsArticleRepository;
    }

    /**
     * 네이버 뉴스 API에서 카테고리/키워드별로 데이터를 가져와 DB에 저장.
     * 트랜잭션은 개별 INSERT 수준으로만 적용되도록 메서드 전체에는 트랜잭션을 걸지 않습니다.
     */
    public void fetchAndSave(String category, String keyword) {
        try {
            String raw = naverNewsClient.searchNews(keyword, "date", 30);
            JsonNode root = objectMapper.readTree(raw);
            JsonNode items = root.path("items");

            if (!items.isArray()) {
                log.warn("네이버 뉴스 응답에 items 배열이 없습니다. category={}, keyword={}", category, keyword);
                return;
            }

            for (JsonNode item : items) {
                String title = clean(item.path("title").asText());
                String description = clean(item.path("description").asText(null));
                String originallink = item.path("originallink").asText(null);
                String link = item.path("link").asText();
                String pubDateStr = item.path("pubDate").asText();

                OffsetDateTime pubDate = OffsetDateTime.parse(pubDateStr, NAVER_DATE_FORMAT)
                        .withOffsetSameInstant(ZoneId.of("Asia/Seoul").getRules().getOffset(java.time.Instant.now()));

                // 중복 link + keyword 는 스키마에서 유니크 인덱스로 막고 있으므로, try-catch 로 무시 가능
                NewsArticle article = new NewsArticle();
                article.setCategory(category);
                article.setKeyword(keyword);
                article.setTitle(title);
                article.setDescription(description);
                article.setOriginallink(originallink);
                article.setLink(link);
                article.setPubDate(pubDate);
                article.setRawJson(item.toString());

                try {
                    newsArticleRepository.save(article);
                } catch (Exception e) {
                    log.debug("기사 저장 실패 (중복 가능): link={}, keyword={}", link, keyword);
                }
            }
            log.info("뉴스 수집 완료. category={}, keyword={}, items={}", category, keyword, items.size());
        } catch (RestClientResponseException e) {
            log.error("네이버 API 응답 오류. status={}, body={}", e.getRawStatusCode(), e.getResponseBodyAsString());
            throw e;
        } catch (IllegalStateException e) {
            log.error("설정 오류: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("네이버 뉴스 수집 중 오류 발생. category={}, keyword={}", category, keyword, e);
            throw new RuntimeException(e);
        }
    }

    private String clean(String value) {
        if (value == null) {
            return null;
        }
        // HTML 태그 제거 후 HTML 엔티티(&quot; 등)와 URL 인코딩을 정리
        String noTags = value.replaceAll("<[^>]*>", "");
        String unescaped = HtmlUtils.htmlUnescape(noTags);
        try {
            return URLDecoder.decode(unescaped, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return unescaped;
        }
    }
}

