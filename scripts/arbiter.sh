#!/bin/sh
# Starts Arbiter from the arbiter.jar beside this script, with Java 25 or later.
# Uses $JAVA_HOME/bin/java if JAVA_HOME is set, otherwise the java command on the PATH.

dir=$(CDPATH='' cd -- "$(dirname -- "$0")" && pwd)
jar="$dir/arbiter.jar"
if [ -n "${JAVA_HOME:-}" ]; then
    java="$JAVA_HOME/bin/java"
else
    java=$(command -v java || true)
fi
if [ -z "$java" ] || [ ! -x "$java" ]; then
    echo "Arbiter needs Java 25 or later, but no java command was found. Install Java 25 or set JAVA_HOME." >&2
    exit 1
fi

version=$("$java" -XshowSettings:properties -version 2>&1 | sed -n 's/^ *java\.specification\.version = //p')
major=${version%%.*}
case "$major" in
    '' | *[!0-9]*) major=0 ;;
esac
if [ "$major" -lt 25 ]; then
    echo "Arbiter needs Java 25 or later, but $java is Java ${version:-of an unknown version}. Install Java 25 or set JAVA_HOME." >&2
    exit 1
fi

if [ ! -f "$jar" ]; then
    echo "arbiter.jar is missing from $dir. Keep this script beside arbiter.jar." >&2
    exit 1
fi
exec "$java" -jar "$jar" "$@"
