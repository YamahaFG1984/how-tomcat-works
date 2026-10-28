#!/usr/bin/env bash
# 启动迷你 Tomcat：等价于 catalina.sh start
exec "$(dirname "$(readlink -f "$0")")/catalina.sh" start "$@"
