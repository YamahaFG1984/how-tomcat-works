package modern.design.kit;

import modern.ch28.leak.LeakyPlugin;
import org.junit.jupiter.api.Test;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

/** 设计工具包：插件自己的类子优先，共享 API 父优先 */
class PluginClassLoaderTest {

    @Test
    void childFirstForPluginClassesParentFirstForSharedApi() throws Exception {
        Path classes = Path.of(LeakyPlugin.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        Path dir = Files.createTempDirectory("plugin-test");
        Path pkg = Files.createDirectories(dir.resolve("modern/ch28/leak"));
        Files.copy(classes.resolve("modern/ch28/leak/LeakyPlugin.class"), pkg.resolve("LeakyPlugin.class"));
        Path kit = Files.createDirectories(dir.resolve("modern/design/kit"));
        Files.copy(classes.resolve("modern/design/kit/Lifecycle.class"), kit.resolve("Lifecycle.class"));

        ClassLoader app = getClass().getClassLoader();
        try (var loader = new PluginClassLoader("t", new URL[] {dir.toUri().toURL()}, app, "modern.design.kit.")) {
            Class<?> plugin = loader.loadClass("modern.ch28.leak.LeakyPlugin");
            assertSame(loader, plugin.getClassLoader());                       // 类路径上也有，但插件优先
            assertNotSame(LeakyPlugin.class, plugin);
            assertSame(Consumer.class, loader.loadClass("java.util.function.Consumer"));
            assertSame(Lifecycle.class, loader.loadClass("modern.design.kit.Lifecycle"));   // 共享包：父优先
        }
    }
}
