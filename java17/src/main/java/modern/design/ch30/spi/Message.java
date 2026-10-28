package modern.design.ch30.spi;

import java.util.Map;

/** 扩展点之间传递的数据：不可变的 record，插件改不了别人看到的内容 */
public record Message(String channel, String to, String text, Map<String, String> headers) {

    public Message withText(String newText) {
        return new Message(channel, to, newText, headers);
    }
}
