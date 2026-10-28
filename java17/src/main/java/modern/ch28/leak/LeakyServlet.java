package modern.ch28.leak;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * 犯了两个经典错误的 Servlet，交给真实的 Tomcat 去检测（见 EmbeddedLeakDetection）：
 *   ① 在容器线程上设置 ThreadLocal，从不 remove
 *   ② 启动一个后台线程，应用停止时不去停它
 */
public class LeakyServlet extends HttpServlet {

    /** 值的类型是应用自己的类（由 WebappClassLoader 加载）——如果是 StringBuilder 这种 JDK 类，就钉不住类加载器 */
    private static final ThreadLocal<Buffer> BUFFER = ThreadLocal.withInitial(Buffer::new);

    static final class Buffer {
        final StringBuilder sb = new StringBuilder();
    }

    @Override
    public void init() {
        Thread t = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(60_000);
                } catch (InterruptedException e) {
                    return;
                }
            }
        }, "leaky-background");
        t.setDaemon(true);
        t.start();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        StringBuilder sb = BUFFER.get().sb;                     // 第一次调用时在当前容器线程上创建
        sb.setLength(0);
        sb.append("由 ").append(getClass().getClassLoader().getClass().getSimpleName()).append(" 加载");
        resp.setContentType("text/plain;charset=UTF-8");
        resp.getWriter().println(sb);
    }
}
