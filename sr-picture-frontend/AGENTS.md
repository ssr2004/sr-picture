# AGENTS

本文件用于指导 AI 编码代理在本仓库内稳定工作。

## 1) 变更范围约束（最高优先级）

- 当用户让你实现某个功能时，只允许修改与该功能直接相关的代码。
- 禁止顺手修改无关代码（包括无关重构、命名清理、样式统一、批量格式化）。
- 若发现无关问题，只在最终说明中提示，不主动改动。
- 除非用户明确要求，不要扩大改动面。

## 2) 常用命令

- 安装依赖: `npm install`
- 本地开发: `npm run dev`
- 生产构建: `npm run build`
- 类型检查: `npm run type-check`
- Lint 修复: `npm run lint`
- 代码格式化: `npm run format`
- 生成 OpenAPI 客户端: `npm run openapi`

命令来源: [package.json](package.json)

## 3) 项目结构与边界

- 页面层: [src/pages](src/pages)
- 路由定义: [src/router/index.ts](src/router/index.ts)
- 权限守卫: [src/access.ts](src/access.ts)
- 登录态 Store: [src/stores/useLoginUserStore.ts](src/stores/useLoginUserStore.ts)
- 请求封装与拦截器: [src/request.ts](src/request.ts)
- OpenAPI 生成配置: [openapi.config.js](openapi.config.js)
- API 生成目录: [src/api](src/api)

约定: `@/` 指向 `src/`，见 [vite.config.ts](vite.config.ts) 与 [tsconfig.app.json](tsconfig.app.json)。

## 4) 功能开发准则

- 新增页面时，仅改动该页面及其直接依赖（必要时包含路由与权限判断）。
- 涉及接口时，优先复用 [src/api](src/api) 中现有客户端；需要新增接口时再执行 `npm run openapi`。
- 涉及登录/权限时，保持 [src/access.ts](src/access.ts) 与 [src/request.ts](src/request.ts) 行为一致：
  - 未登录响应码 `40100` 会触发登录跳转。
  - `/admin` 路径要求管理员角色。

## 5) 常见坑位

- OpenAPI 生成依赖后端文档地址 `http://localhost:8123/api/v2/api-docs`（见 [openapi.config.js](openapi.config.js)）。
- 登录相关改动要避免拦截器运行时报错，访问链路字段时优先使用可选链。
- 管理端删除等操作注意请求参数类型与 API 声明保持一致。

## 6) 参考文档

- 项目基础说明: [README.md](README.md)
