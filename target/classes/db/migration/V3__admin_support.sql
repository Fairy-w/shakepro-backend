ALTER TABLE `users`
    ADD COLUMN `role` VARCHAR(20) NOT NULL DEFAULT 'USER' AFTER `avatar_url`,
    ADD COLUMN `enabled` TINYINT(1) NOT NULL DEFAULT 1 AFTER `role`;

INSERT INTO `users` (`username`, `password_hash`, `nickname`, `role`, `enabled`)
SELECT 'admin', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'System Admin', 'ADMIN', 1
WHERE NOT EXISTS (
    SELECT 1 FROM `users` WHERE `username` = 'admin'
);
