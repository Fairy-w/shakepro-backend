ALTER TABLE materials
    ADD COLUMN name_en VARCHAR(120) NULL COMMENT 'ingredient english name',
    ADD COLUMN image_url VARCHAR(512) NULL COMMENT 'ingredient image url (oss preferred)',
    ADD COLUMN source VARCHAR(32) NULL COMMENT 'data source',
    ADD COLUMN source_id VARCHAR(64) NULL COMMENT 'source primary id';

CREATE INDEX idx_materials_source ON materials(source);
CREATE INDEX idx_materials_source_id ON materials(source_id);
CREATE INDEX idx_materials_name_en ON materials(name_en);
