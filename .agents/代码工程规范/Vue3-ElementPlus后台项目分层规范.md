# Vue3 + Element Plus 后台项目分层规范

## 1. 文档目的

本文档用于约束 Vue3 + Element Plus 后台管理项目的工程分层、目录组织、模块边界和命名方式。

本文档不规定具体页面 UI 长什么样，只规定代码应该放在哪里、每一层负责什么、不同文件之间如何协作。

## 2. 技术栈约定

| 项目 | 推荐方案 |
|------|----------|
| 前端框架 | Vue3 |
| 构建工具 | Vite |
| UI 组件库 | Element Plus |
| 路由 | Vue Router |
| 状态管理 | Pinia |
| 状态持久化 | `pinia-plugin-persistedstate@3.2.1` |
| HTTP 请求 | Axios |
| 时间处理 | `moment@2.29.4` |
| 图表 | 默认内置 ECharts，固定使用 `echarts@5.5.1` |
| 富文本编辑器 | 默认内置 AIEditor，固定使用 `aieditor@1.3.3` |
| 语言 | JavaScript |
| 样式 | Tailwind CSS 优先，少量 CSS 仅用于第三方组件覆盖或特殊效果 |

后台项目根目录必须提供 `.env` 配置文件，后端地址统一写入：

```text
VITE_API_BASE_URL=http://127.0.0.1:7245
```

`utils/request.js` 的 Axios `baseURL` 和 `utils/file.js` 的文件地址拼接都必须优先读取 `VITE_API_BASE_URL`，不允许在页面或 API 模块中散落硬编码后端地址。默认不在 `vite.config.js` 配置 proxy；只有明确需要同源代理、规避跨域或接入特殊网关时，才按当前任务单独增加。

后台 Axios 请求超时时间统一设置为 10 分钟，即 `10 * 60 * 1000` 毫秒，不允许使用 `10000` 这类短超时作为默认值。

后台必须预留 Element Plus 主题变量文件，例如 `assets/styles/theme.css`，并在 `main.js` 中紧跟 `element-plus/dist/index.css` 后引入。后续修改系统主色时，优先修改 `--app-primary`、`--el-color-primary` 及配套 light/dark 色阶变量，保证 `el-button`、`el-input`、`el-message`、`el-pagination`、`el-tag` 等组件跟随主题变化。

后台 Axios 响应拦截器只负责返回 `response.data` 和把 axios 异常归一为标准失败结构，不允许在 `utils/request.js` 中统一弹出业务错误提示。

当后端未启动、网络异常、请求超时、接口 404、接口 500 或后端返回非标准结构时，`utils/request.js` 必须构建并抛出下面结构，交给调用方法自行展示：

```js
{
  code: 404,
  status: false,
  message: '接口不存在，请检查后端接口地址是否正确',
  data: null
}
```

后台每个调用接口的位置都必须自行判断后端 `Result.status`：

- `status === false` 时，在当前页面、组件或 Store 调用链中抛出或处理错误，并在最终交互位置展示后端返回的 `message`。
- 新增、编辑、删除、重置密码、上传、登录、退出、保存个人资料、修改密码等操作类接口成功后，必须在当前操作方法里提示对应成功文案，例如 `新增成功`、`编辑成功`、`删除成功`。
- 操作类接口异常时，必须在当前操作方法的 `catch` 中提示异常信息，不能依赖请求拦截器统一提示。
- 错误提示和成功提示统一显示 1 秒，即 Element Plus `ElMessage` 的 `duration` 固定为 `1000`。
- 路由守卫里静默校验 Token 的接口可以不弹错误提示，但必须清理失效 Token 并按路由规则跳转登录页。

## 3. 目录示例

按需创建目录和文件，不因示例提前生成空模块。

