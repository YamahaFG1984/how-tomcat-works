package modern.ch11.core;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.Servlet;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;

import java.io.IOException;
import java.util.List;

/**
 * 过滤器链（原书 org.apache.catalina.core.ApplicationFilterChain）。
 * 每次 doFilter 调用下一个过滤器，并把自己作为第三个参数传进去；
 * 过滤器都走完之后，调用 Servlet 的 service。和第 5 章的 ValveContext 是同一个思路。
 */
public class ApplicationFilterChain implements FilterChain {

    private final List<Filter> filters;
    private final Servlet servlet;
    private int pos;

    public ApplicationFilterChain(List<Filter> filters, Servlet servlet) {
        this.filters = filters;
        this.servlet = servlet;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response) throws IOException, ServletException {
        if (pos < filters.size()) {
            filters.get(pos++).doFilter(request, response, this);
        } else {
            servlet.service(request, response);
        }
    }
}
