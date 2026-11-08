#!/usr/bin/env node
import fs from 'node:fs'
import path from 'node:path'
import { execSync } from 'node:child_process'
import { fileURLToPath } from 'node:url'

const scriptDir = path.dirname(fileURLToPath(import.meta.url))
const repoRoot = path.resolve(scriptDir, '..')

const violations = []

function toPosix(value) {
  return value.split(path.sep).join('/')
}

function relativePath(file) {
  return toPosix(path.relative(repoRoot, file))
}

function walk(dir, skipDirs = new Set(['node_modules', 'dist', 'target'])) {
  const files = []
  if (!fs.existsSync(dir)) {
    return files
  }
  for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
    const fullPath = path.join(dir, entry.name)
    if (entry.isDirectory()) {
      if (skipDirs.has(entry.name)) continue
      files.push(...walk(fullPath, skipDirs))
    } else if (entry.isFile()) {
      files.push(fullPath)
    }
  }
  return files
}

function readLines(file) {
  return fs.readFileSync(file, 'utf8').split(/\r?\n/)
}

function addViolation(file, line, rule, detail) {
  violations.push({ file: relativePath(file), line, rule, detail })
}

// 1. 后端主代码
const serverJavaRoot = path.join(repoRoot, 'server-api', 'src', 'main', 'java')
for (const file of walk(serverJavaRoot)) {
  if (!file.endsWith('.java')) continue
  const isController = /Controller\.java$/.test(file)
  readLines(file).forEach((rawLine, index) => {
    const lineNo = index + 1

    if (/@PutMapping|@DeleteMapping|@PatchMapping/.test(rawLine)) {
      addViolation(file, lineNo, 'http-method', '禁止 @PutMapping/@DeleteMapping/@PatchMapping，统一使用 POST')
    }

    const sqlRules = [
      { re: /\.setSql\(/, detail: '禁止 .setSql( 拼接 SQL' },
      { re: /\.apply\(/, detail: '禁止 .apply( 拼接 SQL 片段' },
      { re: /\.inSql\(/, detail: '禁止 .inSql( 拼接 SQL 片段' },
      { re: /\.exists\(/, detail: '禁止 .exists( 拼接 SQL 片段' }
    ]
    for (const rule of sqlRules) {
      if (!rule.re.test(rawLine)) continue
      // Files.exists(...) 是 java.nio 文件判断，不属于 MyBatis-Plus 查询拼接
      if (rule.detail.includes('.exists(') && /Files\.exists\(/.test(rawLine)) continue
      addViolation(file, lineNo, 'sql-injection', rule.detail)
    }

    if (isController && /\bimport\s+.*\.mapper\./.test(rawLine)) {
      addViolation(file, lineNo, 'controller-mapper', 'Controller 禁止直接 import mapper')
    }
  })
}

// 2. 三个前端
const frontendRoots = ['backend-ui/src', 'frontend-ui/src', 'uniapp-ui/src']
const textExtensions = new Set([
  '.js', '.mjs', '.cjs', '.jsx', '.vue', '.html', '.htm',
  '.css', '.scss', '.less', '.json', '.wxml', '.wxss', '.uvue', '.nvue'
])
// uni-app 官方脚手架生成的类型声明桩，不是业务 TypeScript 源码
const typescriptAllowlist = new Set(['uniapp-ui/src/shime-uni.d.ts'])

for (const relRoot of frontendRoots) {
  const rootDir = path.join(repoRoot, ...relRoot.split('/'))
  for (const file of walk(rootDir)) {
    const relFile = relativePath(file)
    const ext = path.extname(file).toLowerCase()

    if (ext === '.ts' || ext === '.tsx') {
      if (!typescriptAllowlist.has(relFile)) {
        addViolation(file, 1, 'typescript', '前端统一使用 JavaScript，禁止 TypeScript 源文件')
      }
      continue
    }

    if (!textExtensions.has(ext)) continue

    readLines(file).forEach((rawLine, index) => {
      const lineNo = index + 1
      if (/request\.put\(/.test(rawLine)) {
        addViolation(file, lineNo, 'request-method', '禁止 request.put(，统一使用 POST')
      }
      if (/request\.delete\(/.test(rawLine)) {
        addViolation(file, lineNo, 'request-method', '禁止 request.delete(，统一使用 POST')
      }
      if (/http:\/\/(?:127\.0\.0\.1|localhost):7245/.test(rawLine)) {
        addViolation(file, lineNo, 'hardcoded-api', '禁止硬编码 7245 地址，API 地址必须来自 env')
      }
    })
  }
}

// 3. 仓库卫生
let trackedFiles = []
try {
  const output = execSync('git ls-files', { cwd: repoRoot, encoding: 'utf8' })
  trackedFiles = output.split(/\r?\n/).filter(Boolean)
} catch (error) {
  console.error('无法执行 git ls-files，仓库卫生检查中止：', error.message)
  process.exit(1)
}

for (const tracked of trackedFiles) {
  if (/(^|\/)\.selfcheck(\/|$)/.test(tracked)) {
    violations.push({ file: tracked, line: null, rule: 'repo-hygiene', detail: '禁止跟踪 .selfcheck/' })
  }
  if (/\.cdp\.mjs$/.test(tracked)) {
    violations.push({ file: tracked, line: null, rule: 'repo-hygiene', detail: '禁止跟踪 *.cdp.mjs' })
  }
  if (/(^|\/)\.diag-/.test(tracked)) {
    violations.push({ file: tracked, line: null, rule: 'repo-hygiene', detail: '禁止跟踪 .diag-* 文件' })
  }
}

if (violations.length === 0) {
  console.log('CONVENTIONS PASSED')
  process.exit(0)
}

console.error(`发现 ${violations.length} 条规范违规：`)
for (const violation of violations) {
  const location = violation.line === null ? violation.file : `${violation.file}:${violation.line}`
  console.error(`  ${location}  [${violation.rule}] ${violation.detail}`)
}
process.exit(1)
