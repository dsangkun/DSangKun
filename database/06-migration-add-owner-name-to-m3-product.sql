-- 模块3迁移：给产品主表补充归属人字段（MySQL 5.7）

SET @ddl := (
  SELECT IF(
    EXISTS (
      SELECT 1
        FROM information_schema.COLUMNS
       WHERE TABLE_SCHEMA = DATABASE()
         AND TABLE_NAME = 'm3_product'
         AND COLUMN_NAME = 'owner_name'
    ),
    'SELECT ''owner_name already exists''',
    'ALTER TABLE m3_product ADD COLUMN owner_name VARCHAR(64) DEFAULT NULL COMMENT ''归属人/负责人'' AFTER product_name'
  )
);

PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
