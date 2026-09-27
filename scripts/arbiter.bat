@echo off
rem Starts Arbiter from the arbiter.jar beside this script, with Java 25 or later.
rem Uses %JAVA_HOME%\bin\java.exe if JAVA_HOME is set, otherwise the java command on the PATH.
rem Each error pauses, so its message stays on screen when the script was double-clicked.
setlocal
set "JAR=%~dp0arbiter.jar"
set "JAVA=java"
if defined JAVA_HOME set "JAVA=%JAVA_HOME%\bin\java.exe"

"%JAVA%" -version >/dev/null 2>&1
if errorlevel 1 goto noJava

set "VERSION="
for /f "tokens=1,2 delims== " %%a in ('""%JAVA%" -XshowSettings:properties -version 2>&1"') do if "%%a"=="java.specification.version" set "VERSION=%%b"
set "MAJOR=0"
if defined VERSION for /f "tokens=1 delims=." %%m in ("%VERSION%") do set "MAJOR=%%m"
if %MAJOR% LSS 25 goto oldJava
if not exist "%JAR%" goto noJar
"%JAVA%" -jar "%JAR%" %*
exit /b %ERRORLEVEL%

:noJava
echo Arbiter needs Java 25 or later, but no java command was found. Install Java 25 or set JAVA_HOME. 1>&2
pause
exit /b 1

:oldJava
echo Arbiter needs Java 25 or later, but "%JAVA%" is Java %VERSION%. Install Java 25 or set JAVA_HOME. 1>&2
pause
exit /b 1

:noJar
echo arbiter.jar is missing from "%~dp0". Keep this script beside arbiter.jar. 1>&2
pause
exit /b 1
