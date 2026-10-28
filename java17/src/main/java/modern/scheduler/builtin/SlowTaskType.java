package modern.scheduler.builtin;

import modern.scheduler.spi.Task;
import modern.scheduler.spi.TaskType;

import java.util.Map;

/** 模拟耗时任务：睡 sleep 时长。比周期长时会触发「不重叠」跳过，比超时长时会被中断 */
public class SlowTaskType implements TaskType {

    @Override public String name() { return "slow"; }
    @Override public String description() { return "睡 sleep 毫秒"; }

    @Override
    public Task create(Map<String, String> params) {
        long ms = Long.parseLong(params.getOrDefault("sleep", "1000"));
        return ctx -> {
            ctx.log("开始，预计 " + ms + " ms");
            Thread.sleep(ms);                                  // 响应中断：超时拦截器能打断它
            ctx.log("完成");
        };
    }
}
