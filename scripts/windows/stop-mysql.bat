@echo off
chcp 65001 >nul
setlocal
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0server.ps1" stop-mysql
set "code=%ERRORLEVEL%"
if not "%code%"=="0" (
  echo.
  echo 停止 MySQL 失败，错误码 %code% / stop MySQL failed with exit code %code%
)
echo.
pause
exit /b %code%
