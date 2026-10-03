# P7 步 2 步 e1 报告：四类接取面 + GIVE/REMOVE/CUTSCENE 动作面 + 两新冻结桶（零行为变更）

> **门禁产物清理提示（2026-10-03）**：本文件引用的门禁运行产物（`gates/*.log`、`*-red-classes.tsv`、`*-delta.tsv` 等）已按「只记录重要的过程内容、不记录门禁」口径清理，不再随仓保留；关键读数已内联于正文，复现请重跑对应聚焦套件，清理说明见 `.agents/summary/CLEANUP-LEDGER.zh-CN.md`。

- 批次：P7 DataDriven 步 2 步 e1（计划 §7「P7 DataDriven」/ §10.2 / §10.3-#24；证据笔记
  `p7/P7-STEP2E-PREREQ-ACQUIRE-ACTIONS.zh-CN.md`）
- 日期：2026-10-02（承接步 d2 的 2026-10-02 收口）
- 结论：DD 原生运行时补齐**接取轴**（Talk 对话 `FUN_180c47220` 词汇 4762/1002/1003/1004/1007/20000/20001、
  ItemPlay 事件 5 / EnterWorld 事件 0x12 双角色、LevelUp/LevelUpLogIn 等级等值遍历 kind 8/10）与
  **附加动作面**（真端执行器 case 1/2/4：发/扣物品对 + Cutscene/Cutscene2/Movie/Movie2，只在步进/收口分支
  执行，`FUN_180c46020` 尾部）；两新冻结桶 `ACTION_UNFACED`（未落面 case 3/5/6/7/8/9/10）与
  `ACQUIRE_CONDITION_UNFACED`（`con_quest`/`con_quest_list` 条件列）入冻结面。生产口径**路由集仍为空**
  （零行为变更）。步 d2 分桶（1458/9）变为**可路由 1384 / 冻结 83**
  （ZONE_ABSENT 9 + NAME_UNRESOLVED 6 + ACTION_UNFACED 33 + ACQUIRE_CONDITION_UNFACED 35）。

## 1. 真端事实（原码逐函数，取证笔记全文见 `p7/P7-STEP2E-PREREQ-ACQUIRE-ACTIONS.zh-CN.md`）

| 面 | 真端函数/位置 | 关键语义 |
|---|---|---|
| 接取注册 | `LoadBasicInfo`（`ScriptDLL64.c:2075690-2075920`） | acquire==4 Talk → 0x640 对话对象 #1（condition {type=0,value=-1}，槽 +0x1d8 = `FUN_180c47220`）；**所有行**恒有对象 #2 `reward_npc_name`（{type=4,value=-1}，槽 +0x238）；空步集 → 对象 {type=3,value=0}（槽 +0x1e0）；acquire==3 → 事件 5、==7 → 事件 0x12、==8 → vec0 遍历、==10 → vec0+vec3 双遍历；**acquire==6 EnterArea 无 DLL 注册**（经 `FUN_180c47bf0` 尾部双角色 + 独立接取侧区名哈希树工作 ⇒ e1 接取面缺席 = 镜像真端注册树未填，不冻结） |
| Talk 接取词汇 | `FUN_180c47220` | 打开（状态 0/10）→ 页 4762；1002 → `SetQuestAcquired`(+0xd8) → 页 1003；1003 → 页 1004；1007 → mgr+0x1a0 接取窗（客户端契约 fail-closed）；20000 → 接取 → mgr+0x5d8 完成通道；20001 → +0x2a8 演出（未坐实，e1 只回完成页）；1008 → 完成通道；其余 ≥1000 回显 |
| 进行中接取 NPC 面 | `FUN_180c473e0`（槽 +0x238） | 打开 → 页 10002；1008 → 完成通道；1009 → mgr+0x1b0 报告（e1 不落面——对象 #2 属奖励申报轴） |
| 等级接取 | `FUN_180c46bb0`（升级）/ `FUN_180c46c90`（登录） | 树遍历 + kind 判别 `(kind-8) & ~2 == 0`（匹配 8 与 10）+ **等级等值** `param_2+8 == def+8`；登录遍历另有 +0x138 否决槽（拒绝/删除簿，本服无对应簿面 ⇒ 不镜像，接取仍受 `NativeQuestStartPort` 条件面约束）；登录面只服务 kind 10 |
| 动作执行时机 | `FUN_180c46020` 尾部（Hunt verbatim） | 动作列表只在步进/收口分支执行（`FUN_180c4cd50(def+0x10)`）；组计数部分自增分支提前 return 不执行 |
| 执行器 | `FUN_180c4cd50`（共享，case 1/2/3/4/5/7/9/10）/ `FUN_180c4c8d0`（EnterArea/TalkFOBJ，全 1-10）/ `FUN_180c4d190`（Hunt，生成经 user+0x180） | case 1 发物品 / 2 扣物品 / 4 过场（`Cutscene|Cutscene2|Movie|Movie2 N`；槽 +0x1b8 独立 PlayMovie 槽对应 Movie 词形）；解析格式 1/2=`符号 数量` 对（,/空格分隔）、4=词 + id（+HACTION 链接，e1 忽略）；未知词形 = 真端 Wrong Type!!（记日志跳过，不冻结） |

