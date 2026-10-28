package modern.ch05.core;

/** 实现了它的阀会在加入管道时得知自己属于哪个容器 */
public interface Contained {

    Container getContainer();

    void setContainer(Container container);
}
