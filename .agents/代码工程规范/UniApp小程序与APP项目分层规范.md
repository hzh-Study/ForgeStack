# UniApp 小程序与 APP 项目分层规范

## 1. 文档目的

本文档用于约束 uni-app 小程序与 APP 端（`uniapp-ui` 工程）的工程分层、目录组织、模块边界、命名方式、平台差异处理和安全要求。

本文档不规定具体页面 UI 长什么样，只规定代码应该放在哪里、每一层负责什么、不同文件之间如何协作、小程序与 APP 平台差异如何处理。

本文档基于 uni-app 官方文档与社区验证方案编写，适用于微信小程序（MP-WEIXIN）和 APP（APP-PLUS，Android/iOS）两个目标平台；H5 仅声明为可选目标，不作为默认交付范围。

## 2. 技术栈约定

| 项目 | 推荐方案 |
|------|----------|
| 框架 | uni-app（Vue3 + Composition API） |
| 语言 | JavaScript，不使用 TypeScript |
| 工程方式 | Vite + CLI 工程（`npx degit dcloudio/uni-preset-vue#vite` 模板），npm scripts 驱动 |
| UI 组件库 | uni-ui（easycom 按需引入） |
| 状态管理 | Pinia |
| 状态持久化 | `pinia-plugin-unistorage` |
| 网络请求 | 自封装 `uni.request`（Promise 化统一封装，全局拦截用 `uni.addInterceptor`） |
| 时间处理 | `moment@2.29.4` |
| 样式 | SCSS + rpx + BEM 命名，编译器固定 `sass@1.78.0` |
| 目标平台 | 微信小程序（MP-WEIXIN）+ APP（APP-PLUS） |

硬性约束：

- 禁止依赖 HBuilderX 图形界面操作。执行任务的 agent 只能使用命令行，工程必须通过官方 Vite CLI 模板创建，构建、运行全部走 npm scripts。
- 禁止引入 Axios。Axios 依赖浏览器 XHR 环境，小程序和 APP 端不可用；统一封装官方跨端 API `uni.request`。
- 禁止使用依赖浏览器 localStorage 的 `pinia-plugin-persistedstate`。uni 端持久化必须使用 `pinia-plugin-unistorage`（自动走 `uni.setStorageSync`）或手动同步 `uni.setStorageSync`。
- `main.js` 必须使用 `createSSRApp` 创建应用，并在 `createApp` 返回值中同时返回 `Pinia` 对象，这是 uni-app 官方对 Vue3 工程的硬性要求。
- 状态、UI 组件库不混用其他第三方方案；uni-ui 组件通过 easycom 免注册直接使用，例如 `<uni-icons>`。

版本锁定约束（实测验证，违反会导致安装或构建失败）：

- uni-app CLI 模板锁定的 vue 版本（当前 3.4.21）不允许单独升级：`@dcloudio/uni-cli-shared` 精确依赖对应版本的 `@vue/server-renderer`，强行升级会产生双 vue 副本。
- Pinia 必须使用兼容 vue 3.4 的版本（当前固定 `pinia@2.2.8`），禁止安装要求 vue ≥ 3.5 的 pinia 版本（2.3+ / 3.x）。
- 工程根目录必须提供 `.npmrc` 并开启 `legacy-peer-deps=true`：uni-app 锁版 vue 与 pinia 面向 vue2 的可选 peer 依赖链在 npm 严格解析下冲突，该配置保证克隆仓库后 `npm install` 可复现。
- SCSS 编译器固定 `sass@1.78.0`：更高版本会产生 `@import` 和 legacy JS API 弃用告警，污染构建输出。

工程根目录必须提供 `.env` 配置文件，后端地址统一写入：

```text
VITE_API_BASE_URL=http://127.0.0.1:7245
```

代码内统一通过 `import.meta.env.VITE_API_BASE_URL` 读取，命名与 `backend-ui`、`frontend-ui` 保持一致。`utils/request.js` 的 baseURL 拼接和 `utils/file.js` 的文件地址拼接都必须优先读取该变量，不允许在页面或 API 模块中散落硬编码后端地址。

请求约定（与现有两端前端规范对齐）：

- 请求超时时间统一设置为 10 分钟，即 `10 * 60 * 1000` 毫秒，不允许使用 `10000` 这类短超时作为默认值。
- 请求头统一注入 `Authorization: Bearer <token>`，Token 从本地存储读取。
- 当后端未启动、网络异常、请求超时、接口 404、接口 500 或后端返回非标准结构时，`utils/request.js` 必须构建并抛出下面结构，交给调用方法自行展示：

