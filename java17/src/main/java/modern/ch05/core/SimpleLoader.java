package modern.ch05.core;

import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;

/** 从工作目录下的 webroot 加载类。record 自动生成构造器和 classLoader() 访问器。 */
public record SimpleLoader(ClassLoader classLoader) implements Loader {

    public static final Path WEB_ROOT = Path.of(System.getProperty("user.dir"), "webroot");

    public SimpleLoader() {
        this(newLoader());
    }

    private static ClassLoader newLoader() {
        try {
            return new URLClassLoader(new URL[] {WEB_ROOT.toUri().toURL()}, SimpleLoader.class.getClassLoader());
        } catch (MalformedURLException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public ClassLoader getClassLoader() {
        return classLoader;
    }
}
