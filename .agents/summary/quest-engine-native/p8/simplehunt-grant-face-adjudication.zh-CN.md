# §10.3-#25 裁定笔记：SimpleHunt 哨兵发放宿主面（faction / area）

- 日期：2026-10-02（P8 第二刀）
- 问题：SimpleHunt `_faction_` / `_area_` 行在真端的发放分发路径是什么？与 Talk/Collect 是否同构？
- 约束：禁止按 Talk/Collect 形态类比——以下每条都落到反编译源函数。

## 1. 数据面（先证「发放面在真端真实存在」）

| 轴 | 数据 | 结论 |
|---|---|---|
| SimpleHunt 哨兵行 | `Quest_SimpleHunt.xml` 1865 行中 `_faction_` **127** 行 / `_area_` **17** 行 | 哨兵人口存在 |
| 阵营池 | `npcfactions_quest.xml` 436 行；**125/127** faction 行在池内（如 35009/35016/35023 → 池 `GuardianOfDivine`）；`npcfactions ∩ 全 hunt 行 = 同一批 125` | 阵营轮换系统**确实包含** SimpleHunt 行 |
| 区域绑定 | `ai-areas.xml`（P0c-4 从真端 world files 补齐）324 个绑定 id 中含 **13/17** area 行（39005/39007/39009/49005/49007/49009/12505/12524/16972/18033/22524/26972/28033） | 区域发放绑定**确实存在**（余 4 行真端本就未绑定 = 死边镜像） |

## 2. Faction 宿主面（逐函数，`server58-source/MainServer_Server64`）

1. 装载：`NpcFactionDB::Load`（NpcFactionDB.cpp:40）先读 `npcfactions.xml`（势力记录），:717 读
   `npcfactions_quest.xml`（根校验 :724）；每行 `npcfaction_name` 解析到势力记录，**星期位写入
   势力记录的 7 槽任务向量**（:851-873，`FUN_1406e5fd0(list + weekday*3 + 5, questId)`）。
2. 轮换：`NpcFactionDB::GetTodayFactionQuest`（:1409，0x140cf9450）= `questListBase + tm_wday*3 + 5`
   取当日向量；日界 = 当地零点（`GetTodayUpdateTime` :1526）。
3. 发放决策：`User::CheckNewFactionQuest`（User.cpp:151575）按类别槽（dailyquest/mentor）双轮扫描 →
   对每个过期/缺省势力调 `GetTodayFactionQuest`（:151849）→ `User_CheckQuestAcquireCondition`
   逐行过滤（:151862）→ 均匀随机挑选（:151931）→ `User_InitFactionQuest`（:151932，0x1405f1220）：
   写势力槽 `{questId, state=2, now}` + DB 包 0xaeff3c + 客户端包 0xfe7b。
4. 受理：玩家从势力 UI 受理 → DLL `cab520`（fun_731.cpp:1667）**只按任务状态分支**（0x3ea/20000 等），
   `IUserImp+0xd8`（SetQuestAcquired）→ `AddQuest` → `UserQuestData::AddQuest`
   （UserQuestData.cpp:219-221）`type==3` → `User_AcquireFactionQuest`（state 3）。
5. **家族分支 = 零**：`Quest::CanAcquireQuest`（Quest.cpp:136，0x140d5a960）只含等级/职业/前置/重复/
   势力加入等通用闸（:497 = 势力加入检查），无 hunt/talk/collect switch。类别枚举与家族正交——
   装载器凡见 `npcfaction_name` 即强制 `category=3`（fun_249.cpp:4065-4078）；真端 quest.xml:204902
   的 35009 行实载 `<category1>faction</category1>`。
6. **DLL 无哨兵逻辑**：全 ScriptDLL64 grep `_faction_`/`_area_`/`_challengetask_` 零命中；
   `acquired_npc_name` 在 DLL 内只是 record+0x28 的不透明字符串（fun_912.cpp:1155-1170）。
   「系统发放」完全是宿主侧薄记，DLL 按普通任务受理。

