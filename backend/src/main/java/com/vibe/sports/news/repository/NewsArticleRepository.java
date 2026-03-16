package com.vibe.sports.news.repository;

import com.vibe.sports.news.domain.NewsArticle;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;

public interface NewsArticleRepository extends JpaRepository<NewsArticle, Long> {

    List<NewsArticle> findByCategoryAndPubDateBetweenOrderByPubDateDesc(
            String category,
            OffsetDateTime from,
            OffsetDateTime to
    );

    long deleteByPubDateBefore(OffsetDateTime cutoff);
}

