package modern.ch03.connector.http;

import jakarta.servlet.http.Cookie;

import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 解析 Cookie、表单参数、规范化 URI 的工具方法（原书 org.apache.catalina.util.RequestUtil） */
public final class RequestUtil {

    private RequestUtil() {
    }

    /** "userName=budi; password=pwd" → [Cookie(userName, budi), Cookie(password, pwd)] */
    public static List<Cookie> parseCookieHeader(String header) {
        List<Cookie> cookies = new ArrayList<>();
        if (header == null) {
            return cookies;
        }
        for (String token : header.split(";")) {
            int eq = token.indexOf('=');
            if (eq > 0) {
                try {
                    cookies.add(new Cookie(token.substring(0, eq).trim(), token.substring(eq + 1).trim()));
                } catch (IllegalArgumentException ignored) {
                    // 名字不合法的 cookie 直接丢弃，和原书一样
                }
            }
        }
        return cookies;
    }

    /**
     * 把 "a=1&b=2&a=3" 解析进 map，同名参数的值依次追加：{a=[1, 3], b=[2]}。
     * URLDecoder.decode(String, Charset) 是 Java 10 新增的重载，不用再捕获 UnsupportedEncodingException。
     */
    public static void parseParameters(Map<String, String[]> map, String data, Charset charset) {
        if (data == null || data.isEmpty()) {
            return;
        }
        for (String pair : data.split("&")) {
            if (pair.isEmpty()) {
                continue;
            }
            int eq = pair.indexOf('=');
            String name = URLDecoder.decode(eq < 0 ? pair : pair.substring(0, eq), charset);
            String value = eq < 0 ? "" : URLDecoder.decode(pair.substring(eq + 1), charset);
            map.merge(name, new String[] {value}, (old, add) -> {
                String[] all = Arrays.copyOf(old, old.length + 1);
                all[old.length] = add[0];
                return all;
            });
        }
    }

    public static Map<String, String[]> newParameterMap() {
        return new LinkedHashMap<>();
    }

    /**
     * 规范化 URI：反斜杠变斜杠、合并 "//"、去掉 "/./"、解析 "/../"。
     * 想跳出根目录、或者用 %2e %2f %5c %25 编码特殊字符来绕过检查的，一律返回 null。
     */
    public static String normalize(String path) {
        if (path == null) {
            return null;
        }
        String lower = path.toLowerCase();
        if (lower.contains("%25") || lower.contains("%2f") || lower.contains("%2e") || lower.contains("%5c")) {
            return null;
        }
        String normalized = path.replace('\\', '/');
        if (!normalized.startsWith("/")) {
            normalized = "/" + normalized;
        }
        // 逐段处理比原书的多个 while + indexOf 更直观
        var segments = new ArrayList<String>();
        for (String seg : normalized.split("/")) {
            switch (seg) {
                case "", "." -> { }                                   // "//" 和 "/./"
                case ".." -> {
                    if (segments.isEmpty()) {
                        return null;                                   // 想跳出根目录
                    }
                    segments.remove(segments.size() - 1);
                }
                default -> segments.add(seg);
            }
        }
        String result = "/" + String.join("/", segments);
        return normalized.endsWith("/") && !result.equals("/") ? result + "/" : result;
    }
}
