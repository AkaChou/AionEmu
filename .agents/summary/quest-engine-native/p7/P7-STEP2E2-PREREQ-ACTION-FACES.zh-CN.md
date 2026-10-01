# P7 步 2 步 e2 前置：剩余附加动作宿主渲染面 + con_quest 语义 + 挑战哨兵（真端原码坐实）

> 日期：2026-10-02。分支：`quest`。性质：只读真端原码考古（步 e2 实现前置）。
> 证据根：`<真端根>/MainServer_ScriptDLL64/ScriptDLL64.c`（DLL 反编译）、
> `<真端根>/server58-source/NPCServer_NPCSvr64/` 与 `MainServer_Server64/`（宿主反编译源树，
> 方法同 `p7-prereqs/dd-host-interface-detail.md`）、`<真端根>/Map/XML/strings.xml`（真端字符串表）。

---

## 1. 动作执行矩阵（本次逐 handler 复核，**纠正 e1 的一处执行时机偏差**）

| 进度 kind | handler | 推进边执行动作？ | 证据 |
|---|---|---|---|
| 2 Hunt | `FUN_180c46020` → `FUN_180c4d190` | **是** | 步 d 已证 |
| 1 CollectItem | 对话平面 `FUN_180c474b0` | **否**——推进/完成只发 `+0xf0`（SetQuestProgress）/`+0x100`（SetQuestSuccess），**无任何执行器调用**（C:2071069-2071274 全文无 `FUN_180c4c*50`） |
| 4 Talk | 同上 | **否**（同上） |
| 3 ItemPlay | `FUN_180c46e90` | **否**——推进边只发 0xf0/0x100；尾部 `FUN_180c4cd50` 只在**接取分支**（C:2070878） |
| 5 PvP | `FUN_180c46980` | 无动作面（装载 guard：PvP 无附加动作列） |
| 6 EnterArea | `FUN_180c47bf0` | **是**——推进边 `+0xf0/+0x100` 之后 `FUN_180c4c8d0`（C:2071475） |
| 7 EnterWorld | `FUN_180c467b0` | **否**——推进边只发 0xf0/0x100；尾部 cd50 只在接取分支（C:2070479） |
| 9 TalkFOBJ | `FUN_180c478e0` | **是**——组走满后的推进/完成边调 `FUN_180c4c8d0`（C:2071395） |

**接取侧**（state 非 START 分支）执行步 0 动作：ItemPlay/EnterWorld（cd50）、EnterArea（c8d0）、
LevelUp/LevelUpLogIn（`FUN_180c46bb0` 尾部 `(+0xd8)` 成功 → `FUN_180c4cd50`）；**Talk 对话接取
（`FUN_180c47220` 1002/20000）= 收尾 helper `FUN_180c4d5b0`，不执行动作**（e1 实现一致）。

⇒ **e2 修正**：DD 行的推进边动作只在 kind ∈ {Hunt, EnterArea, TalkFOBJ} 执行（e1 的 apply() 对全部
kind 在推进边跑动作 = 偏真端多执行，零行为变更批内无生产影响，e2 修正 + 门禁改按此断言）。

## 2. 剩余 case 宿主渲染面（宿主反编译源逐函数）

