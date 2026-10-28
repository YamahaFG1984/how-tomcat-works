package modern.ch19.core;

import jakarta.servlet.Servlet;
import jakarta.servlet.ServletException;
import modern.ch12.core.StandardContext;
import modern.common.SimpleServletConfig;

/**
 * 能加载「容器提供的 Servlet」的 Wrapper（对应原书 StandardWrapper.loadServlet 中的特殊处理）：
 *  - isContainerProvidedServlet：类名属于容器自己的包，或者容器的类加载器能加载它且它实现了 ContainerServlet；
 *  - 这样的 Servlet 用容器自己的类加载器加载（Web 应用的类加载器根本看不到容器类），
 *    并在 init 之前调用 setWrapper(this)；
 *  - 只有 privileged 的 Context 才允许这样做（Tomcat 5 起的规则）。
 */
public class ContainerServletWrapper extends modern.ch06.core.SimpleWrapper {

    @Override
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
        String className = getServletClass();
        boolean privileged = getParent() instanceof StandardContext ctx && ctx.isPrivileged();
        ClassLoader loader = isContainerProvidedServlet(className) && privileged
                ? getClass().getClassLoader()                  // 容器的类加载器
                : getLoader().getClassLoader();                // Web 应用的类加载器
        try {
            var servlet = (Servlet) loader.loadClass(className).getDeclaredConstructor().newInstance();
            if (servlet instanceof ContainerServlet cs) {
                if (!privileged) {
                    throw new SecurityException("Servlet " + className + " 需要 privileged=\"true\" 的 Context");
                }
                cs.setWrapper(this);                           // ★ 把自己交给 Servlet
            }
            servlet.init(new SimpleServletConfig(name));
            return servlet;
        } catch (ReflectiveOperationException e) {
            throw new ServletException("无法加载 " + className, e);
        }
    }

    /** 对应原书 Listing 19.2 */
    private boolean isContainerProvidedServlet(String className) {
        if (className.startsWith("modern.")) {                 // 原书是 "org.apache.catalina."
            return true;
        }
        try {
            return ContainerServlet.class.isAssignableFrom(getClass().getClassLoader().loadClass(className));
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
