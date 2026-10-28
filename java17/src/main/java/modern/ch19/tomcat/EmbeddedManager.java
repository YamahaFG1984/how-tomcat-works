package modern.ch19.tomcat;

import org.apache.catalina.Context;
import org.apache.catalina.Wrapper;
import org.apache.catalina.authenticator.BasicAuthenticator;
import org.apache.catalina.realm.MemoryRealm;
import org.apache.catalina.startup.Tomcat;
import org.apache.tomcat.util.descriptor.web.LoginConfig;
import org.apache.tomcat.util.descriptor.web.SecurityCollection;
import org.apache.tomcat.util.descriptor.web.SecurityConstraint;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 把 Tomcat 11 自带的 org.apache.catalina.manager.ManagerServlet 部署到一个 privileged 的 /manager 上，
 * 映射到 /text/*（现代 Manager 的「文本接口」），并要求 manager-script 角色。
 *
 * 运行：./run.sh ch19.tomcat.EmbeddedManager
 *   curl -u script:script http://localhost:8080/manager/text/list
 *   curl -u script:script 'http://localhost:8080/manager/text/stop?path=/app1'
 */
public final class EmbeddedManager {

    public static void main(String[] args) throws Exception {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 8080;
        var tomcat = new Tomcat();
        tomcat.setBaseDir(Files.createTempDirectory("tomcat-ch19").toString());
        tomcat.setPort(port);
        tomcat.getConnector().setProperty("address", "127.0.0.1");

        Context app1 = tomcat.addContext("/app1", Path.of("webapps/app1").toAbsolutePath().toString());
        Tomcat.addServlet(app1, "Primitive", "PrimitiveServlet");
        app1.addServletMappingDecoded("/Primitive", "Primitive");

        Context manager = tomcat.addContext("/manager", Files.createTempDirectory("manager").toString());
        manager.setPrivileged(true);                                    // 没有它，ManagerServlet 拿不到 Wrapper
        Wrapper servlet = Tomcat.addServlet(manager, "Manager", "org.apache.catalina.manager.ManagerServlet");
        manager.addServletMappingDecoded("/text/*", servlet.getName());

        // 安全约束：/text/* 需要 manager-script 角色，BASIC 认证
        var realm = new MemoryRealm();
        realm.setPathname(writeUsers().toString());
        manager.setRealm(realm);
        var collection = new SecurityCollection();
        collection.addPattern("/text/*");
        var constraint = new SecurityConstraint();
        constraint.addCollection(collection);
        constraint.addAuthRole("manager-script");
        manager.addConstraint(constraint);
        manager.addSecurityRole("manager-script");
        var login = new LoginConfig();
        login.setAuthMethod("BASIC");
        login.setRealmName("Tomcat Manager Application");
        manager.setLoginConfig(login);
        manager.getPipeline().addValve(new BasicAuthenticator());

        tomcat.start();
        System.out.println("Tomcat 11 已启动：curl -u script:script http://localhost:" + port + "/manager/text/list");
        tomcat.getServer().await();
    }

    private static Path writeUsers() throws Exception {
        Path users = Files.createTempFile("tomcat-users", ".xml");
        Files.writeString(users, """
                <tomcat-users>
                  <role rolename="manager-script"/>
                  <user username="script" password="script" roles="manager-script"/>
                </tomcat-users>
                """);                                                  // Java 15 文本块
        return users;
    }
}
