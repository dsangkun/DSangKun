# 运维目录

用于存放：

- 环境说明
- 部署脚本
- 启停脚本
- 配置模板
- CI/CD 相关文件

## 当前脚本

### 1. `import_operation_data.py`

早期导入脚本，对应旧版半结构化落库方案（`op_*` 表）。

### 2. `import_module3_stage1.py`

模块3 ETL 第一版，对应当前推荐数据库基线（`m3_*` 表）。

当前已实现：
- ASIN 映射表导入 -> `m3_product` / `m3_product_asin_map`
- 子 ASIN 业务报告导入 -> `m3_biz_report_child_daily`
- 父 ASIN 业务报告导入 -> `m3_biz_report_parent_daily`
- SP 广告报告导入 -> `m3_sp_campaign_daily`
- SBV 广告报告导入 -> `m3_sbv_campaign_daily`
- 产品日报汇总生成 -> `m3_product_daily_summary`

当前仍未实现：
- 评价日报导入
- 面向前端的后端查询接口改造

### 3. `reconcile_module3_summary.py`

模块3离线对账脚本，不依赖 MySQL，直接基于真实样本文件做口径校验。

当前已实现：
- 读取映射表、子业务报告、父业务报告、SP 报告、SBV 报告、最终统计报表
- 支持单日 / 单子ASIN 对账
- 支持同时输出：
  - 子 ASIN 卡片对账
  - 父组 / sheet 顶部汇总对账
- 已验证样本：`20260317 / B0D45HSHDF`

当前已确认的口径结论：
- 子 ASIN 卡片 `listing转化率` 当前更接近 `订单商品总数 / 会话数 - 总计`，`商品会话百分比(unit_session_pct)` 可作为回退参考
- 子 ASIN 卡片内的 `广告订单 / 广告点击 / 花费` 当前样本更接近 SP 口径
- 父组顶部的 `总广告订单 / 总自然流量 / 自然转化率` 可按 父业务报告 + 组内 SP + 组内 SBV 重建

## 示例

```bash
python ops/import_module3_stage1.py \
  --host 127.0.0.1 --port 3306 --user root --password 123456 \
  --database ecommerce_ops --marketplace US --shop-name EXPERLAM \
  --mapping-file D:\\Programs\\testdata\\流量数据统计ASIN映射表.xlsx \
  --biz-child-path D:\\Programs\\testdata\\EXPERLAM-美国 \
  --biz-parent-path D:\\Programs\\testdata\\EXPERLAM-美国 \
  --sp-path D:\\Programs\\testdata\\EXPERLAM-美国 \
  --sbv-path D:\\Programs\\testdata\\SBV数据 \
  --build-summary
```

可选参数：
- `--summary-start-date 2026-04-01`
- `--summary-end-date 2026-04-23`

说明：
- SBV 导入会兼容常见列名别名，并保留异常文本到 `acos_raw` / `cvr_raw`
- 汇总脚本当前已生成销售 / 流量 / 广告 / 评价主指标
- `import_module3_stage1.py` 中 `listing_conversion_rate` 已修正为优先取 `订单商品总数 / 会话数 - 总计`，在无会话数时再回退 `unit_session_pct`
- 当前 `m3_product_daily_summary` 的子卡片层 `ad_*` 指标已调整为优先按 SP 口径汇总；`sbv_*` 拆分字段继续保留，供父组顶部/对账使用
- `natural_session_count` / `natural_conversion_rate` 在落库汇总脚本中仍按保守策略处理；如需核对真实业务口径，可先用 `reconcile_module3_summary.py`

离线对账示例：

```bash
python ops/reconcile_module3_summary.py \
  --mapping-file D:\\Programs\\testdata\\流量数据统计ASIN映射表.xlsx \
  --biz-child-path D:\\Programs\\testdata\\EXPERLAM-美国\\20260317 \
  --biz-parent-path D:\\Programs\\testdata\\EXPERLAM-美国\\20260317 \
  --sp-path D:\\Programs\\testdata\\EXPERLAM-美国\\20260317 \
  --sbv-path D:\\Programs\\testdata\\SBV数据\\20260317 \
  --final-report D:\\Programs\\testdata\\EXPERLAM-美国\\20260317\\EXPERLAM-美国-流量数据统计报表-20260317.xlsx \
  --child-asin B0D45HSHDF
```
```