```js
{
  code: 404,
  status: false,
  message: '接口不存在，请检查后端接口地址是否正确',
  data: null
}
```

- 每个调用接口的位置都必须自行判断后端 `Result.status`：`status === false` 时在当前页面、组件或 Store 调用链中处理错误，并在最终交互位置展示后端返回的 `message`。
- 登录、注册、上传、保存资料、修改密码等操作类接口成功后，必须在当前操作方法里提示对应成功文案，例如 `登录成功`、`头像上传成功`。
- 操作类接口异常时，必须在当前操作方法的 `catch` 中提示异常信息，不能依赖请求拦截器统一提示。
- 错误提示和成功提示统一使用 `uni.showToast({ icon: 'none', duration: 1000 })`，不允许在页面里手写提示 DOM 或使用 `alert`。
- 请求封装层只负责拼接地址、注入 Token、归一化响应和异常，不允许在 `utils/request.js` 中统一弹出业务错误提示。

图表与富文本约定：

- 图表默认不内置。当前任务确实需要图表时，统一使用 ECharts 的 uni-app 跨端适配方案（优先 `lime-echart` 组件），不要临时混用其他图表库。
- 富文本展示优先使用原生 `rich-text` 组件；内容复杂、需要图片预览等能力时使用 `mp-html`。展示前要明确后端字段存储格式是 HTML、Markdown 还是纯文本。

## 3. 目录示例

按需创建目录和文件，不因示例提前生成空模块。

```text
uniapp-ui
├─ src
│  ├─ pages                 # 主包页面，模块子目录 + 目录同名 .vue
│  │  ├─ index
│  │  │  └─ index.vue
│  │  └─ login
│  │      └─ login.vue
│  ├─ pages-sub             # 分包页面根目录，按业务模块再分子目录
│  ├─ static                # 图片、图标、字体等静态资源
│  ├─ components            # 全局通用组件，符合 easycom 规范
│  ├─ api                   # 按业务模块封装请求
│  ├─ stores                # Pinia 状态管理
│  ├─ models                # 默认表单、默认查询参数、字段转换
│  ├─ utils                 # request.js、auth.js、file.js、format.js、validate.js
│  ├─ hooks                 # 可复用组合式函数
│  ├─ constants             # 常量、枚举映射
│  ├─ config                # 项目配置
│  ├─ styles                # 全局 SCSS：variables.scss、base.scss
│  ├─ App.vue
│  ├─ main.js
│  ├─ pages.json            # 页面注册、tabBar、分包、预下载
│  └─ manifest.json         # 应用配置（appid、权限、SDK）
├─ .env
├─ package.json
└─ vite.config.js
```

说明：

- uni-app 官方推荐页面采用“目录同名 .vue”方式，例如 `pages/list/list.vue`，分包页面例如 `pages-sub/order/order.vue`。
- tabBar 图标等主包必须使用的静态资源必须放在主包 `static` 下；只被分包页面使用的静态资源随分包就近放置。
- uni-app 无 PC 式布局壳概念，不单设 `layout` 目录，公共页面结构通过全局组件和页面模板表达。

## 4. 目录职责

| 目录 | 职责 |
|------|------|
| `pages` | 主包页面级组件 |
| `pages-sub` | 分包页面级组件，按业务模块分子目录 |
| `static` | 图片、图标、字体、本地 JSON 等静态资源 |
| `components` | 全局可复用组件 |
| `api` | 按业务模块封装后端接口请求 |
| `stores` | Pinia 全局状态 |
| `models` | JavaScript 字段结构、默认值和数据转换 |
| `utils` | 请求、Token、格式化、校验、文件地址等工具 |
| `hooks` | 可复用组合式函数 |
| `constants` | 常量、枚举映射 |
| `config` | 项目配置 |
| `styles` | 全局 SCSS 变量、基础样式、公共混入 |

## 5. 业务模块组织

页面按业务模块放在 `pages` 或 `pages-sub` 下。

示例：

```text
pages-sub/demo
  ├─ DemoListView.vue
  ├─ DemoDetailView.vue
  └─ components
      └─ DemoCard.vue
```

对应 API：

```text
api/demo.js
```

对应数据模型：

```text
models/demo.js
```

规则：

