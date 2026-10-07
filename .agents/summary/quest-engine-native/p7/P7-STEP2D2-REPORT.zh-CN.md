# P7 步 2 步 d2 报告：DD CollectItem/ItemPlay 事件源与共享对话平面（零行为变更）

> **门禁产物清理提示（2026-10-03）**：本文件引用的门禁运行产物（`gates/*.log`、`*-red-classes.tsv`、`*-delta.tsv` 等）已按「只记录重要的过程内容、不记录门禁」口径清理，不再随仓保留；关键读数已内联于正文，复现请重跑对应聚焦套件，清理说明见 `.agents/summary/CLEANUP-LEDGER.zh-CN.md`。

- 批次：P7 DataDriven 步 2 步 d2（计划 §7「P7 DataDriven」/ §10.2 / §10.3-#24①②）
- 日期：2026-10-02（承接步 d 的 2026-10-02 收口）
- 结论：DD 原生进度运行时补齐剩余三类——kind 1 CollectItem / kind 4 Talk 接入**真端共享对话平面**
  （`FUN_180c474b0`），kind 3 ItemPlay 接入**物品获得事件**（注册事件 5，`FUN_180c46e90`）；`QuestEngine`
  新增 `onDialog` / `onItemGet` 两条分流 hook。生产口径**路由集仍为空**（零行为变更）。步 d 的分桶
  （可路由 1017 / 冻结 450）变为**可路由 1458 / 冻结 9（全部 ZONE_ABSENT）**，未解析载荷名清零。
- 同批纠正步 d 的两处归属（QE-132）：`FUN_180c466a0` 是家族交付 handler（DD 行 +0x10 恒 -2 ⇒ 死代码），
  不是 DD Talk 推进面；CollectItem 不是「宿主采集/交付事件源」，与 Talk 同走共享对话平面。

> **勘误（2026-10-06，13403 实机 + 真端复读）**：本报告「kind 3 ItemPlay 接入**物品获得事件**（注册事件 5）」
> 的措辞有误——真端事件 5 的派发点在 `User__UseItem`（`User.cpp:58921`，`local_90 = 5` →
> `mgr+0x268+5*0x10` walk，`ctx+8` = 被使用物品 id；`User_IdentifyItem`(59477)/`User__DoEnchantItem`(60047)
> 同形），即**物品使用**事件；获得面不派发它。原接线使 DD ItemPlay 步在发放道具时被级联满足（13403
> 实机：「使用探测器」步被跳过）。修正见 `DataDrivenNativeRuntime.onItemUsed` /
> `QuestEngine.onItemUseEvent`（QE-152）；正文第 8/20/71 行的「物品获得」请按「物品使用」理解。

## 1. 真端事实（原码逐函数，本次复读）

| 面 | 真端函数 | 本次坐实的关键语义 |
|---|---|---|
| kind 注册面 | `DataDrivenQuestLoader::LoadProgressInfo` | 注册 switch：1=CollectItem、2=Hunt、3=ItemPlay、4=Talk、5=Pvp、6=EnterArea、7=EnterWorld、9=TalkFOBJ（纠正步 d 的 Talk/ItemPlay 归属） |
| 共享对话平面 | `FUN_180c474b0`（kind 1 与 kind 4 主对象共用） | ① 打开（状态 0/10）→ 阶段页 `select(K+1)`，页表 15 值 {1011,1352,1693,2034,2375,2716,3057,3398,3739,4080,6500,6841,7182,7523,7864}（步 ≥15 无页；空步集 → 10002）；② 页动作 10000..10013，顺序守卫 `code-9999 == 当前步+1`（⇔ 动作码 = 10000+当前步）→ 写步=K（`SetQuestProgress` 0xF0），乱序零写零回发；③ 1009 → 推进（0x100）+ 报告通道（mgr+0x1b0）+ 完成页（mgr+0x5d8）；④ 1008 → 仅完成页；⑤ 10255 → 推进 + 完成页；⑥ 其余 ≥1000 → 回显页；<1000 → 返回 |
| ItemPlay | `FUN_180c46e90`（注册事件 5，物品 id 键控） | 状态必须 = 3（START）+ 当前步 kind==3 + 物品 id 匹配 → 组 1 `(vars>>6&0x3f)+1 < target` 则 `vars += 0x40`；满组 → 步进 / 末步 `SetQuestSuccess`；载荷 = 物品名（可选 `, N` 计数，装载器缺省 1） |
| 距离闸门 | Hunt/PvP 共享前奏（行字段 +112，int，缺省 0） | <0 无闸门；0/1 = 2500+同图+队伍/同盟；2 = 10000+同图；5 = 40000+同图或 raid；6 = 仅 40000。**DD 表无来源列**（`LoadBasicInfo` 只解析 id/category_acquire_/name/dev_name/value0_acquire_/reward_npc_name/con_quest/con_quest_list）⇒ DD 行恒 0；但 mgr+0x98/+0xa0/+0xa8 语义与 param_6 单位未坐实 ⇒ **仍不实现**（§10.3-#24② 收窄，步 f 前闭合） |

