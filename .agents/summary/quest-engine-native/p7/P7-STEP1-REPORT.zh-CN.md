# P7 步 1：DD 原生 handler 契约冻结（零行为变更）

> **门禁产物清理提示（2026-10-03）**：本文件引用的门禁运行产物（`gates/*.log`、`*-red-classes.tsv`、`*-delta.tsv` 等）已按「只记录重要的过程内容、不记录门禁」口径清理，不再随仓保留；关键读数已内联于正文，复现请重跑对应聚焦套件，清理说明见 `.agents/summary/CLEANUP-LEDGER.zh-CN.md`。

> 日期：2026-10-01。分支：`quest`。性质：**只读证据 + 冻结门**（零运行时变更，不抢 P7 实现批）。
> 计划位：§10.2「P7 DataDriven」→ 本批 = **前置步 1**（行语义闭环）；step 2 = 运行时 + 原子切换 + 同批删旧。
> 工具：`p7/tools/dd-native-contract-probe.py`；逐行矩阵：`p7/dd-native-shape-matrix.tsv`（1467 行）；
> 摘要：`p7/dd-native-contract-summary.json`；门：`DataDrivenNativeContractGateTest`（6/6）。

---

## 0. 本批回答的问题

P7 要把 **1467 行** DD 行从「表 → IR 编译器 → 旧 dispatcher」切到**真端形态**（按 `category_progress_`
造 handler 对象、零相机、按 `category_acquire_` 分接取轴）。切换前必须先回答：**这 1467 行到底声明了什么？**
本批把答案复算成**逐行契约矩阵** + 六条不可静默漂移的不变量，作为 step 2 的验收基线。

口径（沿用 §0.1）：真端表 + 客户端证据是唯一权威；禁止在旧 IR 上修补；台账终态删除。

---

## 1. 切换集与分桶（复算，0 例外）

| 桶 | 行数 | 处置 |
|---|---:|---|
| 真端 DD 活行 | 2492 | 块 2526 − 注释 32（其中 18 个 id 只存在于注释里） |
| `CLIENT_ABSENT_LIVE`（客户端三表皆无） | 337 | 表内孤行：**不路由 / 不注册**（§10.3-#5） |
| `COMMENTED_OUT`（真端表注释禁用） | 18 | 装载器不得含 |
| owner `XML_RETENTION`（DD 行但有 XML 定义） | 41 | 走 XML-only IR，**不进 P7 切换集** |
| **P7 切换集** = 活行 ∧ owner `RETAIL_TABLE` | **1467** | 本批契约面；step 2 的原子切换范围 |

100% 客户端可见（客户端 `quest.xml` / `data_driven_quest.xml` / `challenge_task.xml` 至少一表有行）、
100% 声明 `reward_npc_name`（1467/1467，无缺名行 ⇒ §10.3-#4 的缺名形不落在本族切换集）。

证据：`p7/tools/dd-client-presence-probe.py`（前置裁定批）+ 本批工具（切换集与桶重算，与门内一致）。

---

## 2. 接取轴：真端 5 类 kind + DD 的 EnterArea / none

真端 `QuestProgressExtraInfo_Talk.cpp:9-116` 把 `category_acquire_` 映射成 5 类 kind：
`ItemPlay=3` / `Talk=4` / `EnterWorld=7` / `LevelUp=8` / `LevelUpLogIn=10`；DD 表另出现
`EnterArea`（区域任务结束/升级自动接取，16823 形）与 `none`（链式自动接取，前序完成/区域任务结束）。

| 接取类别 | 切换集行数 | 真端触发点 | 本服 native 面 |
|---|---:|---|---|
| `Talk` | 1142 | 对话 NPC（`value0_acquire_`）→ 接取流（`fun_731` 入口页 + 页动作 1007 开窗） | `NativeQuestStartPort` + 对话接取面 |
| `EnterArea` | 165 | 进入区域（区域任务结束/升级自动接取） | 区域轴（`enterAreaSteps` 同名通道） |
| `none` | 120 | 前序任务完成 / 区域任务结束自动发放 | `NativeSystemGrantLanes` 链式面 |
| `LevelUpLogIn` | 15 | 等级提升/登入遍历（NPCSvr64/server64 两宿主同槽） | 系统发放面（kind 10 待接线） |
| `ItemPlay` | 13 | 用物品（kind 3，NPC 键 = `value0_acquire_`） | `SimpleItemPlayHandler` 同轴 |
| `EnterWorld` | 12 | 进世界（kind 7，键 = 世界 id） | 世界轴（`enterWorldSteps` 同通道） |

