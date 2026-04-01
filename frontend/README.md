# 前端目录

## 当前阶段

已从单文件静态演示页，推进到 **Vue 3 + TypeScript + Vite** 前端工程骨架。

## 当前文件说明

- `todo-demo.html`：最初的静态演示页
- `package.json`：前端工程依赖与脚本
- `index.html`：Vite 入口页
- `src/main.ts`：应用入口
- `src/router/`：页面路由
- `src/views/WorkbenchView.vue`：工作台页面
- `src/components/`：待办事项模块组件
- `src/mock/workbench.ts`：Mock 数据
- `src/types/workbench.ts`：类型定义
- `src/styles/index.css`：全局样式

## 已实现内容

- 工作台页面工程化
- 三大板块组件化拆分
- 竞品上新信息处理后消失
- 竞品监控变化项展示
- 运营数据柱状图对比展示
- 基于 Mock 数据驱动页面

## 后续建议

1. 执行 `npm install`
2. 执行 `npm run dev`
3. 开始引入 UI 组件库与图表库
4. 对接后端 API