- 只服务一个业务模块的组件，放在该模块 `components` 子目录中。
- 只有两个以上模块复用的组件，才提升到全局 `src/components`。

## 6. 分层调用规则

推荐调用方向：

```text
pages / pages-sub
  -> api
  -> utils/request
```

```text
pages / pages-sub
  -> stores
```

```text
pages / pages-sub
  -> components
```

禁止：

- 在页面里直接写 `uni.request`。
- 在 API 文件里写页面交互逻辑。
- 在 Store 中直接操作页面。
- 在通用组件里写强业务请求。

## 7. pages.json 配置规范

uni-app 不使用 Vue Router，页面路由统一由 `src/pages.json` 管理：

- 每个新建页面必须在 `pages.json` 的 `pages` 或 `subPackages` 中注册，未注册页面无法访问。
- `pages` 数组第一项为启动页，保持为首页。
- 页面标题、导航栏样式、下拉刷新等页面级配置统一写在对应页面的 `style` 内，不在页面代码里散写 `uni.setNavigationBarTitle` 等覆盖逻辑。
- `tabBar` 项固定为 2-5 个；tabBar 页面必须放在主包 `pages` 下，不允许放入分包。
- 分包页面统一注册到 `subPackages`，`root` 为 `pages-sub` 下的模块目录；需要预下载时通过 `preloadRule` 配置。
- `easycom` 保持官方默认规则，uni-ui 组件免注册直接使用。
- 开发调试用的启动页配置统一写在 `condition` 中维护，不混入生产配置。

分包注册示例：

```json
{
  "pages": [
    { "path": "pages/index/index", "style": { "navigationBarTitleText": "首页" } },
    { "path": "pages/login/login", "style": { "navigationBarTitleText": "登录" } }
  ],
  "subPackages": [
    {
      "root": "pages-sub/order",
      "pages": [
        { "path": "order", "style": { "navigationBarTitleText": "我的订单" } }
      ]
    }
  ]
}
```

## 8. API 分层规范

所有请求必须放在 `api` 目录，通过 `utils/request.js` 发起。

示例：

```js
// api/demo.js
import request from '@/utils/request'

/**
 * 分页查询示例列表
 *
 * @param {Object} params 分页查询参数
 * @returns {Promise} 分页结果
 */
export function pageDemos(params) {
  return request.get('/api/demos/page', params)
}

/**
 * 获取示例详情
 *
 * @param {Number} id 示例 ID
 * @returns {Promise} 详情数据
 */
export function getDemoDetail(id) {
  return request.get(`/api/demos/${id}`)
}
```

命名规则：

| 操作 | 方法名 |
|------|--------|
| 登录 | `login` |
| 注册 | `register` |
| 列表 | `listXxx` |
| 分页 | `pageXxx` |
| 详情 | `getXxxDetail` |
| 新增 | `createXxx` |
| 编辑 | `updateXxx` |
| 删除 | `deleteXxx` |

## 9. Store 分层规范

Store 只存跨页面共享状态。

推荐 Store：

| Store | 职责 |
|-------|------|
| `auth.js` | Token、当前用户资料、登录、退出和资料恢复 |
| `app.js` | 全局设置、平台信息、主题 |

普通列表数据、详情数据、搜索条件优先放在页面内部，不要塞进 Store。

持久化规则：

- 需要跨启动保留的状态（Token、用户资料）通过 `pinia-plugin-unistorage` 持久化，在 `defineStore` 第三个参数开启 `unistorage: true`。
- 禁止引入依赖浏览器 localStorage 的持久化插件。

`main.js` 注册写法（官方要求，`Pinia` 必须随返回值一起返回；unistorage 插件在此处接入）：

```js
import App from './App'
import { createSSRApp } from 'vue'
import * as Pinia from 'pinia'
import { createUnistorage } from 'pinia-plugin-unistorage'

export function createApp() {
  const app = createSSRApp(App)
  // 注册 Pinia 并接入 uni 端持久化适配（自动走 uni.setStorageSync）
  app.use(Pinia.createPinia().use(createUnistorage()))
  return {
    app,
    Pinia // 此处必须将 Pinia 返回，uni-app Vue3 工程硬性要求
  }
}
```

## 10. Models 分层规范

`models` 用于记录接口字段结构、默认查询参数和数据转换方法。本项目使用 JavaScript，不使用类型接口声明。

推荐写法：

