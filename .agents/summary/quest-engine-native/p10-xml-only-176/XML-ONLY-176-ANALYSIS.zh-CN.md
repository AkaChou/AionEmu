# XML-only 176 批：真端驱动面取证与实现路径分析

- 日期：2026-10-03；性质：**只读分析**（零代码/数据变更）。
- 对象：`src/main/resources/aion/data/static_data/quest/retail/retail-xml-retention.tsv` 中
  `NO_TABLE` 的 176 个任务 id（当前由本仓 740 份 XML 定义驱动）。

> ⚠️ **更正（2026-10-03 第二轮取证）：§1/§2 的「真端零驱动面」结论作废** —— 首轮只扫了
> 「首参 = 任务 id」的注册口，漏掉 `FUN_180cb5920(name, questId)` 这个**第三参**注册口
> （构造 `IOneQuestScriptNpc`）。实测 **142/176 命中该口**，抽样三方交叉 3/3 全中。
> 详见 **§7**。§3 的形状统计与 §4/§5 的路径建议均须按 §7 修订后再用。

## 1. 一页结论

176 个任务在真端**没有任何任务驱动面**：8 张族表 0 命中、DD 表 0 命中、ScriptDLL64 注册表
（首参字面量）0/176 命中（对照组：随机 20 个 `SCRIPTED` 任务全部 ≥3 命中）、
`Server64.c` 0/15（对照）、`NPCSvr64.c` 仅 2 处噪声（1000/2006，数字巧合）。
其中 **169 个**在真端 `quest.xml` 有元数据行（等级/阵营/分类齐全，非 999 停用形），
**7 个**连元数据都没有（50110/50111/50123/50124/51110/51111/89999 = 纯本服自造）。

⇒ 这批**不是**「待移植的真端脚本任务」，而是「真端只留元数据登记 / 本服自造」的一批；
实现路径必须在 A/B/C 三选一（§4），不存在"照真端表搬行"的选项。

## 2. 驱动面取证（逐层排除）

| 层 | 检查 | 结果 |
|---|---|---|
| 族表 8 张 | 直接对 `<真端根>/Map/XML/Quest_*.xml` + `data_driven_quest.xml` 扫 id | **0/176** |
| 其他表 | `Quest_SimpleGather.xml`（**空表**）/`item_quest.xml`/`login_event.xml`/`jumping_*.xml`/`challenge_task.xml`/`npcfactions_quest.xml`/`quest_random_rewards.xml` | id 级 0；语义标签级 46（见下） |
| ScriptDLL64 | `(0x<id>,` / `(<id>,` 首参字面量 | **0/176**；对照 SCRIPTED 20/20 非零 |
| MainServer `Server64.c` | 同上 | 7 处噪声（全为 ≤1205 的小整数），对照 SCRIPTED 0/15 ⇒ 该层本就不装任务 id |
| NPCSvr64.c | 同上 | 2 处噪声（1000/2006） |
| 真端 quest.xml | 元数据存在性 | 169 存在 / 7 缺失 |

语义标签级命中（`<end_quest>` / `<quest_id>`）：
- `jumping_endquest.xml` 的 `<end_quest>` 列 40 个（**跳跃升级**时的清理登记：升到目标级后把这些旧链路任务结束掉）；
- `challenge_task.xml` 的 `<quest_list><quest_id>` 6 个（军团/城镇挑战任务的计分清单）。

⇒ 真端对这批的全部"引用"都是**登记/清理语义**，没有一处是驱动语义。

## 3. 本仓现状形状（176 的自我介绍）

