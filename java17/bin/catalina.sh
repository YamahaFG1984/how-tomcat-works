#!/usr/bin/env bash
# -----------------------------------------------------------------------------
# 第 17 章：迷你 Tomcat 的启动/停止脚本（仿照 Tomcat 的 bin/catalina.sh）
#
#   catalina.sh run     在当前终端前台运行
#   catalina.sh start   后台运行，输出写到 logs/catalina.out，进程号写到 CATALINA_PID
#   catalina.sh stop    读取 conf/server.xml 中的关闭端口，发送关闭命令
#
# 环境变量：
#   CATALINA_HOME   安装目录（默认：本脚本所在目录的上一级）
#   CATALINA_BASE   实例目录（conf/、logs/、webapps/ 所在，默认等于 CATALINA_HOME）
#   CATALINA_PID    进程号文件（默认 $CATALINA_BASE/logs/catalina.pid）
#   JAVA_OPTS       传给 JVM 的参数，例如 -Xmx512m
# -----------------------------------------------------------------------------
set -euo pipefail

# 解析符号链接，找到脚本真实所在的目录（原书 startup.sh 里那段 while [ -h "$PRG" ] 的现代写法）
PRG="$(readlink -f "$0")"
CATALINA_HOME="${CATALINA_HOME:-$(cd "$(dirname "$PRG")/.." && pwd)}"
CATALINA_BASE="${CATALINA_BASE:-$CATALINA_HOME}"
CATALINA_PID="${CATALINA_PID:-$CATALINA_BASE/logs/catalina.pid}"
JAVA="${JAVA_HOME:+$JAVA_HOME/bin/}java"

# 类路径：编译好的类 + Maven 依赖（真实 Tomcat 这里只有 bootstrap.jar 和 tomcat-juli.jar）
cd "$CATALINA_HOME"
if [[ ! -f target/cp.txt ]]; then
  mvn -q dependency:build-classpath -Dmdep.outputFile=target/cp.txt >/dev/null
fi
[[ -d target/classes ]] || mvn -q compile
[[ -d webapps/app1/WEB-INF/classes ]] || ./compile-servlets.sh
CLASSPATH="$CATALINA_HOME/target/classes:$(cat target/cp.txt)"

run_java() {
  "$JAVA" ${JAVA_OPTS:-} -classpath "$CLASSPATH" \
    -Dcatalina.home="$CATALINA_HOME" -Dcatalina.base="$CATALINA_BASE" \
    modern.ch17.startup.Bootstrap "$@"
}

case "${1:-}" in
  run)
    shift
    exec "$JAVA" ${JAVA_OPTS:-} -classpath "$CLASSPATH" \
      -Dcatalina.home="$CATALINA_HOME" -Dcatalina.base="$CATALINA_BASE" \
      modern.ch17.startup.Bootstrap "$@" start
    ;;
  start)
    shift
    mkdir -p "$CATALINA_BASE/logs"
    if [[ -f "$CATALINA_PID" ]] && kill -0 "$(cat "$CATALINA_PID")" 2>/dev/null; then
      echo "已经在运行（PID $(cat "$CATALINA_PID")）"; exit 1
    fi
    # nohup + 后台运行；stdin 指向 /dev/null，输出追加到 catalina.out
    nohup "$JAVA" ${JAVA_OPTS:-} -classpath "$CLASSPATH" \
      -Dcatalina.home="$CATALINA_HOME" -Dcatalina.base="$CATALINA_BASE" \
      modern.ch17.startup.Bootstrap "$@" start \
      >> "$CATALINA_BASE/logs/catalina.out" 2>&1 < /dev/null &
    echo $! > "$CATALINA_PID"
    echo "已启动，PID $(cat "$CATALINA_PID")，日志：$CATALINA_BASE/logs/catalina.out"
    ;;
  stop)
    shift
    run_java "$@" stop
    if [[ "${1:-}" == "-force" && -f "$CATALINA_PID" ]]; then
      sleep 5
      kill -9 "$(cat "$CATALINA_PID")" 2>/dev/null && echo "已强制结束"
    fi
    rm -f "$CATALINA_PID"
    ;;
  *)
    echo "用法: catalina.sh ( run | start | stop [-force] )"
    exit 1
    ;;
esac
