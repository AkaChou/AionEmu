# P7 步 2 步 e2 报告：动作面收全（TELEPORT/SPAWN/MESSAGE）× 真端执行矩阵 + con_quest 语义 + 挑战哨兵（零行为变更）

> **门禁产物清理提示（2026-10-03）**：本文件引用的门禁运行产物（`gates/*.log`、`*-red-classes.tsv`、`*-delta.tsv` 等）已按「只记录重要的过程内容、不记录门禁」口径清理，不再随仓保留；关键读数已内联于正文，复现请重跑对应聚焦套件，清理说明见 `.agents/summary/CLEANUP-LEDGER.zh-CN.md`。

- 批次：P7 DataDriven 步 2 步 e2（计划 §7「P7 DataDriven」/ §10.2 / §10.3-#24③；证据笔记
  `p7/P7-STEP2E2-PREREQ-ACTION-FACES.zh-CN.md`）
- 日期：2026-10-02（承接步 e1 的 2026-10-02 收口，提交 fd574e347）
- 结论：按宿主反编译源（`server58-source/NPCServer_NPCSvr64`/`MainServer_Server64`）逐函数坐实剩余
  附加动作的宿主渲染面，落面 **TELEPORT（case 3）/ SPAWN（case 5）/ MESSAGE（case 7）**；同批复核出
  **动作执行矩阵**并纠正 e1 的一处执行时机偏差（推进边只对 Hunt/EnterArea/TalkFOBJ 调执行器——对话平面
  与 ItemPlay/EnterWorld 的推进边零执行器调用）；坐实 `con_quest`/`con_quest_list` = 任务目录显示元数据
  **非接取闸门**（ACQUIRE_CONDITION_UNFACED 35 行解冻）；挑战接取哨兵 `_challengetask_` 按 P0c-58 四源
  裁定落面（6 行解冻）。生产口径**路由集仍为空**（零行为变更）。分桶 e1（1384/83）→ e2
  **可路由 1444 / 冻结 23**（ZONE_ABSENT 9 + ACTION_UNFACED 14）。

## 1. 真端事实（本次逐函数，全文见证据笔记）

### 1.1 动作执行矩阵（逐 handler 复核，**纠正 e1**）

| 进度 kind | 推进边执行动作 | 证据 |
|---|---|---|
| Hunt(2) | **是**（`FUN_180c4d190`） | 步 d 已证 |
| CollectItem(1) / Talk(4) | **否**——对话平面 `FUN_180c474b0` 推进/完成只发 `+0xf0`/`+0x100`，全文无执行器调用（C:2071069-2071274） |
| ItemPlay(3) / EnterWorld(7) | **否**——推进边只发 0xf0/0x100；尾部 cd50 只在**接取分支**（C:2070479 / C:2070878） |
| EnterArea(6) / TalkFOBJ(9) | **是**（`FUN_180c4c8d0`，C:2071475 / C:2071395） |

接取侧执行步 0 动作：ItemPlay/EnterWorld/LevelUp·LevelUpLogIn（cd50，`FUN_180c46bb0` 尾部
`(+0xd8)` 成功 → cd50）、EnterArea（c8d0）；**Talk 对话接取（1002/20000）= 收尾 helper，不执行动作**。
⇒ e1 的 apply() 曾对全部 kind 在推进边跑动作（偏真端多执行）；e2 修正为 `kind ∈ {Hunt, EnterArea,
TalkFOBJ}` 才执行；接取分支维持全 kind 执行。登记：Talk 步动作列在真端**报告通道**（mgr+0x1b0）侧
是否有执行面未坐实（步 f 前闭合；e2 镜像 = 进度 handler 家族证据，零执行）。

### 1.2 剩余 case 宿主渲染面

