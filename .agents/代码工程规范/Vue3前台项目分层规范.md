# Vue3 前台项目分层规范

## 1. 文档目的

本文档用于约束 Vue3 前台项目的工程分层、目录组织、模块边界和命名方式。

本文档不规定具体页面 UI 长什么样，只规定代码应该放在哪里、每一层负责什么、不同文件之间如何协作。

## 2. 技术栈约定

| 项目 | 推荐方案 |
|------|----------|
| 前端框架 | Vue3 |
| 构建工具 | Vite |
| 路由 | Vue Router |
| 状态管理 | Pinia |
| 状态持久化 | `pinia-plugin-persistedstate@3.2.1` |
| HTTP 请求 | Axios |
| 时间处理 | `moment@2.29.4` |
| 图表 | 默认内置 ECharts，固定使用 `echarts@5.5.1` |
| 富文本展示/编辑 | 默认内置 AIEditor，固定使用 `aieditor@1.3.3` |
| 语言 | JavaScript |
| 样式 | Tailwind CSS |
| UI 组件 | Element Plus |

前台项目根目录必须提供 `.env` 配置文件，后端地址统一写入：

```text
VITE_API_BASE_URL=http://127.0.0.1:7245
```

`utils/request.js` 的 Axios `baseURL` 和 `utils/file.js` 的文件地址拼接都必须优先读取 `VITE_API_BASE_URL`，不允许在页面或 API 模块中散落硬编码后端地址。默认不在 `vite.config.js` 配置 proxy；只有明确需要同源代理、规避跨域或接入特殊网关时，才按当前任务单独增加。

前台 Axios 请求超时时间统一设置为 10 分钟，即 `10 * 60 * 1000` 毫秒，不允许使用 `10000` 这类短超时作为默认值。

前台必须预留 Element Plus 主题变量文件，例如 `assets/styles/theme.css`，并在 `main.js` 中紧跟 `element-plus/dist/index.css` 后引入。后续修改系统主色时，优先修改 `--app-primary`、`--el-color-primary` 及配套 light/dark 色阶变量，保证 `el-button`、`el-input`、`el-message`、`el-pagination`、`el-tag` 等组件跟随主题变化。

前台 Axios 响应拦截器只负责返回 `response.data` 和把 axios 异常归一为标准失败结构，不允许在 `utils/request.js` 中统一弹出业务错误提示。

当前端请求遇到后端未启动、网络异常、请求超时、接口 404、接口 500 或后端返回非标准结构时，`utils/request.js` 必须构建并抛出下面结构，交给调用方法自行展示：

```js
{
  code: 404,
  status: false,
  message: '接口不存在，请检查后端接口地址是否正确',
  data: null
}
```

前台每个调用接口的位置都必须自行判断后端 `Result.status`：

- `status === false` 时，在当前页面、组件或 Store 调用链中抛出或处理错误，并在最终交互位置展示后端返回的 `message`。
- 登录、上传、保存个人资料、修改密码等操作类接口成功后，必须在当前操作方法里提示对应成功文案，例如 `登录成功`、`头像上传成功`、`个人资料已更新`。
- 操作类接口异常时，必须在当前操作方法的 `catch` 中提示异常信息，不能依赖请求拦截器统一提示。
- 错误提示和成功提示统一使用 Element Plus 的 `ElMessage`，并统一显示 1 秒，即 `duration` 固定为 `1000`。
- `utils/message.js` 只封装 `showSuccessMessage`、`showErrorMessage` 等触发方法，不允许在工具函数里直接 `document.createElement` 操作 DOM；页面里也不允许重复写提示 DOM 或使用 `window.alert`。
- 路由守卫里静默校验 Token 的接口可以不弹错误提示，但必须清理失效 Token，并按路由规则跳转登录页。

## 3. 目录示例

按需创建目录和文件，不因示例提前生成空模块。

