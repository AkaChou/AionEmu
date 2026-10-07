# 子页动作丢失任务上下文：13403 推进后翻页动作 1694 落两参回显 ⇒ load fail（2026-10-06）

## 现象

用户报障（角色 Kk，任务 13403「大侵袭防备调查」，DataDriven 族；交互 NPC Kinesos=203096，
targetObj=24845）：步推进后停在页 select3(1693)，点击页面按钮（"继续听"/翻页，动作=1694）后
**对话窗口不关、页面 load fail**（"结束对话"按钮随之失效——本地关窗依赖正常页面状态）。

实机日志（`log/quests.log`，2026-10-06 18:38-18:39）：

```
18:38:38 203098（Spatalos）点任务行 31 → 页 4762 → 动作 20000 → 状态=3 步数=0 + 关窗（接取 ✓）
18:38:43 203096（Kinesos）点任务行 31 → 页 1011（questId=13403 ✓）
18:38:44 动作=1012 → 页 1012（questId=13403 ✓，DD 步匹配 → 三参回发）
18:38:45 动作=1013 → 页 1013（questId=13403 ✓）
18:38:46 动作=10000 → 状态=3 步数=1、2 + 页 1693（questId=13403 ✓，STAGE_PAGES[2]=select3）
18:39:13 动作=1694 上一页=1693 → SM_DIALOG_WINDOW targetObj=24845 questId=0 下发页=1694   ← 两参！
```

## 数据事实

- 13403：`data_driven_quest.xml:4370`（DD 族）；接取 Spatalos(203098)，交付 Alaus；步链
  `Talk(Kinesos,203096) → ItemPlay → Talk(Beris) → ItemPlay → Talk(Jenel) → Talk(FOBJ1) → Talk(FOBJ2)`
- 客户端契约（`client_dialog_contract.tsv:19561-19572`）：13403 声明 1011/1012/1013/**1693/1694/1695**/2375/2376/2716/3057/4762/10002
- DD `STAGE_PAGES = {1011,1352,1693,2034,2375,...}`（select1..15，`DataDrivenNativeRuntime.java:102`）；
  `DataDrivenProgress.step(vars) = vars & 0x3F`
- **基线对照**（log/quests.log）：9/28 起 1001/1002/1004/1005/1006/1007/14012 的动作 1694 全部
  **三参回发**（`SM_DIALOG_WINDOW ... questId=<任务> 下发页=1694`）

## 根因链

1. 动作 1694 = `SELECT3_1`（子页页 id 即翻页按钮动作 id；`QuestDialogPage.isSelectionSubPage(1694)=true`）。
2. 引擎链路：各族路由（13403 非 Simple 族）→ `DataDrivenNativeRuntime.onDialog` →
   `dispatchDialog` 的**步守卫**：`DataDrivenProgress.step(vars)=2 != hit.stepIndex()=0`（Kinesos 为
   第 0 步 Talk 目标）→ `continue` → **DD 不认领**（其 `dialogId >= 1000` 三参回发分支在守卫之后，
   够不到）→ `typed.owns(13403)=false` → 引擎 return false。
3. 上游 `DialogService.onDialogSelect`：`questId≠0 && !31 && !isSelectionSubPage` 不成立（1694 是
   子页）→ **不关窗** → 落 NPC 对话平面 switch → **default 两参回发**（`SM_DIALOG_WINDOW(oid, 1694)`，
   questId=0）。
4. 客户端按"无任务上下文"去 NPC 对话 html 找页 1694 → **找不到 ⇒ load fail**；页面状态损坏后
   "结束对话"（本地关窗）随之失效。

**缺环定位**：`isSelectionSubPage` 的消费面只有 SimpleTalk/SimpleItemPlay（族内 START 段分支，
`SM_DIALOG_WINDOW(objectId, dialogId, questId)` 三参）与 DialogService（两参回显）——
**DD 与其余族（Hunt/SerialHunt/CollectItem/UseItem/CombineTask）没有"子页原样回发"分支**，
未认领的子页动作一律落两参回显；当上一页是任务页（1693 ∈ 契约）时即 load fail。

## 修复

