package modern.ch06.core;

/**
 * 生命周期事件。原书是继承 EventObject、带三个 getter 的 final 类；
 * record 一行就表达了「不可变的数据载体」。
 */
public record LifecycleEvent(Lifecycle lifecycle, Type type, Object data) {

    public enum Type {
        BEFORE_START("before_start"),
        START("start"),
        AFTER_START("after_start"),
        BEFORE_STOP("before_stop"),
        STOP("stop"),
        AFTER_STOP("after_stop");

        private final String value;

        Type(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return value;           // 打印出来和原书的字符串常量一致
        }
    }
}
