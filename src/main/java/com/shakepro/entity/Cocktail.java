package com.shakepro.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "cocktails")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Cocktail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "english_name", length = 255)
    private String englishName;

    @Column(length = 100)
    private String category;

    @Column(name = "hero_image", length = 500)
    private String heroImage;

    @Column(length = 50)
    private String difficulty;

    @Column(length = 32)
    private String abv;

    @Column(length = 100)
    private String glass;

    @Column(length = 255)
    private String garnish;

    @Column(columnDefinition = "TEXT")
    private String highlight;

    @Column(columnDefinition = "TEXT")
    private String subtitle;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String story;

    @Column(name = "source_url", length = 768)
    private String sourceUrl;

    @Column(name = "image_url", length = 255)
    private String imageUrl;

    @Column(name = "alcohol_level")
    private Integer alcoholLevel;

    @Column(columnDefinition = "TEXT")
    private String steps;

    @OneToMany(mappedBy = "cocktail", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<CocktailMaterial> cocktailMaterials = new ArrayList<>();

    @OneToMany(mappedBy = "cocktail", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("stepOrder ASC")
    @Builder.Default
    private List<CocktailStep> stepItems = new ArrayList<>();

    @OneToMany(mappedBy = "cocktail", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("sortOrder ASC")
    @Builder.Default
    private List<CocktailFlavorTag> flavorTags = new ArrayList<>();

    @OneToMany(mappedBy = "cocktail", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("sortOrder ASC")
    @Builder.Default
    private List<CocktailFlavorMetric> flavorMetrics = new ArrayList<>();

    @OneToMany(mappedBy = "cocktail", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("sortOrder ASC")
    @Builder.Default
    private List<CocktailPairing> pairings = new ArrayList<>();

    @OneToMany(mappedBy = "cocktail", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("sortOrder ASC")
    @Builder.Default
    private List<CocktailServiceNote> serviceNotes = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
