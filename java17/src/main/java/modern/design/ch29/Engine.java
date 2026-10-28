package modern.design.ch29;

import modern.design.kit.CompositeComponent;
import modern.design.kit.Pipeline;

/**
 * 核心层的入口（相当于 Engine）：按模块名路由。它只认识 Command，不认识任何传输协议。
 */
public class Engine extends CompositeComponent<Module> {

    private final Pipeline<Exchange> pipeline = new Pipeline<>(this::route);

    public Engine(String name) {
        super(name);
    }

    public Pipeline<Exchange> pipeline() {
        return pipeline;
    }

    /** 边界层唯一会调用的方法：一条窄接口（相当于 Container.invoke / CoyoteAdapter.service） */
    public Exchange invoke(Command command) throws Exception {
        var ex = new Exchange(command);
        pipeline.invoke(ex);
        return ex;
    }

    private void route(Exchange ex) throws Exception {
        Module m = findChild(ex.command().module())
                .orElseThrow(() -> new IllegalArgumentException("没有模块 " + ex.command().module()));
        m.invoke(ex);
    }
}