| case | 宿主面（源实证） | 落面 |
|---|---|---|
| 3 Teleport | `IUserImp::Teleport(world, x, y, z+1, heading(度), 1)` | **是**：`NativeTeleportPort` → `TeleportService2.teleportTo`（z+1，deg/3 转 6 位朝向） |
| 5 Spawn | `IUserImp::Spawn`（+0x180 槽名本次定名）：Relative = 玩家位置随机可行走点半径 5 / Absolute = 精确坐标 × count；time = 刷怪单存活 | **是**：`NativeSpawnPort` → `SpawnEngine.addNewSingleTimeSpawn` + 定时回收（偏差：可行走校验随步 f 激活批坐实） |
| 7 Message | `IUserImp::Say`：NC_SAY_CODE（说话者 = 玩家，文本 = 字符串表 id；装载期 `XML_ParseStringIndex` 解析 `STR_` 键） | **是**：键→id 表 `retail-quest-string-ids.tsv`（真端 `Map/XML/strings.xml` 逐键提取 7 键全命中；先例 = 生产在用 `quest_name_string_ids.tsv`）+ `NativeSayPort`（偏差：本服以 `SM_SYSTEM_MESSAGE(id)` 系统消息通道转发同一 id，客户端自解文本） |
| 9 EnterInstance | `IUserImp::EnterInstance`：仅发客户端包 `{objId, creationId}`；creationId→世界映射在客户端表 | **否**（ACTION_UNFACED 保持） |
| 10 Timer | `IUserImp::AddQuestTimer(ms, ctx)`；`vec[1]==1` = 可见倒计时；到期分发面未坐实 | **否**（ACTION_UNFACED 保持） |
| 6 Delay / 8 Message-8 | def+0x270 / def+0x3a0（`IOneQuestScriptNpc` 虚槽）未定名；**只在 EnterArea/TalkFOBJ 步活**（其余 kind 真端执行器无该 case = 装载即死列） | c8d0 步冻结（10035/25606 两行）；其余 kind 镜像忽略 |

数字 token = `wcstoul`（前导整数截断 `83.9`→83，`FUN_18107c0f0` = `FUN_181084c74(_,0,10)`）。

### 1.3 con_quest / con_quest_list（ACQUIRE_CONDITION_UNFACED 桶裁定）

- 解析：`con_quest` → 原文存 def+0x38；`con_quest_list` → 字符串键→id 存 def+0x3c
  （"Data Driven - ZoneQuestList"）。
- **0x640 接取条件表只收字面量条目**（{0,-1}/{4,-1}/{3,0}），con_quest 两列从不进条件表；
  def+0x3c 唯一消费方 = `FUN_180c46590`（def+0x198 元数据槽）→ 区域任务列表注册。
- **裁定**：显示元数据非闸门 ⇒ 35 行解冻；显示注册面归 §10.3-#18（附近任务/目录轴）。

### 1.4 挑战接取哨兵

6 行（17160-17162/27160-27162）`acquire=Talk` + `_challengetask_` + reward_npc_name
（`LF5_Atmos_E`/`DF5_Haldor_E` 唯一可解析）⇒ 哨兵面 = `AcquirePlan(kind 4, 接取 NPC = reward_npc_name)`，
对话词汇与普通 Talk 接取同路（P0c-58 四源：客户端 npc 块 / 入口页 4762 / 按钮 20000/20001 /
遗留 NPC_START+npc-complete）。

## 2. 分桶与冻结面（本批口径）

- 切换集 1467 行 → **可路由 1444 / 冻结 23**：
  - `ZONE_ABSENT` **9**（§10.3-#23，维持）；
  - `ACTION_UNFACED` **14**（携带 col9 EnterInstance〔3 行〕/ col10 Timer〔9 行〕/ c8d0 步 col6
    〔10035、25606〕——宿主渲染面未坐实，fail-closed 保持）；
  - `NAME_UNRESOLVED` **0**、`ACQUIRE_CONDITION_UNFACED` **0**（桶撤销）。
- 已路由行逐类步数：hunt **805** / collectitem **341** / pvp **207** / talk **352** / enterarea **125** /
  itemplay **36** / enterworld **31** / talkfobj **12**（e2 离线镜像
  `p7/tools/dd-planrow-e2-mirror.py` 与 Java 实测逐值一致）；切换集逐类步数与 P7 步 1 契约一致。
- 接取计划直方图（已路由行）：talk **1126**（含挑战哨兵 6 行 = reward NPC）/ itemplay **13** /
  enterworld **12** / leveluplogin **15**（等级键 {30,40,45,50,55,66}——66 级行随 con 解冻入面）/
  none **278**。
