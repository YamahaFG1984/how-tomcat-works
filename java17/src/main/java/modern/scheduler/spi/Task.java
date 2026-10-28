package modern.scheduler.spi;

/** 一次任务执行（相当于 Servlet.service）：拿到上下文，做完返回，失败抛异常 */
@FunctionalInterface
public interface Task {

    void run(TaskContext ctx) throws Exception;
}
