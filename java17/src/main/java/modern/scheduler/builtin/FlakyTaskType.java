package modern.scheduler.builtin;

import modern.scheduler.spi.Task;
import modern.scheduler.spi.TaskType;

import java.util.Map;

/** 模拟不稳定的下游：每次执行的前 failTimes 次尝试都失败。用来演示重试拦截器 */
public class FlakyTaskType implements TaskType {

    @Override public String name() { return "flaky"; }
    @Override public String description() { return "前 failTimes 次尝试失败"; }

    @Override
    public Task create(Map<String, String> params) {
        int failTimes = Integer.parseInt(params.getOrDefault("failTimes", "1"));
        return ctx -> {
            if (ctx.attempt() <= failTimes) throw new IllegalStateException("下游 503（第 " + ctx.attempt() + " 次）");
            ctx.log("下游恢复，处理成功");
        };
    }
}
