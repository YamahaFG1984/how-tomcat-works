import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.management.ManagementFactory;
import java.util.TreeSet;
import javax.management.MBeanServer;
import javax.management.ObjectName;

/**
 * 原书 MyAdminServlet 的现代版：通过 JMX 列出、停止、启动 Tomcat 里的 Web 应用。
 * 原书要从 ServletContext 属性里取得 MBeanServer（只有 privileged 应用才有）；
 * Java 5 起任何代码都能调用 ManagementFactory.getPlatformMBeanServer()——所以 JMX 端口一定要保护好。
 *
 *   ?action=listAllContexts
 *   ?action=stopContext&name=<ObjectName>   ?action=startContext&name=<ObjectName>
 *   ?action=listDomains
 */
public class JmxAdminServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("text/plain; charset=UTF-8");
        PrintWriter out = response.getWriter();
        MBeanServer server = ManagementFactory.getPlatformMBeanServer();
        String action = String.valueOf(request.getParameter("action"));
        try {
            switch (action) {
                case "listDomains" -> {
                    for (String domain : new TreeSet<>(java.util.List.of(server.getDomains()))) {
                        out.println(domain + "  (" + server.queryNames(new ObjectName(domain + ":*"), null).size() + " 个 MBean)");
                    }
                }
                case "listAllContexts" -> {
                    for (ObjectName name : new TreeSet<>(server.queryNames(new ObjectName("*:j2eeType=WebModule,*"), null))) {
                        out.println(name + "  state=" + server.getAttribute(name, "stateName"));
                    }
                }
                case "stopContext", "startContext" -> {
                    var name = new ObjectName(request.getParameter("name"));
                    server.invoke(name, action.equals("stopContext") ? "stop" : "start", null, null);
                    out.println(action + " OK: " + name + "  state=" + server.getAttribute(name, "stateName"));
                }
                default -> out.println("Invalid command");
            }
        } catch (Exception e) {
            out.println("FAIL - " + e);
        }
    }
}
