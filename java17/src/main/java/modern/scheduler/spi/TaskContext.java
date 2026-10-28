package modern.scheduler.spi;

import java.util.Map;

/**
 * 交给任务的上下文（相当于 RequestFacade）：只有任务需要的东西，碰不到调度器内部。
 */
public interface TaskContext {

    String jobName();

    Map<String, String> params();

    /** 第几次尝试（重试拦截器会增加它） */
    int attempt();

    void log(String message);
}
