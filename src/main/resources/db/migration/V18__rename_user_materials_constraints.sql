ALTER TABLE `user_materials`
    DROP FOREIGN KEY `fk_user_material_inventory_user`,
    DROP FOREIGN KEY `fk_user_material_inventory_material`;

ALTER TABLE `user_materials`
    DROP INDEX `uk_user_material_inventory_user_barcode`,
    DROP INDEX `idx_user_material_inventory_user_updated_at`,
    DROP INDEX `idx_user_material_inventory_material_id`;

ALTER TABLE `user_materials`
    ADD UNIQUE KEY `uk_user_materials_user_barcode` (`user_id`, `barcode`),
    ADD KEY `idx_user_materials_user_updated_at` (`user_id`, `updated_at`),
    ADD KEY `idx_user_materials_material_id` (`material_id`),
    ADD CONSTRAINT `fk_user_materials_user`
        FOREIGN KEY (`user_id`) REFERENCES `users`(`id`) ON DELETE CASCADE,
    ADD CONSTRAINT `fk_user_materials_material`
        FOREIGN KEY (`material_id`) REFERENCES `materials`(`id`) ON DELETE SET NULL;
