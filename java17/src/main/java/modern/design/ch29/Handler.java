package modern.design.ch29;

import modern.design.kit.LifecycleBase;

/** 最小的业务单元（相当于 Wrapper + Servlet）：只关心自己的业务，不知道请求从哪里来 */
public abstract class Handler extends LifecycleBase {

    protected Handler(String name) {
        super(name);
    }

    public abstract String handle(Command command);

    @Override protected void startInternal() { }
    @Override protected void stopInternal() { }
}
