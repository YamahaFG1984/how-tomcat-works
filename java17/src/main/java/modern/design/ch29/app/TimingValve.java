package modern.design.ch29.app;

import modern.design.ch29.Exchange;
import modern.design.kit.Pipeline;

/** Engine 级的阀：所有模块共享的横切逻辑 */
public class TimingValve implements Pipeline.Valve<Exchange> {

    @Override
    public void invoke(Exchange ex, Pipeline.Next<Exchange> next) throws Exception {
        long t0 = System.nanoTime();
        try {
            next.invoke(ex);
        } finally {
            System.out.printf("    [TimingValve] %s.%s 用时 %d µs%n",
                    ex.command().module(), ex.command().action(), (System.nanoTime() - t0) / 1000);
        }
    }
}
