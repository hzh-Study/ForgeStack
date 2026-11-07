import fs from 'node:fs'

const CDP = 'http://127.0.0.1:9229'
const FRONT = 'http://127.0.0.1:5174'
const API = 'http://127.0.0.1:7245'
const sleep = (ms) => new Promise((resolve) => setTimeout(resolve, ms))

const targets = await (await fetch(`${CDP}/json/list`)).json()
const target = targets.find((item) => item.type === 'page')
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
  return new Promise((resolve, reject) => {
    pending.set(seq, { resolve, reject })
    ws.send(JSON.stringify({ id: seq, method, params }))
  })
}

async function evaluate(expression) {
  const result = await send('Runtime.evaluate', { expression, awaitPromise: true, returnByValue: true })
  if (result.exceptionDetails) {
    throw new Error(result.exceptionDetails.text || '脚本执行失败')
  }
  return result.result.value
}

async function waitFor(fn, timeout = 12000) {
  const start = Date.now()
  while (Date.now() - start < timeout) {
    try {
      const value = await fn()
      if (value) return value
    } catch (error) {
      // 页面切换过程中可能短暂取不到 DOM，继续等待
    }
    await sleep(150)
  }
  throw new Error('等待超时')
}

async function navigate(path) {
  await send('Page.navigate', { url: `${FRONT}${path}` })
  await waitFor(() => evaluate(`document.readyState === 'complete'`))
  await sleep(300)
}

async function hasText(text) {
  return evaluate(`document.body.innerText.includes(${JSON.stringify(text)})`)
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
  if (!fs.existsSync('.selfcheck')) fs.mkdirSync('.selfcheck')
  fs.writeFileSync(`.selfcheck/${name}.png`, Buffer.from(result.data, 'base64'))
}

await send('Page.enable')
await send('Runtime.enable')
await send('Emulation.setDeviceMetricsOverride', {
  width: 1440,
  height: 1000,
  deviceScaleFactor: 1,
  mobile: false
})

const report = {}

await setRole('13900000002')
await navigate('/logistics/drivers')
await waitFor(() => hasText('王五'))
report.drivers = {
  hasSeedDriver: true,
  rowText: (await evaluate(`document.querySelector('.el-table__body')?.innerText || ''`)).split('\n').filter(Boolean).slice(0, 8),
  overflowX: await evaluate(`document.documentElement.scrollWidth > window.innerWidth`)
}
await screenshot('05-logistics-drivers-1440')

await navigate('/logistics/vehicles')
await waitFor(() => hasText('京A12345'))
report.vehicles = {
  hasSeedVehicle: true,
  overflowX: await evaluate(`document.documentElement.scrollWidth > window.innerWidth`)
}
await screenshot('06-logistics-vehicles-1440')

await navigate('/logistics/quotas')
await waitFor(() => hasText('王五'))
report.quotas = {
  hasDriver: true,
  overflowX: await evaluate(`document.documentElement.scrollWidth > window.innerWidth`)
}

await setRole('13900000003')
await navigate('/driver/vehicle')
await waitFor(() => hasText('京A12345'))
report.driverVehicle = {
  hasSeedVehicle: true,
  overflowX: await evaluate(`document.documentElement.scrollWidth > window.innerWidth`)
}
await screenshot('07-driver-vehicle-1440')

await navigate('/driver/reimbursements')
await waitFor(() => hasText('剩余额度'))
await waitFor(() => hasText('5,000.00'))
report.driverReimbursements = {
  quotaText: (await evaluate(`document.querySelector('.rounded-lg.border.border-slate-200.bg-white.p-6')?.innerText || ''`)).slice(0, 300),
  overflowX: await evaluate(`document.documentElement.scrollWidth > window.innerWidth`)
}

await navigate('/driver/tasks')
await waitFor(() => hasText('我的任务'))
report.driverTasks = {
  tabs: await evaluate(`[...document.querySelectorAll('.el-tabs__item')].map((item) => item.innerText.trim())`),
  uploadCount: await evaluate(`document.querySelectorAll('.el-upload').length`),
  overflowX: await evaluate(`document.documentElement.scrollWidth > window.innerWidth`)
}

// 注册页角色与占位提示
await evaluate(`localStorage.clear()`)
await navigate('/sign-up')
report.signUp = {
  radioTexts: await evaluate(`[...document.querySelectorAll('.el-radio-button')].map((item) => item.innerText.trim())`),
  placeholders: await evaluate(`[...document.querySelectorAll('input')].map((item) => item.placeholder).filter(Boolean)`),
  overflowX: await evaluate(`document.documentElement.scrollWidth > window.innerWidth`)
}

console.log(JSON.stringify(report, null, 2))
ws.close()