```js
// models/demo.js

/**
 * 示例列表默认查询条件
 */
export const defaultDemoQuery = {
  keyword: '',
  pageNum: 1,
  pageSize: 10
}

/**
 * 归一化示例列表项字段
 *
 * @param {Object} item 后端返回的原始数据
 * @returns {Object} 页面使用的标准字段
 */
export function normalizeDemoListItem(item) {
  return {
    id: item.id,
    title: item.title
  }
}
```

规则：

- 可存放默认表单、默认查询条件、字段映射和数据转换方法。
- 不要在页面里到处写重复的临时字段结构。

## 11. Components 分层规范

组件分两类。

### 11.1 全局通用组件

放到 `src/components`，命名加 `Base` 前缀，例如：

- `BaseEmpty`
- `BaseUpload`
- `BaseLoadMore`

基础交互控件优先使用 uni-ui 组件，例如 `uni-icons`、`uni-forms`、`uni-easyinput`、`uni-popup`、`uni-load-more`，不要在同一页面里混用其他第三方 UI 库自行模拟组件。

### 11.2 模块局部组件

放到对应业务模块中，例如 `pages-sub/demo/components/DemoCard.vue`。

### 11.3 文件上传与展示

- 上传接口封装放在 `api/upload.js`，基于 `uni.uploadFile` 提供 `uploadFile(filePath, module, fileType)` 和 `uploadImage(filePath, module)`，页面不直接拼上传请求。
- 数据库只保存后端返回的相对路径，不保存 IP、域名或端口。
- 页面展示图片、附件时统一通过 `utils/file.js` 的 `resolveFileUrl(url)` 拼接访问地址，不在页面内手写后端域名。
- 存在上传交互时，验收覆盖真实选择文件、上传、预览、保存和回显。

## 12. 登录态分层规范

登录相关职责：

| 层 | 职责 |
|----|------|
| `api/auth.js` | 登录、注册、退出、获取用户信息接口 |
| `stores/auth.js` | Token、当前用户资料、登录态恢复与清理，经 unistorage 持久化 |
| `utils/auth.js` | Token 本地读写工具（`uni.setStorageSync` / `uni.getStorageSync` / `uni.removeStorageSync`） |
| 页面级拦截 | 需要登录的页面校验 Token，未登录跳转登录页 |

登录流程：

- 微信小程序端使用 `uni.login({ provider: 'weixin', onlyAuthorize: true })` 获取授权 code，将 code 发给后端换取 Token；后端返回 Token 后保存到本地存储并写入 Pinia。客户端不接触 AppSecret。
- APP 端账号密码、手机号登录走普通接口；如需微信登录（App 开放平台），用条件编译区分后仍复用同一套 Token 存储与状态逻辑。
- 登录页作为一级独立页面放在主包，例如 `pages/login/login`。
- `App.vue` 的 `onLaunch` 读取本地 Token 并调用当前用户信息接口校验；接口失败、Token 失效、用户不存在或被禁用时，必须清理 Token 和登录态，再跳转登录页。
- 页面级登录校验：需要登录的页面在 `onShow` 中校验 Token，未登录时跳转登录页；公共跳转封装可通过 `uni.addInterceptor('navigateTo')` 统一处理。
- 退出登录必须清理本地 Token、Pinia 登录态和持久化数据。
- Token 本地存储 key 统一为 `uniapp_token`，不允许多处各起一个 key。

## 13. 样式规范

- 尺寸单位以 rpx 为主，750rpx 等于屏幕宽度；少量边框、分隔线可用 px。
- 样式预处理统一使用 SCSS；公共变量放 `styles/variables.scss`，通过 `src/uni.scss` 以 `@/` 别名引入（uni.scss 内容随编译注入所有页面，因此其中的 import 必须用 `@/` 别名，相对路径会按各页面自身目录解析导致编译失败）；基础样式放 `styles/base.scss`，在 `App.vue` 中以 `@/` 别名引入一次。
- 类名采用 BEM 命名：`block__element--modifier`，例如 `order-card__title--active`。
- 不引入 Tailwind CSS。小程序端使用 Tailwind 需要社区插件做额外构建转换，增加配置与兼容风险，违背本规范“稳定、安全”的目标；后台和前台使用的 Tailwind 方案不迁移到本端。
- 页面私有样式使用 scoped；全局样式只放变量、reset、字体和确实跨页面复用的基础规则。
- 吸底按钮、底部导航等固定元素必须适配安全区域，使用 `env(safe-area-inset-bottom)`（可叠加 `constant()` 兼容旧版 iOS）。
- 主题色等公共色值统一收敛到 `styles/variables.scss`，不允许在页面里散落硬编码色值。

