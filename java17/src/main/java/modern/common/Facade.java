package modern.common;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;

/**
 * 用 JDK 动态代理生成「外观（facade）」对象。
 *
 * 原书的 RequestFacade / ResponseFacade 是手写的：每个接口方法都要写一个转发方法，
 * 一个类就是上百行样板代码（Tomcat 11 至今仍是这样手写的，因为它还要处理安全管理器等细节）。
 * 动态代理在运行时生成一个只实现了 iface 接口的类，所有调用都转发给 target。
 * Servlet 拿到的代理对象不是 target 的类型，因此无法向下转型去调用容器内部方法。
 */
public final class Facade {

    private Facade() {
    }

    public static <T> T of(Class<T> iface, T target) {
        Object proxy = Proxy.newProxyInstance(iface.getClassLoader(), new Class<?>[] {iface},
                (self, method, args) -> {
                    if (method.getDeclaringClass() == Object.class) {
                        return switch (method.getName()) {
                            case "equals" -> self == args[0];
                            case "hashCode" -> System.identityHashCode(self);
                            default -> iface.getSimpleName() + "Facade@" + Integer.toHexString(System.identityHashCode(self));
                        };
                    }
                    try {
                        return method.invoke(target, args);
                    } catch (InvocationTargetException e) {
                        throw e.getCause();     // 把被调用方法抛出的原始异常交还给调用者
                    }
                });
        return iface.cast(proxy);
    }
}