```text
src
  ├─ main.js
  ├─ App.vue
  ├─ config
  ├─ constants
  ├─ hooks
  ├─ directives
  ├─ api
  │   ├─ auth.js
  │   ├─ user.js
  │   ├─ system.js
  │   └─ demo.js
  │
  ├─ assets
  │   ├─ images
  │   └─ styles
  │       ├─ tailwind.css
  │       ├─ base.css
  │       └─ theme.css
  │
  ├─ components
  │   ├─ BaseTable
  │   ├─ BaseForm
  │   ├─ BaseDialog
  │   └─ BaseUpload
  │
  ├─ layout
  │   ├─ AdminLayout.vue
  │   └─ components
  │       ├─ Sidebar.vue
  │       ├─ HeaderBar.vue
  │       ├─ Breadcrumb.vue
  │       └─ TabsView.vue
  │
  ├─ router
  │   ├─ index.js
  │   ├─ routes.js
  │   └─ permission.js
  │
  ├─ stores
  │   ├─ auth.js
  │   ├─ app.js
  │   └─ permission.js
  │
  ├─ models
  │   ├─ api.js
  │   └─ user.js
  │
  ├─ utils
  │   ├─ request.js
  │   ├─ auth.js
  │   ├─ format.js
  │   └─ validate.js
  │
  └─ views
      ├─ login
      ├─ dashboard
      ├─ system
      └─ demo
```

## 4. 目录职责

| 目录 | 职责 |
|------|------|
| `api` | 按业务模块封装后端接口请求 |
| `assets` | 静态资源、全局样式、图片、字体 |
| `components` | 全局可复用组件 |
| `config` | 项目配置、环境配置、常量配置 |
| `constants` | 业务常量、枚举映射 |
| `directives` | Vue 自定义指令 |
| `hooks` | 可复用组合式函数 |
| `layout` | 后台整体布局和布局相关组件 |
| `router` | 路由定义、路由守卫、动态路由处理 |
| `stores` | Pinia 状态管理 |
| `models` | JavaScript 数据结构说明 |
| `utils` | 工具函数、请求封装、格式化、校验 |
| `views` | 页面级组件 |

## 5. 业务模块组织

后台页面按业务模块放在 `views` 下。

示例：

```text
views/user
  ├─ UserListView.vue
  ├─ UserFormView.vue
  └─ UserDetailView.vue
```

对应 API：

```text
api/user.js
```

对应数据模型：

```text
models/user.js
```

如果模块较复杂，可以在模块内拆分局部组件：

```text
views/user
  ├─ components
  │   ├─ UserSearchForm.vue
  │   └─ UserEditDialog.vue
  ├─ UserListView.vue
  └─ UserDetailView.vue
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

- 在 `api` 中操作页面状态。
- 在 `stores` 中直接操作 DOM。
- 在 `components` 中直接写具体业务接口，除非该组件就是业务组件。
- 在 `views` 中直接写 `axios.get/post`。

## 7. API 分层规范

所有接口请求必须放在 `api` 目录。

示例：

```js
// api/user.js
import request from '@/utils/request'

export function pageUsers(params) {
  return request.get('/admin/users', { params })
}

export function getUserDetail(id) {
  return request.get(`/admin/users/${id}`)
}
```

命名规则：

| 操作 | 方法名 |
|------|--------|
| 分页 | `pageXxx` |
| 列表 | `listXxx` |
| 详情 | `getXxxDetail` |
| 新增 | `createXxx` |
| 编辑 | `updateXxx` |
| 删除 | `deleteXxx` |
| 状态修改 | `updateXxxStatus` |

## 8. Store 分层规范

Store 只放跨页面共享状态。

推荐 Store：

| Store | 职责 |
|-------|------|
| `auth.js` | Token、当前账号资料、登录、退出和资料恢复 |
| `app.js` | 菜单折叠、主题、全局设置 |
| `permission.js` | 菜单、按钮权限、动态路由 |

不建议把普通列表数据放到 Store，列表数据优先放在对应页面内部。

## 9. Router 分层规范

路由相关文件职责：

| 文件 | 职责 |
|------|------|
| `router/index.js` | 创建 router 实例 |
| `router/routes.js` | 静态路由和基础路由 |
| `router/permission.js` | 登录校验、权限校验、动态路由守卫 |

路由 `meta` 建议包含：

```js
meta: {
  title: '页面标题',
  requiresAuth: true,
  permission: 'user:list'
}
```

## 10. Models 分层规范

`models` 用于记录接口字段结构、默认查询参数和数据转换方法。本项目使用 JavaScript，不使用类型接口声明。

推荐写法：

```js
// models/user.js
export const defaultUserQuery = {
  keyword: '',
  status: '',
  pageNum: 1,
  pageSize: 10
}

