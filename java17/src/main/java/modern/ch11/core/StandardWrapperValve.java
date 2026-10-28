package modern.ch11.core;

import jakarta.servlet.Filter;
import jakarta.servlet.Servlet;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import modern.ch03.connector.http.HttpRequest;
import modern.ch03.connector.http.HttpResponse;
import modern.ch05.core.Contained;
import modern.ch05.core.Container;
import modern.ch05.core.Valve;
import modern.ch05.core.ValveContext;
import modern.common.Facade;

import java.util.ArrayList;
import java.util.List;

/**
 * StandardWrapper 的基础阀：
 *   allocate → 创建过滤器链 → chain.doFilter（最终调用 service）→ deallocate
 */
public class StandardWrapperValve implements Valve, Contained {

    private StandardWrapper wrapper;

    @Override
    public void invoke(HttpRequest request, HttpResponse response, ValveContext valveContext) throws Exception {
        Servlet servlet = wrapper.allocate();
        try {
            var chain = createFilterChain(request, servlet);
            chain.doFilter(Facade.of(HttpServletRequest.class, request), Facade.of(HttpServletResponse.class, response));
        } finally {
            wrapper.deallocate(servlet);
        }
    }

    /** 按 Context 里 filter-mapping 的顺序，挑出匹配当前 URI 的过滤器 */
    private ApplicationFilterChain createFilterChain(HttpRequest request, Servlet servlet) throws ServletException {
        var context = (StandardContext) wrapper.getParent();
        List<Filter> filters = new ArrayList<>();
        for (FilterMap map : context.findFilterMaps()) {
            if (map.matches(request.getRequestURI())) {
                filters.add(context.findFilterConfig(map.filterName()).getFilter());
            }
        }
        return new ApplicationFilterChain(filters, servlet);
    }

    @Override public Container getContainer() { return wrapper; }
    @Override public void setContainer(Container container) { this.wrapper = (StandardWrapper) container; }
}
