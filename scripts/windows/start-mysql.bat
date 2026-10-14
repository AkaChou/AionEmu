@echo off
chcp 65001 >nul
setlocal
echo 启动便携 MySQL（窗口保持；按 Ctrl+C 或关窗停止 MySQL）/ starting portable MySQL (window held; Ctrl+C or close stops MySQL)
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0server.ps1" start-mysql
set "code=%ERRORLEVEL%"
if not "%code%"=="0" (
  echo.
  echo 启动 MySQL 失败，错误码 %code% / start MySQL failed with exit code %code%
)
echo.
pause
exit /b %code%
