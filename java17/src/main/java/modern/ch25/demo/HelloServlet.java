package modern.ch25.demo;

import jakarta.servlet.annotation.WebInitParam;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/** 用注解代替 web.xml 里的 &lt;servlet&gt; + &lt;servlet-mapping&gt; + &lt;init-param&gt; */
@WebServlet(urlPatterns = "/hello", loadOnStartup = 1,
        initParams = @WebInitParam(name = "greeting", value = "你好"))
public class HelloServlet extends HttpServlet {

    @Override
    public void init() {
        System.out.println("[HelloServlet] init()，greeting = " + getInitParameter("greeting"));
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("text/plain;charset=UTF-8");
        resp.getWriter().println(getInitParameter("greeting") + "，来自 " + getServletName());
    }
}
