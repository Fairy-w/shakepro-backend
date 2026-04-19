CREATE TABLE IF NOT EXISTS `cocktail_flavor_tags` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `cocktail_id` BIGINT NOT NULL,
    `tag` VARCHAR(100) NOT NULL,
    `sort_order` INT NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_cocktail_flavor_tags_order` (`cocktail_id`, `sort_order`),
    KEY `idx_cocktail_flavor_tags_cocktail_id` (`cocktail_id`),
    CONSTRAINT `fk_cocktail_flavor_tags_cocktail`
        FOREIGN KEY (`cocktail_id`) REFERENCES `cocktails`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `cocktail_pairings` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `cocktail_id` BIGINT NOT NULL,
    `pairing` VARCHAR(255) NOT NULL,
    `sort_order` INT NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_cocktail_pairings_order` (`cocktail_id`, `sort_order`),
    KEY `idx_cocktail_pairings_cocktail_id` (`cocktail_id`),
    CONSTRAINT `fk_cocktail_pairings_cocktail`
        FOREIGN KEY (`cocktail_id`) REFERENCES `cocktails`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `cocktail_service_notes` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `cocktail_id` BIGINT NOT NULL,
    `note` VARCHAR(500) NOT NULL,
    `sort_order` INT NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_cocktail_service_notes_order` (`cocktail_id`, `sort_order`),
    KEY `idx_cocktail_service_notes_cocktail_id` (`cocktail_id`),
    CONSTRAINT `fk_cocktail_service_notes_cocktail`
        FOREIGN KEY (`cocktail_id`) REFERENCES `cocktails`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
