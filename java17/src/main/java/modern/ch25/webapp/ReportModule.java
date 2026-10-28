package modern.ch25.webapp;

import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import modern.ch25.plugin.PluginModule;

import java.io.IOException;

/** 应用里的插件模块：实现插件 jar 定义的接口，不需要任何注解或配置就会被发现 */
public class ReportModule implements PluginModule {

    @Override
    public void register(ServletContext sc) {
        System.out.println("[ReportModule] 注册 /report");
        sc.addServlet("report", new HttpServlet() {
            @Override protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
                resp.setContentType("text/plain;charset=UTF-8");
                resp.getWriter().println("来自 ServletContainerInitializer + @HandlesTypes 的 Servlet");
            }
        }).addMapping("/report");
    }
}
