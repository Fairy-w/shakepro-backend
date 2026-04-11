UPDATE `users`
SET `password_hash` = '$2a$10$gWye2Avgn2a0Y4dfQlEKiOEbdVmwcotDNNpMogaskXtkTFP4SP4hC',
    `role` = 'ADMIN',
    `enabled` = 1,
    `nickname` = COALESCE(`nickname`, 'System Admin')
WHERE `username` = 'admin';

INSERT INTO `users` (`username`, `password_hash`, `nickname`, `role`, `enabled`)
SELECT 'admin', '$2a$10$gWye2Avgn2a0Y4dfQlEKiOEbdVmwcotDNNpMogaskXtkTFP4SP4hC', 'System Admin', 'ADMIN', 1
WHERE NOT EXISTS (
    SELECT 1 FROM `users` WHERE `username` = 'admin'
);
