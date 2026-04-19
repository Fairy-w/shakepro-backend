package com.shakepro.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "cocktail_materials", indexes = {
        @Index(name = "idx_cocktail_id", columnList = "cocktail_id"),
        @Index(name = "idx_material_id", columnList = "material_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CocktailMaterial {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cocktail_id", nullable = false)
    private Cocktail cocktail;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "material_id")
    private Material material;

    @Column(name = "display_name", length = 255)
    private String displayName;

    @Column(length = 50)
    private String amount;

    @Column(length = 500)
    private String note;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;
}