| 维度 | 分布 |
|---|---|
| XML 行为节点 | 158 个有 `<nodes>`；**18 个只有 metadata、零行为**（1489/2590/3219/3220/4219/4220/9554/9555/9556/9557/16984/16989/26984/50032/51032/51038/51040/51041） |
| 节点数 | 4 节点 49 个、7 节点 23、5 节点 19、1 节点 19 …（最大 13） |
| 状态形 | 144 = NONE/START/REWARD/COMPLETE 四态满形；8 = 无 START；5 = 无 REWARD；1 = 仅 NONE |
| 事件形（158 个内） | dialog 2248 / kill-in-world 101 / enter-world 100 / kill-npc 69 / level-up 57 / enter-zone 40 / can-act 37 / die 28 / zone-mission-end 26 / movie-end 25 / quest-timer-end 16 / log-out 12 / use-item 11 / equip-item 8 / pass-flying-ring 7 / npc-reach-target 5 / invisible-timer-end 4 / item-play 4 / attack-npc 3 / at-distance 2 / get-item 1 |
| 分类（真端元数据） | quest 77 / mission 36 / event 24 / non_count 20 / important 11 / challenge_task 6 / significant 2 |
| 阵营 | ELYOS 93 / ASMODIANS 78 / PC_ALL 4 |
| 内容样例 | 1000–1006 天族序章+转职（`꿈속의 목소리` / `데바로 다시 태어나다`）、1100–1423 天族 ZONE01–03 早期链路、2000–2008 魔族镜像、9554–9557 活动、11279–11286 非计数、1489/14010–14026 跳跃服内容 |

## 4. 实现路径（三选一，按件适用）

**A. 保 XML 车道为「本地扩展」一等公民（当前态，零迁移成本）**
- 迁移计划 §2.6 已把「XML-only 任务仍能运行」列为删旧判定的前置条件；这批正是该条款的保护对象。
- 适用：158 个有行为件（尤其带 movie/zone-mission/log-out 等非族表事件的），以及未来本服自造内容。
- 代价：XML 车道（`ProductionQuestDefinitions` + `QuestProductionDispatcher`）必须长期保留，
  不能随 P8 收口删除；账目上要把它标成"本地扩展车道"而不是"待退役旧车道"。

**B. 移植进 native 表族（本地扩展表 + 消费端支持双源）**
- 只对那些**事件形落在族表表达力内**的件适用：纯对话链（→ SimpleTalk 形）、击杀计数（→ SimpleHunt 形）、
  收集交付（→ SimpleCollectItem 形）、用物（→ SimpleUseItem 形）、演出道具（→ SimpleItemPlay 形）、
  制作（→ CombineTask 形）。
- 前置：native loader 需支持"本地扩展表"第二资源（真端表保持 byte 级不动，**不得**往 `quest/retail/*`
  的真端表里加行——否则溯源清单与冻结门全红）。
- 建议落地形态：`quest/legacy-extension/Quest_Simple*_local.xml`（同 schema）+ loader 合并 +
  扩展行打 `LOCAL_EXTENSION` 标记，门禁按标记分账。

**C. 退役**
- 仅适用于确认无玩家可见影响的件：18 个零行为元数据件（客户端仍会收到任务书但服务端永不驱动，
  与真端行为一致）、7 个真端无元数据的自造件（须先确认无引用）、以及确实与现有内容重复的件。

## 5. 建议的落地顺序（分批）

1. **裁定 18 个零行为件**：确认其"客户端可见但零驱动"是否为有意状态（是真端原样），否则按 C 退役或按 B 补行。
2. **按事件形分桶 158 个有行为件**（本文件 §3 的表就是输入）：能落族表的进 B，其余留在 A。
3. **7 个无元数据件**（50110/50111/50123/50124/51110/51111/89999）：核对本服引用后裁定 B/C。
4. 任何一批落地前，先在 `retail-xml-retention.tsv` 把对应行从 `XML_RETENTION/NO_TABLE` 改标为
   目标车道，保持 owner 单一（本仓既有纪律）。

## 6. 需要用户裁定的点

- 是否接受「A 长期保留 = 本地扩展车道」的架构定位（影响 P8 之后的删除边界）。
- B 的本地扩展表落点与命名（`quest/legacy-extension/` vs 其他）。
- 18 个零行为件与 7 个无元数据件的存废。

## 7. 更正与真端实际驱动面（2026-10-03 第二轮，只读取证）

### 7.1 首轮漏扫的注册口

首轮只扫「**首参**=任务 id」的字面量注册口（族表 row 注册 `FUN_180cab520(questId, …)` 等）。
真端 ScriptDLL 还有一个 **(name, questId)** 形注册口：

- `FUN_180cb5920(out, L"<name>", id)`，函数体见 `ScriptDLL64.c:2146365`：写入 `IOneQuestScriptNpc::vftable`、
  把 `L"<name>"` 拷进对象、把第三参存入记录后插入全局表 `DAT_1847204c8`。
