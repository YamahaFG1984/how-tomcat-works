package modern.ch06.tomcat;

import org.apache.catalina.Context;
import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleEvent;
import org.apache.catalina.startup.Tomcat;

import java.nio.file.Files;

/**
 * 观察 Tomcat 11 的生命周期：给 Context 注册一个监听器，启动再停止整个服务器，
 * 打印 Context 收到的每个事件以及它当时的状态（LifecycleState）。
 *
 * 运行：./run.sh ch06.tomcat.EmbeddedLifecycle
 */
public final class EmbeddedLifecycle {

    public static void main(String[] args) throws Exception {
        var tomcat = new Tomcat();
        tomcat.setBaseDir(Files.createTempDirectory("tomcat-ch06").toString());
        tomcat.setPort(0);                                   // 随机端口，本例不处理请求

        Context context = tomcat.addContext("", null);
        context.addLifecycleListener((LifecycleEvent event) -> {
            if (!Lifecycle.PERIODIC_EVENT.equals(event.getType())) {
                System.out.printf("  %-22s state=%s%n", event.getType(), event.getLifecycle().getState());
            }
        });

        System.out.println("== tomcat.start()  只启动最顶层的 Server，事件一路传到 Context");
        tomcat.start();
        System.out.println("== tomcat.stop()");
        tomcat.stop();
        System.out.println("== tomcat.destroy()");
        tomcat.destroy();
    }
}
