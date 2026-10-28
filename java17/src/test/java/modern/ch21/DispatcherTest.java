package modern.ch21;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import modern.ch21.mini.MiniApplicationContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/** 第 21 章：迷你 RequestDispatcher 的 forward / include 语义 */
class DispatcherTest {

    private MiniApplicationContext app;

    @BeforeEach
    void setUp() throws ServletException {
        app = new MiniApplicationContext("/shop");
        app.addServlet("front", new HttpServlet() {
            @Override protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
                resp.getWriter().print("被清掉的内容");
                req.setAttribute("product", "42");
                app.getRequestDispatcher("/views/p.view?lang=zh").forward(req, resp);
                resp.getWriter().print("forward 之后的内容");
            }
        }, "/products/*");
        app.addServlet("view", new HttpServlet() {
            @Override protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
                resp.getWriter().print(req.getAttribute("product") + "|" + req.getRequestURI() + "|" + req.getParameter("lang")
                        + "|" + req.getAttribute(RequestDispatcher.FORWARD_REQUEST_URI) + "|" + req.getDispatcherType());
            }
        }, "*.view");
        app.addServlet("page", new HttpServlet() {
            @Override protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
                resp.getWriter().print("[");
                app.getRequestDispatcher("/fragment").include(req, resp);
                resp.getWriter().print("]");
            }
        }, "/page");
        app.addServlet("fragment", new HttpServlet() {
            @Override protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
                resp.setStatus(500);
                resp.setHeader("X-Fragment", "1");
                resp.getWriter().print(req.getRequestURI());
            }
        }, "/fragment");
    }

    @Test
    void forwardReplacesPathKeepsAttributesAndClosesResponse() throws Exception {
        var resp = app.handle("GET", "/products/42");
        assertEquals("42|/shop/views/p.view|zh|/shop/products/42|FORWARD", resp.body());
    }

    @Test
    void includeKeepsOriginalPathAndCannotChangeStatusOrHeaders() throws Exception {
        var resp = app.handle("GET", "/page");
        assertEquals("[/shop/page]", resp.body());
        assertEquals(200, resp.getStatus());
        assertNull(resp.getHeader("X-Fragment"));
    }

    @Test
    void webInfIsNotReachableFromClient() throws Exception {
        assertEquals(404, app.handle("GET", "/WEB-INF/x.view").getStatus());
        assertNull(app.getRequestDispatcher("relative"));          // 相对路径不归 ServletContext 管
    }
}
