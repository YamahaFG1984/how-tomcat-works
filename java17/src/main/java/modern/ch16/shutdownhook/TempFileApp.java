package modern.ch16.shutdownhook;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 原书 MySwingAppWithShutdownHook 的控制台版（不依赖图形界面，服务器上也能跑）：
 * 启动时创建 temp.txt，无论是正常退出（回车）、Ctrl+C 还是 kill，关闭钩子都会把它删掉。
 *
 * 运行：./run.sh ch16.shutdownhook.TempFileApp
 */
public final class TempFileApp {

    private static final Path TEMP = Path.of(System.getProperty("user.dir"), "temp.txt");

    public static void main(String[] args) throws IOException {
        Runtime.getRuntime().addShutdownHook(new Thread(TempFileApp::shutdown, "cleanup-hook"));
        System.out.println("Creating temporary file " + TEMP);
        Files.writeString(TEMP, "pid=" + ProcessHandle.current().pid());      // Java 9：取得当前进程号
        System.out.println("按回车正常退出，或者在另一个终端执行 kill " + ProcessHandle.current().pid());
        System.in.read();
        System.out.println("正常退出");
    }

    private static void shutdown() {
        try {
            if (Files.deleteIfExists(TEMP)) {
                System.out.println("Deleting temporary file.");
            }
        } catch (IOException e) {
            System.err.println("删除临时文件失败: " + e);
        }
    }
}