## 2. 分桶重算（接取轴入冻结面后）

- 切换集 1467 行 → **可路由 1384 / 冻结 83**：
  - `ZONE_ABSENT` **9**（§10.3-#23，LF6 真端缺席进区别名，维持）；
  - `NAME_UNRESOLVED` **6**（挑战任务接取哨兵 `_challengetask_`：17160/17161/17162/27160/27161/27162——
    接取参数不是 NPC 名；旧车道 P0c-58 四源裁定 = 交付 NPC 自身接取，随接取哨兵语义批落面，e1 fail-closed）；
  - `ACTION_UNFACED` **33**（携带未落面附加动作列：SPAWN(5) 为主 — 单列 11 行、与 6/9/10 组合 10 行、
    其余 TELEPORT(3)/MESSAGE(7)/DELAY(6)/ENTER_INSTANCE(9)/TIMER(10) 形，直方图见镜像输出）；
  - `ACQUIRE_CONDITION_UNFACED` **35**（`con_quest`/`con_quest_list` 条件列非空，真端 0x640 条目 type 表
    未坐实 ⇒ 整行冻结）。
- 已路由行逐类步数：hunt **759** / collectitem **335** / pvp **205** / talk **272** / enterarea **116** /
  itemplay **29** / enterworld **26** / talkfobj **10**（离线镜像与 Java 实测逐值一致）；切换集逐类步数与
  P7 步 1 契约逐值一致（827/348/207/403/153/42/34/19）。
- 已路由行接取计划直方图：talk **1084**（NPC 键 471 个）/ itemplay **13**（物品键 13 个）/ enterworld **10**
  （world 键 {210100000,220110000,302340000×4,302350000×2,400010000×2}）/ leveluplogin **10**
  （等级键 {30,40,45,50,55} 各 2 行）/ none **267**（none 120 + enterarea 165 + 条件/动作/哨兵冻结行的
  接取类别不计）；切换集接取类别直方图 = talk 1142 / enterarea 165 / none 120 / leveluplogin 15 /
  itemplay 13 / enterworld 12，与 P7 步 1 契约逐值一致。
- 动作面：已路由行中 **53 行带已落面动作**，实例 = GIVE 61 / REMOVE 9 / CUTSCENE 17（镜像与 Java 一致）。
- 未解析名集 = 9 个 LF6 区名（原文大小写）+ `_challengetask_`，共 10 个。

## 3. 落地物

| 交付 | 说明 |
|---|---|
| `src/main/java/.../tablelane/DataDrivenNativeRuntime.java` | 接取计划 `AcquirePlan`（`acquirePlan`：talk→parseGroups+resolveMonsters、itemplay→物品索引、enterworld→world id、levelup/login→等级等值键；EnterArea/none = `NONE` 不冻结）+ 四张接取兴趣面（`acquireTalksByNpcId`/`acquireItemsByItemId`/`acquireWorldsByWorldId`/`acquireLevelsByLevel`）+ `dispatchAcquireDialog`（`FUN_180c47220` 词汇；只服务无状态玩家；`requestedOwner` 过滤；1007 走 `QuestDialogContract` fail-closed）+ `acquire` 双角色（progress 事件 state!=START 分支，接取即执行步 0 动作）+ `onLevelReached`（kind 8/10 等级等值；登录面 requiredKind=10）+ 附加动作面（`ActionPlan`/`scanFacedActions`：case 1/2 发扣对、case 4 Cutscene/Movie 词形；未落面 → `ACTION_UNFACED`）+ `runActions`（真端执行器 case 1/2/4：`NativeInventoryPort.give/remove` + `NativeMoviePort.play/playMovie`）+ `apply` 只在 `STEP_ADVANCE`/`STEP_COMPLETE` 分支执行动作；**裁定修正**：接取轴解析移到路由裁定**之前**（接取参数失败整行冻结，routed ∪ frozen 互斥闭合） |
| `src/main/java/.../tablelane/DataDrivenQuestTable.java` | Row 增 `con_quest`/`con_questList` 两条件列（只装载不解释）+ `hasAcquireConditions()` |
| `src/main/java/.../tablelane/NativeMoviePort.java` | `playMovie` 独立缺省方法（电影型资源 `QuestMovieType.CUTSCENE_MOVIE`，对应 DD `Movie|Movie2` 词形 / 真端 +0x1b8 独立槽）；Live 双实现 |
| `src/main/java/.../questEngine/QuestEngine.java` | `onLvlUp` 前置 `onLevelReached(player, level, false)`（升级遍历）；新增 `onLoggedIn(Player)`（登录遍历，best-effort） |
| `src/main/java/.../controllers/PlayerController.java` | `onEnterWorld` 调 `questEngine().onLoggedIn(getOwner())`（DD LevelUpLogIn 接取遍历挂点；路由集空 ⇒ 恒 false） |
| `src/test/java/.../tablelane/DataDrivenNativeRuntimeGateTest.java` | 12 → **16 例**：+⑩ Talk 接取词汇全态（4762/1002+1003/1004/20001+1008/1008 回发/1012 回发/999 零动作/1007 客户端契约 fail-closed/已接取玩家带上下文不再见 4762）、+⑪ 双角色接取（ItemPlay/EnterWorld 双族 66 级探测——该批接取行 minlvl 65/66；等级键集冻结 {30,40,45,50,55}；升级/登录遍历 + 等级等值守卫）、+⑫ 发/扣动作面（表列原文复算期望调用，列序执行）、+⑬ 过场动作面（Cutscene→`play` / Movie→`movie` 词形分型）；⑨ 分桶冻结更新为 1384/83 四桶与未解析名 10 个；⑤a/⑤b/⑥ 页动作改带任务上下文（真端客户端进度页发 questId）；生产单例断言加四张接取兴趣面 + `onLevelReached` 恒 false |
| `p7/tools/dd-planrow-e1-mirror.py` | e1 离线镜像（d2 行内轴 + 接取轴「先裁定后路由」+ 附加动作轴），分桶权威值来源 |

