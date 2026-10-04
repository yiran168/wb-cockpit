/**
 * WorkBuddy <-> Cockpit Tools Account Format Converter
 * Zero-dependency, runs in browser.
 */

(function (global) {
  "use strict";

  // ── Helpers ──────────────────────────────────────────────

  function pad2(n) { return n < 10 ? "0" + n : "" + n; }

  /**
   * Parse "YYYY-MM-DD HH:MM:SS" → Unix timestamp (seconds).
   * Returns null on failure.
   */
  function parseDateStringToTs(str) {
    if (!str || typeof str !== "string") return null;
    var m = str.trim().match(/^(\d{4})-(\d{2})-(\d{2})[T ](\d{2}):(\d{2}):(\d{2})/);
    if (!m) return null;
    var d = new Date(Date.UTC(+m[1], +m[2] - 1, +m[3], +m[4], +m[5], +m[6]));
    var ts = Math.floor(d.getTime() / 1000);
    return isNaN(ts) ? null : ts;
  }

  /**
   * Unix timestamp (seconds) → "YYYY-MM-DD HH:MM:SS" (UTC).
   */
  function tsToDateString(ts) {
    var d = new Date(ts * 1000);
    return d.getUTCFullYear() + "-" + pad2(d.getUTCMonth() + 1) + "-" + pad2(d.getUTCDate()) +
      " " + pad2(d.getUTCHours()) + ":" + pad2(d.getUTCMinutes()) + ":" + pad2(d.getUTCSeconds());
  }

  /**
   * MD5 hash — returns 32-char hex string.
   * Sourced from a well-tested public-domain implementation.
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

  function looksLikeWorkBuddyExport(obj) {
    if (!Array.isArray(obj) || obj.length === 0) return false;
    var item = obj[0];
    return item && typeof item === "object" && "access_token" in item && "expires_at" in item &&
           typeof item.expires_at === "string" && item.expires_at.includes("-");
  }

  function looksLikeCockpitExport(obj) {
    if (!Array.isArray(obj) || obj.length === 0) return false;
    var item = obj[0];
    return item && typeof item === "object" && "access_token" in item &&
           ("id" in item || "auth_raw" in item || "profile_raw" in item);
  }

  // ── WorkBuddy → Cockpit Tools ───────────────────────────

  function workbuddyToCockpit(wbArray) {
    var now = Math.floor(Date.now() / 1000);
    return wbArray.map(function (item) {
      var identity = item.uid || item.email || "workbuddy_user";
      var id = "workbuddy_" + md5(identity);
      var ts = parseDateStringToTs(item.expires_at);

      var account = {
        id: id,
        email: item.email || "",
        access_token: item.access_token || "",
        refresh_token: item.refresh_token || null,
        token_type: "Bearer",
        expires_at: ts,
        nickname: item.email || null,
        created_at: now,
        last_used: now,
        status: "normal"
      };
      if (item.uid) account.uid = item.uid;
      if (ts) account.expires_at = ts;

      return account;
    });
  }

  // ── Cockpit Tools → WorkBuddy ───────────────────────────

  function cockpitToWorkbuddy(cockpitArray) {
    return cockpitArray.map(function (item) {
      var result = {};
      if (item.email) result.email = item.email;
      if (item.uid) result.uid = item.uid;
      if (item.expires_at) result.expires_at = tsToDateString(item.expires_at);
      if (item.access_token) result.access_token = item.access_token;
      if (item.refresh_token) result.refresh_token = item.refresh_token;
      return result;
    });
  }

  // ── Auto-detect & convert ────────────────────────────────

  function convert(jsonString) {
    var parsed;
    try { parsed = JSON.parse(jsonString); }
    catch (e) { return { error: "Invalid JSON: " + e.message }; }

    if (looksLikeWorkBuddyExport(parsed)) {
      return { direction: "wb2cockpit", data: workbuddyToCockpit(parsed) };
    }
    if (looksLikeCockpitExport(parsed)) {
      return { direction: "cockpit2wb", data: cockpitToWorkbuddy(parsed) };
    }
    return { error: "Unrecognized format. Expected WorkBuddy export or Cockpit Tools export." };
  }

  // ── Export ───────────────────────────────────────────────

  var api = { convert: convert, workbuddyToCockpit: workbuddyToCockpit, cockpitToWorkbuddy: cockpitToWorkbuddy,
              parseDateStringToTs: parseDateStringToTs, tsToDateString: tsToDateString, md5: md5 };

  if (typeof module !== "undefined" && module.exports) module.exports = api;
  else global.WorkBuddyConverter = api;

})(typeof window !== "undefined" ? window : this);
