# AGENTS

## 适用范围

本项目所有 AI 编码代理统一以本文件为规范入口，不再区分客户端。Codex、Claude Code、Cursor、Trae、GitHub Copilot 等读取各自的指针文件后，均回到这里。

## 规范读取顺序

生成或修改代码前，按任务涉及端侧读取对应规范：

- 通用工程入口：`.agents/agent.md`
- 工程代码执行约束：`.agents/代码工程规范/工程代码执行约束规范.md`
- SpringBoot 后端：`.agents/代码工程规范/SpringBoot通用项目结构规范.md`
- Vue3 + Element Plus 后台：`.agents/代码工程规范/Vue3-ElementPlus后台项目分层规范.md`
- Vue3 前台：`.agents/代码工程规范/Vue3前台项目分层规范.md`
- UniApp 小程序与 APP：`.agents/代码工程规范/UniApp小程序与APP项目分层规范.md`
- 页面实现与自检：`.agents/代码工程规范/页面实现与自检交付规范.md`
- 页面设计与素材：`.agents/代码工程规范/页面设计与素材处理规范.md`
- 代码注释：`.agents/代码工程规范/代码注释规范.md`
- 终端命令：`.agents/代码工程规范/终端命令执行规范.md`
- 数据库操作：`.agents/代码工程规范/MCP数据库操作通用规范.md`
- 本地环境与验证命令：`docs/agents/dev-environment.md`
- 评审前先读已知取舍：`docs/agents/known-tradeoffs.md`

## 客户端适配

- 规则正文只维护在本文件与 `.agents/`，各客户端入口文件仅做指向，不重复维护规则：
  - `CLAUDE.md`：Claude Code
  - `.cursor/rules/project.mdc`：Cursor
  - `.trae/rules/project_rules.md`：Trae
  - `.github/copilot-instructions.md`：GitHub Copilot
- 数据库 MCP：Codex 使用 `.codex/config.toml`，其他客户端使用根目录 `.mcp.json`，连接信息保持一致。

## 核心约束

- 后端、后台、前台必须保持工程独立，不能混放代码。
- 前端统一使用 JavaScript，不使用 TypeScript。
- 不需要 Swagger、OpenAPI、Knife4j。

## Agent skills

### Issue tracker

Issues and specs live as markdown files under `.scratch/<feature-slug>/`. See `docs/agents/issue-tracker.md`.

### Triage labels

Five canonical triage roles, recorded as `Status:` lines in issue files. See `docs/agents/triage-labels.md`.

### Domain docs

Single-context: one `GLOSSARY.md` at the repo root plus `docs/adr/`. See `docs/agents/domain.md`.
