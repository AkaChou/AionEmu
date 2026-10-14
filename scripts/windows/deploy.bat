@echo off
chcp 65001 >nul
setlocal
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0server.ps1" deploy
set "code=%ERRORLEVEL%"
if not "%code%"=="0" (
  echo.
  echo 部署失败，错误码 %code% / deployment failed with exit code %code%
  pause
)
exit /b %code%
