# ForgeStack

面向 AI 编码代理的多端开发脚手架与代码工程规范体系。

以「规范驱动 + 多端独立工程」为核心：SpringBoot 后端、Vue3 管理端、Vue3 前台、uni-app 小程序/APP 四个相互独立的工程，配套一套完整的 AI 代理编码规范（`AGENTS.md` 生态），Codex、Claude Code、Cursor、Trae、GitHub Copilot 等客户端读取各自入口文件后均回到同一套规范。

## 工程结构

| 端侧 | 目录 | 技术栈 | 说明 |
|------|------|--------|------|
| 后端 | `server-api` | SpringBoot + MyBatis-Plus + Sa-Token + MySQL | 统一返回、全局异常、JWT 登录、管理员/用户账号体系、本地文件存储 |
| 后台管理端 | `backend-ui` | Vue3 + Element Plus + Vite + Pinia + Tailwind CSS | 登录、基础布局、管理员与用户管理 |
| 前台用户端 | `frontend-ui` | Vue3 + Vite + Pinia + Tailwind CSS | 首页壳、注册登录、个人中心壳 |
| 小程序与 APP | `uniapp-ui` | uni-app（Vue3）+ uni-ui + Pinia | 微信小程序与 APP 双端，Vite CLI 工程 |

各端相互独立，均可单独启动、构建和联调；后端地址统一通过各端 `.env` 的 `VITE_API_BASE_URL` 配置（默认 `http://127.0.0.1:7245`）。

## 快速开始

### 1. 初始化数据库

MySQL 中创建数据库 `base`（utf8mb4），执行 `server-api/首次初始化数据库脚本.sql`：

- 内置演示账号：`admin`（管理员）、`user`（用户）。
- 忘记初始密码时，可按 `PasswordUtil` 的摘要规则 `SHA-256(密码:盐)` 生成新摘要更新 `sys_account` 表，或部署后通过修改密码接口重置。

### 2. 启动后端

```bash
cd server-api
mvn spring-boot:run
```

端口 `7245`；数据库连接账号密码按 `src/main/resources/application.yml` 修改（默认本机 `root/123456`）。

### 3. 启动管理端 / 用户端

```bash
# 后台管理端
cd backend-ui
npm install
npm run dev

# 前台用户端
cd frontend-ui
npm install
npm run dev
```

### 4. 小程序与 APP 端

```bash
cd uniapp-ui
npm install
npm run dev:mp-weixin     # 开发构建，产物 dist/dev/mp-weixin
npm run build:mp-weixin   # 发行构建，产物 dist/build/mp-weixin
```

- 用微信开发者工具导入 `dist/dev/mp-weixin` 运行；`uniapp-ui/.npmrc` 的 `legacy-peer-deps` 为必须配置（uni-app 锁版 vue 与 npm 严格解析的兼容约定），请勿删除。
- 微信小程序 `appid` 需在微信公众平台申请后填入 `src/manifest.json`。
- APP 端验证：`npm run build:app`，产物在 `dist/build/app`，用 HBuilderX 导入打包。

## 代码工程规范体系

规范正文只维护在 `AGENTS.md` 与 `.agents/` 下，各 AI 客户端入口文件（`CLAUDE.md`、`.cursor/rules/project.mdc`、`.github/copilot-instructions.md`、`.trae/rules/project_rules.md`、`.codex/config.toml`）仅做指向。

| 规范文件 | 适用场景 |
|----------|----------|
| `工程代码执行约束规范.md` | 工程推进、目录、技术栈、各端边界、自检和交付约束 |
| `SpringBoot通用项目结构规范.md` | 编写 SpringBoot 后端代码 |
| `Vue3-ElementPlus后台项目分层规范.md` | 编写后台管理端代码 |
| `Vue3前台项目分层规范.md` | 编写前台用户端代码 |
| `UniApp小程序与APP项目分层规范.md` | 编写 uni-app 小程序与 APP 端代码 |
| `页面实现与自检交付规范.md` | 页面实现、自检和交付 |
| `页面设计与素材处理规范.md` | 用户提供效果图或素材图时使用 |
| `代码注释规范.md` | 类、方法、字段与复杂逻辑的中文注释 |
| `终端命令执行规范.md` | 终端命令执行与自检服务清理 |
| `MCP数据库操作通用规范.md` | 数据库连接、建库建表与查询验证 |

## AI 数据库工具链

- `.mcp.json`：面向 Codex 之外 AI 客户端的数据库 MCP 配置。
- `.codex/config.toml`：Codex 客户端的数据库 MCP 配置。

两者默认连接本机 MySQL（`root/123456@127.0.0.1:3306/base`），与 `application.yml` 保持一致；请务必改为自己的连接信息后再使用。

## License

[MIT](./LICENSE)
