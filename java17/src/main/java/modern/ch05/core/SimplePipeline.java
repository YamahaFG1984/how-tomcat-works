package modern.ch05.core;

import jakarta.servlet.ServletException;
import modern.ch03.connector.http.HttpRequest;
import modern.ch03.connector.http.HttpResponse;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 管道：先依次调用所有附加阀，最后调用基础阀。
 * CopyOnWriteArrayList 让「运行时增删阀」和「并发处理请求」互不干扰（读多写少的典型场景）。
 */
public class SimplePipeline implements Pipeline {

    private final Container container;
    private final List<Valve> valves = new CopyOnWriteArrayList<>();
    private Valve basic;

    public SimplePipeline(Container container) {
        this.container = container;
    }

    @Override
    public Valve getBasic() {
        return basic;
    }

    @Override
    public void setBasic(Valve valve) {
        if (valve instanceof Contained c) {
            c.setContainer(container);
        }
        this.basic = valve;
    }

    @Override
    public void addValve(Valve valve) {
        if (valve instanceof Contained c) {
            c.setContainer(container);
        }
        valves.add(valve);
    }

    @Override
    public void removeValve(Valve valve) {
        valves.remove(valve);
    }

    @Override
    public List<Valve> getValves() {
        return List.copyOf(valves);
    }

    @Override
    public void invoke(HttpRequest request, HttpResponse response) throws Exception {
        // 和 Tomcat 4 一样，每个请求创建一个新的 ValveContext，它自己记住走到了第几个阀
        new StandardValveContext(List.copyOf(valves), basic).invokeNext(request, response);
    }

    /** 对应原书 StandardPipelineValveContext / Tomcat 5 的 StandardValveContext */
    private static final class StandardValveContext implements ValveContext {

        private final List<Valve> valves;
        private final Valve basic;
        private int stage;

        StandardValveContext(List<Valve> valves, Valve basic) {
            this.valves = valves;
            this.basic = basic;
        }

        @Override
        public void invokeNext(HttpRequest request, HttpResponse response) throws Exception {
            int subscript = stage++;
            if (subscript < valves.size()) {
                valves.get(subscript).invoke(request, response, this);
            } else if (subscript == valves.size() && basic != null) {
                basic.invoke(request, response, this);
            } else {
                throw new ServletException("No valve");
            }
        }
    }
}
