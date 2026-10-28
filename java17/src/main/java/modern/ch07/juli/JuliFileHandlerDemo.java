package modern.ch07.juli;

import org.apache.juli.FileHandler;
import org.apache.juli.OneLineFormatter;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 现代 Tomcat 的做法：Tomcat 5.5 起删除了 org.apache.catalina.Logger 和 FileLogger，
 * 改用 JULI——一个基于 java.util.logging（JUL）的日志实现。
 * JULI 的 FileHandler 就是 FileLogger 的直系后代：同样是 directory + prefix + 日期 + suffix、按天滚动，
 * 还多了 maxDays（自动删除过期文件）和异步写入等功能。
 *
 * 运行：./run.sh ch07.juli.JuliFileHandlerDemo   然后查看 logs/juli-demo.日期.log
 */
public final class JuliFileHandlerDemo {

    public static void main(String[] args) {
        var handler = new FileHandler("logs", "juli-demo.", ".log", 7);   // 只保留最近 7 天
        handler.setFormatter(new OneLineFormatter());                      // Tomcat 默认的单行格式

        Logger logger = Logger.getLogger("modern.ch07");
        logger.setUseParentHandlers(false);                                // 不再输出到控制台
        logger.addHandler(handler);
        logger.setLevel(Level.FINE);                                       // 相当于 verbosity

        logger.info("starting Context");
        logger.fine("一条调试消息（FINE 级别）");
        logger.finest("FINEST 低于设定级别，不会被记录");
        logger.log(Level.SEVERE, "出错了", new IllegalStateException("示例异常"));
        handler.close();
        System.out.println("已写入 logs/juli-demo.<今天的日期>.log");
    }
}
