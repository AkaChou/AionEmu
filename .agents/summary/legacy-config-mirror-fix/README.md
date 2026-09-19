# 遗留配置镜像跨服务覆盖修复（database.url 泄漏到游戏服）

日期：2026-09-19　范围：`src/main/java/com/aionemu/boot/config/AionLegacyPropertySourceEnvironmentPostProcessor.java`

## 1. 现象（用户提供的 IDEA 启动日志）

- 启动本身成功（login/chat/game 全部就绪），但 game 相位所有 DAO 查询都落在登录库上：
  - `09-18 23:48:28 ERROR [virtual-302] c.a.gameserver.dao.impl.PlayerDAO - 无法从 players 表获取 ID 列表`
  - MySQL 返回：`Table 'al_server_ls.player_minions' doesn't exist`
  - 级联：`InventoryDAO` / `PlayerRegisteredItemsDAO` / `LegionDAO` / `MailDAO` / `GuideDAO` / `HousesDAO`
    全部报"无法从 … 表获取 ID 列表"，随后 `HouseScriptsDAO` 加载失败、`HousesDAO`/`TownDAO`/
    `ServerVariablesDAO`/`AbyssLandingDAO`/`SeasonRankingDAO` 保存或加载失败。
- 期望：`player_minions` 属于 `al_server_gs`，游戏服连接池必须使用 `aion/config/network/database.properties`
  里的 `al_server_gs` URL。

## 2. 根因（不是 ServiceContext，也不是 DAO 注册表）

1. `AionLegacyPropertySourceEnvironmentPostProcessor` 把三个服务的遗留文件**拍平进同一个** PropertySource，
   原始键（未加 `aion.legacy.<service>.property.` 前缀）后加载者覆盖先加载者：
   载入顺序 game → login → chat，所以原始键 `database.url` 最终等于**登录服**的 `al_server_ls`。
2. `BootConfigSourceResolver` 发布的解析器就是 `environment::getProperty`，而 `ConfigurableProcessor`
   的取值顺序是「解析器 → 本地 `Properties[]` → `@Property` 默认值」，于是解析器给出的登录服 URL
   压过了游戏服自己文件里的 `al_server_gs`。
3. `DatabaseFactory.init()` 用 `DatabaseConfig.DATABASE_URL` 建 HikariCP 池，game 上下文的连接池因此指向
   `al_server_ls`，之后所有 game 表操作都报 "table doesn't exist"。

排除项（实测）：`LoginDAOClassProvider` 只注册 11 个登录服 DAO，不含 `PlayerMinionsDAO`；若真是
`ServiceContext.current()` 串成了 `login`，`IDFactory` 里的 `DAOManager.getDAO(PlayerMinionsDAO.class)`
会先抛 `DAONotFoundException`，而不是产生 SQL 错误。因此上下文与 DAO 注册表都是对的，错的只有 JDBC URL。
同理，登录服自己的查询全部正常（它的 URL 本来就该是 `al_server_ls`）。

## 3. 修复规则

镜像收集器 `LegacyPropertyCollector`（同一文件内的私有静态类）按服务身份（= 遗留前缀）跟踪每个原始键：

- 单服务内后加载文件覆盖先前值（保持原有 `mygs` / `myls` / `mycs` 覆盖语义）；
- 两个服务为同一原始键给出**不同**值时，该键被永久移出镜像，由各服务自己的遗留加载器读取自己的文件；
- 取值一致的键继续照常镜像（`gameserver.thread.*`、`svstats.*` 等 Bean 绑定路径不受影响）。

实际配置树里被移出镜像的键只有 `database.url` 一个：

```
$ python3 .agents/summary/legacy-config-mirror-fix/raw_key_conflicts.py src/main/resources/aion/config aion/config
DROPPED  database.url
         game:  jdbc:mysql://127.0.0.1:3306/al_server_gs?...
         login: jdbc:mysql://127.0.0.1:3306/al_server_ls?...
watched bean keys kept: ['gameserver.thread.basepoolsize', ..., 'svstats.enable_svstats']
```

## 4. 修复后验证（真实配置目录，未启动服务器）

```
$ java -cp "target/classes:$(cat target/probe-cp.txt)" \
    .agents/summary/legacy-config-mirror-fix/src/com/aionemu/boot/config/ProbeServiceDatabaseUrl.java \
    aion/config src/main/resources/aion/config
=== aion/config ===
mirror raw database.url        = null
game prefix database.url       = jdbc:mysql://127.0.0.1:3306/al_server_gs?...
login prefix database.url      = jdbc:mysql://127.0.0.1:3306/al_server_ls?...
game DatabaseConfig.DATABASE_URL = jdbc:mysql://127.0.0.1:3306/al_server_gs?...
login DatabaseConfig.DATABASE_URL = jdbc:mysql://127.0.0.1:3306/al_server_ls?...
（src/main/resources/aion/config 结果相同）
```

全量回归：`mvn -B -Dstyle.color=never test` → **3457 例 / 0 失败 / 0 错误 / 2 跳过 / BUILD SUCCESS**（基线 3456 例，新增 1 例即下述回归用例）。

单元测试：`AionLegacyPropertySourceEnvironmentPostProcessorTest#keepsEachServicesOwnDatabaseUrlWhenLoginAndGameFilesDisagree`
同时断言「镜像不发布冲突原始键」「两个服务的静态 `DatabaseConfig.DATABASE_URL` 各自落回本服务文件值」。
修复前该用例必然失败（`database.url` 原始键=登录服 URL）。

## 5. 运行时验收（2026-09-19，用户启动服务端 + 客户端）

