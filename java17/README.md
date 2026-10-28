# How Tomcat Works —— Java 17 对照代码

《How Tomcat Works》原书配套代码（`../HowTomcatWorks`）基于 J2SE 1.4、Servlet 2.3 和 Tomcat 4.1.12。
这里是每一章的 **Java 17+ 重写版**，配套的图解教程在 `../docs`（可直接部署到 GitHub Pages）。

- 语言级别：`--release 17`（用 JDK 17 或更高版本编译运行；在 JDK 21 上测试通过）
- 唯一依赖：`org.apache.tomcat.embed:tomcat-embed-core:11.0.21`
  （自带 jakarta.servlet 6.1 API、Catalina、Digester、JULI、modeler）

## 快速开始

```bash
./run.sh ch01.HttpServer                  # 第一次运行会自动 mvn compile 并编译示例 Servlet
curl http://localhost:8080/index.html
curl http://localhost:8080/SHUTDOWN       # 第 1 章的服务器靠这个 URL 关闭
```

`run.sh <类名> [参数...]` 中的类名省略了公共前缀 `modern.`。大多数服务器的第一个参数是端口号（默认 8080），
按回车即可停止（第 14 章起改用关闭端口，第 16 章起支持 Ctrl+C / kill）。

## 每章可运行的类

每章通常包含两部分：`startup/`（或章节根包）是和原书类一一对应的「迷你 Catalina」，
接在前几章的代码上逐步长大；`tomcat/` 是用嵌入式 Tomcat 11 真实 API 写出的同等程序。

| 章 | 主题 | 可运行的类 |
|---|---|---|
| 1 | 简单 Web 服务器 | `ch01.HttpServer` |
| 2 | 简单 Servlet 容器、外观 | `ch02.HttpServer1` `ch02.HttpServer2` |
| 3 | 连接器 | `ch03.startup.Bootstrap` |
| 4 | 默认连接器（处理器池、keep-alive、分块） | `ch04.startup.Bootstrap [端口] [min] [max]` |
| 5 | 容器、管道与阀 | `ch05.startup.Bootstrap1` `ch05.startup.Bootstrap2` `ch05.tomcat.EmbeddedBootstrap` |
| 6 | 生命周期 | `ch06.startup.Bootstrap` `ch06.tomcat.EmbeddedLifecycle` |
| 7 | 日志 | `ch07.startup.Bootstrap` `ch07.juli.JuliFileHandlerDemo` |
| 8 | 载入器、自动重载 | `ch08.startup.Bootstrap` `ch08.tomcat.EmbeddedReload` |
| 9 | Session 管理 | `ch09.startup.Bootstrap [端口] [超时秒数]` `ch09.tomcat.EmbeddedSession` |
| 10 | 安全 | `ch10.startup.Bootstrap1` `ch10.startup.Bootstrap2` `ch10.tomcat.EmbeddedSecurity` |
| 11 | StandardWrapper、过滤器链 | `ch11.startup.Bootstrap` `ch11.startup.InstancePoolDemo` `ch11.tomcat.EmbeddedWrapper` |
| 12 | StandardContext、映射规则、后台线程 | `ch12.startup.MapperDemo` `ch12.startup.Bootstrap` `ch12.tomcat.EmbeddedMapping` |
| 13 | Host 与 Engine、虚拟主机 | `ch13.startup.Bootstrap1` `ch13.startup.Bootstrap2` `ch13.tomcat.EmbeddedVirtualHosts` |
| 14 | Server 与 Service | `ch14.startup.Bootstrap [http] [http2] [关闭端口]` `ch14.startup.Stopper [关闭端口]` `ch14.tomcat.EmbeddedServer` |
| 15 | Digester、ContextConfig | `ch15.digestertest.Test01/02/03` `ch15.mini.MiniDigesterTest` `ch15.startup.Bootstrap` `ch15.tomcat.EmbeddedContextConfig` |
| 16 | 关闭钩子 | `ch16.shutdownhook.ShutdownHookDemo` `ch16.shutdownhook.TempFileApp` `ch16.startup.Bootstrap` `ch16.tomcat.EmbeddedShutdownHook` |
| 17 | 启动（Catalina、Bootstrap、脚本） | `bin/startup.sh` `bin/shutdown.sh` `ch17.startup.Bootstrap start/stop` `ch17.tomcat.RealCatalina` |
| 18 | 部署器 | `ch18.startup.Bootstrap` `ch18.tomcat.EmbeddedHostConfig` |
| 19 | Manager Servlet | `ch19.startup.Bootstrap` `ch19.tomcat.EmbeddedManager` |
| 20 | JMX | `ch20.standardmbeantest.StandardAgent` `ch20.modelmbeantest1.ModelAgent` `ch20.modelmbeantest2.ModelAgent` `ch20.startup.MiniJmxServer` `ch20.tomcat.EmbeddedJmx` |
| 21 | ServletContext、RequestDispatcher | `ch21.mini.DispatcherDemo` `ch21.tomcat.EmbeddedDispatch` |
| 22 | 静态资源、错误页、字符编码 | `ch22.tomcat.EmbeddedStatic`（含 `ch22.mini.MiniDefaultServlet`） |
| 23 | Coyote 与 NIO 连接器 | `ch23.nio.Bootstrap [端口] [worker] [maxConnections]` `ch23.nio.SlowClients [端口] [连接数] [秒]` `ch23.tomcat.EmbeddedNio` |
| 24 | 异步 Servlet、非阻塞 IO | `ch24.mini.AsyncBootstrap` `ch24.tomcat.EmbeddedAsync` `ch24.LoadClient <URL> [并发数]` |
| 25 | 可插拔性、SCI、Spring Boot 式启动 | `ch25.demo.DemoApplication`（MiniBoot） `ch25.tomcat.EmbeddedPluggability [端口] [metadataComplete]` |

## 目录结构

```
java17/
├── pom.xml
├── run.sh                    运行某一章的主类
├── compile-servlets.sh       把 webroot/*.java 编译到 webroot/、myApp/、webapps/app1/
├── bin/                      第 17 章的 catalina.sh / startup.sh / shutdown.sh
├── conf/                     server.xml（迷你 Catalina）、tomcat11/server.xml、tomcat-users.xml
├── etc/                      第 15 章 Digester 示例用的 XML
├── webroot/                  示例 Servlet / Filter 源码（jakarta 版）+ 静态文件
├── myApp/WEB-INF/web.xml     第 8 章起使用的 Web 应用
├── webapps/app1/             第 13 章起使用的 Web 应用
└── src/main/java/modern/
    ├── common/               各章共用：动态代理外观、Servlet 接口的默认实现
    └── ch01 … ch20/
```

## 说明

- 迷你 Catalina 是教学代码：它刻意保持和原书相同的结构，只实现每章讨论的功能，不适合用于生产。
- 嵌入式 Tomcat 的示例默认只监听 `127.0.0.1`。
- 运行 Tomcat 11 的示例时出现的 `--add-opens ... memory leak detection` 警告可以忽略。