| case | 宿主槽/函数 | 渲染语义（宿主源实证） | 本批落面 |
|---|---|---|---|
| 3 Teleport | `IUserImp::Teleport`（+0x2d0，NP `NpcAIOrderFunc.cpp`） | `(world, x, y, z+1, heading, 1)`：z 抬 1.0 落地；heading=度（宿主内部 `360/3` 转 6 位朝向） | **落面**：`NativeTeleportPort` → `TeleportService2.teleportTo(player, world, x, y, z+1, (byte)(deg/3))` |
| 5 Spawn | `IUserImp::Spawn`（+0x180，NP impl C:3005-3260） | 节点 = `[type, npcId, count, time]`（Relative）/ `[+x,y,z,heading]`（Absolute，坐标经 wcstoul 截断小数，如 `83.9`→83）；执行 = 每只 `World_CreateNPC`：Relative → 玩家位置随机可行走点半径 5（`World_RandomWalkableSpawnLocation`），Absolute → 精确坐标；time 入刷怪单 | **落面**：`NativeSpawnPort` → `SpawnEngine.addNewSingleTimeSpawn` + 定时回收（随机可行走点校验随步 f 激活批坐实） |
| 7 Message | `IUserImp::Say`（+0x2b8，NP impl C:5784-5880） | NC_SAY_CODE 包：说话者 = 玩家自己、频道 say、文本 = **字符串表 id**（装载期 `XML_ParseStringIndex` 从 `STR_` 键解析，未解析 = 装载失败） | **落面**：键→id 表 `retail-quest-string-ids.tsv`（源 = 真端 `Map/XML/strings.xml` 逐键提取，6 键全命中；先例 = 生产在用的 `quest_name_string_ids.tsv`）+ `NativeSayPort`（本服以 `SM_SYSTEM_MESSAGE(id)` 转发 id，客户端自解文本；表现通道 = 系统消息 ≠ 真端 say 气泡，偏差已记录） |
| 9 EnterInstance | `IUserImp::EnterInstance`（+0x220，NP impl C:4585） | 仅发客户端包 `{objId, creationId, 0}`——进副本由**客户端**按 creation id 发起；世界 id/离开进度写 def+0x48/+0x44（离开推进机制），节点只带 creationId；creationId→世界映射在客户端表 | **未落面**（ACTION_UNFACED 保持）：本服无该客户端导向面，映射表未坐实 |
| 10 Timer | `IUserImp::AddQuestTimer`（+0x250，NP impl C:4802） | `(ms=time×1000, ctx{defId, vec[1], step})`；`vec[1]==1` → `User_AddVisibleQuestTimer`（客户端倒计时），否则不可见定时器；**到期分发面未坐实**（谁收、到点检查 dest 步（def+0x6c）后做什么） | **未落面**（ACTION_UNFACED 保持） |
| 6 Delay | def+0x270（`IOneQuestScriptNpc` 虚槽） | 未定名；**只在 EnterArea/TalkFOBJ 步活**（cd50/d190 无 case 6 = 其余 kind 真端装载即不执行）；活数据仅 2 行（10035 enterarea=8、25606 talkfobj=2） | col6/col8 其余 kind = **镜像忽略**（真端死列）；enterarea/talkfobj 步上 = ACTION_UNFACED 保持 |
| 8 Message-8 | def+0x3a0（同上未定名） | 活数据 **0 行** | 同 col6 规则（c8d0 步未落面 / 其余忽略） |

数字 token 解析器 = `FUN_18107c0f0` = `wcstoul(base 10)`：**前导整数截断**（`1938.0`→1938、`83.9`→83），
Java 镜像同截断语义。

## 3. con_quest / con_quest_list 语义（ACQUIRE_CONDITION_UNFACED 桶裁定）

- 解析面（`LoadBasicInfo` 尾段 C:2072560-2072606）：`con_quest` → 原文存 **def+0x38**；
  `con_quest_list` → `XML_ParseStringIndex`（字符串键→id，"Data Driven - ZoneQuestList"）存 **def+0x3c**。
- **0x640 接取条件表只收字面量条目**（`{type=0,value=-1}` 可接取对象 / `{type=4,value=-1}` 交付对象 /
  步向量空时 `{type=3,value=0}`；`FUN_180c4df60` 写入）——**con_quest 两列从不进条件表**。
- def+0x3c 唯一消费方 = `FUN_180c46590`（def+0x198 元数据槽，C:2070260-2070290）：遍历接取注册树后
  `(+0x170)(user 派生对象, def+0x3c)` = **区域任务列表注册**（任务目录/追踪器显示轴）；def+0x38 同族
  （`FUN_180c464e0`，原文变体）。
- **裁定**：两列 = 任务目录显示元数据，**不是接取闸门** ⇒ `ACQUIRE_CONDITION_UNFACED` 35 行解冻；
  显示注册面归 §10.3-#18（native 行附近任务/目录轴，与各家族同轴），行模型继续装载两列。

## 4. 挑战接取哨兵 `_challengetask_`（NAME_UNRESOLVED 6 行裁定）

- 数据形：6 行（17160/17161/17162/27160/27161/27162）`acquire = Talk` + `value0_acquire_ = _challengetask_`
  + `reward_npc_name = LF5_Atmos_E / DF5_Haldor_E`（各 3 行，本仓名字索引可唯一解析）。
- 旧车道 P0c-58 四源裁定（客户端 npc 块 `quest_ai_name`==reward 名 + 入口页 4762/按钮 20000/20001 +
  遗留 `NPC_START`+同 id `npc-complete`）= 接取 NPC 就是交付 NPC 本人。
- **裁定**：哨兵面 = `AcquirePlan(kind 4, npcIds = resolveMonsters(reward_npc_name))`，对话词汇与普通
  Talk 接取完全同路（`dispatchAcquireDialog`）；解析失败仍 fail-closed NAME_UNRESOLVED。

## 5. e2 分桶预测（离线镜像 `p7/tools/dd-planrow-e2-mirror.py` 复算后冻结）

- 解冻：ACQUIRE_CONDITION_UNFACED 35 + 挑战哨兵 6 + 动作面收全（TELEPORT/SPAWN/MESSAGE 已落 且
  col6/col8 仅在 c8d0 步冻结）的行。
- 保持 ACTION_UNFACED：携带 col9（EnterInstance，3 行）或 col10（Timer，10 行）或 c8d0 步 col6
  （2 行：10035/25606）的行。
- ZONE_ABSENT 9 维持（§10.3-#23）。
