# 噩梦副本（克萝梅德试炼）马加之药水 730308 点击无传送

> 状态：**实现完成，门禁已过（7 类全绿：Maga_Potion 5/5、PortalDialogAI2 5/5、KromedesTrialInstance 12/12、Quest18602 对齐 4/4、AI2 注册契约 1/1、QuestEngineNpcDialogDispatch 6/6、AcceptProtocol 2/2），待实机验收**
> 日期：2026-10-07
> 关联：quest 18602《噩梦》(Nightmare In Shining Armor)、副本 300230000、AI `maga_potion_1`、`portal_dialog`（205229/205234）

## 1. 报障与现象

用户报障：「噩梦副本，点击 npc 730308，没有传送」。

实机日志（`log/quests.log` 2026-10-07 21:03–21:04，玩家 Kk）：

```
21:03:05 [C->S] CM_DIALOG_SELECT npcId=205229 动作=10000                    ← 点兰尼尼亚「进入恶梦」
21:03:05 [实例] <副本进行中>300230000 ID=2，所有者=151512                     ← 仅创建实例，无任务步推进包
21:03:54 [S->C] SM_DIALOG_WINDOW targetObj=157632 questId=0 下发页=1011      ← 点药水：双参页（questId=0）
21:03:55 [C->S] npcId=730308 动作=1012 → [S->C] questId=18605 下发页=1012    ← 客户端按 18605 渲染该页
21:03:58 [C->S] npcId=730308 questId=18605 动作=1007 → （零响应，共 5 次）
```

玩家看到的按钮是 18605《噩梦的碎片》的对话（"询问是什么时候"/"让他把烙印之石拿出来"），点 1007 后服务端完全静默——**交互死锁，更无传送**。

## 2. 证据链

| # | 证据 | 结论 |
|---|---|---|
| 1 | `log/quests.log` 时间线（上） | 21:03:05 副本创建但**无 18602 同步包** ⇒ 兰尼尼亚的传送走了 Portal2Data（questId=0 跳过任务引擎），任务步未推进 |
| 2 | `fun_893.cpp:3989 FUN_180f859b0`（真端，注册 0x48aa=18602） | 点 10000(SETPRO1)：关窗 + **设 18602 步=1** + 进副本；条件不满足 → 页 1012（组队提示页） |
| 3 | `fun_896.cpp:443 FUN_180f961e0`（真端 730308） | 非 10001 动作 → 通用分发；**10001(SETPRO2)：只看钥匙**——有钥匙 → 关窗+步=2+扣钥匙+**按 location_alias 传送**；无钥匙 → 页 10001 |
| 4 | `58Server/Map/Worlds/idcromede/world.xml:392` | `idcromede_alias_02 = (687.631104, 675.972412, 201.040802, dir 270)` =「宅邸」落点（`fun_896.cpp:467` 0x1e0/0x1c6 别名传送调用；`fun_722.cpp:3417` 同） |
| 5 | `fun_731.cpp:4041 FUN_180caf150` + `:4155 FUN_180caf460` | 通用动作分发：SETPRO 族顺序守卫（`码-9999 == 步+1`）；1007 → 接取窗；1002 → 页 1003；**其余 ≥1000 原样回发**；1008 → 关窗；页轴按步分发（1→1352 等） |
| 6 | `<客户端解包根>/data_unpacked/Dialogs/10000_19999/quest_q18602.html` | select2(1352)"（水晶球…）"→「仔细查看水晶球」(1353)；select2_1(1353)"（必须拿出马加之药水，赶快到卡里加的宅邸去。）"→「拿出药水，瞬间移动到宅邸」(10001)；check_user_item_fail(10001)"（无法打开水晶球…需要钥匙）" |
| 7 | `Quest18602ClientDialogAlignmentTest` + `retail-quest-ai-registrations.xml:2002` | 18602 ∈ 205229/217005/217119/700939/730308 的 Quest-AI 注册集（730308 的脚本即按 18602 硬编码） |
| 8 | `portal_template2.xml:2169` / `portal_loc.xml:453` | 205229/205234 的 `dialog=10000` portal path → `portal_loc 3002300 = (244.98566,244.14162,189.52058)`，**与 18602.xml 的 SETPRO1 任务侧传送坐标完全一致** ⇒ 修复 F1 后进副本落点零变化 |
| 9 | `log/quests.log` 当日记录：21:02 玩家在副本外接取 18605/18606/18607/18617；全天无 18602 交互包 | 玩家「在 18602 流程中」= START 但 **var0=0**（钥匙来自副本怪 216968 掉落/恢复），故药水 AI 的 var0==1 判断不成立 |

