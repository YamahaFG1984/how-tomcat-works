package modern.ch28.leak;

import java.util.function.Consumer;

/**
 * 被单独的类加载器加载的「插件」（相当于一个 Web 应用里的类）。
 * accept("start") 往<strong>当前线程</strong>的 ThreadLocal 里放一个本插件的对象；accept("stop") 把它移除。
 * 当前线程如果是容器线程池里的线程，它会比插件活得久得多——忘了 stop 就泄漏。
 */
public class LeakyPlugin implements Consumer<String> {

    /** 典型写法：静态 ThreadLocal 缓存一个「昂贵」的对象 */
    private static final ThreadLocal<Payload> CACHE = new ThreadLocal<>();

    @Override
    public void accept(String command) {
        switch (command) {
            case "start" -> CACHE.set(new Payload());
            case "stop" -> CACHE.remove();
            default -> throw new IllegalArgumentException(command);
        }
    }

    /** 值的类型来自插件的类加载器：这就是引用链上的关键一环 */
    static final class Payload {
        final byte[] data = new byte[10 * 1024 * 1024];        // 10 MB，泄漏时肉眼可见
    }
}
