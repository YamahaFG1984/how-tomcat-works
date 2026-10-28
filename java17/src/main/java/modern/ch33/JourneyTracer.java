package modern.ch33;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.Filter;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequestEvent;
import jakarta.servlet.ServletRequestListener;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSessionEvent;
import jakarta.servlet.http.HttpSessionListener;
import org.apache.catalina.Context;
import org.apache.catalina.Wrapper;
import org.apache.catalina.authenticator.BasicAuthenticator;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.startup.Tomcat;
import org.apache.catalina.valves.ValveBase;
import org.apache.tomcat.util.descriptor.web.LoginConfig;
import org.apache.tomcat.util.descriptor.web.SecurityCollection;
import org.apache.tomcat.util.descriptor.web.SecurityConstraint;

import java.io.IOException;
import java.nio.file.Files;
import java.util.EnumSet;

/**
 * 综合实战：让一个请求自己「讲出」它的旅程。在 Engine、Host、Context、Wrapper 的管道上各装一个追踪阀，
 * 再加上请求监听器、会话监听器、BASIC 认证、过滤器和一次 forward，每一步都标出对应的章节。
 *   GET /shop/secure/orders/42   需要 tom/tom123（角色 user）
 *
 * 运行：./run.sh ch33.JourneyTracer
 *       curl -u tom:tom123 http://localhost:8080/shop/secure/orders/42
 */
public final class JourneyTracer {

    /** 打印时的缩进深度：每进入一层阀加一级，出来时减一级，形成「洋葱」 */
    private static final ThreadLocal<Integer> DEPTH = ThreadLocal.withInitial(() -> 0);

    static void trace(String chapter, String what) {
        System.out.printf("%-7s %s%s%n", chapter, "  ".repeat(DEPTH.get()), what);
    }

    /** 追踪阀：调用下一个阀之前和之后各打印一行（第 5 章的阀可以包住后续处理） */
    static final class TraceValve extends ValveBase {
        private final String chapter;
        private final String level;

        TraceValve(String chapter, String level) {
            super(true);                                   // 支持异步
            this.chapter = chapter;
            this.level = level;
        }

        @Override
        public void invoke(Request request, Response response) throws IOException, ServletException {
            trace(chapter, "→ " + level + " 管道：" + describe(request));
            DEPTH.set(DEPTH.get() + 1);
            try {
                getNext().invoke(request, response);       // 最后一个阀之后是这一层的基础阀，它负责找下一层容器
            } finally {
                DEPTH.set(DEPTH.get() - 1);
                trace(chapter, "← " + level + "（状态码 " + response.getStatus() + "）");
            }
        }

        private String describe(Request r) {
            return switch (level) {
                case "Engine" -> "线程 " + Thread.currentThread().getName() + "，Mapper 已选好 Host=" + r.getHost().getName()
                        + " Context=" + r.getContext().getName() + " Wrapper=" + r.getWrapper().getName();
                case "Host" -> "StandardEngineValve 交给了 Host " + r.getHost().getName();
                case "Context" -> "StandardHostValve 交给了 Context " + r.getContext().getPath() + "，用户 = " + r.getRemoteUser();
                default -> "StandardContextValve 交给了 Wrapper " + r.getWrapper().getName() + "，用户 = " + r.getRemoteUser();
            };
        }
    }

    public static void main(String[] args) throws Exception {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 8080;
        var tomcat = new Tomcat();
        tomcat.setBaseDir(Files.createTempDirectory("tomcat-ch33").toString());
        tomcat.setPort(port);
        tomcat.getConnector().setProperty("address", "127.0.0.1");
        tomcat.addUser("tom", "tom123");                   // Tomcat 在 Engine 上放了一个简单的内存 Realm（第 10 章）
        tomcat.addRole("tom", "user");

        tomcat.getEngine().getPipeline().addValve(new TraceValve("[13]", "Engine"));
        tomcat.getHost().getPipeline().addValve(new TraceValve("[13]", "Host"));

        Context ctx = tomcat.addContext("/shop", Files.createTempDirectory("ch33-docbase").toString());
        ctx.getPipeline().addValve(new TraceValve("[12]", "Context"));

        // 第 10 章：/secure/* 需要 user 角色，BASIC 认证。认证器本身也是 Context 管道上的一个阀
        var collection = new SecurityCollection();
        collection.addPattern("/secure/*");
        var constraint = new SecurityConstraint();
        constraint.addCollection(collection);
        constraint.addAuthRole("user");
        ctx.addConstraint(constraint);
        ctx.addSecurityRole("user");
        var login = new LoginConfig();
        login.setAuthMethod("BASIC");
        login.setRealmName("shop");
        ctx.setLoginConfig(login);
        ctx.getPipeline().addValve(new BasicAuthenticator());

        // 第 21、25 章：用 SCI 编程式注册监听器、过滤器、Servlet
        ctx.addServletContainerInitializer((classes, sc) -> {
            sc.addListener(new ServletRequestListener() {
                @Override public void requestInitialized(ServletRequestEvent e) {
                    trace("[21]", "ServletRequestListener.requestInitialized");
                }
                @Override public void requestDestroyed(ServletRequestEvent e) {
                    trace("[21]", "ServletRequestListener.requestDestroyed");
                }
            });
            sc.addListener(new HttpSessionListener() {
                @Override public void sessionCreated(HttpSessionEvent e) {
                    trace("[9]", "HttpSessionListener.sessionCreated id=" + e.getSession().getId().substring(0, 8) + "…");
                }
            });
            Filter audit = (req, resp, chain) -> {
                trace("[11]", "→ AuditFilter（" + req.getDispatcherType() + "）");
                DEPTH.set(DEPTH.get() + 1);
                try {
                    chain.doFilter(req, resp);
                } finally {
                    DEPTH.set(DEPTH.get() - 1);
                    trace("[11]", "← AuditFilter");
                }
            };
            sc.addFilter("audit", audit).addMappingForUrlPatterns(
                    EnumSet.of(DispatcherType.REQUEST, DispatcherType.FORWARD), false, "/*");
        }, null);

        Wrapper orders = Tomcat.addServlet(ctx, "orders", new HttpServlet() {
            @Override protected void doGet(HttpServletRequest req, HttpServletResponse resp)
                    throws ServletException, IOException {
                trace("[2,11]", "OrderServlet.doGet：servletPath=" + req.getServletPath() + " pathInfo=" + req.getPathInfo()
                        + " 参数 id=" + req.getPathInfo().substring(1));
                req.getSession();                          // 第 9 章：第一次调用时由 Manager 创建会话
                req.setAttribute("order", "订单 #" + req.getPathInfo().substring(1) + "（" + req.getRemoteUser() + "）");
                trace("[21]", "forward → /WEB-INF/views/order");
                req.getRequestDispatcher("/WEB-INF/views/order").forward(req, resp);
            }
        });
        ctx.addServletMappingDecoded("/secure/orders/*", "orders");
        orders.getPipeline().addValve(new TraceValve("[11]", "Wrapper"));

        Tomcat.addServlet(ctx, "view", new HttpServlet() {
            @Override protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
                trace("[21]", "OrderView 渲染（dispatcherType=" + req.getDispatcherType() + "）");
                resp.setContentType("text/plain;charset=UTF-8");
                resp.getWriter().println(req.getAttribute("order"));
            }
        });
        ctx.addServletMappingDecoded("/WEB-INF/views/*", "view");

        tomcat.start();
        System.out.println("就绪：curl -u tom:tom123 http://localhost:" + port + "/shop/secure/orders/42\n");
        tomcat.getServer().await();
    }
}
