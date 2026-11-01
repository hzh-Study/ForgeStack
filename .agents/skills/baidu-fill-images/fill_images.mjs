#!/usr/bin/env node

import crypto from 'node:crypto';
import fs from 'node:fs/promises';
import { existsSync } from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import { createRequire } from 'node:module';
import { pathToFileURL } from 'node:url';

const HEADERS = {
  'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36',
  Accept: 'text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8',
  'Accept-Language': 'zh-CN,zh;q=0.9,en;q=0.8',
};

const BAIDU_SEARCH_TIMEOUT = Number(process.env.BAIDU_IMAGE_TIMEOUT || 8) * 1000;
const BAIDU_DOWNLOAD_TIMEOUT = Number(process.env.BAIDU_IMAGE_DOWNLOAD_TIMEOUT || 20) * 1000;
const MAX_CANDIDATE_IMAGES = Number(process.env.BAIDU_IMAGE_CANDIDATE_LIMIT || 50);
const usedSourceUrls = new Set();

function printHelp() {
  console.log(`
百度图片补图工具：按提示词找图，上传到后端并输出系统 URL

示例:
  node .agents/skills/baidu-fill-images/fill_images.mjs --prompt "双肩包 商品图" --count 1 --api-base <backend-api-base> --admin-user <admin-user> --admin-password <admin-password>
  node .agents/skills/baidu-fill-images/fill_images.mjs --prompt "商城促销轮播图" --count 2 --upload-url <backend-api-base>/File/batchUpload --token <token>
  node .agents/skills/baidu-fill-images/fill_images.mjs --prompt "头像" --count 13 --api-base <backend-api-base> --token <token>

参数:
  --prompt           图片搜索提示词，必填
  --count            需要上传返回的图片数量，默认 1
  --api-base         后端 API 地址；也可通过 BACKEND_API_BASE 环境变量提供
  --upload-url       上传接口地址，默认 {api-base}/File/batchUpload
  --token            上传接口 Authorization token
  --admin-user       管理员账号；提供后会先登录再上传
  --admin-password   管理员密码；提供后会先登录再上传
  --role-type        登录角色类型，默认 1
`);
}

function parseArgs(argv) {
  const args = {
    count: 1,
    roleType: 1,
  };
  for (let index = 0; index < argv.length; index += 1) {
    const item = argv[index];
    if (item === '--help' || item === '-h') {
      args.help = true;
      continue;
    }
    if (!item.startsWith('--')) {
      continue;
    }
    const key = item.slice(2);
    const next = argv[index + 1];
    if (!next || next.startsWith('--')) {
      args[key] = true;
      continue;
    }
    args[key] = next;
    index += 1;
  }
  return {
    prompt: args.prompt,
    count: Math.max(Number(args.count || 1), 1),
    apiBase: args['api-base'],
    uploadUrl: args['upload-url'],
    token: args.token,
    adminUser: args['admin-user'],
    adminPassword: args['admin-password'],
    roleType: Number(args['role-type'] || args.roleType || 1),
    help: args.help,
  };
}

function normalizeCandidateLimit(count) {
  return Math.min(MAX_CANDIDATE_IMAGES, Math.max(count * 5, count + 10, 30));
}

function shuffle(items) {
  for (let index = items.length - 1; index > 0; index -= 1) {
    const swapIndex = Math.floor(Math.random() * (index + 1));
    [items[index], items[swapIndex]] = [items[swapIndex], items[index]];
  }
  return items;
}

function addCandidate(candidates, sourceUrl) {
  if (!sourceUrl || typeof sourceUrl !== 'string') return;
  const normalized = sourceUrl.replaceAll('\\/', '/').trim();
  if (!normalized.startsWith('http')) return;
  if (usedSourceUrls.has(normalized)) return;
  usedSourceUrls.add(normalized);
  candidates.push(normalized);
}

async function fetchText(url, timeoutMs = BAIDU_SEARCH_TIMEOUT) {
  const response = await fetch(url, {
    headers: HEADERS,
    signal: AbortSignal.timeout(timeoutMs),
  });
  if (!response.ok) {
    throw new Error(`HTTP ${response.status}`);
  }
  return await response.text();
}

