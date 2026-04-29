#!/bin/sh

APP_NAME="Gradle"
APP_BASE_NAME=$(basename "$0")
APP_HOME=$(dirname "$(readlink -f "$0")" 2>/dev/null || cd "$(dirname "$0")" && pwd)

CLASSPATH=$APP_HOME/Gradle/gradle-wrapper.jar

exec java $JAVA_OPTS \
  -classpath "$CLASSPATH" \
  org.gradle.wrapper.GradleWrapperMain \
  "$@"
