package modern.ch11.core;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;

import java.util.Collections;
import java.util.Enumeration;

/**
 * 管理一个过滤器实例（原书 org.apache.catalina.core.ApplicationFilterConfig）。
 * 它自己就是 FilterConfig：过滤器的 init(FilterConfig) 拿到的就是它。
 */
public class ApplicationFilterConfig implements FilterConfig {

    private final StandardContext context;
    private final FilterDef filterDef;
    private Filter filter;

    public ApplicationFilterConfig(StandardContext context, FilterDef filterDef) {
        this.context = context;
        this.filterDef = filterDef;
    }

    /** 第一次调用时用 Web 应用的类加载器加载过滤器类、实例化并 init */
    public synchronized Filter getFilter() throws ServletException {
        if (filter == null) {
            try {
                ClassLoader loader = context.getLoader().getClassLoader();
                filter = (Filter) loader.loadClass(filterDef.filterClass()).getDeclaredConstructor().newInstance();
            } catch (ReflectiveOperationException e) {
                throw new ServletException("无法加载过滤器 " + filterDef.filterClass(), e);
            }
            filter.init(this);
        }
        return filter;
    }

    /** Context 停止时调用 */
    public synchronized void release() {
        if (filter != null) {
            filter.destroy();
            filter = null;
        }
    }

    @Override public String getFilterName() { return filterDef.filterName(); }
    @Override public String getInitParameter(String name) { return filterDef.initParameters().get(name); }
    @Override public Enumeration<String> getInitParameterNames() { return Collections.enumeration(filterDef.initParameters().keySet()); }
    @Override public ServletContext getServletContext() { return null; }
}
