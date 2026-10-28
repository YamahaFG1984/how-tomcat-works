package modern.design.ch30.builtin;

import modern.design.ch30.spi.Channel;
import modern.design.ch30.spi.Message;

/** 内置渠道：打印到控制台（也是没有任何插件时的兜底） */
public class ConsoleChannel implements Channel {

    @Override public String name() { return "console"; }

    @Override
    public void send(Message m) {
        System.out.println("      [console] → " + m.to() + "：" + m.text());
    }
}
