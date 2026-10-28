package modern.design.ch29.app;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** 演示用的「数据库」：真实项目里它在基础设施层，通过接口注入 */
final class OrderStore {

    static final Map<String, Integer> ORDERS = new ConcurrentHashMap<>();

    private OrderStore() { }
}
