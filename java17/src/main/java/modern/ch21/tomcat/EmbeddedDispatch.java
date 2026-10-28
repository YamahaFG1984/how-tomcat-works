package modern.ch21.tomcat;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.Filter;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.catalina.Context;
import org.apache.catalina.startup.Tomcat;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.EnumSet;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Tomcat 11 的 ServletContext 与 RequestDispatcher：
 *   /app/products/42  前端控制器 forward 到 /WEB-INF/views/product（客户端直接访问 WEB-INF 会 404）
 *   /app/page         include 头部和尾部片段；片段里的 setStatus/setHeader 被忽略
 *   /app/old          sendRedirect：客户端会收到 302，再发第二个请求
 *   /app/stats        应用级属性（访问计数）+ 上下文初始化参数
 * 两个过滤器分别只拦截 REQUEST 和 FORWARD/INCLUDE，观察控制台输出。
 *
 * 运行：./run.sh ch21.tomcat.EmbeddedDispatch
 */
public final class EmbeddedDispatch {

    public static void main(String[] args) throws Exception {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 8080;
        var tomcat = new Tomcat();
        tomcat.setBaseDir(Files.createTempDirectory("tomcat-ch21").toString());
        tomcat.setPort(port);
        tomcat.getConnector().setProperty("address", "127.0.0.1");

        Context ctx = tomcat.addContext("/app", Files.createTempDirectory("ch21-docbase").toString());
        ctx.addParameter("site.name", "Tomcat 教程商店");            // 相当于 web.xml 的 <context-param>
        // 不写 web.xml，用 ServletContainerInitializer 在启动时编程式注册（第 25 章细讲）
        ctx.addServletContainerInitializer((classes, sc) -> register(sc), null);

        tomcat.start();
        System.out.println("Tomcat 11 已启动：curl -i http://localhost:" + port + "/app/products/42");
        tomcat.getServer().await();
    }

    private static void register(ServletContext sc) {
        sc.setAttribute("visits", new AtomicLong());

        sc.addServlet("front", new HttpServlet() {
            @Override protected void doGet(HttpServletRequest req, HttpServletResponse resp)
                    throws ServletException, IOException {
                ((AtomicLong) getServletContext().getAttribute("visits")).incrementAndGet();
                req.setAttribute("product", "商品 #" + req.getPathInfo().substring(1));
                req.getRequestDispatcher("/WEB-INF/views/product?lang=zh").forward(req, resp);
            }
        }).addMapping("/products/*");

        sc.addServlet("view", new HttpServlet() {
            @Override protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
                resp.setContentType("text/plain;charset=UTF-8");
                var out = resp.getWriter();
                out.println(req.getAttribute("product"));
                out.println("dispatcherType = " + req.getDispatcherType());
                out.println("requestURI     = " + req.getRequestURI());
                out.println("servletPath    = " + req.getServletPath());
                out.println("lang           = " + req.getParameter("lang"));
                out.println("forward.request_uri  = " + req.getAttribute(RequestDispatcher.FORWARD_REQUEST_URI));
                out.println("forward.query_string = " + req.getAttribute(RequestDispatcher.FORWARD_QUERY_STRING));
                out.println("mapping        = " + req.getHttpServletMapping().getMappingMatch()
                        + "，原始 = " + ((HttpServletRequest) req).getAttribute(RequestDispatcher.FORWARD_MAPPING));
            }
        }).addMapping("/WEB-INF/views/*");

        sc.addServlet("page", new HttpServlet() {
            @Override protected void doGet(HttpServletRequest req, HttpServletResponse resp)
                    throws ServletException, IOException {
                resp.setContentType("text/plain;charset=UTF-8");
                var out = resp.getWriter();
                // 分派路径是 URI：非 ASCII 的查询参数必须先编码，否则解析参数时抛 UnmappableCharacterException
                String title = URLEncoder.encode("首页", StandardCharsets.UTF_8);
                req.getRequestDispatcher("/fragments/header?title=" + title).include(req, resp);
                out.println("== 正文：" + getServletContext().getInitParameter("site.name") + " ==");
                out.flush();
                getServletContext().getNamedDispatcher("fragment").include(req, resp);
            }
        }).addMapping("/page");

        sc.addServlet("fragment", new HttpServlet() {
            @Override protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
                resp.setStatus(500);                          // 被忽略
                resp.setHeader("X-From-Fragment", "yes");     // 被忽略
                resp.getWriter().println("[片段] type=" + req.getDispatcherType()
                        + " requestURI=" + req.getRequestURI()
                        + " include.request_uri=" + req.getAttribute(RequestDispatcher.INCLUDE_REQUEST_URI)
                        + " title=" + req.getParameter("title"));
            }
        }).addMapping("/fragments/*");

        sc.addServlet("old", new HttpServlet() {
            @Override protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
                resp.sendRedirect(req.getContextPath() + "/products/7");
            }
        }).addMapping("/old");

        sc.addServlet("stats", new HttpServlet() {
            @Override protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
                ServletContext app = getServletContext();
                resp.setContentType("text/plain;charset=UTF-8");
                resp.getWriter().printf("站点：%s%n访问 /products/* 的次数：%s%nServer：%s%n",
                        app.getInitParameter("site.name"), app.getAttribute("visits"), app.getServerInfo());
            }
        }).addMapping("/stats");

        Filter onRequest = (req, resp, chain) -> {
            System.out.println("[REQUEST 过滤器]      " + ((HttpServletRequest) req).getRequestURI());
            chain.doFilter(req, resp);
        };
        sc.addFilter("onRequest", onRequest)
                .addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST), false, "/*");

        Filter onDispatch = (req, resp, chain) -> {
            System.out.println("[FORWARD/INCLUDE 过滤器] " + req.getDispatcherType() + " "
                    + ((HttpServletRequest) req).getRequestURI());
            chain.doFilter(req, resp);
        };
        sc.addFilter("onDispatch", onDispatch)
                .addMappingForUrlPatterns(EnumSet.of(DispatcherType.FORWARD, DispatcherType.INCLUDE), false, "/*");
    }
}
