import jakarta.servlet.ServletConfig;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collections;

/** 原书 webroot/ModernServlet.java 的 jakarta 版：打印请求头、方法、参数、查询串和 URI */
public class ModernServlet extends HttpServlet {

    @Override
    public void init(ServletConfig config) {
        System.out.println("ModernServlet -- init");
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("text/html; charset=UTF-8");
        PrintWriter out = response.getWriter();
        out.println("<html><head><title>Modern Servlet</title></head><body>");

        out.println("<h2>Headers</h2>");
        for (String header : Collections.list(request.getHeaderNames())) {
            out.println("<br>" + header + " : " + request.getHeader(header));
        }

        out.println("<h2>Method</h2>");
        out.println("<br>" + request.getMethod());

        out.println("<h2>Parameters</h2>");
        for (String name : Collections.list(request.getParameterNames())) {
            out.println("<br>" + name + " : " + request.getParameter(name));
        }

        out.println("<h2>Query String</h2>");
        out.println("<br>" + request.getQueryString());

        out.println("<h2>Request URI</h2>");
        out.println("<br>" + request.getRequestURI());

        out.println("</body></html>");
    }

    /** 原书只有 doGet；Java 17 版加上 doPost，方便测试请求体里的表单参数 */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        doGet(request, response);
    }
}
