-- 模块3：运营数据原始五表入库方案（MySQL 5.7）
-- 适用场景：
-- 1) 最终“流量数据统计报告”并不是原始数据，而是由五张原始表汇总生成
-- 2) 当前应优先把五张原始表稳定入库，再做汇总口径与前端接口

CREATE DATABASE IF NOT EXISTS ecommerce_ops
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE ecommerce_ops;

CREATE TABLE IF NOT EXISTS op_import_batch (
  id BIGINT NOT NULL AUTO_INCREMENT,
  batch_code VARCHAR(64) NOT NULL,
  source_dir VARCHAR(255) DEFAULT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
  remark VARCHAR(500) DEFAULT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_batch_code (batch_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='导入批次';

CREATE TABLE IF NOT EXISTS op_source_file (
  id BIGINT NOT NULL AUTO_INCREMENT,
  import_batch_id BIGINT DEFAULT NULL,
  shop_name VARCHAR(100) DEFAULT NULL COMMENT '店铺，如 EXPERLAM / KHINYA',
  market_name VARCHAR(50) DEFAULT NULL COMMENT '市场，如 美国',
  stat_date DATE DEFAULT NULL COMMENT '数据日期',
  source_type VARCHAR(50) NOT NULL COMMENT 'BUSINESS_PARENT/BUSINESS_CHILD/SP_AD/SBV_SUMMARY/ASIN_MAPPING/FINAL_REPORT',
  source_filename VARCHAR(255) NOT NULL,
  source_path VARCHAR(500) NOT NULL,
  file_md5 VARCHAR(64) DEFAULT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'IMPORTED',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_source_path (source_path(255)),
  KEY idx_stat_date (stat_date),
  KEY idx_source_type (source_type),
  KEY idx_shop_date (shop_name, stat_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='源文件登记';

CREATE TABLE IF NOT EXISTS op_asin_mapping (
  id BIGINT NOT NULL AUTO_INCREMENT,
  source_file_id BIGINT DEFAULT NULL,
  parent_asin VARCHAR(32) NOT NULL,
  parent_product_name VARCHAR(255) DEFAULT NULL,
  child_asin VARCHAR(32) NOT NULL,
  child_product_name VARCHAR(255) DEFAULT NULL,
  has_sbv TINYINT DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_parent_child (parent_asin, child_asin),
  KEY idx_parent_asin (parent_asin),
  KEY idx_child_asin (child_asin)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='ASIN映射表';

CREATE TABLE IF NOT EXISTS op_business_parent_daily (
  id BIGINT NOT NULL AUTO_INCREMENT,
  source_file_id BIGINT NOT NULL,
  shop_name VARCHAR(100) DEFAULT NULL,
  market_name VARCHAR(50) DEFAULT NULL,
  stat_date DATE NOT NULL,
  parent_asin VARCHAR(32) NOT NULL,
  product_name VARCHAR(500) DEFAULT NULL,
  sessions_total INT DEFAULT NULL,
  sessions_b2b INT DEFAULT NULL,
  ordered_product_sales_total DECIMAL(18,2) DEFAULT NULL,
  ordered_product_sales_b2b DECIMAL(18,2) DEFAULT NULL,
  total_order_items INT DEFAULT NULL,
  total_order_items_b2b INT DEFAULT NULL,
  total_units_ordered INT DEFAULT NULL,
  total_units_ordered_b2b INT DEFAULT NULL,
  unit_session_percentage DECIMAL(10,4) DEFAULT NULL,
  unit_session_percentage_b2b DECIMAL(10,4) DEFAULT NULL,
  page_views_total INT DEFAULT NULL,
  page_views_b2b INT DEFAULT NULL,
  page_views_percentage_total DECIMAL(10,4) DEFAULT NULL,
  page_views_percentage_b2b DECIMAL(10,4) DEFAULT NULL,
  buy_box_percentage DECIMAL(10,4) DEFAULT NULL,
  buy_box_percentage_b2b DECIMAL(10,4) DEFAULT NULL,
  raw_json JSON DEFAULT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_parent_asin_date (parent_asin, stat_date),
  KEY idx_shop_date (shop_name, stat_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='业务报告-父ASIN-日数据';

CREATE TABLE IF NOT EXISTS op_business_child_daily (
  id BIGINT NOT NULL AUTO_INCREMENT,
  source_file_id BIGINT NOT NULL,
  shop_name VARCHAR(100) DEFAULT NULL,
  market_name VARCHAR(50) DEFAULT NULL,
  stat_date DATE NOT NULL,
  parent_asin VARCHAR(32) DEFAULT NULL,
  child_asin VARCHAR(32) NOT NULL,
  product_name VARCHAR(500) DEFAULT NULL,
  sessions_total INT DEFAULT NULL,
  sessions_b2b INT DEFAULT NULL,
  ordered_product_sales_total DECIMAL(18,2) DEFAULT NULL,
  ordered_product_sales_b2b DECIMAL(18,2) DEFAULT NULL,
  total_order_items INT DEFAULT NULL,
  total_order_items_b2b INT DEFAULT NULL,
  total_units_ordered INT DEFAULT NULL,
  total_units_ordered_b2b INT DEFAULT NULL,
  unit_session_percentage DECIMAL(10,4) DEFAULT NULL,
  unit_session_percentage_b2b DECIMAL(10,4) DEFAULT NULL,
  page_views_total INT DEFAULT NULL,
  page_views_b2b INT DEFAULT NULL,
  page_views_percentage_total DECIMAL(10,4) DEFAULT NULL,
  page_views_percentage_b2b DECIMAL(10,4) DEFAULT NULL,
  buy_box_percentage DECIMAL(10,4) DEFAULT NULL,
  buy_box_percentage_b2b DECIMAL(10,4) DEFAULT NULL,
  raw_json JSON DEFAULT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_child_asin_date (child_asin, stat_date),
  KEY idx_parent_asin_date (parent_asin, stat_date),
  KEY idx_shop_date (shop_name, stat_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='业务报告-子ASIN-日数据';

CREATE TABLE IF NOT EXISTS op_sp_ad_daily (
  id BIGINT NOT NULL AUTO_INCREMENT,
  source_file_id BIGINT NOT NULL,
  shop_name VARCHAR(100) DEFAULT NULL,
  market_name VARCHAR(50) DEFAULT NULL,
  stat_date DATE NOT NULL,
  child_asin VARCHAR(32) DEFAULT NULL,
  sku VARCHAR(64) DEFAULT NULL,
  campaign_name VARCHAR(255) DEFAULT NULL,
  ad_group_name VARCHAR(255) DEFAULT NULL,
  target_text VARCHAR(255) DEFAULT NULL,
  impressions INT DEFAULT NULL,
  clicks INT DEFAULT NULL,
  ctr DECIMAL(10,6) DEFAULT NULL,
  cpc DECIMAL(18,4) DEFAULT NULL,
  spend DECIMAL(18,2) DEFAULT NULL,
  sales_7d DECIMAL(18,2) DEFAULT NULL,
  acos DECIMAL(10,6) DEFAULT NULL,
  roas DECIMAL(18,6) DEFAULT NULL,
  orders_7d INT DEFAULT NULL,
  units_7d INT DEFAULT NULL,
  cvr_7d DECIMAL(10,6) DEFAULT NULL,
  advertised_sku_units_7d INT DEFAULT NULL,
  other_sku_units_7d INT DEFAULT NULL,
  advertised_sku_sales_7d DECIMAL(18,2) DEFAULT NULL,
  other_sku_sales_7d DECIMAL(18,2) DEFAULT NULL,
  raw_json JSON DEFAULT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_child_asin_date (child_asin, stat_date),
  KEY idx_sku_date (sku, stat_date),
  KEY idx_shop_date (shop_name, stat_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='SP广告报告-明细';

CREATE TABLE IF NOT EXISTS op_sbv_daily (
  id BIGINT NOT NULL AUTO_INCREMENT,
  source_file_id BIGINT NOT NULL,
  shop_name VARCHAR(100) DEFAULT NULL,
  market_name VARCHAR(50) DEFAULT NULL,
  stat_date DATE NOT NULL,
  child_asin VARCHAR(32) NOT NULL,
  impressions INT DEFAULT NULL,
  clicks INT DEFAULT NULL,
  ctr DECIMAL(10,4) DEFAULT NULL,
  cpc DECIMAL(18,4) DEFAULT NULL,
  spend DECIMAL(18,2) DEFAULT NULL,
  sales DECIMAL(18,2) DEFAULT NULL,
  acos DECIMAL(10,4) DEFAULT NULL,
  ad_orders INT DEFAULT NULL,
  cvr DECIMAL(10,4) DEFAULT NULL,
  raw_json JSON DEFAULT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_child_asin_date (child_asin, stat_date),
  KEY idx_shop_date (shop_name, stat_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='SBV汇总-子ASIN-日数据';

-- 后续给前端模块3使用的“汇总表/视图”建议单独创建，不要把原始表直接暴露给前端。
