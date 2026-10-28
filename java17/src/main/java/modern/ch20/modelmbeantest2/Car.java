package modern.ch20.modelmbeantest2;

public class Car {

    private String color = "red";

    public Car() {
        System.out.println("Car constructor");
    }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    public void drive() {
        System.out.println("Baby you can drive my car.");
    }
}