`_faction_`（52 行）与 `LevelUp`（1 行）**不在切换集内**（全落在 ABSENT 桶）⇒ step 2 的接取轴只需覆盖上表 6 类；
未知类别一律 fail-closed（门内断言 `ACQUIRE_KINDS` 闭合）。

---

## 3. 进度轴：真端 8 类 handler 契约（step 2 的实现清单）

证据：`p7-prereqs/dd-dispatcher-and-handlers.md` §2（真端反汇编逐函数），本批按**逐行复算的用量**排序：

| 类别 | 真端 handler | 切换集 行/步 | vars 算术（真端） | 触发事件 | 副作用（动作表） |
|---|---|---:|---|---|---|
| `Hunt` | `FUN_180c46020` | 731 / 827 | bit0-5 步号校验 `(prog&0x3F)==expectedStep`；子目标 5×6 位（移位 6/12/18/24/30），命中且未满则 +1；全满→步进/成功 | NP `PacketValidMemberList`→`FUN_1402033a0` | `FUN_180c4d190(data+0x20)` |
| `CollectItem` | `FUN_180c46e90` | 348 / 348 | `counter=(prog>>6)&0x3F`；`+1<count → prog+=0x40`；否则步进/成功。状态 ≠3 时按 kind 3 接取 | 宿主 `mgr+0x268+5*0x10`（采集/交付事件） | `FUN_180c4cd50(data+0x10)` |
| `Pvp` | `FUN_180c46980` | 207 / 207 | 等级/军衔闸门（`ctx+0xC + def+0x14 >= GetLevel`）后单组计数 +0x40；满则步进/成功 | NP `PacketDie`→`FUN_140203960`（hash B） | 无 |
| `Talk` | `FUN_180c466a0` | 165 / 403 | 取 state/prog；`(prog&0x3F) < def+0x44` 则 `SetQuestProgress(def+0x40)`；逐项发/扣 `def+0xA0` 向量 | 宿主直接向量 `evt=2` | 逐项 `+0x1D8`（物品） |
| `EnterArea` | `FUN_180c47bf0` | 83 / 153 | `data+8`（区域名哈希）与 `(+0x50)(user)` 比较，命中→步进/成功 | 对象虚槽调用（IOneQuestScriptNpc 体系） | `FUN_180c4c8d0(data+0x28)` |
| `ItemPlay` | `FUN_180c474b0` | 37 / 42 | **无独立计数**：由动作码驱动（`>=10000 → SetQuestProgress(code-9999)`；`0x3F1 → SetQuestSuccess(step+1)`） | 对象虚槽调用（对话/物品玩法流） | `FUN_180c4d5b0` 收尾 |
| `EnterWorld` | `FUN_180c467b0` | 29 / 34 | `(prog&0x3F)==ctx+0xC` 校验后直接步进/成功；状态 ≠3 时按 kind 7 接取 | 宿主 map `+0x468` walk | `FUN_180c4cd50` |
| `TalkFOBJ` | `FUN_180c478e0` | 14 / 19 | 与 Hunt 同款 5×6 位计数；首次命中 0→1 并按动作类型给效果；全满→步进/成功 | 对象虚槽调用 | `FUN_180c4c8d0(data+0x50)` |

共用前奏（所有 6 参 handler）：`prog = GetQuestProgress` → 步号不匹配直接 return →
距离/等级闸门按 `def+0xE0` 取值（0→2500、1/2→10000、5/6→40000；0 还要求同 map 且非同盟）→
完成写回中间步 `SetQuestProgress`、末步 `SetQuestSuccess`。

**每步是一条 handler 记录**（真端 `def+0xF0` 向量元素 `{kind@0, data@8}`）⇒ 同一行的多步链由**按步类别切换 handler**
服务：本批复算的行里 **1188 行只有 1 步**、99 行零步（纯接取→领奖）、180 行 2 步以上（最多 14 步，2 行），
**94 个不同的有序步序列形**（门内冻结去重数；新形即失败，必须显式裁定）。