## 2. 载荷名裁定（171 个失败名，0 换算）

- 探针（`p7/tools/dd-unresolved-name-probe.py`）对切换集 1467 行的 hunt/talk/collectitem/talkfobj/
  itemplay/enterarea 载荷逐名复算解析序，对 HEAD 台账（17 行）得 **171 个未解析载荷名**，逐名裁定：
  - **153 个 = 真端 `quest_ai_name` 组**（retail `npcs.xml` 子元素 `<quest_ai_name>`，本仓 npc 模板无该
    属性，且**不是**旧车道组表已声明键）→ 按组展开全成员 id 入过渡台账 `retail-npc-name-aliases.tsv`
    （与 SimpleHunt 家族 P0a 同轴）；
  - **10 个 = 组表已声明键**（`ab1_blv6_d01/d02/l01/l02`、`event_npc_idsolo_s4/s5`、
    `event_npc_idsweep_splus`、`event_npc_miniring`、`npc_event_goldstar/ppoba`）→ 权威载体 =
    旧车道组表 `quest/retail-quest-ai-name-groups.tsv`（成员展开与探针提取**同集**，逐组验证），
    **不进台账**——台账行会把组键塞进旧车道 byName（spawn 通道）破坏通道互斥闸；原生车道经
    `NativeNpcNameResolver` 新增的**组通道兜底**（byDesc → byName → 台账 → 组展开）直读同一张表；
  - **8 个 = 击杀目标等价集**（`ab1_1131_boss_dr_q1737` / `ab1_1221_boss_dr_q1739` / `idseal_boss_vritra_q18952`
    / `ldf5_fortress_7011_boss_dr` 等，全部是 hunt 步）→ 正确轴是 P0a 家族裁定的击杀目标等价集
    （`RetailNpcNameIndex.addMonsterTargetAliases` 硬编码集），**不是** quest_ai_name 组员（两轴 id 集不同：
    如 ab1_1221_boss_dr_q1739 = {266306,266311} vs quest_ai_name 组 {267811..267815}）→ 台账 8 行按
    硬编码裁定集入账。
- 台账数据行 17 → **178**（+163 quest_ai_name + 8 击杀目标 − 0；首轮曾误入 10 个组表键，已移除）；
  证据 TSV：`p7/dd-unresolved-name-adjudication.tsv`（153+10+8 逐名分类）。
- 终态：探针复算**未解析载荷名 = 0**；唯一不可解析面 = 切换集行引用的 LF6 真端缺席进区别名
  （§10.3-#23 冻结，9 行 ZONE_ABSENT）。

### 2.1 本批事故与修复（QE-133）

- **第一形态（硬编码撞键 → load 重试风暴）**：首轮探针缺 `addMonsterTargetAliases` 镜像 ⇒ 8 个 boss 名
  被错按 quest_ai_name 入账 ⇒ 旧车道 `RetailNpcNameIndex.addVersionedNpcIdAliases` 的
  `putIfAbsent != null` **重复闸抛异常** → `RetailQuestDriver.load()` 恒失败；而 `metadataOf` /
  `collectSlotRequirements` 两调用方 `catch IOException → 记未解返回`，测试每构造一个 handler 都重试
  `ensureLoaded` ⇒ **全量 load 无限重试**（jstack/JFR 取证：模板正则 → 剥前缀别名 → xerces DOM 循环
  往复，族门 20 分钟不出一个类）。修复：台账 8 行改击杀目标裁定集 + 装载器**等集幂等**（等集跳过 /
  异集抛）+ 探针补 `load_monster_target_keys()` 镜像。修后金丝雀：CollectItem 族门单类
  **26.5s / 14 绿**（修前卡死 >20 分钟）。