async function searchBaiduImages(keyword, count = 3) {
  const wanted = normalizeCandidateLimit(count);
  const encodedKeyword = encodeURIComponent(keyword);
  const offsets = shuffle([0, 30, 60]);
  const candidates = [];

  for (const offset of offsets) {
    const query = `/search/acjson?tn=resultjson_com&logid=1234`
      + `&ipn=rj&ct=201326592&fp=result&word=${encodedKeyword}`
      + `&queryWord=${encodedKeyword}&cl=2&lm=-1&ie=utf-8&oe=utf-8`
      + `&st=-1&z=&ic=0&hd=&latest=&copyright=&s=&se=&tab=&width=&height=`
      + `&face=0&istype=2&qc=&nc=1&expermode=&nojc=&isAsync=&pn=${offset}&rn=${wanted}`
      + '&gsm=1e&1234=1234';
    for (const scheme of ['https', 'http']) {
      try {
        const payload = await fetchText(`${scheme}://image.baidu.com${query}`);
        const data = JSON.parse(payload);
        for (const item of data.data || []) {
          if (!item) continue;
          addCandidate(candidates, item.middleURL || item.thumbURL || item.objURL || '');
          if (candidates.length >= wanted) break;
        }
      } catch (error) {
        console.log(`  [警告] 百度接口搜索失败(${scheme}): ${error.message}`);
      }
      if (candidates.length >= wanted) break;
    }
    if (candidates.length >= wanted) break;
  }

  if (candidates.length < count) {
    candidates.push(...await searchBaiduHtml(keyword, wanted - candidates.length));
  }
  if (candidates.length < count) {
    candidates.push(...await searchBaiduWithBrowser(keyword, wanted - candidates.length));
  }

  return shuffle([...new Set(candidates)]).slice(0, wanted);
}

function decodeHtmlUrl(value) {
  return value
    .replaceAll('\\/', '/')
    .replaceAll('&amp;', '&')
    .replaceAll('\\u0026', '&');
}

async function searchBaiduHtml(keyword, count = 3) {
  const encodedKeyword = encodeURIComponent(keyword);
  const candidates = [];
  const patterns = [
    /"objURL"\s*:\s*"(https?:\/\/[^"]+)"/gi,
    /"middleURL"\s*:\s*"(https?:\/\/[^"]+)"/gi,
    /"thumbURL"\s*:\s*"(https?:\/\/[^"]+)"/gi,
    /"hoverURL"\s*:\s*"(https?:\/\/[^"]+)"/gi,
    /<img[^>]+src="(https?:\/\/[^"]+\.(?:jpg|jpeg|png|webp))"/gi,
  ];

  for (const scheme of ['https', 'http']) {
    try {
      const html = await fetchText(`${scheme}://image.baidu.com/search/index?tn=baiduimage&fm=result&ie=utf-8&word=${encodedKeyword}`);
      for (const pattern of patterns) {
        for (const matched of html.matchAll(pattern)) {
          addCandidate(candidates, decodeHtmlUrl(matched[1]));
          if (candidates.length >= count) return candidates;
        }
      }
    } catch (error) {
      console.log(`  [错误] 百度页面搜索失败(${scheme}): ${error.message}`);
    }
  }
  return candidates;
}

async function findPlaywrightCwd() {
  const cwd = process.cwd();
  const candidates = [cwd, path.dirname(cwd)];
  try {
    for (const child of await fs.readdir(cwd, { withFileTypes: true })) {
      if (child.isDirectory()) {
        candidates.push(path.join(cwd, child.name));
      }
    }
  } catch {}
  return candidates.find((item) => existsSync(path.join(item, 'node_modules', 'playwright'))) || null;
}

function findBrowserExecutable() {
  const candidates = [
    'C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe',
    'C:\\Program Files\\Microsoft\\Edge\\Application\\msedge.exe',
    'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe',
    'C:\\Program Files (x86)\\Google\\Chrome\\Application\\chrome.exe',
  ];
  return candidates.find((item) => existsSync(item)) || undefined;
}

async function searchBaiduWithBrowser(keyword, count = 3) {
  const playwrightCwd = await findPlaywrightCwd();
  if (!playwrightCwd) {
    console.log('  [警告] 未找到 Playwright node_modules，跳过浏览器兜底');
    return [];
  }
  try {
    const require = createRequire(pathToFileURL(path.join(playwrightCwd, 'package.json')));
    const { chromium } = require('playwright');
    const executablePath = findBrowserExecutable();
    const browser = await chromium.launch({ headless: true, executablePath });
    const page = await browser.newPage({ userAgent: HEADERS['User-Agent'] });
    const urls = [];
    const seen = new Set();
    const add = (value) => {
      if (!value || seen.has(value)) return;
      try {
        const parsed = new URL(value);
        if (!/^https?:$/.test(parsed.protocol)) return;
        if (!/(^|\.)baidu\.com$/.test(parsed.hostname)) return;
        if (!parsed.pathname.includes('/it/')) return;
        seen.add(value);
        urls.push(value);
      } catch {}
    };
    for (const base of ['https://image.baidu.com/search/index', 'http://image.baidu.com/search/index']) {
      try {
        await page.goto(`${base}?tn=baiduimage&word=${encodeURIComponent(keyword)}&ie=utf-8`, {
          waitUntil: 'domcontentloaded',
          timeout: 60000,
        });
      } catch {}
      await page.waitForTimeout(2500);
      for (let index = 0; index < 4; index += 1) {
        await page.mouse.wheel(0, 1600).catch(() => {});
        await page.waitForTimeout(650);
      }
      const rawUrls = await page.locator('img').evaluateAll((images) => images.flatMap((image) => [
        image.currentSrc,
        image.src,
        image.getAttribute('data-imgurl'),
        image.getAttribute('data-objurl'),
        image.getAttribute('data-thumburl'),
      ]).filter(Boolean)).catch(() => []);
      rawUrls.forEach(add);
      if (urls.length >= count) break;
    }
    await browser.close();
    return urls.slice(0, Math.max(count, 50));
  } catch (error) {
    console.log(`  [警告] 浏览器兜底失败: ${error.message}`);
    return [];
  }
}

