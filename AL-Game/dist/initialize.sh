echo Initializing java patch...

APP_HOME="$(cd "$(dirname "$0")/.." && pwd)"
cd "$APP_HOME" || exit 1

JAVA_HOME=/home/user_name/jdk1.7.0_XX
export JAVA_HOME
PATH=$PATH:$JAVA_HOME/bin
export PATH

echo Please wait 3 seconds...
sleep 3;

echo Gameserver is starting now...
./bin/StartGS.sh
