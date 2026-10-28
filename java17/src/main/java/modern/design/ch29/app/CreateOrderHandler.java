package modern.design.ch29.app;

import modern.design.ch29.Command;
import modern.design.ch29.Handler;

public class CreateOrderHandler extends Handler {

    public CreateOrderHandler(String name) {
        super(name);
    }

    @Override
    public String handle(Command c) {
        int amount = Integer.parseInt(c.param("amount"));
        if (amount <= 0) throw new IllegalArgumentException("金额必须为正数");
        OrderStore.ORDERS.put(c.param("id"), amount);
        return "订单 " + c.param("id") + " 已创建，金额 " + amount;
    }
}
