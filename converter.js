/**
 * WorkBuddy <-> Cockpit Tools Account Format Converter
 * Zero-dependency, runs in browser.
 */

(function (global) {
  "use strict";

  // ── Helpers ──────────────────────────────────────────────

  function pad2(n) { return n < 10 ? "0" + n : "" + n; }

  var MS_THRESHOLD = 1e11; // ~ year 5138 in seconds; anything above is milliseconds

  /**
   * Normalize a Unix timestamp to SECONDS.
   * Cockpit stores expires_at in milliseconds; created_at/last_used in seconds.
   * Heuristic mirrors Cockpit's token_expiry_at: value < 1e11 => seconds.
   */
  function normalizeToSeconds(ts) {
    if (typeof ts === "string" && /^\d+$/.test(ts.trim())) ts = parseInt(ts.trim(), 10);
    if (typeof ts !== "number" || isNaN(ts)) return null;
    return ts > MS_THRESHOLD ? Math.floor(ts / 1000) : Math.floor(ts);
  }

  /**
   * Normalize any expires_at-ish value (number, numeric string, date string)
   * to milliseconds. Returns null on failure.
   */
  function normalizeToMs(value) {
    if (value === null || value === undefined) return null;
    if (typeof value === "number" && !isNaN(value)) {
      return value > MS_THRESHOLD ? Math.floor(value) : Math.floor(value * 1000);
    }
    if (typeof value === "string") {
      var trimmed = value.trim();
      if (!trimmed) return null;
      if (/^\d+$/.test(trimmed)) {
        var n = parseInt(trimmed, 10);
        return n > MS_THRESHOLD ? n : n * 1000;
      }
      // ISO strings with explicit timezone (Z or ±hh:mm) must NOT be parsed as local time
      if (/[zZ]$|[+-]\d{2}:?\d{2}$/.test(trimmed)) {
        var zoned = Date.parse(trimmed);
        return isNaN(zoned) ? null : Math.floor(zoned);
      }
      var parsed = parseDateStringToTs(trimmed);
      if (parsed !== null) return parsed * 1000;
      var iso = Date.parse(trimmed);
      if (!isNaN(iso)) return Math.floor(iso);
    }
    return null;
  }

  /**
   * Parse "YYYY-MM-DD HH:MM:SS" -> Unix timestamp (seconds).
   * Treats input as local time (not UTC) to match WorkBuddy behavior.
   * Returns null on failure.
   */
  function parseDateStringToTs(str) {
    if (!str || typeof str !== "string") return null;
    var m = str.trim().match(/^(\d{4})-(\d{2})-(\d{2})[T ](\d{2}):(\d{2}):(\d{2})/);
    if (!m) return null;
    // Use local time, not UTC — WorkBuddy exports local time
    var d = new Date(+m[1], +m[2] - 1, +m[3], +m[4], +m[5], +m[6]);
    var ts = Math.floor(d.getTime() / 1000);
    return isNaN(ts) ? null : ts;
  }

  /**
   * Unix timestamp (seconds or milliseconds, number or numeric string)
   * -> "YYYY-MM-DD HH:MM:SS" (local time).
   */
  function tsToDateString(ts) {
    var seconds = normalizeToSeconds(ts);
    if (seconds === null) return null;
    var d = new Date(seconds * 1000);
    if (isNaN(d.getTime())) return null;
    return d.getFullYear() + "-" + pad2(d.getMonth() + 1) + "-" + pad2(d.getDate()) +
      " " + pad2(d.getHours()) + ":" + pad2(d.getMinutes()) + ":" + pad2(d.getSeconds());
  }

  /**
   * MD5 hash — returns 32-char hex string.
   * Well-tested implementation using unsigned 32-bit math.
   */
  function md5(str) {
    /* eslint-disable no-bitwise */
    function safeAdd(x, y) {
      var lsw = (x & 0xFFFF) + (y & 0xFFFF);
      var msw = (x >> 16) + (y >> 16) + (lsw >> 16);
      return (msw << 16) | (lsw & 0xFFFF);
    }
    function bitRol(num, cnt) { return (num << cnt) | (num >>> (32 - cnt)); }
    function md5cmn(q, a, b, x, s, t) { return safeAdd(bitRol(safeAdd(safeAdd(a, q), safeAdd(x, t)), s), b); }
    function md5ff(a, b, c, d, x, s, t) { return md5cmn((b & c) | (~b & d), a, b, x, s, t); }
    function md5gg(a, b, c, d, x, s, t) { return md5cmn((b & d) | (c & ~d), a, b, x, s, t); }
    function md5hh(a, b, c, d, x, s, t) { return md5cmn(b ^ c ^ d, a, b, x, s, t); }
    function md5ii(a, b, c, d, x, s, t) { return md5cmn(c ^ (b | ~d), a, b, x, s, t); }

    function strToBinl(str) {
      var bin = [], i;
      for (i = 0; i < str.length * 8; i += 8) {
        bin[i >> 5] = (bin[i >> 5] || 0) | ((str.charCodeAt(i / 8) & 0xFF) << (i % 32));
      }
      return bin;
    }
    function binlToHex(binarray) {
      var hexTab = "0123456789abcdef", str = "", i;
      for (i = 0; i < binarray.length * 4; i++) {
        str += hexTab.charAt((binarray[i >> 2] >> ((i % 4) * 8 + 4)) & 0xF) +
               hexTab.charAt((binarray[i >> 2] >> ((i % 4) * 8)) & 0xF);
      }
      return str;
    }
    function utf8Encode(str) {
      return unescape(encodeURIComponent(str));
    }

    str = utf8Encode(str);
    var x = strToBinl(str);
    x[str.length * 8 >> 5] = (x[str.length * 8 >> 5] || 0) | 0x80 << (str.length * 8 % 32);
    x[(((str.length * 8 + 64) >>> 9) << 4) + 14] = str.length * 8;

    var a = 1732584193, b = -271733879, c = -1732584194, d = 271733878;

    for (var i = 0; i < x.length; i += 16) {
      var olda = a, oldb = b, oldc = c, oldd = d;
      a = md5ff(a, b, c, d, x[i + 0], 7, -680876936); d = md5ff(d, a, b, c, x[i + 1], 12, -389564586);
      c = md5ff(c, d, a, b, x[i + 2], 17, 606105819); b = md5ff(b, c, d, a, x[i + 3], 22, -1044525330);
      a = md5ff(a, b, c, d, x[i + 4], 7, -176418897); d = md5ff(d, a, b, c, x[i + 5], 12, 1200080426);
      c = md5ff(c, d, a, b, x[i + 6], 17, -1473231341); b = md5ff(b, c, d, a, x[i + 7], 22, -45705983);
      a = md5ff(a, b, c, d, x[i + 8], 7, 1770035416); d = md5ff(d, a, b, c, x[i + 9], 12, -1958414417);
      c = md5ff(c, d, a, b, x[i + 10], 17, -42063); b = md5ff(b, c, d, a, x[i + 11], 22, -1990404162);
      a = md5ff(a, b, c, d, x[i + 12], 7, 1804603682); d = md5ff(d, a, b, c, x[i + 13], 12, -40341101);
      c = md5ff(c, d, a, b, x[i + 14], 17, -1502002290); b = md5ff(b, c, d, a, x[i + 15], 22, 1236535329);

      a = md5gg(a, b, c, d, x[i + 1], 5, -165796510); d = md5gg(d, a, b, c, x[i + 6], 9, -1069501632);
      c = md5gg(c, d, a, b, x[i + 11], 14, 643717713); b = md5gg(b, c, d, a, x[i + 0], 20, -373897302);
      a = md5gg(a, b, c, d, x[i + 5], 5, -701558691); d = md5gg(d, a, b, c, x[i + 10], 9, 38016083);
      c = md5gg(c, d, a, b, x[i + 15], 14, -660478335); b = md5gg(b, c, d, a, x[i + 4], 20, -405537848);
      a = md5gg(a, b, c, d, x[i + 9], 5, 568446438); d = md5gg(d, a, b, c, x[i + 14], 9, -1019803690);
      c = md5gg(c, d, a, b, x[i + 3], 14, -187363961); b = md5gg(b, c, d, a, x[i + 8], 20, 1163531501);
      a = md5gg(a, b, c, d, x[i + 13], 5, -1444681467); d = md5gg(d, a, b, c, x[i + 2], 9, -51403784);
      c = md5gg(c, d, a, b, x[i + 7], 14, 1735328473); b = md5gg(b, c, d, a, x[i + 12], 20, -1926607734);

      a = md5hh(a, b, c, d, x[i + 5], 4, -378558); d = md5hh(d, a, b, c, x[i + 8], 11, -2022574463);
      c = md5hh(c, d, a, b, x[i + 11], 16, 1839030562); b = md5hh(b, c, d, a, x[i + 14], 23, -35309556);
      a = md5hh(a, b, c, d, x[i + 1], 4, -1530992060); d = md5hh(d, a, b, c, x[i + 4], 11, 1272893353);
      c = md5hh(c, d, a, b, x[i + 7], 16, -155497632); b = md5hh(b, c, d, a, x[i + 10], 23, -1094730640);
      a = md5hh(a, b, c, d, x[i + 13], 4, 681279174); d = md5hh(d, a, b, c, x[i + 0], 11, -358537222);
      c = md5hh(c, d, a, b, x[i + 3], 16, -722521979); b = md5hh(b, c, d, a, x[i + 6], 23, 76029189);
      a = md5hh(a, b, c, d, x[i + 9], 4, -640364487); d = md5hh(d, a, b, c, x[i + 12], 11, -421815835);
      c = md5hh(c, d, a, b, x[i + 15], 16, 530742520); b = md5hh(b, c, d, a, x[i + 2], 23, -995338651);

      a = md5ii(a, b, c, d, x[i + 0], 6, -198630844); d = md5ii(d, a, b, c, x[i + 7], 10, 1126891415);
      c = md5ii(c, d, a, b, x[i + 14], 15, -1416354905); b = md5ii(b, c, d, a, x[i + 5], 21, -57434055);
      a = md5ii(a, b, c, d, x[i + 12], 6, 1700485571); d = md5ii(d, a, b, c, x[i + 3], 10, -1894986606);
      c = md5ii(c, d, a, b, x[i + 10], 15, -1051523); b = md5ii(b, c, d, a, x[i + 1], 21, -2054922799);
      a = md5ii(a, b, c, d, x[i + 8], 6, 1873313359); d = md5ii(d, a, b, c, x[i + 15], 10, -30611744);
      c = md5ii(c, d, a, b, x[i + 6], 15, -1560198380); b = md5ii(b, c, d, a, x[i + 13], 21, 1309151649);
      a = md5ii(a, b, c, d, x[i + 4], 6, -145523070); d = md5ii(d, a, b, c, x[i + 11], 10, -1120210379);
      c = md5ii(c, d, a, b, x[i + 2], 15, 718787259); b = md5ii(b, c, d, a, x[i + 9], 21, -343485551);

      a = safeAdd(a, olda); b = safeAdd(b, oldb);
      c = safeAdd(c, oldc); d = safeAdd(d, oldd);
    }

    return binlToHex([a, b, c, d]);
    /* eslint-enable no-bitwise */
  }

  // ── Detect format ────────────────────────────────────────

  /**
   * Check if an object looks like a WorkBuddy export item.
   * Key signal: expires_at is a date string like "2026-11-17 17:45:36".
   */
  function isWorkBuddyItem(item) {
    if (!item || typeof item !== "object") return false;
    if (!("access_token" in item)) return false;
    // WorkBuddy: expires_at is a date string with dashes
    if ("expires_at" in item && typeof item.expires_at === "string" && item.expires_at.indexOf("-") !== -1) return true;
    // expires_at present as a number (or numeric string) => Cockpit Unix ts, not WorkBuddy
    if ("expires_at" in item && item.expires_at !== null &&
        (typeof item.expires_at === "number" || /^\d+$/.test(String(item.expires_at).trim()))) return false;
    // Has email + uid but no Cockpit-specific fields
    if ("email" in item && "uid" in item &&
        !("id" in item) && !("auth_raw" in item) && !("created_at" in item) && !("token_type" in item)) return true;
    // Has access_token + email, no Cockpit fields
    if ("email" in item &&
        !("id" in item) && !("auth_raw" in item) && !("created_at" in item) && !("token_type" in item) &&
        !("nickname" in item) && !("profile_raw" in item)) return true;
    return false;
  }

  /**
   * Check if an object looks like a Cockpit Tools export item.
   * Key signals: id field, auth_raw/created_at/token_type, or numeric expires_at.
   */
  function isCockpitItem(item) {
    if (!item || typeof item !== "object") return false;
    if (!("access_token" in item)) return false;
    // Has id field (any id, not just workbuddy_ prefix)
    if ("id" in item && typeof item.id === "string") return true;
    // Has Cockpit-specific metadata fields
    if ("auth_raw" in item || "profile_raw" in item || "usage_raw" in item) return true;
    if ("created_at" in item) return true;
    if ("token_type" in item) return true;
    if ("nickname" in item) return true;
    // Has numeric expires_at (Cockpit uses Unix timestamps; number or numeric string)
    if ("expires_at" in item && item.expires_at !== null &&
        (typeof item.expires_at === "number" || /^\d+$/.test(String(item.expires_at).trim()))) return true;
    return false;
  }

  function looksLikeWorkBuddyExport(obj) {
    if (!Array.isArray(obj) || obj.length === 0) return false;
    // Check ALL items, not just the first
    for (var i = 0; i < obj.length; i++) {
      if (!isWorkBuddyItem(obj[i])) return false;
    }
    return true;
  }

  function looksLikeCockpitExport(obj) {
    if (!Array.isArray(obj) || obj.length === 0) return false;
    // Check ALL items, not just the first
    for (var i = 0; i < obj.length; i++) {
      if (!isCockpitItem(obj[i])) return false;
    }
    return true;
  }

  // ── WorkBuddy -> Cockpit Tools ───────────────────────────

  function nonEmpty(value) {
    if (typeof value === "number" && isFinite(value)) value = String(value);
    if (typeof value !== "string") return null;
    var t = value.trim();
    return t ? t : null;
  }

  /**
   * Identity seed — replicates Cockpit Tools' own algorithm
   * (workbuddy_account.rs): uid (trimmed, lowercased) first; otherwise email
   * (trimmed, lowercased) ONLY when it contains '@'; otherwise "workbuddy_user".
   */
  function identitySeed(item) {
    var uid = nonEmpty(item.uid);
    if (uid) return uid.toLowerCase();
    var email = nonEmpty(item.email);
    if (email) {
      var lowered = email.toLowerCase();
      if (lowered.indexOf("@") !== -1) return lowered;
    }
    return "workbuddy_user";
  }

  function workbuddyToCockpit(wbArray) {
    var now = Math.floor(Date.now() / 1000);
    var usedIds = {};
    return wbArray.map(function (item) {
      var id = "workbuddy_" + md5(identitySeed(item));
      // Keep IDs unique within the batch (Cockpit would collapse identical seeds)
      if (usedIds[id]) {
        var n = usedIds[id] + 1;
        usedIds[id] = n;
        id = id + "_" + n;
      } else {
        usedIds[id] = 1;
      }
      var ms = normalizeToMs(item.expires_at);
      var email = nonEmpty(item.email);

      var account = {
        id: id,
        email: email || "",
        access_token: nonEmpty(item.access_token) || "",
        token_type: "Bearer",
        created_at: now,
        last_used: now,
        status: "normal"
      };
      var uid = nonEmpty(item.uid);
      if (uid) account.uid = uid;
      var rt = nonEmpty(item.refresh_token);
      if (rt) account.refresh_token = rt;
      if (ms !== null) account.expires_at = ms;
      if (email) account.nickname = email;

      return account;
    });
  }

  // ── Gateway (workbuddy-accounts) <-> Cockpit Tools ────────
  //
  // 网关导出格式（workbuddy2api-panel / workbuddy2api-hub 的「导出账号」），
  // 两种容器都认：{format:"workbuddy-accounts",accounts:[...]} 与裸数组。
  // 键名 camelCase：accessToken / refreshToken / expiresAt / realm / enterpriseId。
  //
  // 时间单位：两个网关都用 Unix 秒写 expiresAt，Cockpit 用毫秒 —— 转换时按
  // MS_THRESHOLD 归一（与 normalizeToMs / normalizeToSeconds 同一口径）。

  /**
   * Cockpit item -> 网关导入格式（camelCase 平铺对象）。
   * 输出的数组可直接粘进两个网关的「导入账号」。
   */
  function cockpitToGateway(cockpitArray) {
    return cockpitArray.map(function (item) {
      var row = {};
      var uid = nonEmpty(item.uid);
      if (uid) row.uid = uid;

      var email = nonEmpty(item.email);
      var nickname = nonEmpty(item.nickname) || email;
      if (nickname) row.nickname = nickname;
      if (email) row.email = email;

      var domain = nonEmpty(item.domain);
      if (domain) row.domain = domain;

      var at = nonEmpty(item.access_token);
      if (at) row.accessToken = at;
      var rt = nonEmpty(item.refresh_token);
      if (rt) row.refreshToken = rt;

      // Cockpit 毫秒 -> 网关秒。两个网关都自带毫秒兜底（panel 的
      // secondsMagnitude / hub 的 normalize_epoch），写秒最稳。
      var seconds = normalizeToSeconds(item.expires_at);
      if (seconds !== null) row.expiresAt = seconds;

      var realm = nonEmpty(item.realm);
      if (realm === "global") realm = "intl"; // hub 用 intl，panel 用 global
      if (realm) row.realm = realm;

      var ent = nonEmpty(item.enterprise_id) || nonEmpty(item.enterpriseId);
      if (ent) row.enterpriseId = ent;

      row.platform = "CLI";
      row.enabled = true;
      row.source = "cockpit";
      return row;
    });
  }

  /**
   * 网关导出条目 -> Cockpit 导入格式（snake_case）。
   * 兼容两种键名（camelCase 与 snake_case）与嵌套形（{auth,account}）。
   */
  function gatewayToCockpit(gwArray) {
    var now = Math.floor(Date.now() / 1000);
    return gwArray.map(function (item) {
      var auth = item.auth && typeof item.auth === "object" ? item.auth : {};
      var acct = item.account && typeof item.account === "object" ? item.account : {};

      // 逐层取值：平铺层 -> auth -> account（与 hub 的 pick() 同口径）。
      function pick(key) {
        var layers = [item, auth, acct];
        for (var i = 0; i < layers.length; i++) {
          var v = nonEmpty(layers[i][key]);
          if (v) return v;
          var alt = nonEmpty(layers[i][snake(key)]);
          if (alt) return alt;
        }
        return null;
      }
      function snake(key) {
        return key.replace(/([A-Z])/g, function (m) { return "_" + m.toLowerCase(); });
      }

      var uid = pick("uid");
      var accessToken = pick("accessToken");
      var refreshToken = pick("refreshToken");

      var result = {};
      if (uid) {
        result.id = uid;
        result.uid = uid;
      }
      var email = pick("email");
      if (email) result.email = email;
      var nickname = pick("nickname") || email || uid;
      if (nickname) result.nickname = nickname;
      if (accessToken) result.access_token = accessToken;
      if (refreshToken) result.refresh_token = refreshToken;
      result.token_type = "Bearer";

      // 网关秒 -> Cockpit 毫秒。
      var seconds = normalizeToSeconds(pick("expiresAt"));
      result.expires_at = seconds !== null ? seconds * 1000 : (now + 365 * 24 * 3600) * 1000;

      var domain = pick("domain");
      if (domain) result.domain = domain;
      result.status = "normal";
      result.created_at = now;
      result.last_used = now;
      return result;
    });
  }

  // ── Gateway 导出格式识别 ──────────────────────────────────

  /**
   * 条目是否为网关导出格式（camelCase 键）。
   * 判据：带 accessToken（camelCase）或 JWT 形态的 access_token，
   * 且不带 Cockpit 特有元字段（id / created_at / token_type）。
   */
  function isGatewayItem(item) {
    if (!item || typeof item !== "object") return false;

    // 嵌套形桌面端凭据：{auth:{accessToken...},account:{uid...}}
    var auth = item.auth && typeof item.auth === "object" ? item.auth : null;
    if (auth && nonEmpty(auth.accessToken) !== null) return true;

    var at = nonEmpty(item.accessToken);
    if (at === null) return false;

    // Cockpit 导出是 snake_case 且带 id/created_at/token_type；网关导出没有。
    // 两者都带 access_token 时按这些元字段区分。
    var hasCockpitMeta = !!(item.id || item.token_type || item.created_at);
    var hasSnakeToken = nonEmpty(item.access_token) !== null;
    if (hasSnakeToken && !nonEmpty(item.accessToken)) return false; // 纯 Cockpit
    if (hasCockpitMeta && hasSnakeToken && !nonEmpty(item.accessToken)) return false;

    // camelCase accessToken 存在 = 网关导出（或手工平铺）
    return true;
  }

  function looksLikeGatewayExport(obj) {
    if (!Array.isArray(obj) || obj.length === 0) return false;
    for (var i = 0; i < obj.length; i++) {
      if (!isGatewayItem(obj[i])) return false;
    }
    return true;
  }

  // ── Auto-detect & convert ────────────────────────────────

  /**
   * 解出账号数组：兼容裸数组、{accounts:[...]}、{items:[...]}、单对象。
   * 返回 {rows, format}；format 为 "gateway" 时表示带 workbuddy-accounts 包装。
   */
  function extractRows(parsed) {
    if (Array.isArray(parsed)) return { rows: parsed, format: "" };

    if (parsed && typeof parsed === "object") {
      if (Array.isArray(parsed.accounts)) {
        // {"format":"workbuddy-accounts", accounts:[...]} —— 网关导出容器
        var gw = String(parsed.format || "").indexOf("workbuddy-accounts") === 0;
        return { rows: parsed.accounts, format: gw ? "gateway" : "" };
      }
      if (Array.isArray(parsed.items)) return { rows: parsed.items, format: "" };
      return { rows: [parsed], format: "" };
    }
    return { rows: [], format: "" };
  }

  function convert(jsonString) {
    var parsed;
    try { parsed = JSON.parse(jsonString); }
    catch (e) { return { error: "Invalid JSON: " + e.message }; }

    var extracted = extractRows(parsed);
    var rows = extracted.rows;
    if (rows.length === 0) {
      return { error: "Unrecognized format. Expected WorkBuddy export or Cockpit Tools export." };
    }

    // 网关导出（workbuddy-accounts 包装或 camelCase 裸数组）→ Cockpit
    if (extracted.format === "gateway" || looksLikeGatewayExport(rows)) {
      return { direction: "gw2cockpit", data: gatewayToCockpit(rows) };
    }
    if (looksLikeWorkBuddyExport(rows)) {
      return { direction: "wb2cockpit", data: workbuddyToCockpit(rows) };
    }
    if (looksLikeCockpitExport(rows)) {
      // Cockpit → 网关导入格式（camelCase 秒值），可直接粘进两个网关
      return { direction: "cockpit2wb", data: cockpitToGateway(rows) };
    }
    return { error: "Unrecognized format. Expected WorkBuddy export or Cockpit Tools export." };
  }

  // ── Export ───────────────────────────────────────────────

  var api = { convert: convert, workbuddyToCockpit: workbuddyToCockpit,
              cockpitToWorkbuddy: cockpitToGateway, cockpitToGateway: cockpitToGateway,
              gatewayToCockpit: gatewayToCockpit, extractRows: extractRows,
              parseDateStringToTs: parseDateStringToTs, tsToDateString: tsToDateString, md5: md5,
              identitySeed: identitySeed, normalizeToMs: normalizeToMs, normalizeToSeconds: normalizeToSeconds,
              isWorkBuddyItem: isWorkBuddyItem, isCockpitItem: isCockpitItem, isGatewayItem: isGatewayItem };

  if (typeof module !== "undefined" && module.exports) module.exports = api;
  else global.WorkBuddyConverter = api;

})(typeof window !== "undefined" ? window : this);
