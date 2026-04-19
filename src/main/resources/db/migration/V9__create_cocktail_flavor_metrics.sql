CREATE TABLE IF NOT EXISTS `cocktail_flavor_metrics` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `cocktail_id` BIGINT NOT NULL,
    `metric_name` VARCHAR(100) NOT NULL COMMENT '风味指标名，如 酸感/甜感',
    `metric_value` INT NOT NULL COMMENT '风味指标值',
    `sort_order` INT NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_cocktail_flavor_metrics_order` (`cocktail_id`, `sort_order`),
    KEY `idx_cocktail_flavor_metrics_cocktail_id` (`cocktail_id`),
    CONSTRAINT `fk_cocktail_flavor_metrics_cocktail`
        FOREIGN KEY (`cocktail_id`) REFERENCES `cocktails`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
