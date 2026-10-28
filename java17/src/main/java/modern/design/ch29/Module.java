package modern.design.ch29;

import modern.design.kit.CompositeComponent;
import modern.design.kit.Pipeline;

/**
 * 一个业务模块（相当于 Context）：按动作名找到 Handler。它自己也有一条管道，模块级的横切逻辑挂在这里。
 */
public class Module extends CompositeComponent<Handler> {

    private final Pipeline<Exchange> pipeline = new Pipeline<>(this::dispatch);   // 基础阀：分派给 Handler

    public Module(String name) {
        super(name);
    }

    public Pipeline<Exchange> pipeline() {
        return pipeline;
    }

    public void invoke(Exchange ex) throws Exception {
        pipeline.invoke(ex);
    }

    private void dispatch(Exchange ex) {
        Handler h = findChild(ex.command().action())
                .orElseThrow(() -> new IllegalArgumentException("模块 " + getName() + " 没有动作 " + ex.command().action()));
        if (!h.getState().isAvailable()) throw new IllegalStateException(h.getName() + " 不可用");
        ex.respond(h.handle(ex.command()));
    }
}