| 文件 | 变更 |
| --- | --- |
| `src/main/java/com/aionemu/gameserver/questEngine/QuestEngine.java` | **native 车道统一兜底**：`requestedOwner≠0 && npcId≠0 && !typed.owns && isSelectionSubPage(dialogId) && hasButtonPage(questId, dialogId)` 且任务 START/REWARD ⇒ 带 questId 原样回发该页（各族 handler 之后、typed 之前——movie 等副作用不受影响；与 Talk/ItemPlay 既有语义一致） |
| `src/test/java/.../tablelane/QuestEngineSelectionSubPageEchoTest.java` | 新回归门：实机行 13403（START，vars=2，NPC 203096）+ 动作 1694 ⇒ 唯一页 1694 且 questId=13403 |

行为边界：未声明的子页动作不受影响（fail-closed 保持）；questId=0 的 NPC 对话项照旧走
DialogService 两参回显（1115 验收形）；typed 车道保持原链路；各族已认领路径逐位不变。

## 验证状态

- IDEA 静态检查（QuestEngine + 新测试类）：**无错误**
- 单测：首轮未执行；后续已按 IDEA MCP 补齐（见文末续报，64/64 全绿）
- 实机复测：首轮复测已发生（翻页修复生效；暴露「结束对话循环一下才关闭」新症状，见续报）；
  第二轮修复后的实机复测**待用户**（重启服务端后重走到 13403 页 select3 点结束对话：
  应推进续链、不再循环）

## 续报：结束对话「循环一下才关闭」——DD 对话链推进被步守卫挡下（2026-10-06 18:54 实机 + 修复）

翻页修复生效后（1694/1695 均带 questId=13403 回发 ✓），新症状：点页 select3_1_1 的「结束对话」
（10002）→ 服务端**只发关窗包**（`SM_DIALOG_WINDOW(0,0)`）、**无 SM_QUEST_ACTION**（任务不推进）；
玩家侧「没有关闭窗口、循环了一下才关闭」。

根因：`dispatchDialog` 外层守卫 `step(vars) != hit.stepIndex() → continue`——10002 到达时当前步 = 2
（Beris 步），而该 NPC（Kinesos=203096）的 hit 步 = 0 ⇒ 被挡 ⇒ 落 DialogService 的「未处理任务动作」
兜底关窗。而推进后的新步页（1693）正是 `sendPostAdvancePage` 发给**同一对话窗**的（对话链模型：
Kinesos 开场 → 翻页 → 结束 → 下一步页续链），其按钮必须能在该窗被服务。

修复（`DataDrivenNativeRuntime.dispatchDialog`）：

1. 推进动作（10000+k）免 hit 步守卫（advanceAction 豁免）；其余动作守卫不变；
2. 推进以**当前步**为基准：`currentStep = step(vars)`、`currentPlan = plans.get(currentStep)`——
   advance/动作执行器/续链页全锚当前步；
3. advance 失败不发页（return false）。

验证：IDEA MCP `DataDrivenNativeRuntimeGateTest` **31/31**（含新用例
`dialogChainAdvanceIsServedAtANonCurrentStepNpc`：13403 实机行、乱序静默 + 非当前步 NPC 窗推进
+ 单页续链）；回归 SimpleTalk 16/16、SimpleItemPlay 15/15、QuestEngineSelectionSubPageEchoTest 1/1、
QuestEngineOpenDoorReplayOrderTest 1/1——**64/64 全绿**。实机复测待用户（重启后页 select3 点
结束对话应推进续链、不再循环）。

## 续报三：第三轮实机（20:52）——链条走完到末步但 10255 被挡；「同窗续链」为误判，推进尾改关窗

实机 trace（`log/quests.log` 2026-10-06 20:52:20-38）：与 Kinesos(203096) 的完整链
31→1011→1012→1013→10000（步1,2+页1693）→1694/1695→10002（步3,4+页2375）→2376→10004（步5+页2716）
→10005（步6+页3057）→**10255 仅关窗、无 SM_QUEST_ACTION**（任务停在 START/步6，不进 REWARD）；
重开落两参页 10，随后出现 questId=0 的 1012 回显（客户端残留窗态）。

