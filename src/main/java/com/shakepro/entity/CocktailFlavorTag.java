package com.shakepro.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "cocktail_flavor_tags", indexes = {
        @Index(name = "idx_cocktail_flavor_tags_cocktail_id", columnList = "cocktail_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CocktailFlavorTag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cocktail_id", nullable = false)
    private Cocktail cocktail;

    @Column(nullable = false, length = 100)
    private String tag;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;
}
