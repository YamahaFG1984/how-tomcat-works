package modern.ch25.plugin;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/** 没有注解，由插件 jar 里的 META-INF/web-fragment.xml 声明和映射 */
public class FragmentServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("text/plain;charset=UTF-8");
        resp.getWriter().println("来自 web-fragment.xml 的 Servlet，类加载器：" + getClass().getClassLoader().getClass().getSimpleName());
    }
}
