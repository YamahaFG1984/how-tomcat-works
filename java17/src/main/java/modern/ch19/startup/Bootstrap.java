package modern.ch19.startup;

import jakarta.servlet.http.HttpServletResponse;
import modern.ch04.connector.HttpConnector;
import modern.ch08.loader.WebappLoader;
import modern.ch10.realm.UserDatabaseRealm;
import modern.ch12.core.StandardContext;
import modern.ch13.core.StandardEngine;
import modern.ch13.core.StandardHost;
import modern.ch18.startup.HostConfig;
import modern.ch18.startup.StandardHostDeployer;
import modern.ch19.core.ContainerServletWrapper;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.Principal;
import java.util.Base64;

/**
 * 第 19 章：在第 18 章的自动部署基础上，加一个 privileged 的 /manager 应用。
 * 它受 BASIC 认证保护，需要 manager 角色（conf/tomcat-users.xml 中的 admin / password）。
 *
 * 运行：./run.sh ch19.startup.Bootstrap
 *   curl -u admin:password http://localhost:8080/manager/list
 *   curl -u admin:password 'http://localhost:8080/manager/stop?path=/app1'
 */
public final class Bootstrap {

    public static void main(String[] args) throws Exception {
        System.setProperty("catalina.base", System.getProperty("user.dir"));
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 8080;
        Path appBase = modern.ch18.startup.Bootstrap.prepareAppBase();

        var host = new StandardHost("localhost");
        host.setAppBase(appBase.toString());
        host.addLifecycleListener(new HostConfig(new StandardHostDeployer(host), appBase));
        host.addChild(managerContext());

        var engine = new StandardEngine();
        engine.addChild(host);
        engine.setDefaultHost("localhost");
        var connector = new HttpConnector(port);
        connector.setContainer(engine);
        connector.initialize();
        connector.start();
        engine.start();

        System.in.read();
        engine.stop();
        connector.stop();
    }

    /** 相当于原书的 manager.xml：<Context path="/manager" docBase="..." privileged="true"> */
    static StandardContext managerContext() throws Exception {
        var context = new StandardContext();
        context.setPath("/manager");
        Path docBase = Files.createDirectories(Path.of("target", "manager"));
        context.setDocBase(docBase.toString());
        context.setPrivileged(true);
        context.setLoader(new WebappLoader(context.getDocBase(), context::reload));

        var wrapper = new ContainerServletWrapper();
        wrapper.setName("Manager");
        wrapper.setServletClass("modern.ch19.manager.ManagerServlet");
        context.addChild(wrapper);
        context.addServletMapping("/*", "Manager");

        // 相当于 manager 的 web.xml 里的 <security-constraint> + <login-config>BASIC</login-config>
        var realm = new UserDatabaseRealm();
        realm.createDatabase(Path.of("conf", "tomcat-users.xml"));
        context.getPipeline().addValve((request, response, next) -> {
            Principal user = basic(realm, request.getHeader("Authorization"));
            if (user == null || !realm.hasRole(user, "manager")) {
                response.setHeader("WWW-Authenticate", "Basic realm=\"Tomcat Manager Application\"");
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, null);
                return;
            }
            next.invokeNext(request, response);
        });
        return context;
    }

    private static Principal basic(UserDatabaseRealm realm, String header) {
        if (header == null || !header.startsWith("Basic ")) {
            return null;
        }
        String decoded = new String(Base64.getDecoder().decode(header.substring(6)), StandardCharsets.UTF_8);
        int colon = decoded.indexOf(':');
        return colon < 0 ? null : realm.authenticate(decoded.substring(0, colon), decoded.substring(colon + 1));
    }
}
