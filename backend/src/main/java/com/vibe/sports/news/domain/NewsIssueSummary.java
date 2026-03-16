package com.vibe.sports.news.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

@Entity
@Table(name = "news_issue_summary")
@Getter
@Setter
@NoArgsConstructor
public class NewsIssueSummary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String category;

    @Column(nullable = false, length = 100)
    private String keyword;

    @Column(name = "from_datetime", nullable = false)
    private OffsetDateTime fromDatetime;

    @Column(name = "to_datetime", nullable = false)
    private OffsetDateTime toDatetime;

    @Column(name = "summary_title", columnDefinition = "text")
    private String summaryTitle;

    @Column(name = "summary_text", nullable = false, columnDefinition = "text")
    private String summaryText;

    @Column(name = "related_articles", nullable = false, columnDefinition = "text")
    private String relatedArticles;

    @Column(name = "generated_at", nullable = false)
    private OffsetDateTime generatedAt = OffsetDateTime.now();
}

