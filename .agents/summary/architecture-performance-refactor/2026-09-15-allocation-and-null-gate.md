# 分配热点第二轮 + getMostPlayerDamage 判空闸门 / Allocation round 2 and the getMostPlayerDamage null gate

日期 / Date: 2026-09-15 · 分支 / Branch: `quest` · HEAD: `5e8e065c0` · 状态 / Status: **已实现并通过聚焦验证，未提交**
关联模式 / Related patterns: `AR-007`（新增）、`IR-008`

## 一、本轮目标

1. 把 `getMostPlayerDamage()` / `getMostPlayerDamageOfMembers()` 的判空要求升级为覆盖**全部消费点**的源码闸门。
2. 继续压缩游戏内分配热点（上一轮记录见 `2026-09-15-gameplay-jfr-hotspots.md` 第七节末的新 TOP 列表）。

## 二、JFR 定位（基线 `/tmp/play-2-after.jfr`，300s）

`jfr view allocation-by-site` + `jfr print --events jdk.ObjectAllocationSample --stack-depth 30` 后按“我方第一帧/调用链”聚合：

| 站点 | 采样分配 | 占比 | 说明 |
|---|---|---|---|
| `WorldMapInstance.getNpcs()` | **207.2 MB** | 30.1% | 调用链 `getNpcs ← lambda$broadcastMessage$0 ← execute ← run`；先复制整表快照、再扩容收集 NPC |
| `WorldMapInstance.worldMapObjectsSnapshot()` | 19.9 MB | 2.9% | 同一条广播链路上的第二份临时集合 |
| `java.util.KeyValueHolder`（全部） | **85.9 MB** | 12.5% | 全部来自 `QuestSnapshot` 的 `withXxx` → 紧凑构造器校验链，分配者是 `MapN$MapNIterator.next()` |
| `QuestStateList.getAllFinishedQuests()` | 5.2 MB | 0.8% | `completedQuestIdsOf` 每次物化的中间列表 |
| `PlayerQuestEventPort.toInventoryMap/completedQuestIdsOf` | 8.8 MB | 1.3% | 逐次 `new HashMap/HashSet` 且未预分配 |
| `Arrays.copyOf(Object[],int)`（合计） | 224 MB | 32.6% | 上述扩容/复制的共同底层 |

结论：**“物化整表 + 逐次复制校验”** 是本窗口最大的两块浪费，而不是单点算法问题。

## 三、已实施改动

| 文件 | 改动 |
|---|---|
| `WorldMapInstance` | 新增 `doOnAllNpcs(Visitor)`（锁内一次数组快照、锁外访问）+ `worldMapObjectsArray()`；`getNpcs()` 改为锁内单次预分配收集；`getPlayersInside()` 同样去掉“快照迭代器 + 结果列表”的双份分配 |
| `RetailPatternAI2.broadcastMessage` | 广播改走 `instance.doOnAllNpcs(...)`，不再为每条消息物化整张 NPC 列表；快照语义与锁顺序保持不变 |
| `QuestSnapshot`（紧凑构造器） | `completedQuestIds`/`activeQuestIds` 的 `stream().anyMatch` 改 `for`；`eventActivities`/`inventory`/`currencies` 的 `entrySet().stream().anyMatch` 改 `keySet()` + `get()` 循环（保持校验强度，去掉每条目的 `KeyValueHolder` 与 stream 管道对象） |
| `PlayerQuestEventPort` | `completedQuestIdsOf` 直接过滤 `getAllQuestState()` 视图（不再经由 `getAllFinishedQuests()` 中间列表）并按任务数预分配；`activeQuestIdsOf` 预分配；`toInventoryMap` 按条目数预分配；`currenciesOf` 固定小容量 |

语义边界：`WorldMapInstance` 仍使用 `LinkedHashMap` + `synchronized`（未换 `ConcurrentHashMap`），因此 `getNpc(int)` 的“先到先得”语义与迭代顺序不变。

## 四、判空闸门（新增 `GetMostPlayerDamageNullGateTest`）

- **规则**：`getMostPlayerDamage()` / `getMostPlayerDamageOfMembers()` 的消费点，在**首次真正使用**前必须判空。赋值形态检查“首次使用前是否存在 `x == null` / `x != null`”（内层同名重新声明即停止检查，避免把 `Visitor<Player>` 形参误判）；内联形态要求同语句判空或成对 null 容忍方法（`sendMovie`/`sendPacket`/`sendMessage`/`broadcastPacket`）。
- **规模自检**：至少扫描到 **90** 个消费点，否则失败（防止正则/路径失效后闸门空转）。当前实际扫描 93 个（90 赋值 + 3 内联）。
- **基线**：`KNOWN_UNGUARDED_SITES` 登记闸门落地时已存在的 10 处未判空站点（见下节），登记的是“每文件允许的违规数”，新增站点会超基线失败。
- **负向验证**：在 `HaramelInstance.onDie` 临时插入 `player.getObjectId();` → 闸门失败并点名该文件；还原后转绿。

## 五、闸门发现的 10 处既有未判空站点（2026-09-15 晚已处理）

