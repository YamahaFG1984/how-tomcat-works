import jakarta.servlet.ServletConfig;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Collections;

/** 第 11 章新增：打印 Wrapper 交给它的 ServletConfig（名字、初始化参数、真实类型） */
public class ConfigServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        ServletConfig config = getServletConfig();
        response.setContentType("text/plain; charset=UTF-8");
        var out = response.getWriter();
        out.println("servletName = " + config.getServletName());
        for (String name : Collections.list(config.getInitParameterNames())) {
            out.println("initParam   " + name + " = " + config.getInitParameter(name));
        }
        out.println("config 的真实类型 = " + config.getClass().getName());
    }
}
