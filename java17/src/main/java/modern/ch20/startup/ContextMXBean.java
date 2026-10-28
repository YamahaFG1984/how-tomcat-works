package modern.ch20.startup;

/**
 * 迷你容器里 Context 的管理接口。名字以 MXBean 结尾：JMX 会把它当作 MXBean（Java 6），
 * 只使用基本类型和 String 这类「开放类型」，任何 JMX 客户端（包括不认识我们类的 JConsole）都能显示。
 */
public interface ContextMXBean {

    String getPath();

    String getDocBase();

    boolean isRunning();

    int getActiveSessions();

    void start() throws Exception;

    void stop() throws Exception;

    void reload();
}
