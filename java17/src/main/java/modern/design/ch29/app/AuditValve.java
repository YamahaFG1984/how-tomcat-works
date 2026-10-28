package modern.design.ch29.app;

import modern.design.ch29.Exchange;
import modern.design.kit.Pipeline;

/** 模块级的阀：只有 order 模块需要审计，cancel 需要 operator 参数，否则直接短路 */
public class AuditValve implements Pipeline.Valve<Exchange> {

    @Override
    public void invoke(Exchange ex, Pipeline.Next<Exchange> next) throws Exception {
        if (ex.command().action().equals("cancel") && ex.command().param("operator") == null) {
            ex.respond("拒绝：取消订单必须提供 operator");          // 不调用 next：短路
            return;
        }
        System.out.println("    [AuditValve] 审计 " + ex.command());
        next.invoke(ex);
    }
}
