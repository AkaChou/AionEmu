#!/bin/sh

APP_HOME="$(cd "$(dirname "$0")/.." && pwd)"
cd "$APP_HOME" || exit 1

err=1
until [ $err == 0 ];
do

	java -Xms8m -Xmx32m -ea -cp "./lib/*" com.aionemu.loginserver.LoginServer
	err=$?
	lspid=$!
	echo ${lspid} > loginserver.pid
	echo "LoginServer started!"
	sleep 10
done