## 4. 门态（本批显式命令）

```
# 新门 + 算术门
mvn -o test -Dtest='DataDrivenNativeRuntimeGateTest,DataDrivenProgressTest' -DfailIfNoTests=false
# → 16 + 9 全绿
# 族门 + tablelane
mvn -o test '-Dtest=*FamilyGateTest,*RowAlignmentGateTest,*InventoryGateTest,NativeQuestTableLoaderTest,*RewardClaimGateTest,*NativeTalk*Test,NativeNearbyQuestAxisGateTest,SMNearbyQuestsPacketTest,NativeAcceptEntryAskFlowGateTest,DataDrivenNativeContractGateTest,DataDrivenProgressTest,DataDrivenQuestTableGateTest,DataDrivenEnterAreaPortGateTest,DataDrivenNativeRuntimeGateTest,MultiCellSensoryZoneRegistrationTest,RetailEnterAreaZoneRegistrationGateTest' -DfailIfNoTests=false
# → 187/187 绿（步 d2 基线 183 + e1 新门 4），BUILD SUCCESS 1:05
# 聚焦套件
mvn -o test '-Dtest=*Quest*Test,*Retail*Test' -DfailIfNoTests=false
# → 1703 / 161F+137E / 105 红类（对步 d2 基线红类集逐类相同：ADDED 0 / REMOVED 0；
#   行级 15 对同方法消息序漂移 = 既存红断言消息里 HashMap/HashSet 迭代序非确定性，
#   与步 c→d2 基线间既有的 6 行漂移同现象，零净变化）
# 旧车道 DD 门（动作面/接取面不触旧编译器）
mvn -o test -Dtest=RetailDataDrivenGateTest -DfailIfNoTests=false
# → 9/9 绿
```

日志：`gates/2026-10-02-p7-step2e1-family-tablane.log`、`gates/2026-10-02-focused-run-p7-step2e1.log`（临时，不入库）。

## 5. 反漂移规则（本批新增）

1. **接取轴先于路由裁定**：接取参数名失败 = 整行冻结（`NAME_UNRESOLVED`）；禁止「先路由后补接取」的
   半行状态（routed ∪ frozen 必须互斥闭合）。
2. **动作只在步进/收口分支执行**（真端 `FUN_180c46020` 尾部）：组计数部分自增分支不执行动作列表；
   打开页/回显分支同样零动作。
3. **接取面缺席 ≠ 冻结**：EnterArea 接取（真端无 DLL 注册，经 `FUN_180c47bf0` 尾部双角色 + 独立区哈希树）
   e1 镜像为「无接取面」不冻结，与「接取参数解析失败整行冻结」区分。
4. **等级等值由键索引承担**：`acquireLevelsByLevel` 按真端 def 等级键入桶，遍历事件传玩家当前等级——
   测试断言守卫必须传玩家**真实**等级（运行时不在 `acquire` 内复检等级）。
5. **接取词汇的 owner 上下文**：页动作（10000+K/1009 等）真端客户端携带任务上下文 ⇒ 运行时按
   `requestedOwner` 过滤接取面；测试必须带 owner（无上下文的 open 才是接取面合法入口）。

## 6. 步 2 剩余

- **步 e2**：剩余附加动作面（TELEPORT/SPAWN/DELAY/MESSAGE/ENTER_INSTANCE/TIMER）、`con_quest` 条件列
  语义坐实（35 行）、挑战任务接取哨兵 `_challengetask_` 落面（6 行，P0c-58 四源裁定 = 交付 NPC 自身）、
  EnterArea 接取区几何导入（QuestArea 轴）。
- **步 f**：1467 行原子切换 + 同批删除 DD 编译器与 zone/AI 台账读取 + typed 车道入口页残余随车道删除
  （§10.3-#22）；距离闸门取值来源（§10.3-#24②）须在步 f 前闭合。
