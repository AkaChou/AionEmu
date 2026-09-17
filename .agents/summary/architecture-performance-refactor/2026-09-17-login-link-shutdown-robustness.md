# 登录链路断开路径的健壮性修复（2026-09-17）

> 主题：嵌入式 login/game/chat 单进程架构下，游戏服↔登录服断开清理的两处缺陷。
> 入口：`GsConnection.onDisconnect()`、`PingPongThread`（SvStats 上报）。

## 一、侦察：先排除，再动手

| 候选 | 证据 | 判定 |
|---|---|---|
| 启动刷怪慢 | 交叉日志误导（新实例启动时旧实例仍在跑）；4 个干净实例实测 **25 / 15 / 17 / 16 秒**，刷怪已按 10 核并行（`SpawnEngine.spawnAll()` 一地图一任务） | ❌ 不是问题 |
| `getIfAvailable` 热路径违规（AR-001） | 608 处中 415 在 `gameserver/lifecycle`、123 在 `services`（装配期）；热路径仅 `AI2Engine:186`、`SkillEngine:144` 两处，且都是单例懒初始化 | ❌ 无存量违规 |
| `DataManager` 用线程名比较 | 该比较只出现在 `describeStaticDataThreads()` 的 stall 诊断转储；任务编译并发=3 有 JFR 依据（8 线程会饿死 `static-data-loader`） | ❌ 不是缺陷 |
| `SM_SYSTEM_MESSAGE` 24,797 行 | 纯静态消息工厂（无生成器、无 static init、无运行时热点） | ❌ 不动（大 diff、零收益） |
| **嵌入式 login/game 断开清理** | `log/error.log`：**3× NPE** + **2× `DAONotFoundException: SvStatsDAO`** + `ShutdownHook - 断开登录服务器时出错` | ✅ 真缺陷，本轮已修 |

## 二、缺陷 1：`GsConnection.onDisconnect()` 的 NPE 会吞掉账号解绑

- **证据**：`error.log` 三次 `NullPointerException: Cannot invoke "com.aionemu.loginserver.PingPongThread.closeMe()" because "this.pingThread" is null`。
- **根因**：`pingThread` 只在 `initialized()` 里创建；连接在初始化前被关闭、或同一连接的 owner/peer 各清理一遍时它仍为 null，而 `onDisconnect()` 第一行就解引用它。
- **影响**：NPE 抛出后，同一个 `onDisconnect()` 里的 `gameServerInfo.setConnection(null)`、`clearAccountsOnGameServer()`、`gameServerInfo = null` **全部被跳过** → 登录服可能残留"该游戏服仍在连接 + 账号绑定在该服"的状态（后续登录/切服可能异常）。
- **修法**：把字段取到局部变量并判空（保留 `Config.ENABLE_PINGPONG` 语义）。

## 三、缺陷 2：关机时 SvStats DAO 查询失败并中断清理

- **证据**：`error.log` 两次 `DAONotFoundException: 未实现类 SvStatsDAO 的 DAO`；`console.log` 中该 ERROR 紧跟在 `SpringApplicationShutdownHook`（game 侧）的"与登录服务器的连接已丢失"之后，随后是 `ShutdownHook - 断开登录服务器时出错`。
- **根因**：三个服务共用同一 JVM。登录服关闭流程会调用 `DAOManager.shutdown()` 清空按 ServiceContext 分片的 DAO 注册表；game 侧的断开清理可能在那之后才执行 `PingPongThread` 里的 SvStats 上报，于是 `DAOManager.getDAO(SvStatsDAO.class)` 抛异常。
- **旁证/为什么"清空注册表"是正确语义**：同一份 `loginserver/Shutdown.java:112` 早已用 `DAOManager.isInitialized()` 护栏，
  并且此前已经执行 `update_SvStats_All_Offline(0, 0)`（全服置离线）→ 丢失的只是单服离线更新，**不构成数据错误**，
  真正的代价是关机 ERROR 噪音 + `onDisconnect()` 的剩余清理被异常打断。
- **修法**：`PingPongThread` 增加两个包私有上报助手 `updateSvStatsOnline(...)` / `updateSvStatsOffline(...)`，统一加
  `SvStatsConfig.SVSTATS_ENABLE && DAOManager.isInitialized()` 护栏；三处调用点改走助手。顺带消除 `run()` 中三次
  `getGameServerInfo()` 重复读取，并给 `validateResponse()` 补了空判（与 `closeMe()` 既有的空判一致）。

## 四、测试与验证

- `PingPongThreadTest` +2 例：
  1. `svStatsUpdatesAreSkippedWhenTheDaoRegistryIsGone`——DAO 注册表消失时必须跳过（修复前此例抛 `DAONotFoundException`，是真正的回归闸门，并断言测试 JVM 未注册 DAO）；
  2. `svStatsUpdatesAreSkippedWhenTheFeatureIsDisabled`。
- 命令与结果：

```
mvn -Dtest=PingPongThreadTest,GameServerInfoTest,CM_GS_AUTHTest,GsServerPacketTest,LoginServerConnectionTest,
LoginStartupSequenceLifecycleTest,LoginNetworkServicesTest test
```
→ **BUILD SUCCESS，8 个测试类 22 例全绿**（含与 `GsServerPacketTest` 同名的 chatserver 类）。

- **尚未完成的验收（用户侧动作）**：下次正常关机后扫一遍 `log/error.log`，确认 `pingThread` NPE 与 `SvStatsDAO` 的
  `DAONotFoundException`、以及 `ShutdownHook - 断开登录服务器时出错` 三类日志不再出现；出现即回退本轮改动。

## 五、本次未做（记录原因，避免下轮重复侦察）

- AI 巨石类拆分（`AggressiveNpcAI2` 7,543 行、`RetailPatternAI2` 3,371 行）：属于并行会话改动面，且属高风险重构，非本轮目标。
- 寻路：按 12.26 收口结论不动（不降质量是硬约束）。
