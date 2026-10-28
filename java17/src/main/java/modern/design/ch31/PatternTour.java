package modern.design.ch31;

import modern.common.Facade;
import modern.design.kit.LifecycleBase;
import modern.design.kit.LifecycleState;
import modern.design.kit.Pipeline;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * 十二个设计模式，每个都先在 Tomcat 里出现过，这里换成电商订单的业务场景再写一遍。
 * 每个方法自成一体（局部 record / 接口），可以单独阅读。
 *
 * 运行：./run.sh design.ch31.PatternTour
 */
public final class PatternTour {

    public static void main(String[] args) throws Exception {
        facade();
        chainOfResponsibility();
        observer();
        templateMethod();
        composite();
        strategy();
        factoryByName();
        adapter();
        decorator();
        objectPool();
        copyOnWrite();
        stateMachine();
    }

    private static void title(String s) {
        System.out.println("\n== " + s + " ==");
    }

    /* 1. 外观：Tomcat 把 RequestFacade 交给 Servlet（第 2、9、21 章） */
    public interface OrderView {                                     // 外观代理要反射调用它，必须是 public
        String id();
        BigDecimal total();
    }

    static final class Order implements OrderView {
        private final String id;
        private BigDecimal total;

        Order(String id, BigDecimal total) {
            this.id = id;
            this.total = total;
        }

        @Override public String id() { return id; }
        @Override public BigDecimal total() { return total; }
        void applyRefund(BigDecimal amount) { total = total.subtract(amount); }   // 内部方法，插件不该碰
    }

    static void facade() {
        title("1 外观 Facade");
        var order = new Order("A-1", new BigDecimal("100"));
        OrderView forPlugin = Facade.of(OrderView.class, order);       // 第 2 章的 JDK 动态代理外观
        System.out.println("插件看到：" + forPlugin.id() + " " + forPlugin.total());
        System.out.println("能向下转型吗？" + (forPlugin instanceof Order));
    }

    /* 2. 责任链：Pipeline / Valve、FilterChain（第 5、11 章） */
    static final class Cart {
        BigDecimal price;
        final List<String> notes = new ArrayList<>();

        Cart(String price) {
            this.price = new BigDecimal(price);
        }
    }

    static void chainOfResponsibility() throws Exception {
        title("2 责任链 Chain of Responsibility");
        var pricing = new Pipeline<Cart>(c -> c.notes.add("应付 " + c.price));          // 基础阀：结算
        pricing.addValve((c, next) -> {                                                   // 会员 95 折
                    c.price = c.price.multiply(new BigDecimal("0.95")).setScale(2, RoundingMode.HALF_UP);
                    c.notes.add("会员 95 折");
                    next.invoke(c);
                })
                .addValve((c, next) -> {                                                  // 满 100 减 10
                    if (c.price.compareTo(new BigDecimal("100")) >= 0) {
                        c.price = c.price.subtract(BigDecimal.TEN);
                        c.notes.add("满 100 减 10");
                    }
                    next.invoke(c);
                });
        for (String p : new String[] {"120", "80"}) {
            var cart = new Cart(p);
            pricing.invoke(cart);
            System.out.println("原价 " + p + " → " + cart.notes);
        }
    }

    /* 3. 观察者：LifecycleListener、HttpSessionListener（第 6 章） */
    record OrderEvent(String orderId, String type) { }

    static void observer() {
        title("3 观察者 Observer");
        List<Consumer<OrderEvent>> listeners = new CopyOnWriteArrayList<>();
        listeners.add(e -> System.out.println("  [积分] " + e.orderId() + " 加 10 分"));
        listeners.add(e -> System.out.println("  [短信] 通知用户：订单 " + e.orderId() + " " + e.type()));
        var event = new OrderEvent("A-1", "已支付");
        System.out.println("订单服务只发布事件，不认识积分和短信：");
        listeners.forEach(l -> l.accept(event));
    }

    /* 4. 模板方法：LifecycleBase、HttpServlet.service → doGet（第 6、7 章） */
    abstract static class ImportJob {
        /** 流程固定，final 防止子类改顺序 */
        final String run(List<String> rows) {
            int ok = 0;
            for (String row : rows) {
                if (!validate(row)) continue;
                save(row);
                ok++;
            }
            return getClass().getSimpleName() + "：导入 " + ok + "/" + rows.size();
        }

