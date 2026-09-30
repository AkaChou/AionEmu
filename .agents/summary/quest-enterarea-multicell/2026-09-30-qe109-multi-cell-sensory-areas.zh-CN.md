# QE-109：真端多胞感官区（一个区名 = 多个多边形胞）与 4 行 owner 翻转

> 用户 Goal（2026-09-30）：「摒弃当前的任务引擎，完全按真端来，当前任务引擎只负责现有的任务 XML 运行。」
> QE-108 收尾时 `RETAIL_ENTERAREA_ZONE_UNRESOLVED` 桶还剩 4 行；wave8 曾把它们判为「多胞别名，无据不猜」。
> 本片把**多胞**做成引擎能力：一条 `<zone>` 可以声明多个 `<points>` 环，进入任一胞即算进入该区。

## 1. 判据（真端世界文件的全部胞，逐胞登记）

1. **发放/推进权威 = 真端世界文件的感官区胞**：`<name><别名></name>` + `<sensory_area><points>…</points><top>…</top><bottom>…</bottom></sensory_area>`。
   同一别名在该图里出现几个胞，就要登记几个胞——只登记一个会让其余胞成为**静默死边**
   （wave8 的留拒理由正是这个）。
2. **区名惯例不变**：`别名大写 + _<mapid>`（与 wave8/QE-107 的单胞登记同源），`mapid` 由
   `aion/definitions/compact/id-mappings.xml` 的世界目录名换算。
3. **每胞保留自己的 top/bottom**：把多胞压成一个全局 Z 区间会放行错胞的高度（例：`Q15322b` 三胞的 Z 窗口分别是
   295.5–315.5 / 283.6–303.6 / 228.4–248.4）。
4. **同批几何归一**：早期 `a2306c8e2` 用「感官区 NPC 出生点 + r=10 球体」近似注册的 4 个目标区
   （16987/26987/30722/30772）按真端 `<sensory_area>` 环改回 `POLYGON`（区名不变、解析表不变）。

## 2. 引擎改动（本片生产代码）

| 文件 | 改动 |
|---|---|
| `model/geometry/MultiPolyArea.java`（新） | 多胞区域：`isInside3D` **逐胞**判定（XY 与 Z 必须同胞，不能把 A 胞的 XY 与 B 胞的 Z 拼起来）；距离/最近点取各胞最小；`intersectsRectangle` 任一胞相交即真 |
| `model/templates/zone/ZoneTemplate.java` | `points` 由单个改为**环列表**（`List<Points>`，惰性初始化），并提供显式 `getPoints()` |
| `dataholders/ZoneData.java` | POLYGON 分支按环数分流：单环 → `PolyArea`（行为不变）；多环 → `MultiPolyArea`；无环 fail-closed（`IllegalStateException`，原来会 NPE） |
| `model/templates/zone/WorldZoneTemplate.java`、`world/zone/ZoneService.java` | 跟随环列表形（世界大区固定单环） |
| `data/static_data/zones/zones.xsd` | `<points>` 由 `maxOccurs="1"` 改 `unbounded`（多胞声明进入 schema） |
| `data/static_data/zones/zones_quest.xml` | 新增 11 条多胞区（1×6 胞 + 10×3 胞）；4 条早期球体近似改为真端多边形 |
| `quest/quest_enterarea_zone_resolution.tsv`（双副本） | 新增 12 行别名解析（13962/23962 共享 `IDEternity_War_ShugoSeller`；15322/25322 各 5 个别名） |

常设门 `MultiCellSensoryZoneRegistrationTest`（新增，5 例）：zones XML 过 schema；6 胞区**任一胞**算在区内、区外不算；
`Q15322b` 的**错胞 Z 不得借用**（cell 0 的 XY + cell 2 的 Z ⇒ false）；5×2 别名齐备；4 条球体近似行必须是真端多边形
（用真端 Z 窗口顶沿取样，旧 r=10 球体会漏判）。

## 3. 效果（4 行翻转，逐行可复核）

