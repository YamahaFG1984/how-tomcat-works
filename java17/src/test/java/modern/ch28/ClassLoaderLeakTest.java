package modern.ch28;

import modern.ch28.leak.LeakyPlugin;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.ref.WeakReference;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 第 28 章：ThreadLocal 留在长寿线程上会钉住类加载器；remove 之后可以回收 */
class ClassLoaderLeakTest {

    static Path pluginDir;
    static ExecutorService containerThread;

    @BeforeAll
    static void setUp() throws Exception {
        Path classes = Path.of(LeakyPlugin.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        pluginDir = Files.createTempDirectory("leak-test");
        Path pkg = Files.createDirectories(pluginDir.resolve("modern/ch28/leak"));
        for (String n : new String[] {"LeakyPlugin.class", "LeakyPlugin$Payload.class"}) {
            Files.copy(classes.resolve("modern/ch28/leak").resolve(n), pkg.resolve(n));
        }
        containerThread = Executors.newSingleThreadExecutor();
    }

    @AfterAll
    static void tearDown() {
        containerThread.shutdownNow();
    }

    @SuppressWarnings("unchecked")
    private static WeakReference<ClassLoader> deploy(boolean cleanUp) throws Exception {
        var loader = new URLClassLoader(new URL[] {pluginDir.toUri().toURL()}, ClassLoader.getPlatformClassLoader());
        var plugin = (Consumer<String>) loader.loadClass("modern.ch28.leak.LeakyPlugin").getConstructor().newInstance();
        containerThread.submit(() -> plugin.accept("start")).get();
        if (cleanUp) containerThread.submit(() -> plugin.accept("stop")).get();
        loader.close();
        return new WeakReference<>(loader);
    }

    private static boolean collected(WeakReference<?> ref) throws InterruptedException {
        for (int i = 0; i < 20 && ref.get() != null; i++) {
            System.gc();
            Thread.sleep(50);
        }
        return ref.get() == null;
    }

    @Test
    void cleanedUpLoaderIsCollected() throws Exception {
        assertTrue(collected(deploy(true)));
    }

    @Test
    void leakedThreadLocalPinsLoader() throws Exception {
        assertFalse(collected(deploy(false)));
    }
}
