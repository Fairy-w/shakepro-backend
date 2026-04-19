ALTER TABLE `cocktail_materials`
    MODIFY COLUMN `material_id` BIGINT NULL,
    ADD COLUMN `display_name` VARCHAR(255) DEFAULT NULL COMMENT '展示名称/原始材料名' AFTER `material_id`,
    ADD COLUMN `note` VARCHAR(500) DEFAULT NULL COMMENT '材料备注' AFTER `amount`,
    ADD COLUMN `sort_order` INT NOT NULL DEFAULT 0 COMMENT '材料顺序' AFTER `note`;

CREATE INDEX `idx_cocktail_materials_sort_order` ON `cocktail_materials` (`cocktail_id`, `sort_order`);