根因（两级）：

1. `dispatchDialog` 的 hit 步守卫只豁免 10000..10015；末步按钮 `10255`（= SET_SUCCEED /
   ACTION_ADVANCE_COMPLETE）从非当前步的对话窗到达（hit=Kinesos 步 0 ≠ 当前步 6）被挡 ⇒ 只关窗。
2. **「同窗续链」为误判**：round-2 的「推进后把新步页发回同窗」证据来自本服自己发包后客户端跟点
   下一段按钮（循环论证）。真端/退役面推进尾 = 关窗零发页：退役 SETPRO 全量普查 3923 条 = 3479
   close-dialog+sync / 142 SELECT_QUEST（quest-19671-relay-close-claim）；同平台 SimpleTalk 1131
   实机验收；10528 验收（SET_SUCCEED after-commit = LEVEL_AND_VISIBILITY_REFRESH → CloseDialog）。
   错误尾部同时把 Beris/Jenel/两台机器步骤的按钮流全带进 Kinesos 的窗（链在首个 NPC 处走完）。

修复（`DataDrivenNativeRuntime.dispatchDialog`）：

1. 推进尾 `sendPostAdvancePage` → `DialogService.closeDialog`（关窗零发页，`mgr+0x5d8`）；
   下一步骤由其自身 NPC 窗口的打开/行选（31）服务；
2. `10255` 计入免守卫动作 + 以当前步为基准（收口 = REWARD + 可见性刷新 + 关窗）。

测试：⑤a 增推进尾关窗断言、⑤c 改为「非当前步窗推进 + 关窗」、新增 ⑤f（13403 末步 10255 从
Kinesos 窗到达 → REWARD + 关窗）；IDEA 静态检查无错误；单测待授权（IDEA MCP）；实机复测待用户
（重启后全程：每次推进应关窗、任务书指向下一个 NPC，末步 10255 进 REWARD、可赴 Alaus 交付）。

## 续报四：第四轮实机——「使用探测器的步骤被跳过」= DD ItemPlay 触发接错面（获得 vs 使用）

用户第四轮反馈：**窗口关闭已正确，但会跳过使用探测器的步骤**。

实机形（trace 20:52 已有形）：步 0 Talk(Kinesos) 的完成动作发放 QUEST_13403A 后，一次 `10000`
把步数从 0 推到「1、2」——步 1 ItemPlay（使用探测器 A）被**发放动作级联满足**，玩家无需使用道具。

根因：DD ItemPlay 事件此前接在「物品获得」面——`QuestEngine.onItemGet`（`Storage.java:182` 背包
入包统一入口）→ `DataDrivenNativeRuntime.onItemAcquired`。真端事件 5 的派发点在 `User__UseItem`
（`User.cpp:58921`，`local_90 = 5` → `mgr+0x268+5*0x10` walk；`ctx+8` = 被使用物品 id），
`User_IdentifyItem`(59477)/`User__DoEnchantItem`(60047) 同形 ⇒ **事件 5 = 物品使用**；获得面没有
`0x268` 注册 walk。`P7-STEP2D2-REPORT` 的「物品获得事件 5」为误标（报告顶部已加勘误）；
`p3-prereqs/family-table-shapes.md:49`「无 acquired_npc_name 列——接取 = 使用物品」佐证
接取 kind 3（用物品接取）同样由「使用」事件触发（先接取、后可以再进 ItemPlay 组计数）。

修复：

1. `QuestEngine.onItemUseEvent`（`CM_USE_ITEM:179`）在 Simple 族先手后加 DD 钩子
   `DataDrivenNativeRuntime.instance().onItemUsed(player, itemId)`；
2. `QuestEngine.onItemGet` 删除 DD 调用（保留 typed GetItem 广播）；
3. 运行时 `onItemAcquired` → `onItemUsed`（双角色不变：非 START 且接取 kind==3 → 使用物品接取
   + 步 0 动作；兴趣面/类注释「物品获得」措辞同步为「物品使用」）；
4. 测试：⑤d 改名 `itemPlayStepsCountTheUsedItem`、⑪ 双角色改走 `onItemUsed`、类 javadoc 与
   兴趣面注释同步。

