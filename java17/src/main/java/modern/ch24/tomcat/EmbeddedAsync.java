package modern.ch24.tomcat;

import jakarta.servlet.AsyncContext;
import jakarta.servlet.AsyncEvent;
import jakarta.servlet.AsyncListener;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.catalina.Context;
import org.apache.catalina.Wrapper;
import org.apache.catalina.startup.Tomcat;

import java.io.IOException;
import java.nio.file.Files;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Tomcat 11 的异步 Servlet（maxThreads 故意设为 4）：
 *   /sync       Thread.sleep(2000)：占着容器线程等
 *   /async      startAsync()，2 秒后由定时器线程写响应并 complete()
 *   /timeout    startAsync() 后谁也不 complete，1 秒后 AsyncListener.onTimeout 返回 503
 *   /dispatch   异步算完之后 dispatch() 回容器，由另一个 Servlet 渲染（DispatcherType.ASYNC）
 *   /events     Server-Sent Events：每 500ms 推一条，共 5 条
 *   /upload     Servlet 3.1 非阻塞读：ReadListener 只在有数据时被回调
 *
 * 运行：./run.sh ch24.tomcat.EmbeddedAsync
 */
public final class EmbeddedAsync {

    private static final ScheduledExecutorService TIMER = Executors.newScheduledThreadPool(2, r -> {
        var t = new Thread(r, "biz-timer");
        t.setDaemon(true);
        return t;
    });

    public static void main(String[] args) throws Exception {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 8080;
        var tomcat = new Tomcat();
        tomcat.setBaseDir(Files.createTempDirectory("tomcat-ch24").toString());
        tomcat.setPort(port);
        tomcat.getConnector().setProperty("address", "127.0.0.1");
        tomcat.getConnector().setProperty("maxThreads", "4");
        tomcat.getConnector().setProperty("minSpareThreads", "4");

        Context ctx = tomcat.addContext("", Files.createTempDirectory("ch24-docbase").toString());
        add(ctx, "sync", new SyncServlet(), "/sync");
        add(ctx, "async", new AsyncServlet(), "/async");
        add(ctx, "timeout", new TimeoutServlet(), "/timeout");
        add(ctx, "dispatch", new DispatchServlet(), "/dispatch");
        add(ctx, "result", new ResultServlet(), "/WEB-INF/result");
        add(ctx, "events", new EventsServlet(), "/events");
        add(ctx, "upload", new UploadServlet(), "/upload");

        tomcat.start();
        System.out.println("Tomcat 11 已启动（maxThreads=4）：curl http://localhost:" + port + "/async");
        tomcat.getServer().await();
    }

    private static void add(Context ctx, String name, HttpServlet servlet, String pattern) {
        Wrapper w = Tomcat.addServlet(ctx, name, servlet);
        w.setAsyncSupported(true);              // 链路上的每个 Servlet 和 Filter 都必须支持异步
        ctx.addServletMappingDecoded(pattern, name);
    }

