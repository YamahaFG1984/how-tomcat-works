package modern.ch19.manager;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import modern.ch05.core.SimpleWrapper;
import modern.ch06.core.LifecycleException;
import modern.ch12.core.StandardContext;
import modern.ch13.core.StandardHost;
import modern.ch18.startup.StandardHostDeployer;
import modern.ch19.core.ContainerServlet;

import java.io.IOException;
import java.io.PrintWriter;

/**
 * 迷你 Manager 应用（原书 org.apache.catalina.servlets.ManagerServlet，Tomcat 4 版）。
 * 命令通过 pathInfo 指定：/manager/list、/manager/start?path=/app1 ……
 * 它能做这些事，全靠 setWrapper 拿到的 Wrapper：Wrapper → Context（自己）→ Host（部署器）。
 */
public class ManagerServlet extends HttpServlet implements ContainerServlet {

    private SimpleWrapper wrapper;
    private StandardContext context;
    private StandardHostDeployer deployer;

    @Override
    public SimpleWrapper getWrapper() {
        return wrapper;
    }

    @Override
    public void setWrapper(SimpleWrapper wrapper) {
        this.wrapper = wrapper;
        if (wrapper == null) {
            context = null;
            deployer = null;
        } else {
            context = (StandardContext) wrapper.getParent();
            deployer = new StandardHostDeployer((StandardHost) context.getParent());
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("text/plain; charset=UTF-8");
        PrintWriter writer = response.getWriter();
        String command = request.getPathInfo() == null ? "/list" : request.getPathInfo();
        String path = request.getParameter("path");
        switch (command) {
            case "/list" -> list(writer);
            case "/start" -> manage(writer, path, "started", StandardContext::start);
            case "/stop" -> manage(writer, path, "stopped", StandardContext::stop);
            case "/reload" -> manage(writer, path, "reloaded", StandardContext::reload);
            case "/sessions" -> sessions(writer, path);
            case "/undeploy" -> undeploy(writer, path);
            default -> writer.println("FAIL - Unknown command " + command);
        }
    }

    /** 对应原书 Listing 19.3 */
    private void list(PrintWriter writer) {
        writer.println("OK - Listed applications for virtual host " + context.getParent().getName());
        for (String contextPath : deployer.findDeployedApps()) {
            StandardContext ctx = deployer.findDeployedApp(contextPath);
            int sessions = ctx.getManager() == null ? 0 : ctx.getManager().findSessions().size();
            writer.println((contextPath.isEmpty() ? "/" : contextPath) + ":"
                    + (ctx.isRunning() ? "running" : "stopped") + ":" + sessions + ":" + ctx.getDocBase());
        }
    }

    @FunctionalInterface
    private interface ContextAction {
        void apply(StandardContext context) throws LifecycleException;
    }

    /** start / stop / reload 的公共部分（对应原书 Listing 19.4、19.5） */
    private void manage(PrintWriter writer, String path, String done, ContextAction action) {
        StandardContext target = find(writer, path);
        if (target == null) {
            return;
        }
        if (target == context) {
            writer.println("FAIL - The manager can not reload, undeploy, stop, or start itself");
            return;
        }
        try {
            action.apply(target);
            writer.println("OK - " + capitalize(done) + " application at context path " + path);
        } catch (LifecycleException | RuntimeException e) {
            writer.println("FAIL - Encountered exception " + e);
        }
    }

    private void sessions(PrintWriter writer, String path) {
        StandardContext target = find(writer, path);
        if (target != null) {
            int count = target.getManager() == null ? 0 : target.getManager().findSessions().size();
            writer.println("OK - Session information for application at context path " + path);
            writer.println(count + " active session(s)");
        }
    }

    private void undeploy(PrintWriter writer, String path) {
        StandardContext target = find(writer, path);
        if (target == null) {
            return;
        }
        if (target == context) {
            writer.println("FAIL - The manager can not reload, undeploy, stop, or start itself");
            return;
        }
        try {
            deployer.remove(target.getPath());
            writer.println("OK - Undeployed application at context path " + path);
        } catch (LifecycleException e) {
            writer.println("FAIL - Encountered exception " + e);
        }
    }

    private StandardContext find(PrintWriter writer, String path) {
        if (path == null || !path.startsWith("/")) {
            writer.println("FAIL - Invalid context path " + path + " was specified");
            return null;
        }
        StandardContext target = deployer.findDeployedApp(path.equals("/") ? "" : path);
        if (target == null) {
            writer.println("FAIL - No context exists for path " + path);
        }
        return target;
    }

    private static String capitalize(String s) {
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