```text
src
  ├─ main.js
  ├─ App.vue
  ├─ config
  ├─ constants
  ├─ hooks
  ├─ api
  │   ├─ auth.js
  │   ├─ user.js
  │   ├─ home.js
  │   └─ demo.js
  │
  ├─ assets
  │   ├─ images
  │   ├─ icons
  │   └─ styles
  │       ├─ tailwind.css
  │       ├─ base.css
  │       └─ theme.css
  │
  ├─ components
  │   ├─ BaseCard
  │   ├─ BaseEmpty
  │   ├─ BaseUpload
  │   └─ BasePagination
  │
  ├─ layout
  │   ├─ WebLayout.vue
  │   ├─ UserCenterLayout.vue
  │   └─ components
  │       ├─ HeaderNav.vue
  │       ├─ FooterBar.vue
  │       └─ UserSideMenu.vue
  │
  ├─ router
  │   ├─ index.js
  │   ├─ routes.js
  │   └─ permission.js
  │
  ├─ stores
  │   ├─ auth.js
  │   └─ app.js
  │
  ├─ models
  │   ├─ api.js
  │   └─ user.js
  │
  ├─ utils
  │   ├─ request.js
  │   ├─ auth.js
  │   ├─ storage.js
  │   ├─ format.js
  │   └─ validate.js
  │
  └─ views
      ├─ home
      ├─ auth
      ├─ user-center
      └─ demo
```

## 4. 目录职责

| 目录 | 职责 |
|------|------|
| `api` | 按业务模块封装请求 |
| `assets` | 图片、图标、字体、全局样式 |
| `components` | 可复用前台组件 |
| `config` | 项目配置 |
| `constants` | 常量、枚举映射 |
| `hooks` | 可复用组合式函数 |
| `layout` | 前台公共布局 |
| `router` | 路由定义和路由守卫 |
| `stores` | Pinia 全局状态 |
| `models` | JavaScript 字段结构、默认值和数据转换 |
| `utils` | 请求、格式化、校验、缓存等工具 |
| `views` | 页面级组件 |

## 5. 业务模块组织

页面按业务模块放在 `views` 下。

示例：

```text
views/demo
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

## 6. 分层调用规则

推荐调用方向：

```text
views
  -> api
  -> utils/request
```

```text
views
  -> stores
```

```text
views
  -> components
```

禁止：

- 在页面里直接写 `axios`。
- 在 API 文件里写页面交互逻辑。
- 在 Store 中直接操作 DOM。
- 在通用组件里写强业务请求。

## 7. API 分层规范

所有请求必须放在 `api` 目录。

示例：

```js
// api/demo.js
import request from '@/utils/request'

export function listDemos(params) {
  return request.get('/api/demos', { params })
}

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

## 8. Store 分层规范

Store 只存跨页面共享状态。

推荐：

| Store | 职责 |
|-------|------|
| `auth.js` | Token、当前账号资料、登录、退出和资料恢复 |
| `app.js` | 全局设置、设备类型、主题 |

普通列表数据、详情数据、搜索条件优先放在页面内部，不要塞进 Store。

## 9. Router 分层规范

路由相关文件职责：

| 文件 | 职责 |
|------|------|
| `router/index.js` | 创建 router 实例 |
| `router/routes.js` | 定义路由 |
| `router/permission.js` | 登录态校验和访问控制 |

路由 `meta` 建议包含：

```js
meta: {
  title: '页面标题',
  requiresAuth: false
}
```

需要登录的页面必须设置：

```js
requiresAuth: true
```

## 10. Models 分层规范

`models` 用于记录接口字段结构、默认查询参数和数据转换方法。本项目使用 JavaScript，不使用类型接口声明。

推荐写法：

