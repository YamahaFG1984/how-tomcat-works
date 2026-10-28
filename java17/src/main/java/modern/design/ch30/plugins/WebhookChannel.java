package modern.design.ch30.plugins;

import modern.design.ch30.spi.Channel;
import modern.design.ch30.spi.Message;

/**
 * 「第三方插件」：运行时被复制到一个独立目录，由单独的类加载器加载（第 8 章的隔离思路）。
 * 主程序编译时不知道这个类的存在，只通过 Channel 接口和它打交道。
 */
public class WebhookChannel implements Channel {

    @Override public String name() { return "webhook"; }

    @Override
    public void send(Message m) {
        System.out.println("      [webhook] POST " + m.to() + " {\"text\":\"" + m.text() + "\"}  （由 "
                + getClass().getClassLoader() + " 加载）");
    }
}
