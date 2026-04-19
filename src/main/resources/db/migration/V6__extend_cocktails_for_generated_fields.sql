ALTER TABLE `cocktails`
    ADD COLUMN `english_name` VARCHAR(255) DEFAULT NULL COMMENT '英文名' AFTER `name`,
    ADD COLUMN `category` VARCHAR(100) DEFAULT NULL COMMENT '分类' AFTER `english_name`,
    ADD COLUMN `hero_image` VARCHAR(500) DEFAULT NULL COMMENT '主图' AFTER `category`,
    ADD COLUMN `difficulty` VARCHAR(50) DEFAULT NULL COMMENT '难度' AFTER `hero_image`,
    ADD COLUMN `abv` VARCHAR(32) DEFAULT NULL COMMENT '酒精度展示值，如 22%' AFTER `difficulty`,
    ADD COLUMN `glass` VARCHAR(100) DEFAULT NULL COMMENT '杯型' AFTER `abv`,
    ADD COLUMN `garnish` VARCHAR(255) DEFAULT NULL COMMENT '装饰' AFTER `glass`,
    ADD COLUMN `highlight` TEXT DEFAULT NULL COMMENT '亮点文案' AFTER `garnish`,
    ADD COLUMN `subtitle` TEXT DEFAULT NULL COMMENT '副标题' AFTER `highlight`,
    ADD COLUMN `story` TEXT DEFAULT NULL COMMENT '故事' AFTER `description`;

CREATE INDEX `idx_cocktails_category` ON `cocktails` (`category`);
CREATE INDEX `idx_cocktails_difficulty` ON `cocktails` (`difficulty`);
