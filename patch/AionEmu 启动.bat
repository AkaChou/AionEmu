cd /d "%~dp0"
powershell -Command "(gc SystemOptionGraphics.cfg) -replace 'FULLSCREEN_WIDTH', ' ' | Out-File -encoding ASCII SystemOptionGraphics.cfg"

@echo off
echo Starting [AionEmu5.8S]Server...

start /affinity 7FFFFFFF "" "bin64\AION.exe" -ip:127.0.0.1 -port:2106 -cc:5 -lang:CHS -loginex -disable-ip-fix -vip -f2p -aiontv
