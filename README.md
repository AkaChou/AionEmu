# AionEmu

[中文](README.zh-CN.md)

Aion 5.8 community server. Single Maven project, JDK 25, Spring Boot starts login / game / chat.

Entry: `com.aionemu.AionBootApplication`<br>
Defaults: `src/main/resources/application.yml`<br>
Source config & data: `src/main/resources/aion`<br>
Runtime: `aion/` (`aion.home`; local config/logs stay out of source)

## Build & Run

```bash
./package.sh                 # build and deploy to aion/
./aion/start-silent.sh
tail -f aion/log/aionemu.log
./aion/stop-silent.sh        # stop
# ./aion/shutdown.sh         # graceful shutdown
```

```bash
mvn package                  # or: mvn test
./package.sh test            # pass-through Maven goals
./re-package.sh              # redeploy, keep existing config
```

```bash
AION_HOME=/path/to/runtime ./aion/start-silent.sh
AION_HEAP_OPTS="-Xms2g -Xmx10g" ./aion/start-silent.sh
./aion/start-silent.sh -c    # clean runtime data (keep JAR/scripts)
```

## Credits

Based on Aion 5.8 Community Emulator and earlier community work. Parts also reference [Beyond Aion](https://github.com/beyond-aion/aion-server) (Aion 4.8, GPL-3.0).

Thanks to Aion-Lightning, Encom, [Beyond Aion](https://github.com/beyond-aion/aion-server), and the many community contributors behind earlier Aion server work.
