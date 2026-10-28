package modern.ch16.shutdownhook;

/**
 * 最简单的关闭钩子（原书 Listing 16.1）。原书写了一个继承 Thread 的类；
 * Java 8 起直接把 lambda 交给 Thread 构造器即可。
 *
 * 运行：./run.sh ch16.shutdownhook.ShutdownHookDemo   然后按回车，或按 Ctrl+C
 */
public final class ShutdownHookDemo {

    public static void main(String[] args) throws Exception {
        System.out.println("Demo");
        Runtime.getRuntime().addShutdownHook(new Thread(() -> System.out.println("Shutting down"), "demo-hook"));
        System.in.read();
    }
}
