CREATE TABLE IF NOT EXISTS `cocktail_steps` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `cocktail_id` BIGINT NOT NULL,
    `step_order` INT NOT NULL COMMENT '步骤顺序，从 1 开始',
    `title` VARCHAR(100) DEFAULT NULL COMMENT '步骤标题',
    `detail` TEXT NOT NULL COMMENT '步骤详情',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_cocktail_steps_order` (`cocktail_id`, `step_order`),
    KEY `idx_cocktail_steps_cocktail_id` (`cocktail_id`),
    CONSTRAINT `fk_cocktail_steps_cocktail`
        FOREIGN KEY (`cocktail_id`) REFERENCES `cocktails`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