        protected abstract boolean validate(String row);

        protected void save(String row) { }                    // 钩子方法：有默认实现，按需覆盖
    }

    static final class SkuImportJob extends ImportJob {
        @Override protected boolean validate(String row) { return row.matches("SKU-\\d+"); }
    }

    static void templateMethod() {
        title("4 模板方法 Template Method");
        System.out.println(new SkuImportJob().run(List.of("SKU-1", "bad", "SKU-22")));
    }

    /* 5. 组合：Engine ⊃ Host ⊃ Context ⊃ Wrapper（第 5、13 章） */
    sealed interface Item permits Product, Bundle { BigDecimal price(); String name(); }

    record Product(String name, BigDecimal price) implements Item { }

    record Bundle(String name, List<Item> items, BigDecimal discount) implements Item {
        @Override public BigDecimal price() {
            return items.stream().map(Item::price).reduce(BigDecimal.ZERO, BigDecimal::add).subtract(discount);
        }
    }

    static void composite() {
        title("5 组合 Composite");
        var phone = new Product("手机", new BigDecimal("3000"));
        var caseP = new Product("手机壳", new BigDecimal("50"));
        var gift = new Bundle("礼盒", List.of(caseP, new Product("贴膜", new BigDecimal("30"))), new BigDecimal("10"));
        Item set = new Bundle("套装", List.of(phone, gift), new BigDecimal("100"));
        System.out.println(set.name() + " 价格 " + set.price() + "（单品和组合用同一个 price() 计算）");
    }

    /* 6. 策略：Realm、Manager、Loader（第 8～10 章） */
    interface ShippingFee { BigDecimal of(BigDecimal amount); }

    static void strategy() {
        title("6 策略 Strategy");
        Map<String, ShippingFee> strategies = Map.of(
                "standard", amount -> amount.compareTo(new BigDecimal("99")) >= 0 ? BigDecimal.ZERO : new BigDecimal("8"),
                "express", amount -> new BigDecimal("20"));
        for (String s : new String[] {"standard", "express"}) {
            System.out.println(s + "：订单 50 元运费 " + strategies.get(s).of(new BigDecimal("50"))
                    + "，订单 120 元运费 " + strategies.get(s).of(new BigDecimal("120")));
        }
    }

    /* 7. 按名字创建（工厂 + 反射）：Digester 的 ObjectCreateRule、className 属性（第 15 章） */
    interface PaymentGateway { String pay(BigDecimal amount); }

    public static final class AlipayGateway implements PaymentGateway {
        @Override public String pay(BigDecimal amount) { return "支付宝扣款 " + amount; }
    }

    static void factoryByName() throws ReflectiveOperationException {
        title("7 工厂 + 反射装配 Factory by name");
        String configured = PatternTour.class.getName() + "$AlipayGateway";       // 来自配置文件
        var gateway = (PaymentGateway) Class.forName(configured).getConstructor().newInstance();
        System.out.println("配置 " + configured.substring(configured.lastIndexOf('$') + 1) + " → " + gateway.pay(new BigDecimal("88")));
    }

    /* 8. 适配器：CoyoteAdapter、模型 MBean（第 20、23 章） */
    static final class LegacySmsSdk {                                    // 改不了的第三方 SDK
        int sendSms(String mobile, String content, int retry) {
            System.out.println("  [老 SDK] sendSms(" + mobile + ", " + content + ", retry=" + retry + ")");
            return 0;                                                    // 0 表示成功
        }
    }

    interface Notifier { void notify(String user, String text); }

    static void adapter() {
        title("8 适配器 Adapter");
        var sdk = new LegacySmsSdk();
        Map<String, String> mobiles = Map.of("alice", "13800000000");
        Notifier notifier = (user, text) -> {                            // 把老接口翻译成我们需要的接口
            if (sdk.sendSms(mobiles.get(user), text, 3) != 0) throw new IllegalStateException("短信发送失败");
        };
        notifier.notify("alice", "您的订单已发货");
    }

    /* 9. 装饰器：HttpServletRequestWrapper、ApplicationHttpRequest（第 21 章） */
    interface PriceService { BigDecimal priceOf(String sku); }

