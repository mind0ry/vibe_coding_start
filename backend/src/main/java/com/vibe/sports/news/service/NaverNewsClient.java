package com.vibe.sports.news.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/**
 * 네이버 뉴스 검색 API 호출용 클라이언트.
 * API 키/시크릿은 application.yml 또는 환경 변수에서 주입하도록 비워둡니다.
 */
@Component
public class NaverNewsClient {

    private final RestClient restClient;

    // TODO: 여기에 네이버 API 키/시크릿 값을 채워주세요.
    @Value("${naver.api.client-id:}")
    private String clientId;

    @Value("${naver.api.client-secret:}")
    private String clientSecret;

    public NaverNewsClient(RestClient.Builder builder) {
        this.restClient = builder
                .baseUrl("https://openapi.naver.com/v1")
                .build();
    }

    /**
     * 네이버 뉴스 검색 API 호출.
     *
     * @param query  검색어 (예: "축구")
     * @param sort   정렬 방식 (sim: 정확도, date: 최신순)
     * @param display 가져올 결과 수 (최대 100)
     * @return 네이버 원본 JSON 문자열
     */
    public String searchNews(String query, String sort, int display) {
        if (clientId == null || clientId.isBlank() || clientSecret == null || clientSecret.isBlank()) {
            throw new IllegalStateException("네이버 API 키가 비어있습니다. application.yml의 naver.api.client-id / client-secret 을 설정하세요.");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Naver-Client-Id", clientId);
        headers.set("X-Naver-Client-Secret", clientSecret);

        try {
            return restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/search/news.json")
                            .queryParam("query", query)
                            .queryParam("display", display)
                            .queryParam("sort", sort)
                            .build()
                    )
                    .headers(httpHeaders -> httpHeaders.addAll(headers))
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (req, res) -> {
                        throw new RestClientResponseException(
                                "Naver API error: " + res.getStatusCode(),
                                res.getStatusCode().value(),
                                res.getStatusText(),
                                res.getHeaders(),
                                null,
                                null
                        );
                    })
                    .body(String.class);
        } catch (RestClientResponseException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("네이버 뉴스 API 호출 중 오류가 발생했습니다.", e);
        }
    }
}

