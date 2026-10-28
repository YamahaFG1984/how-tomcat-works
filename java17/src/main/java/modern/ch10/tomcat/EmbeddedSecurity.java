package modern.ch10.tomcat;

import org.apache.catalina.Context;
import org.apache.catalina.authenticator.BasicAuthenticator;
import org.apache.catalina.realm.MemoryRealm;
import org.apache.catalina.startup.Constants;
import org.apache.catalina.startup.ContextConfig;
import org.apache.catalina.startup.Tomcat;
import org.apache.tomcat.util.descriptor.web.LoginConfig;
import org.apache.tomcat.util.descriptor.web.SecurityCollection;
import org.apache.tomcat.util.descriptor.web.SecurityConstraint;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 同样的安全配置跑在 Tomcat 11 上：MemoryRealm 读 conf/tomcat-users.xml，
 * 约束要求 manager 角色，BasicAuthenticator 作为阀装进 Context 的管道。
 * （正常部署时这些都写在 web.xml 里，由 ContextConfig 读出并自动安装认证阀。）
 *
 * 运行：./run.sh ch10.tomcat.EmbeddedSecurity   访问 http://localhost:8080/myApp/WhoAmI
 */
public final class EmbeddedSecurity {

    public static void main(String[] args) throws Exception {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 8080;
        Path myApp = Path.of("myApp").toAbsolutePath();

        var tomcat = new Tomcat();
        tomcat.setBaseDir(Files.createTempDirectory("tomcat-ch10").toString());
        tomcat.setPort(port);
        tomcat.getConnector().setProperty("address", "127.0.0.1");

        Context context = tomcat.addContext("/myApp", myApp.toString());
        var config = new ContextConfig();
        config.setDefaultWebXml(Constants.NoDefaultWebXml);
        context.addLifecycleListener(config);

        var realm = new MemoryRealm();
        realm.setPathname(Path.of("conf", "tomcat-users.xml").toAbsolutePath().toString());
        context.setRealm(realm);

        var collection = new SecurityCollection();
        collection.addPattern("/*");
        collection.addMethod("GET");
        var constraint = new SecurityConstraint();
        constraint.addCollection(collection);
        constraint.addAuthRole("manager");
        context.addConstraint(constraint);
        context.addSecurityRole("manager");

        var loginConfig = new LoginConfig();
        loginConfig.setAuthMethod("BASIC");
        loginConfig.setRealmName("Tomcat 11 Realm");
        context.setLoginConfig(loginConfig);
        context.getPipeline().addValve(new BasicAuthenticator());

        tomcat.start();
        System.out.println("Tomcat 11 已启动：http://localhost:" + port + "/myApp/WhoAmI  （admin / password）");
        tomcat.getServer().await();
    }
}
