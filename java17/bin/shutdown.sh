#!/usr/bin/env bash
# 停止迷你 Tomcat：等价于 catalina.sh stop
exec "$(dirname "$(readlink -f "$0")")/catalina.sh" stop "$@"
