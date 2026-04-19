package com.shakepro.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "cocktail_flavor_metrics", indexes = {
        @Index(name = "idx_cocktail_flavor_metrics_cocktail_id", columnList = "cocktail_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CocktailFlavorMetric {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cocktail_id", nullable = false)
    private Cocktail cocktail;

    @Column(name = "metric_name", nullable = false, length = 100)
    private String metricName;

    @Column(name = "metric_value", nullable = false)
    private Integer metricValue;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;
}
