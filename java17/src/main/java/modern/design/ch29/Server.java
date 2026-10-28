package modern.design.ch29;

import modern.design.kit.CompositeComponent;
import modern.design.kit.Lifecycle;

/**
 * 顶层组件（相当于 Server/Service）：先加 Engine 再加 Endpoint，
 * 于是启动时先把核心准备好、最后才「开门」；停止时先「关门」、再停核心——和 StandardService 的顺序一致。
 */
public class Server extends CompositeComponent<Lifecycle> {

    public Server(String name) {
        super(name);
    }
}
