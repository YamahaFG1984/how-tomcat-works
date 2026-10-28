#!/usr/bin/env bash
# 用法：./run.sh ch01.HttpServer [参数...]
#       ./run.sh ch05.Bootstrap1
#       JAVA_OPTS="-Xmx256m" ./run.sh ch28.leak.ClassLoaderLeakDemo   （JAVA_OPTS 传给 JVM）
# 第一次运行会先 mvn compile，并把依赖的 classpath 缓存到 target/cp.txt
set -euo pipefail
cd "$(dirname "$0")"

if [[ $# -lt 1 ]]; then
  echo "用法: $0 <chNN.主类> [参数...]   例如: $0 ch01.HttpServer" >&2
  exit 1
fi

main="modern.$1"; shift

if [[ ! -f target/cp.txt || pom.xml -nt target/cp.txt ]]; then
  mvn -q dependency:build-classpath -Dmdep.outputFile=target/cp.txt >/dev/null
fi
mvn -q compile

# webroot/ 下的示例 Servlet 不属于 Maven 工程：它们和原书一样，由容器在运行时
# 用自己的类加载器加载。源码比 .class 新时重新编译（见 compile-servlets.sh）。
for src in webroot/*.java; do
  cls="${src%.java}.class"
  if [[ ! -f "$cls" || "$src" -nt "$cls" || ! -d myApp/WEB-INF/classes ]]; then
    ./compile-servlets.sh
    break
  fi
done

# shellcheck disable=SC2086  # JAVA_OPTS 需要按空格拆成多个参数
exec java ${JAVA_OPTS:-} -cp "target/classes:$(cat target/cp.txt)" "$main" "$@"
