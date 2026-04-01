# 后端目录

## 当前阶段

已建立 Spring Boot 3.x 后端骨架，用于支撑工作台中的“待办事项模块”。

## 技术栈

- Java 17
- Spring Boot 3.x
- Spring Web
- Spring Validation
- Springdoc OpenAPI

## 当前已提供

- 启动类
- CORS 配置
- 健康检查接口
- 工作台待办事项接口（Mock 返回）
- Swagger 文档入口约定

## 计划接口

- `GET /api/health`
- `GET /api/workbench/overview`
- `GET /api/workbench/new-arrivals`
- `POST /api/workbench/new-arrivals/{id}/action`
- `GET /api/workbench/competitor-changes`
- `GET /api/workbench/operation-data`

## 后续动作

1. 接入 MySQL
2. 引入数据访问层（MyBatis-Plus 或 JPA）
3. 用真实表结构替换当前 Mock 数据返回
4. 与前端联调