验证：IDEA 静态检查（QuestEngine / DataDrivenNativeRuntime / DataDrivenNativeRuntimeGateTest）
无错误；单测（IDEA MCP，2026-10-06）：`DataDrivenNativeRuntimeGateTest` **32/32**（含第三轮 ⑤a/⑤c/
⑤f 与第四轮 ⑤d/⑪）+ 族门回归 SimpleTalk 16/16、SimpleItemPlay 15/15、子页回显 1/1、开门重放 1/1
——**65/65 全绿**（覆盖第三、四两轮全部改动）；实机复测待用户（重启后 13403：发放 A 后不得自动
推进，**须实际使用探测器**才进下一步）。记忆库沉淀：QE-152 `DD_ITEMPLAY_USES_USE_EVENT`（含
QE-150 路由句扩与两卡 validation 回写）。

## 续报五：第五轮实机——探测器双发 + 丢弃语义（接取执行面修正 + 销毁停止任务）

两条实机反馈：

1. **入侵探测器一次发放了 2 个**：接取收尾（d5b0 `param_2<0` 路径）被执行成了「进度步 0 动作」
   ——13403 步 0 列 = 发 QUEST_13403A ⇒ 接取（Spatalos 20000）即发一次，步 0 完成（Kinesos
   10000）再发一次。反编译复读：d5b0 `param_2<0` 执行 `*(entry+0x10)`（`category_acquire_` 装入的
   `QuestProgressExtraInfo` 对象）= **接取行 `value1..10_acquire_`**；`FUN_180c46e90`/`FUN_180c46bb0`
   接取分支同容器。步 f 的「步 0 动作」为误读（反相实害：1817 接取列发 `QUEST_1817A`、步 0 列空 ⇒
   接取零发放），两份 p7 文档已加勘误，沉淀 QE-153。
2. **扔掉探测器应更新任务状态/自动放弃**：真端 `User_DestroyItem`（User.cpp:61849）——物品被进行中
   任务引用 ⇒ 全部不可放弃 = 拒绝（0x13d87c=1300604）；否则确认窗（0x249f1=150001）；确认（回复
   分支 76230）⇒ 逐任务 `User_DeleteQuest`（= 放弃）+ 删物品；拒绝 = 0x13d87d=1300605 重试。
   落地 = `tryQuestItemDestroy`（CM_DELETE_ITEM 钩子）+ DD 引用面 `questsReferencingItem`
   （发/扣/用物 + 接取列反查）+ `QuestService.canAbandon(Player, questId)`，沉淀 QE-154。

修复（第五轮）：

| 文件 | 变更 |
| --- | --- |
| `QuestEngine.java` | `tryQuestItemDestroy` + `activeQuestsReferencingItem`（typed `questItems` ∪ DD 引用面，START/REWARD 门） |
| `DataDrivenNativeRuntime.java` | `acceptActionsByQuestId` + `scanAcceptActions`/`runAcceptActions`（Talk 收尾与 `acquire()` 双角色改取接取表）；`questRefsByItemId` + `questsReferencingItem` |
| `DataDrivenQuestTable.java` | `Row.acceptColumns` 解析 `value1..10_acquire_`（fail-closed） |
| `QuestService.java` | `canAbandon(Player, questId)` 公开重载（目录 → nativeMetadata 同链） |
| `CM_DELETE_ITEM.java` | 删除前 `tryQuestItemDestroy` 认领（引用中 ⇒ 确认/拒绝/重试，物品暂不删） |
| `DataDrivenNativeRuntimeGateTest.java` | ⑩-b 改写（`talkDialogAcceptRunsTheAcceptRowActions`：探针行断言接取列执行）、新增 `acceptTailDoesNotGrantTheStepZeroItemAgain`（13403 双发回归 + 引用面断言）、⑪ 10500 改「接取零动作」 |

