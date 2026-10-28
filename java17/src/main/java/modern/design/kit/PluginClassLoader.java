package modern.design.kit;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;

/**
 * 插件类加载器（第 8 章 WebappClassLoader 的精简版）：
 *   - 共享的 API 包（以及 java.*）永远交给父加载器——宿主和插件必须看到<strong>同一个</strong> Channel 接口，否则类型转换会失败
 *   - 其余的类先在插件自己的目录里找，找不到再问父加载器（子优先）——插件可以自带和宿主不同版本的库
 */
public class PluginClassLoader extends URLClassLoader {

    static {
        registerAsParallelCapable();
    }

    private final List<String> sharedPrefixes;

    public PluginClassLoader(String name, URL[] urls, ClassLoader parent, String... sharedPackages) {
        super(name, urls, parent);
        this.sharedPrefixes = List.of(sharedPackages);
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        synchronized (getClassLoadingLock(name)) {
            Class<?> c = findLoadedClass(name);
            if (c == null) {
                if (isShared(name)) {
                    c = getParent().loadClass(name);              // 父优先
                } else {
                    try {
                        c = findClass(name);                      // 子优先
                    } catch (ClassNotFoundException e) {
                        c = getParent().loadClass(name);
                    }
                }
            }
            if (resolve) resolveClass(c);
            return c;
        }
    }

    private boolean isShared(String className) {
        return className.startsWith("java.") || className.startsWith("javax.")
                || sharedPrefixes.stream().anyMatch(className::startsWith);
    }

    @Override
    public String toString() {
        return "PluginClassLoader(" + getName() + ")";
    }
}