## 3. 根因

**F1（步骤不推进）**：`PortalDialogAI2.onDialogSelect` 只对 `questId > 0` 进任务引擎；客户端在对话按钮上不带 questId（=0）⇒ 兰尼尼亚的「进入恶梦」(10000) 被 `PortalService.port` 抢走（portal 传送），**18602 的 `started + SETPRO1 → var0=1 + teleport-player-next-available-instance` 永不执行**。玩家在副本里始终 var0=0。

**F2（页面错位 + 静默）**：药水 AI 对「非 var0=1 但有钥匙」发**双参页 1011（questId=0）**⇒ 客户端按玩家进行中的 18605 渲染该页 ⇒ 玩家点到 18605 的按钮（1012/1007）⇒ 1007 无人认领（18605 START 且 730308 非其 NPC）⇒ 零响应。

**F3（接取面空白，2026-10-07 22:07 实机复现）**：18602 未接（前置 1527 已完成、条件满足）时点兰尼尼亚，`PortalDialogAI2.checkDialog` 的 `playerCanStartQuest` 分支发**页 10、questId=0**（双参 `SM_DIALOG_WINDOW(objectId, 10)`）；客户端此时没有进行中任务可映射页上下文，渲染为**空白对话**——玩家「接取不了、没有这个任务显示」。日志链路：22:07:34 `[S->C] questId=0 下发页=10`（空白）；22:07:35 点节点残留的「进入恶梦」10000 → 引擎 openDoorReplay 找不到 18602（**未接不在玩家任务列表**）不认领 → 落 portal 传送（无 `SM_QUEST_ACTION`，var0 不推进）；22:09:09 药水 10001 同理走兜底（扣钥匙 + 传宅邸，任务不推进；Kk 随后在宅邸 spawn 217035 宅邸守卫可证）。**结论：Kk 当时身上没有 18602**。真端语义：这些页由任务脚本发出且**必带 questId**（未接 → select_none 4762、进行中 → select1 1011、可交 → 领奖页）。

## 4. 修复内容

| 文件 | 改动 |
|---|---|
| `ai/portals/PortalDialogAI2.java` | 新增：`questId==0 && dialogId==SETPRO1 && isKromedeTrialEntryNpc(npcId=205229/205234)` → **先经任务引擎**（认领即 var0=1+任务侧传送；不认领且已在 300230000 时不再走 portal，防二次传送）；新增包私有静态 `isKromedeTrialEntryNpc(int)` |
| `ai/portals/PortalDialogAI2.java`（F3） | `questFirstDialogIds(205229/205234)` → `[QUEST_SELECT]`：**开门对话先经任务引擎重放**（`checkDialog` 既有机制，裂缝球先例）——未接 → select_none 接取页 4762、进行中 → select1、可交 → DEFAULT_SUCCESS，全部携带 questId；旧页轴（页 10 双参/传送页）仅在引擎不认领时兜底。引擎候选分发不要求已接（`QuestEngineNpcDialogDispatchTest` 冻结该不变量） |
| `ai/instance/kromedesTrial/Maga_Potion_Temple_VaultAI2.java` | ① 入口页：`var0==1 || 持钥匙(185000109)` → **1352 + questId=18602**（契约 fail-closed；消除旧双参 1011）；无钥匙 → 27。② 兜底：SETPRO2 只看钥匙——有钥匙 → 扣 1 把 +（START 且 var0==1 时）写 var0=2 + `SM_QUEST_ACTION.updateQuest` + `synchronizeRobstinNpc` + **传送到真端别名点 (687.631104, 675.972412, 201.040802, h=270)**；无钥匙 → 契约失败页（`checkFailPage(18602)`，10001）。③ 新增 SELECT2_1(1353)/SELECT1_1(1012) 契约回发、FINISH_DIALOG(1008) 关窗。④ 移除旧「SETPRO1 也传送」兜底（真端在药水处对 10000 无传送语义） |
| `instance/handlers/scripts/KromedesTrialInstance.java` | 新增 `public applyKromedeTransformation(Player)`（按 `player.getRace()` 选 19220/19270）；`onEnterInstance` 改用之；**删除恒 null 的 `skillRace` 死字段**（旧实现所有玩家被给天族变身） |
| `quest/definitions/quests/18602.xml` | SETPRO2 成功分支传送坐标 `687.56116/681.68225/200.28648 h=30` → **`687.631104/675.972412/201.040802 h=90`**（真端别名点；dir 270° 按服务端压缩 byte 换算 ÷3 = 90，附双语注释） |

