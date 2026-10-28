package modern.design.ch30.builtin;

import modern.design.ch30.spi.Formatter;

/** 默认策略：原样输出 */
public class PlainFormatter implements Formatter {

    @Override
    public String format(String text) {
        return text;
    }
}
