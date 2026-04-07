-- 模块3：运营数据导入库初始化脚本（MySQL 5.7）
-- 设计原则：
-- 1) 先把 Excel 半结构化数据稳定落库，再做前端接口需要的聚合视图/汇总表
-- 2) 不强依赖 ASIN；当前源文件中可稳定提取的是“日期 + sheet产品名 + 指标块 + 指标值”
-- 3) 保留原始文本值，便于回溯；同时拆出数值与百分比，便于后续聚合分析

CREATE DATABASE IF NOT EXISTS ecommerce_ops
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE ecommerce_ops;

CREATE TABLE IF NOT EXISTS op_import_batch (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '导入批次ID',
  batch_code VARCHAR(64) NOT NULL COMMENT '批次编码',
  source_dir VARCHAR(255) DEFAULT NULL COMMENT '源目录',
  status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/RUNNING/SUCCESS/FAILED',
  remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_batch_code (batch_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='运营数据导入批次';

CREATE TABLE IF NOT EXISTS op_report_file (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '报表文件ID',
  import_batch_id BIGINT DEFAULT NULL COMMENT '导入批次ID',
  shop_name VARCHAR(100) NOT NULL COMMENT '店铺名',
  market_name VARCHAR(50) DEFAULT NULL COMMENT '市场，如美国',
  report_date DATE NOT NULL COMMENT '报表日期',
  source_filename VARCHAR(255) NOT NULL COMMENT '源文件名',
  source_path VARCHAR(500) DEFAULT NULL COMMENT '源文件完整路径',
  file_md5 VARCHAR(64) DEFAULT NULL COMMENT '文件MD5',
  status VARCHAR(20) NOT NULL DEFAULT 'IMPORTED' COMMENT 'IMPORTED/SKIPPED/FAILED',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_source_filename (source_filename),
  KEY idx_report_date (report_date),
  KEY idx_import_batch_id (import_batch_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='运营原始报表文件';

CREATE TABLE IF NOT EXISTS op_product_sheet (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '产品sheet记录ID',
  report_file_id BIGINT NOT NULL COMMENT '报表文件ID',
  sheet_name VARCHAR(255) NOT NULL COMMENT 'sheet名称，当前视为产品名',
  product_name VARCHAR(255) NOT NULL COMMENT '产品名称，默认与sheet名称一致',
  parent_asin VARCHAR(32) DEFAULT NULL COMMENT '父ASIN，当前允许为空',
  parent_sku VARCHAR(64) DEFAULT NULL COMMENT '父SKU，当前允许为空',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_report_sheet (report_file_id, sheet_name),
  KEY idx_product_name (product_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='报表中的产品sheet';

CREATE TABLE IF NOT EXISTS op_metric_daily (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '日指标事实记录ID',
  report_file_id BIGINT NOT NULL COMMENT '报表文件ID',
  product_sheet_id BIGINT NOT NULL COMMENT '产品sheet记录ID',
  stat_date DATE NOT NULL COMMENT '统计日期',
  entity_type VARCHAR(32) NOT NULL COMMENT 'PARENT_SUMMARY/CHILD_VARIANT/CHILD_SBV/UNKNOWN',
  entity_name VARCHAR(255) NOT NULL COMMENT '指标块名称，如父体名、子体名、子体SBV块名',
  metric_name VARCHAR(100) NOT NULL COMMENT '原始指标名',
  metric_value_raw VARCHAR(100) DEFAULT NULL COMMENT '原始值文本',
  metric_value_num DECIMAL(18,4) DEFAULT NULL COMMENT '数值型值，百分比按原数字保存，如15.11% -> 15.11',
  metric_unit VARCHAR(20) DEFAULT NULL COMMENT 'NUMBER/PERCENT/TEXT',
  row_no INT DEFAULT NULL COMMENT 'Excel原始行号',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_metric_unique (report_file_id, product_sheet_id, stat_date, entity_type, entity_name, metric_name, row_no),
  KEY idx_stat_date (stat_date),
  KEY idx_product_sheet_id (product_sheet_id),
  KEY idx_metric_name (metric_name),
  KEY idx_entity_type (entity_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='运营日指标明细事实表';

-- 后续如果确认了 ASIN / SKU 映射关系，可新增维表并回填：
-- product_dim(id, shop_name, market_name, product_name, parent_asin, child_asin, sku, ...)
-- 当前阶段先保证“稳定导入 + 可追溯 + 可聚合”。
