@echo off

setlocal

set "CURRENT_DIR=%cd%"
rem Guess LAUNCHER_HOME if not defined
if not "%LAUNCHER_HOME%" == "" goto gotHome
set "LAUNCHER_HOME=%CURRENT_DIR%"
if exist "%LAUNCHER_HOME%\bin\bootstrap.jar" goto okHome
cd ..
set "LAUNCHER_HOME=%cd%"
cd "%CURRENT_DIR%"

:gotHome
if exist "%LAUNCHER_HOME%\bin\bootstrap.jar" goto okHome
echo The LAUNCHER_HOME environment variable is not defined correctly
echo This environment variable is needed to run this program
goto end

:okHome
cd "%LAUNCHER_HOME%"

set "LAUNCHER_TMPDIR=%LAUNCHER_HOME%\temp"
set "CLASSPATH=%LAUNCHER_HOME%\bin\bootstrap.jar"
set "MAINCLASS=com.dinstone.motor.launcher.Launcher"

set "LAUNCHER_CONFIG=%LAUNCHER_HOME%\bin\launcher.properties"
set "LAUNCHER_OPTS=-Dlauncher.home=%LAUNCHER_HOME% -Dlauncher.config=%LAUNCHER_CONFIG%"

set "GC_LOG=%LAUNCHER_HOME%\logs\gc.log"
set JAVA_OPTS=-server -Xss{{Xss:256k}} -Xms{{Xms:1g}} -Xmx{{Xmx:1g}} -Xmn{{Xmn:256m}} -Djava.net.preferIPv4Stack=true -Dfile.encoding=UTF-8
set JAVA_GC=-verbose:gc -Xloggc:%GC_LOG% -XX:+PrintGCDetails -XX:+PrintGCDateStamps
rem set JPDA_OPTS=-Xdebug -Xrunjdwp:transport=dt_socket,address=8888,server=y,suspend=y

echo Using LAUNCHER_HOME: "%LAUNCHER_HOME%"
echo Using LAUNCHER_OPTS: "%LAUNCHER_OPTS%"
echo Using JAVA_OPTS    : "%JAVA_OPTS%"

if ""%1"" == ""start"" goto doStart
if ""%1"" == ""stop"" goto doStop
if ""%1"" == ""info"" goto doInfo

echo Usage:  launcher ( commands ... )
echo commands:
echo   start             Start launcher
echo   stop              Stop launcher
echo   info              System info show
goto end

:doStart
shift
java %JAVA_OPTS% %JAVA_GC% %JPDA_OPTS% %LAUNCHER_OPTS% -Djava.io.tmpdir=%LAUNCHER_TMPDIR% -classpath %CLASSPATH% %MAINCLASS% start
goto end

:doStop
shift
java %LAUNCHER_OPTS% -Djava.io.tmpdir=%LAUNCHER_TMPDIR% -classpath %CLASSPATH% %MAINCLASS% stop
goto end

:doInfo
shift
java %LAUNCHER_OPTS% -classpath %CLASSPATH% %MAINCLASS% info
goto end

:end