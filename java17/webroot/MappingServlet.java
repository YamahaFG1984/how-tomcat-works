import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletMapping;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/** 第 12 章新增：显示这个请求是按哪条映射规则找到 Servlet 的 */
public class MappingServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("text/plain; charset=UTF-8");
        var out = response.getWriter();
        out.println("requestURI   = " + request.getRequestURI());
        out.println("servletPath  = " + request.getServletPath());
        out.println("pathInfo     = " + request.getPathInfo());
        HttpServletMapping m = request.getHttpServletMapping();      // Servlet 4.0 新增
        out.println("mappingMatch = " + m.getMappingMatch() + ", pattern = " + m.getPattern()
                + ", matchValue = " + m.getMatchValue());
    }
}
