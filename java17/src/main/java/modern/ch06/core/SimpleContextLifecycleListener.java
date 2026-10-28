package modern.ch06.core;

/** 打印 Context 发出的每个事件；start/stop 事件额外打印一句 */
public class SimpleContextLifecycleListener implements LifecycleListener {

    @Override
    public void lifecycleEvent(LifecycleEvent event) {
        System.out.println("SimpleContextLifecycleListener's event " + event.type());
        switch (event.type()) {
            case START -> System.out.println("Starting context.");
            case STOP -> System.out.println("Stopping context.");
            default -> { }
        }
    }
}
