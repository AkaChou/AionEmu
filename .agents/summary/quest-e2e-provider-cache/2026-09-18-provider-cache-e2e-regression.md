# 2026-09-18 E2E Journey NO_MATCH 根因：引擎解析缓存钉住旧实例

## 结论（根因）

`QuestE2eInfrastructureTest` / `QuestProductionJourneyTest` 的 14 项 NO_MATCH、handled=false 与
INVALID_DIALOG_PACKET 并非任务 XML 或路由数据缺陷，而是 `GameEngineServices.questEngine()` 的
**解析缓存未与 provider 绑定**：

- `f6f1dbc51 perf(lifecycle): cache resolved Spring facade singletons` 为热路径引入静态缓存
  `resolvedQuestEngine`，只要解析到实例就永久返回它。
- 测试夹具 `QuestProtocolLoop` 通过反射换装 `GameEngineServices.questEngineProvider`，并在关闭时还原；
  但访问器始终命中第一次解析出的引擎。
- 于是同一 JVM 内第二个 `QuestProtocolLoop` 起，`ProtocolNpcController` 拿到的是上一个已关闭场景的
  `QuestEngine`（其 dispatcher 指向已关闭 runtime 或处于 disabled 状态），真实 `CM_DIALOG_SELECT`
  返回 false、无 metrics 结论 → `conclusiveResultSince` 返回 UNKNOWN → `attributeProtocolResult`
  回退为“候选可行 transition”，表现即 `status=NO_MATCH`（handled=false，candidates 非 0）。
- 单独运行用例时不会发生，因此此前被误判为“历史 fixture 限制”。

## 复现与反证

1. 单跑一例即绿：`mvn -o test -Dtest='QuestProductionJourneyTest#plansAndExecutesQuest1913FromProductionXml'` → 通过。
2. 走查缓存语义：`GameEngineServices.questEngine()` 先读 `resolvedQuestEngine`，命中即返回，与
   `questEngineProvider` 是否被替换无关。
3. 另有一层叠加因素：并行生命周期改造已退役 `QuestEngine.getInstance()` 静态兜底（fail-fast），
   因此旧测试里“在换装 provider 之前先取一次引擎”的写法会直接抛
   `IllegalStateException: QuestEngine 未由 Spring 提供`。

## 修复

- `GameEngineServices`：把解析缓存改为 **provider 绑定** 的 `ResolvedEngine<T>(provider, engine)`，
  provider 身份变化即视为缓存失效；同一 provider 重复解析仍然零额外分配，保留原有热路径收益。
  同一不变式同步应用到 quest / skill / instance / ai2 / chatProcessor 五个访问器。
- 构造函数（容器重建）显式清空五项缓存；`destroy()` 语义不变。
- `QuestE2eInfrastructureTest.protocolLoopExecutesRealDialogPacketAndRestoresEngineProvider`：
  改用反射比较 provider 字段（不再依赖已退役的静态兜底），并断言换装后访问器必须返回夹具引擎，
  即缓存已失效；循环关闭后断言 provider 已还原。

## 验证证据（本机，2026-09-18）

| 范围 | 结果 |
|---|---|
| `QuestE2eInfrastructureTest`（41）+ `QuestProductionJourneyTest`（10） | 51 例全绿 |
| e2e / lifecycle 联合批次 | 84 例全绿 |
| `com.aionemu.gameserver.lifecycle.**.*Test` | 全绿（含 LegacySingletonFallbackAuditTest、GameEnginesRuntimeBridgeTest、GameServiceProviderCompatibilityTest） |
| questEngine 全包 `-Dtest='com.aionemu.gameserver.questEngine.**.*Test'` | 1438 例：**10 Failures / 0 Errors**（改造审计基线为 96 项失败/错误） |

剩余 10 项失败与本次 E2E 收敛无关，已单独分诊：

**A. 陈旧断言形状（击杀链已由多节点链改为 KillNpcSet + var1 计数器，5 项）**
- 13765:40 / 19636:39 / 19640:39 —— 只统计 `QuestEvent.KillNpc`，现代 XML 使用
  `<kill-npc npc-ids="..."/>`（KillNpcSet），故计数为 0。
- A03Shard:117（23920）—— 断言“击杀转移的 distinct source 节点数 = 10”，计数器架构下只有 1 个源节点。
- QuestEventShard:89（50073）—— 同样只统计 `KillNpc`，现代 XML 为 KillNpcSet。

**B. 门禁口径需与既有决策对齐（2 项）**
- 25640:23 / 25698:23 —— 断言 `started` 下不存在 QUEST_SELECT 路由；但现代 XML 有“满计数恢复路线”
  （`variable-at-least var1 30/…` 门控的 started→reward），该路线已在
  `QuestPrematureRewardRouteExclusionTest` 中被确认为合法（门禁应校验计数条件，而不是禁止存在）。

**C. 待真端/客户端证据裁决（3 项）**
- 2677：repeatable（max-repeat 255）任务的 `complete -> complete` 开局对话缺少 `StartEligible` 条件，
  `QuestDefinitionCatalogManifestTest.assertRepeatStartDialogs` 判定重复接取路线不完整。
- 26930：`quest26930UsesTheSimpleItemCheckForCollectionTurnIn` 与当前简单道具检查形状不符。
- 13841（此前一版为 13849）：`action=108` 的 targetless completion 路线缺失。
  （13841/13849 在同一并行修改中变化，需固定 HEAD 后复核。）

## 遗留发现（未在本批修改）

同一“解析缓存不随 provider 失效”的写法仍存在于其它生命周期门面，例如
`GameFeatureServices`（resolvedSiegeService/resolvedBaseService/...）、`GameWorldBootstrapServices`、
`GameServerNetworkServices`、`GameWorldServices`、`GameEventServices`、`GameCronServices`、
`GameHousingServices` 等同族文件。
单容器生产运行下不触发；但任何 provider 换装（测试夹具、容器重建、脚本重载）都会读到旧实例。
建议按本批相同方式统一收敛，需与并行中的生命周期重构协调后实施。

## 边界

- 未启动/停止/重启任何服务端进程。
- 未创建 git worktree；未提交原始中间日志。
