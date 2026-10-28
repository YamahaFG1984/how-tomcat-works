package modern.ch21.mini;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 不开端口，在内存里演示 forward / include / 按名字分派 / 已提交后 forward 的结果。
 * 运行：./run.sh ch21.mini.DispatcherDemo
 */
public final class DispatcherDemo {

    public static void main(String[] args) throws Exception {
        var app = new MiniApplicationContext("/shop");
        app.setInitParameter("site.name", "迷你商店");
        app.setAttribute("visits", new AtomicLong());

        // ① 前端控制器：处理业务、把结果放进请求属性，然后 forward 给视图
        app.addServlet("Front", new HttpServlet() {
            @Override protected void doGet(HttpServletRequest req, HttpServletResponse resp)
                    throws ServletException, IOException {
                ((AtomicLong) app.getAttribute("visits")).incrementAndGet();
                resp.getWriter().println("这行会被 forward 清掉");
                req.setAttribute("product", "商品 #" + req.getPathInfo().substring(1));
                app.getRequestDispatcher("/WEB-INF/views/product.view?lang=zh").forward(req, resp);
                resp.getWriter().println("forward 之后写的内容也看不到");
            }
        }, "/products/*");

        // ② 视图：只负责渲染。它看到的路径是目标路径，原始路径在 forward 属性里
        app.addServlet("View", new HttpServlet() {
            @Override protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
                resp.setContentType("text/html");
                var out = resp.getWriter();
                out.println("<h1>" + req.getAttribute("product") + "</h1>");
                out.println("  dispatcherType = " + req.getDispatcherType());
                out.println("  requestURI     = " + req.getRequestURI());
                out.println("  servletPath    = " + req.getServletPath() + "  pathInfo = " + req.getPathInfo());
                out.println("  lang 参数      = " + req.getParameter("lang") + "（来自分派路径的查询串）");
                out.println("  属性           = " + MiniRequestDispatcher.dispatchAttributes(req));
            }
        }, "*.view");

        // ③ 页面 = 头部片段 + 正文 + 尾部片段
        app.addServlet("Page", new HttpServlet() {
            @Override protected void doGet(HttpServletRequest req, HttpServletResponse resp)
                    throws ServletException, IOException {
                resp.setContentType("text/html");
                var out = resp.getWriter();
                app.getRequestDispatcher("/fragments/header?title=首页").include(req, resp);
                out.println("<main>正文：" + app.getInitParameter("site.name") + "</main>");
                app.getNamedDispatcher("Fragment").include(req, resp);
            }
        }, "/page");

        // ④ 片段：试图改状态码和响应头（会被忽略），并打印自己看到的路径
        app.addServlet("Fragment", new HttpServlet() {
            @Override protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
                resp.setStatus(500);
                resp.setHeader("X-From-Fragment", "yes");
                resp.getWriter().println("  <fragment> type=" + req.getDispatcherType()
                        + " requestURI=" + req.getRequestURI()
                        + " title=" + req.getParameter("title")
                        + " 属性=" + MiniRequestDispatcher.dispatchAttributes(req));
            }
        }, "/fragments/*");

        // ⑤ 先 flush 再 forward：响应已提交
        app.addServlet("Late", new HttpServlet() {
            @Override protected void doGet(HttpServletRequest req, HttpServletResponse resp)
                    throws ServletException, IOException {
                resp.getWriter().println("先输出一部分");
                resp.flushBuffer();
                try {
                    app.getRequestDispatcher("/page").forward(req, resp);
                } catch (IllegalStateException e) {
                    resp.getWriter().println("forward 失败：" + e.getMessage());
                }
            }
        }, "/late");

        show(app, "/products/42?sort=price");
        show(app, "/page");
        show(app, "/late");
        show(app, "/WEB-INF/views/product.view");
        System.out.println("应用级属性 visits = " + app.getAttribute("visits"));
    }

    private static void show(MiniApplicationContext app, String uri) throws Exception {
        MockResponse resp = app.handle("GET", uri);
        System.out.println("GET " + app.getContextPath() + uri + "  →  " + resp);
        resp.body().lines().forEach(l -> System.out.println("  │ " + l));
        System.out.println();
    }
}