function guessExt(url) {
  const lowered = url.toLowerCase().split('?')[0];
  for (const ext of ['.png', '.jpg', '.jpeg', '.webp', '.gif']) {
    if (lowered.endsWith(ext) || lowered.includes(ext)) {
      return ext;
    }
  }
  return '.jpg';
}

function isSupportedImageData(data) {
  if (!Buffer.isBuffer(data) || data.length < 12) return false;
  const header = data.subarray(0, 12);
  const isJpeg = header[0] === 0xff && header[1] === 0xd8 && header[2] === 0xff;
  const isPng = header.subarray(0, 8).equals(Buffer.from([0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a]));
  const isGif = header.subarray(0, 6).toString('ascii') === 'GIF87a'
    || header.subarray(0, 6).toString('ascii') === 'GIF89a';
  const isWebp = header.subarray(0, 4).toString('ascii') === 'RIFF'
    && header.subarray(8, 12).toString('ascii') === 'WEBP';
  return isJpeg || isPng || isGif || isWebp;
}

function safeFilename(prompt, ext = '.jpg') {
  const promptText = String(prompt || 'image');
  let name = promptText.replace(/[^A-Za-z0-9-]+/g, '_').replace(/^_+|_+$/g, '');
  const lowered = promptText.toLowerCase();
  if (!name || ['logo', 'image'].includes(name.toLowerCase())) {
    if (lowered.includes('logo')) {
      name = 'logo';
    } else if (promptText.includes('头像') || lowered.includes('avatar')) {
      name = 'avatar';
    } else {
      name = 'image';
    }
  }
  return `${name.slice(0, 40)}_${crypto.randomUUID().replaceAll('-', '').slice(0, 8)}${ext}`;
}

async function downloadImage(sourceUrl, saveDir, filename) {
  const savePath = path.join(saveDir, filename);
  try {
    const response = await fetch(sourceUrl, {
      headers: {
        'User-Agent': HEADERS['User-Agent'],
        Referer: 'https://image.baidu.com/',
      },
      signal: AbortSignal.timeout(BAIDU_DOWNLOAD_TIMEOUT),
    });
    if (!response.ok) {
      throw new Error(`HTTP ${response.status}`);
    }
    const data = Buffer.from(await response.arrayBuffer());
    if (data.length < 1000) {
      console.log(`  [跳过] 图片过小(${data.length}B): ${sourceUrl.slice(0, 80)}`);
      return null;
    }
    if (!isSupportedImageData(data)) {
      console.log(`  [跳过] 文件不是支持的图片格式: ${sourceUrl.slice(0, 80)}`);
      return null;
    }
    await fs.writeFile(savePath, data);
    console.log(`  [下载] ${savePath} (${data.length} B)`);
    return savePath;
  } catch (error) {
    console.log(`  [失败] 下载失败: ${error.message} - ${sourceUrl.slice(0, 80)}`);
    return null;
  }
}

async function fileSha256(filePath) {
  const data = await fs.readFile(filePath);
  return crypto.createHash('sha256').update(data).digest('hex');
}

function contentTypeFor(fileName) {
  const lowered = fileName.toLowerCase();
  if (lowered.endsWith('.png')) return 'image/png';
  if (lowered.endsWith('.webp')) return 'image/webp';
  if (lowered.endsWith('.gif')) return 'image/gif';
  return 'image/jpeg';
}