    static void decorator() {
        title("9 装饰器 Decorator");
        var calls = new AtomicInteger();
        PriceService remote = sku -> {
            calls.incrementAndGet();
            return new BigDecimal("199");
        };
        PriceService cached = new PriceService() {                      // 同一个接口，包在外面加缓存
            private final Map<String, BigDecimal> cache = new ConcurrentHashMap<>();
            @Override public BigDecimal priceOf(String sku) { return cache.computeIfAbsent(sku, remote::priceOf); }
        };
        for (int i = 0; i < 3; i++) cached.priceOf("SKU-1");
        System.out.println("查询 3 次，远程调用 " + calls.get() + " 次");
    }

    /* 10. 对象池：第 4 章的 HttpProcessor 池、第 11 章 STM 实例池、线程池、连接池 */
    static final class PdfRenderer {                                     // 假设创建一次要加载字体，很贵
        static final AtomicInteger CREATED = new AtomicInteger();
        PdfRenderer() { CREATED.incrementAndGet(); }
        String render(String order) { return "PDF(" + order + ")"; }
    }

    static final class Pool<T> {
        private final BlockingQueue<T> idle;

        Pool(int size, Supplier<T> factory) {
            idle = new ArrayBlockingQueue<>(size);
            for (int i = 0; i < size; i++) idle.add(factory.get());
        }

        <R> R with(java.util.function.Function<T, R> work) throws InterruptedException {
            T t = idle.take();                                           // 没有空闲的就等（有界）
            try {
                return work.apply(t);
            } finally {
                idle.add(t);                                             // 一定要还回去
            }
        }
    }

    static void objectPool() throws InterruptedException {
        title("10 对象池 Object Pool");
        var pool = new Pool<>(2, PdfRenderer::new);
        for (int i = 1; i <= 5; i++) pool.with(r -> r.render("A-1"));
        System.out.println("渲染 5 次，PdfRenderer 只创建了 " + PdfRenderer.CREATED.get() + " 个");
    }

    /* 11. 写时复制：LifecycleSupport 的监听器数组、管道的阀数组（第 6 章） */
    record PromotionRules(List<String> rules, int version) { }

    static void copyOnWrite() {
        title("11 写时复制 Copy-on-Write");
        var current = new AtomicReference<>(new PromotionRules(List.of("满100减10"), 1));
        PromotionRules seenByRequest = current.get();                    // 一个正在处理的请求拿到的快照
        current.updateAndGet(old -> {                                    // 运营后台改规则：复制一份再替换引用
            var copy = new ArrayList<>(old.rules());
            copy.add("会员95折");
            return new PromotionRules(List.copyOf(copy), old.version() + 1);
        });
        System.out.println("进行中的请求仍然看到 v" + seenByRequest.version() + " " + seenByRequest.rules());
        System.out.println("新请求看到         v" + current.get().version() + " " + current.get().rules());
    }

    /* 12. 状态机：LifecycleState（第 6 章）、AsyncStateMachine（第 24 章） */
    enum OrderStatus {
        CREATED, PAID, SHIPPED, COMPLETED, CANCELLED;

        private static final Map<OrderStatus, Set<OrderStatus>> NEXT = new EnumMap<>(Map.of(
                CREATED, EnumSet.of(PAID, CANCELLED),
                PAID, EnumSet.of(SHIPPED, CANCELLED),
                SHIPPED, EnumSet.of(COMPLETED),
                COMPLETED, EnumSet.noneOf(OrderStatus.class),
                CANCELLED, EnumSet.noneOf(OrderStatus.class)));

        OrderStatus to(OrderStatus next) {
            if (!NEXT.get(this).contains(next)) throw new IllegalStateException(this + " 不能变成 " + next);
            return next;
        }
    }

    static void stateMachine() {
        title("12 状态机 State Machine");
        var s = OrderStatus.CREATED.to(OrderStatus.PAID).to(OrderStatus.SHIPPED);
        System.out.println("CREATED → PAID → SHIPPED：" + s);
        try {
            s.to(OrderStatus.CANCELLED);
        } catch (IllegalStateException e) {
            System.out.println("已发货再取消：" + e.getMessage());
        }
        // 设计工具包的 LifecycleBase 用的是同样的写法
        var component = new LifecycleBase("demo") {
            @Override protected void startInternal() { }
            @Override protected void stopInternal() { }
        };
        component.start();
        System.out.println("LifecycleBase 也是状态机：" + component.getState() + "，允许的下一步 " + LifecycleState.STARTED.next());
    }
}
