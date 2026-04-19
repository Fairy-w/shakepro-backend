package com.shakepro.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "cocktail_steps", indexes = {
        @Index(name = "idx_cocktail_steps_cocktail_id", columnList = "cocktail_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CocktailStep {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cocktail_id", nullable = false)
    private Cocktail cocktail;

    @Column(name = "step_order", nullable = false)
    private Integer stepOrder;

    @Column(length = 100)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String detail;
}