```js
// models/demo.js
export const defaultDemoQuery = {
  keyword: '',
  pageNum: 1,
  pageSize: 10
}

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

- 前台必须在 `main.js` 全局引入 Element Plus 和中文 locale，例如 `import zhCn from 'element-plus/es/locale/lang/zh-cn'` 后 `.use(ElementPlus, { locale: zhCn })`。
- 前台表单、输入框、按钮、弹窗、上传、下拉菜单等基础交互控件优先使用 Element Plus，例如 `el-form`、`el-input`、`el-button`、`el-dialog`、`el-upload`、`el-dropdown`，不要在同一页面里混用大量原生 `form/input/button` 自行模拟组件。

组件分两类：

### 11.1 全局通用组件

放到：

```text
components
```

例如：

- `BaseCard`
- `BaseEmpty`
- `BaseUpload`
- `BasePagination`

### 11.2 模块局部组件

放到对应业务模块中：

```text
views/demo/components
```

例如：

- `DemoCard.vue`
- `DemoFilter.vue`

规则：

- 只有两个以上模块复用的组件，才放到全局 `components`。
- 只服务一个业务模块的组件，放在该模块内部。

### 11.3 文件上传与展示

- 上传接口放在 `api/upload.js`，提供 `uploadFile(file, module, fileType)` 和 `uploadImage(file, module)`，页面不直接拼上传请求或 FormData。
- 页面只保存后端返回的相对路径；图片和附件统一通过 `utils/file.js` 的 `resolveFileUrl(url)` 拼接访问地址。
- 重复上传交互抽离为通用组件，业务差异通过参数或 props 传入。
- 存在上传控件时，浏览器验收覆盖真实文件选择、上传、预览、保存和回显。

## 12. Layout 分层规范

布局组件只负责公共结构，不写具体业务请求。

常见布局：

| 布局 | 作用 |
|------|------|
| `WebLayout.vue` | 普通前台页面公共布局 |
| `UserCenterLayout.vue` | 用户中心类页面公共布局 |

布局中可以包含：

- 顶部导航。
- 底部信息。
- 用户中心菜单。
- `router-view`。

### 12.1 前台公共区域抽离规则

根据用户提供的效果图实现页面时，如果发现某些区域会在多个前台页面重复出现，必须优先抽离到 `layout/components` 或全局 `components`。

常见需要抽离的前台公共区域：

- 顶部欢迎栏。
- 品牌 Logo 与搜索栏。
- 主导航栏和商品分类入口。
- 购物车、帮助中心、登录注册链接。
- 底部保障服务栏。
- 版权页脚。
- 用户中心侧边菜单。

抽离规则：

- 属于页面骨架的公共区域放到 `layout/components`。
- 可以跨多个布局复用的小组件放到 `components`。
- 页面级 `views` 只保留当前页面独有内容，例如注册表单、商品列表、订单卡片。
- 公共区域如果存在轻微展示差异，通过 props、插槽或配置对象处理。
- 不允许在多个页面复制粘贴同一套顶部、底部或导航结构。

## 13. Utils 分层规范

`utils` 只放无业务状态的工具函数。

常见文件：

| 文件 | 职责 |
|------|------|
| `request.js` | Axios 实例和拦截器 |
| `auth.js` | Token 读写 |
| `storage.js` | 本地缓存封装 |
| `format.js` | 使用 Moment 做日期、时间、金额、文本格式化 |
| `validate.js` | 手机号、邮箱、密码等校验 |

## 14. 前台样式规范

前台页面设计 CSS 时，优先使用 Tailwind CSS 完成布局、间距、字号、颜色、圆角、阴影、响应状态和基础交互样式。

使用规则：

- 页面主体样式优先写成 Tailwind CSS 原子类，不为普通布局、间距、颜色和字体额外编写大量 scoped CSS。
- 公共布局组件和公共前台组件也优先使用 Tailwind CSS，保证样式来源统一，减少重复 class。
- 只有遇到复杂动画、第三方组件深度覆盖、富文本内容隔离、浏览器兼容修正或 Tailwind CSS 难以表达的特殊效果时，才补充少量 scoped CSS。
- 补充 scoped CSS 时，类名必须语义化，样式只服务当前组件，不把页面局部样式写进全局样式文件。
- 全局样式只放 reset、基础变量、字体、页面通用背景和确实跨页面复用的基础规则。
- 不要在同一页面里同时堆叠大量 Tailwind CSS、SCSS 和内联 style，避免样式来源混乱。
- 根据效果图还原页面时，优先用 Tailwind CSS 表达视觉结构；确实无法稳定还原的复杂素材，再按素材处理规范使用图片资源。

## 15. 图表、富文本与地图规范

前台时间处理统一使用 Moment，不要临时混用 `dayjs`、`date-fns` 或手写日期格式化。

前台页面如果当前任务需要图表、富文本展示/编辑或地图能力，按下面技术选型实现：

- 图表统一使用 ECharts，不要临时引入其他图表库。
- 富文本相关能力统一使用 AIEditor。
- 地图能力优先使用百度地图，并采用当前页面能满足需求的最简单接入方式。
- 商品详情、公告详情、帮助文章等只展示富文本内容时，要注意内容容器样式隔离，避免污染全局页面样式。
- 富文本内容保存或展示前，要明确后端字段存储格式是 HTML、Markdown 还是纯文本。
- `package.json` 默认包含 ECharts 和 AIEditor，后续页面直接按需引入使用。
- 只需要展示一个地点或简单定位时，优先使用百度地图 JavaScript API 的基础地图、标注点和信息窗能力，不额外引入复杂地图组件库。
- 地图 AK、服务地址等配置应放在项目配置或环境变量中，不要硬编码在多个页面里。
- 地图组件只封装当前任务真实需要的能力，避免提前封装路线规划、区域绘制、热力图、轨迹回放等复杂 GIS 功能。

## 16. 命名规范

| 类型 | 规则 | 示例 |
|------|------|------|
| 页面组件 | 大驼峰 + View | `DemoDetailView.vue` |
| 普通组件 | 大驼峰 | `DemoCard.vue` |
| API 文件 | 小写业务名 | `demo.js` |
| Store 文件 | 小写业务名 | `auth.js` |
| Model 文件 | 小写业务名 | `demo.js` |
| 工具文件 | 小写语义名 | `storage.js` |
| 路由 name | 大驼峰 | `DemoDetail` |
| 路由 path | 小写短横线 | `/demo/detail` |

## 17. 登录态分层规范

登录相关职责：

| 层 | 职责 |
|----|------|
| `api/auth.js` | 登录、注册、退出、获取用户信息接口 |
| `stores/auth.js` | Token、当前账号资料、登录态恢复与清理，按第 2 节选型持久化 |
| `router/permission.js` | 判断是否需要登录 |
| `utils/auth.js` | Token 本地读写工具 |

- 前台登录成功后必须接收后端颁发的 Token，并保存到前端；跨页面登录状态使用 Pinia，并通过第 2 节指定的持久化插件持久化。
- 前台游客访问默认不受影响：没有 Token 时可正常进入公开页面，只有访问 `requiresAuth` 页面才跳转用户登录页。
- 前台登录页必须作为一级独立路由，例如 `/sign-in`，不挂在 `WebLayout` 等公共前台布局 children 下，不套用顶部导航、底部栏等公共页面壳。
- 前台启动或刷新页面时，如果本地已有 Token，必须先调用当前用户信息接口，例如 `/api/auth/profile`；接口成功后把用户信息重新写入 `stores/auth.js`，接口失败、Token 失效、用户不存在或账号禁用时，必须清理 Token，并跳转用户登录页。
- 前台顶部登录后必须显示当前用户昵称和头像，并通过点击昵称或头像打开下拉菜单；下拉菜单至少包含个人修改、密码修改和退出。
- 个人修改成功后必须把后端返回的当前用户信息重新写入 Pinia Store，保证顶部头像和昵称立即刷新。
- 密码修改成功后必须清理当前 Token 和 Store 登录态，并跳转前台登录页重新登录。
- 退出登录必须调用后端 `POST /auth/logout`，成功后清理 Token 并跳转前台登录页。
- 请求层收到 HTTP 401 时必须清理本地 Token，并跳转前台登录页，避免继续使用失效 Token。

注释遵守 `代码注释规范.md`；页面验收遵守 `页面实现与自检交付规范.md`。
