#!/bin/sh

LAUNCHER_HOME=$(cd `dirname $0`; cd .. ; pwd)
JAVA_OPTS="-Xms10m"
LOGGING_CONFIG="-Djava.util.logging.config.file=$LAUNCHER_HOME/config/logging.properties"
CLASSPATH="$LAUNCHER_HOME/bin/bootstrap.jar"

java $JAVA_OPTS -Dlauncher.home=$LAUNCHER_HOME "$LOGGING_CONFIG" -classpath "$CLASSPATH" com.dinstone.motor.launcher.Launcher stop