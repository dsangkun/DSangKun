-- 修复 m3_product_asin_map 唯一键设计
-- 背景：同一个父 ASIN 对多个子 ASIN 产品是正常场景，
-- 因此不能把 (asin, marketplace, shop_name, asin_role) 做成全局唯一。
-- 应改为对 product_id + asin + marketplace + shop_name + asin_role 做唯一约束。

ALTER TABLE m3_product_asin_map
  DROP INDEX uk_m3_asin_market_shop_role,
  ADD UNIQUE KEY uk_m3_product_asin_unique (product_id, asin, marketplace, shop_name, asin_role),
  ADD KEY idx_m3_product_asin_map_asin (asin);
