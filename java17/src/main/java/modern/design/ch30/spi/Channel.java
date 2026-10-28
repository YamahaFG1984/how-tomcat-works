package modern.design.ch30.spi;

/**
 * 【发现机制 + 策略】发送渠道。实现类通过 ServiceLoader 发现：
 * 在 jar 的 META-INF/services/modern.design.ch30.spi.Channel 里写上类名即可（第 25 章 SCI 的同一套约定）。
 */
public interface Channel {

    /** 渠道名，配置和消息里用它来引用 */
    String name();

    void send(Message message) throws Exception;

    /**
     * 后来才加的方法：用 default 实现保持向后兼容，老插件不用重新编译（Servlet API 3.1 起大量这样做）。
     */
    default boolean supports(Message message) {
        return true;
    }
}
