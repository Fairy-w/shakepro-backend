RENAME TABLE `user_material_inventory` TO `user_materials`;

ALTER TABLE `user_materials`
    DROP COLUMN `product_key`,
    DROP COLUMN `source_label`,
    DROP COLUMN `subtitle`,
    DROP COLUMN `note`,
    DROP COLUMN `badge`,
    DROP COLUMN `accent_color`,
    DROP COLUMN `soft_color`;
