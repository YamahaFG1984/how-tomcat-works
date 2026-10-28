package modern.ch07.logger;

import java.io.PrintWriter;
import java.io.StringWriter;

/** 实现除 log(String) 以外的全部方法；默认级别 ERROR，和原书一致 */
public abstract class LoggerBase implements Logger {

    protected volatile Verbosity verbosity = Verbosity.ERROR;

    @Override
    public Verbosity getVerbosity() {
        return verbosity;
    }

    @Override
    public void setVerbosity(Verbosity verbosity) {
        this.verbosity = verbosity;
    }

    /** 原书 setVerbosityLevel(String) 用一串 if/else 比较字符串；枚举自带 valueOf */
    public void setVerbosityLevel(String level) {
        this.verbosity = Verbosity.valueOf(level.toUpperCase());
    }

    @Override
    public void log(String message, Throwable throwable) {
        var trace = new StringWriter();
        throwable.printStackTrace(new PrintWriter(trace));
        log(message + System.lineSeparator() + trace);
    }

    @Override
    public void log(String message, Verbosity level) {
        if (this.verbosity.compareTo(level) >= 0) {      // 枚举按声明顺序比较
            log(message);
        }
    }
}
