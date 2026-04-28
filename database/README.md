# 数据库目录

用于存放：

- 数据库设计文档
- 建表脚本
- 初始化数据脚本
- 版本迁移脚本

## 当前推荐基线

模块3（运营数据展示）当前推荐优先使用：

- `04-module3-operational-data-mysql57.sql`

说明：
- `02-operation-data-init.sql`：属于早期半结构化落库方案
- `03-operation-raw-init.sql`：属于早期原始五表方案
- 当前后续 ETL / 汇总 / API 开发，应优先以 `04` 为基线继续推进