## 14. 条件编译与平台差异

- 条件编译语法：`#ifdef MP-WEIXIN`、`#ifdef APP-PLUS`、`#ifndef H5`，可用于 js、template、style 三种区域。
- 除 H5 条件编译块内部外，禁止使用 `window`、`document`、`localStorage`、`navigator`、`location` 等浏览器专有 API，一律使用 uni 跨端 API，例如 `uni.setStorageSync`、`uni.getSystemInfo`。
- 平台差异逻辑尽量收敛到 `utils`（例如 `utils/platform.js` 统一输出平台判断），页面内条件编译块保持简短。
- 页面逻辑使用 uni-app 页面生命周期（`onLoad`、`onShow`、`onReachBottom`、`onPullDownRefresh`），不混用浏览器事件；窗口尺寸变化监听使用 `uni.onWindowResize`，不使用 `window.addEventListener`。
- `manifest.json` 中微信小程序 appid 由开发者在微信公众平台申请后自行填入，规范文档和示例代码中一律使用占位说明；AppSecret 等敏感配置不允许出现在前端工程任何文件中。

## 15. 分包规范

- 微信小程序主包体积上限 2M，单个分包上限 2M，总包上限以微信官方当前文档为准；接近上限时优先做分包和资源治理。
- 主包只保留：首页等启动页、tabBar 页面、登录页、全局公共资源。
- 业务模块页面统一放 `pages-sub/模块`，通过 `subPackages` 注册，需要时用 `preloadRule` 配置预下载。
- 只被分包页面使用的静态资源随分包放置，不放到主包 `static`。
- 分包之间页面跳转使用完整路径，例如 `/pages-sub/order/order`。

## 16. 安全规范

- 禁止在前端源码硬编码 AppSecret、支付密钥、第三方 secret、测试账号密码；密钥只存在后端。
- 小程序请求域名必须为 HTTPS，并在微信公众平台配置合法域名；开发者工具中“不校验合法域名”仅限本地调试，不得作为交付状态。
- Token 统一存 `uni.setStorageSync`（key 见第 12 节），退出登录必须清除；不把 Token 拼进 URL 查询参数。
- 生产构建不在 console 输出 Token、手机号、密码等敏感信息；debug 日志仅限开发模式。
- 用户输入（手机号、密码）必须先经 `utils/validate.js` 校验再提交；不信任客户端数据，敏感操作以后端校验为准。
- 不使用 `eval`，不拼接执行远程字符串。
- `web-view` 仅加载可信 HTTPS 页面，不加载用户任意输入的地址。

## 17. 命名规范

| 类型 | 规则 | 示例 |
|------|------|------|
| 页面文件 | 目录同名小写 | `pages/demo/demo.vue` |
| 分包页面文件 | 目录同名小写 | `pages-sub/order/order.vue` |
| 模块局部组件 | 大驼峰 | `DemoCard.vue` |
| 全局通用组件 | 大驼峰 + Base 前缀 | `BaseEmpty.vue` |
| API 文件 | 小写业务名 | `demo.js` |
| Store 文件 | 小写业务名 | `auth.js` |
| Model 文件 | 小写业务名 | `demo.js` |
| 工具文件 | 小写语义名 | `storage.js` |
| 常量 | 大写下划线 | `ORDER_STATUS` |
| 分包根目录 | 统一 `pages-sub` | `pages-sub/order` |

## 18. 自检与验收

- 小程序端：`npm run dev:mp-weixin` 启动开发构建，产物在 `dist/dev/mp-weixin`，用微信开发者工具导入该目录验证页面和交互；交付前执行 `npm run build:mp-weixin`，用 `dist/build/mp-weixin` 复验。
- APP 端：`npm run build:app`（或 `dev:app`）验证构建产物。
- 服务启动、端口选择、进程记录和清理遵守 `终端命令执行规范.md`；页面实现和验收遵守 `页面实现与自检交付规范.md`。
- 注释遵守 `代码注释规范.md`，页面方法、API 方法、Store actions 必须写中文 JSDoc 注释。
- 涉及数据库操作时遵守 `MCP数据库操作通用规范.md`。
- 最终回复说明构建验证结果、开发者工具验收结果和未完成事项；不将未执行、失败或受阻的检查写成通过。
