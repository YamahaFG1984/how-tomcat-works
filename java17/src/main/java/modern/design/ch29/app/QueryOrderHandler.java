package modern.design.ch29.app;

import modern.design.ch29.Command;
import modern.design.ch29.Handler;

public class QueryOrderHandler extends Handler {

    public QueryOrderHandler(String name) {
        super(name);
    }

    @Override
    public String handle(Command c) {
        Integer amount = OrderStore.ORDERS.get(c.param("id"));
        return amount == null ? "订单 " + c.param("id") + " 不存在" : "订单 " + c.param("id") + " 金额 " + amount;
    }
}