async function loginBackend(apiBase, userName, password, roleType = 1) {
  const response = await fetch(`${apiBase.replace(/\/+$/, '')}/sysUser/signIn`, {
    method: 'POST',
    headers: { ...HEADERS, 'Content-Type': 'application/json' },
    body: JSON.stringify({ userName, passWord: password, roleType }),
    signal: AbortSignal.timeout(20000),
  });
  const data = await response.json();
  if (!data.success || !data.data) {
    throw new Error(`登录失败: ${JSON.stringify(data)}`);
  }
  return String(data.data);
}

async function uploadFile(filePath, uploadUrl, token) {
  const fileName = path.basename(filePath);
  const fileBytes = await fs.readFile(filePath);
  const formData = new FormData();
  formData.append('file', new Blob([fileBytes], { type: contentTypeFor(fileName) }), fileName);
  const headers = {};
  if (token) {
    headers.Authorization = token;
  }
  const response = await fetch(uploadUrl, {
    method: 'POST',
    headers,
    body: formData,
    signal: AbortSignal.timeout(30000),
  });
  const text = await response.text();
  let result;
  try {
    result = JSON.parse(text);
  } catch {
    throw new Error(`上传接口返回非 JSON: ${text.slice(0, 300)}`);
  }
  const data = Array.isArray(result) ? result : result.data;
  if (Array.isArray(data) && data[0]?.url) {
    return data[0].url;
  }
  throw new Error(`上传接口未返回 url: ${text.slice(0, 300)}`);
}

async function searchDownloadUpload(prompt, count, uploadUrl, token) {
  const tmpDir = await fs.mkdtemp(path.join(os.tmpdir(), 'baidu-fill-images-'));
  const uploaded = [];
  const seenHashes = new Set();
  try {
    const sourceUrls = await searchBaiduImages(prompt, count);
    if (sourceUrls.length < count) {
      console.log(`  [警告] 候选图不足：需要 ${count} 张，得到 ${sourceUrls.length} 张`);
    }
    for (const sourceUrl of sourceUrls) {
      if (uploaded.length >= count) break;
      const filename = safeFilename(prompt, guessExt(sourceUrl));
      const localPath = await downloadImage(sourceUrl, tmpDir, filename);
      if (!localPath) continue;
      const imageHash = await fileSha256(localPath);
      if (seenHashes.has(imageHash)) {
        console.log(`  [跳过] 图片内容重复: ${sourceUrl.slice(0, 80)}`);
        continue;
      }
      seenHashes.add(imageHash);
      try {
        const systemUrl = await uploadFile(localPath, uploadUrl, token);
        uploaded.push(systemUrl);
        console.log(`  [上传] ${systemUrl}`);
      } catch (error) {
        console.log(`  [上传失败] ${error.message}`);
      }
      await new Promise((resolve) => setTimeout(resolve, 300));
    }
  } finally {
    await fs.rm(tmpDir, { recursive: true, force: true });
  }
  return uploaded;
}

function resolveApiBase(args) {
  return args.apiBase || process.env.BACKEND_API_BASE || '';
}

function resolveUploadUrl(args) {
  if (args.uploadUrl) return args.uploadUrl;
  const apiBase = resolveApiBase(args);
  if (!apiBase) {
    throw new Error('请传入 --api-base 或 --upload-url，或设置 BACKEND_API_BASE 环境变量');
  }
  return `${apiBase.replace(/\/+$/, '')}/File/batchUpload`;
}

async function resolveToken(args) {
  if (args.token) return args.token;
  if (args.adminUser && args.adminPassword) {
    const apiBase = resolveApiBase(args);
    if (!apiBase) {
      throw new Error('登录获取 token 时请传入 --api-base，或设置 BACKEND_API_BASE 环境变量');
    }
    console.log(`登录后端获取上传 token: ${apiBase}`);
    return await loginBackend(apiBase, args.adminUser, args.adminPassword, args.roleType);
  }
  return null;
}

async function main() {
  const args = parseArgs(process.argv.slice(2));
  if (args.help) {
    printHelp();
    return;
  }
  if (!args.prompt) {
    printHelp();
    process.exitCode = 1;
    return;
  }
  const uploadUrl = resolveUploadUrl(args);
  const token = await resolveToken(args);
  console.log(`搜索提示词: ${args.prompt}`);
  console.log(`上传接口: ${uploadUrl}`);
  const uploaded = await searchDownloadUpload(args.prompt, args.count, uploadUrl, token);

  console.log('\n系统图片地址:');
  for (const url of uploaded) {
    console.log(url);
  }

  if (uploaded.length < args.count) {
    console.log(`未获取到足够图片：需要 ${args.count} 张，成功 ${uploaded.length} 张；请调整提示词或稍后重试。`);
    process.exitCode = 1;
  }
}

main().catch((error) => {
  console.error(error.message || error);
  process.exitCode = 1;
});