- 规模：**18787 个调用点 / 7148 个唯一 id**；其中 **4682 个落在本仓 6224 任务全集**内
  （4017 属 `RETAIL_TABLE`、665 属 `XML_RETENTION`）。
- 名字来源：真端 `npcs.xml` 每行的 `<quest_ai_name>`（该表共 20600 行带此列）。
  例：`203067 Kalio`、`203071 Muranes`、`203788 Anteros` 都带 `<quest_ai_name>`。

复算脚本：`scan_onequestscriptnpc.py`（注册面抽取 + 与 retention 三组求交）、
`crosscheck_quest_ai.py`（注册面 × npcs.xml × 本仓 XML 引用三方交叉）。

### 7.2 176 的实测覆盖

| 面 | 覆盖 |
|---|---|
| ScriptDLL `IOneQuestScriptNpc` 注册 | **142 / 176** |
| 真端 `quest.xml` 有元数据行 | 169 / 176 |
| 真端 `quest.xml` 含目标/奖励列（collect/drop/check/work/reward_*） | 144 / 176 |
| 客户端 `Quest_unpacked/quest_script_monster.csv`（killedByUser） | 30 / 176 |
| 客户端 `Quest_unpacked/quest_monster.csv`（questItemDropMonster） | 38 / 176 |

**34 个未命中 NPC 注册的 id**（按本文件既有分类）：
- 25 = §3 已判的 18 个零行为件 + 7 个真端无元数据件（50110/50111/50123/50124/51110/51111/89999）；
- 9 = `1000, 1195, 2000, 18744, 21030, 28744, 50038, 50040, 50041`
  （1000/2000 = 序章「无 NPC 对话」件；其余 7 件需逐件裁定驱动面）。

### 7.3 三方交叉（抽样 3/3 全中）

| 任务 | ScriptDLL 注册名（id=任务号） | `npcs.xml` quest_ai_name | 本仓 XML 引用的 NPC |
|---|---|---|---|
| 1001 | `Kalio` / `Muranes` / `WCherubimL_3_n` | 203067 Kalio、203071 Muranes、210670 WCherubimL_3_n | 203067 / 203071 / 210670 **全中** |
| 14010 | `Spatalos` | 203098 Spatalos | 203098 |
| 11279 | `Fillia` | 205553 Filrion（quest_ai_name=Fillia） | 205553 |

即：**本仓自研 XML 里的 NPC 绑定与真端注册面逐 id 对齐**，说明这些件在真端是「可被 NPC 对话接取/交付」的活内容。

### 7.4 真端的三层结构（对本迁移的直接含义）

1. **对话层**：`npcs.xml <quest_ai_name>` ↔ ScriptDLL `IOneQuestScriptNpc(name, questId)`（本批新发现）；
2. **目标层**：`quest.xml` 的 `collect_progress/collect_itemN/drop_monster_N/drop_item_N/drop_prob_N/check_itemK_L` 通用列
   （1001 实测：`collect_progress 7`、`collect_item1 quest_1001a 3`、`drop_monster_1 WCherubimL_4_n`），
   客户端侧镜像为 `quest_script_monster.csv` / `quest_monster.csv`；
3. **族表层**：8 张 `Quest_Simple*.xml` 只承担复杂形（对话链/中继/演出道具/制作）的脚本参数 —— 176 在这层 0 命中。

⇒ 176 不是「真端无驱动的死内容」，而是**走 Quest-AI NPC 对话车道 + quest.xml 通用目标面**的件。

### 7.5 建议（修订 §4）

- §4 路径 A/B/C 作废；新增**路径 D：Quest-AI NPC 车道原生实现**
  （读 `npcs.xml quest_ai_name` ↔ quest id 注册 + `quest.xml` 目标列 + 通用计数/交付口），
  与族表并列成为第三条 native 车道。
- 落地前须逐件裁定 34 个无注册件，其中 1000/2000/1195/18744/21030/28744/50038/50040/50041 需单独取证。
- 本文件 §3 的「零行为 18 件」仍有效（它们在真端同样零驱动）。
