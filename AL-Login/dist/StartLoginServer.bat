@ECHO off
TITLE Aion German - Login Server Console
REM SET PATH="C:\Program Files\Java\jre1.8.0_121\bin"
REM SET PATH="Type here your path to java jdk/jre (including bin folder)."
REM NOTE: Remove tag REM from previous line.
SET APP_HOME=%~dp0..
CD /D "%APP_HOME%"

:start
CLS
echo.

echo Starting Aion German Login Server.
echo.
REM -------------------------------------
REM Default parameters for a basic server.
java -Xms8m -Xmx32m -server -cp ".\lib\*" com.aionemu.loginserver.LoginServer
REM
REM -------------------------------------

SET CLASSPATH=%OLDCLASSPATH%


if ERRORLEVEL 1 goto error
goto end
:error
echo.
echo Login Server Terminated Abnormaly, Please Verify Your Files.
echo.
:end
echo.
echo Login Server Terminated.
echo.
pause
