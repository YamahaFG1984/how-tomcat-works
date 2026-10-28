package modern.ch07.logger;

/**
 * 日志记录器（原书 org.apache.catalina.Logger）。
 * 五个 int 常量换成了枚举；数值越大越啰嗦，DEBUG 记录一切，FATAL 只记录致命错误。
 */
public interface Logger {

    enum Verbosity { FATAL, ERROR, WARNING, INFORMATION, DEBUG }

    Verbosity getVerbosity();

    void setVerbosity(Verbosity verbosity);

    /** 唯一需要子类实现的方法：把一条消息写到目的地 */
    void log(String message);

    void log(String message, Throwable throwable);

    /** 只有 verbosity 不比设定值更啰嗦时才记录 */
    void log(String message, Verbosity verbosity);
}
