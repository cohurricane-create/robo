#!/bin/sh
# Gradle wrapper script for Unix
APP_BASE_NAME=`basename "$0"`
DIRNAME=`dirname "$0"`
[ -z "$DIRNAME" ] && DIRNAME=.
CLASSPATH=$DIRNAME/gradle/wrapper/gradle-wrapper.jar
exec java -jar "$CLASSPATH" "$@"
