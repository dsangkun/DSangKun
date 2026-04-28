-- 模块3：运营数据展示数据库（MySQL 5.7.44 兼容）
-- 项目：Amazon运营推进器
-- 使用方式：先在目标库中执行 `USE your_database_name;`，再执行本脚本。
-- 设计原则：
-- 1) 保留原始导入层（Excel/报表落库），原始层保留店铺维度
-- 2) 提供产品维度汇总层，直接服务前端展示
-- 3) 当前前端主展示维度为子ASIN：一个卡片 = 一个子ASIN = 一个具体产品
-- 4) 父ASIN保留用于汇总、归属、对账校验，不作为前端卡片直接展示对象
-- 5) 仅覆盖模块3：产品基础、评价、销售、流量、广告

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS m3_product (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  product_code VARCHAR(64) NOT NULL COMMENT '内部产品编码，首版直接使用子ASIN',
  product_name VARCHAR(255) NOT NULL COMMENT '产品名称（子ASIN产品名）',
  owner_name VARCHAR(64) DEFAULT NULL COMMENT '归属人/负责人',
  brand_name VARCHAR(128) DEFAULT NULL COMMENT '品牌名',
  marketplace VARCHAR(32) NOT NULL DEFAULT 'US' COMMENT '站点/市场，例如 US/BR',
  shop_name VARCHAR(128) DEFAULT NULL COMMENT '店铺名（辅助追溯，非前端主维度）',
  primary_asin VARCHAR(32) DEFAULT NULL COMMENT '主展示子ASIN',
  image_url VARCHAR(512) DEFAULT NULL COMMENT '产品图片URL',
  product_status TINYINT NOT NULL DEFAULT 1 COMMENT '1启用 0停用',
  remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_m3_product_code (product_code),
  KEY idx_m3_product_marketplace_shop (marketplace, shop_name),
  KEY idx_m3_product_primary_asin (primary_asin)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='模块3-产品主表（前端卡片主对象=子ASIN产品）';

CREATE TABLE IF NOT EXISTS m3_product_asin_map (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  product_id BIGINT UNSIGNED NOT NULL COMMENT '产品ID',
  asin VARCHAR(32) NOT NULL COMMENT 'ASIN',
  asin_role VARCHAR(16) NOT NULL DEFAULT 'child' COMMENT '映射角色：parent/child/ad',
  asin_name VARCHAR(255) DEFAULT NULL COMMENT 'ASIN产品名',
  marketplace VARCHAR(32) NOT NULL DEFAULT 'US' COMMENT '站点/市场',
  shop_name VARCHAR(128) DEFAULT NULL COMMENT '店铺名',
  is_primary TINYINT NOT NULL DEFAULT 0 COMMENT '是否主ASIN 1是 0否',
  runs_sbv TINYINT NOT NULL DEFAULT 0 COMMENT '是否跑SBV 1是 0否',
  start_date DATE DEFAULT NULL COMMENT '生效开始日期',
  end_date DATE DEFAULT NULL COMMENT '生效结束日期',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_m3_product_asin_unique (product_id, asin, marketplace, shop_name, asin_role),
  KEY idx_m3_product_asin_map_product (product_id),
  KEY idx_m3_product_asin_map_asin (asin),
  KEY idx_m3_product_asin_map_role (asin_role),
  CONSTRAINT fk_m3_product_asin_map_product FOREIGN KEY (product_id) REFERENCES m3_product (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='模块3-产品与ASIN映射表';

CREATE TABLE IF NOT EXISTS m3_import_batch (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  batch_no VARCHAR(64) NOT NULL COMMENT '导入批次号',
  batch_type VARCHAR(32) NOT NULL COMMENT '批次类型：mapping/sbv/sp/biz_parent/biz_child/review/summary',
  shop_name VARCHAR(128) DEFAULT NULL COMMENT '店铺名（非SBV数据通常按店铺下载）',
  marketplace VARCHAR(32) DEFAULT NULL COMMENT '站点/市场',
  stat_date DATE DEFAULT NULL COMMENT '统计日期',
  source_dir VARCHAR(500) DEFAULT NULL COMMENT '源目录',
  source_note VARCHAR(500) DEFAULT NULL COMMENT '源说明',
  import_status TINYINT NOT NULL DEFAULT 0 COMMENT '0待处理 1成功 2部分成功 9失败',
  imported_by VARCHAR(64) DEFAULT NULL COMMENT '导入人/脚本',
  started_at DATETIME DEFAULT NULL COMMENT '开始时间',
  finished_at DATETIME DEFAULT NULL COMMENT '结束时间',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_m3_import_batch_no (batch_no),
  KEY idx_m3_import_batch_type_date (batch_type, stat_date),
  KEY idx_m3_import_batch_shop_date (shop_name, stat_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='模块3-导入批次表';

CREATE TABLE IF NOT EXISTS m3_import_file (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  batch_id BIGINT UNSIGNED NOT NULL COMMENT '导入批次ID',
  file_type VARCHAR(32) NOT NULL COMMENT '文件类型：mapping/sbv/sp/biz_parent/biz_child/review/case/final_report',
  file_name VARCHAR(255) NOT NULL COMMENT '文件名',
  file_path VARCHAR(1000) DEFAULT NULL COMMENT '原始路径',
  file_md5 VARCHAR(64) DEFAULT NULL COMMENT '文件MD5',
  file_size_bytes BIGINT UNSIGNED DEFAULT NULL COMMENT '文件大小',
  shop_name VARCHAR(128) DEFAULT NULL COMMENT '店铺名（来源上下文）',
  marketplace VARCHAR(32) DEFAULT NULL COMMENT '站点/市场',
  stat_date DATE DEFAULT NULL COMMENT '统计日期',
  parse_status TINYINT NOT NULL DEFAULT 0 COMMENT '0待处理 1成功 9失败',
  error_message VARCHAR(1000) DEFAULT NULL COMMENT '失败原因',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_m3_import_file_batch (batch_id),
  KEY idx_m3_import_file_type_date (file_type, stat_date),
  KEY idx_m3_import_file_shop_date (shop_name, stat_date),
  CONSTRAINT fk_m3_import_file_batch FOREIGN KEY (batch_id) REFERENCES m3_import_batch (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='模块3-导入文件登记表';

CREATE TABLE IF NOT EXISTS m3_review_daily (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  product_id BIGINT UNSIGNED NOT NULL COMMENT '产品ID（前端卡片对应子ASIN产品）',
  stat_date DATE NOT NULL COMMENT '统计日期',
  rating_avg DECIMAL(4,2) DEFAULT NULL COMMENT '平均评分',
  review_count_total INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '累计评价数',
  review_count_new INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '当日新增评价数',
  qa_count INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '问答数',
  positive_rate DECIMAL(7,4) DEFAULT NULL COMMENT '好评率，例如 0.9234',
  negative_rate DECIMAL(7,4) DEFAULT NULL COMMENT '差评率，例如 0.0234',
  source_file_id BIGINT UNSIGNED DEFAULT NULL COMMENT '来源文件ID',
  batch_id BIGINT UNSIGNED DEFAULT NULL COMMENT '导入批次ID',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_m3_review_daily_product_date (product_id, stat_date),
  KEY idx_m3_review_daily_date (stat_date),
  CONSTRAINT fk_m3_review_daily_product FOREIGN KEY (product_id) REFERENCES m3_product (id),
  CONSTRAINT fk_m3_review_daily_file FOREIGN KEY (source_file_id) REFERENCES m3_import_file (id),
  CONSTRAINT fk_m3_review_daily_batch FOREIGN KEY (batch_id) REFERENCES m3_import_batch (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='模块3-产品评价日报';

CREATE TABLE IF NOT EXISTS m3_biz_report_parent_daily (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  parent_asin VARCHAR(32) NOT NULL COMMENT '父ASIN',
  title VARCHAR(500) DEFAULT NULL COMMENT '标题',
  marketplace VARCHAR(32) NOT NULL DEFAULT 'US' COMMENT '站点/市场',
  shop_name VARCHAR(128) DEFAULT NULL COMMENT '店铺名',
  stat_date DATE NOT NULL COMMENT '统计日期',
  sessions_total INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '会话数 - 总计',
  page_views_total INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '页面浏览量 - 总计',
  conversion_rate_total DECIMAL(10,4) DEFAULT NULL COMMENT '转化率 - 总计，小数存储',
  buy_box_pct DECIMAL(10,4) DEFAULT NULL COMMENT '推荐优惠/推荐展示位百分比，小数存储',
  unit_session_pct DECIMAL(10,4) DEFAULT NULL COMMENT '商品会话百分比，小数存储',
  ordered_product_sales DECIMAL(14,2) NOT NULL DEFAULT 0.00 COMMENT '已订购商品销售额',
  units_ordered INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '已订购商品数量',
  total_order_items INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '订单商品总数',
  source_file_id BIGINT UNSIGNED DEFAULT NULL COMMENT '来源文件ID',
  batch_id BIGINT UNSIGNED DEFAULT NULL COMMENT '导入批次ID',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_m3_biz_parent_asin_date_shop (parent_asin, stat_date, marketplace, shop_name),
  KEY idx_m3_biz_parent_date (stat_date),
  CONSTRAINT fk_m3_biz_parent_file FOREIGN KEY (source_file_id) REFERENCES m3_import_file (id),
  CONSTRAINT fk_m3_biz_parent_batch FOREIGN KEY (batch_id) REFERENCES m3_import_batch (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='模块3-业务报告父ASIN日报（原始层）';

CREATE TABLE IF NOT EXISTS m3_biz_report_child_daily (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  child_asin VARCHAR(32) NOT NULL COMMENT '子ASIN',
  parent_asin VARCHAR(32) DEFAULT NULL COMMENT '父ASIN',
  title VARCHAR(500) DEFAULT NULL COMMENT '标题',
  marketplace VARCHAR(32) NOT NULL DEFAULT 'US' COMMENT '站点/市场',
  shop_name VARCHAR(128) DEFAULT NULL COMMENT '店铺名',
  stat_date DATE NOT NULL COMMENT '统计日期',
  sessions_total INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '会话数 - 总计',
  page_views_total INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '页面浏览量 - 总计',
  conversion_rate_total DECIMAL(10,4) DEFAULT NULL COMMENT '转化率 - 总计，小数存储',
  buy_box_pct DECIMAL(10,4) DEFAULT NULL COMMENT '推荐优惠/推荐展示位百分比，小数存储',
  unit_session_pct DECIMAL(10,4) DEFAULT NULL COMMENT '商品会话百分比，小数存储',
  ordered_product_sales DECIMAL(14,2) NOT NULL DEFAULT 0.00 COMMENT '已订购商品销售额',
  units_ordered INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '已订购商品数量',
  total_order_items INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '订单商品总数',
  source_file_id BIGINT UNSIGNED DEFAULT NULL COMMENT '来源文件ID',
  batch_id BIGINT UNSIGNED DEFAULT NULL COMMENT '导入批次ID',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_m3_biz_child_asin_date_shop (child_asin, stat_date, marketplace, shop_name),
  KEY idx_m3_biz_child_parent_asin (parent_asin),
  KEY idx_m3_biz_child_date (stat_date),
  CONSTRAINT fk_m3_biz_child_file FOREIGN KEY (source_file_id) REFERENCES m3_import_file (id),
  CONSTRAINT fk_m3_biz_child_batch FOREIGN KEY (batch_id) REFERENCES m3_import_batch (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='模块3-业务报告子ASIN日报（原始层）';

CREATE TABLE IF NOT EXISTS m3_sbv_campaign_daily (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  product_id BIGINT UNSIGNED DEFAULT NULL COMMENT '产品ID，按投放中的ASIN映射到子ASIN产品',
  asin VARCHAR(32) DEFAULT NULL COMMENT '投放中的ASIN / 子ASIN',
  marketplace VARCHAR(32) NOT NULL DEFAULT 'US' COMMENT '站点/市场',
  shop_name VARCHAR(128) DEFAULT NULL COMMENT '店铺/账户名',
  stat_date DATE NOT NULL COMMENT '统计日期',
  row_no INT UNSIGNED DEFAULT NULL COMMENT '原表行号（便于追溯）',
  is_summary_row TINYINT NOT NULL DEFAULT 0 COMMENT '是否汇总行 1是 0否',
  campaign_name VARCHAR(255) DEFAULT NULL COMMENT '广告活动名称',
  campaign_status VARCHAR(64) DEFAULT NULL COMMENT '广告活动状态',
  ad_group_name VARCHAR(255) DEFAULT NULL COMMENT '广告组名称',
  promoted_asin VARCHAR(32) DEFAULT NULL COMMENT '投放中的ASIN',
  site_name VARCHAR(128) DEFAULT NULL COMMENT '站点名称',
  impressions INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '曝光量',
  clicks INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '点击量',
  ctr DECIMAL(10,4) DEFAULT NULL COMMENT 'CTR，小数存储',
  cpc DECIMAL(12,4) DEFAULT NULL COMMENT 'CPC',
  spend DECIMAL(14,2) NOT NULL DEFAULT 0.00 COMMENT '花费',
  ad_sales DECIMAL(14,2) NOT NULL DEFAULT 0.00 COMMENT '广告销售额',
  acos DECIMAL(10,4) DEFAULT NULL COMMENT 'ACOS，小数存储',
  acos_raw VARCHAR(128) DEFAULT NULL COMMENT 'ACOS原始文本，兼容异常值',
  ad_orders INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '广告订单数',
  cvr DECIMAL(10,4) DEFAULT NULL COMMENT 'CVR，小数存储',
  cvr_raw VARCHAR(128) DEFAULT NULL COMMENT 'CVR原始文本',
  source_file_id BIGINT UNSIGNED DEFAULT NULL COMMENT '来源文件ID',
  batch_id BIGINT UNSIGNED DEFAULT NULL COMMENT '导入批次ID',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_m3_sbv_campaign_unique (stat_date, marketplace, shop_name, asin, campaign_name, row_no),
  KEY idx_m3_sbv_product_date (product_id, stat_date),
  KEY idx_m3_sbv_asin_date (asin, stat_date),
  CONSTRAINT fk_m3_sbv_product FOREIGN KEY (product_id) REFERENCES m3_product (id),
  CONSTRAINT fk_m3_sbv_file FOREIGN KEY (source_file_id) REFERENCES m3_import_file (id),
  CONSTRAINT fk_m3_sbv_batch FOREIGN KEY (batch_id) REFERENCES m3_import_batch (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='模块3-SBV广告活动日报（原始层）';

CREATE TABLE IF NOT EXISTS m3_sp_campaign_daily (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  product_id BIGINT UNSIGNED DEFAULT NULL COMMENT '产品ID，按广告ASIN映射到子ASIN产品',
  ad_asin VARCHAR(32) DEFAULT NULL COMMENT '广告ASIN',
  report_date DATE DEFAULT NULL COMMENT '报告日期',
  account_name VARCHAR(255) DEFAULT NULL COMMENT '广告组合/账户名称',
  currency_code VARCHAR(16) DEFAULT NULL COMMENT '货币代码',
  marketplace VARCHAR(32) NOT NULL DEFAULT 'US' COMMENT '站点/市场',
  shop_name VARCHAR(128) DEFAULT NULL COMMENT '店铺名',
  stat_date DATE NOT NULL COMMENT '统计日期',
  row_no INT UNSIGNED DEFAULT NULL COMMENT '原表行号（便于追溯）',
  campaign_name VARCHAR(255) NOT NULL COMMENT '广告活动名称',
  ad_group_name VARCHAR(255) DEFAULT NULL COMMENT '广告组名称',
  publisher_name VARCHAR(64) DEFAULT NULL COMMENT '投放渠道/Publisher',
  targeting_text VARCHAR(128) DEFAULT NULL COMMENT '投放/匹配方式',
  advertised_sku VARCHAR(64) DEFAULT NULL COMMENT '广告SKU',
  impressions INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '展示量',
  clicks INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '点击量',
  ctr DECIMAL(10,4) DEFAULT NULL COMMENT '点击率CTR，小数存储',
  cpc DECIMAL(12,4) DEFAULT NULL COMMENT '单次点击成本CPC',
  spend DECIMAL(14,2) NOT NULL DEFAULT 0.00 COMMENT '花费',
  ad_sales_7d DECIMAL(14,2) NOT NULL DEFAULT 0.00 COMMENT '7天总销售额',
  acos DECIMAL(10,4) DEFAULT NULL COMMENT 'ACOS，小数存储',
  roas DECIMAL(12,4) DEFAULT NULL COMMENT 'ROAS',
  ad_orders_7d INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '7天总订单数',
  cvr_7d DECIMAL(10,4) DEFAULT NULL COMMENT '7天转化率CVR，小数存储',
  source_file_id BIGINT UNSIGNED DEFAULT NULL COMMENT '来源文件ID',
  batch_id BIGINT UNSIGNED DEFAULT NULL COMMENT '导入批次ID',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_m3_sp_campaign_unique (stat_date, marketplace, shop_name, ad_asin, campaign_name, row_no),
  KEY idx_m3_sp_product_date (product_id, stat_date),
  KEY idx_m3_sp_asin_date (ad_asin, stat_date),
  CONSTRAINT fk_m3_sp_product FOREIGN KEY (product_id) REFERENCES m3_product (id),
  CONSTRAINT fk_m3_sp_file FOREIGN KEY (source_file_id) REFERENCES m3_import_file (id),
  CONSTRAINT fk_m3_sp_batch FOREIGN KEY (batch_id) REFERENCES m3_import_batch (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='模块3-SP广告活动日报（原始层）';

CREATE TABLE IF NOT EXISTS m3_product_daily_summary (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  product_id BIGINT UNSIGNED NOT NULL COMMENT '产品ID（前端卡片对应子ASIN产品）',
  stat_date DATE NOT NULL COMMENT '统计日期',
  rating_avg DECIMAL(4,2) DEFAULT NULL COMMENT '平均评分',
  review_count_total INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '累计评价数',
  review_count_new INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '新增评价数',
  sales_amount DECIMAL(14,2) NOT NULL DEFAULT 0.00 COMMENT '销售额',
  units_ordered INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '销量件数',
  order_item_count INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '订单商品数',
  sessions_total INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '访问量sessions',
  page_views_total INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '浏览量page views',
  conversion_rate_total DECIMAL(10,4) DEFAULT NULL COMMENT '转化率-总计，小数存储',
  buy_box_pct DECIMAL(10,4) DEFAULT NULL COMMENT '推荐展示位百分比，小数存储',
  unit_session_pct DECIMAL(10,4) DEFAULT NULL COMMENT '商品会话百分比，小数存储',
  listing_conversion_rate DECIMAL(10,4) DEFAULT NULL COMMENT 'listing转化率，小数存储',
  sp_impressions INT UNSIGNED NOT NULL DEFAULT 0 COMMENT 'SP广告曝光量',
  sp_clicks INT UNSIGNED NOT NULL DEFAULT 0 COMMENT 'SP广告点击量',
  sp_spend DECIMAL(14,2) NOT NULL DEFAULT 0.00 COMMENT 'SP广告花费',
  sp_sales DECIMAL(14,2) NOT NULL DEFAULT 0.00 COMMENT 'SP广告销售额',
  sp_orders INT UNSIGNED NOT NULL DEFAULT 0 COMMENT 'SP广告订单数',
  sbv_impressions INT UNSIGNED NOT NULL DEFAULT 0 COMMENT 'SBV广告曝光量',
  sbv_clicks INT UNSIGNED NOT NULL DEFAULT 0 COMMENT 'SBV广告点击量',
  sbv_spend DECIMAL(14,2) NOT NULL DEFAULT 0.00 COMMENT 'SBV广告花费',
  sbv_sales DECIMAL(14,2) NOT NULL DEFAULT 0.00 COMMENT 'SBV广告销售额',
  sbv_orders INT UNSIGNED NOT NULL DEFAULT 0 COMMENT 'SBV广告订单数',
  ad_impressions INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '广告总曝光量',
  ad_clicks INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '广告总点击量',
  ad_ctr DECIMAL(10,4) DEFAULT NULL COMMENT '广告总CTR，小数存储',
  ad_cpc DECIMAL(12,4) DEFAULT NULL COMMENT '广告总CPC',
  ad_spend DECIMAL(14,2) NOT NULL DEFAULT 0.00 COMMENT '广告总花费',
  ad_sales DECIMAL(14,2) NOT NULL DEFAULT 0.00 COMMENT '广告总销售额',
  ad_acos DECIMAL(10,4) DEFAULT NULL COMMENT '广告总ACOS，小数存储',
  ad_orders INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '广告总订单数',
  ad_cvr DECIMAL(10,4) DEFAULT NULL COMMENT '广告总CVR，小数存储',
  natural_order_count INT NOT NULL DEFAULT 0 COMMENT '自然订单数',
  natural_session_count INT NOT NULL DEFAULT 0 COMMENT '自然会话数',
  natural_conversion_rate DECIMAL(10,4) DEFAULT NULL COMMENT '自然转化率，小数存储',
  summary_version INT UNSIGNED NOT NULL DEFAULT 1 COMMENT '汇总版本号，方便重算',
  source_note VARCHAR(500) DEFAULT NULL COMMENT '汇总说明',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_m3_product_daily_summary (product_id, stat_date),
  KEY idx_m3_product_daily_summary_date (stat_date),
  CONSTRAINT fk_m3_product_daily_summary_product FOREIGN KEY (product_id) REFERENCES m3_product (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='模块3-产品日报汇总表（直接服务前端展示，按子ASIN产品维度聚合）';

-- 说明：
-- 1. m3_biz_report_parent_daily / m3_biz_report_child_daily / m3_sbv_campaign_daily / m3_sp_campaign_daily 是原始层。
-- 2. m3_product_daily_summary 是展示层，当前按子ASIN产品维度聚合。
-- 3. 父ASIN相关表用于汇总、归属、对账校验，不作为前端卡片直接展示主体。
-- 4. 前端当前需要的产品卡片 5 个区块，可优先从以下表读取：
--    - 基本信息：m3_product
--    - 评价信息：m3_review_daily
--    - 销售信息：m3_product_daily_summary（或业务报告原始层）
--    - 流量信息：m3_product_daily_summary（或业务报告原始层）
--    - 广告信息：m3_product_daily_summary + m3_sbv_campaign_daily + m3_sp_campaign_daily
