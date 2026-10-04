# WorkBuddy ⇄ Cockpit Tools Converter

<p align="center">
  <strong>在 WorkBuddy 导出格式与 Cockpit Tools 账号格式之间自由转换</strong>
</p>

<p align="center">
  <a href="#features">Features</a> •
  <a href="#usage">Usage</a> •
  <a href="#format">Format</a> •
  <a href="#license">License</a>
</p>

---

## Why

WorkBuddy 和 [Cockpit Tools](https://github.com/jlcodes99/cockpit-tools) 是两个优秀的 AI IDE 账号管理工具，但它们的账号导出格式不同。这个工具让你可以在两种格式之间自由转换，无需手动编辑 JSON。

## Features

- **双向转换** — WorkBuddy → Cockpit Tools，或 Cockpit Tools → WorkBuddy
- **自动识别** — 粘贴 JSON 后自动检测格式方向
- **时间格式处理** — `expires_at` 在日期字符串和 Unix 时间戳之间自动转换
- **纯前端** — 零依赖，零上传，所有转换在浏览器本地完成
- **MIT 协议** — 随意使用、修改、分发

## Usage

### 在线使用

访问 GitHub Pages 站点，粘贴 JSON 即可。

### 本地使用

```bash
# 克隆仓库
git clone https://github.com/YOUR_USERNAME/workbuddy-cockpit-converter.git
cd workbuddy-cockpit-converter

# 直接用浏览器打开 index.html
open index.html
```

### 作为库使用

```javascript
const result = WorkBuddyConverter.convert(jsonString);
// result.direction: "wb2cockpit" | "cockpit2wb"
// result.data: 转换后的数组
```

## Format

### WorkBuddy 导出格式

```json
[
  {
    "email": "63139186",
    "uid": "abb90fa7-xxxx",
    "expires_at": "2026-11-17 17:45:36",
    "access_token": "eyJ...",
    "refresh_token": "eyJ..."
  }
]
```

### Cockpit Tools 格式

```json
[
  {
    "id": "workbuddy_xxxx",
    "email": "63139186",
    "uid": "abb90fa7-xxxx",
    "access_token": "eyJ...",
    "refresh_token": "eyJ...",
    "token_type": "Bearer",
    "expires_at": 1794908736,
    "nickname": "63139186",
    "created_at": 1791025393,
    "last_used": 1791025393,
    "status": "normal"
  }
]
```

### 字段映射

| WorkBuddy | Cockpit Tools | 说明 |
|-----------|---------------|------|
| `email` | `email` | 直接映射 |
| `uid` | `uid` | 直接映射 |
| `access_token` | `access_token` | 直接映射 |
| `refresh_token` | `refresh_token` | 直接映射 |
| `expires_at` (string) | `expires_at` (i64) | 日期字符串 ↔ Unix 时间戳 |
| — | `id` | 自动生成 `workbuddy_` + MD5(uid/email) |
| — | `token_type` | 固定 `"Bearer"` |
| — | `nickname` | 取 `email` 值 |
| — | `created_at` / `last_used` | 当前时间戳 |
| — | `status` | 固定 `"normal"` |

## Tech

- 纯 HTML/CSS/JS，零依赖
- 内置 MD5 实现（RFC 1321）
- GitHub Pages 托管

## License

[MIT](LICENSE)
