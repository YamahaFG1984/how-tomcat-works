package modern.ch11.startup;

import modern.ch04.connector.HttpConnector;
import modern.ch08.loader.WebappLoader;
import modern.ch11.core.FilterDef;
import modern.ch11.core.FilterMap;
import modern.ch11.core.StandardContext;
import modern.ch11.core.StandardWrapper;

import java.util.Map;

/**
 * 第 11 章：用 StandardWrapper 表示 Servlet，并给 Context 配两个过滤器。
 * 运行：./run.sh ch11.startup.Bootstrap    访问 /Primitive、/Modern、/Config
 */
public final class Bootstrap {

    public static void main(String[] args) throws Exception {
        System.setProperty("catalina.base", System.getProperty("user.dir"));
        var connector = new HttpConnector(args.length > 0 ? Integer.parseInt(args[0]) : 8080);

        var context = new StandardContext();
        context.setDocBase("myApp");
        for (String name : new String[] {"Primitive", "Modern", "Config"}) {
            var wrapper = new StandardWrapper();
            wrapper.setName(name);
            wrapper.setServletClass(name + "Servlet");
            context.addChild(wrapper);
            context.addServletMapping("/" + name, name);
        }
        ((StandardWrapper) context.findChild("Config")).addInitParameter("greeting", "你好，ServletConfig");
        context.setLoader(new WebappLoader(context.getDocBase(), context::reload));

        // 相当于 web.xml 里的 <filter> 和 <filter-mapping>，映射顺序就是执行顺序
        context.addFilterDef(new FilterDef("timing", "TimingFilter", Map.of("label", "计时")));
        context.addFilterDef(new FilterDef("poweredBy", "PoweredByFilter", Map.of()));
        context.addFilterMap(new FilterMap("timing", "/*"));
        context.addFilterMap(new FilterMap("poweredBy", "/Config"));

        connector.setContainer(context);
        connector.initialize();
        connector.start();
        context.start();

        System.in.read();
        context.stop();
        connector.stop();
    }
}