---

## 4. 步列词汇表（实测；step 2 的「动作/效果面」实现清单）

每步的 `valueN_progress_` 列就是真端动作表解释器 `FUN_180c4cd50/data+0x10/0x20/0x28/0x50` 的**数据来源**。
本批复算切换集里**实际出现的列号与次数**（出现次数 = 步数；门内逐类冻结）：

| 类别 | 0 | 1 | 2 | 3 | 4 | 5 | 6 | 7 | 9 | 10 |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| hunt | 827 | — | — | — | 5 | 4 | — | — | — | — |
| collectitem | 348 | 72 | 13 | 4 | 2 | 83 | — | — | — | — |
| pvp | 207 | 22 | 22 | 96 | — | — | — | — | — | — |
| talk | 403 | 78 | 20 | 3 | 8 | 11 | 10 | 4 | 3 | 6 |
| enterarea | 153 | — | — | — | 12 | 6 | — | 3 | — | 2 |
| itemplay | 42 | 4 | 3 | — | 2 | 5 | — | 1 | — | 2 |
| enterworld | 34 | 1 | 1 | — | 1 | — | — | — | — | — |
| talkfobj | 19 | 7 | 3 | 1 | 2 | 5 | 2 | — | — | 2 |

观察到的形状（逐条列出，供 step 2 逐形实现；**语义分层按证据强度标注**）：

| 列/形状 | 观察 | 语义判定 |
|---|---|---|
| `0` 全部类别 | 每步必有 | **载荷**（已坐实：hunt=怪物名+计数、collectitem=交付 NPC+多 FOBJ、talk=对话 NPC、pvp=计数、enterarea=区域名、enterworld=世界 id、itemplay=道具符号、talkfobj=FOBJ 名） |
| `1`/`2`/`3`/`4`（collectitem） | 追加 FOBJ 名（最多 4 个：15670/25670 形） | **多 FOBJ 交付链**（P4 已按同轴实现 drop/FOBJ 槽序） |
| `1`/`2`（talk/itemplay/enterworld/talkfobj） | `QUEST_X 1` / `doc_quest_X 1` 形 | 发/扣物品（P3 已坐实的物品符号面） |
| `3`（talk/talkfobj） | `210050000 1440 408 553 77` | **传送坐标 4 元组**（`world x y z heading`） |
| `4`（hunt/collectitem/talk/enterarea/itemplay/enterworld/talkfobj） | `Cutscene N` / `Cutscene2 N` / `Movie N, HACTION_*` | **过场/影像 + 页动作名**（`stepCutscenes` 已装载；`HACTION_*` 是页动作面，待 step 2 接线） |
| `5`（hunt/collectitem/talk/enterarea/itemplay/talkfobj） | `Relative <npc>, n, sec` / `Absolute <npc>, n, sec, x y z h`；collectitem 的 `5` 是纯数字 | **生成事件对象**（相对/绝对刷怪；collectitem 的数字形 = 单 FOBJ 序数，P4 已按交付槽序解释） |
| `6`（talk/talkfobj） | 小整数（5/1/3/2/8） | 待坐实（疑似动作类型/计数闸门） |
| `7`（talk/enterarea/itemplay） | `STR_QUEST_SAY_*` / `STR_CHAT_*` | **喊话串**（`Say` 面，真端 `+0x2B8`） |
| `9`（talk） | `13, 300160000, 7` | 待坐实（三段：世界 + 数 + 数） |
| `10`（talk/enterarea/itemplay/talkfobj） | `120, 7, 1` / `1800, 5, 0` | 待坐实（三段，疑似定时器/延迟动作，真端 `+0x250` 定时器 case 10） |

> **诚实边界**：`0`/`1`/`2` 与 `4`（过场）已在 P1–P6 各族坐实；`5` 的生成语义在 P4 的 collect 面坐实为交付槽，
> hunt/talk/enterarea 的 `Relative/Absolute` 形**本批只做列面冻结，语义映射留 step 2 前置**（真端动作表解释器
> 逐 case 已还原，但 `valueN → def 偏移` 的对应表尚未逐列坐实）。
> 门只冻结「哪些列被使用 + 用了多少次」，**不把未坐实的语义写死**。

