package modern.common;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;

import java.util.Collections;
import java.util.Enumeration;

/**
 * 最小的 ServletConfig：只有名字，没有初始化参数，也没有 ServletContext。
 * 原书调用 servlet.init(null)；但 Servlet 6 的 HttpServlet.init(config) 会读取初始化参数，
 * 传 null 会抛 NullPointerException，所以至少要给一个空的 ServletConfig。
 */
public record SimpleServletConfig(String servletName) implements ServletConfig {

    @Override
    public String getServletName() {
        return servletName;
    }

    @Override
    public ServletContext getServletContext() {
        return null;
    }

    @Override
    public String getInitParameter(String name) {
        return null;
    }

    @Override
    public Enumeration<String> getInitParameterNames() {
        return Collections.emptyEnumeration();
    }
}
