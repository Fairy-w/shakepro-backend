ALTER TABLE `crawl_batch_import_histories`
    ADD COLUMN `remaining_unimported_count` INT NOT NULL DEFAULT 0 AFTER `failure_count`;
