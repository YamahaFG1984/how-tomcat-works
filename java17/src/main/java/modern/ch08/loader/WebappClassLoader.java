package modern.ch08.loader;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

/**
 * Web 应用的类加载器（原书 org.apache.catalina.loader.WebappClassLoader）。
 *
 * 仓库（repository）：WEB-INF/classes/ 目录 + WEB-INF/lib/ 下的每个 JAR。
 * 加载规则：
 *   ① 查本地缓存 resourceEntries；② 查 JVM 缓存 findLoadedClass；③ 查「找不到」缓存；
 *   ④ delegate=true 或属于 java./jakarta. 等受保护的包 → 先问父加载器；
 *   ⑤ 在自己的仓库里找（子优先，child-first）；
 *   ⑥ 还没找到且 ④ 没问过父加载器 → 再问父加载器；
 *   ⑦ 都找不到 → 记入 notFoundResources，抛 ClassNotFoundException。
 */
public class WebappClassLoader extends URLClassLoader {

    static {
        registerAsParallelCapable();                // 不同类名可以并行加载（每个类名一把锁）
    }

    /** 这些包里的类总是先交给父加载器，Web 应用不能覆盖 JDK 和 Servlet API */
    private static final List<String> PACKAGE_TRIGGERS =
            List.of("java.", "javax.", "jakarta.", "org.xml.sax.", "org.w3c.dom.");

    /** 一个已加载类的缓存项：类对象 + 来源文件 + 加载时的修改时间（用来判断是否需要重载） */
    record ResourceEntry(Class<?> loadedClass, Path source, long lastModified) {
    }

    private final Map<String, ResourceEntry> resourceEntries = new ConcurrentHashMap<>();
    private final Set<String> notFoundResources = ConcurrentHashMap.newKeySet();
    private final List<String> repositories;
    private final boolean delegate;

    public WebappClassLoader(Path docBase, ClassLoader parent, boolean delegate) {
        super("webapp:" + docBase.getFileName(), toUrls(docBase), parent);   // Java 9：带名字的类加载器
        this.delegate = delegate;
        this.repositories = Stream.of(getURLs()).map(URL::toString).toList();
    }

    private static URL[] toUrls(Path docBase) {
        var urls = new ArrayList<URL>();
        try {
            Path classes = docBase.resolve("WEB-INF/classes");
            if (Files.isDirectory(classes)) {
                urls.add(classes.toUri().toURL());
            }
            Path lib = docBase.resolve("WEB-INF/lib");
            if (Files.isDirectory(lib)) {
                try (Stream<Path> jars = Files.list(lib)) {
                    for (Path jar : jars.filter(p -> p.toString().endsWith(".jar")).sorted().toList()) {
                        urls.add(jar.toUri().toURL());
                    }
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return urls.toArray(URL[]::new);
    }

    @Override
    public String toString() {
        return "WebappClassLoader[" + getName() + ", delegate=" + delegate + "]";
    }

    public List<String> findRepositories() {
        return repositories;
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        synchronized (getClassLoadingLock(name)) {
            ResourceEntry entry = resourceEntries.get(name);                  // ①
            if (entry != null) {
                return entry.loadedClass();
            }
            Class<?> clazz = findLoadedClass(name);                           // ②
            if (clazz != null) {
                return clazz;
            }
            if (notFoundResources.contains(name)) {                           // ③
                throw new ClassNotFoundException(name);
            }
            boolean delegateFirst = delegate || PACKAGE_TRIGGERS.stream().anyMatch(name::startsWith);
            if (delegateFirst) {                                              // ④
                try {
                    return getParent().loadClass(name);
                } catch (ClassNotFoundException ignored) {
                    // 继续在自己的仓库里找
                }
            }
            try {                                                             // ⑤
                clazz = findClass(name);
                resourceEntries.put(name, new ResourceEntry(clazz, sourceOf(name), lastModified(sourceOf(name))));
                return clazz;
            } catch (ClassNotFoundException ignored) {
                // 继续
            }
            if (!delegateFirst) {                                             // ⑥
                try {
                    return getParent().loadClass(name);
                } catch (ClassNotFoundException ignored) {
                    // 继续
                }
            }
            notFoundResources.add(name);                                      // ⑦
            throw new ClassNotFoundException(name);
        }
    }

    /** 自己仓库里加载过的类，只要有一个的 .class 文件被改过（或被删掉），就返回 true */
    public boolean modified() {
        for (ResourceEntry entry : resourceEntries.values()) {
            if (entry.source() != null && lastModified(entry.source()) != entry.lastModified()) {
                System.out.println("WebappClassLoader: " + entry.source().getFileName() + " 已被修改");
                return true;
            }
        }
        return false;
    }

    /** 类文件在磁盘上的位置；来自 JAR 的类返回 null（本例只检查目录里的 .class） */
    private Path sourceOf(String className) {
        URL url = findResource(className.replace('.', '/') + ".class");
        if (url == null || !"file".equals(url.getProtocol())) {
            return null;
        }
        try {
            return Path.of(url.toURI());
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }

    private static long lastModified(Path path) {
        try {
            return path == null ? -1 : Files.getLastModifiedTime(path).toMillis();
        } catch (IOException e) {
            return -1;                                     // 文件被删了也算「修改」
        }
    }
}
