package modern.scheduler;

import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.io.IOException;

/**
 * 为演示生成一个插件目录：plugins/disk-report/ 下放插件的 class 文件和 META-INF/services 声明，
 * 等价于把一个插件 jar 解压到那里。真实使用时，用户只需要把 jar（或目录）放进 plugins.dir。
 */
final class DemoPlugin {

    private DemoPlugin() { }

    static Path build() throws IOException, URISyntaxException {
        Path classes = Path.of(DemoPlugin.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        Path plugins = Files.createTempDirectory("scheduler-plugins");
        Path plugin = plugins.resolve("disk-report");
        Path pkg = Files.createDirectories(plugin.resolve("modern/scheduler/plugins"));
        Files.copy(classes.resolve("modern/scheduler/plugins/DiskReportTaskType.class"), pkg.resolve("DiskReportTaskType.class"));
        Path services = Files.createDirectories(plugin.resolve("META-INF/services"));
        Files.writeString(services.resolve("modern.scheduler.spi.TaskType"), "modern.scheduler.plugins.DiskReportTaskType\n");
        return plugins;
    }
}
