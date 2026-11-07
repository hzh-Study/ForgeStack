---
name: baidu-fill-images
description: Find suitable images from Baidu by prompt, upload them through the project's backend file upload API, and return system URLs for API-based data repair.
---
# baidu-fill-images

Use this skill when a project needs to fill or replace image fields. This skill only finds images and uploads them through the backend. Business data must be updated according to the project's data rules: some projects write selected system URLs into SQL seed data, while others update rows through APIs such as `list`, `detail`, or `addOrEdit`.

## What This Skill Does

- Searches Baidu images from a direct prompt such as `双肩包 商品图`, `头像`, `品牌 Logo`, or `商城促销轮播图`.
- Builds a candidate pool of up to 50 source images for each prompt.
- Shuffles the candidate pool before download so repeated runs do not always use the first result.
- Downloads candidate images to a temporary directory.
- Checks downloaded image content hashes so different source URLs that point to the same image are not reused in one run.
- Saves temporary upload filenames with ASCII-safe names such as `logo_xxxxxxxx.jpg`, avoiding Chinese characters in backend URLs.
- Uploads each accepted image through the ForgeStack backend upload API: `POST /admin/files/upload` with multipart fields `file`, `module=skill`, and `fileType=image`.
- Prints only the backend-returned system image URLs for callers to use in SQL seed data or normal business API requests.
- Uses the Node.js script by default so projects do not need Python. Requires Node.js 18+ for built-in `fetch`, `FormData`, and `Blob`.
- If Baidu blocks direct HTTP requests, falls back to a local Playwright browser search when the project has Playwright installed.
- Fails clearly when Baidu cannot provide enough usable images. Do not add hard-coded fallback image URL lists.

## Preferred Workflow

1. Read the project's data-preparation rule first and determine whether final demo data must be written to SQL or through APIs.
2. Query the target business rows through SQL or the project backend API.
3. Determine the exact prompt from the business field:
   - user avatar: `头像`
   - product image: `<商品名称> 商品图`
   - brand logo: `<品牌名称> Logo`
   - banner: `商城促销轮播图`
   - activity image: `<活动名称> 活动图`
4. Run this skill with `--prompt` and `--count` equal to the final needed number. The script builds a candidate pool, shuffles it, downloads random candidates, validates image bytes, removes duplicate hashes, uploads accepted files, and returns system URLs.
5. Write the returned system URLs into the project's SQL records or save them through the corresponding business API, following the project rule. Do not add a manual subjective image-selection step.
6. Re-query or load the page and audit the updated fields, referenced files, duplicate image hashes, and browser image decoding.
7. After successful audit, remove only old uploaded files that are no longer referenced by any business image field.

## Commands

```bash
node .agents/skills/baidu-fill-images/fill_images.mjs --prompt "双肩包 商品图" --count 1 --api-base http://127.0.0.1:7245 --admin-user admin --admin-password 123456789
node .agents/skills/baidu-fill-images/fill_images.mjs --prompt "商城促销轮播图" --count 2 --upload-url http://127.0.0.1:7245/admin/files/upload --token <token>
node .agents/skills/baidu-fill-images/fill_images.mjs --prompt "头像" --count 13 --api-base http://127.0.0.1:7245 --token <token>
```

## Arguments

| Argument | Meaning |
| --- | --- |
| `--prompt` | Required. The Baidu image search prompt. |
| `--count` | Number of uploaded system URLs to return. |
| `--api-base` | Backend API base URL. Required when `--upload-url` is not provided, unless `BACKEND_API_BASE` is set. |
| `--upload-url` | Explicit upload API URL. Defaults to `{api-base}/admin/files/upload` when API base is available. |
| `--token` | Existing backend authorization token; `Bearer` prefix is optional. |
| `--admin-user` | ForgeStack admin account used with `POST /auth/login`. |
| `--admin-password` | ForgeStack admin password used with `--admin-user`. |

## Notes For Agents

- This skill is not responsible for changing business data or SQL. It returns uploaded image URLs only.
- Use `node .agents/skills/baidu-fill-images/fill_images.mjs`; this repository only maintains the Node.js script.
- Do not automatically use the first Baidu result. Let the script build and shuffle a candidate pool, then use the returned uploaded system URLs directly. Do not ask the user to do image screening, and do not add an agent subjective inspection step before assignment.
- Follow the current project's data rule after URLs are returned. If demo data belongs in SQL, write the selected system URL into the corresponding table's SQL record instead of creating a separate data script.
- For user avatar fields, use `头像` as the primary prompt. Do not use the user's real name, account, phone, role, or unrelated business text to search avatar images.
- For product, activity, article, venue, service, or other named business records, build the image prompt from the real record name, for example `<商品名称> 商品图` or `<活动名称> 活动图`. Only use broad category prompts when the row itself has no useful name.
- For many user avatars, search `头像` once with `--count` equal to the number of users, build a candidate pool, deduplicate by image content hash, then assign one uploaded system URL per user. Do not run the same avatar prompt once per row.
- When multiple rows need the same semantic image type, search once with `--count <needed_count>` and distribute the returned URLs. Do not run the same prompt once per row, because that wastes network calls and often returns identical images.
- Before downloading, request up to 50 candidates for the prompt and randomly choose from that pool. Do not always download the first search result, because it makes repeated repair runs visually stale.
- After downloading candidates, validate supported image bytes and compare file content hashes before uploading or assigning. Different source URLs can still point to the same image.
- If the chosen URLs will be stored in SQL, make sure the uploaded files are included in the project runtime resources or otherwise available in the target environment.
- In Windows PowerShell batch scripts, avoid hard-coding many Chinese prompts inside inline here-strings if those prompts are passed to subprocesses. Prefer prompts derived from UTF-8 API responses, UTF-8 script files, Unicode escapes, or English helper prompts, and verify sampled candidate images before saving business data.
- If Baidu is slow in the current network, set `BAIDU_IMAGE_TIMEOUT` to a larger value or retry later. The script may use Playwright as a browser fallback, but must not add static backup image URL lists.
- Do not write Baidu source URLs directly into the database or API payload. Always upload through the backend first and use the returned system URL.
