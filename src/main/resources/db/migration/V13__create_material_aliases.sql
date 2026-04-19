CREATE TABLE IF NOT EXISTS `material_aliases` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `material_id` BIGINT NOT NULL,
    `alias` VARCHAR(120) NOT NULL COMMENT '原始别名',
    `alias_normalized` VARCHAR(120) NOT NULL COMMENT '标准化别名，用于匹配',
    `priority` INT NOT NULL DEFAULT 100 COMMENT '值越小优先级越高',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_material_aliases_alias_normalized` (`alias_normalized`),
    KEY `idx_material_aliases_material_id` (`material_id`),
    CONSTRAINT `fk_material_aliases_material`
        FOREIGN KEY (`material_id`) REFERENCES `materials`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT IGNORE INTO `material_aliases` (`material_id`, `alias`, `alias_normalized`, `priority`)
SELECT m.`id`,
       m.`name`,
       LOWER(REPLACE(REPLACE(REPLACE(m.`name`, ' ', ''), '-', ''), '_', '')),
       10
FROM `materials` m;

INSERT IGNORE INTO `material_aliases` (`material_id`, `alias`, `alias_normalized`, `priority`)
SELECT m.`id`, 'gin', 'gin', 1 FROM `materials` m WHERE m.`name` = '金酒' LIMIT 1;

INSERT IGNORE INTO `material_aliases` (`material_id`, `alias`, `alias_normalized`, `priority`)
SELECT m.`id`, 'vodka', 'vodka', 1 FROM `materials` m WHERE m.`name` = '伏特加' LIMIT 1;

INSERT IGNORE INTO `material_aliases` (`material_id`, `alias`, `alias_normalized`, `priority`)
SELECT m.`id`, 'tequila', 'tequila', 1 FROM `materials` m WHERE m.`name` = '龙舌兰酒' LIMIT 1;

INSERT IGNORE INTO `material_aliases` (`material_id`, `alias`, `alias_normalized`, `priority`)
SELECT m.`id`, 'rum', 'rum', 1 FROM `materials` m WHERE m.`name` = '白朗姆酒' LIMIT 1;

INSERT IGNORE INTO `material_aliases` (`material_id`, `alias`, `alias_normalized`, `priority`)
SELECT m.`id`, 'whisky', 'whisky', 1 FROM `materials` m WHERE m.`name` = '威士忌' LIMIT 1;
