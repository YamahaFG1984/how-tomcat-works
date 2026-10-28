package modern.scheduler.core;

/** 每个任务一个 MBean（第 20 章）：运维可以在 JConsole 里看统计、暂停、手动触发 */
public interface JobMXBean {

    String getType();
    String getSchedule();
    long getRunCount();
    long getFailureCount();
    long getSkippedCount();
    long getLastDurationMillis();
    boolean isPaused();

    void pause();
    void resume();
    void runNow();
}
