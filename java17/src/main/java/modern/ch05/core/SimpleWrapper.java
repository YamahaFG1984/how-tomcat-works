package modern.ch05.core;

import jakarta.servlet.Servlet;
import jakarta.servlet.ServletException;
import modern.common.SimpleServletConfig;

/** Wrapper：代表一个 Servlet，负责加载、初始化并分配它的实例 */
public class SimpleWrapper extends ContainerBase {

    private String servletClass;
    protected volatile Servlet instance;

    public SimpleWrapper() {
        pipeline.setBasic(new SimpleWrapperValve());
    }

    public void setServletClass(String servletClass) {
        this.servletClass = servletClass;
    }

    public String getServletClass() {
        return servletClass;
    }

    /** 返回已初始化的 Servlet 实例；第一次调用时才加载（双重检查锁定，volatile 保证可见性） */
    public Servlet allocate() throws ServletException {
        Servlet servlet = instance;
        if (servlet == null) {
            synchronized (this) {
                servlet = instance;
                if (servlet == null) {
                    instance = servlet = loadServlet();
                }
            }
        }
        return servlet;
    }

    private Servlet loadServlet() throws ServletException {
        if (servletClass == null) {
            throw new ServletException("servlet class has not been specified");
        }
        Loader loader = getLoader();
        if (loader == null) {
            throw new ServletException("No loader.");
        }
        try {
            Class<?> clazz = loader.getClassLoader().loadClass(servletClass);
            Servlet servlet = (Servlet) clazz.getDeclaredConstructor().newInstance();
            servlet.init(new SimpleServletConfig(name));
            return servlet;
        } catch (ReflectiveOperationException e) {
            throw new ServletException("Cannot load servlet " + servletClass, e);
        }
    }

    /** Wrapper 是最底层的容器，不能有子容器 */
    @Override
    public void addChild(Container child) {
        throw new IllegalArgumentException("Wrapper 不能有子容器");
    }
}
