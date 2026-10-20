package com.pclnews;

import android.content.Context;
import android.util.Log;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

/** 抓取 + 解析 RSS / Atom / 百度资讯 / 必应 / B 站。零第三方依赖。 */
public final class Fetcher {
    static final String UA = "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 "
            + "(KHTML, like Gecko) Chrome/122.0.0.0 Mobile Safari/537.36";
    static final String TAG = "PCLNews";
    static final String BILI_MID = "36081646";
    static final String LTY_KEYWORD = "洛天依";

    public static class Item {
        public String title = "";
        public String link = "";
        public String source = "";
        public String cat = "";
        public String desc = "";
        public String desc0 = "";
        public long time = 0L;
        public boolean stale = false;
    }

    static class Source {
        final String name, url, cat;
        Source(String n, String u, String c) { name = n; url = u; cat = c; }
    }

    static final Source[] SOURCES = {
            new Source("IT之家", "https://www.ithome.com/rss/", "tech"),
            new Source("cnBeta", "https://www.cnbeta.com.tw/backend.php", "tech"),
            new Source("Solidot", "https://www.solidot.org/index.rss", "deep"),
            new Source("少数派", "https://sspai.com/feed", "deep"),
            new Source("中国新闻网", "http://www.chinanews.com.cn/rss/scroll-news.xml", "news"),
    };

    /* ================= HTTP ================= */

    static String http(String url) throws Exception { return http(url, null); }

    static String http(String url, Map<String, String> extra) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setConnectTimeout(9000);
        c.setReadTimeout(12000);
        c.setInstanceFollowRedirects(true);
        c.setRequestProperty("User-Agent", UA);
        c.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml,application/json;q=0.9,*/*;q=0.8");
        c.setRequestProperty("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.7");
        c.setRequestProperty("Accept-Encoding", "gzip");
        if (extra != null) {
            for (Map.Entry<String, String> e : extra.entrySet()) c.setRequestProperty(e.getKey(), e.getValue());
        }
        int code = c.getResponseCode();
        InputStream in = code >= 400 ? c.getErrorStream() : c.getInputStream();
        if (in == null) throw new RuntimeException("http " + code);
        String enc = c.getContentEncoding();
        if (enc != null && enc.toLowerCase(Locale.US).contains("gzip")) in = new GZIPInputStream(in);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) > 0) bos.write(buf, 0, n);
        in.close();
        c.disconnect();
        return new String(bos.toByteArray(), StandardCharsets.UTF_8);
    }

    static String urlEnc(String s) {
        try { return URLEncoder.encode(s, "UTF-8"); } catch (Exception e) { return s; }
    }

    /* ================= 文本 / 时间 ================= */

