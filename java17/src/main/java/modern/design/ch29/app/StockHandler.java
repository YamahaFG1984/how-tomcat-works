package modern.design.ch29.app;

import modern.design.ch29.Command;
import modern.design.ch29.Handler;

public class StockHandler extends Handler {

    public StockHandler(String name) {
        super(name);
    }

    @Override
    public String handle(Command c) {
        return "商品 " + c.param("sku") + " 库存 42";
    }
}