- 动作面（已路由行实例）：GIVE 80 / REMOVE 20 / CUTSCENE 26 / SPAWN 21 / SAY 7 / TELEPORT 3。
- 未解析名集 = 9 个 LF6 真端缺席进区别名（唯一）。

## 3. 落地物

| 交付 | 说明 |
|---|---|
| `src/main/java/.../tablelane/DataDrivenNativeRuntime.java` | 动作面收全：`ActionType` +TELEPORT/SPAWN/SAY，`scanFacedActions` 六 case（case 3/5/7 解析落面、case 6/8 按 c8d0 步规则、case 9/10 冻结、`wcstoul` 前导截断镜像 `parseRetailInt`、MESSAGE 列 7/8 实列区分）；**执行矩阵修正**：`apply()` 推进边只对 Hunt/EnterArea/TalkFOBJ 跑 `runActions`（接取分支维持全 kind）；挑战哨兵：`acquirePlan` talk 分支 `_challengetask_` → `reward_npc_name` 解析；`ACQUIRE_CONDITION_UNFACED` 桶撤销（planRow 早退删除）；`create(...)` 增 `teleportPort`/`spawnPort`/`sayPort` 三参 |
| `src/main/java/.../tablelane/NativeTeleportPort.java` | 新端口（live = TeleportService2，z+1 落地 + 度→6 位朝向） |
| `src/main/java/.../tablelane/NativeSpawnPort.java` | 新端口（live = SpawnEngine 单次刷 + 定时回收；Relative = 玩家半径均匀偏移，可行走校验偏差已登记） |
| `src/main/java/.../tablelane/NativeSayPort.java` | 新端口（live = SM_SYSTEM_MESSAGE(id)；通道偏差已裁定） |
| `src/main/java/.../retail/RetailStringIds.java` | 真端字符串 id 索引（`retail-quest-string-ids.tsv`，缺键 = fail-closed） |
| `src/main/resources/.../retail/retail-quest-string-ids.tsv` | 新数据：7 个 `STR_` 键 → id（源 = 真端 strings.xml 逐键提取，穷举切换集 col7 全键） |
| `src/test/java/.../tablelane/DataDrivenNativeRuntimeGateTest.java` | 16 例重写/扩展：⑨ 分桶冻结 1444/23 两桶 + 等级键 {30,40,45,50,55,66}；⑫ 改执行矩阵断言（Talk 推进零动作 ×4 渲染通道 + TalkFOBJ 推进按列序执行）；⑬ 扩 EnterArea 推进边过场+刷怪双面；⑪ 补接取分支步 0 动作（10500 接取即播 Movie 32）；新记录夹具 teleports/spawns/says |
| `src/main/resources/.../retail/quest-retail-tsv-manifest.tsv` | 登记新 TSV 行：`retail-quest-string-ids.tsv / name-index / ACTIVE / 真端 DD 附加动作 MESSAGE(Say) 字符串键→id 索引（7 键，缺失即真端加载失败 fail-closed）；消费者 RetailStringIds（P7 步 e2）` |
| `src/test/java/.../retail/RetailTsvManifestGateTest.java` | `EXPECTED_TSV_COUNT` 6→7（登记理由 = 本报告 §3/§4：新增 Say 字符串键索引 TSV，非页码类，role=name-index） |
| `p7/tools/dd-planrow-e2-mirror.py` | e2 离线镜像（动作面 v2 + con 非闸门 + 哨兵），分桶权威值来源 |

## 4. 门态（本批显式命令）

