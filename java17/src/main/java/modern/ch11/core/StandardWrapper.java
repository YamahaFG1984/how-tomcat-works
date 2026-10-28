package modern.ch11.core;

import jakarta.servlet.Servlet;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import modern.ch05.core.Container;
import modern.ch05.core.Loader;
import modern.common.Facade;

import java.util.Collections;
import java.util.Enumeration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 标准 Wrapper（原书 org.apache.catalina.core.StandardWrapper）：
 *  - 自己实现 ServletConfig（名字、初始化参数），但交给 Servlet 的是外观，Servlet 拿不到 Wrapper 的其他方法；
 *  - allocate() 懒加载 Servlet 并调用 init(外观)；deallocate() 归还；
 *  - 父容器只能是 Context，不能有子容器；
 *  - 基础阀是 StandardWrapperValve，由它构造过滤器链并调用 Servlet。
 */
public class StandardWrapper extends modern.ch06.core.SimpleWrapper implements ServletConfig {

    private final Map<String, String> parameters = new ConcurrentHashMap<>();
    private final AtomicInteger countAllocated = new AtomicInteger();
    private final ServletConfig facade = Facade.of(ServletConfig.class, this);   // 对应 StandardWrapperFacade

    public StandardWrapper() {
        getPipeline().setBasic(new StandardWrapperValve());
    }

    public void addInitParameter(String name, String value) {
        parameters.put(name, value);
    }

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
        countAllocated.incrementAndGet();
        return servlet;
    }

    public void deallocate(Servlet servlet) {
        countAllocated.decrementAndGet();
    }

    public int getCountAllocated() {
        return countAllocated.get();
    }

    private Servlet loadServlet() throws ServletException {
        if (getServletClass() == null) {
            throw new ServletException("No servlet class has been specified for servlet " + name);
        }
        Loader loader = getLoader();
        if (loader == null) {
            throw new ServletException("No loader for servlet " + name);
        }
        Servlet servlet;
        try {
            Class<?> clazz = loader.getClassLoader().loadClass(getServletClass());
            servlet = (Servlet) clazz.getDeclaredConstructor().newInstance();
        } catch (ClassCastException e) {
            throw new ServletException("Class " + getServletClass() + " is not a Servlet", e);
        } catch (ReflectiveOperationException e) {
            throw new ServletException("Error instantiating servlet class " + getServletClass(), e);
        }
        servlet.init(facade);               // 传外观，而不是 this
        return servlet;
    }

    /** Wrapper 的父容器必须是 Context */
    @Override
    public void setParent(Container container) {
        if (container != null && !(container instanceof StandardContext)) {
            throw new IllegalArgumentException("Wrapper 的父容器必须是 Context");
        }
        super.setParent(container);
    }

    /* ---------- ServletConfig ---------- */

    @Override public String getServletName() { return getName(); }
    @Override public String getInitParameter(String name) { return parameters.get(name); }
    @Override public Enumeration<String> getInitParameterNames() { return Collections.enumeration(parameters.keySet()); }
    @Override public ServletContext getServletContext() { return null; }   // 第 12 章的 StandardContext 才有 ServletContext
}