验证：IDEA 静态检查无错误；单测（IDEA MCP，2026-10-07）：`DataDrivenNativeRuntimeGateTest` **33/33**
+ SimpleTalk 16/16 + SimpleItemPlay 15/15 + 子页回显 1/1 + 开门重放 1/1 = **66/66 全绿**；实机复测待
用户（13403：接取后探测器恰 1 个 → Kinesos 完成后推进 → **实际使用探测器**进 Beris 步；丢弃探测器：
弹确认窗 → 确认后 13403 被放弃 + 物品删除／拒绝后保留）。

## 续报六：第六轮实机——「无法使用道具」= 使用区球心/半径错位（真端 source_sphere 修正）；「侦测点光圈」= 贝里特拉和平态世界标记（2026-10-07）

两条实机反馈：① 站 1 使用 入侵感测器 → 提示「在现在的位置无法使用道具」；② 探测的位置本应有光圈
（地面特效），现在没有——XML 时代存在（回归）。

### 6.1 使用区球心/半径修复（`zones_quest.xml`）

- **旧数据错位**：LF1A/DF1A × Q13403A/B 四条区均为 r=10 球体，球心为旧导出值。玩家站隐形侦测点
  805263 `(1701.0,1629.1,124.0)`／805264 `(1554.8,2081.3,160.9)`（`spawns/Npcs/210030000_Verteron.xml:3011/3014`）
  距旧球心 ≈32–34m ⇒ `MapRegion.isInsideZone` 失败 ⇒ `PlayerRestrictions.canUseItem` 发
  `SM_SYSTEM_MESSAGE(1300143)`（拒用）。
- **真端权威**（`<真端根>/Map/XML/Subzones/source_sphere.csv`；多边形见 `<真端根>/Map/Worlds/lf1a/world.xml`
  的 `<item_use_area>`；列 = 球心 x/y/z + 外接半径）：

  | 区 | 球心 | 半径 |
  |---|---|---|
  | LF1A_ITEMUSEAREA_Q13403A | 1705.84, 1663.02, 105.40 | 59.37 |
  | LF1A_ITEMUSEAREA_Q13403B | 1588.79, 2078.18, 173.22 | 55.90 |
  | DF1A_ITEMUSEAREA_Q23403A | 1873.01, 1673.65, 264.69 | 48.34 |
  | DF1A_ITEMUSEAREA_Q23403B | 1759.56, 504.04, 255.52 | 51.29 |

- 落面：四条区球心/半径全部替换为上述值；`zone_type` SUB→ITEM_USE（QE-043 先例）。两处实机站位
  均在半径内（3D 距离 38.95 < 59.37；36.26 < 55.90）。
- **旁证（与光圈关联）**：旧球心 `(1702.6613,1662.9213,102.19242)` / `(1586.9154,2078.2305,155.875)`
  与 `spawns/Beritra/210030000_Verteron.xml` 中 702548 的 PEACE 生成坐标**逐位相同**——原使用区就是
  围绕「和平态光圈物件」写的（Altgard Q23403 两点同理，`220030000_Altgard.xml:22/40/58`）。

### 6.2 光圈消失根因：贝里特拉总开关（2026-09-21 起关闭）

- 光圈 = `WorldRaid_SP_Object`（`npc_id=702548`，客户端模型 `NPC/level_object/World_raid`，ai=noaction）：
  贝里特拉入侵系统 PEACE 状态在各入侵点常驻刷出的世界标记（Verteron id=5 → 站 1 光圈、id=4 → 站 2
  光圈，`spawns/Beritra/210030000_Verteron.xml:22/40/58`）。
- 消失原因：`custom.properties:79 gameserver.beritra.enable` 于 **2026-09-21 提交 `216f970f8c`** 由
  `true` 改为 `false` ⇒ `BeritraService.initBeritraLocations()`（BeritraService.java:66-76）不再刷任何
  PEACE 物件；`VisibleObjectSpawner.spawnBeritraNpc`（VisibleObjectSpawner.java:423）第二道门同源。
  运行日志佐证：console.log 中「已加载 N 个贝里特拉位置」（log.6162acf484de）出现 **0 次**。
