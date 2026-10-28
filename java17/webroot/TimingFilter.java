import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;

import java.io.IOException;

/** 第 11 章新增的过滤器：记录每个请求的耗时。init 参数 label 决定日志前缀 */
public class TimingFilter implements Filter {

    private String label;

    @Override
    public void init(FilterConfig config) {
        label = config.getInitParameter("label");
        System.out.println("TimingFilter.init(), filterName=" + config.getFilterName() + ", label=" + label);
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        long start = System.nanoTime();
        chain.doFilter(request, response);                       // 交给下一个过滤器，最后是 Servlet
        System.out.printf("[%s] %s 耗时 %.1f ms%n", label,
                ((HttpServletRequest) request).getRequestURI(), (System.nanoTime() - start) / 1e6);
    }
}
