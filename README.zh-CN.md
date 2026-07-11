# AionEmu

[English](README.md)

Aion 5.8 社区服务端。单 Maven 工程，JDK 25，Spring Boot 启动 login / game / chat。

入口：`com.aionemu.AionBootApplication`<br>
默认配置：`src/main/resources/application.yml`<br>
源码配置与数据：`src/main/resources/aion`<br>
运行目录：`aion/`（`aion.home`，本地配置与日志与源码分离）

## 构建与运行

```bash
./package.sh                 # 构建并部署到 aion/
./aion/start-silent.sh
tail -f aion/log/aionemu.log
./aion/stop-silent.sh        # 停止
# ./aion/shutdown.sh         # 优雅关闭
```

```bash
mvn package                  # 或 mvn test
./package.sh test            # 透传 Maven 目标
./re-package.sh              # 更新部署，保留已有配置
```

```bash
AION_HOME=/path/to/runtime ./aion/start-silent.sh
AION_HEAP_OPTS="-Xms2g -Xmx10g" ./aion/start-silent.sh
./aion/start-silent.sh -c    # 清理运行数据（保留 JAR/脚本）
```

## 致谢

基于 Aion 5.8 Community Emulator 及更早社区工作。部分实现参考 [Beyond Aion](https://github.com/beyond-aion/aion-server)（Aion 4.8，GPL-3.0）。

感谢 Aion-Lightning、Encom、[Beyond Aion](https://github.com/beyond-aion/aion-server)，以及历代 Aion 服务端社区的众多贡献者。
