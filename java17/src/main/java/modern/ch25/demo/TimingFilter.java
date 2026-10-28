package modern.ch25.demo;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;

import java.io.IOException;

@WebFilter("/*")
public class TimingFilter implements Filter {

    @Override
    public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain)
            throws IOException, ServletException {
        long t0 = System.nanoTime();
        chain.doFilter(req, resp);
        System.out.printf("[TimingFilter] %s 用时 %d µs%n",
                ((HttpServletRequest) req).getRequestURI(), (System.nanoTime() - t0) / 1000);
    }
}