    static final class SyncServlet extends HttpServlet {
        @Override protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String entered = Thread.currentThread().getName();
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            resp.getWriter().println("sync: " + entered);
        }
    }

    static final class AsyncServlet extends HttpServlet {
        @Override protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
            String entered = Thread.currentThread().getName();
            resp.setContentType("text/plain;charset=UTF-8");
            AsyncContext ac = req.startAsync();                // 从这里起，doGet 返回也不会结束响应
            ac.setTimeout(10_000);
            TIMER.schedule(() -> {
                try {
                    ac.getResponse().getWriter().println("async: 进入 " + entered + "，完成 " + Thread.currentThread().getName());
                } catch (IOException e) {
                    throw new java.io.UncheckedIOException(e);
                } finally {
                    ac.complete();                             // 告诉容器：可以结束响应了
                }
            }, 2, TimeUnit.SECONDS);
        }                                                      // 容器线程在这里就归还了
    }

    static final class TimeoutServlet extends HttpServlet {
        @Override protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
            resp.setContentType("text/plain;charset=UTF-8");
            AsyncContext ac = req.startAsync();
            ac.setTimeout(1000);
            ac.addListener(new AsyncListener() {
                @Override public void onTimeout(AsyncEvent e) throws IOException {
                    var r = (HttpServletResponse) e.getAsyncContext().getResponse();
                    r.setStatus(503);
                    r.getWriter().println("业务 1 秒内没有完成，返回 503（线程 " + Thread.currentThread().getName() + "）");
                    e.getAsyncContext().complete();
                }
                @Override public void onComplete(AsyncEvent e) {
                    System.out.println("[timeout] onComplete");
                }
                @Override public void onError(AsyncEvent e) { }
                @Override public void onStartAsync(AsyncEvent e) { }
            });
            // 故意不 complete()
        }
    }

    static final class DispatchServlet extends HttpServlet {
        @Override protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
            AsyncContext ac = req.startAsync();
            TIMER.schedule(() -> {
                req.setAttribute("result", "第 " + COUNTER.incrementAndGet() + " 次计算的结果");
                ac.dispatch("/WEB-INF/result");                // 回到容器线程里，用另一个 Servlet 渲染
            }, 500, TimeUnit.MILLISECONDS);
        }
        private static final AtomicInteger COUNTER = new AtomicInteger();
    }

    static final class ResultServlet extends HttpServlet {
        @Override protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            resp.setContentType("text/plain;charset=UTF-8");
            resp.getWriter().printf("%s%ndispatcherType = %s，渲染线程 = %s%n",
                    req.getAttribute("result"), req.getDispatcherType(), Thread.currentThread().getName());
        }
    }

    static final class EventsServlet extends HttpServlet {
        @Override protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
            resp.setContentType("text/event-stream;charset=UTF-8");
            resp.setHeader("Cache-Control", "no-cache");
            AsyncContext ac = req.startAsync();
            ac.setTimeout(0);                                  // 0 = 永不超时，由我们自己 complete
            var n = new AtomicInteger();
            var task = new java.util.concurrent.atomic.AtomicReference<java.util.concurrent.ScheduledFuture<?>>();
            task.set(TIMER.scheduleAtFixedRate(() -> {
                try {
                    var out = ac.getResponse().getWriter();
                    out.printf("data: tick %d %tT%n%n", n.incrementAndGet(), System.currentTimeMillis());
                    out.flush();                               // 立刻推给客户端
                    if (out.checkError() || n.get() == 5) {    // 客户端断开或者推完了
                        task.get().cancel(false);
                        ac.complete();
                    }
                } catch (IOException e) {
                    task.get().cancel(false);
                }
            }, 0, 500, TimeUnit.MILLISECONDS));
        }
    }

    static final class UploadServlet extends HttpServlet {
        @Override protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            resp.setContentType("text/plain;charset=UTF-8");
            AsyncContext ac = req.startAsync();
            ServletInputStream in = req.getInputStream();
            var total = new AtomicLong();
            var callbacks = new AtomicInteger();
            in.setReadListener(new ReadListener() {
                final byte[] buf = new byte[8192];
                @Override public void onDataAvailable() throws IOException {
                    callbacks.incrementAndGet();
                    while (in.isReady()) {                     // 只读「现在就能读到」的部分，读完就返回
                        int n = in.read(buf);
                        if (n < 0) break;
                        total.addAndGet(n);
                    }
                }
                @Override public void onAllDataRead() throws IOException {
                    ac.getResponse().getWriter().printf("收到 %d 字节，onDataAvailable 被回调 %d 次%n", total.get(), callbacks.get());
                    ac.complete();
                }
                @Override public void onError(Throwable t) {
                    ac.complete();
                }
            });
        }                                                      // 立即返回：等数据的时候不占线程
    }
}
