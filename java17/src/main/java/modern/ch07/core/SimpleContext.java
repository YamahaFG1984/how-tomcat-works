package modern.ch07.core;

import modern.ch06.core.Lifecycle;
import modern.ch06.core.LifecycleException;
import modern.ch07.logger.Logger;

/** 在第 6 章 SimpleContext 的基础上加一个 Logger：启动/停止时记日志，并负责启停 Logger 本身 */
public class SimpleContext extends modern.ch06.core.SimpleContext {

    private Logger logger;

    public Logger getLogger() {
        return logger;
    }

    public void setLogger(Logger logger) {
        this.logger = logger;
    }

    @Override
    public synchronized void start() throws LifecycleException {
        Lifecycle.start(logger);           // 先启动 Logger，后面的日志才有地方写
        log("starting Context");
        super.start();
        log("Context started");
    }

    @Override
    public synchronized void stop() throws LifecycleException {
        log("stopping Context");
        super.stop();
        log("Context stopped");
        Lifecycle.stop(logger);            // 最后关闭日志文件
    }

    public void log(String message) {
        if (logger != null) {
            logger.log(message);
        }
    }
}
