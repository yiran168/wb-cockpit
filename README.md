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
| **毫秒级时间精度** — `expires_at` 与 Cockpit 内部的毫秒时间戳完全一致 | **Millisecond Precision** — `expires_at` written as ms timestamps matching Cockpit's internal format |
| **纯前端** — 零依赖，零上传，所有转换在浏览器本地完成 | **Pure Frontend** — Zero dependencies, zero uploads, all conversion happens locally |
| **隐私优先** — Token 不会发送到任何服务器 | **Privacy First** — Tokens never leave your browser |
| **优雅设计** — Apple 风格浅色界面，自动适配深色模式 | **Elegant Design** — Apple-style light UI with automatic dark mode |
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

### 网关导出格式 / Gateway Export Format

[workbuddy2api-panel](https://github.com/linguo2625469/workbuddy2api-panel) 与
[workbuddy2api-hub](https://github.com/ardeyouxipianyi/workbuddy2api-hub) 的
「导出账号」格式。键名 camelCase，`expiresAt` 为 Unix **秒**。

```json
{
  "format": "workbuddy-accounts",
  "version": 1,
  "count": 1,
  "accounts": [
    {
      "uid": "abb90fa7-xxxx",
      "nickname": "63139186",
      "domain": "www.workbuddy.cn",
      "realm": "cn",
      "platform": "CLI",
      "enterpriseId": "",
      "accessToken": "eyJ...",
      "refreshToken": "eyJ...",
      "expiresAt": 1794908736
    }
  ]
}
```

裸数组（`[{...}]`）与单个对象同样支持。

### 字段映射 / Field Mapping

| WorkBuddy | Cockpit Tools | 网关导出 / Gateway | 说明 / Description |
|-----------|---------------|--------------------|---------------------|
| `email` | `email` | `email` | 直接映射 / Direct mapping |
| `uid` | `uid` | `uid` | 直接映射 / Direct mapping |
| `access_token` | `access_token` | `accessToken` | 键名随目标格式切换 / Key name follows target |
| `refresh_token` | `refresh_token` | `refreshToken` | 键名随目标格式切换 / Key name follows target |
| `expires_at` (string) | `expires_at` (ms) | `expiresAt` (s) | 三者互转，按量级自动识别秒/毫秒 / Auto-detects s vs ms by magnitude |
| — | `id` | `uid` | Cockpit 的 `id` 取 `uid` / Cockpit `id` mirrors `uid` |
| — | `token_type` | — | 固定 `"Bearer"` / Fixed `"Bearer"` |
| — | `nickname` | `nickname` | 取 `nickname`，缺省回落 `email` / Falls back to `email` |
| — | `created_at` / `last_used` | — | 当前 Unix 秒时间戳 / Current Unix timestamp (seconds) |
| — | `status` | `enabled` | `"normal"` / `true` |
| — | — | `realm` | `cn` ↔ `global`（hub 用 `intl`）/ Mapped between the two spellings |
| — | — | `enterpriseId` | 企业版标识透传 / Passed through |

### 三种格式互转 / Three-way Conversion

| 输入 / Input | 输出 / Output | 用途 / Use case |
|--------------|---------------|-----------------|
| Cockpit Tools | 网关导入格式 | 把 Cockpit 的号搬进两个网关 / Move Cockpit accounts into either gateway |
| 网关导出 | Cockpit Tools | 把网关的号搬进 Cockpit / Move gateway accounts into Cockpit |
| WorkBuddy 导出 | Cockpit Tools | 原始用途 / Original use case |

时间单位说明：Cockpit 用**毫秒**，两个网关用**秒**。转换时按量级（阈值 `1e11`）
自动识别，无需手工换算。输出的 JSON 可直接粘进两个网关的「导入账号」。

---

## 技术 / Tech

- 纯 HTML/CSS/JS，零依赖 / Pure HTML/CSS/JS, zero dependencies
- 内置 MD5 实现（RFC 1321）/ Built-in MD5 implementation (RFC 1321)
- GitHub Pages 托管 / Hosted on GitHub Pages
- Apple 风格设计：系统字体、自动深色模式、iOS 分段控件 / Apple-style design: system fonts, automatic dark mode, iOS segmented control
- AI 生成的动漫主视觉插画 / AI-generated anime key-visual artwork
- 支持中英文切换 / Chinese and English language support

---

## 许可证 / License

[MIT](LICENSE)
