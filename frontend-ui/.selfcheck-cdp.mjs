import fs from 'node:fs'

const CDP = 'http://127.0.0.1:9229'
const FRONT = 'http://127.0.0.1:5174'
const API = 'http://127.0.0.1:7245'

const sleep = (ms) => new Promise((resolve) => setTimeout(resolve, ms))

const targets = await (await fetch(`${CDP}/json/list`)).json()
const target = targets.find((item) => item.type === 'page')
if (!target) {
  throw new Error('没有可用的页面目标')
}

const ws = new WebSocket(target.webSocketDebuggerUrl)
const pending = new Map()
let seq = 0

ws.addEventListener('message', (event) => {
  const message = JSON.parse(event.data)
  if (message.id && pending.has(message.id)) {
    const { resolve, reject } = pending.get(message.id)
    pending.delete(message.id)
    if (message.error) {
      reject(new Error(JSON.stringify(message.error)))
    } else {
      resolve(message.result)
    }
  }
})

await new Promise((resolve, reject) => {
  ws.addEventListener('open', resolve, { once: true })
  ws.addEventListener('error', reject, { once: true })
})

function send(method, params = {}) {
  seq += 1
  const id = seq
  return new Promise((resolve, reject) => {
    pending.set(id, { resolve, reject })
    ws.send(JSON.stringify({ id, method, params }))
  })
}

async function evaluate(expression) {
  const result = await send('Runtime.evaluate', {
    expression,
    awaitPromise: true,
    returnByValue: true
  })
  if (result.exceptionDetails) {
    throw new Error(result.exceptionDetails.text || '页面脚本执行失败')
  }
  return result.result.value
}

async function waitFor(fn, timeout = 12000) {
  const start = Date.now()
  let lastError = null
  while (Date.now() - start < timeout) {
    try {
      const value = await fn()
      if (value) {
        return value
      }
    } catch (error) {
      lastError = error
    }
    await sleep(150)
  }
  throw new Error(`等待超时${lastError ? `：${lastError.message}` : ''}`)
}

async function navigate(path) {
  await send('Page.navigate', { url: `${FRONT}${path}` })
  await waitFor(() => evaluate(`document.readyState === 'complete'`))
  await sleep(500)
}

async function pageText() {
  return evaluate(`document.body.innerText`)
}

async function hasText(text) {
  return pageText().then((content) => content.includes(text))
}

async function setRole(phone) {
  await evaluate(`(async () => {
    localStorage.clear()
    const response = await fetch('${API}/auth/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username: '${phone}', password: '123456' })
    })
    const result = await response.json()
    localStorage.setItem('standard_web_token', result.data.token)
    localStorage.setItem('standard_web_auth', JSON.stringify({ token: result.data.token }))
    return result.data.role
  })()`)
}

async function screenshot(name) {
  const result = await send('Page.captureScreenshot', { format: 'png', fromSurface: true })
  const dir = '.selfcheck'
  if (!fs.existsSync(dir)) {
    fs.mkdirSync(dir)
  }
  fs.writeFileSync(`${dir}/${name}.png`, Buffer.from(result.data, 'base64'))
}

await send('Page.enable')
await send('Runtime.enable')

const report = {}

// 1. 登录页渲染与真实表单登录
await navigate('/sign-in')
report.signIn = {
  url: await evaluate('location.pathname'),
  title: await evaluate('document.title'),
  hasLoginTitle: await hasText('用户登录'),
  inputCount: await evaluate(`document.querySelectorAll('.el-input__inner').length`)
}
await evaluate(`(() => {
  const setter = Object.getOwnPropertyDescriptor(HTMLInputElement.prototype, 'value').set
  const inputs = document.querySelectorAll('.el-input__inner')
  setter.call(inputs[0], '13900000001')
  inputs[0].dispatchEvent(new Event('input', { bubbles: true }))
  setter.call(inputs[1], '123456')
  inputs[1].dispatchEvent(new Event('input', { bubbles: true }))
  const buttons = [...document.querySelectorAll('button')]
  const loginButton = buttons.find((button) => button.innerText.trim() === '登录')
  loginButton.click()
  return true
})()`)
await waitFor(() => evaluate(`location.pathname === '/shipper/orders'`))
await waitFor(() => hasText('我的订单'))
report.signIn.loginPath = await evaluate('location.pathname')
report.shipperOrders = {
  url: await evaluate('location.pathname'),
  roleTag: await hasText('货主'),
  menu: await evaluate(`[...document.querySelectorAll('aside a')].map((item) => item.innerText.trim())`),
  emptyState: await hasText('暂无订单')
}
await screenshot('01-shipper-orders')

// 2. 货主发货单表单
await navigate('/shipper/orders/new')
report.shipperOrderForm = {
  url: await evaluate('location.pathname'),
  hasFreight: await hasText('运费报价(元)'),
  hasGoods: await hasText('货物明细'),
  hasPickup: await hasText('装货点'),
  hasDelivery: await hasText('卸货点'),
  hasValidDays: await hasText('有效期(天)'),
  sliderCount: await evaluate(`document.querySelectorAll('.el-input-number').length`)
}
await screenshot('02-shipper-order-form')

