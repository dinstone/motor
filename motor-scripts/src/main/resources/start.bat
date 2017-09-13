@echo off

cd %~dp0
set "CURRENT_DIR=%cd%"
echo CURRENT_DIR = %CURRENT_DIR%

if exist "%CURRENT_DIR%\launcher.bat" goto okHome
echo "Can't find %CURRENT_DIR%\launcher.bat file"
goto end

:okHome
set "EXECUTABLE=%CURRENT_DIR%\launcher.bat"

call "%EXECUTABLE%" start

:end