---

## 5. 6 位布局不变量与唯一例外

- 真端 DD 算术：`bit0-5 = 步号`，`bit6..` 每 6 位一个组槽（handler 里移位 6/12/18/24/30）。
- 本批复算：**每步子计数 ≤ 4 组**（0 例外；`hunt` 段数分布 1→772 / 2→45 / 3→4 / 4→6，其余类别单组）；
- **计数 ≤ 63**（0 例外，唯一例外见下）；
- 唯一超 6 位计数 = **80817**（`world_event_camel 100`）：按 §10.3-#5 裁定**原样复刻真端算术**
  （第 64 杀槽回绕 ⇒ 真端自身不可完成），**禁止**改 10 位相机、**禁止**显式禁用。门内逐行断言该集合恒为 `{80817}`。

---

## 6. 失败模式（step 2 的 fail-closed 判据）

| 情形 | 判据 |
|---|---|
| 未知 `category_progress_` | 门内 `PROGRESS_CATEGORIES` 闭合断言；运行时按稳定码拒绝该行（不落任何 handler） |
| 未知 `category_acquire_` | 门内 `ACQUIRE_KINDS` 闭合；运行时拒绝注册（不猜 NPC/参数） |
| 步缺 `value0_progress_` | 门内断言；装载/编译期拒绝 |
| 新增未裁定列号 | 门内步列直方图冻结 ⇒ 失败（禁止静默吞列） |
| 每步子计数 > 4 组 / 计数 > 63 | 门内断言；超限行必须单独裁定（当前只有 80817） |
| 缺 `reward_npc_name` | 门内断言（切换集 0 例外） |
| 新步序列形 | 门内去重形数（94）冻结 ⇒ 失败，必须显式裁定 |

---

## 7. 门与复算

```bash
# 逐行契约矩阵 + 摘要（只读；不读外部根）
python3 .agents/summary/quest-engine-native/p7/tools/dd-native-contract-probe.py \
  --summary-json .agents/summary/quest-engine-native/p7/dd-native-contract-summary.json

# 门（6 例）
mvn -o test -Dtest=DataDrivenNativeContractGateTest -DfailIfNoTests=false
```

**本批门态（2026-10-01，零行为变更批）**：新门 `DataDrivenNativeContractGateTest` **6/6**；族门 + tablelane **144/144**（原 138 + 本批 6；日志 `gates/2026-10-01-p7-step1-family-tablane.log`）；聚焦套件 **1699 例 / 161F + 137E / 105 红类**，对 QE-112 基线 **ADDED 0 / REMOVED 0 且逐类三元组 changed 0**（`gates/2026-10-01-focused-run-p7-step1.log` / `-red-classes.tsv` / `-delta.tsv`）——本批只加门与证据，不改运行时，故红灯面必须逐类不变。

门内**独立重解析**真端表（正则口径与工具一致）并断言：切换集 1467、接取直方图、
8 类 handler 词汇、每类步数、每行步数分布、94 个有序形、步列直方图、6 位布局、
逐行矩阵**规范形 SHA-256**（`3d7b762e16b4977b98844fdca414ea287fa3c3c36429373cb8b6b08fa5b7f77d`）
以及与生产装载器 `RetailDataDrivenTable` 的**逐行一致**（接取类别 / 步序列 / 领奖名）。

---

## 8. 边界与本批不做的

- **不改运行时**：本批零行为变更（无 main 代码改动），DD 行仍走既有 IR 编译面（owner `RETAIL_TABLE` 不变）。
- **不做 step 2**：原生 handler 运行时、1467 行原子切换、`RetailDataDriven*Compiler` 与
  `RetailEnterAreaZoneResolution` / `RetailQuestAiNameGroups` 台账读取删除、typed 车道入口页残余
  （§10.3-#22，裁定见计划）均留给 step 2。
- **未坐实项**：§4 表中的 `6`/`9`/`10` 列与 `5` 的 hunt/talk 生成形；step 2 前必须逐列坐实 `valueN → 真端 def 偏移`
  对应表，否则该列按 fail-closed 处理（不得静默忽略）。
