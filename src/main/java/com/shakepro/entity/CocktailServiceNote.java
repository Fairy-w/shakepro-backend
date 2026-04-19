package com.shakepro.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "cocktail_service_notes", indexes = {
        @Index(name = "idx_cocktail_service_notes_cocktail_id", columnList = "cocktail_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CocktailServiceNote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cocktail_id", nullable = false)
    private Cocktail cocktail;

    @Column(nullable = false, length = 500)
    private String note;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;
}
