package modern.scheduler.plugins;

import modern.scheduler.spi.Task;
import modern.scheduler.spi.TaskType;

import java.io.File;
import java.util.Map;

/**
 * 「第三方插件」：报告磁盘可用空间。演示时会被复制到插件目录，由独立的 PluginClassLoader 加载。
 */
public class DiskReportTaskType implements TaskType {

    @Override public String name() { return "disk-report"; }
    @Override public String description() { return "报告 path 所在磁盘的可用空间"; }

    @Override
    public Task create(Map<String, String> params) {
        var path = new File(params.getOrDefault("path", "."));
        int warnPercent = Integer.parseInt(params.getOrDefault("warnPercent", "10"));
        return ctx -> {
            long free = path.getUsableSpace(), total = path.getTotalSpace();
            long percent = total == 0 ? 0 : free * 100 / total;
            ctx.log(String.format("可用 %d%%（%d GB / %d GB）%s，由 %s 加载", percent, free >> 30, total >> 30,
                    percent < warnPercent ? " ⚠ 低于阈值" : "", getClass().getClassLoader()));
        };
    }
}