修复前的失败启动（对照，`log/console.log:63016-63174`）：

```
09-19 00:05:20 ERROR [virtual-294] PlayerDAO - 无法从 players 表获取 ID 列表
09-19 00:05:20 ERROR [virtual-294] HousesDAO - 无法从 houses 表获取 ID 列表
09-19 00:05:20 java.sql.SQLSyntaxErrorException: Table 'al_server_ls.player_minions' doesn't exist
09-19 00:05:22 ERROR [world-spawner] HousesDAO - 无法从数据库恢复住宅数据
09-19 00:05:2x ERROR [main] HousesDAO - 无法保存住宅数据，住宅 ID：…
09-19 00:05:2x ERROR [main] TownDAO - 加载种族 ELYOS/ASMODIANS 的城镇失败 / 插入城镇失败：1003…
```

修复后的启动与客户端验证（`log/console.log` 08:53 / 08:55 两轮启动，08:55:55 客户端进入世界）：

- `doesn't exist` 计数：**0 条**（08:5x 之后）；8 个 ID 表错误、住宅/城镇/竞拍/服务器时间错误全部消失。
- `HousingService`：`正在加载住宅数据` → `住宅服务已加载` → 各图住宅生成计数（210050000:6 / 210040000:9 / 220040000:9 / 220070000:6 / 700010000:500 / 710010000:500）。
- `HOUSE_AUCTION_LOG`：`已添加 1 条新住宅竞拍`（修复前为 1030 条 + 逐条 `添加住宅竞拍失败` + `HousingBidService:324` NPE，本次未再出现 NPE）。
- `TownService`：`已加载 25 个天族城镇` / `已加载 25 个魔族城镇`（修复前为两组加载失败 + 50 条插入失败）。
- `GAMECONNECTION_LOG`：`09-19 08:55:55 玩家 Ww（账号 cc）进入世界，MAC 地址=6C-0B-5E-A4-1D-57`；`log/cm_login.log` 同日 `cc got authed state`。
- 08:5x 之后残留的 ERROR 共 6 条：4 条 `ChatCommand`（GM 命令文本为空，09:26/09:28/09:47）与 2 条 `KnownList - 对所有 NPC 运行访问器时异常`（09:32/09:43），与数据库连接无关。

## 6. 后续修复：配置目录来源按启动方式对齐（2026-09-19）

用户确认的约定：**本地 IDE 启动读取 `src/main/resources/aion/**`；打包启动
（`scripts/start-silent.sh`）读取 `<aion.home>/**`（即 `aion/config`）**。判别条件就是
`aion.home` 是否被设置——启动脚本第 79 行写入 `-Daion.home=$AION_HOME -Daion.log.dir=$LOG_DIR`。

- 修复前 `AionServicePaths.configureConfig` 不参与源码树判定：IDE 模式下 `aion.config.dir` 落到
  `aion/config`（部署目录），而 data/definitions/geo 早就走 `src/main/resources/aion/**`，同一个进程里
  出现"配置读部署目录、数据读源码树"的混搭；镜像（post-processor）则按源码树读取，于是同一份配置在
  同一个进程里可能来自两棵树。
- 修复：抽出 `configureCheckoutSourceDirectory`（仅在 `aion.home` 未设置且源码树存在时命中），
  `configureConfig` 与 `configureSourceResourceDirectory` 共用它；`aion.home` 已设置时配置目录仍是
  `<aion.home>/config`（由既有用例 `prefersRuntimeGameConfigDirectoryOverProjectResources` 继续守护），
  显式路径依然最高优先。
- 实测（`ProbeIdeConfigTree`，cwd=仓库根，无 `aion.home`/`aion.config.dir`）：

```
[PROBE] aion.home            = null
[PROBE] aion.config.dir      = src/main/resources/aion/config
[PROBE] aion.game.data.dir   = src/main/resources/aion/data
[PROBE] login DatabaseConfig.DATABASE_URL = jdbc:mysql://127.0.0.1:3306/al_server_ls?...
[PROBE] game DatabaseConfig.DATABASE_URL  = jdbc:mysql://127.0.0.1:3306/al_server_gs?...
```

- 测试：`AionServicePathsTest` 新增 `prefersCheckoutSourceConfigDirectoryWhenAionHomeIsDefault`（IDE 取源码树）
  与 `keepsExplicitDirectoriesEvenWhenHomeAndSourceTreeExist`（显式路径不被源码树覆盖）；boot/lifecycle 与配置
  聚焦套件 51 例全绿。
- 全量 `mvn -B test`：共 3469 例，仅 2 例 quest-engine 错误（`QuestTransition.sourceNode()` 为 null，属**并行
  会话**未提交的 quest 改动）与 1 例 `CreatureTest$TestCreature$1` NoClassDefFound（并发编译写入
  `target/test-classes` 造成的假失败，单独重跑 2/2 通过）；与本次路径改动相关的套件 0 失败。

## 7. 遗留风险（未纳入本次修复）

- 未做长时间运行观察；验收覆盖启动 + 登录 + 世界加载，未覆盖全部玩家系统。
- `src/main/resources/aion/config` 与部署目录 `aion/config` 的文件清单当前完全一致（84 个文件，无本地
  额外覆盖文件）；IDE 模式改读源码树后，若在 `aion/config` 放本地覆盖文件将不再被 IDE 运行读取。
- `HousingBidService.executeTask`（`HousingBidService.java:324`）对 `house` 无判空，住宅数据没加载成功时
  必然 NPE；本次 DB 修复后不再触发，但该判空属独立健壮性问题。
