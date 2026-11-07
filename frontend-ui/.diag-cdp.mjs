const CDP = 'http://127.0.0.1:9230'
const API = 'http://127.0.0.1:7245'
const FRONT = 'http://127.0.0.1:5174'
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
    if (message.error) reject(new Error(JSON.stringify(message.error)))
    else resolve(message.result)
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
  if (result.exceptionDetails) throw new Error(result.exceptionDetails.text)
  return result.result.value
}
await send('Page.enable')
await send('Runtime.enable')
await send('Emulation.setDeviceMetricsOverride', { width: 1440, height: 1000, deviceScaleFactor: 1, mobile: false })
const data = await evaluate(`(async () => {
  localStorage.clear()
  const response = await fetch('${API}/auth/login', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username: '13900000002', password: '123456' })
  })
  const result = await response.json()
  localStorage.setItem('standard_web_token', result.data.token)
  localStorage.setItem('standard_web_auth', JSON.stringify({ token: result.data.token }))
  return { status: response.status, role: result.data?.role, hasToken: Boolean(result.data?.token) }
})()`)
text = undefined
console.log('LOGIN', JSON.stringify(data))
await send('Page.navigate', { url: `${FRONT}/logistics/drivers` })
await sleep(3000)
const state = await evaluate(`({
  url: location.href,
  title: document.title,
  body: document.body.innerText.slice(0, 1200),
  rows: [...document.querySelectorAll('.el-table__row')].map((row) => row.innerText.replace(/\\n/g, ' | ')),
  toast: document.querySelector('.el-message')?.innerText || '',
  overflowX: document.documentElement.scrollWidth > window.innerWidth,
  width: window.innerWidth
})`)
console.log(JSON.stringify(state, null, 2))
ws.close()