export function normalizeUserListItem(item) {
  return {
    id: item.id,
    username: item.username,
    status: item.status
  }
}
```

规则：

- 可存放默认表单、默认查询条件、字段映射和数据转换方法。
- 不要在页面里到处写重复的临时字段结构。

## 11. Components 分层规范

组件分两类：

### 11.1 全局通用组件

放到：

```text
components
```

例如：

- `BaseTable`
- `BaseDialog`
- `BaseUpload`
- `BaseEditor`

### 11.2 模块局部组件

放到对应页面模块下：

```text
views/user/components
```

例如：

- `UserSearchForm.vue`
- `UserEditDialog.vue`

规则：

- 只有两个以上模块复用的组件，才提升到全局 `components`。
- 只服务一个模块的组件，不要放到全局。

### 11.3 上传组件和方法

文件上传属于通用能力，后台页面必须按下面规则处理：

- 上传接口封装放在 `api/upload.js`，提供 `uploadFile(file, module, fileType)` 通用方法。
- 图片上传在通用方法基础上封装 `uploadImage(file, module)`，商品图、公告图、轮播图、头像等都复用该方法。
- 数据库只保存后端返回的相对文件路径，例如 `/files/common/image/20260831/xxx.png`，不保存 `http://localhost:{port}/`、IP、域名或端口。
- 页面展示图片、附件时，统一通过 `utils/file.js` 的 `resolveFileUrl(url)` 拼接访问地址，不在页面内手写后端域名。
- 不允许在商品页、公告页、轮播图页等业务页面各写一套上传方法；业务差异通过 `module`、`fileType`、数量限制和组件 props 传入。
- 如果两个以上页面出现相同上传交互，应抽离为全局上传组件，例如 `components/BaseUpload` 或 `components/BaseImageUpload`。
- 业务页面只负责传入业务模块名、数量限制、尺寸提示和保存返回 URL，不在页面里直接写 `FormData` 和上传请求。
- 页面存在上传控件时，交付前必须通过浏览器自检验证真实文件选择、上传成功、预览、保存和回显。

## 12. Layout 分层规范

后台布局组件只负责管理端公共结构，不写具体业务请求。

常见后台公共区域：

- 左侧菜单。
- 顶部栏。
- 面包屑。
- Tabs 标签栏。
- 内容区标题栏。
- 列表页搜索容器。
- 表格工具栏。

抽离规则：

- 后台页面共用的侧边栏、顶部栏、面包屑、Tabs 放到 `layout/components`。
- 多个后台业务页面都会用到的表格、弹窗、上传、状态标签等放到全局 `components`。
- 只服务当前业务模块的搜索表单、编辑弹窗等放到 `views/模块/components`。
- 根据效果图还原页面时，如果发现公共结构已存在，必须复用已有组件。
- 如果效果图出现轻微差异，优先用 props、插槽或配置项扩展已有公共组件。
- 不允许在多个后台页面复制粘贴同一套侧边栏、顶部栏、面包屑或工具栏。

## 13. 列表表格展示规范

后台列表页默认不展示数据库原始 `id` 字段，例如管理员列表不显示 `ID` 列。`id` 只允许作为编辑、删除、详情、重置密码等接口调用的内部标识使用。

列表列优先展示用户可识别的信息，例如头像、账号、姓名、手机号、邮箱、状态、创建时间等。确需展示 `id` 时，必须由用户或产品文档明确要求。

列表中的图片字段必须支持点击预览，例如头像、封面、轮播图、附件图片等。后台统一优先使用 Element Plus 的 `el-image` 和 `preview-src-list` 实现预览，不允许只展示不可点击的静态缩略图。没有图片时再展示文字占位或默认图。

## 14. Utils 分层规范

`utils` 只放无业务状态的工具函数。

常见文件：

| 文件 | 职责 |
|------|------|
| `request.js` | Axios 实例和拦截器 |
| `auth.js` | Token 读写 |
| `format.js` | 使用 Moment 做日期、时间、金额、状态格式化 |
| `validate.js` | 手机号、邮箱、密码等校验 |

