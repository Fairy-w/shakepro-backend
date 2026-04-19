package com.shakepro.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "cocktail_pairings", indexes = {
        @Index(name = "idx_cocktail_pairings_cocktail_id", columnList = "cocktail_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CocktailPairing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cocktail_id", nullable = false)
    private Cocktail cocktail;

    @Column(nullable = false, length = 255)
    private String pairing;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;
}
