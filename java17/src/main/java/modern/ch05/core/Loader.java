package modern.ch05.core;

/** 载入器：为容器提供加载 Servlet 类的类加载器（第 8 章详讲） */
public interface Loader {

    ClassLoader getClassLoader();
}
