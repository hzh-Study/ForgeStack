# 本地开发环境与验证命令

面向 AI 代理的本地环境速查。工程规范与端侧约束见 `.agents/代码工程规范/`，本文件只记录环境事实和验证命令。

## 环境

- JDK 17：优先 `$env:JAVA_HOME`，否则 `%USERPROFILE%\.jdks\corretto-17.0.21`；脚本会自动加入 PATH，找不到时直接报错。
- Maven：使用仓库内 `server-api\mvnw.cmd`。本机离线验证加 `-o`；GitHub CI 有网络，不加 `-o`。
- Node.js 20+ / npm：三个前端均已有 `package-lock.json`，安装用 `npm ci`。
- MySQL：`root / 123456 @ localhost:3306`，业务库 `forgestack`。
  - 首次初始化：执行 `server-api/首次初始化数据库脚本.sql`。
  - 演示数据：可选执行 `server-api/src/main/resources/db/demo-data.sql`，一次性写入，请勿重复执行。
  - 演示账号：管理员 `13800000000 / admin123`；货主 `13900000001 / 123456`；物流公司 `13900000002 / 123456`；司机 `13900000003 / 123456`。

## 端口

| 端侧 | 端口 |
|------|------|
| 后端 `server-api` | 7245 |
| 后台管理端 `backend-ui` | 5173 |
| 前台用户端 `frontend-ui` | 5174 |
| 司机 H5（`uniapp-ui`） | 5175 |

## 验证命令

| 命令 | 用途 |
|------|------|
| `pwsh scripts/check.ps1` | 全量门禁：后端测试 + 约定检查 + 后台/前台/uniapp 构建 |
| `pwsh scripts/check-mysql.ps1` | 创建临时库并运行 `mysql-check` profile 的 schema 冒烟检查 |
| `pwsh scripts/run-backend.ps1` | 后台运行后端，日志写入 `temp/backend.log` |
| `cd backend-ui; npm run build` | 后台管理端构建 |
| `cd frontend-ui; npm run build` | 前台用户端构建 |
| `cd uniapp-ui; npm run build:h5; npm run build:mp-weixin` | uniapp H5 与微信小程序构建 |

## 自检规则

- 验证跑 build/test，不启动长期 dev server。
- 需要联调或查看运行日志时，后台启动服务并把输出写入 `temp/*.log`，结束前关闭本次启动的进程。
- 约定检查只扫描 `server-api/src/main/java` 与三个前端的 `src`；新增规则先改 `scripts/check-conventions.mjs`，不要绕过。
- 更多约束见 `AGENTS.md`、`.agents/agent.md` 与 `.agents/代码工程规范/`。
