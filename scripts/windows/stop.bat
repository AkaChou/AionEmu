@echo off
chcp 65001 >nul
setlocal
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0server.ps1" stop
set "code=%ERRORLEVEL%"
if not "%code%"=="0" (
  echo.
  echo 停止失败，错误码 %code% / stop failed with exit code %code%
)
echo.
pause
exit /b %code%
