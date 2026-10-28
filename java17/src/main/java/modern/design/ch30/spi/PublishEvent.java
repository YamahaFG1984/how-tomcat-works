package modern.design.ch30.spi;

/** 【事件监听】发布过程中的事件。监听器只能「知道」，不能「改变」流程 */
public record PublishEvent(Type type, Message message, Throwable error) {

    public enum Type { PUBLISHED, REJECTED, FAILED }
}
