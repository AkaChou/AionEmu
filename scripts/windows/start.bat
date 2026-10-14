@echo off
chcp 65001 >nul
setlocal
echo 启动 AionEmu（前台，实时日志；按 Ctrl+C 或关闭窗口停止游戏）/ starting AionEmu (foreground, live logs; Ctrl+C or close stops the game)
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0server.ps1" start
set "code=%ERRORLEVEL%"
if not "%code%"=="0" (
  echo.
  echo 启动失败，错误码 %code% / start failed with exit code %code%
)
echo.
pause
exit /b %code%
