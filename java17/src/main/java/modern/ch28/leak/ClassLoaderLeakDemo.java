package modern.ch28.leak;

import java.lang.ref.WeakReference;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * 复现 Web 应用重载时最常见的内存泄漏：ThreadLocal 把整个类加载器钉在内存里。
 *
 *   线程池里的线程（比应用活得久）
 *     └─ Thread.threadLocals ─→ Entry.value = Payload 对象（强引用）
 *          └─ Payload.class ─→ 插件的 ClassLoader ─→ 它加载的所有类、所有静态字段
 *
 * 每轮：新建类加载器加载插件 → 在「容器线程」上 start → （可选）stop → 丢弃类加载器 → GC → 看它是否被回收。
 * 运行：./run.sh ch28.leak.ClassLoaderLeakDemo
 */
public final class ClassLoaderLeakDemo {

    public static void main(String[] args) throws Exception {
        Path pluginDir = copyPluginClasses();
        ExecutorService containerThreads = Executors.newFixedThreadPool(1);   // 模拟容器的工作线程

        for (boolean cleanUp : new boolean[] {false, true}) {
            System.out.println(cleanUp ? "\n—— 停止时调用 stop（ThreadLocal.remove）——" : "—— 停止时忘了清理 ——");
            for (int round = 1; round <= 3; round++) {
                WeakReference<ClassLoader> ref = deployAndUndeploy(pluginDir, containerThreads, cleanUp);
                boolean collected = gcUntilCleared(ref);
                long usedMb = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) >> 20;
                System.out.printf("  第 %d 次重载：旧类加载器%s，堆已用 %d MB%n",
                        round, collected ? "已被回收" : "仍在内存中（泄漏）", usedMb);
            }
        }
        containerThreads.shutdown();
    }

    private static WeakReference<ClassLoader> deployAndUndeploy(Path dir, ExecutorService threads, boolean cleanUp)
            throws Exception {
        // 父加载器是 platform 类加载器：看不到应用类路径，所以插件类一定由这个新加载器加载（像 WebappClassLoader）
        var loader = new URLClassLoader(new URL[] {dir.toUri().toURL()}, ClassLoader.getPlatformClassLoader());
        @SuppressWarnings("unchecked")
        var plugin = (Consumer<String>) loader.loadClass("modern.ch28.leak.LeakyPlugin").getConstructor().newInstance();

        threads.submit(() -> plugin.accept("start")).get();                    // 请求在容器线程上执行
        if (cleanUp) threads.submit(() -> plugin.accept("stop")).get();        // 应用停止时清理
        loader.close();
        return new WeakReference<>(loader);                                    // 方法返回后只剩弱引用
    }

    private static boolean gcUntilCleared(WeakReference<?> ref) throws InterruptedException {
        for (int i = 0; i < 10 && ref.get() != null; i++) {
            System.gc();
            Thread.sleep(50);
        }
        return ref.get() == null;
    }

    /** 把插件的 class 文件复制到一个独立目录，模拟 WEB-INF/classes */
    private static Path copyPluginClasses() throws Exception {
        Path classes = Path.of(LeakyPlugin.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        Path dir = Files.createTempDirectory("ch28-plugin");
        Path pkg = Files.createDirectories(dir.resolve("modern/ch28/leak"));
        for (String name : new String[] {"LeakyPlugin.class", "LeakyPlugin$Payload.class"}) {
            Files.copy(classes.resolve("modern/ch28/leak").resolve(name), pkg.resolve(name));
        }
        return dir;
    }
}
