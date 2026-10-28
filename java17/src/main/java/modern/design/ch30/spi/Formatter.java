package modern.design.ch30.spi;

/**
 * 【策略插槽】正文格式化。一个发布器只有一个 Formatter，由配置里的类名决定，没配置时用默认实现
 * （相当于 Context 的 Realm、Manager、Loader：插槽只有一个，但谁来填可以换）。
 */
@FunctionalInterface
public interface Formatter {

    String format(String text);
}
