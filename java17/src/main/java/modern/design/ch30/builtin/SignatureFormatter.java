package modern.design.ch30.builtin;

import modern.design.ch30.spi.Formatter;

/** 另一个策略：在正文后加签名。通过配置 formatter.class 选择它 */
public class SignatureFormatter implements Formatter {

    @Override
    public String format(String text) {
        return text + " ——Tomcat 教程";
    }
}
