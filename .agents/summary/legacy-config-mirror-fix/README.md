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

## 5. 验证边界与遗留风险

- 未启动/重启服务器，未做真实客户端验收；运行时结论来自配置解析探针 + 单元测试。
- **镜像目录与运行时目录可能不是同一棵树**：`AionServicePaths.configureConfig("aion.config.dir", ...)`
  在服务生命周期 Bean（ApplicationRunner 相位）里才写入系统属性，而 post-processor 更早执行；在 IDEA
  工作目录下它回退到 `src/main/resources/aion/config`，运行中的遗留加载器则读 `aion/config`。本次冲突键
  已被移出镜像，所以 `database.*` 不受影响；但**取值一致**的镜像键仍会由解析器压过运行时目录里的同键文件
  （例：只改 `aion/config/network/database.properties` 的密码，会被源树镜像的旧值覆盖）。若要彻底消除，
  需让解析器只承载外部覆盖（命令行/环境变量/application.yml），或让 `aion.config.dir` 在环境准备前就定好。
- `HousingBidService.executeTask`（`HousingBidService.java:324`）对 `house` 无判空，住宅数据没加载成功时
  必然 NPE；本次 DB 修复后不再触发，但该判空属独立健壮性问题。
