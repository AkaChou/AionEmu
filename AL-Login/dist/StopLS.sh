#!/bin/sh
APP_HOME="$(cd "$(dirname "$0")/.." && pwd)"
cd "$APP_HOME" || exit 1

if [ -e loginserver.pid ]
then
  lspid=`cat loginserver.pid`
  kill ${lspid}
  echo "LoginServer stop signal sent."
else
  echo "LoginServer is not running."
fi
