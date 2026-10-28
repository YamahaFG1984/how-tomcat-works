package modern.ch20.modelmbeantest1;

/** 模型 MBean 的好处：被管理的类不需要实现任何接口 */
public class Car {

    private String color = "red";

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    public void drive() {
        System.out.println("Baby you can drive my car.");
    }
}
