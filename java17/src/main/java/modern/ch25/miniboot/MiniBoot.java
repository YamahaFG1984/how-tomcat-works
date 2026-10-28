package modern.ch25.miniboot;

import org.apache.catalina.Context;
import org.apache.catalina.startup.Tomcat;

import java.nio.file.Files;
import java.util.List;

/**
 * 一个只有一个方法的「Spring Boot」：MiniBoot.run(主类) 做的事和 SpringApplication.run 的 Web 部分一样——
 *   ① 从主类所在的包开始扫描类路径
 *   ② 创建嵌入式 Tomcat（相当于 TomcatServletWebServerFactory.getWebServer）
 *   ③ 不用 Tomcat 自己的注解扫描（addContext 不带 ContextConfig），
 *      而是注册一个自己的 ServletContainerInitializer，在里面编程式注册扫描到的组件
 *   ④ 启动，并让一个非守护线程保持进程存活
 */
public final class MiniBoot {

    private MiniBoot() { }

    public static Tomcat run(Class<?> primary, String... args) throws Exception {
        long t0 = System.nanoTime();
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 8080;

        List<Class<?>> classes = ClassScanner.scan(primary.getClassLoader(), primary.getPackageName());
        System.out.printf("[MiniBoot] 扫描包 %s：%d 个类%n", primary.getPackageName(), classes.size());

        var tomcat = new Tomcat();
        tomcat.setBaseDir(Files.createTempDirectory("miniboot").toString());
        tomcat.setPort(port);
        tomcat.getConnector().setProperty("address", "127.0.0.1");
        Context ctx = tomcat.addContext("", Files.createTempDirectory("miniboot-docbase").toString());
        ctx.addServletContainerInitializer(new MiniBootStarter(classes), null);

        tomcat.start();
        System.out.printf("[MiniBoot] 已在 %d 端口启动，用时 %d ms%n", port, (System.nanoTime() - t0) / 1_000_000);

        // Spring Boot 的 TomcatWebServer 也会起一个非守护线程等待，否则 main 返回后 JVM 就退出了
        Thread await = new Thread(() -> tomcat.getServer().await(), "container-0");
        await.setDaemon(false);
        await.start();
        return tomcat;
    }
}