- 处置（本轮，方案 A）：开关改回 `true`（源码树 + `aion/` 部署副本两处；后者被 .gitignore 忽略）。
  冷重启后全服和平态物件回归（含两处侦测点光圈），入侵日程随之恢复（Verteron 11:00/17:00/21:00，
  入侵窗口内光圈被替换为入侵物件、结束后回归）。启用前覆盖度核对：`beritra_invasion.xml` 55 个 location
  与 `spawns/Beritra/*` 55 个 `beritra_spawn id` 一一对应（`BeritraService.spawn()` 无空表 NPE 风险）。

### 验证状态

- `zones_quest.xml`：IDEA 静态检查无错误；待冷重启实机复测（原点位使用道具应成功）。
- 开关：已改回 `true`；待冷重启复测（两处侦测点可见光圈；启动日志出现「已加载 N 个贝里特拉位置」）。
- 沉淀：待复测通过后按协议入 memory-bank（候选：使用区球心采信 source_sphere.csv；贝里特拉开关 ↔ 侦测点光圈）。

## 续报七：天族地图点「记忆中的目击地点」被送到魔族地图 = 镜像隐形 NPC 搜索 ID 串侧（CM_OBJECT_SEARCH 种族镜像改写）（2026-10-07）

**现象**：天族 GM 在地图上点击任务点位「记忆中的目击地点」→ 被传送到阿尔特盖德（220030000），
而非本侧目标点 B 附近（期望 ≈ `//moveto 210030000 1586.9 2078.2 155.9`）。

**链路（已实锤）**：

- 入口 = `CM_OBJECT_SEARCH`（地图点选/搜索 NPC 包，只带一个 NPC ID）；本服该包的 GM 分支 =
  直接 `TeleportService2.teleportToNpc`（普通玩家则回 `SM_SHOW_NPC_ON_MAP` 标记）——「GM 账号被传送」由此而来。
- 被点目标的名字串：466498「记忆中的目击**地点**」= **魔族**隐形 NPC 805266（Altgard 220030000，
  刷怪点 (1761.36, 506.55)）；天族对应物 466496「记忆中的目击**场所**」= 805264（Verteron 210030000，
  (1554.83, 2081.30)）。两族镜像名仅差一字（场所/地点），客户端任务页链接/地图搜索提交的是该名字串解析出的 ID。
- 四个隐形 NPC 客户端均 `hide_map=0`（`npcs_unpacked/client_npcs_npc.xml:948548-948694`）= 世界内不可见、
  地图常显点位；`npc_template_800031_834289.xml:44584-44596` 为四模板。
- 客户端提交的是魔族侧 ID（805266）⇒ `findSearchTarget` 忠实解析到其 Altgard 刷怪点 ⇒ GM 被送过海。

**修复**（`CM_OBJECT_SEARCH`，沿用本包四个既有同名别名先例）：

- 新增 `resolveFactionMirrorSearchNpcId(Race, int)`：按玩家种族把对侧镜像 ID 改写为本侧
  （天族：805265→805263、805266→805264；魔族：805263→805265、805264→805266），挂在
  `resolveSearchNpcId` 链尾；
- 四个 ID 仅服务 Q13403/Q23403 这两个镜像任务、无其他占用，故按种族直接改写（不挂任务状态门）。

**验证状态**：IDEA 静态检查（CM_OBJECT_SEARCH / CMObjectSearchTest）**无错误**；**实机复测通过**
（2026-10-07 冷重启后天族点该点位落至本侧 210030000 805264 一带）。

**单测（IDEA MCP，2026-10-07，授权后补跑）**：首轮 `CMObjectSearchTest` **16/17**——既有用例
`resolvesSearchNpcIdForNormalPlayerWithActiveQuest` 失败：新链尾读取 `player.getRace()`
（`Player.getRace()` 委托 `playerCommonData.getRace()`），而 Objenesis 桩玩家无 `playerCommonData`
⇒ NPE（生产玩家不受影响）。按本文件既有惯例（反射注入 `questStateList`）补桩 `playerWithRace`
（注入 `PlayerCommonData(race)`），并新增全链用例 `rewritesTheCrossFactionMirrorThroughThePlayerSearchChain`
（天/魔四向经 `resolveSearchNpcId` 全链）⇒ **17/17 全绿**；生产代码无需改动。
