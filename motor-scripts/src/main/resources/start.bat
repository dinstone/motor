@echo off

cd %~dp0
set "CURRENT_DIR=%cd%"
echo CURRENT_DIR = %CURRENT_DIR%

if exist "%LAUNCHER_HOME%\bin\bootstrap.jar" goto okHome
cd ..
set "LAUNCHER_HOME=%cd%"

:okHome

cd "%LAUNCHER_HOME%"
echo LAUNCHER_HOME = %LAUNCHER_HOME%

rem set JAVA_OPTS=-server -Xss256k -Xms1g -Xmx1g -Xmn128m -XX:PermSize=128m -XX:MaxPermSize=256m -Djava.net.preferIPv4Stack=true -Dfile.encoding=UTF-8
rem set JPDA_OPTS=-Xdebug -Xrunjdwp:transport=dt_socket,address=8888,server=y,suspend=y
rem set JAVA_GC=-XX:+DisableExplicitGC -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/tmp/jvm.hprof -XX:+PrintClassHistogram -Xloggc:/tmp/gc.log -XX:+PrintGCDetails -XX:+PrintGCTimeStamps -XX:+PrintHeapAtGC


set CLASSPATH="%LAUNCHER_HOME%/bin/bootstrap.jar;%LAUNCHER_HOME%/bin"

java %JAVA_OPTS% %JPDA_OPTS% %LOGGING_CONFIG% -classpath %CLASSPATH% com.dinstone.motor.launcher.Launcher start

:end