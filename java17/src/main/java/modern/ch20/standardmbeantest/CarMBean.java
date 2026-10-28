package modern.ch20.standardmbeantest;

/** 标准 MBean 的命名约定：被管理的类叫 Car，接口就必须叫 CarMBean；接口里列出的方法才会暴露给管理程序 */
public interface CarMBean {

    String getColor();

    void setColor(String color);

    void drive();
}
