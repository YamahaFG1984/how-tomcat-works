package modern.ch22.tomcat;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import modern.ch22.mini.MiniDefaultServlet;
import org.apache.catalina.Context;
import org.apache.catalina.Wrapper;
import org.apache.catalina.servlets.DefaultServlet;
import org.apache.catalina.startup.Tomcat;
import org.apache.tomcat.util.descriptor.web.ErrorPage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * 静态资源、错误页和字符编码：
 *   /            Tomcat 的 DefaultServlet（欢迎文件、ETag、304、Range 全都有）
 *   /mini/*      本章的 MiniDefaultServlet，读同一个目录，方便对照响应头
 *   /boom        抛 IllegalStateException → 按异常类型映射到 /errors/exception
 *   /crash       抛没有映射的异常 → ErrorReportValve 生成的默认错误页
 *   不存在的路径  404 → 按状态码映射到 /errors/404
 *   /echo        回显参数；/utf8/echo 在另一个设置了 requestCharacterEncoding 的应用里
 *
 * 运行：./run.sh ch22.tomcat.EmbeddedStatic
 */
public final class EmbeddedStatic {

    public static void main(String[] args) throws Exception {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 8080;
        Path docBase = createSite();

        var tomcat = new Tomcat();
        tomcat.setBaseDir(Files.createTempDirectory("tomcat-ch22").toString());
        tomcat.setPort(port);
        tomcat.getConnector().setProperty("address", "127.0.0.1");

        Context ctx = tomcat.addContext("", docBase.toString());           // 根应用
        Wrapper def = Tomcat.addServlet(ctx, "default", new DefaultServlet());
        def.addInitParameter("listings", "false");                         // 生产环境不要列目录
        ctx.addServletMappingDecoded("/", "default");                      // "/" = 默认 Servlet
        ctx.addWelcomeFile("index.html");
        ctx.addMimeMapping("html", "text/html");
        ctx.addMimeMapping("txt", "text/plain");

        Tomcat.addServlet(ctx, "mini", new MiniDefaultServlet(docBase));
        ctx.addServletMappingDecoded("/mini/*", "mini");

        // 错误页：按状态码、按异常类型
        ctx.addErrorPage(errorPage(404, null, "/errors/404"));
        ctx.addErrorPage(errorPage(0, IllegalStateException.class.getName(), "/errors/exception"));
        Tomcat.addServlet(ctx, "errors", new ErrorServlet());
        ctx.addServletMappingDecoded("/errors/*", "errors");

        Tomcat.addServlet(ctx, "boom", new HttpServlet() {
            @Override protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
                throw new IllegalStateException("库存不足");
            }
        });
        ctx.addServletMappingDecoded("/boom", "boom");
        Tomcat.addServlet(ctx, "crash", new HttpServlet() {
            @Override protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
                throw new UnsupportedOperationException("没有为这种异常配置错误页");
            }
        });
        ctx.addServletMappingDecoded("/crash", "crash");

        // 字符编码：默认设置 vs 应用级 UTF-8
        Tomcat.addServlet(ctx, "echo", new EchoServlet());
        ctx.addServletMappingDecoded("/echo", "echo");
        Context utf8 = tomcat.addContext("/utf8", docBase.toString());
        utf8.setRequestCharacterEncoding("UTF-8");                         // web.xml: <request-character-encoding>
        utf8.setResponseCharacterEncoding("UTF-8");                        // web.xml: <response-character-encoding>
        Tomcat.addServlet(utf8, "echo", new EchoServlet());
        utf8.addServletMappingDecoded("/echo", "echo");

        tomcat.start();
        System.out.println("Tomcat 11 已启动：curl -i http://localhost:" + port + "/big.txt");
        tomcat.getServer().await();
    }

    private static ErrorPage errorPage(int code, String exceptionType, String location) {
        var page = new ErrorPage();
        if (code > 0) page.setErrorCode(code);
        if (exceptionType != null) page.setExceptionType(exceptionType);
        page.setLocation(location);
        return page;
    }

    /** 错误页本身也是一个 Servlet：容器用 ERROR 类型的分派把请求转给它，并放上 jakarta.servlet.error.* 属性 */
    static final class ErrorServlet extends HttpServlet {
        @Override protected void service(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            resp.setContentType("text/plain;charset=UTF-8");
            Throwable ex = (Throwable) req.getAttribute(RequestDispatcher.ERROR_EXCEPTION);
            resp.getWriter().printf("""
                    出错了（这是应用自己的错误页 %s）
                    dispatcherType = %s
                    status_code    = %s
                    request_uri    = %s
                    servlet_name   = %s
                    exception      = %s
                    """, req.getPathInfo(), req.getDispatcherType(),
                    req.getAttribute(RequestDispatcher.ERROR_STATUS_CODE),
                    req.getAttribute(RequestDispatcher.ERROR_REQUEST_URI),
                    req.getAttribute(RequestDispatcher.ERROR_SERVLET_NAME),
                    ex == null ? "无" : ex.getClass().getSimpleName() + ": " + ex.getMessage());
        }
    }

    static final class EchoServlet extends HttpServlet {
        @Override protected void service(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String before = req.getCharacterEncoding();                     // 读参数之前看一眼
            String name = req.getParameter("name");
            if ("latin".equals(req.getParameter("out"))) {
                resp.setContentType("text/plain");                         // 不指定 charset
            } else {
                resp.setContentType("text/plain;charset=UTF-8");
            }
            resp.getWriter().printf("%s %s%n请求编码 = %s%nname = %s%n响应编码 = %s%n",
                    req.getMethod(), req.getContextPath() + req.getServletPath(),
                    before, name, resp.getCharacterEncoding());
        }
    }

    /** 准备一个小站点：首页、子目录、1000 行的文本文件 */
    private static Path createSite() throws IOException {
        Path dir = Files.createTempDirectory("ch22-site");
        Files.writeString(dir.resolve("index.html"), "<h1>首页</h1>\n");
        Files.createDirectories(dir.resolve("docs"));
        Files.writeString(dir.resolve("docs/index.html"), "<h1>文档首页</h1>\n");
        Files.writeString(dir.resolve("big.txt"), IntStream.rangeClosed(1, 1000)
                .mapToObj(i -> String.format("line %04d%n", i)).collect(Collectors.joining()));
        return dir;
    }
}
