package modern.ch10.startup;

import modern.ch04.connector.HttpConnector;
import modern.ch06.core.SimpleWrapper;
import modern.ch08.loader.WebappLoader;
import modern.ch10.core.LoginConfig;
import modern.ch10.core.SecureContext;
import modern.ch10.core.SecurityConstraint;
import modern.ch10.core.SimpleContextConfig;
import modern.ch10.realm.Realm;
import modern.ch10.realm.SimpleRealm;

import java.util.Set;

/**
 * 第 10 章应用 1：所有 GET 请求都需要 manager 角色；用户写死在 SimpleRealm 里（ken / blackcomb）。
 * 运行：./run.sh ch10.startup.Bootstrap1     访问 /Primitive、/Modern、/WhoAmI
 */
public final class Bootstrap1 {

    public static void main(String[] args) throws Exception {
        run(args, new SimpleRealm(), "Simple Realm");
    }

    static void run(String[] args, Realm realm, String realmName) throws Exception {
        System.setProperty("catalina.base", System.getProperty("user.dir"));
        var connector = new HttpConnector(args.length > 0 ? Integer.parseInt(args[0]) : 8080);

        var context = new SecureContext();
        context.setDocBase("myApp");
        context.addLifecycleListener(new SimpleContextConfig());
        for (String name : new String[] {"Primitive", "Modern", "WhoAmI"}) {
            var wrapper = new SimpleWrapper();
            wrapper.setName(name);
            wrapper.setServletClass(name + "Servlet");
            context.addChild(wrapper);
            context.addServletMapping("/" + name, name);
        }
        context.setLoader(new WebappLoader(context.getDocBase(), context::reload));

        // 相当于 web.xml 中的 <security-constraint> 和 <login-config>
        context.addConstraint(new SecurityConstraint(Set.of("/*"), Set.of("GET"), Set.of("manager")));
        context.setLoginConfig(new LoginConfig("BASIC", realmName));
        context.setRealm(realm);

        connector.setContainer(context);
        connector.initialize();
        connector.start();
        context.start();

        System.in.read();
        context.stop();
        connector.stop();
    }
}
