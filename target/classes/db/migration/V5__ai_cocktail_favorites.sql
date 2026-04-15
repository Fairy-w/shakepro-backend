CREATE TABLE IF NOT EXISTS `favorite_ai_cocktails` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `recipe_key` VARCHAR(128) NOT NULL,
    `name` VARCHAR(100) NOT NULL,
    `description` TEXT DEFAULT NULL,
    `materials_json` TEXT NOT NULL,
    `steps_json` TEXT NOT NULL,
    `prompt` TEXT DEFAULT NULL,
    `source` VARCHAR(32) NOT NULL DEFAULT 'ai',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_favorite_ai_cocktail_user_recipe` (`user_id`, `recipe_key`),
    KEY `idx_favorite_ai_cocktail_user_created_at` (`user_id`, `created_at`),
    CONSTRAINT `fk_favorite_ai_cocktail_user`
        FOREIGN KEY (`user_id`) REFERENCES `users`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
