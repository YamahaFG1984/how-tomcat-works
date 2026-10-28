package modern.ch07.logger;

import modern.ch06.core.Lifecycle;
import modern.ch06.core.LifecycleException;
import modern.ch06.core.LifecycleListener;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 写文件的日志记录器：文件名是「前缀 + 日期 + 后缀」，日期一变就换一个新文件（按天滚动）。
 *
 * 和原书的区别：
 *  - java.sql.Timestamp + substring 取日期 → java.time.LocalDateTime + DateTimeFormatter；
 *  - new File(...).mkdirs() + FileWriter → Files.createDirectories + Files.newBufferedWriter（显式 UTF-8）；
 *  - 原书的双重检查读写了非 volatile 的 date 字段，这里把「检查日期 + 写一行」整体放进 synchronized。
 */
public class FileLogger extends LoggerBase implements Lifecycle {

    private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private String directory = "logs";
    private String prefix = "catalina.";
    private String suffix = ".log";
    private boolean timestamp;

    private LocalDate date;          // 当前打开的文件对应的日期
    private PrintWriter writer;
    private boolean started;

    public void setDirectory(String directory) { this.directory = directory; }
    public void setPrefix(String prefix) { this.prefix = prefix; }
    public void setSuffix(String suffix) { this.suffix = suffix; }
    public void setTimestamp(boolean timestamp) { this.timestamp = timestamp; }

    @Override
    public synchronized void log(String message) {
        LocalDateTime now = LocalDateTime.now();
        if (!now.toLocalDate().equals(date)) {   // 跨天了（或者第一次写）：换文件
            close();
            date = now.toLocalDate();
            open();
        }
        writer.println(timestamp ? TIMESTAMP.format(now) + " " + message : message);
    }

    /** 文件路径：catalina.base（默认当前目录）/ directory / prefix + 2026-10-28 + suffix */
    public Path currentFile() {
        Path dir = Path.of(directory);
        if (!dir.isAbsolute()) {
            dir = Path.of(System.getProperty("catalina.base", System.getProperty("user.dir"))).resolve(dir);
        }
        return dir.resolve(prefix + (date != null ? date : LocalDate.now()) + suffix);
    }

    private void open() {
        try {
            Path file = currentFile();
            Files.createDirectories(file.getParent());
            writer = new PrintWriter(Files.newBufferedWriter(file, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND), true);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void close() {
        if (writer != null) {
            writer.close();
            writer = null;
        }
        date = null;
    }

    /* ---------- Lifecycle：stop 时关闭文件 ---------- */

    @Override public void addLifecycleListener(LifecycleListener listener) { }
    @Override public List<LifecycleListener> findLifecycleListeners() { return List.of(); }
    @Override public void removeLifecycleListener(LifecycleListener listener) { }

    @Override
    public synchronized void start() throws LifecycleException {
        if (started) {
            throw new LifecycleException("FileLogger has already been started");
        }
        started = true;
    }

    @Override
    public synchronized void stop() throws LifecycleException {
        if (!started) {
            throw new LifecycleException("FileLogger has not yet been started");
        }
        started = false;
        close();
    }
}