`13962 23962 15322 25322`：`REJECTED:RETAIL_ENTERAREA_ZONE_UNRESOLVED` → `ADOPTED` → `RETAIL_TABLE`
（retention 双副本 + XML 删除 + 目录登记行删除 + drift/指纹同片）。DD 分类桶：`ADOPTED` 1459 → 1463，
`RETAIL_ENTERAREA_ZONE_UNRESOLVED` **4 → 0**。

| quest | 真端别名（胞数） | world/mapid | shared / onlyXml / onlyRetail |
|---|---|---|---|
| 13962 | `IDEternity_War_ShugoSeller`(6) | IDEternity_War / 302350000 | 27 / 4 / 4 |
| 23962 | 同上（镜像） | 同上 | 25 / 2 / 6 |
| 15322 | `DF5_SensoryArea_65_Deva_Q15322{b,d,f,h,j}`(各 3) | df5 / 220080000 | 22 / 13 / 124 |
| 25322 | `LF5_SensoryArea_65_Deva_Q25322{b,d,f,h,j}`(各 3) | LF5 / 210070000 | 22 / 13 / 120 |

**4/4 行共享边非零**（无零共享红线）。

## 4. 翻转证据（QE-104 生产路径对拍）

方法（`probe/`，与 QE-106/107/108 同规格）：A = 现行 owner（XML 在场，`ProductionQuestDefinitions.definitionInOverlay(id)`）；
B = 打开多胞区登记 + 把这 4 行翻成 `RETAIL_TABLE` 并移走 XML + 删目录登记行后，同一入口再 dump。

差异面逐条定性（**没有语义边被静默删除**）：

1. **推进边换形**：13962/23962 的 `started --EnterZone[IDETERNITY_WAR_SHUGOSELLER_302350000]` 由真端别名解析而来
   （旧壳是 `AtDistance(835385) → SetStatus(REWARD)` 的邻近捷径，23962 侧直接退场）；15322/25322 的
   `started --EnterZone[...] → s1` 用多胞区名取代旧壳的 4 个大区名（WASHRUN_STRETCH / AURELIAN_TIMBERS /
   BONECREAK_VALLEY / THE_BLOOD_GRAINS 与 CRIMSON_HILLS / TWILIGHT_TEMPLE / PERENNIAL_MOSSWOOD /
   DRAGON_LORDS_CENTERPIECE）。
2. **分段数按真端**：15322/25322 由旧壳的 4 段（4 次进区 + 4 段各 10 杀，`KillNpcSet` 单边）改为真端 5 段
   （5 次进区 + 5 段各 10 杀），击杀集合按真端逐 NPC 展开成 `KillNpc` 边（节点 11 → 13：
   新增 `s8`、`s9`，`reward` 投影携带末段计数 `var1=10`）。
3. **接取形**：两族接取 NPC 不变（15322 = 805330、25322 = 805342），但由遗留的 `AtDistance`（走到附近即接）
   换成真端客户端受理手势 `TalkToNpc(npc, QUEST_ACCEPT_SIMPLE=20000)`；13962/23962 的接取 NPC 也保持不变。
4. **领奖窗**：补真端 `reward --QuestDialog(108)` 全局自动领取入口（旧壳只有逐 NPC 的 `SELECT_QUEST_REWARD`）。

临时翻转已还原（`git status` 复核）；A/B dump、比值与探针脚本留在 `probe/`。

## 5. 连带测试修订（同片，属「遗留壳合同」类）

| 测试 | 原断言 | 修订 |
|---|---|---|
| `QuestEnterZoneStartOwnerRegressionTest` | 15322/25322 的接取 owner = `AtDistance(...)` | 改成真端受理形 `TalkToNpc(同一 NPC, QUEST_ACCEPT_SIMPLE)`，并注明壳形退役 |
| `QuestResidualCounterLocksTest#quests15322And25322StageEdgesResetCountersWithoutOverIncrement` | 读 XML 文件 + 恰 4 条清零边 | 改走生产视图；按**源节点**聚合：清零段 = `{s1,s3,s5,s7}`（末段 s9→reward 只推进 var0，领奖投影携带末段计数） |
| `MigratedQuestRepairDefinitionTest#infiltrationOwnersResetTheCounterAfterEachTenKillStage` | `KillNpcSet` + 4 条完成边 | 按源节点聚合到真端 5 段（`{s1..s9}`）；中段完成边清零 var1、末段进 reward；完成边一律不得自增 var1 |

