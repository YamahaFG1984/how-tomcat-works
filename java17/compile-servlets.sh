#!/usr/bin/env bash
# 把 webroot/*.java（示例 Servlet）编译成 .class，放到两个地方：
#   webroot/                 —— 第 2~7 章的容器直接从 webroot 加载类
#   myApp/WEB-INF/classes/   —— 第 8 章起按 Web 应用的标准目录结构加载
#   webapps/app1/WEB-INF/classes/ —— 第 13 章起，Host 的 appBase 下的应用
# 修改了 Servlet 源码、想观察第 8 章的「自动重载」时，单独运行本脚本即可。
set -euo pipefail
cd "$(dirname "$0")"
[[ -f target/cp.txt ]] || mvn -q dependency:build-classpath -Dmdep.outputFile=target/cp.txt >/dev/null
[[ -d target/classes ]] || mvn -q compile
mkdir -p myApp/WEB-INF/classes webapps/app1/WEB-INF/classes
javac -nowarn -cp "target/classes:$(cat target/cp.txt)" -d webroot webroot/*.java
javac -nowarn -cp "target/classes:$(cat target/cp.txt)" -d myApp/WEB-INF/classes webroot/*.java
javac -nowarn -cp "target/classes:$(cat target/cp.txt)" -d webapps/app1/WEB-INF/classes webroot/PrimitiveServlet.java webroot/ModernServlet.java
