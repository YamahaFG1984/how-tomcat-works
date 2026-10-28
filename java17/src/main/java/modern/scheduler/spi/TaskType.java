package modern.scheduler.spi;

import java.util.Map;

/**
 * 插件的扩展点（SPI）：一种任务类型。插件 jar 在 META-INF/services/modern.scheduler.spi.TaskType 里声明实现类，
 * 配置文件里用 name() 引用它。create 在装配时调用一次，返回的 Task 会被反复执行。
 */
public interface TaskType {

    String name();

    Task create(Map<String, String> params);

    /** 后加的方法用 default 实现，老插件不用改（第 30 章标准 6） */
    default String description() {
        return name();
    }
}
