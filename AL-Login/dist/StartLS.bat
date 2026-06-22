@ECHO off
TITLE Aion 5.8 - Login Emu Console
@COLOR 1A
SET PATH="..\JavaJDK_8\bin"
SET APP_HOME=%~dp0..
CD /D "%APP_HOME%"

:START
CLS
echo.

echo Starting Aion 5.8 - Login Emu.
echo.
REM -------------------------------------
REM Default parameters for a basic server.
java -Xms64m -Xmx256m -server -cp ".\lib\*" com.aionemu.loginserver.LoginServer
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
