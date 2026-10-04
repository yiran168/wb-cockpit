# WorkBuddy ⇄ Cockpit Tools Converter

<div align="center">

**在 WorkBuddy 导出格式与 Cockpit Tools 账号格式之间自由转换**

Convert between WorkBuddy export format and Cockpit Tools account format.

<p>
  <a href="#功能特性--features">功能特性 / Features</a> •
  <a href="#使用方法--usage">使用方法 / Usage</a> •
  <a href="#格式说明--format">格式说明 / Format</a> •
  <a href="#许可证--license">许可证 / License</a>
</p>

</div>

---

## 为什么做这个 / Why

**中文：** WorkBuddy 和 [Cockpit Tools](https://github.com/jlcodes99/cockpit-tools) 是两个优秀的 AI IDE 账号管理工具，但它们的账号导出格式不同。这个工具让你可以在两种格式之间自由转换，无需手动编辑 JSON。

**English:** WorkBuddy and [Cockpit Tools](https://github.com/jlcodes99/cockpit-tools) are both excellent AI IDE account managers, but they use different export formats. This tool lets you convert between the two formats without manually editing JSON.

---

## 功能特性 / Features

| 中文 | English |
|------|---------|
| **双向转换** — WorkBuddy → Cockpit Tools，或反向 | **Bidirectional** — WorkBuddy → Cockpit Tools or vice versa |
| **自动识别** — 粘贴 JSON 后自动检测格式方向 | **Auto Detect** — Paste JSON and it auto-detects the format |
| **时间格式处理** — `expires_at` 在日期字符串和 Unix 时间戳之间自动转换 | **Smart Date Handling** — `expires_at` auto-converts between date strings and Unix timestamps |
| **纯前端** — 零依赖，零上传，所有转换在浏览器本地完成 | **Pure Frontend** — Zero dependencies, zero uploads, all conversion happens locally |
| **隐私优先** — Token 不会发送到任何服务器 | **Privacy First** — Tokens never leave your browser |
| **MIT 协议** — 随意使用、修改、分发 | **MIT License** — Use, modify, and distribute freely |

---

## 使用方法 / Usage

### 在线使用 / Online

访问 GitHub Pages 站点，粘贴 JSON 即可。

Visit the GitHub Pages site and paste your JSON.

**https://yiran168.github.io/wb-cockpit/**

### 本地使用 / Local

```bash
# 克隆仓库 / Clone the repo
git clone https://github.com/yiran168/wb-cockpit.git
cd wb-cockpit

# 直接用浏览器打开 / Open in browser
open index.html
```

### 作为库使用 / As a Library

```javascript
const result = WorkBuddyConverter.convert(jsonString);
// result.direction: "wb2cockpit" | "cockpit2wb"
// result.data: 转换后的数组 / Converted array
```

---

## 格式说明 / Format

### WorkBuddy 导出格式 / WorkBuddy Export Format

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

### Cockpit Tools 格式 / Cockpit Tools Format

```json
[
  {
    "id": "workbuddy_xxxx",
    "email": "63139186",
    "uid": "abb90fa7-xxxx",
    "access_token": "eyJ...",
    "refresh_token": "eyJ...",
    "token_type": "Bearer",
    "expires_at": 1794908736000,
    "nickname": "63139186",
    "created_at": 1791025393,
    "last_used": 1791025393,
    "status": "normal"
  }
]
```

### 字段映射 / Field Mapping

| WorkBuddy | Cockpit Tools | 说明 / Description |
|-----------|---------------|---------------------|
| `email` | `email` | 直接映射 / Direct mapping |
| `uid` | `uid` | 直接映射 / Direct mapping |
| `access_token` | `access_token` | 直接映射 / Direct mapping |
| `refresh_token` | `refresh_token` | 直接映射 / Direct mapping |
| `expires_at` (string) | `expires_at` (i64) | 日期字符串 ↔ Unix 毫秒时间戳 / Date string ↔ Unix timestamp (ms) |
| — | `id` | `workbuddy_` + MD5(uid 小写；无 uid 时用含 @ 的小写 email) / MD5 of lowercased uid, else lowercased email with @ |
| — | `token_type` | 固定 `"Bearer"` / Fixed `"Bearer"` |
| — | `nickname` | 取 `email` 值 / Uses `email` value |
| — | `created_at` / `last_used` | 当前 Unix 秒时间戳 / Current Unix timestamp (seconds) |
| — | `status` | 固定 `"normal"` / Fixed `"normal"` |

---

## 技术 / Tech

- 纯 HTML/CSS/JS，零依赖 / Pure HTML/CSS/JS, zero dependencies
- 内置 MD5 实现（RFC 1321）/ Built-in MD5 implementation (RFC 1321)
- GitHub Pages 托管 / Hosted on GitHub Pages
- AI 生成的二次元风格背景 / AI-generated anime-style background
- 支持中英文切换 / Chinese and English language support

---

## 许可证 / License

[MIT](LICENSE)
