package modern.ch25.miniboot;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.JarURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

/**
 * 在类路径上找出某个包（含子包）下的所有类，目录和 jar 都支持。
 * Spring 的 ClassPathScanningCandidateComponentProvider、Tomcat 的 ContextConfig 做的是同一件事，
 * 只是它们直接读字节码判断注解，不加载类；这里为了简单直接 Class.forName(name, false, loader)。
 */
public final class ClassScanner {

    private ClassScanner() { }

    public static List<Class<?>> scan(ClassLoader loader, String packageName) throws IOException {
        String path = packageName.replace('.', '/');
        List<String> names = new ArrayList<>();
        for (URL url : Collections.list(loader.getResources(path))) {
            switch (url.getProtocol()) {
                case "file" -> names.addAll(fromDirectory(url, packageName));
                case "jar" -> names.addAll(fromJar(url, path));
                default -> { }                                     // 其他协议（如 jrt）不处理
            }
        }
        List<Class<?>> classes = new ArrayList<>();
        for (String name : names) {
            try {
                classes.add(Class.forName(name, false, loader));   // false：只加载，不初始化
            } catch (ClassNotFoundException | LinkageError e) {
                System.err.println("跳过无法加载的类 " + name + "：" + e);
            }
        }
        return classes;
    }

    private static List<String> fromDirectory(URL url, String packageName) {
        try {
            Path root = Path.of(url.toURI());
            try (Stream<Path> files = Files.walk(root)) {
                return files.filter(p -> p.toString().endsWith(".class"))
                        .map(p -> packageName + "." + root.relativize(p).toString()
                                .replace(java.io.File.separatorChar, '.').replaceAll("\\.class$", ""))
                        .toList();
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private static List<String> fromJar(URL url, String path) throws IOException {
        var conn = (JarURLConnection) url.openConnection();
        try (var jar = conn.getJarFile()) {
            return jar.stream()
                    .map(e -> e.getName())
                    .filter(n -> n.startsWith(path + "/") && n.endsWith(".class"))
                    .map(n -> n.replace('/', '.').replaceAll("\\.class$", ""))
                    .toList();
        }
    }
}