不要在工具函数里写页面逻辑。

## 15. 图表与富文本规范

后台时间处理统一使用 Moment，不要临时混用 `dayjs`、`date-fns` 或手写日期格式化。

后台页面如果当前任务需要图表或富文本编辑器，按下面技术选型实现：

- 图表统一使用 ECharts，不要临时引入其他图表库。
- 富文本编辑器统一使用 AIEditor，不要手写假的富文本工具栏作为最终实现。
- 后台表单中的富文本编辑器默认可编辑区域必须足够书写长内容，外层容器高度或最小高度不得低于 `500px`。
- 图表初始化、销毁、窗口尺寸变化监听等逻辑优先封装到页面局部组件或 hooks，避免散落在页面主逻辑中。
- 富文本内容提交前按业务字段统一转换，保存到后端字段时要明确是 HTML、Markdown 还是纯文本。
- `package.json` 默认包含 ECharts 和 AIEditor，后续页面直接按需引入使用。

## 16. 命名规范

| 类型 | 规则 | 示例 |
|------|------|------|
| 页面组件 | 大驼峰 + View | `UserListView.vue` |
| 普通组件 | 大驼峰 | `UserEditDialog.vue` |
| API 文件 | 小写业务名 | `user.js` |
| Store 文件 | 小写业务名 | `auth.js` |
| Model 文件 | 小写业务名 | `user.js` |
| 工具文件 | 小写语义名 | `format.js` |
| 路由 name | 大驼峰 | `UserList` |
| 路由 path | 小写短横线 | `/user/list` |

## 17. 样式与基础组件

- 后台样式必须接入并实际使用 Tailwind CSS，普通布局、间距、颜色、字号、圆角、阴影和响应状态优先使用 Tailwind CSS 原子类，不为常规页面结构编写大段 scoped CSS。
- 只有第三方组件深度覆盖、复杂动画、富文本内容隔离、浏览器兼容修正或 Tailwind CSS 难以稳定表达的特殊效果，才允许补充少量 CSS。
- Element Plus 必须在 `main.js` 全局配置中文 locale，例如 `import zhCn from 'element-plus/es/locale/lang/zh-cn'` 后 `.use(ElementPlus, { locale: zhCn })`，避免分页、日期、弹窗等组件出现 `10/page`、`Go to` 等英文文案。
- 后台列表页使用 `el-pagination` 的 `sizes` 时，必须设置 `:teleported="false"`，避免页面滚动后页大小下拉浮层脱离分页区域、漂移到表格中间。

## 18. 登录态与账号交互

- 后台基础账号管理必须同时提供管理员管理和用户管理，管理员维护 `sys_admin`，用户维护 `sys_user`，两者页面、接口和字段结构分开。
- 后台登录成功后必须接收后端颁发的 Token，并保存到前端；跨页面登录状态使用 Pinia，并通过第 2 节指定的持久化插件持久化。
- 后台路由守卫必须以后台为默认入口：除登录页外，访问任意后台地址都要先检查 Token；没有 Token 直接跳转后台登录页。
- 后台启动或刷新页面时，如果本地已有 Token，必须先调用当前管理员信息接口，例如 `/admin/auth/profile`；接口成功后把管理员信息重新写入 `stores/auth.js`，接口失败、Token 失效、账号不存在或账号禁用时，必须清理 Token 并跳转后台登录页。
- 后台顶部右侧必须显示当前管理员昵称和头像，并通过点击昵称或头像打开下拉菜单；下拉菜单至少包含个人修改、密码修改和退出。
- 个人修改成功后必须把后端返回的当前管理员信息重新写入 Pinia Store，保证顶部头像和昵称立即刷新。
- 密码修改成功后必须清理当前 Token 和 Store 登录态，并跳转后台登录页重新登录。
- 退出登录必须调用后端 `POST /auth/logout`，成功后清理 Token 并跳转后台登录页。
- 请求层收到 HTTP 401 时必须清理本地 Token，并跳转后台登录页，避免继续使用失效 Token。

注释遵守 `代码注释规范.md`；页面验收遵守 `页面实现与自检交付规范.md`。