**判定**：阵营发放链**与家族无关**——SimpleHunt 行与 Talk/Collect 行走同一
星期位 → 资格过滤 → 随机挑选 → 槽建档 → 受理链。

## 3. Area 宿主面（逐函数）

1. 装载：世界文件 `<questscript_area><name/><quest>39005</quest>`（df2a/world.xml:1089）→ 两宿主
   `World::LoadObject case 0x894`（MainServer World.cpp:33130 / NPCServer World.cpp:5319）→
   `QuestArea` 构造（QuestArea.cpp:48，名称 0x1003F 哈希为区域 id :170-181，quest 向量 :+0xF0）→
   `NpcScriptMgr_AddQuestArea`（NpcScriptMgr.cpp:1765，按区域 id 的有序树；查询 :1664）。
2. 触发：`User::MoveNew`（User.cpp:84454）逐移动把所在区域入队 `User+0xEBF` → 60 tick 周期排水
   （User.cpp:82662-82688）→ `User_AddAreaQuest(user, areaId)`（:197108）→ `GetAreaQuest` →
   逐任务去重后 `User_AddQuest(user, questId, {acquireType=3,0,0}, 0, 1)`（:197213）。
3. 通用闸：`CanAcquireQuest`（同上）+ 未建档前置；acquire type 3 = 全宿主共用的系统建档原语
   （`SetQuestAcquired`、HTML 超链、GM `//addquest` 同一 type）。
4. **家族分支 = 零，DLL 不参与发放**；宿主里也没有 `_area_` 字面量（哨兵只是表数据标记，
   宿主行动依据是 world 文件的 `<quest>` 绑定清单）。
5. 39005 对拍：ai-areas.xml:4064（df2a, InvadePortalDest_41_questArea_02）↔ 真端 df2a/world.xml:1089
   逐字一致。

**判定**：区域发放链**与家族无关**（引擎级）；SimpleHunt `_area_` 行与 Talk 的 8 行 `_area_` 同一面。

## 4. 落面

1. `SimpleHuntHandler implements NativeSystemGrantLane`（第三车道）：行级
   `RetailGrantKind.of(acquired_npc_name)` + `NativeNpcFactionNames.idOf(quest.xml npcfaction_name)`
   随构造装载；`grantKind` / `isSystemGranted`（routes ∧ 非 NPC ∧ grantable，挑战哨兵照旧拒绝）/
   `factionId` / `factionRotationCandidates` / `factionRotationEligible`（`NativeFactionRotation`
   共用判据）/ `grantSystemStart`（`NativeQuestStartPort.grant`——真端 AddQuest type 3 的同一
   建档原语）。
2. `NativeSystemGrantLanes.Holder` 注册第三车道（Talk + Collect + **Hunt**）。
3. `RetailSystemGrantDispatchTest` 两断言翻转：faction 行「laneOf 命中 ∧ isSystemGranted」
   （≥62 行）+ owner 交叠恒空；area 行绑定断言保留 + 发放面同断言。
4. **残留轴（新登记 §10.3-#26）**：进区触发器——真端 `MoveNew` 入队 + 60 tick 排水 +
   `AddAreaQuest(type 3)` 在本服无对应物（`_area_` 行的发放面已就绪但无人触发），人口 =
   Talk 8 行 + SimpleHunt 17 行（引擎级，跨家族）。

## 5. 证据命令

```bash
python3 - <<'PY'   # 数据面交集（真端表 utf-16）
import re
H='/Users/mc/IdeaProjects/58Server/Map/XML'
hunt=open(H+'/Quest_SimpleHunt.xml',encoding='utf-16').read()
blocks=re.findall(r'<id id="(\d+)">(.*?)</id>',hunt,re.S)
# … faction/area 哨兵 × npcfactions_quest.xml 池 × ai-areas.xml 绑定
PY
grep -n "GetTodayFactionQuest\|InitFactionQuest\|AddAreaQuest\|CanAcquireQuest" \
  /Users/mc/IdeaProjects/58Server/server58-source/MainServer_Server64/classes/Account/User.cpp \
  /Users/mc/IdeaProjects/58Server/server58-source/MainServer_Server64/classes/Quest/Quest.cpp
```

