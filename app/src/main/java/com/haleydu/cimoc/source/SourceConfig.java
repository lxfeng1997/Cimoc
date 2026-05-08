package com.haleydu.cimoc.source;

import android.text.TextUtils;

import com.haleydu.cimoc.App;
import com.haleydu.cimoc.manager.PreferenceManager;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public final class SourceConfig {

    public static final String DEFAULT_SOURCE_URL = "https://raw.githubusercontent.com/haleydu-test/cimocUpdate/main/sourceBaseUrl.json";

    private static JSONObject sConfig;
    private static List<UrlMapping> sMappings;

    private SourceConfig() {
    }

    public static synchronized void update(String json) {
        if (TextUtils.isEmpty(json)) {
            return;
        }
        try {
            sConfig = new JSONObject(json);
            sMappings = null;
            App.getPreferenceManager().putString(PreferenceManager.PREF_SOURCE_CONFIG_JSON, sConfig.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String resolveRemoteDomains(String json, OkHttpClient client) {
        if (TextUtils.isEmpty(json) || client == null) {
            return json;
        }
        try {
            JSONObject object = new JSONObject(json);
            Iterator<String> keys = object.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                Object item = object.opt(key);
                if (item instanceof JSONObject) {
                    JSONObject source = (JSONObject) item;
                    String updateUrl = source.optString("updateUrl", "");
                    if (!TextUtils.isEmpty(updateUrl)) {
                        String baseUrl = fetchFirstUrl(client, updateUrl);
                        if (!TextUtils.isEmpty(baseUrl)) {
                            source.put("baseUrl", baseUrl);
                        }
                    }
                }
            }
            return object.toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return json;
    }

    public static String getSourceUrl() {
        String url = App.getPreferenceManager().getString(PreferenceManager.PREF_SOURCE_CONFIG_URL, DEFAULT_SOURCE_URL);
        return normalizeConfigUrl(url);
    }

    public static void setSourceUrl(String url) {
        App.getPreferenceManager().putString(PreferenceManager.PREF_SOURCE_CONFIG_URL, normalizeConfigUrl(url));
        App.getPreferenceManager().putString(PreferenceManager.PREF_SOURCE_CONFIG_JSON, "");
        sConfig = null;
        sMappings = null;
    }

    public static Request rewriteRequest(Request request) {
        if (request == null) {
            return null;
        }
        String rewritten = rewriteUrl(request.url().toString(), true);
        if (rewritten.equals(request.url().toString())) {
            return request;
        }
        Request.Builder builder = request.newBuilder().url(rewritten);
        String referer = request.header("Referer");
        if (!TextUtils.isEmpty(referer)) {
            builder.header("Referer", rewriteUrl(referer, true));
        }
        String origin = request.header("Origin");
        if (!TextUtils.isEmpty(origin)) {
            builder.header("Origin", rewriteUrl(origin, true));
        }
        String host = request.header("Host");
        if (!TextUtils.isEmpty(host)) {
            String rewrittenHost = rewriteHost(host);
            if (!host.equals(rewrittenHost)) {
                builder.header("Host", rewrittenHost);
            }
        }
        return builder.build();
    }

    public static String normalizeResponse(String body) {
        if (TextUtils.isEmpty(body)) {
            return body;
        }
        String result = body;
        for (UrlMapping mapping : mappings()) {
            if (!mapping.oldBase.equals(mapping.newBase)) {
                result = result.replace(mapping.newBase, mapping.oldBase);
                result = result.replace(stripScheme(mapping.newBase), stripScheme(mapping.oldBase));
            }
        }
        return result;
    }

    public static String value(String key, String field, String fallback) {
        try {
            Object item = config().opt(key);
            if (item instanceof JSONObject) {
                String value = ((JSONObject) item).optString(field, "");
                return TextUtils.isEmpty(value) ? fallback : value;
            }
            if (item instanceof String && "baseUrl".equals(field)) {
                String value = (String) item;
                return TextUtils.isEmpty(value) ? fallback : value;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return fallback;
    }

    public static String describeConfig() {
        StringBuilder builder = new StringBuilder();
        builder.append("当前配置地址：").append(getSourceUrl()).append("\n\n");
        builder.append("已生效域名映射：").append("\n");
        for (UrlMapping mapping : mappings()) {
            if (!mapping.oldBase.equals(mapping.newBase)) {
                builder.append(mapping.oldBase).append(" -> ").append(mapping.newBase).append("\n");
            }
        }
        builder.append("\n图源配置列表：").append("\n");
        JSONObject config = config();
        Iterator<String> keys = config.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            Object item = config.opt(key);
            builder.append(key).append("\n");
            if (item instanceof JSONObject) {
                JSONObject object = (JSONObject) item;
                Iterator<String> fields = object.keys();
                while (fields.hasNext()) {
                    String field = fields.next();
                    String value = object.optString(field, "");
                    if (!TextUtils.isEmpty(value)) {
                        builder.append("  ").append(field).append(": ").append(value).append("\n");
                    }
                }
            } else if (item instanceof String) {
                String value = (String) item;
                if (!TextUtils.isEmpty(value)) {
                    builder.append("  baseUrl: ").append(value).append("\n");
                }
            }
        }
        return builder.toString();
    }

    private static String rewriteUrl(String url, boolean toNew) {
        if (TextUtils.isEmpty(url)) {
            return url;
        }
        for (UrlMapping mapping : mappings()) {
            String from = toNew ? mapping.oldBase : mapping.newBase;
            String to = toNew ? mapping.newBase : mapping.oldBase;
            if (!from.equals(to) && url.startsWith(from)) {
                return to + url.substring(from.length());
            }
        }
        return url;
    }

    private static synchronized List<UrlMapping> mappings() {
        if (sMappings != null) {
            return sMappings;
        }
        sMappings = new ArrayList<>();
        add("https://www.manhuagui.com", "IKANMAN", "baseUrl");
        add("https://tw.manhuagui.com", "IKANMANTW", "baseUrl");
        add("https://i.hamreus.com", "IKANMANSERVER", "baseUrl");
        add("http://m.dmzj1.com", "DMZJ", "baseUrl");
        add("http://s.acg.dmzj1.com", "DMZJSEARCH", "baseUrl");
        add("http://v3api.dmzj1.com", "DMZJSERVER", "baseUrl");
        add("http://images.dmzj1.com", "DMZJPICTURE", "baseUrl");
        add("https://m.dmzj.com", "DMZJV2", "baseUrl");
        add("http://m.dmzj.com", "DMZJV2", "baseUrl");
        add("http://v2.api.dmzj.com", "DMZJV2SERVER", "baseUrl");
        add("https://v3api.idmzj.com", "DMZJV2SERVER", "baseUrl");
        add("http://api.dmzj.com", "DMZJFIXSERVER", "baseUrl");
        add("https://images.dmzj.com", "DMZJV2PICTURE", "baseUrl");
        add("http://images.dmzj.com", "DMZJPICTURE", "baseUrl");
        add("http://m.dm5.com", "DM5", "baseUrl");
        add("http://www.dm5.com", "DM5", "baseUrl");
        add("https://m.webtoons.com", "WEBTOON", "baseUrl");
        add("http://m.webtoons.com", "WEBTOON", "baseUrl");
        add("https://www.dongmanmanhua.cn", "WEBTOONDONGMANMANHUA", "baseUrl");
        add("http://m.buka.cn", "BUKA", "baseUrl");
        add("http://m.wuqimh.net", "MH57", "baseUrl");
        add("http://m.wuqimh.com", "MH57", "baseUrl");
        add("http://imagesold.502215.com", "MH57SERVER", "baseUrl");
        add("http://images.lancaier.com", "MH57SERVER", "baseUrl");
        add("https://m.manhuadai.com", "MH50", "baseUrl");
        add("https://www.mh160.xyz", "MH160", "baseUrl");
        add("https://m.mh160.xyz", "MH160", "baseUrl");
        add("https://mhpic5.gezhengzhongyi.cn:8443", "MH160SERVER1", "baseUrl");
        add("https://res.gezhengzhongyi.cn:20207", "MH160SERVER2", "baseUrl");
        add("https://mhpic88.miyeye.cn:8443", "MH160SERVER3", "baseUrl");
        add("http://m.517manhua.com", "MH517", "baseUrl");
        add("http://m.pufei8.com", "PUFEI", "baseUrl");
        add("http://m.pufei.com", "PUFEI", "baseUrl");
        add("http://res.img.youzipi.net", "PUFEISERVER", "baseUrl");
        add("http://qiman6.com", "QIMANWU", "baseUrl");
        add("https://comic.mkzcdn.com", "QIMANWU", "baseUrl");
        add("https://www.qimiaomh.com", "QIMIAOMH", "baseUrl");
        add("https://m.bnmanhua.com", "BAINIAN", "baseUrl");
        add("http://m.chuiyao.com", "CHUIYAO", "baseUrl");
        add("http://www.chuixue.net", "CHUIXUE", "baseUrl");
        add("http://chuixue1.tianshigege.com", "CHUIXUESERVER", "baseUrl");
        add("https://m.gufengmh9.com", "GUFENG", "baseUrl");
        add("https://m.gufengmh8.com", "GUFENG", "baseUrl");
        add("https://www.gufengmh8.com", "GUFENG", "baseUrl");
        add("https://res.xiaoqinre.com", "GUFENGSERVER", "baseUrl");
        add("https://m.manhuatai.com", "MANHUATAIPIC", "baseUrl");
        add("https://www.manhuatai.com", "MANHUATAI", "baseUrl");
        add("https://m.kanman.com", "MANHUATAIPIC", "baseUrl");
        add("https://www.kanman.com", "MANHUATAI", "baseUrl");
        add("https://image.yqmh.com", "MANHUATAIPIC", "baseUrl");
        add("http://www.mangabz.com", "MANGABZ", "baseUrl");
        add("https://www.manhuadb.com", "MANHUADB", "baseUrl");
        add("http://m.ccmh6.com", "CCMH", "baseUrl");
        add("http://m.50mh.com", "CCMH", "baseUrl");
        add("http://m.tuku.cc", "CCTUKU", "baseUrl");
        add("https://www.cartoonmad.com", "CARTOONMAD", "baseUrl");
        add("http://www.cartoonmad.com", "CARTOONMAD", "baseUrl");
        add("http://www.2animx.com", "ANIMX2", "baseUrl");
        add("http://www.u17.com", "U17", "baseUrl");
        add("http://m.u17.com", "U17", "baseUrl");
        add("http://so.u17.com", "U17_SO", "baseUrl");
        add("http://ac.qq.com", "TENCENT", "baseUrl");
        add("https://m.ac.qq.com", "TENCENTM", "baseUrl");
        add("http://m.ac.qq.com", "TENCENTM", "baseUrl");
        add("http://www.migudm.cn", "MIGU", "baseUrl");
        add("http://m.migudm.cn", "MIGU", "baseUrl");
        add("https://m.ykmh.net", "YKMH", "baseUrl");
        add("https://m.ykmh.com", "YKMH", "baseUrl");
        add("https://wap.ykmh.com", "YKMH", "baseUrl");
        add("https://www.ykmh.com", "YKMH", "baseUrl");
        add("http://js.haotuyk.top", "YKMH_SERVER", "baseUrl");
        add("https://www.copy20.com", "COPYMH", "baseUrl");
        add("https://www.copy20.com/api/kb/web/searchbg/comics", "COPYMHSERVER", "baseUrl");
        add("https://copymanga.com", "CopyManHua", "baseUrl");
        add("https://api.copymanga.com", "CopyManHua", "serverUrl3");
        add("https://www.cocomanhua.com", "CoCoManHua", "baseUrl");
        add("https://img.cocomanhua.com/comic", "CoCoManHua", "serverUrl");
        add("https://mangakakalot.com", "MANGAKAKALOT", "baseUrl");
        add("https://manganelo.com", "MANGANEL", "baseUrl");
        add("http://manganelo.com", "MANGANEL", "baseUrl");
        add("https://manganel.com", "MANGANEL", "baseUrl");
        add("http://manganel.com", "MANGANEL", "baseUrl");
        add("https://m.manga2020.com", "HOTMANGA", "baseUrl");
        add("https://mapi.hotmangasg.com:12001", "HOTMANGASERVER", "baseUrl");
        add("https://mapi.hotmangasd.com:12001", "HOTMANGASERVER", "baseUrl");
        add("https://tuhao456.com", "TUHAO", "baseUrl");
        add("https://m.tohomh456.com", "TUHAO", "baseUrl");
        add("http://8comic.se", "YYLS", "baseUrl");
        add("http://99770.hhxxee.com", "HHXXEE", "baseUrl");
        add("http://20.125084.com", "HHXXEE", "baseUrl");
        add("http://www.sixmh6.com", "SIXMH", "baseUrl");
        add("http://www.php06.com", "MHLOVE", "baseUrl");
        add("http://www.9qv.cn", "MHLOVE", "serverUrl");
        add("http://www.177pic66.com", "PIC177", "baseUrl");
        add("http://www.5qmh.com", "MH57", "baseUrl");
        add("https://18comic1.one", "BASEURLJMV2", "baseUrl");
        add("https://18comic2.one", "BASEURLJMV2", "baseUrl");
        add("https://18comic.vip", "BASEURLJMV2", "baseUrl");
        add("https://cm365.xyz", "BASEURLJMV2", "baseUrl");
        add("https://e-hentai.org", "EHENTAI", "baseUrl");
        add("http://g.e-hentai.org", "EHENTAI", "baseUrl");
        add("http://ehgt.org", "EHENTAIINFOCOVER", "baseUrl");
        add("https://exhentai.org", "EXHENTAI", "baseUrl");
        add("https://nhentai.net", "NHENTAI", "baseUrl");
        add("https://www.wnacg.com", "WNACG", "baseUrl");
        return sMappings;
    }

    private static void add(String oldBase, String key, String field) {
        oldBase = trimEnd(oldBase);
        String newBase = trimEnd(urlValue(key, field, oldBase));
        if (TextUtils.isEmpty(newBase)) {
            newBase = oldBase;
        }
        if (!isHttpUrl(newBase)) {
            newBase = oldBase;
        }
        if (!isHttpUrl(oldBase)) {
            return;
        }
        newBase = trimEnd(newBase);
        sMappings.add(new UrlMapping(oldBase, newBase));
    }

    private static String urlValue(String key, String field, String fallback) {
        String url = value(key, field, fallback);
        return isHttpUrl(url) ? url : fallback;
    }

    private static String fetchFirstUrl(OkHttpClient client, String url) {
        Response response = null;
        try {
            Request request = new Request.Builder().url(url).build();
            response = client.newCall(request).execute();
            if (response.isSuccessful() && response.body() != null) {
                Matcher matcher = Pattern.compile("https?://[^\\s\"'<>]+").matcher(response.body().string());
                if (matcher.find()) {
                    return trimEnd(matcher.group());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (response != null) {
                response.close();
            }
        }
        return "";
    }

    private static JSONObject config() {
        if (sConfig != null) {
            return sConfig;
        }
        String json = App.getPreferenceManager().getString(PreferenceManager.PREF_SOURCE_CONFIG_JSON, "");
        if (TextUtils.isEmpty(json)) {
            json = readAsset();
        }
        try {
            sConfig = new JSONObject(json);
        } catch (Exception e) {
            sConfig = new JSONObject();
        }
        return sConfig;
    }

    private static String normalizeConfigUrl(String url) {
        if (TextUtils.isEmpty(url)) {
            return DEFAULT_SOURCE_URL;
        }
        String result = url.trim();
        if (result.startsWith("https://github.com/") && result.contains("/blob/")) {
            result = result.replace("https://github.com/", "https://raw.githubusercontent.com/");
            result = result.replace("/blob/", "/");
        }
        return result;
    }

    private static String readAsset() {
        InputStream input = null;
        try {
            input = App.getAppContext().getAssets().open("sourceBaseUrl.json");
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int count;
            while ((count = input.read(buffer)) != -1) {
                output.write(buffer, 0, count);
            }
            return output.toString("UTF-8");
        } catch (Exception e) {
            return "{}";
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (Exception ignored) {
                }
            }
        }
    }

    private static String trimEnd(String url) {
        if (TextUtils.isEmpty(url)) {
            return "";
        }
        while (url.endsWith("/") && url.length() > "https://".length()) {
            url = url.substring(0, url.length() - 1);
        }
        return url;
    }

    private static String stripScheme(String url) {
        HttpUrl httpUrl = HttpUrl.parse(url);
        return httpUrl == null ? url : httpUrl.host();
    }

    private static String rewriteHost(String host) {
        for (UrlMapping mapping : mappings()) {
            HttpUrl oldUrl = HttpUrl.parse(mapping.oldBase);
            HttpUrl newUrl = HttpUrl.parse(mapping.newBase);
            if (oldUrl != null && newUrl != null && oldUrl.host().equals(host)) {
                return newUrl.host();
            }
        }
        return host;
    }

    private static boolean isHttpUrl(String url) {
        return !TextUtils.isEmpty(url) && (url.startsWith("http://") || url.startsWith("https://"));
    }

    private static final class UrlMapping {
        final String oldBase;
        final String newBase;

        UrlMapping(String oldBase, String newBase) {
            this.oldBase = oldBase;
            this.newBase = newBase;
        }
    }

}
