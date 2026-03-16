package com.vibe.sports.news.repository;

import com.vibe.sports.news.domain.NewsIssueSummary;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface NewsIssueSummaryRepository extends JpaRepository<NewsIssueSummary, Long> {

    Optional<NewsIssueSummary> findFirstByCategoryAndKeywordOrderByGeneratedAtDesc(
            String category,
            String keyword
    );
}