## 6. 真机验收清单（用户执行）

1. **13962 / 23962**（IDEternity_War「暗地交易商人」）：接取后走进 6 个感官胞**任意一个**（例：992.99, 1015.21、
   1062.61, 956.73、787.44, 925.41、696.51, 414.10、739.61, 719.31、741.60, 974.54）都应推进任务书；
   只站在胞外不应推进；领奖窗 108 唯一。
2. **15322 / 25322**（Enshar/Cygnea「渗透侦察」5 段链）：每段走进对应感官区（每区别 3 个胞，任一胞均可）后杀 10 只
   目标怪推进；第 5 段杀满进领奖；未进区不应计数推进。
3. **16987 / 26987 / 30722 / 30772**：走进真端多边形（含顶沿高度带，例 Q30722 z≈330）应推进——旧 r=10 球体会漏判。
4. 旧存档自愈：停在中间段的存档登录后按真端分段继续。

## 7. 门禁与验证

- `RetailDataDrivenGateTest`：漂移登记与冻结指纹与实现同源（新翻转 4 行指纹入库；**80817 的既存漂移刻意不重冻**）。
- `RetailEnterAreaZoneRegistrationGateTest`（解析表目标区名必须已登记）、`RetailSimpleTalkGateTest`、
  `RetailSimpleItemPlayGateTest`、`RetailOwnershipGateTest`、`RetailTsvManifestGateTest`、
  `MultiCellSensoryZoneRegistrationTest`（新）、`ProductionCatalogWhitelistVerificationTest` —— 绿。
- 区域/静态数据侧：`XmlDataLoaderTest`（24）、`DataManagerTest`（3）、`DataholderLookupIndexTest`（9）、`StaticDataTest`、
  `ZoneNameConcurrencyTest`（2）、`RetailAiDefinitionLoaderTest`（6）、`MultiCellSensoryZoneRegistrationTest`（5）
  —— **50 例全绿**（`mvn -o -Dtest=… test`）。
- `run_quest_gates.sh T1`：81 例，2 条**既存红**（80817 指纹、封顶登记行）。
- `run_quest_gates.sh T3`：1856 例，红身份集 **198 = 与基线逐行相等**，对 `target/agent-logs/qe107b/t3.ids`
  **ADDED 0 / REMOVED 0**（定稿日志 `target/agent-logs/qe109c/T3-221829.log`；本片第一次 T3 曾新增 3 个红，
  已按第 5 节随片修订，修订后归零）。
- `verify_retirement.py`：`catalog=746 directory=746 retired=5478 sum=6224 — OK`。

## 8. 既存红（本片未引入，也未顺手回写）

1. `RetailDataDrivenGateTest.frozenFingerprintsCoverExactlyTheRetiredQuests`：`80817` 冻结值与实际 IR 不一致，
   QE-102 起刻意保留。
2. `RetailNonIrAxisGateTest.cappedQuestsAreNeverRetailDriven`：26 条封顶登记行（HEAD 既存，属行为变更，需单独成片）。
3. `MigratedQuestRepairDefinitionTest` 的 3 例与 `QuestResidualCounterLocksTest#quest28504…`：HEAD 既存红。

## 9. 边界与未做

- 未做真机验收（第 6 节清单待用户执行）。
- 仍以「球体 r=10」注册的其它 `*SENSORYAREA*` 行（10503/10506/10507/1123/1336/… 与 `ID_ETERNITY_Q_SENSORYAREA_*`）
  未在本片普查内：本片只归一了 QE 轴已翻转/已采纳的 4 行（16987/26987/30722/30772），其余留待单独普查成片。
- 多胞区只支持**多边形胞**；球体/圆柱多胞形在真端数据里未见，按 fail-closed 不发明。
