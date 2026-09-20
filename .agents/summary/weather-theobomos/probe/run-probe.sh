#!/usr/bin/env bash
# 重新编译 WeatherTable 并用真实 JAXB 反序列化 + 真实 WeatherService 私有方法反射调用，
# 验证「空天气表（weather_count=0）= 空列表而非 null」以及该图恒为 code 0。
#
# Recompiles WeatherTable and verifies, with real JAXB unmarshalling plus a reflective call into the
# real WeatherService, that an empty weather table yields an empty list instead of null and that the
# map permanently reports code 0.
#
# 用法 / Usage: bash .agents/summary/weather-theobomos/probe/run-probe.sh
set -euo pipefail

PROBE_DIR="$(cd "$(dirname "$0")" && pwd)"
REPO_ROOT="$(cd "$PROBE_DIR/../../../.." && pwd)"
M2="${HOME}/.m2/repository"
TMP_CLASSES="$(mktemp -d)"
trap 'rm -r "$TMP_CLASSES"' EXIT

CP="target/classes"
CP="$CP:$M2/jakarta/xml/bind/jakarta.xml.bind-api/4.0.2/jakarta.xml.bind-api-4.0.2.jar"
CP="$CP:$M2/org/glassfish/jaxb/jaxb-core/4.0.9/jaxb-core-4.0.9.jar"
CP="$CP:$M2/org/glassfish/jaxb/jaxb-runtime/4.0.9/jaxb-runtime-4.0.9.jar"
CP="$CP:$M2/org/glassfish/jaxb/txw2/4.0.9/txw2-4.0.9.jar"
CP="$CP:$M2/com/sun/istack/istack-commons-runtime/4.1.2/istack-commons-runtime-4.1.2.jar"
CP="$CP:$M2/jakarta/activation/jakarta.activation-api/2.1.4/jakarta.activation-api-2.1.4.jar"
CP="$CP:$M2/org/eclipse/angus/angus-activation/2.0.3/angus-activation-2.0.3.jar"
CP="$CP:$M2/org/springframework/spring-beans/7.0.8/spring-beans-7.0.8.jar"
CP="$CP:$M2/org/springframework/spring-core/7.0.8/spring-core-7.0.8.jar"
LOMBOK="$M2/org/projectlombok/lombok/1.18.46/lombok-1.18.46.jar"

cd "$REPO_ROOT"
javac -proc:full -nowarn \
	-cp "target/classes:$M2/jakarta/xml/bind/jakarta.xml.bind-api/4.0.2/jakarta.xml.bind-api-4.0.2.jar:$LOMBOK" \
	-processorpath "$LOMBOK" -d "$TMP_CLASSES" \
	src/main/java/com/aionemu/gameserver/model/templates/world/WeatherTable.java
jshell --class-path "$TMP_CLASSES:$CP" \
	-R-Dprobe.xml=src/main/resources/aion/data/static_data/weather_table.xml \
	"$PROBE_DIR/TheobomosWeatherProbe.jsh"
