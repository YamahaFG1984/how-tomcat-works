package modern.ch12.core;

/** 需要周期性做点事的组件实现它（Tomcat 5 给 Container 接口加了 backgroundProcess 方法） */
public interface BackgroundProcessable {

    void backgroundProcess();
}