    static String clean(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder(s.length());
        boolean tag = false;
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch == '<') { tag = true; continue; }
            if (ch == '>') { tag = false; continue; }
            if (!tag) sb.append(ch);
        }
        return sb.toString()
                .replace("<![CDATA[", "").replace("]]>", "")
                .replace("&nbsp;", " ").replace("&amp;", "&").replace("&lt;", "<")
                .replace("&gt;", ">").replace("&quot;", "\"").replace("&#39;", "'")
                .replace("&apos;", "'").replace("&ldquo;", "“").replace("&rdquo;", "”")
                .replace("&mdash;", "—").replace("&hellip;", "…").replace("&middot;", "·")
                .replaceAll("\\s+", " ").trim();
    }

    static final String[] DATE_PATTERNS = {
            "EEE, dd MMM yyyy HH:mm:ss Z", "EEE, dd MMM yyyy HH:mm:ss ZZ",
            "EEE, dd MMM yyyy HH:mm:ss zzz", "dd MMM yyyy HH:mm:ss Z",
            "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", "yyyy-MM-dd'T'HH:mm:ssXXX",
            "yyyy-MM-dd'T'HH:mm:ssZ", "yyyy-MM-dd'T'HH:mm:ss",
            "yyyy-MM-dd HH:mm:ss", "yyyy/MM/dd HH:mm:ss", "yyyy-MM-dd"
    };

    static long parseDate(String raw) {
        if (raw == null) return 0L;
        String s = raw.trim();
        if (s.isEmpty()) return 0L;
        for (String p : DATE_PATTERNS) {
            try {
                SimpleDateFormat f = new SimpleDateFormat(p, Locale.US);
                f.setLenient(true);
                java.util.Date d = f.parse(s);
                if (d != null) return d.getTime();
            } catch (Exception ignored) { }
        }
        return 0L;
    }

    /* ================= RSS / Atom ================= */

    static List<Item> parseFeed(String xml, String sourceName, String cat, int max) {
        List<Item> out = new ArrayList<>();
        try {
            XmlPullParserFactory f = XmlPullParserFactory.newInstance();
            f.setNamespaceAware(false);
            XmlPullParser p = f.newPullParser();
            p.setInput(new StringReader(xml.replace("\uFEFF", "")));

            Item cur = null;
            String field = null;
            int ev = p.getEventType();
            while (ev != XmlPullParser.END_DOCUMENT) {
                if (ev == XmlPullParser.START_TAG) {
                    String name = p.getName().toLowerCase(Locale.US);
                    if (name.equals("item") || name.equals("entry")) {
                        cur = new Item();
                        cur.source = sourceName;
                        cur.cat = cat;
                    } else if (cur != null) {
                        field = name;
                        if (name.equals("link")) {
                            String href = p.getAttributeValue(null, "href");
                            if (href != null && cur.link.isEmpty()) cur.link = href;
                        }
                    }
                } else if (ev == XmlPullParser.TEXT) {
                    if (cur != null && field != null) {
                        String t = p.getText();
                        if (t != null) {
                            if (field.equals("title")) cur.title += t;
                            else if (field.equals("link")) { if (cur.link.isEmpty()) cur.link += t; }
                            else if (field.equals("pubdate") || field.equals("published")
                                    || field.equals("updated") || field.equals("date")) cur.desc0 += ("\u0001" + t);
                            else if (field.equals("description") || field.equals("summary") || field.equals("content"))
                                cur.desc += t;
                        }
                    }
                } else if (ev == XmlPullParser.END_TAG) {
                    String name = p.getName().toLowerCase(Locale.US);
                    if ((name.equals("item") || name.equals("entry")) && cur != null) {
                        finish(cur);
                        if (!cur.title.isEmpty()) out.add(cur);
                        cur = null;
                        if (out.size() >= max) break;
                    }
                    field = null;
                }
                ev = p.next();
            }
        } catch (Exception e) {
            Log.w(TAG, "parse " + sourceName + " failed: " + e);
        }
        return out;
    }

    static void finish(Item it) {
        it.title = clean(it.title).replace("\u0001", "");
        it.desc = clean(it.desc);
        if (it.desc.length() > 150) it.desc = it.desc.substring(0, 150) + "…";
        if (it.link != null) it.link = it.link.trim();
        long best = 0L;
        for (String chunk : it.desc0.split("\u0001")) {
            long t = parseDate(chunk);
            if (t > best) best = t;
        }
        it.time = best;
    }

    /* ================= 配图 ================= */

    static String fetchOgImage(String url) {
        try {
            HashMap<String, String> h = new HashMap<>();
            h.put("Accept", "text/html,application/xhtml+xml");
            String html = http(url, h);
            Pattern p1 = Pattern.compile(
                    "<meta[^>]+(?:property|name)=[\"'](?:og:image|og:image:url|twitter:image)[\"'][^>]*?content=[\"']([^\"']+)",
                    Pattern.CASE_INSENSITIVE);
            Matcher m = p1.matcher(html);
            if (!m.find()) {
                Pattern p2 = Pattern.compile(
                        "<meta[^>]+content=[\"']([^\"']+)[\"'][^>]*?(?:property|name)=[\"']og:image",
                        Pattern.CASE_INSENSITIVE);
                m = p2.matcher(html);
                if (!m.find()) return "";
            }
            String img = m.group(1).trim();
            if (img.startsWith("//")) return "https:" + img;
            if (img.startsWith("http")) return img;
            return "";
        } catch (Exception e) {
            Log.i(TAG, "og fail: " + e);
            return "";
        }
    }

    /* ================= 洛天依 ================= */

    static boolean ltySkip(String title, String link) {
        String[] badHost = {"baike.baidu", "weibo.com", "weibo.cn", "huati.weibo", "kugou.com",
                "kuwo.cn", "itunes.apple", "music.163", "y.qq.com", "douban.com", "zhidao.baidu",
                "bilibili.com/read", "bilibili.com/bangumi", "bilibili.com/v/", "wikipedia"};
        for (String h : badHost) if (link.contains(h)) return true;
        String[] badTitle = {"超话", "百科", "个人空间", "精选集", "在线试听", "歌单", "歌词",
                "壁纸", "头像", "简谱", "钢琴谱", "手办", "铃声", "论坛"};
        for (String b : badTitle) if (title.contains(b)) return true;
        return false;
    }

    /** 百度资讯搜索：国内必通，天依主源 */
    static List<Item> fetchBaiduNews(String kw, int max) {
        List<Item> out = new ArrayList<>();
        try {
            HashMap<String, String> h = new HashMap<>();
            h.put("Referer", "https://www.baidu.com/");
            String html = http("https://www.baidu.com/s?wd=" + urlEnc(kw) + "&tn=news&rn=20", h);
            Pattern p = Pattern.compile(
                    "<a[^>]+href=\"(https?://[^\"]+)\"[^>]*?aria-label=\"标题：([^\"]+)\"",
                    Pattern.DOTALL);
            Matcher m = p.matcher(html);
            HashSet<String> seen = new HashSet<>();
            long t = System.currentTimeMillis();
            while (m.find() && out.size() < max) {
                String link = m.group(1);
                String title = clean(m.group(2));
                if (title.isEmpty() || title.length() < 6 || ltySkip(title, link) || !seen.add(title)) continue;
                Item it = new Item();
                it.title = title;
                it.link = link;
                it.source = "百度资讯";
                it.cat = "lty";
                it.time = t - out.size() * 60000L;
                out.add(it);
            }
        } catch (Exception e) {
            Log.i(TAG, "baidu fail: " + e);
        }
        return out;
    }

    /** 必应网页搜索：天依兜底源 */
    static List<Item> fetchBingSearch(String kw, int max) {
        List<Item> out = new ArrayList<>();
        try {
            HashMap<String, String> h = new HashMap<>();
            h.put("Accept-Language", "zh-CN,zh;q=0.9");
            String html = http("https://cn.bing.com/search?q=" + urlEnc(kw) + "&mkt=zh-CN", h);
            Pattern p = Pattern.compile(
                    "<h2[^>]*>\\s*<a[^>]+href=\"(https?://[^\"]+)\"[^>]*>([\\s\\S]{0,200}?)</a>",
                    Pattern.DOTALL);
            Matcher m = p.matcher(html);
            HashSet<String> seen = new HashSet<>();
            long t = System.currentTimeMillis();
            while (m.find() && out.size() < max) {
                String link = m.group(1);
                String title = clean(m.group(2));
                if (title.length() < 6 || ltySkip(title, link) || !seen.add(title)) continue;
                if (!title.contains(kw)) continue;
                Item it = new Item();
                it.title = title;
                it.link = link;
                it.source = "必应";
                it.cat = "lty";
                it.time = t - out.size() * 90000L;
                out.add(it);
            }
        } catch (Exception e) {
            Log.i(TAG, "bing fail: " + e);
        }
        return out;
    }

    static List<Item> fetchLuotianyi(boolean allowBili) {
        List<Item> out = new ArrayList<>();
        if (allowBili) {
            try {
                HashMap<String, String> h = new HashMap<>();
                h.put("Referer", "https://space.bilibili.com/" + BILI_MID + "/video");
                String j = http("https://api.bilibili.com/x/space/arc/search?mid=" + BILI_MID
                        + "&ps=6&pn=1&order=pubdate", h);
                int vs = j.indexOf("\"vlist\":[");
                int ve = j.indexOf(']', vs);
                if (vs > 0 && ve > vs) {
                    String arr = j.substring(vs + 8, ve + 1);
                    int i = 0;
                    while (out.size() < 6) {
                        int ts = arr.indexOf("\"title\":\"", i);
                        if (ts < 0) break;
                        int te = arr.indexOf('"', ts + 9);
                        String title = arr.substring(ts + 9, te).replace("\\\"", "\"").replace("\\/", "/");
                        int bs = arr.indexOf("\"bvid\":\"", te);
                        String bv = "";
                        if (bs > 0) { int be = arr.indexOf('"', bs + 8); bv = arr.substring(bs + 8, be); }
                        int cs = arr.indexOf("\"created\":", te);
                        long created = 0L;
                        if (cs > 0) {
                            int ce = cs + 10;
                            StringBuilder num = new StringBuilder();
                            while (ce < arr.length() && Character.isDigit(arr.charAt(ce))) num.append(arr.charAt(ce++));
                            if (num.length() > 0) created = Long.parseLong(num.toString()) * 1000L;
                        }
                        Item it = new Item();
                        it.title = "【官方投稿】" + title;
                        it.link = bv.isEmpty()
                                ? "https://space.bilibili.com/" + BILI_MID + "/video"
                                : "https://www.bilibili.com/video/" + bv;
                        it.source = "B站 官方";
                        it.cat = "lty";
                        it.time = created;
                        if (!title.isEmpty()) out.add(it);
                        i = te + 1;
                    }
                }
            } catch (Exception e) {
                Log.i(TAG, "bili fail: " + e);
            }
        }
        out.addAll(fetchBaiduNews(LTY_KEYWORD, 6));
        out.addAll(fetchBaiduNews(LTY_KEYWORD + " 演唱会", 3));
        out.addAll(fetchBaiduNews(LTY_KEYWORD + " 新歌", 3));
        out.addAll(fetchBingSearch(LTY_KEYWORD, 6));
        try {
            String xml = http("https://news.google.com/rss/search?q=" + urlEnc(LTY_KEYWORD)
                    + "&hl=zh-CN&gl=CN&ceid=CN:zh-Hans");
            out.addAll(parseFeed(xml, "Google 资讯", "lty", 8));
        } catch (Exception e) {
            Log.i(TAG, "google lty fail: " + e);
        }
        return out;
    }

    /* ================= 汇总 ================= */

    static boolean hasBili(List<Item> items) {
        for (Item it : items) if (it.source != null && it.source.contains("B站")) return true;
        return false;
    }

    public static List<Item> fetchAll(boolean withLty, final boolean allowBili) {
        ExecutorService pool = Executors.newFixedThreadPool(SOURCES.length + (withLty ? 1 : 0));
        List<Future<List<Item>>> futures = new ArrayList<>();
        for (final Source s : SOURCES) {
            futures.add(pool.submit(new Callable<List<Item>>() {
                public List<Item> call() {
                    try { return parseFeed(http(s.url), s.name, s.cat, 12); }
                    catch (Exception e) { Log.i(TAG, s.name + " fail: " + e); return new ArrayList<Item>(); }
                }
            }));
        }
        if (withLty) {
            futures.add(pool.submit(new Callable<List<Item>>() {
                public List<Item> call() { return fetchLuotianyi(allowBili); }
            }));
        }
        List<Item> all = new ArrayList<>();
        for (Future<List<Item>> f : futures) {
            try { all.addAll(f.get(22, TimeUnit.SECONDS)); } catch (Exception ignored) { }
        }
        pool.shutdownNow();

        Map<String, Item> seen = new LinkedHashMap<>();
        for (Item it : all) {
            String key = it.title.length() > 20 ? it.title.substring(0, 20) : it.title;
            if (!seen.containsKey(key)) seen.put(key, it);
        }
        List<Item> list = new ArrayList<>(seen.values());
        Collections.sort(list, new Comparator<Item>() {
            public int compare(Item a, Item b) { return Long.compare(b.time, a.time); }
        });
        return list;
    }

    /** 抓取 + 天依缓存兜底 + B 站限流退避 */
    public static List<Item> fetchAllWithCache(Context ctx, boolean withLty) {
        boolean allowBili = System.currentTimeMillis() - Store.getLong(ctx, Store.K_BILI_BLOCK, 0L) > 3600000L;
        List<Item> items = fetchAll(withLty, allowBili);
        if (withLty) {
            if (hasBili(items)) Store.putLong(ctx, Store.K_BILI_BLOCK, 0L);
            else Store.putLong(ctx, Store.K_BILI_BLOCK, System.currentTimeMillis());
            if (Store.hasLty(items)) {
                Store.saveLty(ctx, items);
            } else {
                List<Item> cached = Store.parseLty(ctx);
                if (!cached.isEmpty()) items.addAll(cached);
            }
            Collections.sort(items, new Comparator<Item>() {
                public int compare(Item a, Item b) { return Long.compare(b.time, a.time); }
            });
        }
        return items;
    }
}
