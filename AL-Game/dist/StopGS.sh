#!/bin/bash
APP_HOME="$(cd "$(dirname "$0")/.." && pwd)"
cd "$APP_HOME" || exit 1

if [ -e gameserver.pid ]
then
  gspid=`cat gameserver.pid`
  kill ${gspid}
  echo "GameServer stop signal sent"
else
  echo "GameServer is not running."
fi
exit 0
