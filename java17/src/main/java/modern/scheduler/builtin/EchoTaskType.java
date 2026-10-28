package modern.scheduler.builtin;

import modern.scheduler.spi.Task;
import modern.scheduler.spi.TaskType;

import java.util.Map;

/** 最简单的任务：打印一条消息。参数 message */
public class EchoTaskType implements TaskType {

    @Override public String name() { return "echo"; }
    @Override public String description() { return "打印 message 参数"; }

    @Override
    public Task create(Map<String, String> params) {
        String message = params.getOrDefault("message", "hello");
        return ctx -> ctx.log(message);
    }
}