```
# 新门 + 算术门
mvn -o test -Dtest='DataDrivenNativeRuntimeGateTest,DataDrivenProgressTest' -DfailIfNoTests=false
# → 16 + 9 全绿
# 族门 + tablelane
mvn -o test '-Dtest=*FamilyGateTest,*RowAlignmentGateTest,*InventoryGateTest,NativeQuestTableLoaderTest,*RewardClaimGateTest,*NativeTalk*Test,NativeNearbyQuestAxisGateTest,SMNearbyQuestsPacketTest,NativeAcceptEntryAskFlowGateTest,DataDrivenNativeContractGateTest,DataDrivenProgressTest,DataDrivenQuestTableGateTest,DataDrivenEnterAreaPortGateTest,DataDrivenNativeRuntimeGateTest,MultiCellSensoryZoneRegistrationTest,RetailEnterAreaZoneRegistrationGateTest' -DfailIfNoTests=false
# → 187/187 绿，BUILD SUCCESS
# 聚焦套件
mvn -o test '-Dtest=*Quest*Test,*Retail*Test' -DfailIfNoTests=false
# → Tests run: 1703, Failures: 161, Errors: 137, Skipped: 2（mvn-exit=1 = 既有红类，预期）
# → 红类 105 == 步 e1 基线 105，类级 ADDED 0 / REMOVED 0
# 旧车道 DD 门
mvn -o test -Dtest=RetailDataDrivenGateTest -DfailIfNoTests=false
# → 9/9 绿
# 清单门复跑（登记后）
mvn -o test-compile surefire:test -Dtest=RetailTsvManifestGateTest
# → 3 例 1 红：仅剩既有 table-source-provenance.tsv 未登记（基线同信息，非本批引入）
```

聚焦套件行级归因（对 d2 基线 18 增 / 18 删）：**17 对 = 既有红的失败消息序漂移**（Set/Map 迭代序或
首失败实例漂移：var 映射序 {var1,var0}/{var0,var1}、15546 四计数器序、214371 击杀目标序、
首个缺失目录 quest 18035→30719、首个悬空前置 1870/1868→2870/2868——失败形状逐对相同）；
**1 处真实内容新增** = `RetailTsvManifestGateTest` 清单面多出本批 `retail-quest-string-ids.tsv`
未登记红 ⇒ 已按门规登记（§3）并同步冻结计数 6→7，复跑后该门与基线同信息。

日志：`gates/2026-10-02-p7-step2e2-family-tablane.log`、`gates/2026-10-02-focused-run-p7-step2e2.log`（临时，不入库）。

## 5. 反漂移规则（本批新增）

1. **动作执行矩阵以 handler 家族证据为准**：推进边执行面 = {Hunt, EnterArea, TalkFOBJ}；对话平面与
   ItemPlay/EnterWorld 推进边零执行器调用 = 零动作；接取分支（3/6/7/8/10）执行步 0；Talk 对话接取
   （1002/20000）零动作。禁止「所有推进边都跑动作」的统一化。
2. **宿主渲染面逐函数坐实后才落 case**：case 9/10 与 c8d0 步 case 6/8 的宿主面（creationId 客户端表 /
   到期分发 / def 虚槽）未坐实 ⇒ 保持 ACTION_UNFACED，不猜渲染。
3. **真端死列镜像忽略**：case 6/8 在非 c8d0 步 = 装载即死列（执行器无该 case）⇒ 解析忽略不冻结。
4. **数字 token = wcstoul 前导截断**（`83.9`→83、无数字→0），不从直觉用严格 parseInt。
5. **字符串键 fail-closed**：`STR_` 键缺表 = 真端装载失败语义 ⇒ NAME_UNRESOLVED，禁止猜 id；
   键表只收穷举切换集实键、逐键取自真端 strings.xml（先例 = quest_name_string_ids.tsv）。
6. **con_quest 两列 = 目录显示元数据非闸门**（条件表只收字面量；唯一消费方 = +0x198 槽注册）；
   显示面归 §10.3-#18，不得再因该列冻结接取。
7. **新 TSV 入冻结目录必须登记清单**：`quest/retail/*.tsv` 是冻结面，新增须同时改
   `quest-retail-tsv-manifest.tsv`（role/status/note 写明消费者）+ `EXPECTED_TSV_COUNT` + 报告登记
   理由；本批先例 = `retail-quest-string-ids.tsv`（name-index）。

## 6. 步 2 剩余

- **步 f**：1467 行原子切换 + 同批删除 DD 编译器与 zone/AI 台账读取 + typed 车道入口页残余随车道删除
  （§10.3-#22）；距离闸门旁路语义（§10.3-#24②）、Talk 步报告通道动作面（§1 登记）、Spawn 可行走校验、
  Say 通道表现偏差（系统消息 vs say 气泡）随步 f 前置清单闭合。
- EnterArea 接取区几何导入（QuestArea 轴 20 行非空别名）仍为独立前置（§10.3-#23 同流程）。