| 文件 | 首次未判空使用 | 风险 |
|---|---|---|
| `FallenPoetaInstance` | `stopInstance(player)` | **低**：`stopInstance` 形参未被使用 |
| `DrakenseerLairInstance` | `stopDrakenseerLairTimer(player)` | **低**：形参未被使用 |
| `KumukiCaveInstance` | `stopInstance2(player)` | **低**：形参未被使用 |
| `TrialsOfEternityInstance` | `AbyssPointsService.addGp(player, 1200)` | **高**：NPE 会中断 onDie 后续逻辑 |
| `TalocsHollowInstance` | `player.getSummon()` / `ItemService.addItem(player, …)` | **高** |
| `MirashSanctuaryInstance` | `player.getSkillList()` / `spawn(…, player.getX(), …)` | **高** |
| `AturamSkyFortressInstance` | `sp(…, player.getX(), player.getY(), player.getZ(), …)` ×4 | **高** |
| `Event_AturamSkyFortressInstance` | 同上 ×4 | **高** |
| `IDEvent_Def_HInstance` | `ItemService.addItem(player, …)` / `player.getCommonData().addExp(...)` | **高** |
| `Event_ContaminatedUnderpathInstance` | 同上 | **高** |

修法涉及玩法语义（“跳过玩家奖励”还是“以 NPC 坐标回退刷怪”），闸门落地时仅登记基线、不改行为。
**后续（同日）已全部处理**：`TalocsHollowInstance` 因运行态 NPE（Queen Mosqua 死亡链）补齐判空，
其余 6 处按“无归属则跳过玩家奖励 / 回退到 NPC 坐标”统一修复；三条形参未被使用的 `stop*(player)` 调用
随后按独立清理删掉了未使用形参，因此 **`KNOWN_UNGUARDED_SITES` 现为空表**（`MIN_CALL_SITES` 90→85，实际 90 处消费点）。
同轮还发现闸门只校验“首次使用前判空”，据此复核出并修复了 Aturam 两个文件中被放行的 8 处 AP/GP 发放；
聚焦验证 `GetMostPlayerDamageNullGateTest,InstanceMovieNullGuardTest,ModelCollectionImplementationTest`
= 20 例 / 0 失败 / 0 错误（BUILD SUCCESS）。
证据与逐点决策见 `.agents/summary/talocs-hollow-mosqua-egg/2026-09-15-ondie-null-player-npe.md`。

## 六、验证 / Validation

| 项 | 命令 | 结果 |
|---|---|---|
| 聚焦回归 | `mvn test -Dtest='GetMostPlayerDamageNullGateTest,InstanceMovieNullGuardTest,PacketSendUtilityTest,WorldMapInstanceTest,WorldMapTest,KnownListTest,RetailPatternAI2Test,RetailNpcPartyEngineTest,QuestSnapshotCurrencyCaptureTest,QuestPlayerFactsConditionTest,QuestEventConditionTest,PlayerQuestInventoryPortTest,PlayerQuestRewardPortTest,PlayerQuestAiPortTest,PlayerQuestTeleportPortTest,PlayerQuestBroadcastPortTest,CompositeQuestActionPortTest,Quest1913ProductionFlowTest'` | **175 例 / 0 失败 / 0 错误**（BUILD SUCCESS，44s） |
| 闸门负向验证 | 插入未判空使用后运行闸门 | 按预期失败并点名文件 |
| 沉淀校验 | `sync_memory_bank.py` + `check_memory_bank.py` | `MEMORY_BANK_OK ROUTER_IDS=39 PATTERNS=39 SYMPTOM_INDEX=39` |

## 七、边界与后续

- **运行期收益已复测**（21:32 重启 + 300s，见 `2026-09-15-gameplay-jfr-hotspots.md` 第八节）：`getNpcs` 207.2MB→0、`worldMapObjectsSnapshot` 19.9MB→0、`getAllFinishedQuests` 5.2MB→0，采样分配总量 −55.8%（CPU 采样 237→272 同量级）。
- **更正**：`KeyValueHolder` 84.9MB→27.1MB **未归零**——`ImmutableCollections$MapN` 未覆写 `keySet()`/`forEach()`，改遍历方式不能消除该分配，需把逐次校验合并为一次（下一轮）。
- 10 处既有未判空站点：三条 `stop*` 形参未使用可判为安全；其余需要“跳过 vs 回退坐标”的玩法决策后单独提交。
- 其它仍未处理的分配源：`StreamSupport.stream`（1.78%，疑似 `Collection.stream()`）、`LinkedHashMap$LinkedValues.toArray()`（1.24%）、`Throwable.fillInStackTrace`（2.96%，疑似用异常做控制流）。

## 八、第三轮（针对复测暴露的新热点，2026-09-15 晚）

1. `TemporarySpawn`：XML 注入的 `时.日.月` 字符串改为首次使用解析并缓存——`isInSpawnTime()` 以前每次调用做 6 次 `String.split("\\.")`（`.` 非快路径，会走正则），JFR 实测 52.4MB/17.2%。新增 `TemporarySpawnTimeWindowTest`（解析正确性 + 缓存契约）。
2. `QuestSnapshot`：容器校验改为“入参已是不可变副本则跳过逐条目校验”，并把 `PlayerQuestEventPort` 的端口侧改回返回可变集合，保证**首建仍完整校验一次**；新增 `QuestSnapshotValidationTest` 锁定“可变入参被校验 + 结果被冻结”。
3. 聚焦验证：`TemporarySpawnTimeWindowTest`、`TemporarySpawnEngineTest`、`RetailOpenWorldSpawnDataTest`、任务运行时 13 个测试类、两个闸门共 **90 例全绿**；`QuestSnapshotValidationTest` + `TemporarySpawnTimeWindowTest` 单独复跑 7 例全绿。
4. 沉淀：新增 `AR-008`（模板字符串热路径重复解析），更新 `AR-007`（把“keySet 替代 entrySet”更正为“减少遍历次数 + 不可变副本识别”）。
