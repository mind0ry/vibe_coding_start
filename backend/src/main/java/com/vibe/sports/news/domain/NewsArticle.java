package com.vibe.sports.news.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

@Entity
@Table(name = "news_article")
@Getter
@Setter
@NoArgsConstructor
public class NewsArticle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String category; // 예: SOCCER, BASEBALL, BASKETBALL, VOLLEYBALL

    @Column(nullable = false, length = 100)
    private String keyword; // 예: "축구"

    @Column(nullable = false, columnDefinition = "text")
    private String title;

    @Column(columnDefinition = "text")
    private String description;

    @Column(columnDefinition = "text")
    private String originallink;

    @Column(nullable = false, columnDefinition = "text")
    private String link;

    @Column(name = "pub_date", nullable = false)
    private OffsetDateTime pubDate;

    @Column(name = "raw_json", columnDefinition = "text")
    private String rawJson;

    @Column(name = "collected_at", nullable = false)
    private OffsetDateTime collectedAt = OffsetDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }
}