- **第二形态（组表撞键 → 通道互斥被夺）**：10 个组表已声明键被写进台账 ⇒ 旧车道 byName 出现组键 ⇒
  `RetailQuestAiNameGroupGateTest`（通道互斥/形态判据）与 `QuestEventShardRetailAlignmentTest`
  （50073 报告 NPC [835570,835571] 被夺成 []）红。修复：10 行移出台账 + `NativeNpcNameResolver`
  增组通道兜底（同表同集，DD 侧解析零变化——镜像复算分桶不变 1458/9 佐证）+ 探针补
  `load_declared_group_keys()` 镜像。
- **连带：DD IR 指纹 4 行重冻**：台账新别名让旧车道 DD 编译器补全了全图击杀名的展开 ⇒ 13841/23841
  （+14 边）、15473/25473（+80 边，`Ab1_*_Boss_*_all` 族）的 IR 指纹漂移。逐行 diff 归因后按标准流程
  重冻（`-Dretail.dataDriven.fpOut` 再生 → `src/test/resources/quest/retail-data-driven-ir-fingerprints.tsv`）。
  **这是本批唯一的生产行为面变化**：这 4 个任务的旧车道击杀目标集从「部分解析」变为「真端全图击杀集」
  ——名字裁定把真端事实同时带给两条车道的直接结果，聚焦套件对步 d 基线零红类漂移佐证无回归。
  新 DD 运行时本身仍零行为变更（生产路由集为空）。

## 3. 落地物

| 交付 | 说明 |
|---|---|
| `src/main/java/.../tablelane/DataDrivenNativeRuntime.java` | 新增：`itemPlaysByItemId` 兴趣面 + `onItemAcquired`（真端事件 5 语义：步号/kind/物品 id 三重匹配 + 组 1 计数 + 满组步进/收口）；`onDialog` → `dispatchDialog`（共享对话平面：开页/顺序页动作/1009/1008/10255/回显；`requestedOwner` 单 owner 口径）；TALK+COLLECT_ITEM 合并 planRow 分支（`parseGroups` → `resolveMonsters` 组感知 → talk 兴趣）；TALK_FOBJ 改组感知解析；`create(...)` 增第 5 参 `RetailItemNameIndex`（路由时必需） |
| `src/main/java/.../questEngine/QuestEngine.java` | 两条 hook：`onItemGet` 前置 `onItemAcquired`（:1018）；`onDialog` 在 typed owner 之前 `onDialog(...)`（:304）。生产路由集为空 ⇒ 恒 false |
| `src/main/java/.../retail/RetailNpcNameIndex.java` | `addVersionedNpcIdAliases` 等集幂等守卫（等集跳过 / 异集抛，见 §2.1） |
| `src/main/java/.../tablelane/NativeNpcNameResolver.java` | 组通道兜底：直读旧车道组表 `quest/retail-quest-ai-name-groups.tsv`，组键按成员 name_desc/name 展开（声明序）；解析序 = name_desc → name → 台账 → 组 |
| `src/main/resources/.../retail/retail-npc-name-aliases.tsv` | +171 行（163 quest_ai_name 组 + 8 击杀目标裁定集），批次注释行标记 |
| `src/test/resources/quest/retail-data-driven-ir-fingerprints.tsv` | 4 行重冻（13841/23841/15473/25473，见 §2.1 连带） |
| `src/test/java/.../tablelane/DataDrivenNativeRuntimeGateTest.java` | 9 → **12 例**：+对话平面三态（开页/乱序静默零写/顺序步进）、+1009 末步推进开奖励窗（页 5）、+ItemPlay 物品计数（同 id 自增、异 id 零动作）；⑨ 分桶冻结更新为 1458/9 与逐类步数 |
| `p7/tools/dd-unresolved-name-probe.py` | 裁定探针：+collectitem/itemplay/enterarea 载荷轴、+`--aliases` 覆盖（证据复现）、+`--emit-aliases`、+`load_monster_target_keys()` 击杀目标轴镜像 |
| `p7/tools/dd-planrow-mirror.py` | planRow 精确离线镜像（八类逐分支），与 Java 实测分桶逐值对拍——本批分桶权威值由此裁定（旧口径复算差 1 行系镜像不完整，见 §4） |

