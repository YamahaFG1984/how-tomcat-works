package modern.ch11.tomcat;

import org.apache.catalina.Wrapper;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.startup.Tomcat;
import org.apache.tomcat.util.descriptor.web.FilterDef;
import org.apache.tomcat.util.descriptor.web.FilterMap;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 直接用 Tomcat 11 的 StandardContext / StandardWrapper / FilterDef / FilterMap 搭出和 Bootstrap 相同的结构。
 * 运行：./run.sh ch11.tomcat.EmbeddedWrapper    访问 http://localhost:8080/myApp/Config
 */
public final class EmbeddedWrapper {

    public static void main(String[] args) throws Exception {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 8080;
        var tomcat = new Tomcat();
        tomcat.setBaseDir(Files.createTempDirectory("tomcat-ch11").toString());
        tomcat.setPort(port);
        tomcat.getConnector().setProperty("address", "127.0.0.1");

        var context = new StandardContext();
        context.setPath("/myApp");
        context.setDocBase(Path.of("myApp").toAbsolutePath().toString());
        context.addLifecycleListener(new Tomcat.FixContextListener());   // 相当于原书的 SimpleContextConfig
        tomcat.getHost().addChild(context);

        for (String name : new String[] {"Primitive", "Modern", "Config"}) {
            Wrapper wrapper = context.createWrapper();                   // 返回 StandardWrapper
            wrapper.setName(name);
            wrapper.setServletClass(name + "Servlet");
            context.addChild(wrapper);
            context.addServletMappingDecoded("/" + name, name);
        }
        ((Wrapper) context.findChild("Config")).addInitParameter("greeting", "你好，Tomcat 11");

        var timing = new FilterDef();
        timing.setFilterName("timing");
        timing.setFilterClass("TimingFilter");
        timing.addInitParameter("label", "Tomcat11 计时");
        context.addFilterDef(timing);
        var timingMap = new FilterMap();
        timingMap.setFilterName("timing");
        timingMap.addURLPattern("/*");
        context.addFilterMap(timingMap);

        tomcat.start();
        System.out.println("Tomcat 11 已启动：http://localhost:" + port + "/myApp/Config");
        tomcat.getServer().await();
    }
}