// 3. 货主越权访问司机路由应跳回角色首页
await navigate('/driver/tasks')
await sleep(600)
report.guardShipperToDriver = {
  path: await evaluate('location.pathname'),
  redirected: (await evaluate('location.pathname')) === '/shipper/orders'
}

// 4. 公共页面状态
await navigate('/realname')
report.realname = {
  path: await evaluate('location.pathname'),
  passed: await hasText('实名认证已通过'),
  submitButton: await evaluate(`[...document.querySelectorAll('button')].some((button) => button.innerText.includes('提交校验'))`)
}
await navigate('/onboarding')
report.onboarding = {
  path: await evaluate('location.pathname'),
  approved: await hasText('入驻审核已通过'),
  hasForm: await hasText('提交入驻资料')
}
await navigate('/messages')
report.messages = {
  path: await evaluate('location.pathname'),
  hasTabs: await hasText('系统通知'),
  hasNewConversation: await hasText('新建会话')
}

// 5. 物流公司主路径
await setRole('13900000002')
await navigate('/logistics/order-pool')
await waitFor(() => hasText('订单池'))
report.logisticsPool = {
  url: await evaluate('location.pathname'),
  menu: await evaluate(`[...document.querySelectorAll('aside a')].map((item) => item.innerText.trim())`),
  emptyState: await hasText('当前没有可接订单')
}
await screenshot('03-logistics-pool')
await navigate('/logistics/waybills')
report.logisticsWaybills = {
  hasTitle: await hasText('我的运单'),
  emptyState: await hasText('暂无运单')
}
await navigate('/logistics/drivers')
report.logisticsDrivers = {
  hasTitle: await hasText('司机管理'),
  hasCreate: await hasText('创建司机'),
  hasSeedDriver: await hasText('王五')
}
await navigate('/logistics/vehicles')
report.logisticsVehicles = {
  hasTitle: await hasText('车辆管理'),
  hasSeedVehicle: await hasText('京A12345')
}
await navigate('/logistics/reimbursements')
report.logisticsReimbursements = {
  hasTitle: await hasText('报销审核'),
  emptyState: await hasText('暂无报销单')
}
await navigate('/logistics/quotas')
report.logisticsQuotas = {
  hasTitle: await hasText('司机额度设置'),
  hasDriver: await hasText('王五')
}
// 物流公司越权访问货主路由
await navigate('/shipper/orders')
await sleep(600)
report.guardLogisticsToShipper = {
  path: await evaluate('location.pathname'),
  redirected: (await evaluate('location.pathname')) === '/logistics/order-pool'
}

// 6. 司机 Web 主路径
await setRole('13900000003')
await navigate('/driver/tasks')
await waitFor(() => hasText('我的任务'))
report.driverTasks = {
  url: await evaluate('location.pathname'),
  menu: await evaluate(`[...document.querySelectorAll('aside a')].map((item) => item.innerText.trim())`),
  tabs: await evaluate(`[...document.querySelectorAll('.el-tabs__item')].map((item) => item.innerText.trim())`),
  uploadCount: await evaluate(`document.querySelectorAll('.el-upload').length`),
  checkinEntry: await evaluate(`[...document.querySelectorAll('button')].some((button) => /出发打卡|到达打卡|现场拍照/.test(button.innerText))`)
}
await navigate('/driver/records')
report.driverRecords = {
  hasTitle: await hasText('我的记录'),
  readOnlyTip: await hasText('不提供任何上传或打卡入口'),
  uploadCount: await evaluate(`document.querySelectorAll('.el-upload').length`)
}
await navigate('/driver/reimbursements')
await waitFor(() => hasText('我的报销'))
report.driverReimbursements = {
  hasQuota: await hasText('剩余额度'),
  hasSubmit: await hasText('提交报销'),
  uploadCountAllowedForReceipt: await evaluate(`document.querySelectorAll('.el-upload').length === 0`)
}
await screenshot('04-driver-reimbursements')
await navigate('/driver/vehicle')
report.driverVehicle = {
  hasTitle: await hasText('我的车辆'),
  hasSeedVehicle: await hasText('京A12345'),
  uploadCount: await evaluate(`document.querySelectorAll('.el-upload').length`)
}
await navigate('/onboarding')
await sleep(600)
report.guardDriverToOnboarding = {
  path: await evaluate('location.pathname'),
  redirected: (await evaluate('location.pathname')) === '/driver/tasks'
}

// 7. 注册页（清理登录态后检查角色选项）
await evaluate(`localStorage.clear()`)
await navigate('/sign-up')
report.signUp = {
  hasRoleSelector: await hasText('货主') && await hasText('物流公司'),
  hasDriverOption: await hasText('司机'),
  hasPhone: await hasText('手机号（登录账号）')
}

console.log(JSON.stringify(report, null, 2))
ws.close()
