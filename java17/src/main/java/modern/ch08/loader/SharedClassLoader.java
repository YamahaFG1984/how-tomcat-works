package modern.ch08.loader;

/**
 * Web 应用类加载器的父加载器：只让 Web 应用看到 JDK 和 Servlet API（jakarta.*），
 * 看不到容器自己的类。真实 Tomcat 里，这个角色由 common 类加载器（$CATALINA_HOME/lib）承担；
 * 容器内部类（catalina.jar 等）虽然也在 common 里，但 WebappClassLoader 会过滤掉它们。
 */
public final class SharedClassLoader extends ClassLoader {

    static {
        registerAsParallelCapable();
    }

    private final ClassLoader application = SharedClassLoader.class.getClassLoader();

    public SharedClassLoader() {
        super("shared", ClassLoader.getPlatformClassLoader());   // 父加载器只认识 JDK 的类
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        if (name.startsWith("jakarta.")) {
            return application.loadClass(name);                   // Servlet API 放行
        }
        throw new ClassNotFoundException(name);                   // 其他一律看不见
    }
}
