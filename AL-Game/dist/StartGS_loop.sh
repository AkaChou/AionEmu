#!/bin/bash

APP_HOME="$(cd "$(dirname "$0")/.." && pwd)"
cd "$APP_HOME" || exit 1

err=1
until [ $err == 0 ];
do
	[ -d log/ ] || mkdir log/
	[ -d log/backup/ ] || mkdir -p log/backup/
	[ -f log/console.log ] && mv log/console.log "log/backup/`date +%Y-%m-%d_%H-%M-%S`_console.log"
	java -Xms4096m -Xmx8192m -ea -javaagent:./lib/al-commons-1.0-SNAPSHOT.jar -cp "./lib/*" com.aionemu.gameserver.GameServer > log/console.log 2>&1
	err=$?
	gspid=$!
	echo ${gspid} > gameserver.pid
	sleep 10
done