## 4. 分桶与冻结面（本批口径）

- 切换集 **1467 行**；本批 **可路由 1458 行 / 冻结 9 行（全部 `ZONE_ABSENT`，§10.3-#23）**；
  `KIND_NOT_WIRED` / `NAME_UNRESOLVED` 两桶**清零**。
- 已路由行逐类步数：hunt **821** / collectitem **345** / pvp **207** / talk **398** / enterarea **137** /
  itemplay **42** / enterworld **34** / talkfobj **18**（镜像与 Java 双算一致）；
  切换集逐类步数与 P7 步 1 契约逐值一致（hunt 827 / collectitem 348 / pvp 207 / talk 403 / enterarea 153
  / itemplay 42 / enterworld 34 / talkfobj 19）。
- **对拍注记**：早先一版离线复算给出 talk 399 / collectitem 346（与 Java 实测差 1）——经精确镜像
  （`dd-planrow-mirror.py`，含 parseGroups 尾整数剥离与空白回退）复算，权威值 = **398 / 345**，Java 无误。

## 5. 门态（本批显式命令）

```
# 新门 + 算术门
mvn -o test -Dtest='DataDrivenNativeRuntimeGateTest,DataDrivenProgressTest' -DfailIfNoTests=false
# → 12 + 9 全绿
# 族门 + tablelane
mvn -o test '-Dtest=*FamilyGateTest,*RowAlignmentGateTest,*InventoryGateTest,NativeQuestTableLoaderTest,*RewardClaimGateTest,*NativeTalk*Test,NativeNearbyQuestAxisGateTest,SMNearbyQuestsPacketTest,NativeAcceptEntryAskFlowGateTest,DataDrivenNativeContractGateTest,DataDrivenProgressTest,DataDrivenQuestTableGateTest,DataDrivenEnterAreaPortGateTest,DataDrivenNativeRuntimeGateTest,MultiCellSensoryZoneRegistrationTest,RetailEnterAreaZoneRegistrationGateTest' -DfailIfNoTests=false
# → 183/183 绿（步 d 基线 180 + 新门 3），BUILD SUCCESS 1:05
# 聚焦套件
mvn -o test '-Dtest=*Quest*Test,*Retail*Test' -DfailIfNoTests=false
# → 1703 / 161F+137E / 105 红类（对步 d 基线 ADDED 0 / REMOVED 0 / changed 0）
# DD IR 指纹门（重冻后）
mvn -o test -Dtest=RetailDataDrivenGateTest -DfailIfNoTests=false
# → 9/9 绿（重冻前 4 行漂移：13841/23841/15473/25473）
```

日志：`gates/2026-10-02-p7-step2d2-family-tablane.log`、`gates/2026-10-02-focused-run-p7-step2d2.log`（临时，不入库）。

## 6. 反漂移规则（本批新增）

1. **handler 归属以装载器注册面为准**（QE-132）：邻近函数不作归属证据；死代码判据 = 行字段恒值 +
   装载器不写。
2. **动作码从守卫恒等式推导**（QE-132）：`code-9999 == 当前步+1` ⇔ 动作码 = 10000+当前步；测试动作码
   不从直觉推导。
3. **镜像复算必须覆盖装载器全部解析轴**（QE-133）：模板名 / NPC_ 剥前缀 / 硬编码击杀目标 / 台账，缺一轴
   裁定即会错轴。
4. **同名事实多载体收敛**（QE-133）：台账重复行等集幂等、异集 fail-closed；装载失败必须快速失败或缓存
   失败标记，禁止 catch-当未解 + 调用方重试的组合。
5. **击杀目标名用击杀目标等价集轴**（QE-133）：boss / 要塞神长 / AllKill 名不按 quest_ai_name 组展开。

## 7. 步 2 剩余

- **步 e**：6 类接取 kind + 通用附加动作执行面（列 1..10）。
- **步 f**：1467 行原子切换 + 同批删除 DD 编译器与 zone/AI 台账读取 + typed 车道入口页残余随车道删除
  （§10.3-#22）；距离闸门取值来源（§10.3-#24②）须在步 f 前闭合。
