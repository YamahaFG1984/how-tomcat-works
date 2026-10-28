package modern.ch22.mini;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 迷你版 DefaultServlet：Tomcat 里所有「没被别的 Servlet 映射到」的请求都交给它，负责读静态文件。
 * 一个合格的静态文件 Servlet 至少要做这几件事：
 *   ① 防路径穿越；目录要么补 "/" 重定向，要么找欢迎文件
 *   ② 根据扩展名给出 Content-Type
 *   ③ 条件请求：ETag / If-None-Match、Last-Modified / If-Modified-Since → 304，省掉响应体
 *   ④ 范围请求：Range: bytes=a-b → 206 + Content-Range，断点续传和视频拖动靠它
 * HEAD 请求不用单独处理：HttpServlet.doHead 会调用 doGet 并丢弃响应体。
 */
public class MiniDefaultServlet extends HttpServlet {

    private static final Map<String, String> MIME = Map.of(
            "html", "text/html", "css", "text/css", "js", "text/javascript",
            "txt", "text/plain", "json", "application/json", "png", "image/png", "svg", "image/svg+xml");
    private static final Pattern RANGE = Pattern.compile("bytes=(\\d*)-(\\d*)");

    private final Path root;

    public MiniDefaultServlet(Path root) {
        this.root = root.toAbsolutePath().normalize();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String rel = req.getPathInfo() == null ? "/" : req.getPathInfo();
        Path file = root.resolve(rel.substring(1)).normalize();
        if (!file.startsWith(root) || !Files.exists(file)) {              // ①
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, rel);
            return;
        }
        if (Files.isDirectory(file)) {
            if (!rel.endsWith("/")) {                                     // /docs → /docs/，相对链接才不会错
                resp.sendRedirect(req.getRequestURI() + "/");
                return;
            }
            file = file.resolve("index.html");                            // 欢迎文件
            if (!Files.isRegularFile(file)) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND, "目录列表已关闭");
                return;
            }
        }

        long length = Files.size(file);
        long lastModified = Files.getLastModifiedTime(file).toMillis() / 1000 * 1000;   // HTTP 日期只精确到秒
        String etag = "W/\"" + length + "-" + lastModified + "\"";        // 和 Tomcat 默认格式一样：弱 ETag
        resp.setHeader("ETag", etag);
        resp.setDateHeader("Last-Modified", lastModified);
        resp.setHeader("Accept-Ranges", "bytes");
        resp.setContentType(contentType(file));                           // ②

        if (notModified(req, etag, lastModified)) {                       // ③
            resp.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
            return;
        }

        long start = 0, end = length - 1;
        String range = req.getHeader("Range");
        if (range != null && matchesIfRange(req, etag)) {                 // ④
            Matcher m = RANGE.matcher(range.trim());
            if (!m.matches() || (m.group(1).isEmpty() && m.group(2).isEmpty())) {
                resp.setHeader("Content-Range", "bytes */" + length);
                resp.sendError(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
                return;
            }
            if (m.group(1).isEmpty()) {                                   // bytes=-500：最后 500 字节
                start = Math.max(0, length - Long.parseLong(m.group(2)));
            } else {
                start = Long.parseLong(m.group(1));
                if (!m.group(2).isEmpty()) end = Math.min(end, Long.parseLong(m.group(2)));
            }
            if (start > end) {
                resp.setHeader("Content-Range", "bytes */" + length);
                resp.sendError(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
                return;
            }
            resp.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
            resp.setHeader("Content-Range", "bytes " + start + "-" + end + "/" + length);
        }

        resp.setContentLengthLong(end - start + 1);
        try (InputStream in = Files.newInputStream(file)) {
            in.skipNBytes(start);
            var out = resp.getOutputStream();
            byte[] buf = new byte[8192];
            for (long left = end - start + 1; left > 0; ) {                // 只拷贝范围内的字节
                int n = in.read(buf, 0, (int) Math.min(buf.length, left));
                if (n < 0) break;
                out.write(buf, 0, n);
                left -= n;
            }
        }
    }

    /** If-None-Match 优先；没有它时才看 If-Modified-Since（RFC 9110 的规定） */
    static boolean notModified(HttpServletRequest req, String etag, long lastModified) {
        String inm = req.getHeader("If-None-Match");
        if (inm != null) {
            for (String tag : inm.split(",")) {
                String t = tag.trim();
                if (t.equals("*") || weakEquals(t, etag)) return true;
            }
            return false;
        }
        long ims = req.getDateHeader("If-Modified-Since");
        return ims != -1 && lastModified <= ims;
    }

    /** If-Range 带的 ETag 不匹配，说明文件变了，应当忽略 Range、返回整个文件 */
    private static boolean matchesIfRange(HttpServletRequest req, String etag) {
        String ifRange = req.getHeader("If-Range");
        return ifRange == null || ifRange.trim().equals(etag);
    }

    private static boolean weakEquals(String a, String b) {
        return a.replaceFirst("^W/", "").equals(b.replaceFirst("^W/", ""));
    }

    private String contentType(Path file) {
        String name = file.getFileName().toString();
        String ext = name.substring(name.lastIndexOf('.') + 1).toLowerCase();
        String type = MIME.getOrDefault(ext, "application/octet-stream");
        return type.startsWith("text/") ? type + ";charset=UTF-8" : type;
    }
}
