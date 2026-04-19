SET @source_url_exists := (
    SELECT COUNT(1)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'cocktails'
      AND COLUMN_NAME = 'source_url'
);
SET @source_url_sql := IF(
    @source_url_exists = 0,
    'ALTER TABLE `cocktails` ADD COLUMN `source_url` VARCHAR(768) DEFAULT NULL COMMENT ''来源详情页 URL'' AFTER `story`',
    'ALTER TABLE `cocktails` MODIFY COLUMN `source_url` VARCHAR(768) DEFAULT NULL COMMENT ''来源详情页 URL'' AFTER `story`'
);
PREPARE stmt_source_url FROM @source_url_sql;
EXECUTE stmt_source_url;
DEALLOCATE PREPARE stmt_source_url;

SET @source_url_idx_exists := (
    SELECT COUNT(1)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'cocktails'
      AND INDEX_NAME = 'uk_cocktails_source_url'
);
SET @drop_source_url_idx_sql := IF(
    @source_url_idx_exists = 0,
    'SELECT 1',
    'DROP INDEX `uk_cocktails_source_url` ON `cocktails`'
);
PREPARE stmt_drop_source_url_idx FROM @drop_source_url_idx_sql;
EXECUTE stmt_drop_source_url_idx;
DEALLOCATE PREPARE stmt_drop_source_url_idx;

CREATE UNIQUE INDEX `uk_cocktails_source_url` ON `cocktails` (`source_url`);

CREATE TABLE IF NOT EXISTS `crawl_import_records` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `detail_url` VARCHAR(768) NOT NULL,
    `list_url` VARCHAR(1000) DEFAULT NULL,
    `status` VARCHAR(32) NOT NULL,
    `saved_cocktail_id` BIGINT DEFAULT NULL,
    `error_message` TEXT DEFAULT NULL,
    `last_crawled_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_crawl_import_records_detail_url` (`detail_url`),
    KEY `idx_crawl_import_records_status_last_crawled_at` (`status`, `last_crawled_at`),
    KEY `idx_crawl_import_records_saved_cocktail_id` (`saved_cocktail_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
