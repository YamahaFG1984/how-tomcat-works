package modern.design.ch30;

import modern.design.ch30.spi.Message;
import modern.design.ch30.spi.PublishEvent;
import modern.design.kit.PluginClassLoader;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;

/**
 * 四种扩展点一起工作：
 *   发现：类路径上的 ConsoleChannel、SmsChannel + 插件目录里的 WebhookChannel（独立类加载器）
 *   策略：formatter.class 配成 SignatureFormatter
 *   拦截：TraceValve 加追踪号；SensitiveWordValve 遇到敏感词短路
 *   监听：统计监听器 + 一个故意出错的监听器
 *
 * 运行：./run.sh design.ch30.ExtensionDemo
 */
public final class ExtensionDemo {

    public static void main(String[] args) throws Exception {
        var publisher = new Publisher();

        System.out.println("① 发现扩展");
        publisher.discoverChannels(ExtensionDemo.class.getClassLoader(), "应用类路径");
        // 插件自己的类子优先；Channel 等 API 包和设计工具包父优先，保证宿主和插件看到的是同一个接口
        try (var pluginLoader = new PluginClassLoader("webhook-plugin", new URL[] {buildPluginDir().toUri().toURL()},
                ExtensionDemo.class.getClassLoader(), "modern.design.ch30.spi.", "modern.design.kit.")) {
            publisher.discoverChannels(pluginLoader, "plugins/webhook");

            System.out.println("\n② 填充策略插槽（来自配置）");
            Map<String, String> config = Map.of("formatter.class", "modern.design.ch30.builtin.SignatureFormatter");
            publisher.setFormatterClass(config.get("formatter.class"));

            System.out.println("\n③ 装上拦截链和监听器");
            publisher.pipeline()
                    .addValve((m, next) -> next.invoke(new Message(m.channel(), m.to(), m.text(),
                            Map.of("trace", UUID.randomUUID().toString().substring(0, 8)))))          // TraceValve：改写
                    .addValve((m, next) -> {                                                         // SensitiveWordValve：短路
                        if (m.text().contains("内部密码")) {
                            System.out.println("      [SensitiveWordValve] 拦截：" + m.text());
                            publisher.reject(m, "包含敏感词");
                            return;
                        }
                        next.invoke(m);
                    });
            var stats = new java.util.EnumMap<PublishEvent.Type, Integer>(PublishEvent.Type.class);
            publisher.addPublishListener(e -> stats.merge(e.type(), 1, Integer::sum));
            publisher.addPublishListener(e -> {
                if (e.type() == PublishEvent.Type.FAILED) {
                    System.out.println("      [AlertListener] 发送失败：" + e.error().getMessage());
                }
            });
            publisher.addPublishListener(e -> {
                throw new IllegalStateException("有 bug 的监听器");
            });
            publisher.describe().forEach((k, v) -> System.out.printf("  %-16s = %s%n", k, v));

            System.out.println("\n④ 发布");
            publisher.start();
            send(publisher, "console", "alice", "构建成功");
            send(publisher, "sms", "13800000000", "验证码 123456");
            send(publisher, "sms", "13800000000", "这是一条很长的营销短信，".repeat(8));
            send(publisher, "sms", "0001234", "测试号码");
            send(publisher, "webhook", "https://hooks.example/abc", "部署完成");
            send(publisher, "console", "bob", "内部密码是 hunter2");
            send(publisher, "fax", "123", "传真？");
            publisher.stop();
            System.out.println("\n统计监听器：" + stats);
        }
    }

    private static void send(Publisher p, String channel, String to, String text) {
        System.out.println("  publish(" + channel + ", " + (text.length() > 20 ? text.substring(0, 20) + "…" : text) + ")");
        p.publish(new Message(channel, to, text, Map.of()));
    }

    /** 模拟一个插件 jar：类文件 + META-INF/services 声明，放在一个主程序类路径之外的目录里 */
    private static Path buildPluginDir() throws Exception {
        Path classes = Path.of(ExtensionDemo.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        Path dir = Files.createTempDirectory("ch30-plugin");
        Path pkg = Files.createDirectories(dir.resolve("modern/design/ch30/plugins"));
        Files.copy(classes.resolve("modern/design/ch30/plugins/WebhookChannel.class"), pkg.resolve("WebhookChannel.class"));
        Path services = Files.createDirectories(dir.resolve("META-INF/services"));
        Files.writeString(services.resolve("modern.design.ch30.spi.Channel"), "modern.design.ch30.plugins.WebhookChannel\n");
        return dir;
    }
}
