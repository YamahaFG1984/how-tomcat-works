package modern.ch03.connector.http;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 每个包一个实例的错误消息管理器（原书 org.apache.catalina.util.StringManager）。
 *
 * 原书用 synchronized 静态方法 + Hashtable 实现「每个包一个单例」；
 * 这里用 ConcurrentHashMap.computeIfAbsent，既线程安全又不用整体加锁。
 * 消息从同包下的 LocalStrings[_语言].properties 读取，Java 9+ 默认按 UTF-8 解析 properties。
 */
public final class StringManager {

    private static final Map<String, StringManager> MANAGERS = new ConcurrentHashMap<>();

    private final ResourceBundle bundle;

    private StringManager(String packageName) {
        this.bundle = ResourceBundle.getBundle(packageName + ".LocalStrings", Locale.getDefault());
    }

    public static StringManager getManager(String packageName) {
        return MANAGERS.computeIfAbsent(packageName, StringManager::new);
    }

    public String getString(String key, Object... args) {
        try {
            String pattern = bundle.getString(key);
            return args.length == 0 ? pattern : MessageFormat.format(pattern, args);
        } catch (MissingResourceException e) {
            return "Cannot find message associated with key '" + key + "'";
        }
    }
}
