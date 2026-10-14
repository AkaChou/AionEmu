@echo off
chcp 65001 >nul
setlocal
echo 部署并启动 AionEmu + MySQL（窗口保持；按 Ctrl+C 或关窗同时停止两者）/ deploying and starting AionEmu + MySQL (window held; Ctrl+C or close stops both)
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0server.ps1" deploy
set "code=%ERRORLEVEL%"
if not "%code%"=="0" (
  echo.
  echo 部署失败，错误码 %code% / deployment failed with exit code %code%
)
echo.
pause
exit /b %code%
