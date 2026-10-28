package modern.design.ch29;

import modern.design.kit.LifecycleBase;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * 边界层（相当于 Connector）：从一个队列读文本消息，解析成 Command，交给 Engine，把结果写回另一个队列。
 * 换成 HTTP、MQ、命令行，只需要再写一个 Endpoint；Engine 和所有 Handler 一行都不用改。
 */
public class QueueEndpoint extends LifecycleBase {

    private final BlockingQueue<String> inbox;
    private final BlockingQueue<String> outbox;
    private Engine engine;
    private volatile Thread worker;

    public QueueEndpoint(String name, BlockingQueue<String> inbox, BlockingQueue<String> outbox) {
        super(name);
        this.inbox = inbox;
        this.outbox = outbox;
    }

    /** 装配时注入：边界层依赖核心层的接口，而不是反过来 */
    public void setEngine(Engine engine) {
        this.engine = engine;
    }

    @Override
    protected void startInternal() {
        worker = new Thread(this::loop, getName() + "-worker");
        worker.start();
    }

    @Override
    protected void stopInternal() {
        worker.interrupt();
        try {
            worker.join(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void loop() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                String line = inbox.poll(100, TimeUnit.MILLISECONDS);
                if (line == null) continue;
                String reply;
                try {
                    reply = "OK  " + engine.invoke(Command.parse(line)).response();     // 协议 → 核心 → 协议
                } catch (Exception e) {
                    reply = "ERR " + e.getMessage();                                  // 错误也翻译成协议的样子
                }
                outbox.put(line + "  →  " + reply);
            } catch (InterruptedException e) {
                return;
            }
        }
    }
}
