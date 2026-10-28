package modern.ch17.startup;

import java.io.File;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.Arrays;

/**
 * 迷你版 org.apache.catalina.startup.Bootstrap：
 *  1. 建立类加载器：catalinaLoader 加载容器自身的类，它的父加载器只认识 JDK（平台类加载器），
 *     所以容器类不会出现在 Web 应用能看到的地方；
 *  2. 用 catalinaLoader 加载 Catalina 类，通过反射调用 setParentClassLoader(shared) 和 process(args)。
 *
 * 原书的 Tomcat 4 有 common / catalina(server) / shared 三个加载器；Tomcat 6 起默认只剩 common。
 * 这里的仓库就是当前的 java.class.path（target/classes 和依赖 JAR）；真实 Tomcat 的系统 CLASSPATH
 * 里只有 bootstrap.jar 和 tomcat-juli.jar，其余 JAR 都在 $CATALINA_HOME/lib，只有 common 加载器能看到。
 *
 * 运行：./run.sh ch17.startup.Bootstrap start        （或 bin/startup.sh）
 * 停止：./run.sh ch17.startup.Bootstrap stop         （或 bin/shutdown.sh）
 */
public final class Bootstrap {

    public static void main(String[] args) throws Throwable {
        if (System.getProperty("catalina.home") == null) {
            System.setProperty("catalina.home", System.getProperty("user.dir"));
        }
        if (System.getProperty("catalina.base") == null) {
            System.setProperty("catalina.base", System.getProperty("catalina.home"));
        }

        URL[] repositories = Arrays.stream(System.getProperty("java.class.path").split(File.pathSeparator))
                .map(Bootstrap::toUrl).toArray(URL[]::new);
        var catalinaLoader = new URLClassLoader("catalina", repositories, ClassLoader.getPlatformClassLoader());
        ClassLoader sharedLoader = catalinaLoader;      // Tomcat 6+ 的默认配置：shared = common = catalina

        Thread.currentThread().setContextClassLoader(catalinaLoader);
        Class<?> startupClass = catalinaLoader.loadClass("modern.ch17.startup.Catalina");
        Object catalina = startupClass.getDeclaredConstructor().newInstance();
        System.out.println("Bootstrap: Catalina 由 " + startupClass.getClassLoader().getName()
                + " 类加载器加载（不是加载 Bootstrap 的 " + Bootstrap.class.getClassLoader().getName() + "）");

        // 原书用 getMethod + invoke；Java 7 起可以用方法句柄（MethodHandle），调用开销更小
        var lookup = MethodHandles.publicLookup();
        lookup.findVirtual(startupClass, "setParentClassLoader", MethodType.methodType(void.class, ClassLoader.class))
              .invoke(catalina, sharedLoader);
        lookup.findVirtual(startupClass, "process", MethodType.methodType(void.class, String[].class))
              .invoke(catalina, args);
    }

    private static URL toUrl(String entry) {
        try {
            return Path.of(entry).toUri().toURL();
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException(entry, e);
        }
    }
}