**测试**：`Quest18602ClientDialogAlignmentTest`（坐标断言同步）；新增 `Maga_Potion_Temple_VaultAI2Test`（源码/常量断言：questId 页、扣钥匙、var0=2、罗勃斯汀同步、别名点、失败页、翻页/关窗）；`PortalDialogAI2Test` 增补（`isKromedeTrialEntryNpc` + 引擎优先分支断言）。

## 5. 坐标结论修订记录（重要）

反编译初读曾把 `(653.0, 774.0, 216.0)` 当作玩家传送目标（浮点位模式 0x44234000/0x44418000/0x43580000）。**经核实该参数组属于同一函数中另一调用**：`0x268(…, 0x44de9=282089, …, &{653,774,216}, …)`，而 **282089 = 副本隐形 NPC `IDCromede_Invisible_NPC7`**（`npc_template_270058_286320.xml:60033`）——该坐标是**它的生成位置**。玩家真正的落点来自 `0x1e0/0x1c6 + L"IDCromede_Alias_02"`（别名传送命令）= `idcromede_alias_02`。旧代码 (687.56116,681.68225,200.28648) 与别名点接近（x/z 差 <1，y 差约 5.7），属旧近似值。
另：**真端 SETPRO2 无变身调用**（0x1e0/0x1c6 均为别名传送命令）——变身由「进入副本时」承担（`onEnterInstance` 已有，对应剧情"进入恶梦之后变身成克罗梅内"）。

**朝向单位修订记录（门禁红灯暴露）**：首版把真端 `dir 270`（度）直接写进 `heading` 字段，`Quest18602ClientDialogAlignmentTest` 立即红灯——`quest_definition.xsd` 中所有 `heading` 属性为 `xs:byte`（≤127），且全库取值 ≤120。核实约定：服务端 byte heading = **度 ÷ 3 压缩**（`HouseObject.setHeading((byte) Math.ceil(rotation / 3f))`；120 × 3 = 360），真端 world.xml `dir` 为度数（值域含 315/245 等非 3 倍数值）。故 **270° → heading=90**，已同步修正 XML、`HOME_HEADING`、两处测试断言。同批还遇到一次资源同步竞态（测试 classpath 读 `target/classes` 旧副本，报错仍为 270；Make 拷贝完成后复跑即绿）——非代码问题。

## 6. 待验证（实机）

0. **接取面（F3）**：未接 + 前置已满足时点兰尼尼亚 → 接取对话（"这孩子，好像有很多话想说" +「继续听。」）→「接受」→ `SM_QUEST_ACTION 18602 状态=START`；进行中再点时 → select1（「进入恶梦」）；
1. 点兰尼尼亚「进入恶梦」→ `SM_QUEST_ACTION 18602 步数=1` + 进副本（落点不变）+ 任务追踪跳"去封印室寻找马加之药水"；
2. 有钥匙点药水 → 18602 的"水晶球"页（不再是 18605 对话）→「仔细查看水晶球」→「拿出药水…」→ 扣 1 把钥匙 + 传送别名点 + 任务跳第 3 步；
3. 无钥匙点药水 → "（无法打开水晶球。）…需要钥匙"失败页；
4. 已完成 18602 的持钥匙玩家 → 同样可走"查看水晶球→传送"。

**风险点**：别名点/朝向为真端数据直抄，确认落点合理（不卡墙、朝向自然）；异常则微调并回写本文档。

## 7. 边界（本次不做）

- `Maga_Potion_Manor_EntranceAI2`（730341，同类双参页模式）、`Specialize01PortalAI2`（兄弟类同 questId>0 门槛）——记录待评估；
- 28602（魔族镜像）无服务端 XML；730308 AI 只认 18602（F1 对 205234 亦生效，但缺 28602 XML 时引擎不认领）；
- 真端兰尼尼亚"组队中→页 1012"分支未实现（依赖未确认条件）；
- 真端 SETPRO2 伴生的 `Invisible_NPC7` 生成与模拟器缺失的 idcromede location_alias 数据（`ai-location-aliases.xml` 无 300230000）——以字面坐标落地，未引入别名系统。
