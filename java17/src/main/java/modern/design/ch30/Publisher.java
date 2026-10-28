package modern.design.ch30;

import modern.design.ch30.builtin.PlainFormatter;
import modern.design.ch30.spi.Channel;
import modern.design.ch30.spi.Formatter;
import modern.design.ch30.spi.Message;
import modern.design.ch30.spi.PublishEvent;
import modern.design.kit.LifecycleBase;
import modern.design.kit.Lifecycle;
import modern.design.kit.Pipeline;

import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * 一个「消息发布器」，集中展示四种扩展点（对应 Tomcat 的同类机制）：
 *   拦截链   pipeline()            ← Valve / Filter：包住发送过程，可以改写、可以短路
 *   事件监听 addPublishListener()  ← LifecycleListener / ServletContextListener：只通知，出错不影响主流程
 *   策略插槽 setFormatter()        ← Realm / Manager / Loader：一个插槽一个实现，有默认值
 *   发现机制 discoverChannels()    ← className 属性 / ServiceLoader / SCI：实现类从哪里来
 */
public final class Publisher extends LifecycleBase {

    private final Map<String, Channel> channels = new ConcurrentHashMap<>();
    private final List<Consumer<PublishEvent>> listeners = new CopyOnWriteArrayList<>();
    private final Pipeline<Message> pipeline = new Pipeline<>(this::deliver);   // 基础阀：真正发送
    private volatile Formatter formatter = new PlainFormatter();                 // 策略插槽的默认值

    public Publisher() {
        super("publisher");
    }

    /* ---------- 扩展点 ---------- */

    public Pipeline<Message> pipeline() {
        return pipeline;
    }

    public void addPublishListener(Consumer<PublishEvent> listener) {
        listeners.add(listener);
    }

    public void setFormatter(Formatter formatter) {
        this.formatter = formatter == null ? new PlainFormatter() : formatter;
    }

    /** 按类名创建策略（server.xml 里 className 属性的做法）；类名为空就保留默认值 */
    public void setFormatterClass(String className) throws ReflectiveOperationException {
        if (className != null && !className.isBlank()) {
            setFormatter((Formatter) Class.forName(className.trim()).getConstructor().newInstance());
        }
    }

    /**
     * 用给定的类加载器发现渠道：应用类路径一次，每个插件各一次。
     * ServiceLoader 会顺着父加载器把宿主的服务也找出来，所以只接受「由这个加载器自己定义」的实现，
     * 否则每个插件都会把宿主的渠道重复发现一遍（Tomcat 的 WebappServiceLoader 也要处理同样的问题）。
     */
    public void discoverChannels(ClassLoader loader, String source) {
        ServiceLoader.load(Channel.class, loader).stream()
                .filter(p -> p.type().getClassLoader() == loader)
                .map(ServiceLoader.Provider::get)
                .forEach(c -> {
                    Channel old = channels.putIfAbsent(c.name(), c);
                    System.out.printf("  发现渠道 %-8s ← %s%s%n", c.name(), source, old == null ? "" : "（同名已存在，忽略）");
                });
    }

    /** 让外界能看到装了哪些扩展：可诊断性也是扩展点设计的一部分 */
    public Map<String, String> describe() {
        var out = new TreeMap<String, String>();
        channels.forEach((k, v) -> out.put("channel." + k, v.getClass().getName()));
        out.put("formatter", formatter.getClass().getName());
        out.put("listeners", String.valueOf(listeners.size()));
        return out;
    }

    /* ---------- 生命周期：策略如果自己也有生命周期，就跟着启停（Tomcat 对 Realm、Manager 也是这样） ---------- */

    @Override
    protected void startInternal() {
        if (formatter instanceof Lifecycle l) l.start();
    }

    @Override
    protected void stopInternal() {
        if (formatter instanceof Lifecycle l) l.stop();
    }

    /* ---------- 主流程 ---------- */

    public void publish(Message message) {
        if (!getState().isAvailable()) throw new IllegalStateException("发布器没有启动");
        try {
            pipeline.invoke(message);
        } catch (Exception e) {
            fire(new PublishEvent(PublishEvent.Type.FAILED, message, e));
        }
    }

    /** 拦截链短路时调用：让监听器知道消息被拒绝了 */
    public void reject(Message message, String reason) {
        fire(new PublishEvent(PublishEvent.Type.REJECTED, message, new IllegalArgumentException(reason)));
    }

    private void deliver(Message m) throws Exception {
        Channel c = channels.get(m.channel());
        if (c == null) throw new IllegalArgumentException("没有渠道 " + m.channel());
        Message formatted = m.withText(formatter.format(m.text()));
        if (!c.supports(formatted)) throw new IllegalArgumentException(c.name() + " 不支持这条消息");
        c.send(formatted);
        fire(new PublishEvent(PublishEvent.Type.PUBLISHED, formatted, null));
    }

    /** 监听器之间互相隔离：一个抛异常，不影响其他监听器，也不影响发布结果 */
    private void fire(PublishEvent event) {
        for (var l : listeners) {
            try {
                l.accept(event);
            } catch (RuntimeException e) {
                System.out.println("      （监听器出错已忽略：" + e.getMessage() + "）");
            }
        }
    }
}
