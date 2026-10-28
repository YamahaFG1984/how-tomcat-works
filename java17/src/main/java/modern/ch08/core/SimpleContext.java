package modern.ch08.core;

import modern.ch06.core.LifecycleException;

import java.nio.file.Path;

/** 第 6 章的 Context 加上 docBase 和 reload()：重载 = 停止全部组件再重新启动 */
public class SimpleContext extends modern.ch06.core.SimpleContext {

    private Path docBase;

    public Path getDocBase() {
        return docBase;
    }

    /** 相对路径相对于 catalina.base（默认当前目录） */
    public void setDocBase(String docBase) {
        this.docBase = Path.of(System.getProperty("catalina.base", System.getProperty("user.dir")))
                .resolve(docBase).normalize();
    }

    public synchronized void reload() {
        System.out.println("===== 检测到类被修改，重新加载 Context =====");
        try {
            stop();        // Wrapper 调用 Servlet.destroy()，载入器关闭旧的类加载器
            start();       // 载入器创建新的类加载器，Servlet 会在下次请求时用新类重新加载
        } catch (LifecycleException e) {
            e.printStackTrace();
        }
    }
}
