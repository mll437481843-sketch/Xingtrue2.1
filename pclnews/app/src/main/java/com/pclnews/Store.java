package com.pclnews;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.List;

/** 缓存与设置。数据量小，直接放 SharedPreferences。 */
public final class Store {
    static final String SP = "pclnews";
    public static final String K_CACHE = "cache_json";
    public static final String K_TIME = "cache_time";
    public static final String K_HEAD = "cache_head";
    public static final String K_HOUR = "hour";
    public static final String K_MIN = "minute";
    public static final String K_NOTIFY = "notify";
    public static final String K_LTY = "lty";
    public static final String K_IMG = "img";
    public static final String K_LTY_LAST = "lty_last";
    public static final String K_LTY_CACHE = "lty_cache";
    public static final String K_BILI_BLOCK = "bili_block_at";

    static SharedPreferences sp(Context c) {
        return c.getSharedPreferences(SP, Context.MODE_PRIVATE);
    }

    public static String get(Context c, String k, String def) { return sp(c).getString(k, def); }
    public static int getInt(Context c, String k, int def) { return sp(c).getInt(k, def); }
    public static long getLong(Context c, String k, long def) { return sp(c).getLong(k, def); }
    public static boolean getBool(Context c, String k, boolean def) { return sp(c).getBoolean(k, def); }
    public static void putString(Context c, String k, String v) { sp(c).edit().putString(k, v).apply(); }
    public static void putInt(Context c, String k, int v) { sp(c).edit().putInt(k, v).apply(); }
    public static void putLong(Context c, String k, long v) { sp(c).edit().putLong(k, v).apply(); }
    public static void putBool(Context c, String k, boolean v) { sp(c).edit().putBoolean(k, v).apply(); }

    /** 列表 → JSON（供 WebView 与通知共用） */
    public static String buildJson(List<Fetcher.Item> items) {
        StringBuilder sb = new StringBuilder(4096);
        sb.append("{\"updated\":").append(System.currentTimeMillis()).append(",\"items\":[");
        for (int i = 0; i < items.size(); i++) {
            Fetcher.Item it = items.get(i);
            if (i > 0) sb.append(',');
            sb.append("{\"t\":\"").append(esc(it.title))
              .append("\",\"u\":\"").append(esc(it.link))
              .append("\",\"s\":\"").append(esc(it.source))
              .append("\",\"c\":\"").append(esc(it.cat))
              .append("\",\"d\":\"").append(esc(it.desc))
              .append("\",\"p\":").append(it.time)
              .append(",\"o\":").append(it.stale ? 1 : 0).append('}');
        }
        sb.append("]}");
        return sb.toString();
    }

    public static void saveCache(Context c, List<Fetcher.Item> items) {
        String head = "";
        for (Fetcher.Item it : items) {
            if (!"lty".equals(it.cat) && !it.title.isEmpty()) { head = it.title; break; }
        }
        sp(c).edit().putString(K_CACHE, buildJson(items))
                .putLong(K_TIME, System.currentTimeMillis())
                .putString(K_HEAD, head).apply();
    }

    /** 天依条目单独存一份，抓不到时兜底 */
    public static void saveLty(Context c, List<Fetcher.Item> items) {
        StringBuilder sb = new StringBuilder();
        int n = 0;
        for (Fetcher.Item it : items) {
            if (!"lty".equals(it.cat)) continue;
            if (n++ > 0) sb.append('\u0002');
            sb.append(it.title).append('\u0001').append(it.link).append('\u0001')
              .append(it.time).append('\u0001').append(it.source).append('\u0001').append(it.desc);
        }
        if (n > 0) sp(c).edit().putString(K_LTY_CACHE, sb.toString())
                .putLong("lty_cache_at", System.currentTimeMillis()).apply();
    }

    public static String ltyCacheAge(Context c) {
        long t = getLong(c, "lty_cache_at", 0L);
        if (t == 0) return "";
        long m = (System.currentTimeMillis() - t) / 60000L;
        if (m < 60) return m + " 分钟前";
        if (m < 1440) return (m / 60) + " 小时前";
        return (m / 1440) + " 天前";
    }

    public static List<Fetcher.Item> parseLty(Context c) {
        List<Fetcher.Item> out = new ArrayList<>();
        String raw = get(c, K_LTY_CACHE, "");
        if (raw.isEmpty()) return out;
        for (String rec : raw.split("\u0002")) {
            String[] f = rec.split("\u0001", -1);
            if (f.length < 3 || f[0].isEmpty()) continue;
            Fetcher.Item it = new Fetcher.Item();
            it.title = f[0];
            it.link = f[1];
            try { it.time = Long.parseLong(f[2]); } catch (Exception ignored) { }
            it.source = f.length > 3 ? f[3] : "B\u7ad9";
            it.desc = f.length > 4 ? f[4] : "";
            it.cat = "lty";
            it.stale = true;
            out.add(it);
        }
        return out;
    }

    public static boolean hasLty(List<Fetcher.Item> items) {
        for (Fetcher.Item it : items) if ("lty".equals(it.cat)) return true;
        return false;
    }

    public static String esc(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder(s.length() + 8);
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            switch (ch) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': break;
                case '\t': sb.append(' '); break;
                default:
                    if (ch < 0x20) sb.append(' ');
                    else sb.append(ch);
            }
        }
        return sb.toString();
    }
}
