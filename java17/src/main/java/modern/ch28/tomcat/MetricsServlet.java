package modern.ch28.tomcat;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import javax.management.JMException;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.management.ManagementFactory;

/**
 * 把第 20 章的 JMX 数据翻译成 Prometheus 的文本格式。Spring Boot Actuator + Micrometer 做的是同一件事：
 * Tomcat 的指标本来就在 MBean 里，监控系统只是换了一种取法。
 *   Catalina 域（嵌入式里叫 Tomcat）:type=ThreadPool          线程池：忙碌线程、当前线程、上限、连接数
 *                                  :type=GlobalRequestProcessor  请求数、错误数、累计处理时间、发送字节
 *                                  :type=Manager                 每个应用的活动会话数
 */
public class MetricsServlet extends HttpServlet {

    private final String domain;

    public MetricsServlet(String domain) {
        this.domain = domain;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("text/plain; version=0.0.4; charset=UTF-8");
        PrintWriter out = resp.getWriter();
        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        try {
            for (ObjectName on : mbs.queryNames(new ObjectName(domain + ":type=ThreadPool,*"), null)) {
                String label = "{connector=" + on.getKeyProperty("name") + "}";   // name 本身带引号
                gauge(out, "tomcat_threads_busy", label, mbs.getAttribute(on, "currentThreadsBusy"));
                gauge(out, "tomcat_threads_current", label, mbs.getAttribute(on, "currentThreadCount"));
                gauge(out, "tomcat_threads_max", label, mbs.getAttribute(on, "maxThreads"));
                gauge(out, "tomcat_connections", label, mbs.getAttribute(on, "connectionCount"));
            }
            for (ObjectName on : mbs.queryNames(new ObjectName(domain + ":type=GlobalRequestProcessor,*"), null)) {
                String label = "{connector=" + on.getKeyProperty("name") + "}";
                counter(out, "tomcat_requests_total", label, mbs.getAttribute(on, "requestCount"));
                counter(out, "tomcat_errors_total", label, mbs.getAttribute(on, "errorCount"));
                counter(out, "tomcat_processing_time_ms_total", label, mbs.getAttribute(on, "processingTime"));
                counter(out, "tomcat_sent_bytes_total", label, mbs.getAttribute(on, "bytesSent"));
                gauge(out, "tomcat_request_max_time_ms", label, mbs.getAttribute(on, "maxTime"));
            }
            for (ObjectName on : mbs.queryNames(new ObjectName(domain + ":type=Manager,*"), null)) {
                String label = "{context=\"" + on.getKeyProperty("context") + "\"}";
                gauge(out, "tomcat_sessions_active", label, mbs.getAttribute(on, "activeSessions"));
            }
        } catch (JMException e) {
            throw new IOException(e);
        }
        var heap = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();
        gauge(out, "jvm_heap_used_bytes", "", heap.getUsed());
        gauge(out, "jvm_heap_max_bytes", "", heap.getMax());
        gauge(out, "jvm_threads_live", "", ManagementFactory.getThreadMXBean().getThreadCount());
    }

    private static void gauge(PrintWriter out, String name, String labels, Object value) {
        out.printf("# TYPE %s gauge%n%s%s %s%n", name, name, labels, value);
    }

    private static void counter(PrintWriter out, String name, String labels, Object value) {
        out.printf("# TYPE %s counter%n%s%s %s%n", name, name, labels, value);
    }
}