## 6. 阵营归属门禁收口（P8 cut 2 追加取证）

接线后 `QuestNpcFactionRetailGateTest` 复跑暴露三类偏差，逐类取证后收口：

### 6.1 生产轮换表镜像陈旧（npc_factions_quest.xml）

对拍真端 `npcfactions_quest.xml`（436 行）发现生产快照（425 行）两向漂移：

- **缺 11 行**：35027-35030 / 45027-45030（SimpleTalk `_faction_` 日常）、36514 / 36517
  （BountyHunter_L，前者 SimpleHunt 路由行）、37006（Mentee_L → 生产 npc_factions id 8
  Kaisinel Academy）——全部真端星期位全 0（真端本就不轮换）。
- **掩码本地"修复"漂移 2 行**：39713 / 49713（SimpleItemPlay 行，P0c-3 修正脚本
  `p0c3_fix_faction_masks.py` 的普查范围只含 Hunt+Talk，漏了 ItemPlay 家族）被本地写成
  全 1，真端为全 0。按 P0c-3 判例（真端全 0 = 该日常永不发放，生产写全 1 等于发放真端
  下线内容）回正。

同步工具：`p8/tools/sync_faction_rotation_and_contract.py`（幂等，dry-run 可预览）。
生产表 425 → 436 行，与真端逐位一致。行为影响：35027-35030/45027-45030/36514/36517
从"无行即每日可发"变回真端本征的"永不发放"（对齐性回正，非漂移）。

### 6.2 契约快照 v2（三列含掩码）

- 旧 253 行口径 =「quest_data.xml npcfaction_id ∧ 轮换表 faction_id 两源一致」（XML 时代）；
  原生车道时代实发人口 = typed 元数据（现全 0）∪ 三发放车道路由的 `_faction_` 行。
- **新增 34 条可达行**：轮换 ∧ 势力名可解析 ∧ 车道路由 ∧ 不在旧快照（35027-35030、36514、
  39602/39606/39611/39614/39617/39619、39703/39704/39705/39714/39717/39719 及 Asmodian
  镜像）——这些行**今天就已被车道实发**但从未入评审基线（正是门禁要抓的静默未评审发放）。
- 快照变三列：`quest_id / faction_id / mask`（真端星期位逐位冻结）。287 行。
- **休眠行（until-ported）**：无车道路由 ∧ 无移植定义的行（35052/35055-35065、35505-35515、
  BountyHunter 缺行块、39713/49713 等 ~90 行）保留在快照，沿袭 quest-prerequisite 契约的
  fail-open-until-ported 模式：覆盖后自动转为强制。

### 6.3 门禁三法重锚（QuestNpcFactionRetailGateTest）

- **m1 归属声明**：可达行走 `laneOf → factionId`（车道）或 typed 元数据（XML 行）；
  休眠行必须至少持有真端 quest.xml 的阵营绑定（`NativeQuestXmlTable` 直读）作兜底证明。
- **m2 池等式**：逐源对齐实发组合——typed 元数据声明该势力 ∨ 车道已路由 `_faction_` 行
  （`routes()` 口径；装载但不可路由的 Collect 行如 39611 按休眠处理）；池 == 可达评审集。
- **m3 轮换保真**：每条契约行必须在轮换表有行且势力 + 掩码逐位等于快照；全 0 掩码是
  真端本征（Silverlin/Greenhat 全阵营 + 上述 11 行），按快照冻结禁止本地"修复"；
  至少一条活掩码保底（轮换系统不可整体死灭）。
- `NpcFactionQuestDataTest` 行数下限 425 → 436。

结果：三法 + 数据门 7+3+7 全绿（gates/2026-10-02-focused-run-p8-cut2.log 收全量对拍）。
