# 任务 10527 领奖行投影错位（使用 182216075 后任务书仍停在“调查影子潜入部队秘密文书”）与同族行/状态对齐审计

- 日期：2026-09-21
- 范围：`quests/10527.xml`（用户报障）、`quests/10528.xml` 与 `quests/10525.xml`/`quests/20525.xml`（镜像同型）、
  新增聚焦门禁 `src/test/java/com/aionemu/gameserver/questEngine/definition/ArchdaevaRewardRowContractTest.java`
- 状态：**批次 1-5 实现完成 + 静态验证通过 + 聚焦 Maven 测试通过（批次 1-4 为 49 项，批次 5 为 57 项，
  生产目录 6189/6189 编译通过、白名单 0 违规）；客户端实机复测未做（PENDING_CLIENT）**
- 批次 1（用户授权后执行）：`11294`、`15002`、`15010`、`15070`、`15514`、`19004`、`25062`、`25073`、`26820`
  按镜像模板修复领奖行并补自愈边，新增 `AlignedMirrorRewardRowContractTest`（见第五节）
- 批次 2（用户授权后执行）：120 个“天/魔镜像两侧都缺末行”的任务，按“末行 NPC 名字证据 + 镜像同形”收口，
  新增 `MirrorPairRewardRowContractTest`（见第五之二节）
- 批次 3（用户授权后执行）：202 个“末行 NPC 命中 reward 路线”的单侧任务，新增
  `JournalRewardRowRepairContractTest`（见第五之三节）
- 批次 4（用户授权后执行）：`10529/20529` 报告行拆成独立状态、`10530` 对齐镜像 reward=9、
  `3090` 位段隔离 + 领奖行，新增 `JournalReportRowSplitContractTest` 与全库 SECTION 位段门禁（见第七节）
- 批次 5（用户授权后执行）：`15550/25550`（三段对话链）与 `15551-15554`/`25551-25554`（感应区坐骑族）共 10 个任务，
  按 origin/history 旧 handler 恢复被迁移丢掉的行推进，新增 `SensoryAreaRideRowContractTest`（见第八节）
- 证据脚本：[audit_reward_row_vs_client_steps.py](audit_reward_row_vs_client_steps.py)、
  [apply_batch5_sensory_area_rows.py](apply_batch5_sensory_area_rows.py)（批次 5 的可重放改动脚本）、
  [audit-output.tsv](audit-output.tsv)、[audit-stdout.txt](audit-stdout.txt)

## 一、现象（用户实机报障，10527）

- 玩家已经使用任务工作物品 `182216075`（`doc_quest_10527a`）后，任务停在“调查影子潜入部队秘密文书”，
  任务书没有切到下一步；用户给出目标状态：领奖（`REWARD`）+ `var0=15`，当前错误投影为 `var0=14`。
- 期望文案“和代理人维达对话”对应客户端任务书最后一行（第 15 行，0 基）。

## 二、根因

- 客户端任务书行索引读 `SECTION_0`（= `var0`，见 memory-bank `QE-012`）。Aion 5.8 客户端解包数据
  `Dialogs/10000_19999/quest_q10527.html` 的 `quest_summary` 共 16 行（0..15）：
  第 14 行是“调查 `doc_quest_10527a`”（使用道具步），第 15 行才是“和代理人维达对话”（领奖行）。
- `10527.xml` 的 `reward` 节点投影写成 `var0=14`，与使用道具前的 `s14` 投影相同。`s14 -> reward` 的
  `use-item` 交接只扣物品、不改写 `var0`，`QuestMutationPlanner` 便用目标投影补出 `var0=14`
  （目标投影对动作未触及的字段是权威的），客户端于是按第 14 行解释领奖态。
- 旧 handler `origin/history:.../_10527Finding_The_Traces_Of_The_Sage.java` 的 `onItemUseEvent` 调用
  `useQuestItem(env, item, 14, 15, true)`，意图是 `14 -> 15`；旧 `QuestHandler.changeQuestStep(..., reward=true)`
  只置 `QuestStatus.REWARD`、不写 `nextStep`，迁移时把 `to=15` 丢成 reward 投影 `14`。
- 同批迁移的魔族镜像 `20527.xml` 的 reward 投影是 `var0=15`（同样的 `s14` 使用道具交接），
  镜像不一致是本任务的直接旁证。

## 三、修复

`quests/10527.xml`：

1. `reward` 节点投影 `var0` 由 14 改为 15（与镜像 20527 对齐；`use-item` 交接保持不改写 `var0`，
   由目标投影给出 15）。
2. 新增无 source 的 `enter-world` 恢复边：`status=REWARD && var0==14 -> reward`，
   动作 `set-variable var0=15`，after-commit `LEVEL_AND_VISIBILITY_REFRESH`。
   用途：纠正已经落盘的错误存档——`QuestMutationPlanner#matchesSourceNode` 会把 `REWARD/var0!=15`
   的存档挡在 `reward` 节点的所有领奖路由之外，缺这条边玩家连奖励对话都点不出来
   （与 15300/25300、15001 的旧存档自愈边同形）。

`quests/10528.xml`（镜像同型）：

1. `reward` 节点投影 `var0` 由 11 改为 12；`s11 -> reward` 交接的 `set-variable var0` 同步改为 12
   （镜像 20528 就是 `reward var0=12` + 交接写 `var0=12`）。
2. 新增无 source 的 `enter-world` 恢复边：`status=REWARD && var0==11 -> set-variable var0=12`。

`quests/10525.xml` / `quests/20525.xml`（镜像同型，按第四节“每行都要有状态”的判据修复）：

1. `reward` 节点投影 `var0` 由 6 改为 7（两侧一致改）；`s6 -> reward` 的使用道具交接不改写该字段。
2. 新增无 source 的 `enter-world` 恢复边：`status=REWARD && var0==6 -> set-variable var0=7`。

新增 `ArchdaevaRewardRowContractTest`：锁定 10525/20525、10527/20527、10528/20528 的 reward 投影、
交接 transition 的条件/动作/after-commit（含 `LF6_ITEMUSEAREA_Q10525` 区域门）、planner 计划结果
（`REWARD` + 领奖行 var0）、恢复边形状与镜像一致。

## 四、行 ↔ 状态对齐审计（“每一行都必须有状态”）

判据：客户端 `Dialogs/*/quest_q<id>.html` 的 `quest_summary` 第 k 行（0 基）必须能在服务端任务定义里
找到 `var0 == k` 的 `START`/`REWARD` 节点。缺口的行在游戏里永远不会被高亮；`var0 >= 行数` 的状态
则落在客户端不存在的行号上。

10527（本次报障任务，修复后）：16 行 ↔ `var0` 0..15 一一对应，**错位 0 行**；
修复前第 15 行（“和代理人维达对话”领奖行）没有任何状态，正是报障现象。

| 行 | 文案（客户端 quest_summary） | 状态 | 进入事件 |
| --- | --- | --- | --- |
| 0 | 和代理人维达对话 | `started` (START, var0=0) | level-up / zone-mission-end |
| 1 | 和永恒之塔的德扎波波对话 | `s1` | 806075 `SETPRO1` |
| 2 | 和代理人维达对话 | `s2` | 806291 `SETPRO2` |
| 3 | 寻找感官区域 a | `s3` | 806075 `SETPRO3` |
| 4 | 调查受困的德扎波波 a | `s4` | `enter-zone FALLOW_RUINS_210100000` |
| 5 | 救出了德扎波波！去调查塔之碎片 a | `s5` | 703313 `USE_OBJECT` |
| 6 | 寻找感官区域 b | `s6` | 731705 `USE_OBJECT` |
| 7 | 调查受困的德扎波波 b | `s7` | `enter-zone TARHA_KRALL_VILLAGE` |
| 8 | 救出了德扎波波！调查塔之碎片 b | `s8` | 703314 `USE_OBJECT` |
| 9 | 寻找感官区域 c | `s9` | 731706 `USE_OBJECT` |
| 10 | 调查受困的德扎波波 c | `s10` | `enter-zone VALLEY_OF_THE_WAYWARD` |
| 11 | 在塔之碎片 c 附近消灭黑暗之拉 (n/7) | `s11` | 703315 `USE_OBJECT`（var1 计杀 7） |
| 12 | 回收 quest_10527b 后报告代理人维达 | `s12` | 244107 第 7 次击杀 |
| 13 | 和代理人维达对话 | `s13` | 806075 `CHECK_USER_HAS_QUEST_ITEM` |
| 14 | 调查 doc_quest_10527a（使用 182216075） | `s14` | 806075 `SETPRO14` |
| 15 | 和代理人维达对话（领奖行） | `reward` (REWARD) | 使用 `182216075`（本条为本次修复点） |

同族（10520-10530 / 20520-20530）行 ↔ 状态清单：

| 任务 | 客户端行数 | 服务端 state var0 | 缺口行 | 结论 |
| --- | --- | --- | --- | --- |
| 10520 / 20520 | 6 | 0..5 | 无 | 对齐 |
| 10521 / 20521 | 15 | 0..14 | 无 | 对齐 |
| 10522 / 20522 | 2 | 0 | **第 1 行**（“和代理人维达对话”） | 外部写入者（创造力点数）任务，见 QE-046，待单独设计 |
| 10525 / 20525 | 8 | 0..7（本次修复前 0..6） | 无（修复前第 7 行） | 本次修复并补恢复边 |
| 10526 / 20526 | 13 | 0..12 | 无 | 对齐 |
| 10527 / 20527 | 16 | 0..15（本次修复前 0..14） | 无（修复前第 15 行） | 本次修复并补恢复边 |
| 10528 / 20528 | 13 | 0..12（10528 修复前 0..11） | 无（修复前第 12 行） | 本次修复并补恢复边 |
| 10529 / 20529 | 12 | 0..10 | **第 11 行**（“和代理人维达对话”） | 折叠链：第 10 行“带上沉睡的德扎波波，向代理人维达报告”目前落在 REWARD 上，需要新增 `var0=10` 的 START 状态并把领奖行改到 11，待客户端观测确认报告行归属 |
| 10530 / 20530 | 11 / 10 | 0..8 | **第 9、10 行** / **第 9 行** | 折叠链且两阵营客户端行数不同（11 vs 10），需逐行核对后单独设计 |

全库筛查（2026-09-21 复核版；证据 `audit-output.tsv` / `audit-stdout.txt` / `audit-missing-last-row.tsv`）：

- 复核中修正了审计脚本的一处覆盖缺陷：客户端解包目录里 `quest_q<id>.html`（小写 6430 个）与
  `QUEST_Q<id>.html`（大写 2695 个，多为只有大写命名的任务）并存，旧索引按大小写敏感匹配，
  把 2016 个其实带 `quest_summary` 的任务误判成 `NO_CLIENT_HTML`。修正后客户端任务书覆盖
  5572 / 6222（其中 2016 个来自大写命名副本；`unused/` 目录副本已降权，`client_file` 列可追溯来源）。
- 行 ↔ 状态（判据“每一行都必须有 START/REWARD 状态”，仅在 `var0` 就是行号的任务族成立）：
  `ROW_STATE_ALIGNED 2335`、`ROW_WITHOUT_STATE 610`、`STATE_OUT_OF_RANGE 2450`、
  `BOTH_MISALIGNED 177`、`NO_CLIENT_HTML 650`。
- 形状聚类：`ALIGNED 2335`、`STATES_BEYOND_ROWS 2627`、`MISSING_LAST_ROW 161`、
  `INTERIOR_GAP 265`、`MISSING_TAIL_ROWS 94`、`NO_STATE 89`、`NO_CLIENT_HTML 650`。
- 领奖行（reward 投影 vs 客户端最后一行）：`ROW_ALIGNED 2553`、`ROW_BEHIND 290`、
  `ROW_AHEAD 2593`、`NO_REWARD_ROW 178`、`NO_CLIENT_HTML 608`。
- 三类高风险集合的边界（**禁止机械批量改**）：
  - `STATES_BEYOND_ROWS 2627` 中有 2593 个就是 `ROW_AHEAD`：客户端只有 1 行（1590 个）或 2 行
    （955 个），而服务端 `var0` 充当阶段/计数槽（例 `1221`：2 行而 `var0` 0..3 是击杀计数；
    `1001`：5 行而 `var0` 0..8）。这类只有客户端 `quest_script` 明写 `SECTION_0==N` 时才算行索引
    （全库仅 42 个任务有该声明，其中 6 个已对齐、36 个待逐个核对），其余必须先实机观测。
  - `MISSING_LAST_ROW 161` 中约 130 个末行是“对话/报告/交付”，`QE-045` 锁 10 个；镜像同样缺末行
    192 个（结构同型）。**反例** `15300/25300` 也落在这一类（15 行、reward=13），但客户端验收值
    就是 13，说明“最后一行”并不总是领奖行，必须逐族核对。
  - `progress` 字段上限耦合：reward 投影必须 ≤ 声明 `max`，否则生产目录直接编译失败
    （本轮 Maven 已抓到 10525/20525 的 `var0 max=6` 与 26820 的 `var0 max=1`）。审计脚本 [7b] 显示
    当前 0 个任务越界；批次 2/3 已连带抬高 38 个任务的 `max`，其中 8 个还要同步加宽 `width`
    （`max` 超出 `width` 会在编译期报 `bit-field value range exceeds its declared width`），
    剩余 MISSING_LAST_ROW 改口径前同样要先过这两道字段校验。
  - 逐任务核对后“`MISSING_LAST_ROW` + 末行是领奖行 + reward 投影不是末行”共 404 个候选
    （`audit-qe051-candidates.tsv`）。其中 **9 组镜像一侧行数相同、行/状态全对齐且 reward 投影
    就是其领奖行**，可直接当模板（另一侧就是 10527 同型缺陷）：
    `11294/21294`、`15002/25002`、`15010/25010`、`15070/25070`、`15514/25514`、`19004/29004`、
    `25062/15062`、`25073/15073`、`26820/16820`（末行均为“和 X 对话/向 X 报告”）。
    其余候选仍需逐任务核对客户端末两行文案、`origin/history` 旧 handler 与实机复测后才能动。


## 五、批次 1：镜像已对齐的 9 组领奖行修复（2026-09-21，用户授权后执行）

修复对象（每对里“缺末行”的一侧，另一侧本来就是正确模板）：

| 任务 | reward 行 | 镜像（已对齐） | 客户端末行（领奖行） |
| --- | --- | --- | --- |
| `11294` | 0 -> 1 | `21294` | 向 Cainus 报告杰米娅的情况 |
| `15002` | 0 -> 1 | `25002` | 和 LF5_Nubes_E 对话 |
| `15010` | 0 -> 1 | `25010` | 和 LF5_Mosphera_E 对话 |
| `15070` | 0 -> 1 | `25070` | 和 LF5_Yuraringring_E 对话 |
| `15514` | 0 -> 1 | `25514` | 和 LF6_Hemellos_E 对话 |
| `19004` | 1 -> 2 | `29004` | 和 Hilarus 对话 |
| `25062` | 0 -> 1 | `15062` | 和 DF5_Sorg_E 对话 |
| `25073` | 0 -> 1 | `15073` | 向 DF5_Cnut_E 报告结果 |
| `26820` | 1 -> 2 | `16820` | 向圣所管理者 DF6_Conrto_E 报告 |

每个任务的改动与 10527 模板一致：

1. `reward` 节点投影 `var0` 改为客户端领奖行（交接 transition 不改写该字段，由目标投影补足）；
2. 新增无 source 的 `enter-world` 恢复边：`status=REWARD && var0==旧行 -> set var0=新行` +
   `LEVEL_AND_VISIBILITY_REFRESH`，修复已落盘的旧存档（双语注释）。

同时修正两处 `progress` 字段上限（否则生产目录编译直接失败，均由本轮 Maven 抓到）：

- `10525/20525`：`var0` `max=6 -> 7`（上一轮把领奖行改成 7，但没抬高字段上限）；
- `26820`：`var0` `max=1 -> 2`。

新增聚焦门禁 `src/test/java/com/aionemu/gameserver/questEngine/definition/AlignedMirrorRewardRowContractTest.java`：
锁定 9 组任务的 reward 行 = 客户端领奖行 = 镜像 reward 行、交付路线的 planner 计划
（`REWARD` + 领奖行）、恢复边的条件/动作/after-commit 与 planner 自愈结果、
以及“任何进入 reward 的路线都不得写入过期行号”。

## 五之二、批次 2：双侧都缺末行的镜像对（120 个任务，用户授权后执行）

筛选口径（每条都要能自动复核，见 `audit-output.tsv` 的 `last_row_npc_matches_quest` 列）：

1. 形状 = `MISSING_LAST_ROW`（states 恰好 0..N-2，缺且只缺最后一行）；
2. 末行是“对话/报告/交付”行（`DIALOG_ROW_RE` 本轮补入“交给/交付/递交/转交/归还/送达”）；
3. **末行的 `STR_DIC_N_XXX` 必须对得上任务内出场 NPC 的客户端名**（`npcs_unpacked/client_npcs_npc.xml`
   的 `<id>/<name>`；例 25052 末行 `STR_DIC_N_DF5_Soglo_E` ↔ 任务 NPC）；
4. 天/魔镜像两侧行数相同、同形状；
5. 排除：`QE-045` 锁 10、`QE-046` 引擎外直写 8、末行与首行重复（`15300/25300` 型）12、
   reward 路线已写 `var0` 12、镜像不同形 177、末行 NPC 对不上 68、非对话末行 75。

改动（每任务与 10527 模板一致）：`reward` 投影 N-2 -> N-1；`var0 max` 不足的 12 个同步抬高
（10110、13961、14026、14031、14052、17540、18830、20110、24026、24031、27540、28830）；
新增无 source 的 `enter-world` 自愈边（`status=REWARD && var0==N-2 -> set N-1`）。
另外补两条镜像非对称条目：`25052`（末行是“把海巨人水晶交给…”交付行，关键词此前漏判）与
`28253`（击杀路线已写 `var0=1` 但 reward 投影仍为 0，两者已对齐）。

人工挂账：`18036/28036` 的 reward 路线带 `variable-at-least var0 1` 条件，而其 source
（`started`，var0=0）不可能满足该条件，条件与投影不自洽，**本轮未改**，需单独核对后收口。

新增门禁 `src/test/java/com/aionemu/gameserver/questEngine/definition/MirrorPairRewardRowContractTest.java`：
120 条合同逐条锁定 reward 行 = 客户端末行、镜像同值、自愈边条件/动作/after-commit 与 planner 结果、
以及“任何进入 reward 的路线都不得写入过期行号”。

结果：`MISSING_LAST_ROW 483 -> 363`、`ALIGNED 2014 -> 2135`、`ROW_ALIGNED 2230 -> 2351`。

## 五之三、批次 3：末行 NPC 命中领奖路线的单侧任务（202 个任务，用户授权后执行）

比批次 2 更严的一条证据：末行的 `STR_DIC_N_XXX` 不只是任务内出场 NPC，而必须是
**reward 相关路线（target=reward 或 source=reward 的对话/完成 NPC）里的 NPC**
（用 `npcs_unpacked/client_npcs_npc.xml` 的 `<id>/<name>` 反查；205 个候选里 203 个通过，2600、21075 剔除）。

筛选口径：`MISSING_LAST_ROW` + 末行是“对话/报告/交付”行 + 上述 reward NPC 命中 + 非 QE-045/QE-046 +
末行不与首行重复 + reward 路线不写非目标行号、条件与 source 投影自洽。

改动（每任务与 10527 模板一致）：

1. `reward` 投影 -> 客户端末行（N-1）。其中 155 个原投影是 N-2，**48 个原投影是 0（迁移时从未设置）**，
   两种都统一推到末行；
2. 无 source 的 `enter-world` 自愈边：`status=REWARD && var0==旧值 -> set N-1`；
3. `progress` 字段上限：26 个抬高 `max`；其中 9 个的 `max` 超出声明 `width`，8 个同步加宽
   （1921 w2->3、2634/2669/24201/24202 w1->2、2946/11076/14051 w2->3），`3090` 因紧凑布局
   （`var0@0 w2` + `var1@2` + `var2@3`）回退挂账。

挂账（本轮未解决，需要单独设计）：

- `3090`：紧凑位段布局，加宽 `var0` 会与 `var1/var2` 重叠；
- `24201/24202`：reward 行已修正，但两任务**缺 var0=1 的击杀阶段状态**（`quest_q24201` 第 1 行
  “消灭…(n/12)”没有状态可指向），修复后形状是 `INTERIOR_GAP` 而非 `ALIGNED`，需补 START 阶段；
- `10529/20529`、`10530/20530`：折叠链（同一 REWARD 上压了两行）需要拆状态而不是只改投影。

新增门禁 `src/test/java/com/aionemu/gameserver/questEngine/definition/JournalRewardRowRepairContractTest.java`
（202 条合同：reward 行 = 客户端末行、自愈边条件/动作/after-commit 与 planner 结果、reward 路线不得写过期行号）。

结果：`MISSING_LAST_ROW 363 -> 161`、`ALIGNED 2135 -> 2335`、`ROW_ALIGNED 2351 -> 2553`、
`ROW_BEHIND 492 -> 290`。三批累计修复 331 个任务（9 + 120 + 202）。

## 六、验证

已执行（本轮）：

```bash
xmllint --noout --schema src/main/resources/aion/data/static_data/quest_definition/quest_definition.xsd \
  src/main/resources/aion/data/static_data/quest_definition/quests/10525.xml \
  src/main/resources/aion/data/static_data/quest_definition/quests/20525.xml \
  src/main/resources/aion/data/static_data/quest_definition/quests/10527.xml \
  src/main/resources/aion/data/static_data/quest_definition/quests/20527.xml \
  src/main/resources/aion/data/static_data/quest_definition/quests/10528.xml \
  src/main/resources/aion/data/static_data/quest_definition/quests/20528.xml
python3 .agents/summary/quest-10527-reward-row/audit_reward_row_vs_client_steps.py
git diff --check
```

- 结果：6 个 XML 均 `validates`；审计脚本全库运行成功（行 ↔ 状态与领奖行两套判定）；
  IDE 检查改动 XML 与新增测试 0 error/warning。

复核（2026-09-21，覆盖全部 6222 个任务定义）：

```bash
python3 .agents/summary/quest-10527-reward-row/audit_reward_row_vs_client_steps.py
```

- 修正客户端文件索引的大小写敏感缺陷后重跑；输出 `audit-output.tsv`（全库）、
  `audit-missing-last-row.tsv`（“只缺最后一行” 492 行）、`audit-qe051-candidates.tsv`（404 候选）、
  `audit-stdout.txt`（[1]-[9] 汇总）。10527/20527、10525/20525、10528/20528 复核结果均为
  `ALIGNED` + `ROW_STATE_ALIGNED`。

已执行（用户授权后，2026-09-21）：

```bash
mvn -B test -Dtest='AlignedMirrorRewardRowContractTest,ArchdaevaRewardRowContractTest,QuestCollectProgressAlignmentGateTest,Quest10520ClientDialogAlignmentTest,QuestDialog31RegressionTest,QuestDefinitionCatalogManifestTest,ProductionCatalogWhitelistVerificationTest'
```

- 结果：`Tests run: 36, Failures: 0, Errors: 0, Skipped: 0`；`PRODUCTION_COMPILE_OK=6189`、
  `PRODUCTION_COMPILE_FAILURES=0`、`PRODUCTION_WHITELIST_VIOLATIONS=0`。
- 第一轮运行曾失败于 `10525/20525: value out of range for progress field: var0`（`max=6` 容纳不了 7），
  抬高 `max` 后全绿；`AlignedMirrorRewardRowContractTest` 又抓到 `26820` 的 `var0 max=1`，同样已修正。

客户端复测路径：

1. 10527：用 `REWARD/var0=14` 的旧存档登录或切图，恢复边应纠正为 15 并下发 `SM_QUEST_ACTION`
   （状态=4，packed step 低 6 位为 15）；任务书应显示“和代理人维达对话”，与 806075 对话应下发
   `DEFAULT_SUCCESS(10002)`，点 `SELECT_QUEST_REWARD(1009)` 打开奖励窗口并完成。
2. 10528：`REWARD/var0=11` 纠正为 12；交付（806292 `SET_SUCCEED`）后任务书显示第 12 行“和代理人维达对话”。
3. 10525 / 20525：`REWARD/var0=6` 纠正为 7；使用 `182216072`（DF6 为 `182216084`）后任务书应显示
   第 7 行“和代理人维达对话”，不再是第 6 行的“使用 quest_10525d”。

## 七、批次 4：折叠报告行拆分 + SECTION_0 位段隔离（2026-09-21，用户授权后执行）

### 七之一、10529 / 20529：报告行拆成独立状态，领奖行回到第 11 行

- 客户端 `quest_q10529.html` 共 12 行：第 9 行“和 IDLF6_Q_Down_Dejabobo_E 对话”、
  第 10 行“带上陷入沉睡的德扎波波，向代理人维达报告”、第 11 行“和代理人维达对话”。
- 服务端原本只有 `0..9` 的 START 状态 + `reward` 投影 10：第 10 行被压在 REWARD 上，
  第 11 行（真正的领奖行）没有任何状态，玩家回到主城后任务书一直停在“报告”一行。
- 旧 handler `_10529Protection_Artifact_2` 的 `STEP_TO_10` 在同一分支里
  `giveQuestItem(182216107)` + `qs.setStatus(REWARD)` + `changeQuestStep(env, 9, 10, false)`：
  迁移把“报告行”和“领奖态”合并成了一个状态，`to` 看起来没有产物。
- 修复（天/魔同形）：
  1. 新增 START 节点 `s10`（`var0=10`）；
  2. `s9 → s10`（806294 / 806299 `SETPRO10`，保留给物品与传送回主城的 after-commit）；
  3. 新增 `s10` 与领奖 NPC 的报告链：`QUEST_SELECT→SELECT11`、`SELECT11_1→SELECT11_1`、
     `SELECT11_1_1→SELECT11_1_1`、`SET_SUCCEED→reward`（写 `var0=11`）；
  4. `reward` 投影 10→11，`var0 max` 10→11；
  5. 无 source `enter-world` 自愈边：`REWARD && var0==10 → 11`（旧存档）。
- 客户端页面 `select11_1_1`（id 6502，旧 handler 的 `SELECT_ACTION_6503` 同族）在服务端枚举里缺失，
  本轮补上 `QuestDialogPage.SELECT11_1_1(6502)` 与 `QuestDialogAction.SELECT11_1_1(6502)`，
  否则报告链在 `select11_1` 处没有下一页可下发。

### 七之二、10530：与镜像 20530 对齐到 reward=9

- `quest_q10530.html` 有 11 行，但第 8 行是空行且与第 9 行共用 `visible="[%24]"`（同一状态的两行文本）；
  去重后的可见槽位集合 `{0,3,6,9,12,15,18,21,24,27}` 与魔族镜像 `quest_q20530.html` 完全一致。
- 修复：`reward` 投影 8→9（`var0 max` 已是 9），补无 source `enter-world` 自愈边
  （`REWARD && var0==8 → 9`），与 20530 的同名自愈边对称。
- **已知偏差**：审计脚本按“行号”判据会把 10530 报成“缺第 10 行”，这里以“与镜像可见槽位集合一致 +
  旧 handler 的步骤序列（0..8）”为准；`client_rows != client_states` 的任务需要人工复核，不能直接套行号模板。

### 七之三、3090：SECTION_0 位段污染（var1/var2 原先落在任务书行字段里）

- 3090（Theobomos“In Search Of Pippi The Porgus”）的 progress 原本是
  `var0@0 w2 + var1@2 w1 + var2@3 w1`：var1/var2 落在 SECTION_0（bit 0..5）内，
  而客户端是把 `questVars` 的低 6 位当任务书行索引读的。
  于是 `s1-zone`（var0=1, var1=1）实际下发 `SECTION_0 = 1 | (1<<2) = 5`、
  `s1-object`（var0=1, var2=1）下发 `1 | (1<<3) = 9`，行号被计数位污染。
- 独立旁证：客户端 quest_script 声明 `Progress(SECTION_1==0; SECTION_0==1)`，
  要求 var1 落在 SECTION_1（bit 6）而不是 bit 2；旧 handler 的
  `changeQuestStep(env, 0, 1, false, 2)` / `(env, 0, 1, false, 1)` 也把两个标记当作独立变量段。
- 全库核对：6161 个带位段任务里 6140 个满足“varN 在 6N”（99.7%），只有 21 个例外，3090 是其中之一。
- 修复：
  1. progress 改为 `var0@0 w3 max=4` + `var1@6 w1` + `var2@12 w1`（三者仍在各自 SECTION，互不污染）；
  2. `reward` 投影 3→4（`QUEST_Q3090.html` 共 5 行，末行“向 LF2A_NPC_Morati 报告”才是领奖行，
     第 3 行是“找到皮皮，并关在捕兽器里”）；
  3. 无 source `enter-world` 自愈边：`REWARD && var0==3 → 4`。
- 新增全库门禁 `ClientQuestSectionAlignmentTest#sectionNamedCountersStayAtTheirFixedSixBitOffset`：
  遍历生产目录，要求所有 `varN`（N≥1）字段落在 `6N`；当前仅剩 14 个历史任务例外并在测试里显式挂账
  （1842/1843/1844/2843/2844/2845、4928、16800、18738、19078、20034、28738、29074、29078）。
  修好一个就必须从清单里删一个，否则门禁红。

### 七之四、未纳入（人工挂账）

- `24201/24202`：客户端 quest_script 里 24201 的声明是 `Progress(SECTION_0<12; SECTION_5==0)`、
  24202 是 `Progress(2)`。24201 的 `SECTION_0` 参与“<12”比较，很可能同时充当击杀计数槽
  （HTML 第 2 行就是 `([%11]/12)`），与“行索引”模型冲突；本轮**没有**按行号模板补
  `stage1(var0=1)`，需要先做客户端观测再决定是补 START 阶段还是改计数模型。
  两者当前只有 `started(var0=0)` 与 `reward(var0=2)`，形状 `INTERIOR_GAP`。
- 七之三清单里其余 14 个 SECTION 位段历史任务：逐个需要客户端脚本证据才能动，不得机械套用。
- `10522/20522`：引擎外直写 REWARD（创造力点数），走 QE-046 合同，仍在挂账。

### 七之五、验证（2026-09-21，用户授权后执行）

```bash
mvn -B test -Dtest='JournalReportRowSplitContractTest,JournalRewardRowRepairContractTest,ClientQuestSectionAlignmentTest,ArchdaevaRewardRowContractTest,AlignedMirrorRewardRowContractTest,MirrorPairRewardRowContractTest,ProductionCatalogWhitelistVerificationTest,QuestDefinitionCatalogManifestTest,ExternalRewardAdvanceReentryContractTest,QuestCollectProgressAlignmentGateTest'
```

- 结果：`Tests run: 49, Failures: 0, Errors: 0, Skipped: 0`；`PRODUCTION_COMPILE_OK=6189`、
  `PRODUCTION_COMPILE_FAILURES=0`、`PRODUCTION_INTERACTION_OBJECT_FAILURES=0`、`PRODUCTION_WHITELIST_VIOLATIONS=0`。
- `xmllint --schema quest_definition.xsd`：本次改动的 `3090.xml`、`10529.xml`、`20529.xml`、`10530.xml` 全部 `validates`。
- 首轮失败记录：`JournalReportRowSplitContractTest#carrierHandoverEntersTheReportRow` 曾因测试内快照没有
  携带 20529 需要扣除的 `182216090/91/92` 而 `NoSuchElementException`（planner 拒绝不可行的 remove-item），
  补齐快照背包事实后通过——这属于测试夹具问题，不是生产缺陷。
- 客户端复测路径（PENDING_CLIENT）：
  1. 10529/20529：旧存档 `REWARD/var0=10` 登录应自愈为 11，任务书显示最后一行“和代理人维达对话”；
     用 `182216107`（魔族 `182216108`）状态的存档回城后先看到第 10 行“带上沉睡的德扎波波报告”，
     与代理人对话（806075/806079）走 `select11 → select11_1 → select11_1_1` 后进入 REWARD。
  2. 10530：`REWARD/var0=8` 登录应自愈为 9，任务书显示最后一行“再次和 Jucleas 对话”。
  3. 3090：`REWARD/var0=3` 登录应自愈为 4，任务书显示“向 LF2A_NPC_Morati 报告”；
     同时核对 `s1-zone`/`s1-object` 过渡期间任务书第 2 行是否仍高亮（不再被 var1/var2 顶到第 4/5 行）。

## 八、批次 5：感应区行推进丢失（导览/坐骑族 10 个，2026-09-21 用户授权后执行）

### 八之一、候选与证据（这批不是“只差一行”，而是行推进整条被迁移丢掉）

审计把这 10 个任务记为 `MISSING_LAST_ROW + ROW_WITHOUT_STATE`（客户端 3 行，服务端只到 `var0=0..1`）：

| 任务 | 客户端 quest_summary 的 3 行 | 旧 handler（origin/history） |
| --- | --- | --- |
| 15550 / 25550 | 和 `LF6_Volter_E`/`DF6_Svanhild_E` 对话、和 `LF6_Ador_E`/`DF6_Conrto_E` 对话、和 `LF6_Aquaris_E`/`DF6_Vadorei_E` 对话（领奖行） | `_15550Iluma_Field_Guide` / `_25550A_Norsvold_Story`：`STEP_TO_1` 让 var0 0→1、`STEP_TO_2` 让 var0 1→2、领奖 NPC 的 `SELECT_REWARD` 置 `REWARD` |
| 15551-15554 / 25551-25554 | 使用 A→X 吸引物、使用 X→A 吸引物、和 `Aquaris`/`Vadorei` 对话（领奖行） | `_15551Giddyup_Starturtle`、`_15552Surfing_The_Ancient_Well`、`_15553Ride_An_Iluman_Butterfly`、`_15554A_Rickety_Ride` 与魔镜像四个脚本：`onEnterZoneEvent` 在 `A_TO_X` 感应区把 var0 0→1、在 `X_TO_A` 感应区把 var0 1→2 并 `setStatus(QuestStatus.REWARD)` |

根因：迁移提交 `79bc5d3a6`（"migrate enter-zone auto-accept dialog quest handlers"）退役这一族旧 handler 时，
只把“进入感应区自动接取”搬成了 `unaccepted → started`，**把 legacy 的行推进整条丢掉**。
后果不是“停在上一行”而是更彻底：第 2 行没有任何 START 状态（永远不亮），
reward 投影停在倒数第二行（var0=1），第一段坐骑之后任务书一直显示第 1 行、最后一步也不会切到“和领奖 NPC 对话”。

### 八之二、修复

- Group A（15550/25550，三段对话链）：
  1. `reward` 投影 1 → 2（客户端领奖行；旧 handler 的 Ador/Conrto `STEP_TO_2` 已经走到 var0=2）；
  2. `s1 → reward` 交接写 `var0=2`；
  3. 无 source `enter-world` 自愈边：`REWARD && var0==1 → 2`。
- Group B（8 个坐骑任务，天/魔同形）：
  1. `var0` 位段 `width 1 → 2`、`min 0 / max 1 → max 2`（reward 行 2 超出旧位段会在生产目录编译期报
     `bit-field value range exceeds its declared width`）；
  2. 新增 START 节点 `s1`（`var0=1`），`reward` 推到 2 —— 客户端 3 行各自都有状态；
  3. 保留迁移原有的两条 `unaccepted → started` 感应区自动接取（`start-eligible` + `VISIBILITY_REFRESH`），不退化成“只能从 NPC 接取”；
  4. 恢复 legacy 的两条行推进（与 26800 的 `DF_TOWER_SENSORY_AREA` 同型）：
     `started + enter-zone A_TO_X → s1`（条件 `var0==0`、动作 `var0=1`、`PACKET_ONLY`）与
     `s1 + enter-zone X_TO_A → reward`（条件 `var0==1`、动作 `var0=2`、`LEVEL_AND_VISIBILITY_REFRESH`）；
  5. `started → reward`（领奖 NPC `SELECT_QUEST_REWARD` 报告路线）写 `var0=2`；
  6. 无 source `enter-world` 自愈边：`REWARD && var0==1 → 2`。
- 新增门禁 `src/test/java/com/aionemu/gameserver/questEngine/definition/SensoryAreaRideRowContractTest.java`
  （8 条合同）：逐任务锁定三行状态与位段、两条感应区自动接取、两条推进路线的条件/动作/after-commit、
  planner 两段推进结果（`START/var0=0 → START/var0=1`、`START/var0=1 → REWARD/var0=2`）、自愈边条件/动作/after-commit
  与 planner 自愈结果、“任何进入 reward 的路线都不得写过期的行号”、领奖交接仍开奖励窗口，以及天/魔镜像同形
  （节点投影 + enter-zone 路线形状一致）。
- `MirrorPairRewardRowContractTest` 追加 `15550/25550` 两条合同（同一“双侧缺末行”合同的三段对话链变体）。

### 八之三、边界与已知偏差

- **自动接取的固有粒度（本轮未改）**：玩家从未接任务时进入 `A_TO_X` 感应区会直接以 `var0=0`（第 1 行）接取，
  需要再次经过该感应区才推进到第 2 行；这是迁移“进入即接取”模型的既有取舍（保留它是为了不回退用户可见行为），
  retail 主路径（先与领奖 NPC 接取、再坐两段）逐行正确。若客户端实测认为“进入即接取应直接记第 1 行已完成”，
  再单独设计 `unaccepted + A_TO_X → s1` 并复核 QE-051 的行状态清单。
- **报告捷径**：`started → reward`（领奖 NPC `SELECT_QUEST_REWARD`）是迁移既有路线，本轮只把它写的行号对齐到领奖行，
  未改动其可达性策略（旧 handler 只在 `REWARD` 态处理该 NPC，属同一族待观测项）。
- 本轮**未**按“末行 NPC 命中即可批量推进”继续扩大批量：审计 `[5]` 的 104 个候选里大部分只有行号证据，
  缺这一族这样的 legacy 行推进证据（详见 `audit-qe051-candidates.tsv` 与 QE-051 的边界条款）。

### 八之四、验证（2026-09-21，用户授权后执行）

```bash
mvn -B test -Dtest='SensoryAreaRideRowContractTest,JournalReportRowSplitContractTest,JournalRewardRowRepairContractTest,ClientQuestSectionAlignmentTest,ArchdaevaRewardRowContractTest,AlignedMirrorRewardRowContractTest,MirrorPairRewardRowContractTest,ProductionCatalogWhitelistVerificationTest,QuestDefinitionCatalogManifestTest,ExternalRewardAdvanceReentryContractTest,QuestCollectProgressAlignmentGateTest'
```

- 结果：`Tests run: 57, Failures: 0, Errors: 0, Skipped: 0`；`PRODUCTION_COMPILE_OK=6189`、
  `PRODUCTION_COMPILE_FAILURES=0`、`PRODUCTION_INTERACTION_OBJECT_FAILURES=0`、`PRODUCTION_WHITELIST_VIOLATIONS=0`。
- `xmllint --noout --schema quest_definition.xsd`：本轮 10 个文件全部 `validates`；IDE lint 0 警告；`git diff --check` 干净。
- 全库审计同工作树前后对照（先 `git checkout HEAD --` 这 10 个文件取基线、跑完审计再回放修复版本）：
  `ROW_ALIGNED 2556 → 2566`、`ROW_BEHIND 287 → 277`、`ROW_STATE_ALIGNED 2338 → 2348`、
  `ROW_WITHOUT_STATE 607 → 597`、`ALIGNED 2338 → 2348`、`MISSING_LAST_ROW 159 → 149`，
  10 个任务逐条转为 `ALIGNED + ROW_ALIGNED`（`visible_state_var0 = 0 1 2`，`reward_row_within_field_max=True`）。
- 客户端复测路径（PENDING_CLIENT）：
  1. 15550/25550：第 1、2 行分别是与 Volter/Svanhild、Ador/Conrto 的对话，第 3 行“和 Aquaris/Vadorei 对话”；
     旧存档若为 `REWARD/var0=1`，登录时应自愈为 2 并高亮第 3 行。
  2. 15551-15554/25551-25554：坐完第一段吸引物后任务书应从第 1 行切到第 2 行（`PACKET_ONLY` 状态包），
     坐完第二段应直接进入领奖行并可在领奖 NPC 处领奖；旧存档 `REWARD/var0=1` 登录自愈为 2。

## 九、批次 6：竞技场两阶段行推进与领奖行对齐（苍穹试炼场单人竞技场 4 个，2026-09-21 用户授权后执行）

### 九之一、候选与证据（SECTION_0 约束审计 + 行审计双入口命中）

这一批来自两条独立审计线的交集：

1. `check_section0_requirements.py`（本轮新增）：把客户端 `quest_script_monster.csv` 里每个
   `SECTION_0==N` 声明与服务端 typed 定义的 `var0` 可达值比对，初始 5 个 `UNREACHABLE`——1922、2947、
   14054、18208、18209（魔镜像 28208/28209 同族）。18208/18209/28208/28209 的客户端声明是
   `SECTION_0==0; SECTION_1<5`（5 次 `IDArena_Solo_S6_VanqJr_55_An` 击杀）与
   `SECTION_0==1; SECTION_2<1`（精英击杀，匹配 `IDArena_Solo_H1_DrakanAs_noble_55_Ae` 与
   `IDArena_Solo_H2_TempleD_Fi_55_Ae`），迁移后的服务端却产不出这些值。
2. 行审计（`audit_reward_row_vs_client_steps.py`）：客户端 `quest_q18208/18209/28208/28209` 的
   quest_summary 各 3 行——第 1 行“消灭第 2 神庙训练所的最终训练对象 ([%2]/5)”、第 2 行
   “穿过…消灭 quest_18208a ([%5]/1)”、第 3 行“和 Molfus [%]对话”（领奖行）。迁移把天族侧的击杀路线
   整条丢掉（reward 投影 0、只剩“对话即领奖”）、魔族侧改成 `k1..k7`（var0=1..7，每个击杀一个阶段）且
   reward=7：
   - 天族：`MISSING_TAIL_ROWS`，行 1、2 永远不亮；
   - 魔族：`STATES_BEYOND_ROWS`（var0 3..7 落在客户端不存在的行号上），并把客户端未声明的
     218192（`IDArena_Solo_H2_TempleL`）当成必须击杀的目标。

旧 handler 证据（origin/history，7e9f0316c 之前，仓库内已删除）：`_18208IllusionOrInfiltration`、
`_28208ARiftAdrift` 等天/魔两侧同形——`var0==0` 时 `defaultOnKillEvent(env, 217819, 0, 4, 1)` 用 var1
累加 0..4，`var1==4` 的第 5 次击杀 `qs.setQuestVar(1)`；`var0==1` 时击杀 218185 写 `var2=1` 并
`setStatus(REWARD)`（进入 REWARD 时不再改写 var0）。H1 龙族贵族（218185）与 H2 神殿守护者（218200）
在客户端 script 里共用 `SECTION_0==1; SECTION_2<1` 声明。

### 九之二、修复

- 四份定义统一为“2 阶段 + 2 计数器”模型：`var0@0`（任务书行号 0/1/2）、`var1@6`（5 次击杀计数，max 4）、
  `var2@12`（精英标志，max 1）；计数位不写进节点投影（否则自环累加后匹配不到 source 节点）。
- 节点：`unaccepted(0) / started(0) / s1(1) / reward(2) / complete(0)`——第 3 行“和 Molfus 对话”按
  QE-051 领奖行合同投影到 2（与同型任务 2620/4210 的修复一致；旧 handler 停在 1 的“pre-REWARD step”
  读法被行状态判据取代）。
- 击杀路线：`started + kill 217819 (var1<4) → increment var1`（PACKET_ONLY）、
  `started + kill 217819 (var1==4) → s1`（PACKET_ONLY）、
  `s1 + kill 218185|218200 (var2==0) → set var2=1 → reward`（目标投影把 var0 补成 2）。
- 旧存档收敛边（无 source enter-world，4 条，均为 `LEVEL_AND_VISIBILITY_REFRESH`）：
  1. `START && var0>=1 && var0<5 && var1<4 → set var0=0`（旧 k1..k4 回第 1 行重算；
     `var1<4` 守卫保证正常 s1（var1=4）不被误伤）；
  2. `START && var0>=5 → set var0=1 + set var1=4`（旧 k5..k7 进第 2 行）；
  3. `REWARD && var0<2 → set var0=2`（天族旧投影 / “对话即领奖”存档）；
  4. `REWARD && var0>=3 → set var0=2`（魔族旧 k3..k7 的领奖态）。
- 保留迁移既有的 `NPC_REPORT（领奖 NPC）started → reward` 捷径与 `reward` 态 `QUEST_SELECT` 自环，
  领奖 NPC 处 `npc-complete` 打开奖励窗口不变。
- 新增门禁 `src/test/java/com/aionemu/gameserver/questEngine/definition/ArenaPhaseRowContractTest.java`
  （6 条合同）：逐任务锁定三行状态与位段（var0 行号、var1@6、var2@12、var0 max ≥ 2）、
  5 次击杀累加与推进、精英击杀提交 REWARD（var0=2）、4 条旧存档收敛边的条件/动作/planner 结果
  （含“正常 s1 不被误伤”）、任何进入 reward 的路线不得写过期的行号、天/魔镜像同形。
  可重放脚本：`.agents/summary/quest-10527-reward-row/apply_batch6_arena_phase_rows.py`。

### 九之三、边界与已知偏差

- **旧 k2..k4 的进度损失**：迁移版把每个 217819 击杀写成 var0 阶段、且没有把击杀数写进 var1，
  旧存档无法恢复精确计数，收敛边只能把 k2..k4 拉回第 1 行重算（k5..k7 的第一行已完成，映射到第 2 行）。
- **NPC_REPORT 捷径**：`started → reward` 仍是迁移既有的“对话即领奖”捷径，本轮未收紧（与 QE-051 边界一致）。
- 218192（H2 TempleL）从击杀目标中移除：客户端 script 只声明 218185/218200。
- 与 QE-045 的关系：本任务 reward 行号按 QE-051 行状态判据（领奖行=2）收口，不属于 QE-045 的
  “reward 保持 pre-REWARD step”例外族（那一族有 15300/25300 等客户端验收记录）。

### 九之四、验证（2026-09-21，用户授权后执行）

```bash
mvn -B test -Dtest='ArenaPhaseRowContractTest,SensoryAreaRideRowContractTest,JournalReportRowSplitContractTest,JournalRewardRowRepairContractTest,ClientQuestSectionAlignmentTest,ArchdaevaRewardRowContractTest,AlignedMirrorRewardRowContractTest,MirrorPairRewardRowContractTest,ProductionCatalogWhitelistVerificationTest,QuestDefinitionCatalogManifestTest,ExternalRewardAdvanceReentryContractTest,QuestCollectProgressAlignmentGateTest'
```

- 结果：`Tests run: 63, Failures: 0, Errors: 0, Skipped: 0`；`PRODUCTION_COMPILE_OK=6189`、
  `PRODUCTION_COMPILE_FAILURES=0`、`PRODUCTION_INTERACTION_OBJECT_FAILURES=0`、`PRODUCTION_WHITELIST_VIOLATIONS=0`。
- `xmllint --noout --schema quest_definition.xsd`：本轮 4 个文件全部 `validates`；IDE lint 0 警告；`git diff --check` 干净。
- `check_section0_requirements.py`：`UNREACHABLE 5 → 3`（18208/18209/28208/28209 及镜像全部转 `OK`，
  剩 1922、2947、14054）。
- 全库审计同工作树前后对照（先 `git checkout HEAD --` 这 4 个文件取基线、跑完审计再用脚本回放）：
  `ROW_ALIGNED 2566 → 2570`、`ROW_BEHIND 277 → 275`、`ROW_AHEAD 2593 → 2591`、
  `ROW_STATE_ALIGNED 2348 → 2352`、`ROW_WITHOUT_STATE 597 → 595`、`STATE_OUT_OF_RANGE 2450 → 2448`、
  `MISSING_TAIL_ROWS 92 → 90`、`STATES_BEYOND_ROWS 2627 → 2625`、`ALIGNED 2348 → 2352`
  （`MISSING_LAST_ROW 149`、`INTERIOR_GAP 267`、`BOTH_MISALIGNED 177` 不变）；
  两轮审计输出逐行 diff 只有这 4 个任务发生变化，全部转为
  `ALIGNED + ROW_ALIGNED + ROW_STATE_ALIGNED`（`visible_state_var0 = 0 1 2`）。
- 客户端复测路径（PENDING_CLIENT）：
  1. 18208/18209（天族）：杀 5 只 VanqJr 后任务书从第 1 行（n/5）切到第 2 行（n/1），
     击杀 H1 龙族贵族/H2 神殿守护者后应直接进入领奖行（“和 Molfus 对话”）并能在 Molfus 处领奖；
     旧存档 `REWARD/var0=0` 登录自愈为 2。
  2. 28208/28209（魔族）：同上，领奖 NPC 为 205320/205321；旧存档 `START/var0=2..4` 登录回第 1 行、
    `START/var0=5..7` 进第 2 行、`REWARD/var0=7` 收敛到领奖行（var0=2）。

## 十、剩余 UNREACHABLE 三族的处置结论：实现缺口，不在行投影范围

`check_section0_requirements.py` 复跑后仍剩 3 个 `UNREACHABLE`：1922、2947、14054。逐族核对客户端
quest_summary/quest_script 与 origin/history 旧 handler 后，结论是这三族**不是行投影错位，而是
AionEmu 原实现缺口**（legacy handler 从未实现客户端声明的中间阶段），因此不按 QE-051 的
“领奖行投影 + 自愈边”机械修复：

| 任务 | 客户端声明（quest_script） | 客户端行数 | legacy 实际实现 | 缺口 |
| --- | --- | --- | --- | --- |
| 1922 / 2947 | `SECTION_0==2`（Baku/Cures/Varanus 等 ×3 的野外击杀）、`SECTION_0==5`（竞技场 ×10） | 10 | var0 0→4→5→7（2947 为 0→4→5→9）→ REWARD：`_1922Deliveron_Your_Promises` 用 `defaultCloseDialog(env, 0, 4)` 直接跳过 1..3；野外击杀、收集物与中间对话链从未实现 | var0=1/2/3（对话 + 收集物 + 野外击杀）及其余任务书行对应状态 |
| 14054 | `SECTION_0==2/3`（第一轮 6+3、Noble_Vritra）、`SECTION_0==12/13`（第二轮 6+3、NagaFighterNM） | 9 | `_14054Kralling_To_Kralltumagna`：var0 0→1→2→3→4→REWARD，只实现第一轮击杀（var1 0..5、var2 0..3、233861） | 第二轮（12/13）、Barantina 两次对话、NagaFighterNM_47_AhN / LF3_NagaFighterNM_47_Q_Ah、最终向 Javlantia 报告 |

修复这些缺口需要**单独设计**：补对话链、传送、掉落与击杀阶段（1922 还包括失败重试与竞技场计时），
NPC/道具/地图数据要逐页核对客户端 HTML（`quest_q1922.html`、`quest_q2947.html`、`quest_q14054.html`），
风险与工作量都远超“行投影对齐”。本轮已用 SECTION_0 约束审计把它们从“疑似投影错位”里分流出来；
后续若要实现，建议单独立项（每族一份证据表 + 阶段设计 + 客户端实测记录）。

**审计口径说明**：`check_section0_requirements.py` 的 `UNREACHABLE` 只表示“客户端 quest_script 声明的
`SECTION_0` 值在当前服务端不可达”。对 18208/18209/28208/28209（批次 6）它是**迁移丢阶段**（可修，本轮已修）；
对 1922/2947/14054 它是 **legacy 从未实现**（需单独立项）。两类缺陷的探测判据相同、处置不同。

## 十一、批次 7：报告/交付型末行（成长任务族 + 3210 + 18036/28036，2026-09-21 用户授权后执行）

### 十一之一、候选与证据

这一批来自用户点选的候选第 3 项（`audit-qe051-candidates.tsv` 的 `MISSING_LAST_ROW` 存量）与候选第 2 项的
SECTION_0 直证族收口，收口标准与批次 3 一致：**客户端行清单 + 末行 NPC 命中 reward 路线 + 天/魔镜像或同型模板**。

| 组 | 任务 | 客户端行清单 | 服务端旧状态 | 证据 |
| --- | --- | --- | --- | --- |
| A（18） | 19672 / 19677 / 19684 / 19685 / 19686 / 19687 / 19688 / 19689 / 19694 + 镜像 29672 / 29677 / 29684 / 29685 / 29686 / 29687 / 29688 / 29689 / 29694 | 2 行：收集 N 个成长货币、向成长支援教官报告 | `unaccepted/started/reward/complete` 全部投影 var0=0，无任何事务写 var0 | 定义里只有唯一 NPC（天 806698 = `LC1_L_grow_npc_Rena_01`、魔 806700 = `DC1_D_grow_npc_Melrania_01`），接取与领奖共用；天/魔镜像同形 |
| B（1） | 3210 | 3 行：救出 `Shugo_Shulack_01`、除掉海盗团头目、和 `Shugo_Shulack_02` 对话 | 已有 `k1(var0=1)`、`k2(var0=2)` 与 `k2→reward`，但 reward 投影仍是 0（ROW_BEHIND） | 镜像 4210 的 reward 已是 2 且带 `REWARD/var0=1 → 2` 自愈边；旧 handler `_3210Rescue_Haorunerk`（origin/history）用 `defaultOnKillEvent` 推进两个击杀阶段 |
| C（2） | 18036 / 28036 | 2 行：和 `LDF5b_IDLDF5b_TD_Drakan_Fighter` 对话、向 `LDF5b_Demades_E` / `LDF5b_Latkel_E` 报告 | `started(SETPRO1)` 事务写 var0=1，但 `started` 节点投影仍是 0；两条 `var0>=1` 事务以 `started` 为 source | 客户端行 1 的 NPC 必须在任务内：18036 的 801281/802008、28036 的 801280/802015 与末行、首行文本逐一对上 |

C 组的严重性高于“末行不亮”：`QuestMutationPlanner#matchesSourceNode` 要求 source 节点的每个投影变量都与实际
packed 值全等（`src/main/java/com/aionemu/gameserver/questEngine/runtime/QuestMutationPlanner.java:340` 起），
旧定义把 var0 写成 1 之后，`started`（投影 var0=0）已不匹配任何报告/领奖事务，玩家交出物品后**任务卡死**，
既不能报告也不能领奖。批次 2 曾以“条件不自洽”把这两个任务排除，本批次给出节点级修复。

### 十一之二、修复

- A 组：`reward` 投影 `0 → 1`，并补无 source 的 `enter-world` 自愈边（`REWARD && var0==0 → set var0=1` +
  `LEVEL_AND_VISIBILITY_REFRESH`）。
- B 组：`reward` 投影 `0 → 2`，补两条自愈边覆盖旧存档（`REWARD && var0==0 → 2`、`REWARD && var0==1 → 2`）。
- C 组：新增 `<node label="s1" status="START"><var name="var0" value="1"/></node>`；`started(SETPRO1)` 事务
  target 改为 `s1`；报告 NPC 的 `QUEST_SELECT` 事务 source/target 改为 `s1`；`SELECT_QUEST_REWARD` 事务
  source 改为 `s1`；`reward` 投影 `0 → 1` 并补 `REWARD && var0==0 → 1` 自愈边。
- 21 个文件都写入中英双语 QE-051 注释（客户端文件名、行数、领奖 NPC id、旧投影值）。
- 可重放脚本：`.agents/summary/quest-10527-reward-row/apply_batch7_report_row_contract.py`（`--check` 只校验，
  默认应用；证据表 `batch7-evidence.tsv` 记录每个任务的客户端行数/行文本/领奖 NPC）。
- 门禁：`src/test/java/com/aionemu/gameserver/questEngine/definition/ReportRowRewardProjectionContractTest.java`
  （5 条合同：末行投影与位段容量、每个旧值都有 enter-world 自愈边并能被 planner 规划、进入 reward 的路线不得写
  过期行号、C 组的 `s1` 节点与三条事务形状、成长任务天/魔镜像同形）。

### 十一之三、边界与已知偏差

- **成长任务末行的字典名与 NPC 名表不同名**：末行 `STR_DIC_N_Grow_Main_NPC_L` / `_D` 与客户端 NPC 名表里的
  `LC1_L_grow_npc_Rena_01` / `DC1_D_grow_npc_Melrania_01` 不是同一个字符串，审计脚本的“末行 NPC 命中”判据记为
  `False`；但这两个任务只有唯一 NPC（接取与领奖同一个人），因此按“单 NPC 任务”收口，并在 §十一之一与证据表记录。
- **26838 / 30614 未纳入**：末行 NPC 与定义 NPC 不一致——26838 末行是 `Jarik01`（806574）而定义用 806575，
  30614 末行是 `Astella`（800327）而定义用 800326（`Aluna`）；需先核实是客户端字典陈旧还是定义接错 NPC。
  26838 的镜像 16838 用 `k1(var0=1)` 覆盖第 2 行（reward 仍投影 0），形状被判 `ALIGNED`，本批次未改动它。
- **QE-046 引擎外写入者 8 个不得只改 XML**：10522 / 20522 / 15542 / 25542 / 30211 / 30213 / 30311 / 30313 的
  reward 投影必须等于写入方（`CM_CREATIVITY_POINTS`、`CoalescenceService`、`RiftOrbAI2`）留下的 packed step
  （批次 7 时为 0），`ExternalRewardAdvanceReentryContractTest` 会直接校验；要修必须先改写入方并重刷
  `src/test/resources/quest/external-reward-advance-baseline.tsv`。**该挂账已由批次 8 收口（见 §十二）**：
  写入方改为先写领奖行 `var0=1` 再置 `REWARD`，投影同步 `0 → 1`，基线重刷为 `writer step=1 / projection=1`。
- **其余未收口候选**：`started` 无 var 投影导致行 0 缺状态的 28932 / 30203 / 30303；多阶段/内部缺口
  （`INTERIOR_GAP` / `MISSING_TAIL_ROWS`）的 1607 / 1990 / 2990 / 3502 / 14012 / 14013 / 17511 / 27511；
  领奖 NPC 待核实的 19064 / 29064；legacy 从未实现的 1922 / 2947 / 14054（见第十节）。
- 剩余 `MISSING_LAST_ROW` 仍有 129 个（其中 62 个末行 NPC 对不上任务内 NPC），按同一标准逐批收口，禁止
  “末行是对话行就批量推进”。

### 十一之四、验证（2026-09-21，用户授权后执行）

```bash
mvn -B test -Dtest='ReportRowRewardProjectionContractTest,ArenaPhaseRowContractTest,SensoryAreaRideRowContractTest,JournalReportRowSplitContractTest,JournalRewardRowRepairContractTest,ClientQuestSectionAlignmentTest,ArchdaevaRewardRowContractTest,AlignedMirrorRewardRowContractTest,MirrorPairRewardRowContractTest,ProductionCatalogWhitelistVerificationTest,QuestDefinitionCatalogManifestTest,ExternalRewardAdvanceReentryContractTest,QuestCollectProgressAlignmentGateTest'
```

- 结果：`Tests run: 68, Failures: 0, Errors: 0, Skipped: 0`（含新增 `ReportRowRewardProjectionContractTest` 5 例）；
  `PRODUCTION_COMPILE_OK=6189`、`PRODUCTION_COMPILE_FAILURES=0`、`PRODUCTION_INTERACTION_OBJECT_FAILURES=0`、
  `PRODUCTION_WHITELIST_VIOLATIONS=0`。
- `xmllint --noout --schema quest_definition.xsd`：本轮 21 个文件全部 `validates`；IDE lint 0 警告；`git diff --check` 干净。
- 全库审计同工作树前后对照（先回放批次 7 前的 21 个文件取基线、跑完审计再回放修复版本）：
  `ROW_ALIGNED 2570 → 2591`、`ROW_BEHIND 275 → 254`、`ROW_STATE_ALIGNED 2352 → 2372`、
  `ROW_WITHOUT_STATE 595 → 575`、`ALIGNED 2352 → 2372`、`MISSING_LAST_ROW 149 → 129`
  （`ROW_AHEAD 2591`、`STATE_OUT_OF_RANGE 2448`、`INTERIOR_GAP 267`、`STATES_BEYOND_ROWS 2625`、
  `BOTH_MISALIGNED 177` 不变）；两轮审计输出逐行 diff 只有这 21 个任务变化，全部转为
  `ROW_ALIGNED + ROW_STATE_ALIGNED`（3210 由 `ROW_BEHIND` 转 `ROW_ALIGNED`，其余 20 个由
  `MISSING_LAST_ROW + ROW_WITHOUT_STATE` 转 `ALIGNED + ROW_STATE_ALIGNED`）。
- `check_section0_requirements.py` 复跑：`UNREACHABLE` 仍为 3（1922 / 2947 / 14054），3210 保持 `OK`。
- 客户端复测路径（PENDING_CLIENT）：
  1. 成长任务（19672 一类）：收集满成长货币后与教官对话交给物品，任务书应切到第 2 行“向成长支援教官报告”
     并能打开奖励窗口；旧存档 `REWARD/var0=0` 登录后应自愈为 1。
  2. 3210：击杀 `Shugo_Shulack_01` 与头目后任务书应停在第 3 行“和 Shugo_Shulack_02 对话”，
     领奖后不再回到第 1 行；旧存档 `REWARD/var0=0/1` 登录后都应自愈为 2。
  3. 18036 / 28036：与德拉坎战士交出物品后任务书应切到第 2 行“向 Demades / Latkel 报告”，
     并且报告 NPC 能正常打开奖励窗口（修复前该状态没有任何可匹配事务）。

### 十一之五、候选 2（SECTION_0 直证族）剩余分类

`check_section0_requirements.py` 复跑后 42 个“客户端 quest_script 明写 SECTION_0”任务的分布：

| 判定 | 数量 | 任务 / 处置 |
| --- | --- | --- |
| `OK` | 34 | SECTION_0 取值可达，其中 3210 本批次随 reward 投影一起收口（保持 OK） |
| `UNREACHABLE` | 3 | 1922 / 2947 / 14054 —— legacy 从未实现客户端声明的中间阶段，需单独立项（见第十节） |
| `NO_SECTION0_FIELD` | 5 | 3031 / 3725 / 4725 / 11468 / 21468 —— var0 不落在 SECTION_0（恒 0），与 `SECTION_0==0` 声明一致，非缺陷 |
| `NO_DEFINITION` | 43 | 客户端有 SECTION_0 声明但 `quest_definition/quests` 下没有定义文件（1039、2015、2057、9685、10020-10022、10073、11250-11300、18402、18502、20021-20024、20073、21250-21301、24071、28402、28502、41556）——属未实现/未迁移任务，不是行投影错位，需要按任务链立项 |

`OK` 判定只说明“客户端声明的 SECTION_0 值在服务端可达”，不等于“每一行都有状态”。仍被判错位（`ROW_BEHIND` /
`ROW_WITHOUT_STATE`）的 SECTION_0 任务及处置：11149（QE-045 回归锁，按客户端验收值固定）；10522 / 20522 /
15542 / 25542 / 30211 / 30213 / 30311 / 30313（QE-046 引擎外写入者，见 §十一之三）；28932 / 30203 / 30303
（`started` 节点无 var 投影，行 0 缺状态，需要补投影）；1607 / 1990 / 2990 / 3502 / 14012 / 14013 / 17511 / 27511
（多阶段或内部缺口，需要独立的阶段设计）。

## 十二、批次 8：QE-046 引擎外写入者与领奖行求交（8 个任务，2026-09-21 用户授权后执行）

### 十二之一、候选与证据（两条合同在同一批任务上冲突）

批次 2 与批次 7 都把 10522 / 20522 / 15542 / 25542 / 30211 / 30213 / 30311 / 30313 排除在外，理由是它们被
`QE-046`（引擎外写入者）基线锁住：`src/test/resources/quest/external-reward-advance-baseline.tsv` 要求
`reward` 投影必须等于写入方留下的 packed step（当时为 0）。但 `QE-051` 要求 `reward` 投影等于客户端
`quest_summary` 的领奖行；这两条合同对这 8 个任务给出的值不同，**只有让写入方同时写领奖行才能同时成立**——
这与同族模板 15545 / 25545（`MinionService#checkQuest` 已 `setQuestVar(1)` + `setStatus(REWARD)`）一致。

| 组 | 任务 | 客户端行清单 | 引擎外写入方 | 领奖 NPC | 旧 reward 投影 |
| --- | --- | --- | --- | --- | --- |
| A（2） | 10522 / 20522 | 2 行：`quest_q10522.html`（和代理人 Weatha 对话）/ `quest_q20522.html`（和代理人 Feregran 对话，客户端字典写作 `LF6_Weatha_E`） | `CM_CREATIVITY_POINTS#checkQuestCompletion` | 806075 / 806079 | 0（且已有 `var0==1` 自愈边） |
| B（2） | 15542 / 25542 | 2 行：和 `LF6_Felen_E` / `DF6_Edorin_E` 对话 | `CoalescenceService#updateQuestsOnCoalescenceComplete` | 806074 / 806078 | 0（无自愈边） |
| C（4） | 30211 / 30213 / 30311 / 30313 | 2 行：和 `Pilomenes` / `Cainus` / `Herka` / `Hler` 对话（贝希蒙德神殿裂隙宝珠） | `RiftOrbAI2#forQuest`（一处代码覆盖 4 个任务） | 798941 / 798926 / 799322 / 799225 | 0（无自愈边） |

- 客户端行清单与末行 NPC 已由 `audit_reward_row_vs_client_steps.py` 逐任务核对（末行文本与任务内 NPC、
  入口页 NPC 一致）；8 个任务的行数都是 2，因此 `var0` 只有 0（进行行）与 1（领奖行）两个合法值。
- 语义依据：`QuestMutationPlanner#matchesSourceNode`（`src/main/java/com/aionemu/gameserver/questEngine/runtime/QuestMutationPlanner.java:340`）
  只比较 **source 节点投影里声明过的变量**，因此写入方只要把 `var0` 写到 1 就能匹配 `reward` 节点的领奖行投影；
  `QuestVars#setVarById(0, 1)` 即 packed `var0=1`（`var1..var5` 保持 0，不污染其它槽位）。

### 十二之二、修复

- **写入方（3 个文件、8 个任务）**：在每个任务的 `setStatus(QuestStatus.REWARD)` 之前插入
  `qs.setQuestVarById(0, 1);` 与中英双语 `QE-046/QE-051` 注释：
  `CM_CREATIVITY_POINTS#checkQuestCompletion`（10522、20522）、
  `CoalescenceService#updateQuestsOnCoalescenceComplete`（15542、25542）、
  `RiftOrbAI2#forQuest`（30211 / 30213 / 30311 / 30313，循环内插入一次）。
- **定义（8 个 XML）**：`reward` 节点投影 `0 → 1`；补（A 组为改写）无 source 的 `enter-world` 自愈边
  `status=REWARD && var0==0`（**无 actions**，仅 `LEVEL_AND_VISIBILITY_REFRESH`，步数由 `reward` 投影补足）。
  A 组原有的 `var0==1` 自愈边在写入方改为写 1 之后已无对象，改为 `var0==0` 并重写注释说明覆盖的是旧存档。
- 可重放脚本：`.agents/summary/quest-10522-reward-reentry/apply_batch8_external_writer_reward_row.py`
  （`--check` 只校验，默认应用；复跑输出 `BATCH8_VERIFY_OK quests=8`）。
- 基线重刷：`src/test/resources/quest/external-reward-advance-baseline.tsv` 由
  `.agents/summary/quest-10522-reward-reentry/audit_external_reward_advance.py --out ...` 重生成，
  10 个任务全部 `writer step=1 / projection=1 aligned / entry-page-ok / recovery=[0]`，
  `verify_external_reward_reentry_contract.py` 输出 `static contract verified for 10 quests`。
- 门禁更新：`Quest10522AutoStartDialogTest` 的 `reward` 投影断言 `var0 0 → 1`、自愈边断言 `var0==1 → ==0`
  （注释同步改写为“领奖行 = 与代理人维达对话”）。

### 十二之三、边界与已知偏差

- **只有引擎外写入者才需要这批改法**：写入方与定义必须同时改，只改 XML 会被 `ExternalRewardAdvanceReentryContractTest`
  的基线校验直接拒绝；反过来只改写入方会让客户端行清单与投影不一致（QE-051 的原始报障形态）。
- **15545 / 25545 未纳入**：它们走的是 `MinionService#checkQuest` 的 `setQuestVar(1)`（`var0..var5` 全 1），
  `var0` 同为 1，与领奖行一致且已有基线记录，本轮不改。
- **`setQuestVar(1)` 与 `setQuestVarById(0, 1)` 不等价**：前者把 6 个槽位都写成 1，后者只写 `var0`。
  本批次统一使用 `setQuestVarById(0, 1)`，避免把未声明的 `var1..var5` 写脏；若后续有任务在客户端脚本里
  使用 `SECTION_1` 以上槽位，必须先按 `check_section0_requirements.py` 核对该任务的槽位声明再决定写法。
- **自愈边只在登录/切图触发**：在线且不重登、不切图的旧存档（`REWARD/var0=0`）不会立即纠正；
  这是 `enter-world` 边的既有边界，与批次 1-7 相同。
- **这批只覆盖“2 行任务”**：客户端 3 行以上的引擎外写入者任务（若有）需要单独核对领奖行号，不能套用 1。
- 剩余 `MISSING_LAST_ROW` 从 129 降到 121，其中 61 个末行 NPC 对不上任务内 NPC、32 个末行是其它目标；
  仍按“客户端行清单 + 末行 NPC 命中 reward 路线 + 镜像/同型模板”三证据逐批收口，禁止机械推进。

### 十二之四、验证（2026-09-21，用户授权后执行）

```bash
mvn -B test -Dtest='ExternalRewardAdvanceReentryContractTest,Quest10522AutoStartDialogTest,Quest30313RetailAlignmentTest,Quest10520ClientDialogAlignmentTest,BroadcastZoneMissionEndDefinitionTest,ReportRowRewardProjectionContractTest,ArenaPhaseRowContractTest,SensoryAreaRideRowContractTest,JournalReportRowSplitContractTest,JournalRewardRowRepairContractTest,ClientQuestSectionAlignmentTest,ArchdaevaRewardRowContractTest,AlignedMirrorRewardRowContractTest,MirrorPairRewardRowContractTest,ProductionCatalogWhitelistVerificationTest,QuestDefinitionCatalogManifestTest,QuestCollectProgressAlignmentGateTest'
```

- 结果：`Tests run: 84, Failures: 0, Errors: 0, Skipped: 0`（17 个测试类）；`PRODUCTION_COMPILE_OK=6189`、
  `PRODUCTION_COMPILE_FAILURES=0`、`PRODUCTION_INTERACTION_OBJECT_FAILURES=0`、`PRODUCTION_WHITELIST_VIOLATIONS=0`。
  首轮（更新测试前）暴露且已修的两处断言：`reward` 投影 `0 → 1` 与自愈边 `var0==1 → ==0`。
- 静态：`xmllint --noout --schema src/main/resources/aion/data/static_data/quest_definition/quest_definition.xsd`
  8 个文件全部 `validates`；IDE lint 0 警告（Java 侧仅有既存的风格类 warning，与本次 hunk 无关）；
  `git diff --check` 干净；`apply_batch8_external_writer_reward_row.py --check` 幂等通过。
- 全库审计同工作树前后对照（先回放批 8 前的 8 个 XML 取基线，跑完审计再回放修复版本）：
  `ROW_ALIGNED 2591 → 2599`、`ROW_BEHIND 254 → 246`、`ROW_STATE_ALIGNED 2372 → 2380`、
  `ROW_WITHOUT_STATE 575 → 567`、`ALIGNED 2372 → 2380`、`MISSING_LAST_ROW 129 → 121`
  （`ROW_AHEAD 2591`、`STATE_OUT_OF_RANGE 2448`、`INTERIOR_GAP 267`、`MISSING_TAIL_ROWS 90`、
  `STATES_BEYOND_ROWS 2625`、`BOTH_MISALIGNED 177` 不变）；两轮审计输出逐行 diff 只有这 8 个任务变化，
  全部由 `ROW_BEHIND + MISSING_LAST_ROW + ROW_WITHOUT_STATE` 转 `ROW_ALIGNED + ALIGNED + ROW_STATE_ALIGNED`。
- 客户端复测路径（PENDING_CLIENT）：
  1. 10522 / 20522：在创造力面板分配或重置点数触发 `CM_CREATIVITY_POINTS` 后，任务书应切到第 2 行
     “和代理人 Weatha / Feregran 对话”并能打开奖励窗口；旧存档 `REWARD/var0=0` 登录后自愈为 1。
  2. 15542 / 25542：融合成功后任务书应切到第 2 行“和 LF6_Felen_E / DF6_Edorin_E 对话”。
  3. 30211 / 30213 / 30311 / 30313：贝希蒙德神殿裂隙宝珠触发完成后，任务书应切到第 2 行
     “和 Pilomenes / Cainus / Herka / Hler 对话”，点任务行能打开 `select_success(10002)` 与奖励窗口。

### 十二之五、批次 1-7 的隐藏回归收口（同一批任务，2026-09-21）

批次 1-7 给 375 个任务补了**无 source 的 `enter-world` 自愈边**，但当时只跑了 12-13 个测试类。
本批次接着做了一次定向回归扫描：先枚举 `HEAD` 提交里改过的 375 个任务，再挑出「引用了这些任务 id 且
用 `X.sourceNode().equals(...)` 过滤」的 43 个测试类，一次性跑完 —— 结果 **259 例里 36 例失败**，分三类：

| 类别 | 数量 | 代表 | 原因与处置 |
| --- | --- | --- | --- |
| 测试 helper 空指针 | 21 个类 | `Quest18602ClientDialogAlignmentTest`、`LegacyKillFlowRepairDefinitionTest`、`MigratedQuestRepairDefinitionTest`、`EarlyElyosQuestRegressionTest`、`CollectTurnInClientActionAlignmentBatchTest`、`Quest2634/2669/24026/14026/14051/1553…` | helper 用 `candidate.sourceNode().equals(source)` 过滤，新自愈边的 `sourceNode()` 为 null 直接 NPE；改为 `Objects.equals(candidate.sourceNode(), source)`（21 个文件、约 70 处） |
| 期望值过期 | 7 个任务 | 1553（3 行→领奖行 2）、1988/2988（4 行→3）、3082（4 行→3）、14026/24026（6 行→5）、14051（5 行→4）、15550/25550（3 行→2，且交接写 2）、24030（10 行→9） | 这些测试写的是批次前的旧 reward 投影；按客户端 `quest_summary` 行数更新断言并补 QE-051 双语注释（`mvn` 已复核全部转为客户端领奖行） |
| 客户端契约门禁新指纹 | 2 个任务 | `QuestClientContractGateTest` 报 `BUTTON_WITHOUT_ROUTE|28208/28209|reward|205321|31|10002|1009` | 见下 |

**28208 / 28209 的领奖人补全**：客户端 `quest_q28208.html` / `quest_q28209.html` 的 quest_summary 末行是
“和 **Anja** 对话”，而 `npcs_unpacked/client_npcs_npc.xml` 里 **Anja = npc 205321**（205320 = Inggness）。
批次 6 只给 205321 加了 reward 态入口页（`QUEST_SELECT → DEFAULT_SUCCESS`），却没有对应的
`SELECT_QUEST_REWARD(1009)` 完成路线 —— 玩家按任务书找到 Anja、点开 10002 页后按钮无效。本批次按迁移既有
的 205320 块补上 `<npc-complete npc-id="205321" …>`（`<preview actions="USE_OBJECT SELECT_QUEST_REWARD"/>`），
205320 的既有路线保留（列为领奖 NPC），并把客户端证据写进中英双语注释。

**顺带发现（非本批引入）**：`QuestMovieAndDialogLoopRegressionTest#quest15301And25301AcceptanceAdvancesToStarted`
在 `HEAD` 就已红 —— 任务 15301/25301 的三段流程在 98b34418a（2026-09-20，另一个任务的提交）把首段节点
`started` 改名为 `s0`，但该测试仍断言 `started`。本批次按新节点名对齐断言（接取事件
`QUEST_ACCEPT_1 → s0` + `SHOW_QUEST_PAGE QUEST_ACCEPT_1` 不变），并在注释里标注来源。

### 十二之六、验证（批次 8 + 回归收口，2026-09-21 用户授权后执行）

```bash
mvn -B test -Dtest='<43 个定向回归类>,QuestClientContractGateTest,Quest20522AutoStartDialogTest,Quest10522AutoStartDialogTest,
ExternalRewardAdvanceReentryContractTest,Quest30311RetailAlignmentTest,BroadcastZoneMissionEndDefinitionTest,
ReportRowRewardProjectionContractTest,JournalReportRowSplitContractTest,JournalRewardRowRepairContractTest,
ClientQuestSectionAlignmentTest,ArchdaevaRewardRowContractTest,AlignedMirrorRewardRowContractTest,
MirrorPairRewardRowContractTest,ProductionCatalogWhitelistVerificationTest,QuestDefinitionCatalogManifestTest,
QuestCollectProgressAlignmentGateTest,QuestDialogOrderAuditTest,QuestDefinitionDirectoryLoaderTest'
```

- 结果：`Tests run: 302, Failures: 0, Errors: 0, Skipped: 0`；`PRODUCTION_COMPILE_OK=6189`、
  `PRODUCTION_COMPILE_FAILURES=0`、`PRODUCTION_INTERACTION_OBJECT_FAILURES=0`、`PRODUCTION_WHITELIST_VIOLATIONS=0`。
- `QuestClientContractGateTest` 的两条新指纹（28208/28209）已归零；批次 8 的 10 个 XML（含 28208/28209）
  `xmllint --schema quest_definition.xsd` 全部 `validates`；IDE lint 无 error；`git diff --check` 干净
  （`audit-missing-last-row.tsv` 的 trailing tab 是该审计产物的既有格式，与批次 7 提交一致）。
- 全库行审计重跑与批次 8 收口前一致：`ROW_ALIGNED 2599`、`ROW_BEHIND 246`、`ROW_STATE_ALIGNED 2380`、
  `ROW_WITHOUT_STATE 567`、`ALIGNED 2380`、`MISSING_LAST_ROW 121`（28208/28209 只增加完成路线，
  不影响行/状态判定）。
- 客户端复测仍为 **PENDING_CLIENT**（含 10522 的新 `REWARD/var0=1` 合同与 28208/28209 的 Anja 领奖链）。

## 十三、批次 9：自闭合 started 节点行状态 + var0 标志位例外判定（2026-09-21）

### 十三之一、候选与证据

审计在“行 ↔ 状态”维度剩下三类未收口（`ROW_BEHIND` 246 / `ROW_WITHOUT_STATE` 567 / `INTERIOR_GAP` 266），
本批次先处理证据最硬的两小类：

| 组 | 任务 | 现象 | 证据 |
| --- | --- | --- | --- |
| A（1） | 28932 | `started` 节点是**自闭合**（`<node label="started" status="START"/>`，无任何 var 投影），审计判 `INTERIOR_GAP` + `ROW_WITHOUT_STATE`，`visible_state_var0` 只有 1 | 客户端 `quest_q28932.html` 2 行（消灭 Dreadgion 德拉克忍者 → 向 `DF6_Olivia_E` 报告）；天族镜像 **18932 已声明 `started` 的 var0=0** 且审计为 `ALIGNED`——单侧缺投影 |
| B（2） | 30203 / 30303 | 同样是自闭合 `started`，但客户端 3 行、`reward` 投影 var0=1、审计判 `INTERIOR_GAP` + `ROW_WITHOUT_STATE` | **不能按行号改**：`origin/history` 的 `_30203GroupHalttheCeremony` 把 var0..var3 当作 4 只守护者（216175/216177/216179/216181）的击杀标志位逐个 `setQuestVarById(n, 1)`、集齐后对 216263 置 `REWARD`（领奖态 = var0..3=1，与当前 `reward` 投影一致）；客户端 `quest_script_monster.csv` 同样是 `Progress(SECTION_0<1) … Progress(SECTION_3<1)` 四个标志位 + `Progress(266305)` |

### 十三之二、修复（仅 A 组）

- `28932.xml`：`started` 由自闭合改成显式节点，**只声明行号** `<var name="var0" value="0"/>`
  （不声明 var1 击杀计数），并写入中英双语 QE-051 注释说明“与天族镜像 18932 同形”。
- 只声明 var0 的原因：28932 的“满计数恢复路线”（`started` + `var1>=1` 的 QUEST_SELECT /
  SELECT_QUEST_REWARD 对话）依赖 `started` 匹配 `var0=0、var1=1` 的旧存档；若照着 `reward` 的投影把
  var1 也钉成 0，这些恢复路线会全部失配（`QuestMutationPlanner#matchesSourceNode` 要求声明变量全等）。
- B 组不改动：属“var0 是标志位而非行号”的已核实例外，已在审计脚本头部以
  `VAR0_FLAG_EXCEPTIONS = {30203, 30303}` 记录证据（不改判定，仅防止后续误修）。
- 新增门禁 `src/test/java/com/aionemu/gameserver/questEngine/definition/Quest28932RewardRowContractTest.java`
  （3 条合同）：`started` 只声明 var0=0（且与 18932 同形）、`reward` 投影领奖行 var0=1/var1=1、
  单次击杀路线（`kill-npc npc-ids=243953`）计划到 `REWARD` 且打包 var0=1/var1=1、
  `var0=0、var1=1` 的满计数旧存档在 806261/806260 两个 NPC 的 QUEST_SELECT 与
  SELECT_QUEST_REWARD 上都能被 planner 规划到领奖行（防止未来把 var1 钉进投影）。

### 十三之三、验证（2026-09-21，用户授权后执行）

- `mvn -B test -Dtest='Quest28932RewardRowContractTest,ArenaPhaseRowContractTest,QuestClientContractGateTest,QuestDefinitionCatalogManifestTest,ProductionCatalogWhitelistVerificationTest'`
  → `Tests run: 21, Failures: 0, Errors: 0`；`PRODUCTION_COMPILE_OK=6189`、`PRODUCTION_COMPILE_FAILURES=0`。
- `xmllint --noout --schema quest_definition.xsd` 28932 `validates`。
- 全库审计同工作树前后对照（先回放 HEAD 版 28932、跑完再回放修复版）：
  `ROW_STATE_ALIGNED 2380 → 2381`、`ROW_WITHOUT_STATE 567 → 566`、`ALIGNED 2380 → 2381`、
  `INTERIOR_GAP 267 → 266`（`ROW_ALIGNED 2599`、`ROW_BEHIND 246`、`MISSING_LAST_ROW 121`、
  `BOTH_MISALIGNED 177` 不变）；两轮审计输出逐行 diff 只有 28932 一行
  （`last_start_var0` 由 `None` 变为 `0`）。
- 客户端复测路径（PENDING_CLIENT）：接取 28932 后任务书应停在第 1 行（消灭德拉克忍者），
  击杀后切到第 2 行“和 Olivia 对话”并可在两名 NPC 处领奖；旧存档（击杀已记录但未进领奖）与
  NPC 对话仍应能进入领奖。

## 十四、批次 10：retail“单步任务”族 + 镜像单侧的领奖行收口（18 个任务，2026-09-22）

### 十四之一、候选与证据（retail 源定义 + 族内模板 + 镜像）

- **retail 源定义**：`origin/history:src/main/resources/aion/definitions/compact/quests/scripts/zz_retail_simple_quests.xml`
  里这批任务都是 `data_driven_quest` 的**单 step** 结构（start + 1 个交付/收集 step），
  与客户端 `quest_summary` 的 2 行一一对应。
- **族内模板**：同族 249 个 retail 单步任务中 **184 个**已经是 `reward var0=1`（末行）且形状统一
  （`unaccepted/started(0) + reward(1) + complete(0)` + 无 source 的 `enter-world` 自愈边，样例
  1526、21458、11455）；本批 18 个的 `reward var0` 停在 `0`，其中
  - **A 组（12 个，末行完全没有 START/REWARD 状态，审计 `MISSING_LAST_ROW`）**：
    1527、1528、1725、2135、2247、2266、3087、4020、21455、26838、80735、80736；
  - **B 组（5 个，末行已有 `s1`/`k1`（var0=1）中间态，但领奖态仍显示上一行）**：
    1963、1964、16838、16977、18035；
  - **C 组（1 个，对侧镜像已对齐的单侧缺陷）**：29002 —— 镜像 19002 的 `reward` 投影已是 1，
    且旧 handler `origin/history:.../crafting/_29002ExpertAethertappersTest` 在 NPC 204099 的
    `STEP_TO_1` 里 `qs.setQuestVarById(0, 1)`、在 204257 领奖时只 `setStatus(REWARD)`（var0 保持 1）。
- **客户端行清单**（`quest_q<id>.html` 的 `quest_summary`，2 行）：末行都是领奖/交付行
  （交给/送给/报告/再次对话），末行 NPC 与任务定义内的领奖 NPC 一致（唯一例外 21455，见十四之四）。
  逐任务证据见 `batch10-evidence.tsv`（client_rows=2、领奖行 1、旧投影 0、领奖 NPC id/名、末行文案）。

### 十四之二、修复

- 18 个 XML 的 `reward` 节点投影 `var0`：`0 → 1`（= 客户端领奖行）；
- 每个任务补一条无 source 的 `enter-world` 自愈边：
  `status-is REWARD && variable-is var0==0 → set-variable var0=1`，
  after-commit 仅 `sync-quest-state mode=LEVEL_AND_VISIBILITY_REFRESH`（与族内模板同形）；
  不加这条边时旧存档（`REWARD/var0=0`）会因 `QuestMutationPlanner#matchesSourceNode`
  的“投影变量必须全等”语义匹配不到任何领奖路由。
- 可重放脚本 `apply_batch10_retail_single_step_rows.py`（`--check` 幂等）与证据表 `batch10-evidence.tsv`。
- 审计脚本新增证据常量 `DUPLICATE_VISIBLE_SLOT_BLANK_ROWS = {10530}`（只登记证据、不改判定）。

### 十四之三、验证（2026-09-22，用户授权后执行）

- `xmllint --noout --schema quest_definition.xsd`：18/18 `validates`。
- **聚焦 Maven**：主工作树当时被并行任务的 `PlayerCommonData.java` / `CM_HOTSPOT_TELEPORT.java`
  留在编辑中的语法错误态（mtime 23:59 / 00:01，非本任务文件），按项目规则改用**临时 worktree**
  验证（`git worktree add --detach /tmp/aionemu-verify-batch10b HEAD`，拷入本批 18 个 XML + 新测试），
  验证后已 `git worktree remove --force` + `git worktree prune`：
  `mvn -B test -Dtest='RetailSingleStepRewardRowContractTest,MirrorPairRewardRowContractTest,JournalRewardRowRepairContractTest,QuestClientContractGateTest,QuestDefinitionCatalogManifestTest,ProductionCatalogWhitelistVerificationTest,QuestDialog31RegressionTest,QuestMinionTutorialRetailAlignmentTest,ItemCollectingDialogProtocolAlignmentTest,QuestHandoverContinuationAuditTest,QuestRepeatLifecycleTest'`
  → **Tests run: 38, Failures: 0, Errors: 0**；`PRODUCTION_COMPILE_OK=6189`、`PRODUCTION_COMPILE_FAILURES=0`、
  `PRODUCTION_INTERACTION_OBJECT_FAILURES=0`、`PRODUCTION_WHITELIST_VIOLATIONS=0`。
- **全库审计同工作树前后对照**（先跑 HEAD 版得基线，再跑修复版）：
  `ROW_ALIGNED 2599 → 2617`、`ROW_BEHIND 246 → 228`、`ROW_STATE_ALIGNED 2381 → 2394`、
  `ROW_WITHOUT_STATE 566 → 553`、`ALIGNED 2381 → 2394`、`MISSING_LAST_ROW 121 → 108`
  （`ROW_AHEAD 2591`、`STATE_OUT_OF_RANGE 2448`、`INTERIOR_GAP 266`、`MISSING_TAIL_ROWS 90`、
  `STATES_BEYOND_ROWS 2625`、`BOTH_MISALIGNED 177`、`NO_REWARD_ROW 178`、`NO_CLIENT_HTML 608/650` 均不变）；
  `audit-output.tsv` 逐行 diff 只有这 18 个任务，`audit-missing-last-row.tsv` 减 13 行、
  `audit-qe051-candidates.tsv` 减 7 行。
- 新增门禁 `RetailSingleStepRewardRowContractTest`（4 条合同）：reward 投影 = 领奖行且 `started` 保持行 0、
  同族已对齐参照（1526/21458/11455）与 C 组镜像 19002 同值、自愈边的条件/动作/after-commit/priority
  与 planner 计划结果（`REWARD/var0=0 → 1`）、任何进入 `reward` 的路线都不得写回旧行号 0。
- 客户端实机复测：**PENDING_CLIENT**（本批 18 个任务的领奖态应显示末行文案）。

### 十四之四、边界与后续（下一批前必读）

- **21455 的领奖 NPC 归属待核实**：retail `end_npc_ids=799244`（server `name_desc=Unset`，与客户端
  `STR_DIC_N_Unset` 一致），typed 定义把领奖/completion 放在起始 NPC 799404 上；本批只收口行投影，
  NPC 归属归入“领奖 NPC 待核实”族（30614、26838、19064/29064 同族）。
- **25608 需补中间行**：retail 7 step ↔ 客户端 7 行，定义缺“前往 SZ_I_Queen”（ENTER_AREA 206542）
  这一行的状态，需要新增 `step5` 并把 reward 推到 6 —— 归入下一批“多阶段/内部缺口”范围。
- **10530 是审计误报（已登记）**：客户端第 8 行是空 `<p visible="[%24]"></font></p>`，第 9 行
  “倾听 Jucleas 的故事”同样是 `[%24]`（镜像 20530 没有这个空行），`client_rows` 比真实状态多 1；
  批次 4 已按镜像把 reward 对齐到 9，本批把证据写进审计脚本 `DUPLICATE_VISIBLE_SLOT_BLANK_ROWS`。
- **“名人考试”族不得按行号加一**：19008/19014/19020/19026/19032（镜像 29014/29020/29026/29032）
  的 `reward=1` 属 legacy 语义 —— `_19008MasterWeaponsmithsPotential` 在收材料时 `setQuestVarById(0,1)`、
  领奖只置状态；`_19057MasterConstructorsPotential` 另有“缺材料/图纸”的 `setQuestVar(2)` 失败分支
  （19057/29057 因此同时有 var0=1 与 var0=2 两个状态），所以“末行行号 2”不是这批的领奖行。
- **其余 `MISSING_LAST_ROW` 108 个**（本批前 121 − 13）中：镜像两侧同缺 32、QE-045 锁 10、
  末行是“其它目标”（非对话/报告）26，其余需逐族 legacy/retail 证据，禁止按行号机械推进。
- memory-bank 同批更新：`patterns/quest-engine.md` 的 QE-051（fix_or_guardrail 第 9/10 条、boundaries、
  validation、keywords）与 `activeContext.md`（批次 10 完成 + 批次 10 边界）；
  `sync_memory_bank.py` + `verify_memory_bank.py` 通过（并顺手把并行提交顶到 407 字符的 QE-051
  keywords 行裁回 255 字符，修复了结构校验失败）。

## 十五、批次 11：25608 七步真端行对齐 + 两个 ENTER_AREA 触发器（2026-09-22）

### 十五之一、候选与证据（客户端行槽位 + retail 七步 + 客户端 level 坐标）

- **客户端行清单**：`data_unpacked/Dialogs/20000_29999/quest_q25608.html` 的 `quest_summary`
  有 8 个 `<p>`，其中第 3、4 行共用 `visible="[%9]"`（“萨波拉开花了…” + “消灭库库勒工人 (x/10)”），
  去重后正好 7 个可见槽位 `0/3/6/9/12/15/18`，即行 0..6：
  `和 DF6_Mondhes_E 对话 → 向 DF6_Mumu01_E 询问 → 前往 DF6_SZ_I → 消灭库库勒工人 x/10 →
  和 DF6_Mumu01_E 对话 → 前往 DF6_SZ_I_Queen → 从 Named 怪获得 quest_25608a 交给 DF6_op_goods_Seller`。
- **retail 源定义**：`git show 9c0d44eb0:src/main/resources/aion/definitions/compact/quests/scripts/zz_retail_simple_quests.xml`
  中 `data_driven_quest id="25608"` 恰为 7 步：`TALK 806177 → TALK 806197 → ENTER_AREA 206534 →
  HUNT 241235 x10 → TALK 806197 → ENTER_AREA 206542 → COLLECT_ITEM 805964`。
- **客户端任务数据**：`Quest_unpacked/quest.xml` 的 25608 节点声明 `collect_progress=6`，
  与末行（COLLECT_ITEM / 领奖行 6）一致。
- **旧定义错位**：旧迁移只有 5 个进度状态（`step1..step4` + `reward`）：把 HUNT 放在 var0=2、
  把 Mumu 对话放在 var0=3、把交付行放在 var0=4、reward 停在 var0=5 —— 行 2（前往 SZ_I）与
  行 5（前往 SZ_I_Queen）没有任何 START/REWARD 状态，且所有后续状态都比客户端行号小 1。
- **历史缺口**：`zones_quest.xml` 从未注册 25608 的两个 sensory zone；`a2306c8e2` 的提交说明明确记录
  “25608 (no spawn coordinates anywhere in history)”并因此把它留在 `METADATA_ONLY`。本次从本机
  Aion 5.8 客户端 `Levels/DF6/Level.pak` 解包 `mission_mission0.xml`，取到两个触发点：
  `DF6_SensoryArea_Q25608a_Dynamic_Env @ (1540.3777, 556.31085, 310)` 与
  `DF6_SensoryArea_Q25608b_Named @ (1513.4391, 544.90009, 295.16571)`。

### 十五之二、修复

- `quests/25608.xml`：
  - 节点重排为 `started(0) → step1(1) → step2(2, ENTER_AREA 206534) → step3(3, HUNT) →
    step4(4, Mumu SELECT5) → step5(5, ENTER_AREA 206542) → step6(6, COLLECT_ITEM) → reward(6)`；
    `step6` 与 `reward` 投影同为 var0=6，但 status 分别为 `START`/`REWARD`（交付行在任务书里
    即领奖行，符合 QE-051）。
  - HUNT 自环与收尾路线改为 `step3 → step3`（var1<9，自增）与 `step3 → step4`
    （var1>=9，var0=4 且 var1=0）。
  - 新增两条 `enter-zone` 转换：`step2→step3`（A 区）、`step5→step6`（B 区），条件为对应
    var0 投影，after-commit 仅 `PACKET_ONLY`。
  - 旧领奖态自愈：无 source 的 `enter-world`（REWARD + var0<6 → var0=6 +
    `LEVEL_AND_VISIBILITY_REFRESH`）与无 source 的 `QUEST_SELECT`（同样条件 → var0=6 +
    `DEFAULT_SUCCESS`），修复旧存档 `REWARD/var0=5` 匹配不到任何领奖路由的问题。
  - `drop collecting-step` 由 0 改为 6，与客户端 `collect_progress=6` 一致。
- `zones_quest.xml`：在 220110000 段新增 `DF6_SENSORY_AREA_Q25608_A_DYNAMIC_ENV_220110000`
  与 `DF6_SENSORY_AREA_Q25608_B_DYNAMIC_ENV_220110000` 两个 SPHERE/SUB zone，坐标取客户端
  level 触发点、半径 10（与同族 sensory zone 一致）。
- 新增门禁 `Quest25608RetailSevenStepAlignmentTest`（6 条合同）：七步行覆盖、两个 enter-zone
  事件与 zones_quest.xml 注册/坐标、HUNT 自环与收尾、交付行归属 reward、两条旧存档自愈边、
  drop collecting-step 与客户端 collect_progress 一致。

### 十五之三、验证（2026-09-22，用户授权后执行）

- `xmllint --noout --schema quest_definition.xsd` → 25608 `validates`；
  `xmllint --noout --schema zones.xsd zones_quest.xml` → `validates`。
- **主工作树被并行任务的 13 个 XML 阻塞**：`QuestDefinitionCatalogManifestTest` 报
  `AMBIGUOUS_TRANSITION: TALK_TO_NPC`，逐文件编译定位为并行任务未提交改动
  `1183/1319/1483/1514/1721/1724/2449/2646/2692/2767/3966/3968/4501`（均不在本批范围；
  本批 25608 单独编译通过）。按项目规则改用**临时 worktree**（`git worktree add --detach
  /tmp/aion-q25608-verify-20260922 HEAD`，HEAD=`79775ca10`，拷入本批 25608.xml + zones_quest.xml +
  新测试），验证后已 `git worktree remove --force` + `git worktree prune`：
  `mvn -Dtest='Quest25608RetailSevenStepAlignmentTest,QuestDefinitionCatalogManifestTest,ProductionCatalogWhitelistVerificationTest,QuestInteractionObjectCatalogTest,Quest30721And30771RetailFlowTest,QuestClientContractGateTest' test`
  → **Tests run: 28, Failures: 0, Errors: 0**；`PRODUCTION_COMPILE_OK=6189`、
  `PRODUCTION_COMPILE_FAILURES=0`、`PRODUCTION_INTERACTION_OBJECT_FAILURES=0`、
  `PRODUCTION_WHITELIST_VIOLATIONS=0`。worktree 中 `QuestDefinitionCatalogManifestTest` 10 例全绿，
  证明主工作树的 6 个 error 全部来自并行任务改动。
- **单任务审计回放**：25608 由 `ROW_BEHIND / MISSING_LAST_ROW`（reward_var0=5、visible 0..5、
  rows_without_state=6）变为 `ROW_ALIGNED / ALIGNED`（reward_var0=6、visible 0..6、
  rows_without_state 空）；`audit-output.tsv` 中该行是本批唯一的单任务 diff。
- **全库快照**（`audit_reward_row_vs_client_steps.py`）：当前 `ROW_ALIGNED 2628`、`ROW_BEHIND 217`、
  `ROW_STATE_ALIGNED 2405`、`ROW_WITHOUT_STATE 542`、`ALIGNED 2405`、`MISSING_LAST_ROW 102`；
  该快照同时包含并行任务已提交的修复，不能只用差值归因本批次。
- 客户端实机复测：**PENDING_CLIENT**。路径：接取 25608 后任务书应依次高亮 7 行；与 Mumu 对话后
  进入 A 区自动切到“消灭库库勒工人 (x/10)”，10 只后与 Mumu 对话进入 B 区，再击杀 Named 怪获得
  `quest_25608a` 交付 Bindeil（805964），领奖态任务书应停在末行且 `DEFAULT_SUCCESS` 可打开奖励窗口。

### 十五之四、边界

- **旧 START 存档不做自愈**：旧 `START/var0=2/3/4` 在新链里的语义分别变为“前往 A 区/HUNT/和 Mumu 对话”，
  条件无法与新存档区分，因此只能由玩家重做对应步骤；只有旧领奖态 `REWARD/var0=5` 有无歧义自愈边。
- **zone 名按同族命名规则推断**：A/B 后缀与 `DYNAMIC_ENV` 采用 15608/25601 的 `zones_quest.xml`
  既有规则；坐标来自客户端 level 触发点（同族坐标同样来自客户端/零售位置），仍需实机确认
  enter-zone 能触发。

## 十六、批次 12：领奖 NPC 归属核实族 + 30614 报告行（4 个任务，2026-09-22）

### 十六之一、候选与证据（归属口径 = 客户端 quest_summary 行内 NPC）

候选来自审计的「末行 NPC 与定义领奖 NPC 不一致」族：`19064`、`29064`、`21455`、`30614`
（`26838` 的 806574/806575 仍待单独取证，本批不动）。判定口径固定为：
**客户端 `quest_summary` 末行写明的 NPC = 实现侧的领奖/completion NPC；起始 NPC 不得兼任领奖。**

| 任务 | 客户端行 | 行内 NPC（客户端解包 id） | 修复前领奖/completion | 修复后 |
|---|---|---|---|---|
| 19064 | 行 0 Undin、行 1 徽章+圣物交 Jucleas | Jucleas = 203752 | 203701（起始 NPC Lavirintos 兼任） | 203752 |
| 29064 | 行 0 Darfen、行 1 徽章+圣物交 Balder | Balder = 204075 | 204053（起始 NPC Kvasir 兼任） | 204075 |
| 21455 | 行 0 交给 Schiemann、行 1 解毒剂交 Unset | Unset = 799244 | 799404（起始 NPC Miener 兼任） | 799244 |
| 30614 | 行 0 战斗 6/15、行 1 向 Astella 报告 | Astella = 800327 | 800326（Aluna） | 800327 |

补充证据：

- 19064 / 29064：legacy `_19064Templar_Of_Construction`、`_29064Fang_Of_Construction` 注册
  `{起点, 中间, 领奖}` 三个 NPC，中间 NPC 的 `STEP_TO_1` 写 `var0=1`（旧 typed 投影因此在领奖前
  停在行 0），领奖 NPC 用 `checkQuestItemsSimple(1,2,true,5)` 进 REWARD；retail 侧
  `end_npc_ids=203752/204075` 与客户端行 1 一致。
- 21455：legacy `_21455Ingredients_For_The_Antidote` 的 `npc_ids={799404,799240,799244}` 中，
  799240 走 `STEP_TO_1` 换物品、799244 进 REWARD；retail `end_npc_ids=799244`；accept 仍是
  799404（客户端 `quest_complete`「米埃奈尔说果实经过一段时间之后效果就会消失」）。
- 30614：见十六之三（首轮判断被修订）。

### 十六之二、修复（首轮）

- **19064 / 29064**：新增 `s1(START,var0=1)`；行 0 路由改到 798450/798452（`QUEST_SELECT` → SELECT2、
  `SETPRO1` 写 `var0=1`）；行 1 与领奖/completion 全部改到 203752/204075（`QUEST_SELECT` → SELECT5、
  `CHECK_USER_HAS_QUEST_ITEM_SIMPLE` 校验并回收徽章+圣物后进 REWARD）；`reward` 投影 0→1；
  旧存档 `reward/var0=0`（旧 typed 投影）与 `var0=2`（旧 handler 的 `var0+1` 语义）各补一条
  enter-world 自愈边。
- **21455**：领奖/completion 由 799404 改到 799244；删除客户端没有按钮的 `SETPRO2` 死路由，把
  换物品（182209514 → 182209515）并入客户端 `select2_1` 的 `SETPRO1`；删除
  `started + 799404 QUEST_SELECT -> SELECT5` 死路由，改由 `reward + 799244 QUEST_SELECT`
  打开奖励窗口。
- **30614**：`reward` 投影 0→1；补 `REWARD/var0=0` 的 enter-world 自愈边；补领奖态入口页
  `reward + QUEST_SELECT(31) -> DEFAULT_SUCCESS(10002)`，并保证入口页位于 `npc-complete` **之后**
  （首版脚本曾把它插进 `npc-complete` 内部，导致 XSD 报错，已修并加防重入断言）。

### 十六之三、修订：30614 的领奖 NPC 由 800326 回滚为 800327（Astella）

首轮把 30614 的领奖 NPC 留在 Aluna(800326)，理由是「retail `start_npc_ids=800326` 与 legacy 模板
双源确认」。复核发现**这两个来源是同一个文件**（`docs/quest/client-dialog-mapping/legacy-quest-dialog-*.csv`
的 `source_resource` 都是 3.0/4.5 期手写脚本 `terath_dredgion.xml`），属单源重复计数；支持
Astella(800327) 的证据链反而更强：

1. 客户端 `quest_summary` 行 1 直接写「向 Astella 报告」（`STR_DIC_N_Astella`）。
2. 同文件 `quest_complete`：「阿斯泰拉说必须对萨德哈德雷得奇安进行持续的进攻，让你继续渗入到那里
   进行战斗」——同族中该句主语与接取 NPC 一致。
3. 同族命名规则 4/4 成立：30611/30612 行内是 Aluna(800326)、实现 NPC 也是 800326；30610/30613
   行内是 Astella(800327)、实现 NPC 也是 800327。
4. 2026-08-06「全量 6021 任务客户端/真端交叉审计修正」(`f737cfef1`) 原本就把该任务改成 800327。
5. 反向证据只有 `terath_dredgion.xml` 单文件，且它对该族同样不完整（30610 的中间 Aluna 对话步骤
   在该文件里不存在）；`10a2e7e57`（2026-09-13「批量对齐 69 个任务 report/completion NPC」）仅依据
   它把 800327 改成 800326，并把该次改动登记进 `.agents/summary/quest-report-npc-mismatch/report-npc-mismatch.csv`
   第 64 行（`30614,800327,800326,...`）。本次修订即回滚该次回归，并把 NPC_START / NPC_REPORT /
   npc-complete / 领奖态入口页统一到 800327。

### 十六之四、验证（2026-09-22）

- **XSD**：`xmllint --noout --schema quest_definition.xsd` 对 19064 / 29064 / 21455 / 30614
  4/4 `validates`。
- **可重放脚本**：`.agents/summary/quest-10527-reward-row/apply_batch12_reward_npc_ownership.py`
  `--check` 4/4 `BATCH12_CHECK_OK`（幂等；含入口页误插搬出、重复入口页去重、旧修订注释同步）。
- **单任务审计**（`audit_reward_row_vs_client_steps.py`，同一工作树前后对照）：

| 任务 | 修复前 | 修复后 |
|---|---|---|
| 19064 | `ROW_BEHIND / MISSING_LAST_ROW / ROW_WITHOUT_STATE`，reward_var0=0，`last_row_npc_matches_quest=False` | `ROW_ALIGNED / ALIGNED / ROW_STATE_ALIGNED`，reward_var0=1，`True` |
| 29064 | 同上 | 同上 |
| 21455 | `ROW_ALIGNED / ALIGNED`，但 `last_row_npc_matches_quest=False`（领奖在起始 NPC 上） | `ROW_STATE_ALIGNED`，`True` |
| 30614 | `ROW_BEHIND / MISSING_LAST_ROW`，reward_var0=0，`last_row_npc_matches_quest=False`（Aluna 800326） | `ROW_ALIGNED / ALIGNED / ROW_STATE_ALIGNED`，reward_var0=1，`True` |

  全库快照（同一次刷新）：`ROW_ALIGNED 2633`、`ROW_BEHIND 211`、`NO_REWARD_ROW 179`、
  `ROW_AHEAD 2591`、`NO_CLIENT_HTML 608`；`last_row_npc_matches_quest=False` 由 523 降到 519，
  正好是本批 4 个任务。快照同时包含并行任务（1430/1643/2513/2962/4542 族）的改动，不能只用差值
  归因本批次。
- **门禁测试**：`src/test/java/com/aionemu/gameserver/questEngine/definition/RewardNpcOwnershipContractTest.java`
  6 例——① 领奖/completion owner 必须是客户端行内 NPC 且不再落在起始 NPC；② 各任务 talk 路线 NPC
  集合精确相等（防止再出现向 Aluna 汇报的残留路由）；③ Terath Dredgion 族命名规则
  （30610/30611/30612/30613 参照 + 30614 只允许 800327）；④ 旧存档自愈边（0 → 1、2 → 1）与
  planner 计划结果；⑤ 30614 领奖态入口页必须唯一且位于 `npc-complete` 之后；⑥ 21455 不再保留
  `SETPRO2` 与 799404 的领奖路由。
- **Maven**：PENDING_MAVEN（未获授权，未执行）。拟在同一口径的临时 worktree 上运行
  `mvn -Dtest='RewardNpcOwnershipContractTest,RetailSingleStepRewardRowContractTest,QuestClientContractGateTest,QuestDefinitionCatalogManifestTest,ProductionCatalogWhitelistVerificationTest' test`。
- 客户端实机复测：**PENDING_CLIENT**。复测要点：① 19064/29064 行 0 与 Undin/Darfen 对话后任务书
  切到行 1，行 1 与 Jucleas/Balder 对话可开奖励窗口并完成；② 21455 与 Schiemann 对话换到解毒剂、
  与 Unset 对话领奖；③ 30614 领奖态在 Astella(800327) 处显示“?”并可打开 `DEFAULT_SUCCESS`
  与奖励窗口。

### 十六之五、边界与后续

- **归属验收口径**：客户端行内命名 NPC 优先于 legacy/retail 单文件登记；两者冲突时必须给出客户端
  命名规则或族内一致性证据（本批 30614 即按该口径回滚）。`report-npc-mismatch.csv` 第 64 行是
  2026-09-13 批次的历史登记，本批不改写它，但该行结论已被本次修订取代。
- **`last_row_npc_matches_quest` 不是缺陷总数**：全库仍有 519 个 False，多数属于（a）末行是中间
  对话对象（如 30610 行 0 的 Aluna、收集类行的交付 NPC 与定义注册 NPC 不同）或（b）定义确实未注册
  该 NPC；只能用作族内一致性定位，逐族收口时仍要回到客户端行 + 真端/legacy 三方证据。
- **剩余 MISSING_LAST_ROW 99 个**（75 DIALOG + 24 OTHER，均 `ROW_WITHOUT_STATE`）继续逐族收口；
  另有 `ROW_BEHIND 211`（多为 `MISSING_TAIL_ROWS`）与 `STATES_BEYOND_ROWS` 族，禁止按行号机械推进。
- **旧存档**：19064/29064 的 `reward/var0=2` 是旧 handler `var0+1` 语义留下的中间值，本批按自愈边
  归一到 1；`START` 态旧存档不做迁移（会由行 0 路由重新推进）。
- **26838 仍待取证，本批不改**：客户端两行都写 `IDEternity_03_Jarik01_E`（客户端解包 id = 806574），
  而 legacy `_26838Some_Sorcerer_Records` 与 typed 定义都用 806575（`IDEternity_03_Jarik02_E`）；
  天族镜像 16838 呈同一形态（客户端行内 `Ostia01_E` vs 定义/legacy 806566 = `Ostia02_E`），
  说明这是「01/02 变体」的族级分歧而不是单任务接错。可用证据目前为空：806565/806566/806574/806575
  在 `spawns/**` 都没有静态 spawn（该 NPC 由任务/事件动态生成），`quest.xml` 与真端 `NPCS.xml`
  都不含 NPC→任务绑定。下一批若要收口，需先用「真端 NPC 绑定或实机观察哪个 NPC 显示 ? 并打开
  reward 窗口」二选一取证，禁止仅凭客户端行内名字直接改 id（与 30614 不同：30614 还有
  `f737cfef1` 真端交叉审计与同族 4/4 命名规则做旁证）。

---

## 十七、批次 13：“两行、末行是与领奖 NPC 的对话”族（6 个任务，2026-09-22）

### 十七之一、候选与证据

族定义：客户端 `quest_summary` **恰好 2 行** —— 行 0 是“前往/交付/进入/接取对话”，行 1 是
“（再次）与 X 对话 / 向 X 报告”，并且**行 1 点名的 NPC 就是定义里的领奖/完成 NPC**。定义原先把
`reward` 投影停在 0，于是 REWARD 态在任务书里仍然渲染已完成的行 0，行 1 永远拿不到 START/REWARD
状态（审计 `MISSING_LAST_ROW` + `ROW_WITHOUT_STATE`）。

| 任务 | 行 0 | 行 1（领奖/完成 NPC） | legacy / 定义证据 |
|---|---|---|---|
| 1926 | 去找 Lavirintos 获取推荐信 | 和 Latri 对话（203894） | `_1926Secret_Library_Access`（`7e9f0316c^`）203894 处 `giveQuestItem(182206022)` + `setStatus(REWARD)`，REWARD 态在 203894 开 `10002 -> 5`；`npc-complete` owner = 203894 |
| 2938 | 去找 Sueron 获取推荐信 | 和 Izwin 对话（204267） | `_2938Secret_Library_Access` 同 1926 结构；`npc-complete` owner = 204267 |
| 39003 | 和 DF2a_Ionia_E_LHW（800512）对话 | 和 DF2a_Nevma_G_LHM（800504）对话 | 定义 `NPC_START=800500`、`NPC_REPORT=800504(page SELECT5)`、`npc-complete` owner = 800504；客户端行 0 的 800512 只发 `SETPRO1` |
| 49003 | 和 Noorn（800511）对话 | 和 dromik（800505）对话 | 定义 `NPC_REPORT=800505(page SELECT5)`、`npc-complete` owner = 800505；客户端行 1 的 `STR_DIC_N_dromik` ↔ 客户端表 `800505 = LF2a_dromik_G_DHM`（带前缀，故 `last_row_npc_matches_quest=False`，非缺陷） |
| 80989 | 进入 IDRun 战场见咕咕咻 | 回到大城市再次和咕咕咻对话 | 客户端表 `836196 = IDRUN_Entrance_guide`；定义 `SET_SUCCEED/DEFAULT_SUCCESS` 与 `npc-complete` 都在 836196 |
| 80990 | 同 80989（另一阵营事件变体） | 同 80989 | 定义 NPC 与领奖 owner 同为 836196 |

**QE-045 边界打回（本批最重要的判断）**：同形的 `13965/23965`（enter-zone 置 REWARD）与
`15674/25674`（`CHECK_COLLECTED_ITEMS` 置 REWARD）**不属于本批**。它们虽然是同样的 2 行结构（行 1
= 与 835217/835220/806114/806116 对话），但 `reward` 投影 0 与 `REWARD && var0==1` 恢复边是
commit `f6aff952a`“保留 legacy packed step”（2026-09-09，共 22 个任务）的基线，并被
`LegacyRewardStepProjectionRegressionTest` 的 20 例硬锁（改投影会同时打破该测试的
`reward var0 == 0`、`recovery route == 1`、`recovery.actions() == List.of()` 三条断言）。按 QE-045
boundary，投影重定基线必须先有客户端验收证据，因此**本批只登记证据、不改这 4 个任务**，与批次 2
“排除 QE-045 锁任务”的口径一致。

（同批脚本另登记 50008/51008 为不可机械套用的边界：legacy 用 `setQuestVarById(0, var0 + 1)` 把
var0 当 0→2 的投递计数，且客户端行内 `HousingLf_Event_ShugoSanta` / `E_HousingDF_Event_ShugaShugo`
在 5.8 客户端 NPC 表中不存在，归属无法核对。）

### 十七之二、修复

统一模板（与批次 10 的 retail 单步族同形）：

1. `<node label="reward" status="REWARD">` 的 `var0` 投影 `0 -> 1`（领奖行 = 客户端行 1）；
2. 补一条**无 source** 的 `enter-world` 自愈边 `REWARD && var0 == 0 -> set-variable var0 = 1`
   （`LEVEL_AND_VISIBILITY_REFRESH`，无 priority），把旧存档的旧投影纠正为 1；否则
   `QuestMutationPlanner#matchesSourceNode` 的“投影变量必须全等”语义会让这些存档匹配不到任何领奖路由；
3. 1926/2938 迁移期已有的 `REWARD && var0 == 1` 入口边**保留**（与自愈边条件互斥，不会产生
   `AMBIGUOUS_TRANSITION`；39003/49003/80989/80990 原本没有该边，本批不新增）。

脚本：`.agents/summary/quest-10527-reward-row/apply_batch13_two_row_talk_rows.py`
（`--check` 幂等，6/6 `BATCH13_CHECK_OK`；`QE045_DEFERRED` 常量登记 4 个锁任务）。
逐任务证据：`.agents/summary/quest-10527-reward-row/batch13-evidence.tsv`。

### 十七之三、验证（2026-09-22，用户授权后执行）

- **单任务审计**（`audit_reward_row_vs_client_steps.py 1926 2938 39003 49003 80989 80990`）：
  6/6 `ROW_BEHIND / MISSING_LAST_ROW / ROW_WITHOUT_STATE`（`visible_state_var0=0`）
  → `ROW_ALIGNED / ALIGNED / ROW_STATE_ALIGNED`（`visible_state_var0=0 1`）；39003/49003/80989/80990
  的 `recovery` 由 `False` 转 `True`。
- **全库快照**（同一次刷新）：`ROW_ALIGNED 2633 -> 2639`、`ROW_BEHIND 211 -> 205`、
  `MISSING_LAST_ROW 99 -> 93`、`ROW_AHEAD 2591`、`NO_REWARD_ROW 179`、`NO_CLIENT_HTML 608`；
  形状 `ALIGNED` +6。快照同样包含并行任务（1430/1643/2513/2962/4542/10525 等族）的改动，
  不能只用差值归因本批次；4 个 QE-045 锁任务在这一步保持 `ROW_BEHIND / MISSING_LAST_ROW` 不变。
- **结构校验**：`xmllint --noout --schema quest_definition.xsd` 6/6 `validates`；IDEA lint 0 problems；
  `git diff --check` 干净。
- **门禁测试**：`src/test/java/com/aionemu/gameserver/questEngine/definition/RewardRowTwoRowTalkFamilyContractTest.java`
  6 例——① `reward` 投影 = 1 且 `started` 保持 0；② 领奖 owner 必须等于客户端行 1 的 NPC；
  ③ 自愈边唯一（`var0==0 -> 1`，无 priority）且 `QuestMutationPlanner` 对 `REWARD/var0=0` 计划出
  `var0=1`；④ 迁移期 `REWARD/var0==1` 入口边按任务保留/缺失（1926/2938 保留）；⑤ 任何 target=reward
  的事务都不许写非领奖行的 `var0`；⑥ QE-045 锁的 4 个同形姊妹任务必须保持 `reward var0=0`
  （把“本批故意不收口”写成可执行边界）。
- **Maven**：PENDING_MAVEN（未获授权，未执行）。拟在同一口径的临时 worktree 上运行
  `mvn -Dtest='RewardRowTwoRowTalkFamilyContractTest,RewardNpcOwnershipContractTest,RetailSingleStepRewardRowContractTest,LegacyRewardStepProjectionRegressionTest,QuestClientContractGateTest,QuestDefinitionCatalogManifestTest,ProductionCatalogWhitelistVerificationTest' test`。
- 客户端实机复测：**PENDING_CLIENT**。复测要点：① 1926/2938 与 Lavirintos/Sueron 交付后任务书切到
  行 1，与 Latri/Izwin 对话开奖励窗口；② 39003/49003 与行 0 NPC 对话推进后，行 1 高亮，与
  DF2a_Nevma_G_LHM/dromik 对话领奖；③ 80989/80990 事件链在 IDRun 再次对话后行 1 高亮。

### 十七之四、边界与后续

- **QE-045 与 QE-051 的正面冲突已挂账（需一次客户端观测）**：13965/23965/15674/25674 在 QE-051
  口径下缺行 1 状态，在 QE-045 口径下则是“retail 忠实投影”。判定取舍只需一次实机观测：
  **在 REWARD 态（可开奖励窗口时）看任务书高亮的是行 0 还是行 1**——若显示行 1，则 QE-045 的
  packed-step 基线在这 4 个任务上需要按客户端证据重定基线（同时改 XML + 锁测试 + 审计常量）；
  若显示行 0，则保留现状并在审计里把“末行不可达”记为零售原样。观测前禁止批量翻动这 20 个锁任务。
- **`last_row_npc_matches_quest` 不是缺陷数**：本批 6 个任务里 1926/2938 为 `True`，
  39003/49003/80989/80990 为 `False`，后者是客户端表名（`LF2a_dromik_G_DHM`、
  `DF2a_Nevma_G_LHM`、`IDRUN_Entrance_guide`）与行内 `STR_DIC_*` key 不同形造成的，不是归属错误；
  该列只能做族内一致性定位。§十六 记录的 523→519 是当时的窄口径统计，本次刷新按全列统计为 2216
  （含 `STATES_BEYOND_ROWS` 等非缺陷族），两者不可直接比较，逐任务真值以 `batch13-evidence.tsv` 为准。
- **剩余 MISSING_LAST_ROW 93 个**（69 DIALOG + 24 OTHER）：其中 10 个为 QE-045 锁（含本批挂账的 4 个）、
  30 个镜像同缺末行、其余 53 个继续逐族收口；`ROW_BEHIND 205` 多为 `MISSING_TAIL_ROWS`（82），
  与 `STATES_BEYOND_ROWS 2625`、`INTERIOR_GAP 266` 一样禁止按行号机械推进。

---

## 十八、批次 14：事件族“两行、末行是与领奖 NPC 的对话”（4 个任务，2026-09-22）

### 十八之一、候选与证据

沿用批次 13 的族定义（客户端 2 行：行 0 目标、行 1 与领奖 NPC 对话），本批从刷新后的审计里挑出
两组**互相独立**的证据链：

| 任务 | 行 1 | 客户端 NPC 表 | 证据 |
|---|---|---|---|
| 80255 | 和帕尔图对话 | `831163 = event_Parutoo` | 行程第 1 行用**字面中文名**（无 `STR_DIC_N_` 键），所以批次 1-7 按“末行 NPC 键”筛选时漏掉；同族 `80257/80258/80259/80260` 已在 `7a7d27809`（批次 1-7）改成 `reward var0=1` + 同形自愈边，本批两个是族内漏网项；`npc-complete` owner = 831163 |
| 80256 | 和布巴纳对话 | `831164 = event_Boobanah` | 同 80255；owner = 831164 |
| 80601 | 向伊斯达报告 | `831831 = event_Isda` | legacy `_80601Fight_Of_The_Navigators`（`7e9f0316c^`）击杀分支 `setQuestVarById(1, +1)` 计数后 **`setQuestVarById(0, 1)` + `setStatus(REWARD)`** → legacy 领奖行就是 1；typed 击杀事务也保留 `set-variable var0=1`，但 `reward` 节点投影仍是 0，于是经**无 actions 的 `NPC_REPORT`** 进入领奖态的存档（投影补足为 0）匹配不到任何 reward 路由（`matchesSourceNode` 要求投影变量全等），必须同时补投影与自愈边 |
| 80606 | 向夏尔梅因报告 | `831832 = event_Charmeine` | legacy `_80606The_Good_News_And_Bad` 与 80601 同结构；owner = 831832 |

族内边界（本批不改，登记在 `batch14-evidence.tsv`）：

- `80602/80603/80604/80605/80607/80608/80609/80610`：客户端同样 2 行，但 `reward var0 = 3/4/5/9`
  是**阶段计数**（审计 `STATES_BEYOND_ROWS`，客户端只有 2 行），属“var0 不是行索引”族，禁止按行号
  机械压成 1。
- `50008/51008`：legacy 用 `setQuestVarById(0, var0 + 1)` 把 var0 当 0→2 的投递计数；行内
  `HousingLf_Event_ShugoSanta` 在 5.8 客户端 NPC 表里不存在（表内的 `831036 = Housing_lf_ChristmasEvent_Sugo`），
  归属无法核对 → 单独设计。
- `1123/1466/2484/2842/4712`：同形候选但证据不足——1123 行内是 `STR_DIC_LA12/STR_DIC_FLA07/STR_DIC_LA53`
  等非 NPC 键；1466 的 `reward` 节点**没有 var0 投影**（审计 `NO_REWARD_ROW`）；2484/4712 的 completion
  owner 不唯一（203331/204407/700267、279042/798327/798330）；2842 由击杀自环推进（`hunting->reward`）。

### 十八之二、修复

与批次 13 同一模板：`reward` 投影 `0 -> 1` + 无 source 的 `REWARD && var0==0 -> set var0=1`
（`LEVEL_AND_VISIBILITY_REFRESH`，无 priority）自愈边；80255/80256/80601/80606 原本都没有
`REWARD/var0==1` 入口边，本批不新增。脚本：
`.agents/summary/quest-10527-reward-row/apply_batch14_event_two_row_rows.py`（`--check` 幂等 4/4）。

### 十八之三、验证（2026-09-22，用户授权后执行）

- **单任务审计**：4/4 `ROW_BEHIND / MISSING_LAST_ROW / ROW_WITHOUT_STATE`（`visible_state_var0=0`，
  `recovery=False`）→ `ROW_ALIGNED / ALIGNED / ROW_STATE_ALIGNED`（`visible 0 1`，`recovery=True`）。
- **全库快照**：`ROW_ALIGNED 2639 -> 2643`、`ROW_BEHIND 205 -> 201`、`MISSING_LAST_ROW 93 -> 89`、
  `ROW_AHEAD 2591`、`NO_REWARD_ROW 179`、`NO_CLIENT_HTML 608`；`ROW_STATE_ALIGNED 2417 -> 2421`。
- **结构校验**：`xmllint --noout --schema quest_definition.xsd` 4/4 `validates`；IDEA lint 0 problems；
  `git diff --check` 干净。
- **Maven（原授权范围的同一条命令重跑）**：29 例全绿，`PRODUCTION_COMPILE_OK=6189 / FAILURES=0 /
  INTERACTION_OBJECT_FAILURES=0 / WHITELIST_VIOLATIONS=0` —— 覆盖本批 4 个 XML 的生产目录编译与
  既有 24 例门禁（`RewardRowTwoRowTalkFamilyContractTest` 6/6、`LegacyRewardStepProjectionRegressionTest` 1/1 等）。
- **门禁测试**：`src/test/java/com/aionemu/gameserver/questEngine/definition/RewardRowEventTwoRowContractTest.java`
  6 例——① `reward` 投影=1 且 `started` 保持 0；② 领奖 owner = 行 1 NPC；③ 自愈边唯一（`var0==0 -> 1`，
  无 priority）且 planner 可收敛；④ 无 target=reward 事务写非领奖行 var0；⑤ 同族已对齐参照
  80257-80260 必须保持 `reward var0=1`（族级模板护栏）；⑥ 80601/80606 的击杀事务必须保留
  legacy 的 `set-variable var0=1` 且以 `var0==0` 为门控。**已按追加授权跑 Maven：8 个测试类 35 例全绿**（下条）。
- 客户端实机复测：**PENDING_CLIENT**。复测要点：① 80255/80256 使用烟花道具后任务书切到行 1，
  与帕尔图/布巴纳对话可开奖励窗口；② 80601/80606 击杀 Boss 后行 1 高亮，与伊斯达/夏尔梅因对话领奖
  （旧存档在登录/切图时由自愈边纠正）。

### 十八之四、边界与后续

- **Maven（追加授权后执行，2026-09-22 11:52）**：`mvn -Dtest='RewardRowEventTwoRowContractTest,RewardRowTwoRowTalkFamilyContractTest,RewardNpcOwnershipContractTest,RetailSingleStepRewardRowContractTest,LegacyRewardStepProjectionRegressionTest,QuestClientContractGateTest,QuestDefinitionCatalogManifestTest,ProductionCatalogWhitelistVerificationTest' test` → **35 例全绿**（新增 `RewardRowEventTwoRowContractTest` 6/6、`RewardRowTwoRowTalkFamilyContractTest` 6/6、`RetailSingleStepRewardRowContractTest` 4/4、`RewardNpcOwnershipContractTest` 6/6、`LegacyRewardStepProjectionRegressionTest` 1/1、`QuestDefinitionCatalogManifestTest` 10/10、`QuestClientContractGateTest` 1/1、`ProductionCatalogWhitelistVerificationTest` 1/1），`PRODUCTION_COMPILE_OK=6189 / FAILURES=0 / INTERACTION_OBJECT_FAILURES=0 / WHITELIST_VIOLATIONS=0`。
- **同形候选清单（下一批可选）**：`1123 / 1466 / 2484 / 2842 / 4712`（各缺一项证据：非 NPC 行内键、
  缺 reward 投影、owner 不唯一、击杀自环），以及 `50008/51008`（var0 是投递计数 + 客户端表缺名）。
- **剩余 MISSING_LAST_ROW 89 个**（其中 10 个 QE-045 锁、30 个镜像同缺末行、49 个待逐族取证）；
  `ROW_BEHIND 201` 多为 `MISSING_TAIL_ROWS 82` 与 `INTERIOR_GAP 266`，`STATES_BEYOND_ROWS 2625`
  里的 var0 多为计数/阶段槽 —— 一律禁止按行号机械推进。

---

## 十九、批次 15：残余“两行、末行是与领奖 NPC 的对话”族（1123 / 2484，2026-09-22）

### 十九之一、候选与证据

批次 13/14 之后，审计里同形（客户端 2 行 + 末行对话 + `[%3]` = 值 1）的候选只剩 11 个，逐个取证后
只有两个可安全收口：

| 任务 | 行 1 | 证据 |
|---|---|---|
| 1123（Where's Tutty?） | 任务完成！和 `STR_DIC_LA12` 对话 | 行 1 的键在客户端 **actor 名命名空间**（`STR_DIC_LA12`），不在 `client_npcs_npc.xml` 的 `STR_DIC_N_*` 表里，所以审计 `last_row_npc_matches_quest=False`。**解键**：同族的 1006（末行“和 STR_DIC_LA12 对话，选择将来之路”）、1122、1124、30507 四个任务的 `npc-complete` owner 全部是 790001，而客户端表 790001 = **Pernos**；1123 自己的 NPC_START 与 npc-complete owner 同为 790001，自洽。进入 REWARD 的唯一路由是 `LF1_SENSORY_AREA_Q1123_210010000` enter-zone + `play-movie 11`（不写 var0）→ 投影必须为 1 |
| 2484（Our Man In Elysea） | 和 Hippolyta 对话 | legacy `_2484OurManInElysea` 在 **700267（烽火对象）处 `setQuestVarById(0, 1)`**，随后在 203331（`Hippolyta_Q2484`）处 `setStatus(REWARD)` → legacy 领奖行 = 1；typed 三条 `NPC_REPORT`（204407 Gelkugin / 700267 烽火 / 203331 Hippolyta → reward）都无 var0 动作，靠 reward 投影补足；客户端表 203331 = `Hippolyta_Q2484`（键名不同形 → `last_row_npc_matches_quest=False`，非缺陷） |

**同形候选的处置边界（本批登记、不改）**：

- **1466**：`reward` 节点**没有 var0 投影**（NO_REWARD_ROW），且被 `Quest1466ClientDialogAlignmentTest` 硬锁
  ——该测试断言 `reward` 必须无投影、`SELECT_QUEST_REWARD` 路由必须写 `var0=2`，并有 headless retail
  journey 通过。按 QE-051 收到行 1 需要同时改这条锁（补投影 + 自愈边 + 把报告路由的 2 改成 1），属
  重定基线，需一次客户端观测（领奖态任务书显示行 0 / 行 1 / 空白）。
- **4712**：行 1 要求“向 Henir 报告”，但 completion owner 是 279042（客户端表无名）与
  798327/798330（`IDAB1_Dreadgion_prisoner_dark1/4`）——报告 NPC 未登记，属批次 12 同型的**归属核实题**。
- **2842**：var0 是 0..39 的击杀计数（行 0 显示 `[%2]/39`），行 1 的可见槽位是 `[%15]`（= 值 5）而非
  `[%3]`，属“var0 不是行索引”族。
- **50008/51008**：legacy `setQuestVarById(0, var0 + 1)` 把 var0 当 0→2 的投递计数 + 行内 NPC 名不在
  5.8 客户端 NPC 表里。

### 十九之二、修复

与批次 13/14 同一模板：`reward` 投影 `0 -> 1` + 无 source 的 `REWARD && var0==0 -> set var0=1`
（`LEVEL_AND_VISIBILITY_REFRESH`，无 priority）自愈边。脚本
`.agents/summary/quest-10527-reward-row/apply_batch15_residual_two_row_rows.py`（`--check` 幂等 2/2）。

### 十九之三、验证（2026-09-22）

- **单任务审计**：2/2 `ROW_BEHIND / MISSING_LAST_ROW / ROW_WITHOUT_STATE`（`visible=0`，`recovery=False`）
  → `ROW_ALIGNED / ALIGNED / ROW_STATE_ALIGNED`（`visible 0 1`，`recovery=True`）。
- **全库快照**：`ROW_ALIGNED 2643 -> 2645`、`ROW_BEHIND 201 -> 199`、`MISSING_LAST_ROW 89 -> 87`、
  `ROW_STATE_ALIGNED 2421 -> 2423`。
- **结构校验**：`xmllint --schema` 2/2 validates；IDEA lint 0 problem；`git diff --check` 干净。
- **Maven（已授权范围的 8 个测试类重跑）**：35 例全绿，`PRODUCTION_COMPILE_OK=6189 / FAILURES=0 /
  INTERACTION_OBJECT_FAILURES=0 / WHITELIST_VIOLATIONS=0`（覆盖本批 2 个 XML 的生产目录编译）。
- **门禁测试**：`src/test/java/com/aionemu/gameserver/questEngine/definition/RewardRowResidualTwoRowContractTest.java`
  6 例——① 投影=1 且 started=0；② 领奖 owner = 行 1 NPC；③ **1123 的 REWARD 入口必须是 LF1 感应区
  enter-zone + `play-movie 11`、2484 必须保留 legacy 的三条报告 route（204407/700267/203331）**；
  ④ 自愈边唯一且 planner 可收敛；⑤ 无 target=reward 事务写非领奖行 var0；⑥ **STR_DIC_LA12 解键护栏**
  （1006/1122/1124/30507 的 completion owner 必须都是 790001）。
- **Maven（追加授权后执行，2026-09-22 12:03）**：9 个测试类 **41 例全绿**（含新增 `RewardRowResidualTwoRowContractTest` 6/6），`PRODUCTION_COMPILE_OK=6189 / FAILURES=0 / INTERACTION_OBJECT_FAILURES=0 / WHITELIST_VIOLATIONS=0`。首轮运行暴露两条**本测试自身过紧**的断言并已修正：2484 的 completion owner 实际是 3 条备用路径（204407/700267/203331，本批只收口行投影，断言改为“行 1 NPC 必须是 owner 之一”）、1122 的完成走 choice/select 事务而非 `TalkToNpc`（解键护栏改为直接断言原文件的 `<npc-complete npc-id="790001"`）
- 客户端实机复测：**PENDING_CLIENT**。① 1123 进入 LF1 感应区看完影片后任务书切到行 1，与 Pernos
  对话领奖；② 2484 点燃烽火后任务书切到行 1，与 Hippolyta 对话领奖（旧存档登录/切图时自愈）。

### 十九之四、结论与后续

- **“两行 + 末行对话”族已收口完毕**：批次 13/14/15 共收口 12 个（6 + 4 + 2），剩余同形候选全部转入
  明确挂账（1466 锁、4712 归属、2842 计数槽、50008/51008 计数槽 + 客户端表缺名）。
- 剩余 `MISSING_LAST_ROW 87`：10 个 QE-045 锁（含 13965 族，等一次客户端观测）、30 个镜像同缺末行、
  47 个待逐族取证（多为 `INTERIOR_GAP`/`MISSING_TAIL_ROWS`/`STATES_BEYOND_ROWS` 阶段语义，禁止按行号机械推进）。

---

## 二十、批次 16：领奖 owner 收敛（4712 / 2484，2026-09-22）

### 二十之一、候选与证据

批次 15 之后同形候选里剩下的“owner 不唯一”题只有两个，本批逐个取证：

| 任务 | 行 1（客户端 quest_summary） | 证据 |
|---|---|---|
| 4712（[Group] Escape From The Dredgion!，魔族） | 向 `STR_DIC_N_Henir` 报告 | legacy `_4712Escape_From_The_Dredgion`（`7e9f0316c^`）：START 态囚犯 798327/798330 的 `STEP_TO_1` 调 `defaultCloseDialog(env, 0, 1, true, false)`（**写 var0=1 并置 REWARD**，囚犯随即 `onDelete`），REWARD 态只处理 **279042**（`sendQuestDialog(10002)` / `sendQuestEndDialog`）。Henir 解键：同族 **4713/4714/4715/4716 行内都写 `STR_DIC_N_Henir` 且 `npc-complete` owner 全部 = 279042**（审计 4713/4714/4716 均 `ROW_ALIGNED`、reward=1）→ Henir = 279042。即 279042 = 领奖台，囚犯只是行 0 的开监狱门交互 |
| 2484（[Spy] Our Man in Elysea，魔族） | 和 Hippolyta 对话 | 批次 15 已把 reward 投影收到行 1，但 completion 仍同时登记在 **204407（接取）/ 700267（烽火对象）/ 203331（Hippolyta_Q2484）**；legacy `_2484OurManInElysea` 只在 203331 处 `setStatus(REWARD)`，700267 只 `setQuestVarById(0, 1)`、204407 只负责接取 → 领奖 owner 必须唯一收敛到 203331 |

**同批登记、不改的边界**：28208/28209 的 Inggness(205320) 与 Anja(205321) 仍并存为领奖 NPC（客户端是否两个都能领奖未取证，禁止先收敛）；26838/16838 的 `Jarik01/Ostia01`(806574/806565) vs 定义 `Jarik02/Ostia02`(806575/806566) 01/02 变体分歧仍需真端绑定。

### 二十之二、修复

- **4712**：`reward` 投影 `0 -> 1` + 无 source 的 `REWARD && var0==0 -> set var0=1`（`LEVEL_AND_VISIBILITY_REFRESH`，无 priority）自愈边；**删除囚犯 798327/798330 的 `npc-complete`**，保留它们的 `NPC_REPORT -> reward` 入口路由（行 0 的“开监狱门并与囚犯对话”）。
- **2484**：**删除 204407/700267 的 `npc-complete`**，保留三条 `NPC_REPORT -> reward` 入口路由；领奖唯一落在 203331。
- 脚本 `.agents/summary/quest-10527-reward-row/apply_batch16_reward_owner_trim.py`（`--check` 幂等 2/2）；4712.xml 内写入 QE-051/QE-052 双语注释。

### 二十之三、验证（2026-09-22）

- **单任务审计**：4712 `ROW_BEHIND / MISSING_LAST_ROW / ROW_WITHOUT_STATE`（`visible=0`、`recovery=False`）
  → `ROW_ALIGNED / ALIGNED / ROW_STATE_ALIGNED`（`visible 0 1`、`recovery=True`）；2484 批次 15 已对齐，本批 owner 收敛后判定不变。
- **全库快照**：`ROW_ALIGNED 2645 -> 2646`、`ROW_BEHIND 199 -> 198`、`ROW_STATE_ALIGNED 2423 -> 2424`、
  `ROW_WITHOUT_STATE 524 -> 523`、`MISSING_LAST_ROW 87 -> 86`（`last_row_npc_matches_quest` 2216/4006 不变）。
- **结构校验**：`xmllint --noout --schema quest_definition.xsd` 2/2 `validates`；IDEA lint 0 problem；
  `git diff --check` 干净。
- **门禁测试**：`src/test/java/com/aionemu/gameserver/questEngine/definition/RewardOwnerTrimContractTest.java`
  7 例——① 领奖 completion owner 唯一且 = 领奖行 NPC；② 行 0 入口路由 owner 集合保真（4712 = 279042/798327/798330，
  2484 = 204407/700267/203331）；③ 被裁剪 owner 不再有 `npc-complete` 且每个任务恰好一个 `npc-complete`；
  ④ 投影 = 行 1 且 `started` 保持 0；⑤ 自愈边唯一且 planner 可收敛；⑥ 无 target=reward 事务写非领奖行 var0；
  ⑦ Henir 解键护栏（4713/4714/4716 的 completion owner 必须都是 279042）。
- **Maven（授权后执行，2026-09-22 12:13）**：`mvn -Dtest='RewardOwnerTrimContractTest,RewardRowResidualTwoRowContractTest,
  RewardRowEventTwoRowContractTest,RewardRowTwoRowTalkFamilyContractTest,RewardNpcOwnershipContractTest,
  RetailSingleStepRewardRowContractTest,LegacyRewardStepProjectionRegressionTest,QuestClientContractGateTest,
  QuestDefinitionCatalogManifestTest,ProductionCatalogWhitelistVerificationTest' test` → **10 个测试类 48 例全绿**
  （新增 `RewardOwnerTrimContractTest` 7/7），`PRODUCTION_COMPILE_OK=6189 / FAILURES=0 /
  INTERACTION_OBJECT_FAILURES=0 / WHITELIST_VIOLATIONS=0`；覆盖本批 2 个 XML 的生产目录编译。
- 客户端实机复测：**PENDING_CLIENT**。复测要点：① 4712 救出囚犯后任务书切到行 1，与 Henir(279042) 对话领奖，
  且囚犯身上不再能直接领奖；② 2484 点燃烽火后任务书切到行 1，与 Hippolyta 对话领奖，接取 NPC 与烽火对象
  不再弹奖励窗（旧存档登录/切图时由自愈边纠正）。

### 二十之四、结论与后续

- 新增模式卡 **QE-052**（领奖 owner 必须等于客户端任务书领奖行 NPC，`REWARD_OWNER_MUST_BE_JOURNAL_REWARD_ROW_NPC`）：
  QE-051 管投影（var0 = 行号），QE-052 管 owner 集合，两者判定口径互不替代。
- 剩余 `MISSING_LAST_ROW 86`（其中 QE-045 锁 10 个；其余按镜像/逐族取证，禁止按行号机械推进）。
- Maven 已按上述命令执行（48 例全绿），后续批次沿用同一命令再加新门禁类；历史待授权命令：
  `mvn -Dtest='RewardOwnerTrimContractTest,RewardRowResidualTwoRowContractTest,RewardRowEventTwoRowContractTest,RewardRowTwoRowTalkFamilyContractTest,RewardNpcOwnershipContractTest,RetailSingleStepRewardRowContractTest,LegacyRewardStepProjectionRegressionTest,QuestClientContractGateTest,QuestDefinitionCatalogManifestTest,ProductionCatalogWhitelistVerificationTest' test`

---

## 二十一、批次 17：圣灵守护者武器事件族（80290/80291/80294/80295，2026-09-22）

### 二十一之一、族级证据链（先做全库扫描，再定族）

本批先用 **legacy 入口 helper 全库扫描** 定位“nextStep 被迁移丢弃”的完整族（新工具
`.agents/summary/quest-10527-reward-row/audit_legacy_reward_entry_steps.py`，输出 `legacy-reward-entry-scan.tsv`）：

| 口径 | 数量 | 说明 |
|---|---|---|
| 迁移前 handler 里进入 REWARD 的调用（`useQuestItem` / `defaultCloseDialog` / `checkQuestItems` / `checkQuestItemsSimple` / `changeQuestStep`，`reward=true`，`step != nextStep`） | 651 个任务 | 来源 commit `7e9f0316c^` 的 `quest/handlers/**`，签名见同 commit 的 `QuestHandler` |
| 其中当前 reward 投影 == legacy `nextStep`（迁移正确） | 86 | 例如 4712/10527/10528/11216/14023 |
| `LEGACY_NEXTSTEP_DROPPED`（当前投影 == `step` 且 `nextStep` == 客户端末行） | **4 → 2 → 0** | 收口前 = 15300/25300（QE-045 基线，`Quest15300And25300RewardProjectionTest` 锁定，不得动）+ 80291/80295（本批收口）；收口后为空 |
| `STEP_EQUALS_NEXTSTEP`（legacy 本身保持进入前的 packed step，QE-045 语义） | 392 | 例如 2600/1920/2945 的 `defaultCloseDialog(env, s, s, true, ...)`，禁止按行号机械推进 |

### 二十一之二、四个任务的证据

| 任务 | 客户端 quest_summary | 当前投影 | 证据 | 修复 |
|---|---|---|---|---|
| 80291（Durable Daevanion Weapon，天） | 2 行：行 0「收集 5 个 `relic_weapon_30`，交给 `EVENT_Zephyrin`」、行 1「从 `EVENT_Zephyrin` 那里获得圣灵守护者武器」 | 0 | legacy `checkQuestItems(env, 0, 1, true, 5, 0)`：packed step 0 → **1** 并置 REWARD；迁移丢了 nextStep | 投影 `0→1` + `REWARD/var0=0 → 1` 自愈边 |
| 80295（魔族镜像） | 同上（`EVENT_Lilyolin` / 831387） | 0 | legacy `checkQuestItems(env, 0, 1, true, 5, 0)` 同上 | 同上 |
| 80290（护甲变体，天） | **1 行**：「收集 10 个 `relic_armor_30`，交给 `EVENT_Zephyrin`」 | 1 | 投影 1 落在客户端不存在的行号上（审计 `STATE_OUT_OF_RANGE`），领奖态任务书无行可高亮；迁移前仓库无该任务 handler，证据 = 客户端行数 + 同族 80291 的 legacy nextStep | 投影 `1→0` + `REWARD/var0=1 → 0` 自愈边 |
| 80294（护甲变体，魔） | 1 行（`EVENT_Lilyolin` / 831387） | 1 | 同 80290 | 同上 |

领奖 owner（QE-052）：四个任务的 `npc-complete` 都只有一个，且等于客户端行内 NPC（831384 = `event_Zephyrin`、
831387 = `event_Lilyolin`），行 0/行 1 同一 NPC，无需 owner 收敛。

### 二十一之三、修复与验证（2026-09-22）

- 脚本 `.agents/summary/quest-10527-reward-row/apply_batch17_daevanion_durable_weapon.py`（`--check` 幂等 4/4）。
- **单任务审计**：80291/80295 `ROW_BEHIND / ROW_WITHOUT_STATE`（`visible=0`）→ `ROW_ALIGNED / ROW_STATE_ALIGNED`（`visible 0 1`、`recovery=True`）；
  80290/80294 `ROW_AHEAD / STATE_OUT_OF_RANGE`（`visible 0 1`）→ `ROW_ALIGNED / ROW_STATE_ALIGNED`（`visible 0`、`recovery=True`）。
- **全库快照**：`ROW_ALIGNED 2646 -> 2650`、`ROW_BEHIND 198 -> 196`、`ROW_AHEAD 2591 -> 2589`、
  `ROW_STATE_ALIGNED 2424 -> 2428`、`ROW_WITHOUT_STATE 523 -> 521`、`STATE_OUT_OF_RANGE 2448 -> 2446`、
  `MISSING_LAST_ROW 86 -> 84`。
- **族级扫描复核**：`LEGACY_NEXTSTEP_DROPPED` 由 `[15300, 25300, 80291, 80295]` 收敛为 `[]`（15300/25300 转入 `TEST_LOCKED` 基线）。
- **结构校验**：`xmllint --schema` 4/4 validates；IDEA lint 0 problem；`git diff --check` 干净。
- **门禁测试**：`src/test/java/com/aionemu/gameserver/questEngine/definition/DurableDaevanionWeaponRewardRowContractTest.java`
  7 例——① 武器变体投影行 1；② 护甲变体投影行 0 且不得暴露行 1；③ 领奖 owner 唯一 = 族 NPC；④ 同族兄弟共用事件 NPC
  且行数不同；⑤ 自愈边唯一 + planner 收敛（武器 0→1、护甲 1→0）；⑥ 无 target=reward 事务写非领奖行 var0；
  ⑦ 15300/25300 保持 legacy packed step 13（QE-045 基线护栏）。
- **Maven（授权后执行，2026-09-22 12:31）**：12 个测试类 **58 例全绿**（含本批新增 `DurableDaevanionWeaponRewardRowContractTest` 7/7），
  `PRODUCTION_COMPILE_OK=6189 / FAILURES=0 / INTERACTION_OBJECT_FAILURES=0 / WHITELIST_VIOLATIONS=0`；
  覆盖本批 4 个 XML 的生产目录编译与 `QuestItemSourceContractGateTest` 的 80291/80295 交付物合同。
- 客户端实机复测：**PENDING_CLIENT**。要点：① 80291/80295 交满 5 个武器残骸后任务书切到行 1 并与 Zephyrin/Lilyolin 领奖；
  ② 80290/80294 交满 10 个护甲残骸后任务书停留在唯一行且奖励窗正常打开（旧存档登录/切图时由自愈边纠正）。

### 二十一之四、结论与后续

- “legacy nextStep 被丢弃”的族至此**清空**：全库扫描后仅剩 `TEST_LOCKED`（15300/25300）与 `QE045_LOCKED`（3722/4722 等）两个基线集合。
- 剩余 `MISSING_LAST_ROW 84`：多为 `STEP_EQUALS_NEXTSTEP`（legacy 本身保持进入前 step，需客户端观测才能改）与
  多阶段/计数任务，禁止按行号机械推进。
- 本批 Maven 命令（已执行，58 例全绿；后续批次沿用并追加新门禁类）：
  `mvn -Dtest='DurableDaevanionWeaponRewardRowContractTest,RewardOwnerTrimContractTest,RewardRowResidualTwoRowContractTest,RewardRowEventTwoRowContractTest,RewardRowTwoRowTalkFamilyContractTest,RewardNpcOwnershipContractTest,RetailSingleStepRewardRowContractTest,LegacyRewardStepProjectionRegressionTest,QuestClientContractGateTest,QuestItemSourceContractGateTest,QuestDefinitionCatalogManifestTest,ProductionCatalogWhitelistVerificationTest' test`


## 二十二、批次 18：镜像单侧投影落后族（11110/14201/16974/17160/17161/17526，2026-09-22）

### 二十二之一、族级定位方法（先扫全库，再定族）

批次 1-3 已用过的镜像方法在本批固化为可复用判据：**对每一对同形镜像 `q` / `q±10000`（天/魔）**，

1. 客户端 `quest_summary` **行数相同**；
2. **末行 NPC 都能在该任务自己的 `npc-*` 路由里对上**（`last_row_npc_matches_quest=True`）；
3. 但只有**一侧**的 `reward` 投影等于领奖行（末行行号），另一侧仍停在行 0。

此时**已对齐的那一侧就是另一侧的参照基线**——两侧客户端任务书同形，行号语义相同，没有理由只有一侧进入领奖行。
本批 6 个成员都满足上述三条，且**都已具备完整的 `0..N-1` 行状态**（`rows_without_state` 为空、`row_state` 已是
`ROW_STATE_ALIGNED`），差别只在 `reward` 节点这一行投影——所以修复是纯投影 + 自愈，不需要补状态阶梯。

### 二十二之二、六个任务的证据

| 任务 | 镜像（参照） | 客户端 quest_summary | reward 投影 | 证据 | 修复 |
|---|---|---|---|---|---|
| 11110 | 21110 | 2 行：行 0「消灭工房巨人（[%2]/10）」、行 1「和 Suleion 对话」 | 0 → 1 | 镜像 21110 的 reward 投影已是 1；两侧行数/NPC 对齐 | 投影 `0→1` + `REWARD/var0=0 → 1` 自愈边 |
| 14201 | 24201 | **3 行**：行 0 Honglas、行 1 Serimnir、行 2「向 Atropos 报告（collectitem）」 | 0 → 2 | 镜像 24201 reward=2；迁移前 legacy `changeQuestStep(2->2)` 独立印证领奖态保持 packed step 2 | 投影 `0→2` + `REWARD/var0=0 → 2` 自愈边 |
| 16974 | 26974 | 2 行：行 0「消灭贝尔库勒军团长（[%2]/1）」、行 1「和 IDLDF5_Under_01_Theano_E 对话」 | 0 → 1 | 镜像 26974 reward=1 | 投影 `0→1` + 自愈边 |
| 17160 | 27160 | 2 行：行 0「在 LF5 消灭 IDLegion_Q17160（[%2]/10）」、行 1「向 LF5_Atmos_E 报告」 | 0 → 1 | 镜像 27160 reward `var0=1`（**另带 `var1=10`**，属镜像侧独有计数槽，本批只对齐行号 `var0`） | 投影 `0→1` + 自愈边 |
| 17161 | 27161 | 2 行：行 0「去 DF5 消灭 IDLegion_Q17161（[%2]/10）」、行 1「向 LF5_Atmos_E 报告」 | 0 → 1 | 镜像 27161 reward `var0=1`（同样带 `var1=10`） | 投影 `0→1` + 自愈边 |
| 17526 | 27526 | 2 行：行 0「前往 IDAbRe_Core_03 消灭 Witch_Boss_Ae（[%2]/1）」、行 1「向 Ab1_Plania_E 报告」 | 0 → 1 | 镜像 27526 reward=1 | 投影 `0→1` + 自愈边 |

领奖 owner（QE-052）：6 个任务的客户端末行 NPC 与任务内 `npc-complete` 一致（799075 = Suleion、798155 = Atropos、
801763 = IDLDF5_Under_01_Theano_E、804699 = LF5_Atmos_E（17160/17161 共用）、806781 = Ab1_Plania_E），无需 owner 收敛。
14201 侧另有 798212 = Serimnir、800407 = honglas 两个前置交付 NPC，均落在行 0/行 1，不冲突。

### 二十二之三、同族但锁定、本批不改

`16837 / 16986 / 16988` 属同一“镜像单侧落后”形态，但被既有门禁
`QuestPrematureRewardRouteExclusionTest` 的 `RewardCase.reward = {var0: 0}` 断言锁定（完成报告后 packed `var0`
**保持 0**）。这是仓库里与镜像推断相反的既有契约，**在拿到客户端观测前不得按镜像推进**；本批把这三个任务写进
`MirrorRewardProjectionLagContractTest` 的 `LOCKED_MIRROR_SIBLINGS` 护栏。

### 二十二之四、修复与验证（2026-09-22）

- 脚本 `.agents/summary/quest-10527-reward-row/apply_batch18_mirror_projection_lag.py`
  （`--check` 改动前 FAIL → APPLY 6/6 OK → `--check` 6/6 幂等）。
- **单任务审计**（6/6）：`ROW_BEHIND / ROW_STATE_ALIGNED`（`visible = 0 1`，14201 为 `0 1 2`，`recovery=False`）
  → `ROW_ALIGNED / ROW_STATE_ALIGNED`（`visible` 不变、`recovery=True`、`rows_without_state` 空）。
- **全库快照**：`ROW_ALIGNED 2650 -> 2656`、`ROW_BEHIND 196 -> 190`（`ROW_AHEAD 2589`、`NO_CLIENT_HTML 608`、
  `NO_REWARD_ROW 179` 不变）；`row_state` 计数不变（`ROW_STATE_ALIGNED 2428`、`ROW_WITHOUT_STATE 521`、
  `STATE_OUT_OF_RANGE 2446`、`BOTH_MISALIGNED 177`）——本批属“有状态、只落后投影”形态，不动 `MISSING_LAST_ROW`
  （仍 84 行数据）。
- **结构校验**：`xmllint --noout --schema quest_definition.xsd` 6/6 `validates`；IDEA lint 0 problem；`git diff --check` 干净。
- **门禁测试**：`src/test/java/com/aionemu/gameserver/questEngine/definition/MirrorRewardProjectionLagContractTest.java`
  5 例——① 落后侧投影 == 镜像领奖行且 `status=REWARD`；② 自愈边唯一 + `QuestMutationPlanner` 收敛到镜像行；
  ③ 无 `target=reward` 事务写非领奖行 `var0`；④ `16837/16986/16988` 保持 `var0=0`（锁定兄弟护栏）；
  ⑤ 镜像两侧领奖行 `var0` 一致（只比行号：27160/27161 另有 `var1=10` 计数槽，不参与比较）。
- **Maven（授权后执行，2026-09-22 12:37）**：15 个测试类 **92 例全绿**（含本批新增
  `MirrorRewardProjectionLagContractTest` 5/5），
  `PRODUCTION_COMPILE_OK=6189 / FAILURES=0 / INTERACTION_OBJECT_FAILURES=0 / WHITELIST_VIOLATIONS=0`。
  第 1 轮曾因测试自身过紧（整张投影 Map 比较，27160/27161 的 `var1=10` 造成假失败）在 92 例中失败 1 例，
  已改为只比较 `var0` 后重跑全绿——**生产 XML 未因该断言改动**。
- **证据表**：[batch18-evidence.tsv](batch18-evidence.tsv)（含 3 个锁定兄弟行）。
- 客户端实机复测：**PENDING_CLIENT**。要点：① 已进入 `REWARD/var0=0` 的旧存档在登录/切图时由自愈边纠正到镜像领奖行；
  ② 六个任务领奖对话正常打开、任务书高亮末行（14201 为第 3 行）。17160/17161 另需确认 `var1=10` 是否随领奖态保留。

### 二十二之五、结论与后续

- “镜像单侧投影落后”形态本批清空（参考族剩 `16837/16986/16988` 锁定三例，需客户端观测）。
- 下一批（批次 19）转向**镜像单侧 `ROW_BEHIND` 且需要补中间状态阶梯**的 B 组：`15000/15670`（rows=4，reward 0→3）、
  `23809`（0→3）、`23918`（1→5）、`24046`（6→7，镜像行状态本身有缺口）。这些**不能只改投影**，要补 `START` 状态阶梯
  （`MISSING_TAIL_ROWS` / `INTERIOR_GAP`），须逐族取 legacy/客户端证据。
- 本批 Maven 命令（已执行，92 例全绿；后续批次沿用并追加新门禁类）：
  `mvn -Dtest='MirrorRewardProjectionLagContractTest,QuestPrematureRewardRouteExclusionTest,Quest11110And1548PostKillReportDialogTest,DurableDaevanionWeaponRewardRowContractTest,RewardOwnerTrimContractTest,RewardRowResidualTwoRowContractTest,RewardRowEventTwoRowContractTest,RewardRowTwoRowTalkFamilyContractTest,RewardNpcOwnershipContractTest,RetailSingleStepRewardRowContractTest,LegacyRewardStepProjectionRegressionTest,QuestClientContractGateTest,QuestItemSourceContractGateTest,QuestDefinitionCatalogManifestTest,ProductionCatalogWhitelistVerificationTest' test`


## 二十三、批次 19：单步塌陷对（Elyos 存根补回行状态阶梯，15000 / 15670，2026-09-22）

### 二十三之一、族级定位方法（与批次 18 同源，落点不同）

批次 18 收的是“**有完整状态、只有 reward 投影落后**”的镜像对；本批收的是同一镜像方法下更重的一类：

1. 同形镜像对 `q` / `q±10000` 客户端 quest_summary **行数相同**、末行 NPC 都能在任务内对上；
2. 镜像那一侧已经有完整的 `0..N-1` START/REWARD 阶梯（审计 `ROW_ALIGNED / ROW_STATE_ALIGNED`）；
3. 落后那一侧被迁移**塌陷成“单步交接直接置 REWARD”**，只剩行 0 一个状态
   （审计 `ROW_BEHIND / MISSING_TAIL_ROWS / ROW_WITHOUT_STATE`）。

这类任务**不能只改 reward 投影**——改完会指向一个客户端有、服务端却没有状态的行，任务书在中间步骤会空白。
必须按客户端对话页 `<Act href="HACTION_*">` 给出的动作序列把中间行补回来（本批两个任务的页动作链完整可见）。
全库扫描（`audit-output.tsv` 与镜像列交叉）后，同形且镜像已对齐的塌陷任务共 7 个：
`1000/11000`、`15000/25000`、`15670/25670`、`23809/13809`、`23918/13918`、`24046/14046`、`39713/49713`；
本批先收**客户端页动作链可直接复原**、镜像结构干净的两个（15000、15670），其余 5 个挂账（见二十三之五）。

### 二十三之二、两个任务的证据

| 任务 | 镜像（参照） | 客户端 quest_summary | 客户端页动作链（解包 HTML） | 迁移前塌陷形态 |
|---|---|---|---|---|
| 15000（The Elyos stub） | 25000（4 行 / reward=3，结构干净） | 行 0 回收 `quest_15000a` 交给米利亚德 / 行 1 和米利亚德对话 / 行 2 在人工奥德生成器附近使用修理工具 / 行 3 和米利亚德对话 | `select1` → `HACTION_CHECK_USER_HAS_QUEST_ITEM`（交背囊 182215661）→ `check_user_item_ok` `HACTION_SELECT2`(1352) → `select2` `HACTION_SELECT2_1`(1353) → `select2_1` `HACTION_SETPRO2`(10001)（“从米利亚德那里接过修理工具”，give-item 182215662）→ 行 2 使用工作物品 → 行 3 `HACTION_SELECT_QUEST_REWARD` | legacy `_15000Not_Any_Fool_Tool` 只有 `checkQuestItems(env, 0, 0, true, 10000, 10001)`：交物品直接置 REWARD、`var0` 停在 0 |
| 15670（Investigating the Ancient Archon Weapon Invasion） | 25670（4 行 / reward=3：行 0/1 布里兹内 806105、行 2 FOBJ 731794、行 3 莱茵哈特 806116） | 行 0 与赫梅洛斯对话 / 行 1 把 4 处痕迹的证据交给赫梅洛斯 / 行 2 调查第五处痕迹 `..._Q15670E` / 行 3 向伊利西亚报告 | `select1` → `HACTION_SETPRO1`(10000) → `select2` `HACTION_CHECK_USER_HAS_QUEST_ITEM`(39)（交 4 件证据 182216189-192）→ `select3` `HACTION_SET_SUCCEED`(10255) → `select_success` `HACTION_SELECT_QUEST_REWARD`(1009) | 迁移把**所有东西都挂到领奖 NPC 伊利西亚 806114**：NPC_START/NPC_REPORT/4 件证据交付一次性收口（旧 route `started --SELECT_QUEST_REWARD--> reward`） |

天/魔 id 对照（客户端 `npcs_unpacked/client_npcs_npc.xml`，两列同名同角色）：
米利亚德 804874 ↔ 柯格豪根 804718；赫梅洛斯 806093 ↔ 布里兹内 806105；`LF6_FOBJ_Od_Track_Q15670e` 731793 ↔ `DF6_FOBJ_Od_Track_Q25670e` 731794；
伊利西亚 806114 ↔ 莱茵哈特 806116；4 处痕迹 703434-703437 ↔ 703439-703442。

### 二十三之三、修复

两个任务都按“**每行一个状态**”重建：`started(0) → s1(1) → s2(2) → reward(3)`，并补
`status=REWARD && var0==0 -> set var0=3` 的无 source `enter-world` 自愈边——塌陷定义把“交完物品”的玩家
直接写成 `REWARD + var0=0`，改完阶梯后这类旧存档匹配不到 reward 节点的投影，必须自愈到领奖行，
否则连奖励对话都点不出来（与 10527/15300 的旧存档自愈边同形）。

- **15000**：`started --CHECK_USER_HAS_QUEST_ITEM(39)--> s1`（条件 `has-item 182215661`、动作 remove-item），
  `s1 --SELECT2(1352)--> page SELECT2`、`s1 --SELECT2_1(1353)--> page SELECT2_1`、
  `s1 --SETPRO2(10001)--> s2`（`give-item 182215662`，工作物品已在 metadata 声明）、
  `s2 --use-item 182215662--> reward`；领奖窗口仍由 `npc-complete` 的 preview 路由（`USE_OBJECT` / `SELECT_QUEST_REWARD`）承接。
- **15670**：`started --SETPRO1(10000)--> s1`（在赫梅洛斯 806093 处）、
  `s1 --CHECK_USER_HAS_QUEST_ITEM--> s2`（条件/动作 4 件证据）、4 处痕迹的 `TALK_TO_NPC`/`can-act` 自环
  从行 0 **移到行 1**、`s2 --USE_OBJECT--> page SELECT3`、`s2 --SET_SUCCEED(10255)--> reward`（第五处痕迹 731793）、
  行 3 在伊利西亚 806114 报告；metadata 里 4 处痕迹 drops 的 `collecting-step` 由 `0` 同步改为 `1`
  （`QuestInteractionObjectValidator#validateCatalogDrops` 用 source 节点 var0 比对 collecting-step，镜像 25670 即 1）。

### 二十三之四、验证（2026-09-22）

- 脚本 `.agents/summary/quest-10527-reward-row/apply_batch19_collapsed_single_step_ladder.py`
  （`--check` 改动前 PENDING → APPLY 2/2 → `--check` 2/2 幂等）。
- **单任务审计**：15000/15670 `ROW_BEHIND / MISSING_TAIL_ROWS / ROW_WITHOUT_STATE`（`visible=0`、`rows_without_state=1 2 3`、`recovery=False`）
  → `ROW_ALIGNED / ALIGNED / ROW_STATE_ALIGNED`（`visible=0 1 2 3`、`recovery=True`、`rows_without_state` 空）。
- **全库快照**：`ROW_ALIGNED 2656 -> 2658`、`ROW_BEHIND 190 -> 188`、`ROW_STATE_ALIGNED 2428 -> 2430`、
  `ROW_WITHOUT_STATE 521 -> 519`（`ROW_AHEAD 2589`、`STATE_OUT_OF_RANGE 2446`、`BOTH_MISALIGNED 177`、
  `MISSING_LAST_ROW 84` 不变）——本批属“缺状态阶梯”形态，`MISSING_LAST_ROW` 名单不动。
- **结构校验**：`xmllint --noout --schema quest_definition.xsd` 2/2 validates；`git diff --check` 干净。
- **编译期冲突定位（本批新增经验）**：首轮 `npc-complete` 的 `<preview actions="USE_OBJECT SELECT_QUEST_REWARD"/>`
  已经展开出 `reward -reward / TalkToNpc(npc, 1009)` 与 `TalkToNpc(npc, USE_OBJECT=-1)` 两条路由，
  再显式写一条 `reward --SELECT_QUEST_REWARD--> reward` 会触发 `AMBIGUOUS_TRANSITION`；
  领奖窗口**必须留在 npc-complete 的 preview 上**，不要重复声明（15000/15670 均已按此处理）。
- **门禁测试**：`src/test/java/com/aionemu/gameserver/questEngine/definition/CollapsedSingleStepLadderContractTest.java`
  7 例——① 两个任务 reward 投影 == 镜像领奖行 3 且状态 REWARD；② 每行都有状态（行 0..2 为 START、行 3 为 REWARD）；
  ③ 15000 页按钮链（`CHECK_USER_HAS_QUEST_ITEM` → `SELECT2`(1352) → `SELECT2_1`(1353) → `SETPRO2` → `use-item`）
  与服务端页面/物品一一对应；④ 15670 阶梯 + 4 件证据消耗 + 领奖 owner 唯一（806114）；
  ⑤ 4 处痕迹的 `can-act` 与 `collecting-step` 已移到行 1；⑥ 旧存档自愈边唯一 + `QuestMutationPlanner` 收敛到 3；
  ⑦ 不再保留 `started -> reward` 塌陷跳转、reward 路由不写非领奖行 var0。
- **Maven（授权后执行，2026-09-22 12:59）**：16 个测试类 **99 例全绿**（含本批新增 7 例），
  `PRODUCTION_COMPILE_OK=6189 / FAILURES=0 / INTERACTION_OBJECT_FAILURES=0 / WHITELIST_VIOLATIONS=0`。
  首轮 `QuestClientContractGateTest` 报 1 条真实断点
  `BUTTON_WITHOUT_ROUTE|15000|s1|804874|31|1352|1353`——`select2` 页的 `HACTION_SELECT2_1(1353)` 没有对应路由
  （且我当时把 1352 的落地页写成了 `SELECT2_1`）；已按客户端页按钮链拆成
  `1352 -> page SELECT2` 与 `1353 -> page SELECT2_1` 两条路由并加入门禁断言，重跑全绿。
- **证据表**：[batch19-evidence.tsv](batch19-evidence.tsv)。
- 客户端实机复测：**PENDING_CLIENT**。要点：① 15000 在米利亚德处交背囊后任务书切到行 1、接过修理工具后切到行 2、
  在人工奥德生成器附近使用修理工具后切到行 3 并可领奖；② 15670 与赫梅洛斯对话后行 1、调查 4 处痕迹后交出证据切到行 2、
  调查第五处痕迹后行 3 向伊利西亚报告领奖；③ 老存档（历史上已交物品、`REWARD+var0=0`）登录/切图后应落在领奖行。

### 二十三之五、结论与后续

- “单步塌陷对”还剩 5 个挂账，均需逐族处理，**不能套用本批模板**：
  - `23809/13809`（4 行，三棵 `DeadTree` 730969/730970/730971）：镜像 13809 自身也把树登记成 `NPC_START` + `npc-complete`
    （owner 冗余），需要“补阶梯 + 领奖 owner 收敛（QE-052）”一起做；
  - `23918/13918`（6 行）：客户端 `quest_monster.csv` 用 `SECTION_0..4` 串行门控 5 只精锐兵，而当前 23918 是
    32 节点组合式模型，需先定“串行阶梯 vs 组合计数”的建模口径再动；
  - `24046/14046`（8 行，`INTERIOR_GAP`：缺行 4、行 7）：两侧客户端页动作并不同形（24046 没有 14046 的
    `select2`/`select3` 页），且 14046 侧有既有的 movie 翻页修复记录（`.agents/summary/quest-14045-14046-movie-page-turn/`），
    需要单独设计；
  - `1000/11000`（4 行）与 `39713/49713`（3 行，FACTION 日任、三名可互换报告 NPC）：两侧页动作链不同形，
    需要先确定“谁是行内 NPC、谁是 owner”。
- 本批 Maven 命令（已执行，99 例全绿；后续批次沿用并追加新门禁类）：
  `mvn -Dtest='CollapsedSingleStepLadderContractTest,MirrorRewardProjectionLagContractTest,QuestPrematureRewardRouteExclusionTest,Quest11110And1548PostKillReportDialogTest,DurableDaevanionWeaponRewardRowContractTest,RewardOwnerTrimContractTest,RewardRowResidualTwoRowContractTest,RewardRowEventTwoRowContractTest,RewardRowTwoRowTalkFamilyContractTest,RewardNpcOwnershipContractTest,RetailSingleStepRewardRowContractTest,LegacyRewardStepProjectionRegressionTest,QuestClientContractGateTest,QuestItemSourceContractGateTest,QuestDefinitionCatalogManifestTest,ProductionCatalogWhitelistVerificationTest' test`

---

## 二十四、批次 20：焦树族 23809 / 13809（三棵 DeadTree + 领奖 owner 收敛，2026-09-22）

### 二十四之一、族级判据与证据

QC 判据（与批次 18/19 同源，但落点不同）：同形镜像对 `q` / `q±10000` 的客户端 `quest_summary` **行数相同**、
末行都指向任务内的 NPC，而**两侧都不满足**“每行一个状态 + owner 唯一”：
23809 被迁移塌陷（`ROW_BEHIND / MISSING_TAIL_ROWS / ROW_WITHOUT_STATE`、`visible=0`、缺行 1 2 3），
13809 阶梯已对却让三棵树同时兼任接取与领奖（QE-052 owner 冗余）。

- **迁移前 legacy handler**（`git show '7e9f0316c^:.../kaldor/_23809Scar_Of_The_Past.java'` 与
  `_13809Tree_Is_Company.java`，**两侧逐行同形**）：`802429`(Vidarr/23809) 与 `802427`(Caetess/13809)
  负责接取（`sendQuestDialog(env, 1011)`）、对话（`START_DIALOG -> 2375`）、领奖
  （`SELECT_REWARD -> changeQuestStep(env, 3, 4, true)`、`REWARD -> sendQuestEndDialog`）；
  `730969`/`730970`/`730971` 三棵树只做 `useQuestObject(env, 0,1 / 1,2 / 2,3, false, 0)`。
  第 5 个参数是 `varNum`（`QuestHandler` 签名 `useQuestObject(env, step, nextStep, reward, varNum, ...)`），
  **不发放物品**；`changeQuestStep(step,nextStep,true)` 在旧 helper 里只 `setStatus(REWARD)`、保留 packed step，
  因此领奖态仍是 packed step 3（QE-045/QE-051 同形）。
- **客户端页动作**（`Dialogs/20000_29999/quest_q23809.html` 与 `Dialogs/10000_19999/quest_q13809.html`，两侧同形）：
  `select1`(accept) / `select2`(1352, `HACTION_SETPRO1`=10000) / `select3`(1693, `HACTION_SETPRO2`=10001) /
  `select4`(2034, `HACTION_SETPRO3`=10002) / `select5`(2375, `HACTION_SELECT_QUEST_REWARD`=1009)；
  `quest_summary` 行 0/1/2 = 依次调查 `DeadTree_a/b/c` 并采集 `quest_<id>a/b/c`，行 3 = 向守卫报告。
- **owner 归属**：`client_npcs_npc.xml` 里 `802429 = LDF5_Fortress_Village_Guard01_D`、
  `802427 = LDF5_Fortress_Village_Guard01_L`，与行 3 的行内字典键完全一致；三棵树是**天/魔共用世界物件**
  （`LDF5_Fortress_FOBJ_B1_DeadTree_a|b|c`，`ai="quest_use_item"`，spawn 在 `600090000_Kaldor.xml`）。
- **物品来源**：`quest_data.xml` 两侧都只声明 `<quest_work_items>`（182215485-487 / 182215493-495），
  **没有** `quest_drop`、也没有 `collect_items`；三棵树 template 虽是 `quest_use_item`，但没有 quest drop，
  所以采集物只能由任务自身发放——这是 13809 侧缺失 `give-item` 的判据（23809 侧原本就有）。

### 二十四之二、改动（两侧同形，脚本 `apply_batch20_tree_ladder_owner_trim.py`）

节点统一为 `unaccepted(0) / started(0) / stage1(1) / stage2(2) / reward(3) / complete(0)`（`var0` 仍 6 位宽）：

- **每棵树两条边**：`USE_OBJECT` 显示本树页（730969→`SELECT2`、730970→`SELECT3`、730971→`SELECT4`），
  `SETPRO1/2/3` 推进一格并按行发放本行采集物（`give-item`）。
  同步模式按 legacy `sendUpdatePacket` 逐字对齐：START 行只发 `SM_QUEST_ACTION`（`PACKET_ONLY`），
  最后一行进入 REWARD 时 legacy 额外 `onLvlUp + updateZone + updateNearbyQuests`
  （即 `LEVEL_AND_VISIBILITY_REFRESH`）。
- **owner 收敛（QE-052）**：`NPC_START` 与 `npc-complete` 都只留领奖 NPC（23809=802429、13809=802427），
  三棵树不再接取/完成；删除 13809 侧 `started --802427 SETPRO1--> stage1` 的重复边
  （它只是为 `select2` 页的 10000 按钮凑路由，掩盖了“树页挂在 NPC 上”的错位）。
- **行 3（领奖行）**：`reward --QUEST_SELECT(领奖 NPC)--> reward` 显示客户端 `select5`；
  `1009` 与领奖窗口继续由 `npc-complete` 的 `preview`（`USE_OBJECT SELECT_QUEST_REWARD`）承接，
  **不再显式声明 SELECT_QUEST_REWARD 自环**（批次 19 的 `AMBIGUOUS_TRANSITION` 教训）。
- **旧存档自愈**：`status=REWARD && var0==0 -> var0=3` 的无 source `enter-world` 边（两侧各一条，与
  `JournalRewardRowRepairContractTest(13809, 3, 0)` 合同一致；塌陷定义把“在三棵树处一次性交付”的玩家写成 `REWARD+var0=0`）。

### 二十四之三、验证（2026-09-22）

- 脚本：`--check` 改动前 `BATCH20_PENDING` 2/2 → APPLY 2/2 → `--check` 2/2 幂等。
- **单任务审计**：23809 `ROW_BEHIND / MISSING_TAIL_ROWS / ROW_WITHOUT_STATE`（`visible=0`、`rows_without_state=1 2 3`）
  → `ROW_ALIGNED / ALIGNED / ROW_STATE_ALIGNED`（`visible=0 1 2 3`、`recovery=True`）；
  13809 保持 `ROW_ALIGNED / ALIGNED / ROW_STATE_ALIGNED`（owner 由 4 条收敛为 1 条）。
- **全库快照**：`ROW_ALIGNED 2658 -> 2659`、`ROW_BEHIND 188 -> 187`、`ROW_STATE_ALIGNED 2430 -> 2431`、
  `ROW_WITHOUT_STATE 519 -> 518`（`ROW_AHEAD 2589`、`STATE_OUT_OF_RANGE 2446`、`BOTH_MISALIGNED 177`、
  `MISSING_LAST_ROW 84`、`MISSING_TAIL_ROWS 79` 不变）——本批属“缺状态阶梯 + owner 冗余”形态。
- **客户端页路由审计**（`QuestDialogOrderAudit` 全库跑，`docs/quest/client-dialog-mapping/*.csv` 为客户端页索引）：
  两侧从 before 的 `select3(1693)/select4(2034)/select5(2375)` 三条 `CLIENT_PAGE_UNREACHED`（每侧 3 条，共 6 条）
  变为 **0 条**：`1352 -> 10000`、`1693 -> 10001`、`2034 -> 10002`、`2375 -> 1009` 全部 `PAGE_ACTION_MATCHED`；
  全库 `CLIENT_PAGE_UNREACHED 1374 -> 1368`、`TERMINAL_PAGE_REACHED 16727 -> 16723`（三棵树的 `npc-complete` preview 行消失）。
- **结构校验**：`xmllint --noout --schema quest_definition.xsd` 2/2 validates；`git diff --check` 干净。
- **IDEA lint / 脚本语法**：新门禁 `TreeLadderOwnerTrimContractTest.java` 经 IDEA 检查**无 error**（仅 2 条 helper 形参恒定值 warning）；`apply_batch20_tree_ladder_owner_trim.py` 语法检查通过并 `--check` 幂等。
- **门禁测试**：`src/test/java/com/aionemu/gameserver/questEngine/definition/TreeLadderOwnerTrimContractTest.java` 7 例——
  ① 每行一个状态（行 0..2 START、行 3 REWARD）且与镜像同形、领奖行落在 `var0` 位域内；
  ② 每棵树只开自己的页（`SELECT2/3/4`）、只推一格、发放本行采集物、`PACKET_ONLY`→`LEVEL_AND_VISIBILITY_REFRESH` 同步；
  ③ `QuestMutationPlanner` 逐行走阶梯（0→1→2→3 且状态 REWARD），**跳行/回看任何一行都无计划**（顺序门禁）；
  ④ 接取与完成 owner 唯一 = 行内 NPC，talk 路由集合恰好 = 三棵树 + 领奖 NPC；
  ⑤ 行 3 显示 `select5`、`1009` 只有 preview 一条路由（无重复自环）、完成计划移除三件 work item；
  ⑥ 不再保留 `started/stage1 -> reward` 的塌陷跳转，reward 只能从行 2（或行 3 自身/自愈边）进入；
  ⑦ 旧存档自愈边唯一且收敛到 3。
- **Maven（授权后执行，2026-09-22 13:21）**：18 个测试类 **109 例全绿**（含本批新增 7 例），
  `PRODUCTION_COMPILE_OK=6189 / FAILURES=0 / INTERACTION_OBJECT_FAILURES=0 / WHITELIST_VIOLATIONS=0`
  （三棵 `quest_use_item` 树在 `QuestInteractionObjectValidator` 下仍满足“显式 TALK 路由或 catalog drop”合同）。
- **证据表**：[batch20-evidence.tsv](batch20-evidence.tsv)。
- 客户端实机复测：**PENDING_CLIENT**。要点：① 802429/802427 接取后任务书停在第 1 行；
  ② 依次使用三棵树（天/魔共用 730969/730970/730971），每棵树的页分别是“烧焦/烧剩/烧成灰烬”文案，
  点“结束调查”后任务书前进一行并拿到对应采集物；
  ③ 三棵树调查完后任务书停在第 4 行（向守卫报告），此时守卫页是“拿出采集到的东西，报告调查结果”，
  点按钮应弹奖励窗口并可领奖；④ 老存档（历史上在三棵树处一次性交付、`REWARD+var0=0`）登录/切图后应落在领奖行；
  ⑤ 三棵树在未接取/非本行时点击应无对话或无推进。

### 二十四之四、后续

- “单步塌陷对”剩余 4 个挂账：`23918/13918`（6 行、`SECTION_0..4` 串行门控 vs 32 节点组合计数模型）、
  `24046/14046`（8 行、两侧页动作不同形 + 既有 movie 翻页修复）、`1000/11000`（4 行）、
  `39713/49713`（3 行、FACTION 日任、三名可互换报告 NPC）。
- 本批还顺带暴露一个**非本批范围**的镜像残留：13809 的 `<items>`（itemRequirements）在 23809 侧不存在，
  两侧 `quest_data.xml` 都只声明 `quest_work_items`；该字段仅参与掉落上限判定（`QuestService`），
  本族无 `quest_drop`，因此无行为差异，留待 metadata 对齐批次处理。
- 本批 Maven 命令（已执行，109 例全绿；后续批次沿用并追加新门禁类）：
  `mvn -Dtest='TreeLadderOwnerTrimContractTest,CollapsedSingleStepLadderContractTest,MirrorRewardProjectionLagContractTest,JournalRewardRowRepairContractTest,QuestPrematureRewardRouteExclusionTest,Quest11110And1548PostKillReportDialogTest,DurableDaevanionWeaponRewardRowContractTest,RewardOwnerTrimContractTest,RewardRowResidualTwoRowContractTest,RewardRowEventTwoRowContractTest,RewardRowTwoRowTalkFamilyContractTest,RewardNpcOwnershipContractTest,RetailSingleStepRewardRowContractTest,LegacyRewardStepProjectionRegressionTest,QuestClientContractGateTest,QuestItemSourceContractGateTest,QuestDefinitionCatalogManifestTest,ProductionCatalogWhitelistVerificationTest' test`


---

## 二十五、批次 21：精锐兵链式计数族 23918 / 13918（SECTION_0..4 串行 0/1 阶梯 + owner 收敛，2026-09-22）

### 二十五之一、族级判据与证据

- 客户端 `quest_q13918.html` / `quest_q23918.html` 的 `quest_summary` **6 行**：行 0..4 依次消灭袭击指挥官的五名特殊精锐兵
  （每行 `([%n]/1)`），行 5 = 和 `STR_DIC_N_LDF4_Advance_Elger_E`(13918) / `STR_DIC_N_LDF4_Advance_Helgund_E`(23918) 对话。
- 权威门控在 `quest_monster.csv`：`SECTION_0<1; SECTION_5==0`、`SECTION_1<1; SECTION_0==1`、`SECTION_2<1; SECTION_1==1`、
  `SECTION_3<1; SECTION_2==1`、`SECTION_4<1; SECTION_3==1` —— 五个 **0/1 计数槽 + 链式串行**：行 n 只在
  `SECTION_n<1` 且 `SECTION_(n-1)==1` 时成立，前一只打掉后下一行才亮。
- 因此 QE-051 的“`var0` = 行号”口径对本族不适用：`audit_reward_row_vs_client_steps.py` 已登记
  `VAR0_FLAG_EXCEPTIONS = {30203, 30303, 13918, 23918}`，本族权威口径是
  `audit_section0_report_row_closure.py` 的 `COUNTER_CHAIN`。
- 领奖行 NPC（客户端 NPC 表解键）：13918 → `802350 LDF4_Advance_Elger_E`（接取 NPC 是 `802328 LDF4_Advance_Village_Guard09_L`）；
  23918 → `802353 LDF4_Advance_Helgund_E`（接取 NPC 是 `802347 LDF4_Advance_Village_Guard09_D`）。

### 二十五之二、旧模型的两侧缺陷

**13918（天族侧）**：

- `<progress>` 只声明 `var0..var3`，**缺 var4**（客户端的 `SECTION_4`），审计报 `COUNTER_CHAIN_GAP`；
  `k1..k5` 把 `var0` 写成 0..5 的“步骤号”，reward 投影 `var0=5` —— 既不是 0/1 计数槽，也没覆盖第 5 只精锐兵。
- 后果：打掉第 2 只精锐兵后 `var0=2`，行 0 要求 `SECTION_0<1`、行 1 要求 `SECTION_0==1`，两者同时不成立，行 1..4 也因前置槽为 0
  全部不成立——中间段不再显示任何一行（只有 `SECTION_5==0` 的报告行保持可见），与用户报的“下一步该和 NPC 对话 / 任务列表不前进”同形。
- `NPC_START` / `NPC_REPORT` / `npc-complete` 全挂在 `802328`（接取守卫），客户端行 5 点名的 `802350` 根本接不到报告/领奖对话（QE-052 owner 错位）。
- `audit_section0_report_row_closure.py` 改前明确报 `235322`-`235325` 未计入链式口径。

**23918（魔族侧）**：

- 定义是 `<counter-grid>` 五维**自由组合**（32 个 `a1b1c1d1e1` 式节点 = 2^5，每维独立 `required=1`）：五只精锐兵可以任意顺序击杀，
  与客户端“前一只打掉后下一行才可见”的串行门控直接冲突（先打第 3 只 → 行 0/1 都不满足条件，任务书错位）。
- owner 同样错挂 `802347`（接取 NPC），行 5 的 `802353` 没有报告/领奖对话。
- `npc-complete` 的 `fixed-reward-indices="0 1"` 把客户端 `select_quest_reward1` 的 **ITEM `169405255`×6** 丢掉
  （只剩 GOLD 451980 / EXP 7927072）。
- 本侧**没有 `<kills>` metadata**。

### 二十五之三、落点（两侧同形重建）

- **五槽计数**：`<progress>` 声明 `var0..var4`，各占 `SECTION_n = 6n` 位（`offset=0/6/12/18/24`，`width=6 min=0 max=63`）；
  **不声明 var5**（客户端 `SECTION_5==0` 是报告行门控）。
  位宽保留 6 bit 而不是 1 bit：旧 step 模型把 `var0` 写成 0..5，只有 6 bit 才能读到并迁移这些存档
  （`VariableIs` 条件会被 `ProgressLayout.pack` 的上界校验用上）；新模型自身只写 0/1（节点投影与自愈动作都由门禁锁死）。
- **9 节点串行阶梯**：`unaccepted(全0) / started(全0) / k1(1,0,0,0,0) / k2(1,1,0,0,0) / k3(1,1,1,0,0) / k4(1,1,1,1,0) /
  k5(全1) / reward(全1) / complete(全0)`；每只精锐兵只把自己那一槽推到 1，同步 `PACKET_ONLY`
  （START 态只发 `SM_QUEST_ACTION`）；23918 的 32 节点组合网格删除，替换成同一 9 节点形态。
- **owner 收敛（QE-052）**：`NPC_START` 留在接取守卫（`802328`/`802347`），`NPC_REPORT` 与 `npc-complete` 都收在领奖行 NPC
  （`802350`/`802353`）；`k5 --QUEST_SELECT--> SELECT2` 打开报告页，`1009` 与领奖窗口继续由 `npc-complete` 的
  `preview`（`USE_OBJECT SELECT_QUEST_REWARD`）承接，**不再显式声明 SELECT_QUEST_REWARD 自环**（批次 19 的
  `AMBIGUOUS_TRANSITION` 教训）。
- **奖励索引**：`fixed-reward-indices="0 1 2"`（GOLD/EXP/ITEM）恢复 `169405255`×6，`complete-reward-index="0"`。
- **kills metadata**：两侧各五条（13918: `235321`-`235325`；23918: `235559`/`235560`/`235561`/`235326`/`235327`，本批补回）。
- **迁移自愈 6 条**：
  - 13918 专有 4 条 `START && variable-is var0==N`（N=2..5）+ `enter-world` → 按前 N 槽补齐并落到 `k2..k5`
    （旧“步骤号”存档无损迁移；**23918 定义里不得出现这类边**，由门禁反向断言）；
  - 两侧各 2 条 `REWARD` 自愈：`variable-at-least var0>=2`（旧步骤投影）与
    `variable-sum-below "var0 var1 var2 var3 var4" < 5`（旧组合网格/领奖投影）→ 补齐全 1，`enter-world` 与
    `TALK_TO_NPC` 各一条（后者 after-commit `SHOW_QUEST_PAGE SELECT2`）。

### 二十五之四、验证（2026-09-22）

- 脚本 `.agents/summary/quest-10527-reward-row/apply_batch21_chain_elite_ladder.py`（`--check` 2/2 幂等 → APPLY 2/2 → `--check` 2/2）。
- 结构校验：`xmllint --noout --schema quest_definition.xsd` 2/2 validates；`git diff --check` 干净。
- **COUNTER_CHAIN 口径**：`audit_section0_report_row_closure.py` 两侧 `COUNTER_CHAIN_OK`（改前 13918 = `COUNTER_CHAIN_GAP`），
  `field_alignment_problems` / `monster_alignment_problems` 空、`reward: var0=1;var1=1;var2=1;var3=1;var4=1`、
  `kills: k1;k2;k3;k4;k5`。
- **行号口径（已登记例外）**：两侧现为 `ROW_BEHIND / MISSING_TAIL_ROWS / ROW_WITHOUT_STATE`（`visible=0 1`、
  `rows_without_state=2 3 4 5`、`recovery=True`）——这是本族按 `VAR0_FLAG_EXCEPTIONS` 登记的**预期**结果，不是回归；
  改前 13918 在行号口径下反而显示 `ROW_ALIGNED`，正说明“行号口径会误放行本族”。
- **全库快照（口径变化）**：`ROW_ALIGNED 2659 -> 2658`、`ROW_BEHIND 187 -> 188`、`ROW_STATE_ALIGNED 2431 -> 2430`、
  `ROW_WITHOUT_STATE 518 -> 519`、`ALIGNED 2431 -> 2430`、`MISSING_TAIL_ROWS 79 -> 80` —— 全部由 13918 从
  “行号口径误判对齐”转为登记例外引起；`MISSING_LAST_ROW 84`、`INTERIOR_GAP 266`、`NO_STATE 89`、
  `STATES_BEYOND_ROWS 2623`、`ROW_AHEAD 2589`、`BOTH_MISALIGNED 177`、`STATES_OUT_OF_RANGE 0` 均不变。
- **客户端页路由审计**（`QuestDialogOrderAudit` 全库跑）：改前报告页只挂在接取 NPC 上；改后两侧报告/领奖页全部落在
  `802350`/`802353`，无 `CLIENT_PAGE_UNREACHED`；全库 `PAGE_ACTION_MATCHED 101205 -> 101206`、
  `TERMINAL_PAGE_REACHED 16723 -> 16724`、`CLIENT_PAGE_UNREACHED 1368` 不变。
- **planner 探针**（`/tmp/b21/ProbeBatch21.java`）：两侧顺序击杀逐槽推进、乱序/回看无计划、`k5` 报告页与领奖窗口可开、
  完成事务消费 5 只怪并发放 GOLD/EXP/ITEM；13918 旧“步骤号”存档与两侧旧 REWARD 投影存档都被自愈边补齐到全 1。
- **门禁测试**：`src/test/java/com/aionemu/gameserver/questEngine/definition/ChainEliteLadderContractTest.java` 7 例——
  ① 五槽 6n 对齐 + 只声明 `var0..var4` + 9 节点投影 + 镜像同形 + `kills` 五只；
  ② 每只精锐兵只推自己那一槽且 `PACKET_ONLY`；③ 乱序/回看/领奖态击杀无计划；
  ④ owner 唯一（接取 ≠ 报告/领奖）、`k5 -> SELECT2`、reward 自带 `LEVEL_AND_VISIBILITY_REFRESH` + 领奖窗口；
  ⑤ 完成发 GOLD/EXP/ITEM×6 并移除 5 只怪；⑥ 13918 `START var0=2..5` 无损迁移、23918 **无**此类边；
  ⑦ REWARD 旧投影（步骤号 / 四维全 0）在 `enter-world` 与 `TALK` 下各 heal 到全 1，正规全 1 不被改写且能开领奖窗完成。
  同批删除 `QuestMonsterProgressContractAuditTest#quest23918ChainsFiveKillerCountersOnTheClientSections`
  （它锁定旧的组合网格模型），原地留注释指向新门禁。
- **Maven（授权后执行，2026-09-22）**：21 个测试类 **149 例全绿**（含本批新增 7 例），
  `PRODUCTION_COMPILE_OK=6189 / FAILURES=0 / INTERACTION_OBJECT_FAILURES=0 / WHITELIST_VIOLATIONS=0`。
- **证据表**：[batch21-evidence.tsv](batch21-evidence.tsv)。
- 客户端实机复测：**PENDING_CLIENT**。要点：① `802328`/`802347` 接取后任务书停在行 0（消灭第一只精锐兵）；
  ② **必须按行 0→4 的顺序击杀**（五只怪各自占 `SECTION_n` 门控），乱序击杀不计数、任务书不动；
  每杀一只任务书下沉一行、下一行亮起；③ 五只打完后任务书停在行 5（和 Elger/Helgund 对话），
  此时 `802350`/`802353` 的报告页是“报告结果。”，点按钮弹奖励窗口，能领到 GOLD + EXP + 道具×6；
  ④ 13918 的旧“步骤号”存档与两侧旧的 `REWARD + 计数未落齐` 存档登录/切图后应落在第 6 行（行 5）；
  ⑤ 五只精锐兵在未接取或非本行时击杀应无计数。

### 二十五之五、边界与后续

- 本族**唯一权威口径**是 `quest_monster.csv` 的 `SECTION_n` 链式门控 + 客户端行数；`audit_reward_row_vs_client_steps.py`
  的行号口径对本族必然给 `ROW_BEHIND`，已登记 `VAR0_FLAG_EXCEPTIONS`，**不得**据此再去“修正” reward 投影。
- 位宽**必须保留 6 bit**：1-bit 位域无法表示旧 step 存档的 `var0=2..5`，`VariableIs` 自愈边会被 `ProgressLayout.pack`
  的上界校验拒绝，旧存档会永久卡在中间行。
- `var5` 不声明（客户端 `SECTION_5==0` 是报告行门控）；将来若要把报告行也纳入计数槽，必须同时改 `quest_monster.csv`
  口径与客户端行可见性，不得只在服务端补位域。
- 剩余“单步塌陷/错位”挂账：`24046/14046`（8 行、两侧页动作不同形 + 既有 movie 翻页修复记录
  `.agents/summary/quest-14045-14046-movie-page-turn/`）、`1000/11000`（4 行）、`39713/49713`（3 行 FACTION 日任、三名可互换报告 NPC）。
- 本批 Maven 命令（已执行，149 例全绿；后续批次沿用并追加新门禁类）：
  `mvn -Dtest='ChainEliteLadderContractTest,QuestMonsterProgressContractAuditTest,TreeLadderOwnerTrimContractTest,CollapsedSingleStepLadderContractTest,MirrorRewardProjectionLagContractTest,JournalRewardRowRepairContractTest,QuestPrematureRewardRouteExclusionTest,Quest11110And1548PostKillReportDialogTest,DurableDaevanionWeaponRewardRowContractTest,RewardOwnerTrimContractTest,RewardRowResidualTwoRowContractTest,RewardRowEventTwoRowContractTest,RewardRowTwoRowTalkFamilyContractTest,RewardNpcOwnershipContractTest,RetailSingleStepRewardRowContractTest,LegacyRewardStepProjectionRegressionTest,QuestClientContractGateTest,QuestDialogOrderAuditTest,QuestItemSourceContractGateTest,QuestDefinitionCatalogManifestTest,ProductionCatalogWhitelistVerificationTest' test`


---

## 二十六、批次 22：副本段行阶梯 24046（The Shadow Calls，2026-09-22）

### 二十六之一、族级判据与证据

- 客户端 `quest_q24046.html` 的 `quest_summary` **8 行**：行 0 到 DF2A 和 `Phyper`(798300) 见面；行 1 查看 Phyper 的预言是否会成为现实；
  行 2 收到了传票！到 `Srudgelmir`(204253) 去看一下；行 3 和竞技场管理员 `Galm`(204089) 进行对话；
  **行 4 进入 `DC1_door_Q2076` 寻找沉默审判官**；**行 5 找到 `IDDC1_Arena_3F_Exit`(700369) 逃出秘密监狱**；
  行 6 和 `Srudgelmir` 进行对话；行 7 向伊斯夏尔肯的 `Muninn`(203550) 报告监狱里发生的事情。
- 本族**没有** `quest_monster` 门控（该 CSV 里 24046 无任何行进），所以权威口径是 QE-051 的**行号口径**（行 n ↔ var0=n），
  不是批次 21 的 `COUNTER_CHAIN`；`var0` 是行号，位域只需容纳末行 7。
- 迁移前 legacy handler（`git show '7e9f0316c^:…/mission/_24046The_Shadow_Calls.java'`）是行阶梯的原始证据：
  `798300` STEP_TO_1 → 1；`BALTASAR_HILL_VILLAGE_220050000` 离区 + `giveQuestItem(182205502)` → 2；
  `204253` STEP_TO_3（移除 182205502）→ 3；**`204089` STEP_TO_4 = `getNextAvailableInstance(320120000)` +
  `changeQuestStep(env, 3, 5, false)`**；`700369` USE_OBJECT（var==5）→ 传送 120010000 + `changeQuestStep(5, 6)`；
  `204253` `defaultCloseDialog(env, 6, 6, true, false)` → REWARD；`onDie`/`onEnterWorld` 在 var==5 时回退 3。
- 行 4 的物件 `DC1_door_Q2076` 实为 **700368**（客户端 NPC 表），但它在 `spawns/Npcs/120010000_Panium.xml` ×
  `portals/portal_template2.xml` 里是**地下竞技场入口 portal（loc_id 3200900）**，不是本任务的审判所副本；
  副本 320120000 里只有出口 `700369`（`spawns/Instances/320120000_Shadow_Court_Dungeon.xml`）。
  因此**保留 legacy 的“与加尔姆对话即传送进副本”**，不把行 4 改成走 700368。

### 二十六之二、旧模型缺陷

- **行 4 没有任何状态**：legacy `changeQuestStep(3, 5)` 把“进副本/找审判官”这一格整体跳过，客户端第 5 行永远不亮
  （审计 `INTERIOR_GAP`、`rows_without_state=4 7`）。
- **领奖行与行 6 共用投影**：reward 节点投影 `var0=6`，而客户端末行是行 7 —— 玩家在领奖态看到的是“和 Srudgelmir 进行对话”，
  且 `var0_max=6` 连行 7 都表达不了（`last_row_within_field_max=False`）。这与用户报障同形（“下一步该找 NPC 对话/任务列表不前进”）。

### 二十六之三、落点

- `var0`：`width=4 min=0 max=6` → **`width=3 min=0 max=7`**（仍是 `SECTION_0` offset 0；行号口径不需要 6-bit 位段）。
- 节点：补 `s4(START, var0=4)`，`reward` 投影 `6 -> 7`；`started(0)/s1(1)…s6(6)/reward(7)` 八行齐备。
- 副本段（每步只推一格）：
  - `s3 --204089 SETPRO4--> s4`：`set var0=4` **并保留** `teleport-player-next-available-instance world-id="320120000"`；
  - `s4 --enter-world(world-is 320120000)--> s5`：`set var0=5`（行 5“在副本里找出口”是可持续的副本内状态）；
  - `s4 --enter-world(world-is 320120000 expected="false")--> s3`：传送未生效/掉线时回退到“找加尔姆再进”，避免卡在无对话的行 4；
  - `s5 --700369 USE_OBJECT--> s6`（传送回 120010000）与 `s5 --die--> s3`、`s5 --enter-world(副本外)--> s3` 原样保留；
  - `s6 --204253 SET_SUCCEED--> reward`（投影给出行 7）。
- 旧存档：补无 source `REWARD && var0==6 -> 7` 的 `enter-world` 自愈边（`LEVEL_AND_VISIBILITY_REFRESH`），
  与天族镜像 14046 的 `Contract(14046, 7, 6)` 同形；领奖/完成 owner 唯一 = `Muninn(203550)`（QE-052）。

### 二十六之四、验证（2026-09-22）

- 脚本 `.agents/summary/quest-10527-reward-row/apply_batch22_shadow_court_row_ladder.py`（`--check` PENDING → APPLY → `--check` 幂等）。
- 结构校验：`xmllint --noout --schema quest_definition.xsd` 1/1 validates；`git diff --check` 干净。
- **单任务审计**：24046 由 `ROW_BEHIND / INTERIOR_GAP / ROW_WITHOUT_STATE`
  （`visible=0 1 2 3 5 6`、`rows_without_state=4 7`、`var0_max=6`、`recovery=False`）
  → `ROW_ALIGNED / ALIGNED / ROW_STATE_ALIGNED`（`visible=0 1 2 3 4 5 6 7`、`var0_max=7`、`recovery=True`）；
  镜像 14046 保持 `ROW_ALIGNED / ALIGNED / ROW_STATE_ALIGNED`（本批未改动）。
- **全库快照**：`ROW_ALIGNED 2658 -> 2659`、`ROW_BEHIND 188 -> 187`、`ALIGNED 2430 -> 2431`、
  `INTERIOR_GAP 266 -> 265`、`ROW_STATE_ALIGNED 2430 -> 2431`、`ROW_WITHOUT_STATE 519 -> 518`；
  `MISSING_LAST_ROW 84`、`MISSING_TAIL_ROWS 80`、`NO_STATE 89`、`STATES_BEYOND_ROWS 2623`、`ROW_AHEAD 2589`、
  `BOTH_MISALIGNED 177`、`STATE_OUT_OF_RANGE 2446` 均不变 —— 全部变化都来自 24046 一个任务。
- **门禁测试**：
  - 新增 `src/test/java/com/aionemu/gameserver/questEngine/definition/ShadowCourtRowLadderContractTest.java`（6 例）：
    ① 两侧 8 行各有 START/REWARD 状态、reward 投影 = 7、`var0` 在 `SECTION_0` 且 `max=7`；
    ② 副本段每步只推一格（3→4 带副本传送、4→5 需身处副本世界、4→3 是副本外回退、5→6 经 700369 逃出并传送回主城、
       6→reward 由 203550 承接），且旧的 3→5 跳格已删除；
    ③ planner 逐行推进 0→7 每步只进一格、同一动作在其他行号（回看/乱序）不产生计划、副本外触发 4→3 回退而副本内不触发；
    ④ `REWARD + var0==6` 的 `enter-world` 自愈边唯一且只改行号，已修复存档不再改写；
    ⑤ 700369 的 `can-act` 只在 `s5`、`s5` 的 die / 副本外 enter-world 回退保留；
    ⑥ 两侧变体不互相污染（天族用道具+影片推进行 4、魔族用副本世界推进；天族不得传送进 320120000）。
  - `JournalRewardRowRepairContractTest` 登记 `Contract(24046, 7, 6)`（reward 投影/自愈边/无陈旧行写入三条断言同时覆盖）。
- **Maven（授权后执行，2026-09-22 14:24）**：9 个测试类 **44 例全绿**（含本批新增 6 例与扩表后的
  `JournalRewardRowRepairContractTest` 3 例），`PRODUCTION_COMPILE_OK=6189 / FAILURES=0 /
  INTERACTION_OBJECT_FAILURES=0 / WHITELIST_VIOLATIONS=0`。
- **提交时复跑（更宽的回归面）**：24 个测试类 **158 例全绿**（上表 9 类 + 批次 15–21 的门禁类，`PRODUCTION_COMPILE_OK=6189 / FAILURES=0 / INTERACTION_OBJECT_FAILURES=0 / WHITELIST_VIOLATIONS=0`）。
- **已知无关红（不在本批范围，未修）**：`MissionItemConsumptionBatchRegressionTest` 断言 `20529 s9 -> reward` 与
  `29064 started -> reward` 两条转换存在，而这两条在 **HEAD（未改动的 XML）里本来就不存在**（`git show HEAD:…` 计数为 0），
  属既有失败，与本批无关；本批的 Maven 选择器因此不含该类。
- **证据表**：[batch22-evidence.tsv](batch22-evidence.tsv)。
- 客户端实机复测：**PENDING_CLIENT**。要点：① 与 Phyper/Srudgelmir/Galm 逐行推进时，任务书依次停在行 0→1→2→3；
  ② 与加尔姆对话结束的瞬间任务书应切到行 4（进副本），进入审判所副本后切到行 5（找 3F 出口）；
  ③ 用出口逃出并传送回主城后停在行 6（和 Srudgelmir 对话），对话结束任务书切到行 7（向 Muninn 报告）；
  ④ 在 Muninn 处应能直接领奖（GOLD/EXP/AP/TITLE/道具），领奖行不再是“和 Srudgelmir 对话”；
  ⑤ 旧存档（`REWARD + var0=6`）登录/切图后任务书应自动落在行 7。

### 二十六之五、边界与后续

- **行 4 是“与加尔姆对话后、进入副本世界前”的那一格**（legacy 自动传送，所以只有极短窗口可见）；副本内可持续的状态是行 5。
  不要把行 4 改成“与 700368 交互”：700368 是 `portal_template2.xml` 里通往 3200900（地下竞技场）的入口，不是本任务副本。
- `s4` 的两条 `enter-world` 边靠 `world-is 320120000` 的 true/false 互斥（编译器 `factConditionsAreMutuallyExclusive` 认这一对），
  新增同类边必须保持条件互斥，否则 `AMBIGUOUS_TRANSITION`。
- 本任务 `var0` 是行号而**不是** `SECTION_n` 计数槽，因此 `width=3/max=7` 足够；不要照搬批次 21 精锐兵族的 6-bit 约束。
- reward 投影从 6 改到 7 属于 QE-046/QE-051 的“投影变更必须配自愈边”形态：跨部署在线的 `REWARD+6` 存档只靠 `enter-world` 自愈，
  若后续要覆盖“在线不重登”的窗口，需要按 QE-046 追加同 owner 的领奖态入口自愈（本批未加，与镜像 14046 保持一致）。
- 剩余“单步塌陷/错位”挂账：`1000/11000`（4 行，1000 侧 `NO_REWARD_ROW`）、`39713/49713`（3 行 FACTION 日任、三名可互换报告 NPC）；
  另有 `COUNTER_CHAIN_GAP` 族（1842-1844、2842-2845、13910、16962、17016、18033、21292/21305、23703、23905-23908/23910/23917、
  24112、24201、28030/28033、28313、28915、30600/30610、39001/39002、49002）待逐族判定。
- 本批 Maven 命令（已执行，44 例全绿；后续批次沿用并追加新门禁类）：
  `mvn -Dtest='ShadowCourtRowLadderContractTest,JournalRewardRowRepairContractTest,Quest14045And14046MoviePageTurnContractTest,CoreCapabilityRepresentativeDefinitionTest,QuestClientContractGateTest,QuestDialogOrderAuditTest,QuestItemSourceContractGateTest,QuestDefinitionCatalogManifestTest,ProductionCatalogWhitelistVerificationTest' test`

---

## 二十七、批次 23：领奖行阶梯 39713（[Daily] Fresh Powder，2026-09-22）

### 二十七之一、族级判据与证据

- 客户端 `quest_q39713.html` 的 `quest_summary` **3 行**（与魔族镜像 49713 同形，只有道具名不同）：
  行 0 和 `[%dic:STR_DIC_E_LDF5a_Greenhat_BA]` 对话；**行 1 把 `[%dic:STR_DIC_I_quest_39713a]` 撒在 `[%dic:STR_DIC_W_LDF5b_SZ_G1_001]` 上**；
  行 2 向 `[%dic:STR_DIC_E_LDF5a_Greenhat_BA]` 报告。
- 两侧客户端页/按钮完全同形：`select2` = `HACTION_SETPRO1`（收下净化粉末）、`select5` = `HACTION_SELECT_QUEST_REWARD`（报告结果）、
  `ask_quest_accept` = `HACTION_FINISH_DIALOG`、`select_quest_reward1` 无 `Act`（奖励页）。
- **owner 有独立文本证据**：末行的 `STR_DIC_E_LDF5a_Greenhat_BA` 是“绿林团南部卡塔拉姆支部”的集合名，
  其正文点名 `STR_DIC_N_LDF5b_Ubarung_Greenhat` / `LDF5b_Dieroonroon_Greenhat` / `LDF5b_Argarung_Greenhat`，
  客户端 NPC 表对应 **800936 / 800937 / 800938**；天族侧沿用这三名成员不是归属错误（本批未收敛 owner）。
- `quest_monster.csv` 两侧同形：`39713,Progress(1),quest_39713a,itemUseArea,,1,usearea_ldf5b_itemusearea_q39713`
  —— 行 1 是**道具使用区域**目标，权威口径是 QE-051 的**行号口径**（行 n ↔ var0=n），不是批次 21 的 `COUNTER_CHAIN`（没有 `SECTION_0` 判定）。
- `var0` 是行号，`SECTION_0` 仍是 offset 0；位域只需容纳末行 2（本批与镜像对齐到 `width=2/max=3`）。
- 迁移证据：`527dc4017^` 的生成物 `Quest39713.java` 已把当时的 XML 形态固化（`var0` 1 bit、节点只有 `unaccepted/started/reward/complete`、
  9 条 `started -> reward`），与迁移前 legacy 的“无阶梯直跳”一致；本批按客户端行清单重建（同族 49713 在更早批次已重建，可作模板）。

### 二十七之二、旧模型缺陷

- **行 1、行 2 没有任何状态**：旧定义把整条阶梯塌陷成 9 条无守卫的 `started -> reward` 直跳
  （`npc-item-report` ×3 + `SET_SUCCEED` ×3 + `SELECT_QUEST_REWARD` ×3），客户端第 2、3 行在整个流程里都高亮不起来
  （审计 `MISSING_TAIL_ROWS`、`rows_without_state=1 2`、`visible=0`）。
- **行 0 打开了错误的页**：`started --QUEST_SELECT--> started` 显示 `SELECT5`（“你从遗忘沼泽回来了吗”报告页），
  而领粉末页是 `SELECT2`（“我把净化粉末给你”）—— 玩家在行 0 点任务行会看到报告页、却在同一 NPC 上用 `SETPRO1` 发粉。
- **领奖行错位（QE-051）**：reward 节点投影 `var0=0`，而客户端末行是 2；同时 `var0` 只有 1 bit（`max=1`），连行 2 都表达不了
  （`last_row_within_field_max=False`）。
- **报告路由重复索取道具**：`npc-item-report item-id="182215285" required="1"` 要求报告时仍持有净化粉末，
  但该粉末在行 1 就被 `use-item` 消耗掉了（镜像 49713 的报告路由不要求任何物品）。

### 二十七之三、落点

- `var0`：`width=1 min=0 max=1` → **`width=2 min=0 max=3`**（与 49713 同形；offset 0 不变）。
- 节点：`unaccepted(0)/started(0)`、补 **`powder-received(1)`** 与 **`powder-used(2)`**（均 `START`）、`reward` 投影 `0 -> 2`、`complete(0)`。
- 阶梯（每位支部成员各一套，与 49713 同形；各自的物品 id 独立：天族 182215285 / 魔族 182215277）：
  - `started --QUEST_SELECT--> started`：改开 `SELECT2`（领取粉末页）；
  - `started --SETPRO1--> powder-received`：`give-item 182215285` + `set var0=1`，`PACKET_ONLY` + `close-dialog`；
  - `powder-received --use-item 182215285--> powder-used`：条件 `var0==1` + `has-item`，动作 `remove-item` + `set var0=2`，`PACKET_ONLY`；
  - `powder-used --QUEST_SELECT--> powder-used`：显示 `SELECT5`（报告页）；
  - `powder-used --SELECT_QUEST_REWARD--> reward`：条件 `var0==2`，`LEVEL_AND_VISIBILITY_REFRESH` + `SHOW_SELECT_QUEST_REWARD_WINDOW1`；
  - 删除 9 条 `started -> reward` 直跳与 3 条 `npc-item-report`（报告不再要求道具）。
- 旧存档：补无 source 的 `REWARD && var0==0 -> reward(2)` `enter-world` 自愈边（`LEVEL_AND_VISIBILITY_REFRESH`），
  与 `JournalRewardRowRepairContractTest` 的 `Contract(39713, 2, 0)` 同形。
- `npc-complete`（reward 预览 + `reward -> complete` ×3）与 completion owner（800936/800937/800938）保持不动（QE-052 不适用本批）。
- 接取口径保持族内既有差异：天族经支部 NPC 对话接取（`TALK_TO_NPC`），魔族走无目标 `QUEST_ACTION` —— 本批不动接取，只改行阶梯。

### 二十七之四、验证（2026-09-22）

- 脚本 `.agents/summary/quest-10527-reward-row/apply_batch23_faction_daily_rows.py`（`--check` PENDING → APPLY → `--check` 幂等）。
- 结构校验：`xmllint --noout --schema quest_definition.xsd` 1/1 validates；`git diff --check` 干净。
- **单任务审计**：39713 由 `ROW_BEHIND / MISSING_TAIL_ROWS / ROW_WITHOUT_STATE`
  （`visible=0`、`rows_without_state=1 2`、`var0_max=1`、`reward_var0=0`、9 条 `started->reward` 直跳、`recovery=False`）
  → `ROW_ALIGNED / ALIGNED / ROW_STATE_ALIGNED`（`visible=0 1 2`、`var0_max=3`、`reward_var0=2`、`recovery=True`）；
  镜像 49713 保持 `ROW_ALIGNED / ALIGNED / ROW_STATE_ALIGNED`（本批未改动）。
- **全库快照**（before → after，同一脚本两次全库运行）：`ROW_ALIGNED 2659 -> 2660`、`ROW_BEHIND 187 -> 186`、
  `ALIGNED 2431 -> 2432`、`MISSING_TAIL_ROWS 80 -> 79`、`ROW_STATE_ALIGNED 2431 -> 2432`、`ROW_WITHOUT_STATE 518 -> 517`；
  `MISSING_LAST_ROW 84`、`NO_STATE 89`、`INTERIOR_GAP 265`、`STATES_BEYOND_ROWS 2623`、`ROW_AHEAD 2589`、`BOTH_MISALIGNED 177`、
  `STATE_OUT_OF_RANGE 2446`、`NO_CLIENT_HTML 650` 均不变 —— 全部变化都来自 39713 一个任务
  （`audit-output.tsv` 仅 39713 一行变化，`audit-missing-last-row.tsv` / `audit-qe051-candidates.tsv` 逐字节不变）。
- **门禁测试**：
  - 新增 `src/test/java/com/aionemu/gameserver/questEngine/definition/FactionDailyRowLadderContractTest.java`（7 例）：
    ① 两侧 3 行各有 START/REWARD 状态、reward 投影 = 2、`var0` 在 `SECTION_0` 且与镜像同形（2 bit / max 3）、可见行 = 0/1/2；
    ② 行 1 由三名支部成员任一位发放本侧粉末并推进一步、行 0 的 `QUEST_SELECT` 打开 `select2`；
    ③ `use-item` 消耗粉末并推进到行 2、条件含 `var0==1` 与持有道具、行 2 的报告页自环给 `select5`；
    ④ 无 `started -> reward` 直跳、领奖路由只挂行 2 且条件 `var0==2`、领奖/完成/预览 owner 保持三名成员；
    ⑤ planner 逐格推进（0→1→2→REWARD）、同一动作在其他行号或无道具时不产生计划；
    ⑥ 39713 的 `REWARD + var0==0` 自愈边唯一且只改行号、已修复存档不再改写，且 49713 不得长出多余自愈边；
    ⑦ 两侧物品互不污染（39713 只碰 182215285、49713 只碰 182215277）、接取口径各自独立。
  - `JournalRewardRowRepairContractTest` 登记 `Contract(39713, 2, 0)`（reward 投影/自愈边/无陈旧行写入三条断言同时覆盖）。
- **Maven（授权后执行，2026-09-22 14:44）**：27 个测试类 **174 例全绿**（含本批新增 7 例、`Quest49713RetailFlowAlignmentTest`
  同族镜像锁与扩表后的 `JournalRewardRowRepairContractTest` 3 例），`PRODUCTION_COMPILE_OK=6189 / FAILURES=0 /
  INTERACTION_OBJECT_FAILURES=0 / WHITELIST_VIOLATIONS=0`。
- **已知无关红（不在本批范围，未修）**：`MissionItemConsumptionBatchRegressionTest` 断言 `20529 s9 -> reward` 与
  `29064 started -> reward` 两条转换存在，而这两条在 **HEAD（未改动的 XML）里本来就不存在**，属既有失败；本批的 Maven 选择器不含该类。
- **证据表**：[batch23-evidence.tsv](batch23-evidence.tsv)。
- 客户端实机复测：**PENDING_CLIENT**。要点：① 接任务后任务书停在行 0（和支部对话）；② 与任一名支部成员对话并点“收下净化粉末”后
  任务书切到行 1（把粉末撒在污染根源上）且背包拿到 182215285；③ 在污染区域使用粉末后任务书切到行 2（向支部报告）且粉末被消耗；
  ④ 再与任一名成员对话应看到报告页并可直接领奖（领奖行不再停在行 0）；⑤ 旧存档（`REWARD + var0=0`）登录/切图后任务书应自动落在行 2。

### 二十七之五、边界与后续

- owner **不由本批收敛**：三名支部成员（800936/800937/800938）既是报告人也是领奖人，是客户端集合名文本的明确点名；
  同族 49713 同形（不必、也不要照 QE-052 收敛成单点）。
- 镜像 49713 **不得**被套用本批的自愈边：它本就把领奖态写在行 2，多出来的 `REWARD/var0=0 -> 2` 边会与 `RewardRowEventTwoRow`
  一类断言冲突（本批门禁显式锁这一点）。
- 接取路径两侧不同形（天族 `TALK_TO_NPC`、魔族 `QUEST_ACTION`）是既有实现，本批未改；若要统一必须先单独取证客户端接取按钮链。
- 剩余“单步塌陷/错位”挂账：`1000/11000`（4 行，1000 侧 `NO_REWARD_ROW`、客户端文件名大写 `QUEST_Q1000.html`、`quest_monster` 无条目）；
  另有 `COUNTER_CHAIN_GAP` 族（1842-1844、2842-2845、13910、16962、17016、18033、21292/21305、23703、23905-23908/23910/23917、
  24112、24201、28030/28033、28313、28915、30600/30610、39001/39002、49002）待逐族判定。
- 本批 Maven 命令（已执行，全绿；后续批次沿用并追加新门禁类）：
  `mvn -Dtest='FactionDailyRowLadderContractTest,Quest49713RetailFlowAlignmentTest,QuestWorldReachabilityOracleTest,ShadowCourtRowLadderContractTest,JournalRewardRowRepairContractTest,Quest14045And14046MoviePageTurnContractTest,CoreCapabilityRepresentativeDefinitionTest,QuestClientContractGateTest,QuestDialogOrderAuditTest,QuestItemSourceContractGateTest,QuestDefinitionCatalogManifestTest,ProductionCatalogWhitelistVerificationTest,ChainEliteLadderContractTest,QuestMonsterProgressContractAuditTest,TreeLadderOwnerTrimContractTest,CollapsedSingleStepLadderContractTest,MirrorRewardProjectionLagContractTest,QuestPrematureRewardRouteExclusionTest,Quest11110And1548PostKillReportDialogTest,DurableDaevanionWeaponRewardRowContractTest,RewardOwnerTrimContractTest,RewardRowResidualTwoRowContractTest,RewardRowEventTwoRowContractTest,RewardRowTwoRowTalkFamilyContractTest,RewardNpcOwnershipContractTest,RetailSingleStepRewardRowContractTest,LegacyRewardStepProjectionRegressionTest' test`

---

## 二十八、批次 24：空槽位族登记与序幕边界（1000/2000/1400/11000，2026-09-22）

### 二十八之一、族级判据与证据

- 挂账 `1000/11000` 从批次 20 起被列为“剩余单步塌陷/错位”，本批取证后判定**两者都不是 QE-051 缺陷**：
  - `11000`（幻想之光军团工作态度调查，ELYOS min-level 50）是正常 4 行任务，定义早已对齐（`ROW_ALIGNED/ALIGNED/ROW_STATE_ALIGNED`），
    只是与 1000 数字上相差 10000；两者同属天族、**不是镜像对**（1000 是序幕）。
  - `1000`（Prologue，ELYOS min-level 1）的客户端 `quest_summary` 固定渲染 4 个 `<step>`，但每一步的可见文本都是空白：
    `<step><p visible="[%0]"><font color="[%1]"> </font></p>[%collectitem]</step>` —— 行 0 只挂 `[%collectitem]` 占位符，
    而 `Quest_unpacked/quest.xml` 里 1000 既没有 `collect_item`、也没有任何 start/end NPC（`reward_exp1=1`、`cannot_giveup=1`）。
  - 服务端实现（迁移前 `527dc4017^` 的 `Quest1000.java` 与当前 XML 同形）是
    `AKARIOS_PLAINS_210010000` enter-zone 接取 → `play-movie 1` → `movie-end` 直接 `complete`，**没有 REWARD 节点、没有领奖行**。
  - 因此审计的行号口径（`NO_REWARD_ROW / MISSING_TAIL_ROWS / ROW_WITHOUT_STATE`、`rows_without_state=1 2 3`、`visible=0`）
    是**误报**：这些空槽不是“永不亮的任务书行”，按行号补 `s1/s2/s3` 只会造出永远不显示的节点。
- 全库只读扫描（新增脚本 `.agents/summary/quest-10527-reward-row/audit_blank_journal_slots.py`，覆盖 9116 个客户端任务书）
  找到 **13 个空槽位任务**，明细见 `blank-journal-slots.tsv`：

  | 分类 | 任务 | 形态 | 服务端现状 |
  |---|---|---|---|
  | ALL_BLANK | 1000、2000 | 4 个空槽 + 行 0 的 `[%collectitem]` | 序幕：enter-zone → movie → complete（无 REWARD） |
  | ALL_BLANK | 16984、26984 | 4 个空槽 | 服务端 XML 存在但**没有 `<nodes>`**（属“无状态”族） |
  | ALL_BLANK | 3959、4963、18706、18744、20015、28706、28744、29706 | 4~8 个空槽 | **服务端没有定义文件**（属“缺定义”族） |
  | TRAILING_BLANK | 1400 | 行 0 = 击杀计数、行 1 空 | `var0/var1` 是 8×4 击杀计数组合（35 节点），reward = 计数饱和值 |

- `2000` 与 `1000` 完全同形（魔族序幕：`ALDELLE_BASIN_220010000` → `play-movie 2` → `complete`）。
- `1400`（Paion's Worry，ELYOS min-level 34）：客户端 `QUEST_Q1400.html` 只有 1 个可见行“除掉作恶的特洛尔和托尔金 (/7)”，
  行 1 是空槽；服务端是 32 个 `aNbM` 计数组合 + `unaccepted/reward/complete`，领奖路由 `a7b3 --NPC_REPORT 203941--> reward`（page SELECT2），
  reward 投影 `var0=7/var1=3` 是计数饱和值 —— 行号口径的 `ROW_AHEAD / STATES_BEYOND_ROWS / STATE_OUT_OF_RANGE` 同属误报。

### 二十八之二、落点（不改任何任务定义）

- 审计脚本登记 `BLANK_JOURNAL_SLOT_EXCEPTIONS`（13 个 id，双语注释说明“判定不变、只登记证据”），
  与既有 `VAR0_FLAG_EXCEPTIONS` / `DUPLICATE_VISIBLE_SLOT_BLANK_ROWS` 同一形式。
- 新增只读扫描脚本 `audit_blank_journal_slots.py`（`--out-csv` 生成 `blank-journal-slots.tsv`），
  以后新增客户端任务书可按同一口径复查空槽位族。
- 新增门禁 `src/test/java/com/aionemu/gameserver/questEngine/definition/BlankJournalSlotBoundaryContractTest.java`（4 例）：
  ① 1000/2000 无 REWARD 节点、无任何 `target=reward` 路由；② 序幕触发器保持（enter-zone 接取 + 重播 + `movie-end` 完成，影片 1/2）；
  ③ 序幕不得长出 NPC 对话或 `s1/s2/s3` 行节点、`var0` 保持 6-bit 打包槽；④ 1400 保持 35 节点计数组合、`reward var0=7/var1=3`、领奖路由在 `a7b3`、owner 203941。
- **本批没有任何 XML 改动**（0 个任务定义被修改），因此全库审计计数与批次 23 后完全一致。

### 二十八之三、验证（2026-09-22）

- 扫描：`audit_blank_journal_slots.py` 覆盖 9116 个任务书 → `ALL_BLANK=12 / TRAILING_BLANK=1`；
  交叉 `audit-output.tsv`：1000/2000/16984/26984 判 `NO_REWARD_ROW`、1400 判 `ROW_AHEAD`，8 个缺定义任务不参与（无 XML）。
- 审计脚本改动后重跑全库：三份 TSV 与批次 23 提交逐字节一致（`git status` 对三份 TSV 无差异）。
- **Maven（授权后执行，2026-09-22 14:51）**：7 个测试类 **32 例全绿**（含新增 `BlankJournalSlotBoundaryContractTest` 4 例、
  批次 23 的 `FactionDailyRowLadderContractTest` 7 例与 `Quest49713RetailFlowAlignmentTest` 6 例），
  `PRODUCTION_COMPILE_OK=6189 / FAILURES=0 / INTERACTION_OBJECT_FAILURES=0 / WHITELIST_VIOLATIONS=0`。
- **证据表**：[batch24-evidence.tsv](batch24-evidence.tsv)、[blank-journal-slots.tsv](blank-journal-slots.tsv)。

### 二十八之四、边界与后续

- 序幕/空槽位任务**不得**按 QE-051 行号口径补阶梯：任务书上没有行，补出来的节点永远不会显示；
  同类任务先跑 `audit_blank_journal_slots.py`，空槽必须用客户端 `quest.xml`（collect_item/NPC/reward 字段）交叉核对。
- `1400` 的 `var0/var1` 是击杀计数组合，`reward` 投影是饱和值；若将来要按 QE-051 口径核对它，
  必须先有“任务书可点亮行”的客户端证据（当前只有 1 行可见文本）。
- **新挂账（属独立族，本批未修）**：
  - `NO_NODES`：16984、26984（客户端有任务书、服务端 XML 无 `<nodes>`）—— 需要先取客户端目标链再建状态；
  - `MISSING_DEFINITION`：3959、4963、18706、18744、20015、28706、28744、29706（客户端有任务书、服务端无 XML）—— 属定义缺口族。
- 批次 20 起挂账的“剩余单步塌陷/错位”清单至此只剩 `COUNTER_CHAIN_GAP` 族
  （1842-1844、2842-2845、13910、16962、17016、18033、21292/21305、23703、23905-23908/23910/23917、24112、24201、
  28030/28033、28313、28915、30600/30610、39001/39002、49002）。

---

## 二十九、批次 25：COUNTER_CHAIN 三槽族与 2842 饱和领奖投影（18033/28033/28313/2842，2026-09-22）

### 二十九之一、族级判据与证据

- `audit_section0_report_row_closure.py` 的 `COUNTER_CHAIN_GAP` 在批次 24 后仍有 13 个。逐族取证后分三类：
  1. **口径例外 6 个**：`1842/1843/1844/2843/2844/2845` —— 客户端 `quest_monster.csv` 不是链式 0/1 槽，而是
     `Progress(SECTION_0<80; SECTION_5==0)` + `Progress(SECTION_1<1; SECTION_5==0)`（80 只普通怪 + 1 只将军）：
     `var0` 需要 **7 bit** 才装得下 80，`var1` 只能落在 bit 7，本审计“`SECTION_n == 6n`”的对齐断言对这种值域不成立。
     该族早已由 `ClientQuestSectionAlignmentTest.EXTENDED_COUNTER_QUESTS` 显式锁定（`var0` width=7 / `var1` offset=7），
     本批只在 section0 审计里登记 `EXTENDED_COUNTER_EXCEPTIONS`，判为 `COUNTER_CHAIN_EXTENDED_EXCEPTION`（**不是** GAP、不改任务数据）。
  2. **真正的三槽族 3 个**：`18033/28033/28313`（本批修复）。
  3. **剩余 GAP 3 个**：`24112 / 30600 / 30610`（批次 26 处理）。
- 三槽族的客户端证据（`quest_monster.csv` 三条链式记录 + `quest_summary` 2 行）：

  | 任务 | 行 0 计数（SECTION_0/1/2 三条 0/1 记录） | 行 1 报告 NPC |
  |---|---|---|
  | 18033 [Alliance] Retreat Into Death（ELYOS，前置 18036） | `idf5_td_nor_fi_n_65_ae`=230744 / `idf5_td_nor_kn_n_65_ae`=230745 / `idf5_td_nor_ra_n_65_ae`=230749 | `LDF5b_Demades_E` = **801281** |
  | 28033 [Alliance] Confusing the Chain of Command（ASMODIAN，前置 28036） | 同上三只 | `LDF5b_Latkel_E` = **801280** |
  | 28313 A Wealthy Patron（ASMODIAN） | `IDStation_Hugen_NM_58An` 系列 217371/246131/248077、`IDStation_ShulackFlight_NM_58_An` 系列 217373/246132/248078、`IDStation_DrakanNinja_NM_58An` 系列 217376/246133/248079 | `DF5_Nineveh_E` = 804821（同时是接取 NPC） |

- `2842`（The Zephyr Island Treasure Chamber，ASMODIAN，repeat）与其天族镜像 `1841` 的客户端门控是
  `Progress(SECTION_0<39; SECTION_5==0)` —— **单行狩猎计数**，`var0` 是 0..39 的击杀数而不是行号；
  两侧 `quest_summary` 都是 2 行（行 0 = `([%2]/39)`，行 1 = 和 Herz/Sakmis 对话），
  镜像 `1841` 的 reward 投影早已是饱和值 **39**。

### 二十九之二、旧模型缺陷

- `18033` / `28033`：三条链式记录被压成一个 `<counter-grid>`（单维 `var0` `required=1`，任意一只怪即可满足），
  `SECTION_1/SECTION_2` 永远为 0 → 行 0 里第 2、3 个计数永远显示 `0/1`、任务书行永不沉下（QE-053 症状）；
  报告与领奖还挂在**行 0 之前**的接取 NPC 上（18033: 801037 `LDF5_Village_Guard11_L`/Stifas；28033: 801047 `LDF5_Village_Guard11_D`/Tobald），
  28033 更并存 801047 的旧 `NPC_REPORT` 与重复的 `k1 -> reward` 边（QE-052）。
- `28313`：旧定义是“步骤号”单槽（`var0` = 已完成组数 0..3，节点 `started/k1/k2/k3` 投影 0/1/2/3、reward=3），
  `SECTION_1/SECTION_2` 永远为 0；击杀路线只按 217371/217373/217376 三个基础变体登记，另 6 个难度变体不计数。
- `2842`：`hunting --kill--> hunting`（`variable-below var0=38` 累加）与 `hunting --kill--> reward`
  （`variable-at-least var0=38` 后 `+1`，最后一只写到 **39**）是正确路线，但 `reward` 节点投影仍是 **0**；
  `QuestMutationPlanner#matchesSourceNode` 要求 source 节点投影与存档逐字段全等，于是 **REWARD/var0=39 的领奖态存档匹配不到任何 reward 路由**，
  玩家在领奖阶段卡死（`npc-complete` 的 `source="reward"` 永不满足）。

### 二十九之三、落点

- 三槽族统一重建为 `var0/var1/var2` @ offset **0/6/12**（各 6 bit，保留旧存档可读宽度），节点
  `unaccepted(0,0,0) / started(0,0,0) / k1(1,0,0) / k2(1,1,0) / k3(1,1,1) / reward(1,1,1) / complete(0,0,0)`，
  每只（组）怪只推自己那一槽、`PACKET_ONLY` 同步、乱序与回看击杀无计划；报告路由 `k3 --NPC_REPORT--> reward`（page SELECT2）
  与 `npc-complete`（`fixed-reward-indices="0 1 2"`、`preview actions="USE_OBJECT SELECT_QUEST_REWARD"`）落在客户端行 1 的 NPC 上。
- `18033`：报告/领奖 owner 801037 → **801281**；`28033`：801047 → **801280**，并删除 801047 的旧报告边与重复边。
- `28313`：**精确锚点**只替换三条击杀 transition（补 6 个难度变体）与 `<progress>/<nodes>`，
  **保留 27 条按职业展开的 `reward -> complete` 分支**（`GLADIATOR`×6、`TEMPLAR/RANGER/ASSASSIN`×3、`SORCERER/CLERIC/CHANTER/GUNSLINGER`×2、
  `SPIRIT_MASTER`×2、`SONGWEAVER/AETHERTECH`×1，覆盖 `SELECTED_QUEST_REWARD1..6`）与全部 `grant-reward`/`complete-quest`；
  owner 804821 同时是接取与报告 NPC（客户端同形，不收敛）。
- 旧存档自愈（无 source 的 `enter-world` 边，`LEVEL_AND_VISIBILITY_REFRESH`）：
  - `18033/28033` 的旧 0/1 网格（START 节点只有 `var0=0/1`、领奖投影只写 `var0=1`）：条件用
    `variable-sum-below fields="var0 var1 var2" value="3"`，只补齐未饱和的旧存档，**不重放正规领奖态**；
  - `28313` 的旧步骤号：START `var0=2 -> k2(1,1,0)`、START `var0=3 -> k3(1,1,1)`、REWARD `var0=3 -> reward(1,1,1)`（条件 `variable-is var0=3`，与新版不可能混淆）。
- `2842`：`reward` 节点投影 `var0=0 -> 39`（双语注释说明饱和值与 planner 语义），与镜像 1841 一致；本批**不改**击杀路线、节点集与 owner。
- 应用脚本：`.agents/summary/quest-10527-reward-row/apply_batch25_counter_chain_triplets.py`（`--check` 幂等，`BATCH25_APPLIED` → 再跑 `BATCH25_OK ... already-applied`）。
- 审计口径登记：
  - `audit_section0_report_row_closure.py` 新增 `EXTENDED_COUNTER_EXCEPTIONS = {1842,1843,1844,2843,2844,2845}` →
    `COUNTER_CHAIN_EXTENDED_EXCEPTION`（6 个）；
  - `audit_reward_row_vs_client_steps.py` 新增 `COUNTER_SATURATED_REWARD_ROWS = {1841,2842}`（2842 的行号口径 `ROW_AHEAD/STATES_BEYOND_ROWS` 属误报，
    权威口径是 section0 的 `COUNTER_CHAIN_OK`）。

### 二十九之四、验证（2026-09-22）

- **静态**：`xmllint --noout --schema quest_definition.xsd` 4 个任务全 `validates`；`git diff --check` 干净。
- **section0 审计**：`COUNTER_CHAIN_GAP` **13 → 3**（余 24112/30600/30610）；
  `COUNTER_CHAIN_OK 816 → 820`（18033/28033/28313 + 2842 侧稳定）；新增 `COUNTER_CHAIN_EXTENDED_EXCEPTION 6`；
  `ROW_INDEX_CLOSED` 680 与 `SAME_CLASS_CONFIRMED 7` 不变。
- **全库行号审计**（`audit_reward_row_vs_client_steps.py`，header 对比 HEAD 的 `audit-output.tsv`，变化任务恰好 4 个）：
  - `28313`：`ROW_AHEAD/STATES_BEYOND_ROWS/STATE_OUT_OF_RANGE` → **`ROW_ALIGNED/ALIGNED/ROW_STATE_ALIGNED`**（visible 0..3 → 0..1）；
  - `18033/28033`：`handovers k1->reward[]` → `k3->reward[]`、`recovery False → True`、末行 NPC 命中 → True（`ROW_ALIGNED` 保持不变）；
  - `2842`：`reward_var0 0 → 39`（行号口径 `MISSING_LAST_ROW` → `STATES_BEYOND_ROWS`，属登记误报）；
  - 全库计数：`ROW_ALIGNED 2660→2661`、`ROW_BEHIND 186→185`、`MISSING_LAST_ROW 84→83`、`ROW_STATE_ALIGNED 2432→2433`、
    `ROW_WITHOUT_STATE 517→516`、`STATE_OUT_OF_RANGE 2446→2445`、`BOTH_MISALIGNED 177→178`（=2842 换桶），
    `MISSING_TAIL_ROWS 79 / INTERIOR_GAP 265 / NO_STATE 89 / STATES_BEYOND_ROWS 2623 / ROW_AHEAD 2589 / NO_CLIENT_HTML 650` 不变。
- **Maven（授权后执行，2026-09-22 15:15）**：30 个测试类 **193 例全绿**，含新增
  `CounterChainTripletContractTest` 8 例与批次 24 的 `BlankJournalSlotBoundaryContractTest` 4 例；
  `PRODUCTION_COMPILE_OK=6189 / FAILURES=0 / INTERACTION_OBJECT_FAILURES=0 / WHITELIST_VIOLATIONS=0`。
  （已知无关红：`MissionItemConsumptionBatchRegressionTest` 的断言在 HEAD 本就不成立，本批不运行、不修。）
- **证据表**：[batch25-evidence.tsv](batch25-evidence.tsv)。
- **客户端实机：PENDING_CLIENT**（静态、Maven 与真机验收分层，未做真机复测）。

### 二十九之五、边界与后续

- 本卡只适用于客户端 `quest_monster` 明确写成**三条链式 0/1 记录**的族；`SECTION_0<80` 这类大值域计数器必须按
  `EXTENDED_COUNTER_EXCEPTIONS` 走 7-bit 口径，禁止把 80 压进 6 bit 或把 `var1` 挪回 bit 6。
- 28313 的 27 条职业奖励分支是**客户端兑奖证据**，任何后续重构都不得用 `fixed-reward-indices` 覆盖/删除。
- 自愈边条件必须排除已饱和的正规领奖态（`variable-sum-below` 或与新版不可能混淆的旧值），否则每次进入世界都会重放刷新。
- **下一批（批次 26）**：剩余 `COUNTER_CHAIN_GAP` 三个 —— `24112`（`SECTION_0<1` + 3 个 html step + reward 投影 0 + legacy
  `_24112NoLaissezfaireforLepharists`）、`30600`/`30610`（`SECTION_0<1; SECTION_1<1` + 4 个 html step + reward 投影 2 + legacy handler），
  需先解钩怪名 `lehparaschd_15_an` 与 `iddreadgion_03_drakanfinamedaa_60_ae`/`iddreadgion_03_drakanwi_boss_ah` 并核对镜像与领奖 NPC 归属。

## 三十、批次 26：简报标志位 + Named/Boss 双层计数（24112/30600/30610，2026-09-22）

### 三十之一、族级判据与证据

批次 25 后 section0 审计只剩 3 个 `COUNTER_CHAIN_GAP`：`24112`、`30600`、`30610`。三者同属
**`SECTION_5` 简报标志位 + 链式 0/1 计数** 族（客户端 `quest_monster` 的计数行都带 `SECTION_5==0` 门控）：

- `24112`（No Laissez-faire for Lepharists，ASMODIAN，min-level 14，IMPORTANT，Algard）：
  单条链式 `Progress(SECTION_0<1; SECTION_5==0)`，怪名 `lehparaschd_15_an` → 服务端 **210510**
  （`LehparAsChD_15_An`）；`quest_summary` 3 行 = 去 DF1A 入口处见见 Brodir / 消灭头目 ([%11]/1) / 告诉 Brodir。
- `30600` / `30610`（[Group] Fight Of The Navigators / The Good News, And Bad，min-level 56）：
  两条链式记录 `Progress(SECTION_0<1; SECTION_5==0)` → `iddreadgion_03_drakanfinamedaa_60_ae` = **219256**
  （另一变体 219257，Named 指挥官）、`Progress(SECTION_1<1; SECTION_0==1)` → `iddreadgion_03_drakanwi_boss_ah` = **219264**
  （舰长）；`quest_summary` 4 行 = 和 Linocus/Aluna 对话 / 击杀 Named ([%11]/1) / 击杀舰长 ([%14]/1) / 向 Hejitor/Astella 报告。
- legacy handler 直接给出 `SECTION_5` 的语义：`_24112NoLaissezfaireforLepharists` 在 `ACCEPT_QUEST_SIMPLE` 里
  `setQuestVarById(5, 1)`、与 Brodir 的 `STEP_TO_1` 里 `setQuestVarById(5, 0)`、击杀 210510 时
  `setQuestVarById(0, 1)`、`SELECT_REWARD` 置 REWARD（var0=1、var5=0）。
- 任务书 NPC 解键（服务端 `npc_template` + 静态 spawn）：`Linocus=800324`、`Hejitor=800325`、`Aluna=800326`、
  `Astella=800327`（`spawns/Npcs/210070000_Cygnea.xml`、`220080000_Enshar.xml`）；旧 owner `205842`(Ancanus)/
  `205864`(Udvi) 在静态 spawn 与实例/AI 代码里都**没有任何出场点**。

### 三十之二、旧模型缺陷

- `24112`：旧定义只有 `var0`（无 `var5` 槽），`reward` 投影是 **var0=0**，而击杀已把 var0 推到 1 ——
  领奖态存档既不匹配 `reward` 节点也不匹配任何路线（`QuestMutationPlanner#matchesSourceNode` 逐字段全等），
  玩家在 Brodir 处**卡死**；行 0/行 2 也没有对应状态。
- `30600`/`30610`：旧定义只有一个步骤号 `var0`（0/1/2），**`var1` 缺失**（客户端行 2 的计数永远是 0/1），
  还有两条**无守卫**的 `started --SETPRO1--> reward` 直跳（接取后即可领奖），领奖 owner 落在无出场点的
  205842/205864 上。
- **门禁暴露的第三类缺陷（本批定案）**：把简报对话直接挂在 `QUEST_SELECT(31)` 上会触发
  `QuestClientContractGateTest` 的 `BUTTON_WITHOUT_ROUTE` —— 客户端 `select2` 页（page **1352**）只暴露
  **一个**可见按钮 `HACTION_SETPRO1(10000)`（"结束对话"），`QUEST_SELECT` 只负责打开页面。族级正确形状是
  两段式：`started --QUEST_SELECT--> started + SHOW_QUEST_PAGE SELECT2`（保持标志位），
  `started --SETPRO1--> briefed`（清 `var5`、`LEVEL_AND_VISIBILITY_REFRESH`、`close-dialog`）。

### 三十之三、落点

- `24112`：`var0 @ 0`（6 bit）+ `var5 @ 30`（2 bit，max 1）；节点
  `unaccepted(0,0) / started(0,1) / briefed(0,0) / killed(1,0) / reward(1,0) / complete(0,0)`；
  接取 Nokir 203631、行 0 两段式对话 + Brodir 832821、行 1 击杀 210510 只推 `var0`、行 2
  `killed --NPC_REPORT--> reward`（page SELECT5）与 `npc-complete`（`fixed-reward-indices="0 1 2"`）都在 Brodir；
  自愈边：START `var0=1 && var5=1 -> killed`、REWARD `var0<1 -> (1,0)`。
- `30600`/`30610`：`var0 @ 0` + `var1 @ 6` + `var5 @ 30`；节点
  `unaccepted(0,0,0) / started(0,0,1) / briefed(0,0,0) / k1(1,0,0) / k2(1,1,0) / reward(1,1,0) / complete(0,0,0)`；
  接取/报告 `800325`(30600)/`800327`(30610)，简报 `800324`/`800326`；行 1 的 219256/219257 任一 1 只只推 `var0`，
  行 2 的 219264 只推 `var1`（客户端门控是 `SECTION_1<1` 且 `SECTION_0==1`）；
  `k2 --NPC_REPORT--> reward`（page SELECT5）+ `npc-complete`（`fixed-reward-indices="0 1 2 3"`）；
  自愈边：START `var0=2 -> k2`、REWARD `var0=2 -> (1,1)`。
- 应用脚本：`.agents/summary/quest-10527-reward-row/apply_batch26_briefing_and_named_ladders.py`
  （`--check` 幂等，`BATCH26_APPLIED` → 再跑 `BATCH26_OK ... already-applied`）。
- 审计口径登记：`audit_reward_row_vs_client_steps.py` 新增
  `VAR0_FLAG_EXCEPTIONS = {30203, 30303, 13918, 23918, 24112, 30600, 30610}`（var0/var1 是逐行计数器，
  权威口径是 section0 的 `COUNTER_CHAIN_OK`）。

### 三十之四、验证（2026-09-22）

- **静态**：`xmllint --noout --schema quest_definition.xsd` 3 个任务全 `validates`；旧 owner 205842/205864 与
  `started -> reward` 直跳计数都是 **0**；`SETPRO1` 各只有 1 条（简报推进）。
- **section0 审计**（`--npcs-root` 用客户端解包目录）：`COUNTER_CHAIN_GAP 3 -> 0`；
  `residual rows: 837 -> {'COUNTER_CHAIN_EXTENDED_EXCEPTION': 6, 'COUNTER_CHAIN_OK': 823, 'REVIEW_LEGACY_NO_VAR0': 1, 'SAME_CLASS_CONFIRMED': 7}`；
  三个任务由 `COUNTER_CHAIN_GAP` 全部转为 `COUNTER_CHAIN_OK`（24112 `reward_projection var0=1;var5=0`、
  30600/30610 `var0=1;var1=1;var5=0`，`kill_route_targets` = `killed` / `k1;k2`，`migration_repair=True`）。
- **全库行号审计**（`audit_reward_row_vs_client_steps.py`，对比 HEAD 的 `audit-output.tsv`，变化任务恰好 3 个）：
  - `24112`：`MISSING_TAIL_ROWS -> MISSING_LAST_ROW`、`handovers started->reward[] -> killed->reward[]`、`recovery False -> True`、`visible_state_var0 0 -> 0 1`；
  - `30600`/`30610`：`INTERIOR_GAP -> MISSING_TAIL_ROWS`、`handovers started->reward[dialog]x4 -> k2->reward[]`、`recovery False -> True`、`visible_state_var0 2 -> 0 1`；
  - 全库换桶：`MISSING_LAST_ROW 83 -> 84`、`MISSING_TAIL_ROWS 79 -> 80`、`INTERIOR_GAP 265 -> 263`（三任务换桶，均已登记 `VAR0_FLAG_EXCEPTIONS`），
    领奖行与行↔状态桶不变。
- **Maven（授权后执行，2026-09-22）**：31 个测试类 **199 例全绿**（批次 25 的 30 类 193 例 + 本批新增
  `CounterChainBriefingStageContractTest` 6 例）；`PRODUCTION_COMPILE_OK=6189 / FAILURES=0 /
  INTERACTION_OBJECT_FAILURES=0 / WHITELIST_VIOLATIONS=0`。含 `QuestClientContractGateTest`（修复 `BUTTON_WITHOUT_ROUTE` 后转绿）
  与 `QuestDialogOrderAuditTest` 17 例。
  （已知无关红：`MissionItemConsumptionBatchRegressionTest` 的断言在 HEAD 本就不成立，本批不运行、不修。）
- **证据表**：[batch26-evidence.tsv](batch26-evidence.tsv)。
- **客户端实机：PENDING_CLIENT**（静态、Maven 与真机验收分层，未做真机复测）。

### 三十之五、边界与后续

- 本族判据是客户端 `quest_monster` 的计数行**显式带 `SECTION_5==0`**，且 legacy handler 在接取/简报处读写第 5 号变量；
  没有这条门控的 `SECTION_0` 任务不得套用"简报标志位"。
- `select2` 页（1352）只允许 `SETPRO1(10000)` 一种可见动作；把推进逻辑挂在 `QUEST_SELECT` 会立刻被
  `QuestClientContractGateTest` 判 `BUTTON_WITHOUT_ROUTE`，后续同类任务必须先读
  `docs/quest/client-dialog-mapping/quest-dialog-action-details.csv` 再决定动作拆分。
- owner 收敛依据是**客户端任务书行 NPC + 静态 spawn**；205842/205864 这类只在 legacy 里出现的 owner
  不得因为迁移省事而保留。
- **批次 26 后 `COUNTER_CHAIN_GAP` 归零**。剩余全库挂账：`NO_NODES`（16984、26984）、
  `MISSING_DEFINITION`（3959、4963、18706、18744、20015、28706、28744、29706）与行号口径的
  `MISSING_LAST_ROW 84 / ROW_BEHIND 185 / BOTH_MISALIGNED 178 / ROW_WITHOUT_STATE 516` 等，按族继续收口。

## 三十一、批次 27：legacy 落盘 step 口径与住宅回收箱领奖行（18805/28805，2026-09-22）

### 三十一之一、族级判据与证据

批次 26 之后全库还有 84 个 `MISSING_LAST_ROW`。本批先把“行号口径”与“legacy 落盘 step”对齐，得到一条可复用的权威判据
（QE-054）：**客户端任务书行由 `SM_QUEST_ACTION` 下发的 step（= 节点打包后的 var0）驱动，因此 reward 投影必须等于
legacy 进入 REWARD 时真正落盘的 step，而不是一律等于客户端末行索引。** legacy 的两类 API 语义不同：

- `useQuestItem(env, item, old, new, true)`：**会写 nextStep**（`10527` 的 `useQuestItem(env, item, 14, 15, true)` → step=15，
  与客户端 16 行的末行索引 15 一致；`10100/20100` 的 `useQuestItem(env, item, 4, 4, true)` → step=4）；
- `changeQuestStep(env, old, new, true)`：**只置 REWARD、不写 nextStep**（`15300/25300` 的
  `changeQuestStep(env, 13, 14, true)` → step 停在 13；提交 `075464ebd` 的说明 + 2026-09-19/20 用户真机全程验收）。

据此对 84 个 `MISSING_LAST_ROW` 做首轮自动分类（从 legacy handler 直接提取 reward step 写入）：

| 结果 | 数量 | 处置 |
|---|---|---|
| `LEGACY_REWARD_STEP_MATCH`（XML 投影 = legacy 落盘 step） | 11 | 行号口径误报，登记例外（本批登记 4 个代表：15300/25300/10100/20100） |
| `LEGACY_REWARD_STEP_MISMATCH`（XML 投影 ≠ legacy 落盘 step） | 3 | 真缺陷候选（16800/18805/28805，见下） |
| 无可提取的 reward 写入（其它 handler 形态） | 61 | 待逐族取证 |
| 无 legacy handler | 9 | 待逐族取证 |

真缺陷候选逐一核对后，本批收口其中同形的两个：**18805/28805**（住宅回收箱任务）。`16800/26800`
（archives_of_eternity：legacy 停在 step 2，16800 投影写成 1、26800 投影写成 3，两者客户端都是 3 行）
留待下一批，因为需要同时重建 0/1/2 三段阶梯与两个 zone 触发点。

### 三十一之二、旧模型缺陷（18805/28805）

- 客户端 `quest_summary` 3 行：行 0 = 和旧货商主人对话、行 1 = 阅读回收箱说明、行 2 = 和旧货商主人对话（领奖行）；
  任务书用通用名 `STR_DIC_N_Shugo_housing_rec`（天）/`STR_DIC_N_Shugo_housing_drec`（魔），客户端字符串正文点名
  gomirrun/risarrinrin/gomurrun（天）与 regirron/rogirron/daserinrin（魔）一组旧货商主人。
- legacy `_18805Going_Thrifting`/`_28805SomethingOld_SomethingNew`：回收箱（730522/730525）的
  `STEP_TO_2 -> defaultCloseDialog(env, 1, 2)` 把 step 推到 2；回到旧货商主人（830660/830661、830662/830663 共用分支）
  的 `SELECT_REWARD` 才 `changeQuestStep(env, 2, 2, true)` —— 领奖态 step = 2（客户端末行索引 2）。
- 旧 XML：`reward` 投影写成 **1**，且 `s1 -> reward` 交接又显式 `set-variable var0=1`；玩家读完回收箱进入领奖态后，
  客户端任务书停在行 1（“阅读回收箱说明”），与 10527 的报障同型。

### 三十一之三、落点（18805/28805）

- `reward` 节点投影 `var0 1 -> 2`；
- 删除 `s1 -> reward` 交接里的 `set-variable var0=1`（改由 target 投影生效，与同族已对齐的 18809 的
  `stage1 -> reward` 形态一致）；
- 新增无 source 的 `ENTER_WORLD` 恢复边：`REWARD && var0=1 -> reward`（`set var0=2` +
  `LEVEL_AND_VISIBILITY_REFRESH`），正规态 `var0=2` 不再被重放；
- owner 保持任务书点名的旧货商主人（830520 / 830521）——客户端 `STR_DIC` 正文点名的是“这一组旧货商主人”，
  830520/830521（gomurrun / regirung）与 legacy 的 830660/830661、830662/830663 同组；是否要把 legacy 分支
  的两个成员也补成多 owner 需另行取证（已记为遗留观察，本批不改）；
- 应用脚本：`.agents/summary/quest-10527-reward-row/apply_batch27_housing_reward_row.py`（`--check` 幂等）。

### 三十一之四、口径例外登记（QE-054）

- `15300/25300`（Taking Arms / A Bloody Battle with Beritra）：legacy `changeQuestStep(env, 13, 14, true)` →
  step 停 13，reward 投影 13 是权威值，且已由 2026-09-19/20 真机验收（含领奖）与
  `Quest15300And25300RewardProjectionTest` 锁定；行号口径（末行索引 14）属误报。
- `10100/20100`（Kahrun Intrigue / Ghost Of A Bygone Age）：legacy `useQuestItem(env, item, 4, 4, true)` → step=4，
  reward 投影 4 与 legacy 一致，由 `Quest10100And20100ItemUseRemovalTest`（道具消耗）锁定；同族 10101/10110 的投影
  等于末行索引，说明**同一系列里两种口径并存**，必须逐任务看 legacy，禁止按行号批量“修正”。
- 本批在证据表按 `LEGACY_STEP_EXCEPTION` 登记这 4 个任务（`batch27-evidence.tsv`），并在模式卡
  QE-054 记下判据；审计脚本的例外集登记待下一批与 `/tmp` 批量分类脚本一起补。

### 三十一之五、验证（2026-09-22）

- **静态**：`xmllint --noout --schema quest_definition.xsd` 2/2 validates；`apply_batch27_*.py --check` 幂等
  （`BATCH27_APPLIED` → `BATCH27_OK ... already-applied`）。
- **全库行号审计**：18805/28805 由 `MISSING_LAST_ROW / ROW_BEHIND / ROW_WITHOUT_STATE / recovery=False` 转为
  **`ALIGNED / ROW_ALIGNED / ROW_STATE_ALIGNED / recovery=True`**；全库 `MISSING_LAST_ROW 84 -> 82`、
  `ROW_ALIGNED 2661 -> 2663`、`ROW_STATE_ALIGNED 2433 -> 2435`、`ROW_WITHOUT_STATE 516 -> 514`（其余桶不变）。
- **section0 审计**：`residual rows 837` 与批次 26 完全一致（`COUNTER_CHAIN_OK 823` / 例外 6 / 闭环 7 + 1），
  本批不涉及 COUNTER_CHAIN 族。
- **Maven（授权后执行，2026-09-22）**：32 个测试类 **204 例全绿**（批次 26 的 31 类 199 例 + 本批新增
  `HousingRecycleRewardRowContractTest` 5 例）；`PRODUCTION_COMPILE_OK=6189 / FAILURES=0 /
  INTERACTION_OBJECT_FAILURES=0 / WHITELIST_VIOLATIONS=0`。
  （已知无关红：`MissionItemConsumptionBatchRegressionTest` 的断言在 HEAD 本就不成立，本批不运行、不修。）
- **证据表**：[batch27-evidence.tsv](batch27-evidence.tsv)（2 个修复 + 4 个口径例外）。
- **客户端实机：PENDING_CLIENT**（静态、Maven 与真机验收分层，未做真机复测）。

### 三十一之六、边界与后续

- 行号口径（QE-051）是**发现工具、不是权威**：任何 `MISSING_LAST_ROW` 在改 reward 投影前，必须先按
  `changeQuestStep(..., true)` / `useQuestItem(..., old, new, true)` 的语义提取 legacy 落盘 step；两者一致时
  只能登记例外，不一致才是真缺陷。
- 机械把 reward 投影推到末行索引会破坏已验收行为（15300/25300 真机 + 10100/20100 道具消耗门禁）——禁止批量替换。
- **下一批（批次 28）候选**：
  - `16800/26800`（真缺陷，legacy step=2；16800 投影 1、26800 投影 3，客户端 3 行；需补 0/1/2 阶梯与
    `LF_TOWER_SENSORY_AREA_Q16800_210110000` / `IDETERNITY_01_Q16800_301540000` 两个 zone 触发点与 931 movie）；
  - 继续对剩余 70 个 `MISSING_LAST_ROW`（61 个其它 handler 形态 + 9 个无 legacy）做同口径分类，把
    `LEGACY_STEP_EXCEPTION` 集补进审计脚本的登记表。

## 三十二、批次 28：永恒档案馆领奖阶（16800/26800，2026-09-22 用户授权后执行）

### 三十二之一、候选与证据（批次 27 首轮分类里剩下的真缺陷）

批次 27 用 QE-054 口径对 84 个 `MISSING_LAST_ROW` 做过首轮自动分类，真缺陷候选有 3 个：
`18805/28805`（同批收口）与 **`16800/26800`**（留待本批）。两个任务都来自
`archives_of_eternity`，客户端任务书都是 3 行，legacy 落盘 step 都是 2，而迁移后的 XML 分别写成
`16800 = 1`、`26800 = 3`。

客户端证据（`quest_q16800.html` / `quest_q26800.html` 的 `quest_summary`）：

| 任务 | 行 0 | 行 1 | 行 2（领奖行） |
| --- | --- | --- | --- |
| 16800（ELYOS） | 使用激活的塔碎片进入 `STR_DIC_W_LF_Tower` | 和永恒之塔警备组长 `STR_DIC_N_LF_Tower_Etezar_E`（806232）对话 | 向在 `STR_DIC_W_IDEternity_01` 等待的代理人维达（806148）报告 |
| 26800（ASMODIAN） | 使用激活的塔碎片进入 `STR_DIC_W_DF_Tower` | 和永恒之塔警备组长 `STR_DIC_N_DF_Tower_Enfitenta_E`（806233）对话 | 向在 `STR_DIC_W_IDEternity_01` 等待的代理人佩莱格兰（806149）报告 |

两个客户端的任务书 HTML 都只有 `select_none`(4762) / `select_none_1`(4763) / `select2`(1352, 按钮
`HACTION_SET_SUCCEED`) / `select_success`(10002, 按钮 `HACTION_SELECT_QUEST_REWARD`) 四个任务页，
与 legacy 的接取页链（4762/4763）和警备组长页（1352/1353）一一对应。

legacy 证据（迁移前 commit `7e9f0316c^`）：

- `_16800Into_The_Archives` / `_26800A_Call_For_Champions` 的 `onEnterZoneEvent`：
  `LF_TOWER_SENSORY_AREA_Q16800_210110000` / `DF_TOWER_SENSORY_AREA_Q26800_220120000` 上
  `changeQuestStep(env, 0, 1, false)`（写 1）；`IDETERNITY_01_Q16800_301540000` 上
  `playQuestMovie(env, 931/932)` + `changeQuestStep(env, 2, 3, true)`；
- 警备组长 806232 / 806233 的 `SET_REWARD` 走 `changeQuestStep(env, 1, 2, false)`（写 2）；
- 领奖/完成 owner 是代理人 806148（`IDEternity_Q_Weatha_E`）/ 806149（`IDEternity_Q_Feregran_E`），
  即任务书行 2 点名的 NPC；
- 迁移前 `QuestHandler.changeQuestStep(env, step, nextStep, reward)` 的 **reward 分支只
  `setStatus(REWARD)`、不写 nextStep**（该 commit 的 `questEngine/handlers/QuestHandler.java` 第 99-113 行），
  所以 `changeQuestStep(env, 2, 3, true)` 的落盘 step 是 **2 = 客户端末行索引**，与 QE-051 的行号口径一致。

### 三十二之二、旧模型缺陷

- **16800**：`var0` 只有 1 bit（`width=1 max=1`）、另开 `var1` 影片旗标；节点只有
  `unaccepted(0)/started(0)/reward(var0=1)/complete(0)`，没有 s1/s2 阶梯；行推进用
  `at-distance npc-id="206535"` + `var1` 自环代替 legacy 的感应区（`LF_TOWER_SENSORY_AREA_Q16800_210110000`
  这条 zone 在 `zones_quest.xml:426` 已声明但 XML 从未引用）；`806075/806148/806232` 三个 NPC 在
  `started` 态各有 `SELECT_QUEST_REWARD`（显式写 `var0=1`）与 `SET_SUCCEED` 直跳 `reward` 的捷径
  （共 6 条），其中两个 NPC 还挂着 `npc-complete`。玩家只要在领奖态，任务书就停在行 0/行 1
  （行 2 永远不亮），与 10527 的报障同型。
- **26800**：阶梯与 zone 触发已经在，但 `reward` 投影被迁移读成 `nextStep=3`，`s2 -> reward` 交接又显式
  回写 `set-variable var0=3`。客户端只有 0..2 三行，领奖态行索引越界（审计判 `STATES_BEYOND_ROWS /
  ROW_AHEAD / STATE_OUT_OF_RANGE`）。
- **门禁把误读固化**：`Quest26800ClientDialogAlignmentTest` 此前锁的就是迁移误读
  （`assertNode(..., "reward", REWARD, 3)` 与 `SetVariable("var0", 3)`），本批按 QE-054 改为 2 并在测试里写明依据。

### 三十二之三、落点（两侧同形 `unaccepted(0)/started(0)/s1(1)/s2(2)/reward(2)/complete(0)`）

1. **16800**
   - `var0` 改 `width=2 min=0 max=3`，删除 offset 1 的 `var1` 影片旗标；
   - 补 `s1(1)` / `s2(2)` 节点，`reward` 投影 `var0 = 2`；
   - `started --enter-zone LF_TOWER_SENSORY_AREA_Q16800_210110000--> s1`（条件 `var0=0`、动作 `var0=1`、
     `PACKET_ONLY`）；
   - `s1` 上补 806232 的 `QUEST_SELECT -> SELECT2`、`SELECT2_1 -> SELECT2_1` 页与
     `SET_SUCCEED -> s2`（条件 `var0=1`、动作 `var0=2`、`PACKET_ONLY` + `close-dialog`）；
   - `s2 --enter-zone IDETERNITY_01_Q16800_301540000--> reward`（条件 `var0=2`、无写回、
     `LEVEL_AND_VISIBILITY_REFRESH` + `play-movie 931`）；
   - 删除 806075/806148/806232 在 `started` 态的 6 条直跳捷径与 806075/806232 的 `npc-complete`；
     领奖 owner 收敛到任务书行 2 点名的 806148（`reward --QUEST_SELECT--> DEFAULT_SUCCESS(10002)` +
     `npc-complete` 领奖窗口）；
   - 无 source 的 `ENTER_WORLD` 自愈边：`REWARD && var0=1 -> 2`（旧投影留下的 1 号存档）。
2. **26800**
   - `reward` 投影 `3 -> 2`；
   - 删除 `s2 -> reward` 交接里的 `set-variable var0=3`（改由 target 投影生效）；
   - 无 source 的 `ENTER_WORLD` 自愈边：`REWARD && var0=3 -> 2`（旧投影留下的 3 号存档）。
3. 应用脚本：`.agents/summary/quest-10527-reward-row/apply_batch28_archives_reward_row.py`（`--check` 幂等，
   带旧 XML 指纹校验；16800 的 `at-distance 206535` / `var1` 位段 / 两个多余 `npc-complete` 缺一即报
   `BATCH28_16800_FINGERPRINT_MISSING`）。
4. 逐行进度只有在行阶梯完整时才对得上：`visible_state_var0 = 0 1 2`（三行各有 START/REWARD 状态），
   领奖态 `ROW_ALIGNED`。

### 三十二之四、口径例外登记补录（批次 27 挂账）

把批次 27 证据表里的 4 个 `LEGACY_STEP_EXCEPTION`（15300/25300 的 `changeQuestStep(env, 13, 14, true)`
落盘 13；10100/20100 的 `useQuestItem(env, item, 4, 4, true)` 落盘 4）按双语注释补进
`audit_reward_row_vs_client_steps.py`，并在审计输出的 `[5]` 段落直接打印已登记的例外清单；
**只登记证据、判定不变**：重跑后所有桶的分布与本批修复前完全一致（见三十二之五）。

### 三十二之五、验证（2026-09-22）

- **静态**：`xmllint --noout --schema quest_definition.xsd` 2/2 validates；
  `apply_batch28_archives_reward_row.py --check` 幂等（`BATCH28_APPLIED` → `BATCH28_OK ... already-applied`）；
  IDE lint 0 error（仅 3 条“形参取值恒定”的既有风格提示）。
- **全库行号审计**（`audit_reward_row_vs_client_steps.py`）：16800 由 `MISSING_LAST_ROW / ROW_BEHIND /
  ROW_WITHOUT_STATE / recovery=False`、26800 由 `STATES_BEYOND_ROWS / ROW_AHEAD / STATE_OUT_OF_RANGE /
  recovery=False` 双双转为 **`ALIGNED / ROW_ALIGNED / ROW_STATE_ALIGNED / recovery=True`**
  （`visible_state_var0=0 1 2`、`handover_writes` 清空）；全库
  `MISSING_LAST_ROW 82 -> 81`、`ROW_ALIGNED 2663 -> 2665`、`ROW_STATE_ALIGNED 2435 -> 2437`、
  `ROW_WITHOUT_STATE 514 -> 513`、`STATES_BEYOND_ROWS 2623 -> 2622`、`ROW_AHEAD 2589 -> 2588`、
  `ROW_BEHIND 183 -> 182`；`INTERIOR_GAP 263`、`MISSING_TAIL_ROWS 80`、`NO_STATE 89` 不变。
- **section0 审计**：`residual rows 837` 与批次 26/27 完全一致（`COUNTER_CHAIN_OK 823` / 例外 6 /
  闭环 7 + `REVIEW_LEGACY_NO_VAR0 1`），本批不涉及 COUNTER_CHAIN 族。
- **legacy 落盘 step 扫描**（`audit_legacy_reward_entry_steps.py`）：重跑后 16800/26800 由
  `ROW_BEHIND/ROW_WITHOUT_STATE` 与 `ROW_AHEAD/STATE_OUT_OF_RANGE` 转为 `ROW_ALIGNED/ROW_STATE_ALIGNED`；
  同一文件同时刷新了批次 17 挂账后累积的另外 6 行（14201、15000、18805、23809、24046、28805，属批次
  19/20/22/27 的既有修复），共 8 行变化。
- **Maven（授权后执行）**：27 个测试类 **146 例全绿**，含本批新增
  `ArchivesRewardStepLadderContractTest`（7 例）与同步更新的 `Quest26800ClientDialogAlignmentTest`、
  `ClientQuestSectionAlignmentTest`（16800 退役 `var1` 后从 `SECTION_LAYOUT_DEBT` 名单移出，13 个剩余例外）；
  `PRODUCTION_COMPILE_OK=6189 / FAILURES=0 / INTERACTION_OBJECT_FAILURES=0 / WHITELIST_VIOLATIONS=0`。
  （已知无关红：`MissionItemConsumptionBatchRegressionTest` 的断言在 HEAD 本就不成立，本批不运行、不修。）
- **证据表**：[batch28-evidence.tsv](batch28-evidence.tsv)（2 个修复）。
- **客户端实机：PENDING_CLIENT**（静态、Maven 与真机验收分层，未做真机复测）。

客户端复测路径（PENDING_CLIENT）：

1. 16800：从代理人维达接取后，任务书应逐行推进——行 0“使用激活的塔碎片进入 LF_Tower”在进入
   `LF_TOWER_SENSORY_AREA_Q16800_210110000` 后切到行 1；与 Etezar 806232 对话（`select2` 页只有一个
   `SET_SUCCEED` 按钮）后切到行 2“向代理人维达报告”；进入知识书库 `IDETERNITY_01_Q16800_301540000`
   播放影片 931 并进入领奖态，任务书停在行 2；与 806148 对话领奖后完成。
2. 26800：同形路径（`DF_TOWER_SENSORY_AREA_Q26800_220120000` → Enfitenta 806233 → 知识书库影片 932 →
   佩莱格兰 806149 领奖）。
3. 旧存档：16800 的 `REWARD/var0=1` 与 26800 的 `REWARD/var0=3` 在登录/切图时应自愈为 2，任务书显示
   领奖行（行 2），且不重复提示“任务更新”。

### 三十二之六、边界与后续

- QE-054 的“旧投影值”是**逐侧**的：16800 旧投影 1、26800 旧投影 3，正确值都是 2，自愈边必须按各自旧值写，
  不能两侧共用一条；只在旧值上做等值判断，正规态 `var0=2` 不会被重放。
- 修投影时必须同时重建行阶梯：删掉 `started -> reward` 之类的直跳捷径与多余 owner，否则玩家仍能绕过
  legacy 的 0/1/2 步链，任务书行号会再次与状态脱节（本批 16800 的 6 条捷径就是这类残留）。
- 行阶梯的推进点必须来自 legacy 事件（感应区 zone / 对话 action），`<play-movie>` 与 REWARD 迁移留在同一
  条 `s2 -> reward` 边，保持 legacy 的“先置 REWARD 再播影片”顺序。
- **下一批（批次 29）候选**：剩余 80 个 `MISSING_LAST_ROW`（61 个其它 handler 形态 + 9 个无 legacy +
  口径例外）按 QE-054 同口径逐族分类；全库挂账 `NO_NODES 16984/26984`、
  `MISSING_DEFINITION 3959/4963/18706/18744/20015/28706/28744/29706`。

## 三十三、批次 29：剩余 MISSING_LAST_ROW 逐族收口（2026-09-22 用户授权后执行）

### 三十三之一、族级判据与证据

批次 28 之后全库还有 **81 个 `MISSING_LAST_ROW`**。本批把 QE-054 口径推广到全部 81 个任务：对每个任务从
迁移前的 legacy handler（`7e9f0316c^` 的 `src/main/java/com/aionemu/gameserver/quest/handlers/**`，解包在
`/tmp/b25/legacy`）提取“进入 REWARD 时真正落盘的 `var0`”，再与当前 XML 的 reward 投影逐一比对。完整明细见
[batch29-triage.tsv](batch29-triage.tsv)（81 行，含 `legacy_persisted` 与逐任务依据）。

legacy 落盘规则的复述（与批次 27/28 同一份代码依据，`QuestHandler`）：

- `changeQuestStep(env, step, nextStep, reward=true)` —— **只 `setStatus(REWARD)`，不写 nextStep**，所以落盘值恒等于
  调用点的 `step`（即进入领奖时已有的 `var0`）；`defaultCloseDialog` / `checkQuestItems` /
  `checkItemExistence` / `useQuestItem` / `useQuestObject` / `defaultFollowEndEvent` / `checkQuestItemsSimple`
  的 reward 分支最终都走这一条；
- `qs.setQuestVar(N)` / `qs.setQuestVarById(0, N)` / `changeQuestStep(..., reward=false)` 之后的
  `setStatus(REWARD)` 才把 `var0` 写成 N；
- `defaultOnKillEvent(env, npcId, var, true)` 的 `var` 是**匹配值**（`var0 == var` 时只置 REWARD）。

按此把 81 个任务分成四族：

| 族 | 数量 | 判定 | 处置 |
|---|---|---|---|
| 族 A：legacy 落盘 ≠ XML 投影 | 6 | 真缺陷 | **本批修复**（1876/2876/14123/2600/11010/1466） |
| 族 B：legacy 落盘 = XML 投影，但 < 末行索引 | 62 | 行号口径误报 | 登记 `LEGACY_STEP_EXCEPTION`（判定不变） |
| 族 C：var0 不是任务书行号（计数器 / 标志位 / 分支领奖行） | 6 | 口径不适用 | 登记 `COUNTER_SLOT_EXCEPTIONS`（判定不变） |
| 族 D：legacy 无 handler 也无脚本 | 7 | 缺依据 | 登记 `NO_LEGACY_HANDLER_OBSERVED`（判定不变） |

族 A 逐一取证（`legacy_persisted` 列）与修复落点见 [batch29-evidence.tsv](batch29-evidence.tsv)。要点：

- **1876/2876**（Taranis / Votan Emergency Orders）：客户端 3 行（0 = 和 Sakmis/Lisya 对话、1 = 和
  Ascalon/Semotor 对话、2 = 向 Taranis/Votan 报告）。legacy 在 278503/278017 的 `SET_REWARD` 上先
  `changeQuestStep(env, 1, 2, false)` **写 2**、再 `setStatus(REWARD)`，领奖态落盘 2；旧 XML 投影写成 1。
  本批投影 1→2 + 自愈边 `REWARD && var0=1 -> 2`。
- **14123**（The Shadow Of Vengeance）：客户端 3 行（槽位 0/9/24，行 1/2 不是 3×行号）。legacy
  `defaultOnKillEvent(env, 206360, 0, 1)` 写 1，`SELECT_REWARD` 只 setStatus，落盘 1；旧 XML 的 4 条
  `report -> reward` 交接又显式写 `set-variable var0=0` 把领奖态打回行 0。本批投影 0→1、4 处交接
  `var0 0 -> 1`、自愈边 `REWARD && var0=0 -> 1`；末行索引 2 属行号口径误报（同批登记 `LEGACY_STEP_EXCEPTION`）。
- **2600**（Humongous Malek）：legacy 只在 `var0 == 1` 时响应 204734 的 `SELECT_REWARD` 并 setStatus，落盘 1；
  旧 XML 投影写成 0。本批投影 0→1 + 自愈边 `0 -> 1`（末行索引 2 同步登记为例外）。
- **11010**（Angel To The Wounded）：客户端 4 行。legacy 730323 的 `defaultCloseDialog(env, 2, 3)` 写 3，
  随后 799071 的 `SELECT_REWARD` 只 setStatus，落盘 3；旧 XML 只有 `started(0)/stage1(1)/stage2(2)` 且 reward
  投影写成 0。本批投影 0→3（`stage2 -> reward` 由 target 投影生效）+ 自愈边 `0 -> 3`。
- **1466**（Respect For Deltras）：客户端只有 2 行（0 = 燃放奥德爆竹、1 = 向 Valerius 报告）。legacy 两条进入
  REWARD 的路径分别落盘 0（道具 `setStatus(REWARD)`）与 2（203903 的 `qs.setQuestVar(2)`，**越出 0..1**，任务书
  会空白）。本批统一落盘 1：reward 节点补 `var0=1`、203903 交接 `2 -> 1`、道具交接补 `set-variable var0=1`，
  两条自愈边 `0 -> 1` 与 `2 -> 1`。

族 B 的代表（62 个，全部保留 legacy 落盘值）：`1149`（`defaultFollowEndEvent(1,1,true,12)` 停 1）、
`1920/2945`（`defaultCloseDialog(1,1,true,false)` 停 1）、`2006`（`checkQuestItems(1,1,true,...)` 停 1）、
`2007`（`setQuestVar(8)` 后 setStatus → 8）、`2633`（`var0 == 2` 时 setStatus）、`4200`（`var0 == 3`）、
`3933/3934/3935/3939`（`defaultCloseDialog(6,6)/(8,8)/(4,4)/(3,3,true,false,0)`）、`10100/20100`
（`useQuestItem(...,4,4,true)`，已有门禁）、`15300/25300`（`changeQuestStep(13,14,true)` 停 13，已有真机验收）、
`19008..19038` / `29014..29038`（制作名人族的 `setQuestVarById(0,1)` 与 `checkItemExistence(11,11,true,...)`）等。

族 C：`2303`（var0 = 11..15 / 21..25 击杀计数，客户端 `Progress(11~14)/(15)/(21~24)/(25)` 驱动行；XML 的
reward1/reward2 投影 1/2 是行标记）、`50008/51008`（`ProgressAll` + sensoryArea 计数，末行槽位 15）、
`11467`（reward0..3 四条投影覆盖 `changeQuestStep(var,var,true)` 的 0..3）、`1114`（行 4/5 是两条分支各自的
领奖行）、`80690`（末行槽位 15，击杀计数族）。

族 D：`1005/1479/24120/24123/51010/51020/51022` 在 legacy 侧既无 Java handler、也无
`quest_script_data/*.xml` 脚本（`quest_data.xml` 只有奖励元数据），迁移投影比客户端末行少一行；缺 legacy
依据无法判定正误，本批登记为待取证（不改）。

### 三十三之二、落点（6 个 XML）

- `1876.xml` / `2876.xml`：reward 投影 `var0 1 -> 2`；追加无 source 的 `ENTER_WORLD` 自愈边
  `REWARD && var0=1 -> reward(set var0=2)`。
- `14123.xml`：reward 投影 `0 -> 1`；4 条 `report -> reward` 交接的 `set-variable var0 0 -> 1`；
  自愈边 `REWARD && var0=0 -> 1`。
- `2600.xml`：reward 投影 `0 -> 1`；自愈边 `REWARD && var0=0 -> 1`。
- `11010.xml`：reward 投影 `0 -> 3`；自愈边 `REWARD && var0=0 -> 3`。
- `1466.xml`：reward 节点补 `var0=1`；道具交接新增 `set-variable var0=1`；203903 交接 `var0 2 -> 1`；
  两条自愈边 `REWARD && var0=0 -> 1` 与 `REWARD && var0=2 -> 1`。
- 应用脚本：`.agents/summary/quest-10527-reward-row/apply_batch29_reward_row_closure.py`（`--check` 幂等，
  6/6 指纹校验；自愈边插入 `</transitions>` 之前）。
- 审计脚本登记集：`.agents/summary/quest-10527-reward-row/audit_reward_row_vs_client_steps.py` 的
  `LEGACY_STEP_EXCEPTION`（64 项）、`COUNTER_SLOT_EXCEPTIONS`（6 项）、`NO_LEGACY_HANDLER_OBSERVED`（7 项），
  报告第 [5] 节会直接列出三组已登记任务。

### 三十三之三、验证（2026-09-22）

- **静态**：`xmllint --noout --schema quest_definition.xsd` 6/6 validates；`apply_batch29_*.py --check`
  幂等（`BATCH29_APPLIED`×6 → `BATCH29_OK all 6 quests already-applied`）。
- **全库行号审计**：`MISSING_LAST_ROW 81 -> 77`（1466/1876/2876/11010 由 `ROW_BEHIND`/`NO_REWARD_ROW` 转
  **`ALIGNED`**；14123/2600 按 QE-054 保留 legacy 落盘值 1，转入登记例外）；`ROW_ALIGNED 2665 -> 2669`、
  `ROW_BEHIND 182 -> 179`、`NO_REWARD_ROW 179 -> 178`，其余桶不变。剩余 77 个 `MISSING_LAST_ROW` 现在
  **100% 落在三组登记集合内**（62 + 6 + 7 + 已登记 2 = 77）。
- **section0 审计**：`residual rows 837` 与批次 26/27/28 完全一致（`COUNTER_CHAIN_OK 823` / 扩展例外 6 /
  闭环 7 + `REVIEW_LEGACY_NO_VAR0 1`），本批不涉及 COUNTER_CHAIN 族。
- **Maven（授权后执行）**：reward/row/ladder/journal/counter 相关 **65 个测试类 299 例**，其中与本次改动相关
  的全绿；新增 `Batch29RewardRowClosureContractTest`（4 例）与同步更新的
  `Quest1466ClientDialogAlignmentTest`（reward 投影 `{} -> {var0=1}`、道具交接新增 `SetVariable(var0,1)`、
  203903 交接 `var0 2 -> 1`）；`PRODUCTION_COMPILE_OK=6189 / FAILURES=0 /
  INTERACTION_OBJECT_FAILURES=0 / WHITELIST_VIOLATIONS=0`。
  （已知无关红，HEAD 本就不成立，本批不修：`MissionItemConsumptionBatchRegressionTest` 2 例
  （20529 `s9 -> reward`、29064 `started -> reward`）；`QuestKillCounterRetailGateTest
  .singleCounterQuestsRequireExactlyTheClientGate` 1 例（15101 在 `c44c50bd0` 加了 var0 行索引字段，
  与 `30d2daff9` 的单计数器契约冲突，两个提交都在本批之前）。）
- **证据表**：[batch29-evidence.tsv](batch29-evidence.tsv)（6 个修复 + 3 个族的登记汇总）。
- **客户端实机：PENDING_CLIENT**。

客户端复测路径（PENDING_CLIENT）：

1. 1876：与 Sakmis（278502）对话后任务书切到行 1；与 Calon（278503）对话进入领奖态，任务书应停在
   **行 2“向 Taranis 报告”**（修复前停在行 1）；向 278501 领奖后完成。2876 同形（Lisya → Semotor → Votan）。
2. 14123：接取后击杀结界塔后空地的 hippolyta 一次，任务书应切到“向 Dionera 报告”所在行；与 Dionera 领奖后完成。
3. 2600：与 Shugo 对话拿到守护石、召唤并击杀 Malek 拿到原石后，回 Shugo 处领奖，任务书应显示
   `var0=1` 对应的“搜集原石交给 Shugo”行（修复前停在行 0）。
4. 11010：Naiting → Lionel → 调查 Supply_Box → 回 Naiting 领奖；领奖时任务书应显示 **行 3“和 Naiting 对话”**
   （修复前停在行 0）。
5. 1466：在指定区域燃放奥德爆竹后任务书切到行 1“向 Valerius 报告”，与 203903 领奖后完成；两条路径都
   不应出现任务书空白。
6. 旧存档：处于 REWARD 且 `var0` 等于各任务旧值（1876/2876=1、14123/2600/11010/1466=0、1466 的越界 2）
   的角色登录/切图时应自愈到新值，正规态（已是新值）不重复提示“任务更新”。

### 三十三之四、边界与后续

- 行号口径（QE-051）依旧只是**发现工具**：本批 81 个任务里只有 6 个属于“legacy 落盘 ≠ XML 投影”的真缺陷，
  其余 75 个要么是 legacy 落盘值与投影一致的误报，要么是 var0 不承载行号的计数器族、要么缺 legacy 依据；
  禁止按末行索引批量替换（`15300/25300` 真机验收、`10100/20100` 道具消耗门禁都会被打断）。
- 族 B 的“末行不亮”是 legacy 行为（领奖态停在前一行），本批按 QE-054 保留；若后续客户端复测认为末行必须
  点亮，需要**先**确认 legacy 是否本来就有写入缺口，再逐族重开，而不是直接改投影。
- 族 D（`1005/1479/24120/24123/51010/51020/51022`）需要客户端/数据侧进一步取证：迁移投影比客户端末行少
  一行，但 legacy 侧没有任何进入 REWARD 的写入可供比对。
- **剩余挂账（不要遗忘）**：`MISSING_LAST_ROW 77`（全部已登记，等待客户端复测或进一步取证）、
  `STATES_BEYOND_ROWS 2622`、`INTERIOR_GAP 263`、`MISSING_TAIL_ROWS 80`、`NO_NODES 16984/26984`、
  `MISSING_DEFINITION 3959/4963/18706/18744/20015/28706/28744/29706`；`section0 residual 837`
  （COUNTER_CHAIN_OK 823 / 扩展例外 6 / 闭环 7 + `REVIEW_LEGACY_NO_VAR0 1`）；
  新增已知无关红 `QuestKillCounterRetailGateTest`（15101，见上）。

## 三十四、批次 30：客户端独有“过场/影片播放隐藏任务”族（2026-09-22 用户授权后执行）

### 三十四之一、族判据：真端 dev_name 与客户端空槽位

批次 29 之后剩下的两个挂账桶（`NO_NODES 16984/26984`、`MISSING_DEFINITION 3959/4963/18706/18744/20015/28706/28744/29706`）
在真端与客户端两侧都能定性，不再属于“未知缺口”。真端 58Server `Map/XML/quest.xml`（UTF-16，共 11.5 MB 转码后检索）
里这 10 个 id 的 `dev_name` 直接写明用途：

- `18744/28744` = 「타메스 컷신 재생용(천)」（泰梅斯过场播放用，天族/魔族各一条）；
- `16984/26984` = 「룬의 안식처 컷신 재생용 히든 퀘스트 (천)」（符文圣所过场播放用隐藏任务）；
- `20015` = 「[5.5 업데이트 인트로 영상 재생용 히든 퀘스트] 영상 재생용 히든 퀘스트 (천마공용)」（5.5 更新开场影片播放用隐藏任务，天/魔共用）；
- `18706/28706` = 「진리는 세월과 함께 흐른다」等真实任务，但客户端 `minlevel_permitted=maxlevel_permitted=999`、前置 `Q18700/Q28700` 未实现；
- `3959/4963` = 「파이널 미션 에필로그」，真端前置 `Q1099/Q2099` 被取消（`043426b47` 已删除，由
  `DisabledClientQuestPlaceholderCatalogTest` 锁定“不得注册、不得打包”）；
- `29706` 在客户端 `quest.xml` 与真端 `quest.xml` 中都不存在，只剩残留 HTML。

客户端任务书行由 `quest_summary` 里的 `<step>` 槽表达（不是 `<p>`，批次 24 的扫描脚本口径）：`18744/28744/16984/26984`
各 4 槽、`20015` 3 槽，槽内可见文本全为空（step0 只挂 `[%collectitem]` 占位）。这一族没有可点亮的任务书行，
QE-051 的行号口径对它们不适用——本批把这点写进审计脚本的 `BLANK_JOURNAL_SLOT_EXCEPTIONS` / `CLIENT_ONLY_ISOLATED_QUESTS` 登记，
判定保持不变。

### 三十四之二、落点（18744/28744 补齐可执行定义）

证据链：

1. **模板**（客户端 5.8 `quest.xml` + 真端 `quest.xml` + 历史 `quest_data.xml` 条目）：`min-level 60`、
   `maxlevel_permitted 0`（无上限）、`race pc_light/pc_dark`、`max_repeat_count 1`、
   `reward_exp1 = reward_gold1 = 0`、`category QUEST`、名称 `1800942`「阿비소가 알려준 리멘투 정보」/
   `1800950`「프로쿠라가 알려준 리멘투 정보」（CHS 表对应“阿比索提供的勒门图的情报/普罗库拉提供的勒门图的情报”）。
2. **行为**：迁移前 handler `AbstractRaksangIntro`（`origin/history 77d99efd6` 的
   `_18744Avisos_Intelligence`/`_28744Procuras_Intelligence`）：world `300610000`（Raksang Ruins）+
   等级 >= 60 + 阵营相符 → 自动接取并播放过场；已接取/领奖的存档再次进入时重播；过场结束置 REWARD 并完成；
   已完成（COMPLETE）不再触发。
3. **资源**：过场 `912` = 客户端 `CutScenes.xml` 的 `CS_ID_132`（`cs_id_132.seq`），其文本 `cs_id_132.xml`
   正是拉科兰遗迹开场（精神支配实验、三岔路、支援品）；`CutSceneMovies.xml` 只到 id 37，因此包类型必须是
   `CUTSCENE`(0)——迁移前 handler 写的 `SM_PLAY_MOVIE(1, 912)` 指向影片表之外，不能照抄。
   全库交叉验证：232 个 `play-movie type=CUTSCENE` 的 id 100% 落在 `CutScenes.xml`，8 个
   `CUTSCENE_MOVIE` 的 id 100% 落在 `CutSceneMovies.xml`（1..37）。

定义（`quests/18744.xml`、`quests/28744.xml`，目录 2 条 `EXECUTABLE`）：节点
`unaccepted(0)/started(0)/complete(0)`（不造任务书行），三条转换：

1. `unaccepted -> started`：`enter-world` + `world-is 300610000` + `start-eligible`（引擎元数据门控：等级、
   阵营、已完成 `canRepeat=false` 全部生效）→ `play-movie 912 CUTSCENE` + `VISIBILITY_REFRESH`；
2. `started -> started`：`enter-world` + `world-is` → `play-movie 912` + `PACKET_ONLY`（重播，旧 handler 的 REPLAY 分支）；
3. `started -> complete`：`movie-end 912` → `complete-quest(0)` + `COMPLETION`（真端无奖励，故无 reward 行）。

### 三十四之三、验证（2026-09-22）

- **静态**：`xmllint + quest_definition.xsd` 2/2 validates；`quest_definition_catalog.xsd` validates；
  `docs/QUEST_CATALOG.zh-CN.md` 补 2 行并把数据范围更新为 6224 条（`refresh_full_catalog.py` 重跑后行值幂等）。
- **门禁**：新增 `CutsceneHiddenQuestFamilyContractTest`（4 例：自动接取/重播/过场结束完成 + 不造任务书行、
  不挂对话 + `16984/26984` 保持 `METADATA_ONLY` + 其余 6 个保持未注册且未打包）。
- **审计**（全库行号口径）：`NO_REWARD_ROW 178 -> 180`（新增的两行即这一族：4 个空槽、无奖励行）、
  客户端任务书覆盖 `5572 -> 5574`；`MISSING_LAST_ROW 77`、`ROW_ALIGNED 2669`、`ROW_BEHIND 179` 不变；
  脚本新增 `[10]` 节打印“已补定义 / 仍隔离”两个登记集。
- **Maven**：`CutsceneHiddenQuestFamilyContractTest` 4 例全绿；引擎组合与目录门禁 90 例全绿
  （含 `QuestEngineRuntimeCompositionTest`、`QuestRuntimeCompositionCatalogSnapshotTest`、`QuestRewardValueGateTest`、
  `BlankJournalSlotBoundaryContractTest`、`DisabledClientQuestPlaceholderCatalogTest`）；
  `PRODUCTION_COMPILE_OK=6191`（6189 + 2）、`FAILURES=0`、`INTERACTION_OBJECT_FAILURES=0`、`WHITELIST_VIOLATIONS=0`。
- **客户端实机 PENDING_CLIENT**：进入拉科兰遗迹（300610000）应自动接取并播放过场 912，过场结束任务完成；
  已完成的角色再次进入不应重播。`16984/26984` 仍不出现在任务书（`METADATA_ONLY`）。

### 三十四之四、边界与后续

- “过场/影片播放隐藏任务”族的**包类型必须由资源表决定**：id ∈ `CutScenes.xml` → `CUTSCENE`(0)，
  id ∈ `CutSceneMovies.xml` → `CUTSCENE_MOVIE`(1)；迁移前 handler 的 `SM_PLAY_MOVIE(1, …)` 不是类型依据。
- `16984/26984` 仍是 `METADATA_ONLY`：要补行为必须先取到它们各自的过场 id 与触发世界/区域；当前只有真端
  `dev_name` 能证明用途（符文圣所过场），不足以写行为。
- `20015`（5.5 开场影片，客户端还带 `check_user_item_ok/fail` 检查页）、`18706/28706`（等级 999 占位族）、
  `3959/4963`（禁用占位）、`29706`（客户端数据集里不存在）继续隔离。
- 挂账收敛：`NO_NODES` / `MISSING_DEFINITION` 两桶自此为“**1 族登记 + 2 个已实现**”，后续审计按 `[10]` 节登记集核对，
  不得再把它们当成缺口批量补定义；其余挂账（`STATES_BEYOND_ROWS 2622`、`INTERIOR_GAP 263`、
  `MISSING_TAIL_ROWS 80`、`section0 residual 837`）不变。

## 三十五、批次 31：Gelkmaros 三行交接塌陷族（21217 / 21244 / 21249，2026-09-22 用户授权后执行）

### 三十五之一、族判据与证据（MISSING_TAIL_ROWS 桶的第一族收口）

批次 30 之后全库还剩 **82 个 `MISSING_TAIL_ROWS`**（服务端可见状态是 `0..k-1` 的完整阶梯，但 k < 客户端行数）。
本批先用「legacy 事件链 vs 客户端行数 vs 当前 XML 阶梯」三向比对做全量 triage，再挑出形态最干净的一族落地：

| 任务 | 客户端 3 行（quest_summary） | legacy 事件链 | 旧定义（塌陷） |
|---|---|---|---|
| 21217 New Research Plan | 行 0 把伊塔尔的报告书交给 Wolfgang(799239) / 行 1 交给 Fjoersvith(798713) / 行 2 交给 Barretta(799226) | 799316 接取并给报告书 182207890；799239 `defaultCloseDialog(0, 1)` 写 1；798713 `setQuestVar(2)` + `defaultCloseDialog(2, 2, true, false)` 写 2 并置 REWARD；799226 领奖 | 4 个 NPC 全部 `NPC_START + started -> reward`（SELECT_QUEST_REWARD / SETPRO1），行 1/行 2 无状态 |
| 21244 Search For The Biolab | 行 0 向 Batalrion(799318) 转达 / 行 1 向 Helen(799320) 转达 / 行 2 把卷轴交给 Tanar(799317) | 799318 `defaultCloseDialog(0, 1)`；799320 `giveQuestItem(182207924)` + `setQuestVar(2)` + `defaultCloseDialog(2, 2, true, false)`；799317 REWARD 态 `removeQuestItem(182207924)` 后结束对话 | 3 个 NPC 全部 `NPC_START + started -> reward`，工作物品在接取时就发 |
| 21249 The Invincible Starket | 行 0 和 Tonistar(799416) 对话 / 行 1 和变身成德拉坎的 Tonistar(799529) 对话 / 行 2 向 Javis(799417) 转达 | 799416 `defaultCloseDialog(0, 1)`（并删除本体、生成 799529）；799529 `setQuestVar(2)` + `defaultCloseDialog(2, 2, true, false)`；799417 REWARD 态结束对话 | 3 个 NPC 都是 `NPC_REPORT started -> reward` + `SET_SUCCEED` 直跳 |

三个要点：

1. **reward 投影取 legacy 落盘值 2（QE-054）**：三条 legacy 都在最后一步用 `setQuestVar(2)` 或等价写入把 packed step 推到 2，
   再 `setStatus(REWARD)`；`changeQuestStep(..., true)` / `defaultCloseDialog(..., true, false)` 的 reward 分支只换状态、不写 nextStep，
   所以落盘值恒等于进入领奖前的 2，而不是客户端末行索引之外的任何值。
2. **每一行都要有状态（QE-051）**：客户端 3 行分别由 `started(0)`、`s1(1)`、`reward(2)` 承载；旧定义把 NPC 全塌陷成
   “接取 + 一步领奖”，审计判 `MISSING_TAIL_ROWS + ROW_WITHOUT_STATE(1 2)`，行 1/行 2 永远拿不到 START/REWARD 状态。
3. **页面按钮链必须留在 IR 里**（`QuestClientContractGateTest` 会报 `BUTTON_WITHOUT_ROUTE`）：
   21217/21244 的行 0 是 `select2 -> SELECT2_1(1353) -> SETPRO1(10000)`，行 1 是 `select3 -> SELECT3_1(1694) -> SETPRO2(10001)`，
   行 2 是 `select5 -> SELECT_QUEST_REWARD(1009)`；21249 是 `select1 -> SETPRO1`、`select2 -> SET_SUCCEED(10255)`、
   `select_success(10002 = DEFAULT_SUCCESS) -> SELECT_QUEST_REWARD`。逐任务证据见 [batch31-evidence.tsv](batch31-evidence.tsv)。

### 三十五之二、落点（三节点阶梯 + 自愈边 + owner 收敛）

统一模板：`unaccepted(0) / started(0) / s1(1) / reward(2) / complete(0)`，并补齐 legacy 的推进事件：

- 21217：`started --SETPRO1(799239)--> s1`、`s1 --SETPRO2(798713)--> reward`；接取给报告书 182207890，
  开奖励窗口时 `remove-item 182207890`，`npc-complete` owner = 799226。
- 21244：`started --SETPRO1(799318)--> s1`、`s1 --SETPRO2(799320)--> reward`（同一 transaction 里 `give-item 182207924`）；
  工作物品从“接取时给”改成“行 1 交接时给”（与 legacy 一致），开奖励窗口时 `remove-item 182207924`，owner = 799317。
- 21249：`started --SETPRO1(799416)--> s1`、`s1 --SET_SUCCEED(799529)--> reward`；owner = 799417。
- 三边都补无 source 的 `REWARD && var0 == 0 -> set var0 = 2`（`enter-world` + `LEVEL_AND_VISIBILITY_REFRESH`，无 priority），
  把旧定义“一步领奖”留下的旧存档投影纠正到领奖行；正规态（已是 2）不重复提示。
- `execute` 顺序：`defined(...)` → transition 级 `conditions` 只允许 var0 与源行匹配（0/1），阶梯内部禁止回写 var0=0。

### 三十五之三、验证（2026-09-22）

- **静态**：`xmllint --noout --schema quest_definition.xsd` 3/3 validates；`apply_batch31_gelkmaros_row_ladder.py --check`
  幂等（`BATCH31_OK 21217/21244/21249 already-applied`）。
- **全库行号审计**：`MISSING_TAIL_ROWS 82 -> 79`、`ROW_BEHIND 179 -> 176`、`ROW_WITHOUT_STATE 511 -> 508`、
  `ROW_ALIGNED 2669 -> 2672`、`ROW_STATE_ALIGNED 2441 -> 2444`、`ALIGNED 2441 -> 2444`；`STATES_BEYOND_ROWS 2622`、
  `INTERIOR_GAP 263`、`MISSING_LAST_ROW 77`、`NO_REWARD_ROW 180` 不变。三个任务由
  `ROW_BEHIND / MISSING_TAIL_ROWS / ROW_WITHOUT_STATE` 全部转 `ROW_ALIGNED / ALIGNED / ROW_STATE_ALIGNED`（`visible=0 1 2`、`recovery=True`）。
- **审计脚本新增 [11] 节**：`MISSING_TAIL_ROWS` 逐族盘点——本批修复 3 个、已登记例外 6 个（1000/2000/18744/28744 空槽位族 +
  30600/30610 双层计数族；30600/30610 由 `MULTI_LAYER_COUNTER_EXCEPTIONS` 登记，批次 26 的自愈边与
  `Quest15546KillCounterSaturationFlowTest` 已锁定）、其余 73 个待逐族收口。
- **Maven（授权后执行）**：27 个 reward/row/ladder/owner/catalog 测试类 **151 例全绿**，含新增
  `Batch31GelkmarosRowLadderContractTest`（8 例：三行投影 / 每行一个状态 / legacy 推进链 / 领奖 owner / 奖励窗口路由 /
  工作物品生命周期 / 旧存档自愈 + planner 收敛 / 不再保留 `started -> reward` 塌陷跳转）；
  `PRODUCTION_COMPILE_OK=6191 / FAILURES=0 / INTERACTION_OBJECT_FAILURES=0 / WHITELIST_VIOLATIONS=0`。
- **客户端实机 PENDING_CLIENT**：① 21217 接取后任务书应停在行 0，与 Wolfgang 交报告书后切到行 1（Fjoersvith）、
  再切到行 2（Barretta）并开奖励窗口；② 21244 同形（Batalrion -> Helen -> Tanar，卷轴在行 1 过手）；
  ③ 21249 与 Tonistar 对话切行 1、与变身成德拉坎的 Tonistar 对话切行 2（Javis）并领奖；④ 旧存档
  （历史上被旧定义一步写成 REWARD + var0=0）登录/切图后应落在领奖行，正规态不重复提示“任务更新”。

### 三十五之四、边界与后续

- 21249 的换装召唤（legacy 在行 0 删除 799416 并 `addNewSpawn(..., 799529, ...)`）**未在本批落地**：本批只重建行阶梯，
  行 1 仍按现状依赖世界中的 799529；若实机发现 799529 不在场，需要单独按 spawn 契约补，并追加对应运行时证据。
- `MISSING_TAIL_ROWS` 剩下的 73 个已按 [11] 节列出（样例：1582/1634/1938/2223/2239/2289/2307/2372/2411/2922/3013/3217 …），
  形态包含“legacy 递增但客户端行更多（需先判 var0 是否行号）”“legacy 阶梯短于客户端末行（末行不亮，属 legacy 行为）”
  “无 legacy handler（需数据侧取证）”三类，**禁止按客户端行号机械补阶梯**；后续批次按族继续收口。
- 挂账不变：`STATES_BEYOND_ROWS 2622`、`INTERIOR_GAP 263`、`MISSING_LAST_ROW 77`（全部已登记）、
  `section0 residual 837`、客户端隔离族 8 个（16984/26984/20015/18706/28706/3959/4963/29706）。

## 三十六、批次 32：卡多尔迎新两阶段阶梯（13800 / 23800，2026-09-22 用户授权后执行）

### 三十六之一、族判据与证据（MISSING_TAIL_ROWS 第二族：客户端页链长于 legacy 一跳）

批次 31 之后全库还剩 **77 个 `MISSING_TAIL_ROWS`**。本批把「retail `zz_retail_simple_quests.xml` 的 step 数 vs 客户端
`quest_summary` 行数」交叉筛了一遍：**1 retail step + 3 客户端行**的只有 7 个（13800、14200、23800、24155、25094、30504、30554），
其中 **13800/23800 是阵营镜像对且形态最干净**（14200/24155 属 `STATES_BEYOND_ROWS` 计数行，25094 是 use-item 族，
30504/30554 是 `SET_SUCCEED` 塌陷族，留待后续批次）。

| 任务 | 客户端 3 行（quest_summary） | 客户端页链 | legacy handler（迁移前） | 旧定义（塌陷） |
|---|---|---|---|---|
| 13800 New Lands to Behold | 行 0 带上 `quest_13800a` 去找 `LF5_OP1_ZoneTeleport_L`(804782) / 行 1 移动到卡多尔，和 `LDF5_Fortress_Alphion_E`(802431) 对话 / 行 2 再次和 Alphion 对话 | `select2 -> SELECT2_1(1353) -> SETPRO1(10000)`、`select3 -> SELECT3_1(1694) -> SELECT3_1_1(1695) -> SETPRO2(10001)`、`select5(2375) -> SELECT_QUEST_REWARD(1009)` | 804782 的 `STEP_TO_1`：`setQuestVarById(0, +1)` 写 1；802431 的 `SELECT_REWARD`：只 `setStatus(REWARD)`（落盘恒为 1，跳过 select3 段） | 804699/804782/802431 三个 NPC 全部 `NPC_START + started -> reward`，行 1/行 2 无状态，reward 投影停在 0 |
| 23800 A Full New World | 行 0 带上 `quest_23800a` 去找 `DF5_OP1_ZoneTeleport_D`(804753) / 行 1 前往卡多尔，和 `LDF5_Fortress_Pintz_E`(802433) 对话 / 行 2 再次和 Pintz 对话 | 同形页链 | 804753 的 `STEP_TO_1` 写 1；802433 的 `SELECT_REWARD` 只 `setStatus(REWARD)` | 804719/804753/802433 三个 NPC 全部 `NPC_START + started -> reward`，且三处都能领奖，行 1/行 2 无状态 |

三个要点：

1. **legacy 落盘 step=1 只解释旧存档为什么会停在行 1；行阶梯按客户端页链推到 2（QE-054 解释存档值 + QE-051 决定投影）**：legacy 的 `setStatus(REWARD)`
   不写 nextStep，所以旧存档里 `REWARD` 会带 `var0=1`；而客户端 `quest_summary` 有 3 行、页链给出两次推进
   （`SETPRO1` 与 `SETPRO2`），因此本批按客户端页链重建 `started(0) -> s1(1) -> reward(2)`，**并且**为 `var0=0`（旧定义
   一步领奖留下的更早存档）与 `var0=1`（legacy 一跳留下的存档）各补一条 enter-world 自愈边。
2. **每一行都要有状态（QE-051）**：旧定义判 `MISSING_TAIL_ROWS + ROW_WITHOUT_STATE(1 2)`；本批后三行分别由
   `started(0)`、`s1(1)`、`reward(2)` 承载，`visible=0 1 2`。
3. **领奖 owner 必须收敛到末行点名的 NPC**：旧定义让传送点（804782/804753）也能 `started -> reward` 领奖；
   本批把 `npc-complete` 收到 802431/802433，并保留 `reward` 态 `select5` 开奖励页、`SELECT_QUEST_REWARD` 开奖励窗口。
   逐任务证据见 [batch32-evidence.tsv](batch32-evidence.tsv)。

### 三十六之二、落点（两阶段阶梯 + 双自愈边 + letter 生命周期）

统一模板：`unaccepted(0) / started(0) / s1(1) / reward(2) / complete(0)`：

- `started --SETPRO1(传送点)--> s1`：条件 `var0 == 0`，动作 `set var0 = 1`，
  after-commit = `PACKET_ONLY` + `close-dialog`（行 0 只是“拿出书信”，不刷新任务书）。
- `s1 --SETPRO2(迎宾 NPC)--> reward`：条件 `var0 == 1`，动作 `set var0 = 2`，
  after-commit = `LEVEL_AND_VISIBILITY_REFRESH` + `close-dialog`（行 1 的说明段收口后行 2 立即点亮）。
- 两条无 source 的 `enter-world` 自愈边：`REWARD && var0 == 0 -> set var0 = 2` 与 `REWARD && var0 == 1 -> set var0 = 2`，
  均带 `LEVEL_AND_VISIBILITY_REFRESH`、无 priority。
- 书信生命周期跟随客户端交接：接取（`NPC_START` 的 `accept-actions`）发 `182215482` / `182215490`，开奖励窗口时 `remove-item`。
- 页面按钮链全部落在 IR 里：`SELECT2 -> SHOW SELECT2`、`SELECT2_1 -> SHOW SELECT2_1`、`SELECT3/SELECT3_1/SELECT3_1_1`
  与 `SELECT5` 同理，避免 `QuestClientContractGateTest` 的 `BUTTON_WITHOUT_ROUTE`。
- `unaccepted --SELECT1_1--> unaccepted`（`SHOW_QUEST_PAGE SELECT1_1`，页 1012）保留，衔接客户端的
  `SELECT1_1 -> ASK_QUEST_ACCEPT(1007)` 前置页。

### 三十六之三、验证（2026-09-22）

- **静态**：`xmllint --noout --schema quest_definition.xsd` 2/2 validates；`apply_batch32_kaldor_row_ladder.py --check`
  幂等（`BATCH32_OK 13800/23800 already-applied`）。
- **全库行号审计**：`MISSING_TAIL_ROWS 79 -> 77`、`ROW_BEHIND 176 -> 174`、`ROW_WITHOUT_STATE 508 -> 506`、
  `ROW_ALIGNED 2672 -> 2674`、`ROW_STATE_ALIGNED 2444 -> 2446`、`ALIGNED 2444 -> 2446`；`STATES_BEYOND_ROWS 2622`、
  `INTERIOR_GAP 263`、`MISSING_LAST_ROW 77`、`NO_REWARD_ROW 180` 不变。两个任务由
  `ROW_BEHIND / MISSING_TAIL_ROWS / ROW_WITHOUT_STATE` 全部转 `ROW_ALIGNED / ALIGNED / ROW_STATE_ALIGNED`
  （`visible=0 1 2`、`recovery=True`）。
- **审计脚本**：`BATCH32_KALDOR_ROW_LADDER = {13800, 23800}` 登记进 `audit_reward_row_vs_client_steps.py`，
  [11] 节改为双批次合并（批次 31 修复 3 个 + 批次 32 修复 2 个、已登记例外 6 个、其余 71 个待逐族收口）。
- **Maven（授权后执行）**：28 个 reward/row/ladder/owner/catalog 测试类 **159 例全绿**，含新增
  `Batch32KaldorRowLadderContractTest`（8 例：三行投影 / 每行一个状态 / 两阶段页链推进（条件 + 动作 + after-commit）/ 领奖 owner
  收敛且传送点不能领奖 / `select5` 与 `SELECT_QUEST_REWARD` 路由 / 书信生命周期 / **两条**旧存档自愈 + planner 收敛 /
  不再保留 `started -> reward` 塌陷跳转）；`PRODUCTION_COMPILE_OK=6191 / FAILURES=0 /
  INTERACTION_OBJECT_FAILURES=0 / WHITELIST_VIOLATIONS=0`。
- **客户端实机 PENDING_CLIENT**：① 13800 接取后任务书应停在行 0，向传送点 804782 出示书信（`select2_1` 拿出书信）后切到行 1；
  与 Alphion(802431) 走完 `select3/select3_1/select3_1_1` 说明段并 `SETPRO2` 后切到行 2 并开奖励窗口；
  ② 23800 同形（804753 传送点 -> Pintz(802433)）；③ 旧存档（`REWARD` + `var0=0` 或 `var0=1`）登录/切图后应直接落在领奖行，
  且正规态（已是 2）不重复提示“任务更新”。

### 三十六之四、边界与后续

- 本批**没有**改动 13800/23800 的接取前置（retail `start_ids=804699/804719`、`TALK` 型、阵营 ELYOS/ASMODIANS 各自一侧），
  也没有改奖励内容；只重建行阶梯、owner、letter 生命周期与自愈边。
- `MISSING_TAIL_ROWS` 剩下的 71 个仍在 [11] 节逐族列出；本批筛出的同形态候选中 **14200/24155**（`STATES_BEYOND_ROWS` 计数行）、
  **25094**（行 0 接取 / 行 1 use-item / 行 2 领奖，retail `COLLECT_ITEM 702768`）、**30504/30554**
  （retail `ACTION 701098`，旧定义 `started --SET_SUCCEED--> reward` 塌陷）留待后续批次；
  其余大批形态是「legacy 递增但客户端行更多」，**必须先判 `var0` 是否行号**，禁止机械补阶梯。
- 挂账不变：`STATES_BEYOND_ROWS 2622`、`INTERIOR_GAP 263`、`MISSING_LAST_ROW 77`（全部已登记）、
  `section0 residual 837`、客户端隔离族 8 个（16984/26984/20015/18706/28706/3959/4963/29706）。

## 三十七、批次 33：共享可见槽位族（25094，2026-09-22 用户授权后执行）

### 三十七之一、族判据与证据（quest_summary 可见槽位重复 = 行号口径多算一行）

批次 32 之后全库还剩 **76 个 `MISSING_TAIL_ROWS`**。本批先把 `quest_summary` 的 `visible="[%N]"` 槽位序列
全量扫了一遍：绝大多数任务满足「槽位 = 3 × 状态号」（10527/20527 的 16 行是 `0,3,6,...,45`，其 reward=15
已由用户在客户端验收；13800/23800/21217 的 3 行是 `0,3,6`），**3 行任务里只有 25094 是重复槽位形态
`%0 / %3 / %3`**——行 1「和 Bakring 对话」与行 2「把贝克灵的礼物交给 Daruku」共用同一个槽位。

| 项 | 证据 |
|---|---|
| 客户端 `quest_q25094.html` | `quest_summary` 3 行，槽位 `%0 / %3 / %3`；页链 `select1`(CHECK_USER_HAS_QUEST_ITEM) → `check_user_item_ok`(FINISH_DIALOG)、`select2`(SET_SUCCEED)、`select_success`(SELECT_QUEST_REWARD) |
| 客户端 `quest.xml` | `collect_item1 quest_25094a 10` / `check_item1_1 quest_25094a 10` / `drop_monster_1 DF5_FOBJ_dragonbone_Q25094` |
| retail `zz_retail_simple_quests.xml` | `start_type="TALK" start_ids="804929" end_npc_ids="804740" reset_world_id="300280000"`，单步 `COLLECT_ITEM ids="804929" action_ids="702768"` |
| 领奖合同 | `report_open=31/QUEST_SELECT -> 10002/DEFAULT_SUCCESS`、`report_action=1009/SELECT_QUEST_REWARD -> REWARD`、`reward_page=5/SHOW_SELECT_QUEST_REWARD_WINDOW1` |
| 迁移前 handler | `_25094An_Offering_Of_Friendship`：804929 的 `CHECK_COLLECTED_ITEMS` → `checkQuestItems(env, 0, 1, true, 10000, 10001)`；804740 的 REWARD 态 `sendQuestEndDialog` |

三个要点：

1. **槽位=3×状态号 ⇒ 真实状态只有 2 个**：25094 的客户端状态 0 = 行 0（采集龙骨），状态 1 = 行 1 + 行 2
   （交骨骸给 Bakring 之后同时显示“和 Bakring 对话 / 把礼物交给 Daruku”）。行号口径（client_rows=3）多算
   一行，这正是审计把它判成 `MISSING_TAIL_ROWS + ROW_WITHOUT_STATE(1 2)` 的原因，也是 QE-051 boundaries 里
   登记过的“相邻状态共用 visible 槽位”形态。
2. **legacy 意图与槽位口径一致（QE-054 解释旧存档）**：旧 handler 的 `checkQuestItems(env, 0, 1, true, ...)`
   参数就是 0 -> 1 且置 REWARD，与客户端的状态 1 吻合；但旧 helper 的 reward 分支只 `setStatus(REWARD)`、
   不写 nextStep（QE-054），所以旧存档实际落盘 `var0=0`，领奖态任务书会停在“把龙骨交给 Bakring”那一行。
3. **reward 投影取 1（不是行号 2）**：按槽位口径，领奖行与行 1 同槽；本批把 `reward` 投影改成 1 并补
   `REWARD/var0=0 -> 1` 的 enter-world 自愈边，同时补上缺失的 `s1(1)` START 状态。逐任务证据见
   [batch33-evidence.tsv](batch33-evidence.tsv)。

### 三十七之二、落点（两状态阶梯 + 双入口 + 共享槽位自愈边）

- `started(0)`：龙骨物件 702768 的 `can-act ACTION_ITEM_USE` 与 `TALK_TO_NPC/USE_OBJECT` 自环保留，
  `drop ... collecting-step="0"` 不变。
- `started --CHECK_USER_HAS_QUEST_ITEM(804929)--> s1(1)`：条件 `has-item 182215736 ×10`，动作
  `set var0=1` + `remove-item ×10`，after-commit = `LEVEL_AND_VISIBILITY_REFRESH` + 显示 `CHECK_USER_ITEM_OK`；
  失败分支（无 source 条件）`priority=1` 显示 `CHECK_USER_ITEM_FAIL`。
- `s1(1)` 上保留 `QUEST_SELECT` → `SELECT2`（客户端“好了，完成啦。你把这个交给 Daruku 吧”页）与
  `FINISH_DIALOG` → `close-dialog`（`check_user_item_ok` 页唯一按钮；缺它 `QuestClientContractGateTest`
  会报 `BUTTON_WITHOUT_ROUTE|25094|started|804929|39|10000|1008`）。
- `s1 --SET_SUCCEED(804929)--> reward(1)`（`LEVEL_AND_VISIBILITY_REFRESH` + `close-dialog`）为规范路径；
  另加防呆入口 `s1 --QUEST_SELECT(804740)--> reward(1)`（显示 `DEFAULT_SUCCESS`），保证玩家直接去找
  Daruku 也不会卡死在状态 1。
- `reward(1)`：`NPC_REPORT 804740 -> DEFAULT_SUCCESS`（合同 31 -> 10002）与 `SELECT_QUEST_REWARD ->
  SHOW_SELECT_QUEST_REWARD_WINDOW1`，`npc-complete` owner 仍为 804740。
- 自愈边：无 source 的 `enter-world`，条件 `REWARD && var0 == 0` → `set var0 = 1` + `LEVEL_AND_VISIBILITY_REFRESH`，
  无 priority；已经是 1 的存档不重复提示。

### 三十七之三、验证（2026-09-22）

- **静态**：`xmllint --noout --schema quest_definition.xsd` 1/1 validates；`apply_batch33_shared_visible_slot_row.py
  --check` 幂等（`BATCH33_OK 25094 already-applied`）。
- **全库行号审计**：`MISSING_TAIL_ROWS 77 -> 76`、`MISSING_LAST_ROW 77 -> 78`（25094 从“缺两行”变成
  “只差共享槽位那一行”），`ROW_ALIGNED/ROW_STATE_ALIGNED/ALIGNED/STATES_BEYOND_ROWS/INTERIOR_GAP` 不变；
  25094 由 `ROW_BEHIND + ROW_WITHOUT_STATE(1 2) + visible=0` 变为 `visible=0 1 + recovery=True`，并在 [5] 节
  登记为 **共享可见槽位族例外**（行号口径多算一行，禁止按末行索引 2 再补一行）。
- **审计脚本**：新增 `SHARED_VISIBLE_SLOT_EXCEPTIONS = {25094}` 与 [5] 节对应打印行，[11] 节例外集合同步并入。
- **Maven（授权后执行）**：29 个 reward/row/ladder/owner/catalog 测试类 **167 例全绿**，含新增
  `Batch33SharedVisibleSlotContractTest`（8 例：两状态投影 / 每个可见槽位都有状态 / 采集交接推进 /
  `SET_SUCCEED` 收尾 + `FINISH_DIALOG` 路由 / 奖励 NPC 防呆入口 / 领奖窗口与 owner / 旧存档自愈 + planner 收敛 /
  不再保留 `started -> reward` 塌陷跳转且无越界写 2）；`PRODUCTION_COMPILE_OK=6191 / FAILURES=0 /
  INTERACTION_OBJECT_FAILURES=0 / WHITELIST_VIOLATIONS=0`。修复过程中 `QuestClientContractGateTest` 先报出
  `check_user_item_ok` 页按钮缺路由，已按门禁补 `FINISH_DIALOG` 自环后再跑全绿。
- **客户端实机 PENDING_CLIENT**：① 接取后任务书应停在行 0“把灼热的龙族骨骸交给贝克灵”；② 交满 10 个骨骸
  并和 Bakring 对话（拿出骨骸）后，任务书应立即切到“和贝克灵对话 / 把贝克灵的礼物交给达鲁库”两行
  （同槽同时可见），而不是仍然显示交骨骸；③ 按 `select2` 的“结束对话”或直接找 Daruku，都应进入领奖态并能在
  Daruku 的开奖励窗口里领奖；④ 旧存档（历史上被写成 `REWARD + var0=0`）登录/切图后应直接落在共享槽位那一行。

### 三十七之四、边界与后续

- **本族的判据是“槽位重复”而不是“行数”**：只有确认 `quest_summary` 的两行共用同一个 `%N` 时才能少算一行；
  槽位序列为 `0,3,6,...`（大多数任务）时行号与状态号一致，仍按 QE-051 的行号口径处理。
- 已知未收口的同类形态：`10525/20525` 的槽位是 `0,3,6,18,21,24,27,30`（状态 3/4/5 缺槽、行号口径与槽位口径
  不一致），`10530` 的第 8/9 行共用槽位——这两个族需要各自的客户端证据，**不要**用 25094 的结论直接套改。
- `MISSING_TAIL_ROWS` 剩余 70 个仍在 [11] 节列出；本批筛出的同形态候选 **14200/24155**（`STATES_BEYOND_ROWS`
  计数行）、**30504/30554**（retail `ACTION 701098`，旧定义 `started --SET_SUCCEED--> reward` 塌陷，**无迁移前
  handler**，只有 retail+客户端页链可依，需单独取证）留待后续批次。
- 挂账不变：`STATES_BEYOND_ROWS 2622`、`INTERIOR_GAP 263`、`MISSING_LAST_ROW 78`、`section0 residual 837`、
  客户端隔离族 8 个（16984/26984/20015/18706/28706/3959/4963/29706）。

## 三十八、批次 34：Rentus Base 营救 Paios 镜像族（30504 / 30554，2026-09-22 用户授权后执行）

### 三十八之一、族判据与证据（柱物件 ACTION step + 三行任务书，迁移把三行塌陷成一个不存在的按钮）

批次 33 之后全库还剩 **74 个 `MISSING_TAIL_ROWS`**。本批收口的是 retail 单步 `ACTION` 形态中唯一还未对齐的一族：

| 项 | 证据 |
|---|---|
| 客户端 `quest_q30504/quest_q30554.html` | `quest_summary` 三行，槽位 `%0/%3/%6`（标准「槽位 = 3 × 状态号」）：行 0「找到并救出 Paios」、行 1「和 Paios 对话」、行 2「和 Lition 对话」；`quest_complete` 文案「把派奥斯的死讯告诉李迪安」 |
| 客户端页链 | `select_none`(accept=true，Lition 委托营救) / `select2`(Paios 遗言，按钮 **SET_SUCCEED**) / `select_success`(Lition「我一直在等你。派奥斯怎么样了？」，按钮 **SELECT_QUEST_REWARD**) / `select_quest_reward1`(奖励窗) |
| 客户端数据 | Lition = 205438、Paios = 799536（`npcs_unpacked/client_npcs_npc.xml`）；柱子物件 701098 = `IDYun_Column_Q30504`，静态刷在 `300280000_Rentus_base` 与 `300620000_Occupied_Rentus_Base` |
| retail | `start_type="TALK" start_ids="205438" end_npc_ids="799536" reset_world_id="300280000"`，单步 `ACTION action_ids="701098"`；领奖合同 `31 -> DEFAULT_SUCCESS(10002)`、`1009 -> REWARD`、奖励页 `5 -> SHOW_SELECT_QUEST_REWARD_WINDOW1` |
| 迁移前 handler | **不存在**（`git ls-tree 7e9f0316c^` 无 `30504/30554` 匹配），定义由 retail/数据驱动迁移生成 |

三个要点：

1. **迁移把三行塌陷成一行，而且按钮在客户端根本不存在**：旧定义只有 `started` 传 `SET_SUCCEED(205438)` 直达 `reward`，
   但 205438(Lition) 的客户端页里没有 `SET_SUCCEED` 按钮（那是 Paios 的 `select2` 按钮）；`reward` 投影抄成 0，
   `var0` 只有 1 bit（`max=1`）连第 2 行都放不下。
2. **柱子物件从未接进 IR**：retail 的 `ACTION action_ids="701098"` 没有落成任何 transition，玩家无法完成行 0
   （只能在 Lition 处点一个客户端不存在的按钮）。本批按同族先例（13809 焦树族的物件路由）补
   `started --USE_OBJECT(701098)--> s1`。
3. **领奖 owner 取客户端末行点名的 Lition(205438)（QE-052）**：客户端末行、`select_success/select_quest_reward1`
   的说话人、`quest_complete` 三方一致；retail 的 `end_npc_ids=799536` 与之冲突，按客户端口径收敛并登记实机复测点
   （见三十八之四）。逐任务证据见 [batch34-evidence.tsv](batch34-evidence.tsv)。

### 三十八之二、落点（三行阶梯 + 柱物件接线 + 双自愈边）

- `progress`：`var0` 由 `width=1/max=1` 放宽到 `width=3/max=7`（原字段放不下行 2；低位偏移不变，旧存档 0 仍是 0）。
- 节点：`unaccepted(0) / started(0) / s1(1) / reward(2) / complete(0)`。
- 行 0：`started --TALK_TO_NPC(701098) USE_OBJECT--> s1`（`LEVEL_AND_VISIBILITY_REFRESH`）。
- 行 1：`s1 --TALK_TO_NPC(799536) QUEST_SELECT--> s1`（显示 `SELECT2` 遗言页）+ `s1 --SET_SUCCEED(799536)--> reward(2)`
  （`LEVEL_AND_VISIBILITY_REFRESH` + `close-dialog`）。
- 行 2：`reward + QUEST_SELECT(205438) -> DEFAULT_SUCCESS`（合同 31 → 10002）+ `npc-complete` owner 205438
  （`<preview actions="USE_OBJECT SELECT_QUEST_REWARD"/>` 展开 1009 奖励窗路由，不再显式声明同名自环以免
  `AMBIGUOUS_TRANSITION`）。
- 自愈边：无 source 的 `enter-world`，`REWARD && var0==0 -> 2` 与 `REWARD && var0==1 -> 2`，均 `LEVEL_AND_VISIBILITY_REFRESH`、无 priority。
- **实作坑（已记入 QE-051）**：本任务同一个 NPC(205438) 同时是接取与领奖 NPC，`<dialog type="NPC_REPORT">` 与
  `NPC_START` 的展开会撞 `AMBIGUOUS_TRANSITION: TALK_TO_NPC`，因此 REWARD 态入口改写成显式
  `reward + QUEST_TO_NPC/QUEST_SELECT -> SHOW_QUEST_PAGE DEFAULT_SUCCESS`（与同链 30503 同形）。

### 三十八之三、验证（2026-09-22）

- **静态**：`xmllint --noout --schema quest_definition.xsd` 2/2 validates；`apply_batch34_rentus_base_row_ladder.py
  --check` 幂等 2/2。
- **全库行号审计**：`MISSING_TAIL_ROWS 76 -> 74`、`ROW_BEHIND 174 -> 172`、`ROW_WITHOUT_STATE 506 -> 504`、
  `ROW_ALIGNED 2674 -> 2676`、`ROW_STATE_ALIGNED/ALIGNED 2446 -> 2448`；30504/30554 由
  `MISSING_TAIL_ROWS + ROW_BEHIND + ROW_WITHOUT_STATE(1 2) + visible=0` 全部转
  `ALIGNED + ROW_ALIGNED + ROW_STATE_ALIGNED + visible=0 1 2 + recovery=True`。
- **审计脚本**：新增 `BATCH34_RENTUS_BASE_ROW_LADDER = {30504, 30554}`，[11] 节已修复族与残留计数同步刷新。
- **Maven（授权后执行）**：30 个 reward/row/ladder/owner/catalog 测试类 **175 例全绿**，含新增
  `Batch34RentusBaseRowLadderContractTest`（8 例：三行投影 + 字段上限 / 每行一个状态 / 柱物件接线 / 遗言行推进
  + `select2` 页 / owner 收敛且被救者不得领奖 / `10002` 入口与 `1009` 领奖路由 / 0 与 1 两条自愈边 + planner 收敛 /
  不再保留塌陷跳转与伪造的 `SET_SUCCEED`）；`PRODUCTION_COMPILE_OK=6191 / FAILURES=0 /
  INTERACTION_OBJECT_FAILURES=0 / WHITELIST_VIOLATIONS=0`。
- **客户端实机 PENDING_CLIENT**：① 接取后任务书应停在行 0「找到并救出派奥斯」；② 对柱子 701098 用/点后切到行 1
  「和派奥斯对话」，与其对话出现遗言页、点“结束对话”后切到行 2「和李迪安对话」；③ 回去找 Lition 应能开奖励窗领奖；
  ④ 旧存档（`REWARD + var0=0/1`）登录或切图后应直接落在行 2；⑤ **归属复测点**：若实机确认奖励窗只能在 Paios(799536)
  打开（retail `end_npc_ids` 口径），则需把 owner 与 31 入口迁回 799536 并保留 205438 的入口路由。

### 三十八之四、边界与后续

- **retail `end_npc_ids` 与客户端末行冲突时的取舍**：本批按 QE-052（owner = 客户端 quest_summary 领奖行 NPC）取
  205438；retail 口径未删除任何入口路由证据，冲突已登记为实机复测点，**不得**在未复测前把 owner 改回 799536。
- 本批顺带确认 `var0` 字段宽度属于契约的一部分：原 `max=1` 无法容纳行 2，任何「按行号口径补阶梯」的批次都必须
  同时检查 `progress` 字段上限（审计 [7b] 节已列同类候选 29 个）。
- `MISSING_TAIL_ROWS` 剩余 68 个仍在 [11] 节列出；本批之后同形态候选只剩 **14200/24155**（`STATES_BEYOND_ROWS`
  计数行）与 **10525/20525、10530**（槽位缺口/共用，需各自取证）。
- 挂账不变：`STATES_BEYOND_ROWS 2622`、`INTERIOR_GAP 263`、`MISSING_LAST_ROW 78`、`section0 residual 837`、
  客户端隔离族 8 个（16984/26984/20015/18706/28706/3959/4963/29706）。

## 三十九、批次 35：情人节巧克力塔镜像族（50019 / 51019，2026-09-22 用户授权后执行）

### 三十九之一、族判据与证据（事件任务的三行任务书 + 客户端 SETPRO2 页链）

批次 34 之后全库还剩 **72 个 `MISSING_TAIL_ROWS`**。本批用「客户端槽位 + 页链 + 客户端 NPC 表 + 真端 event.xml」
四向取证，收口事件驱动形态里成员最多的一族：

| 项 | 证据 |
|---|---|
| 客户端 `quest_q50019/quest_q51019.html` | `quest_summary` 三行、槽位 `%0/%3/%6`：行 0「消灭情人节布朗尼、收集巧克力交给术古」([%collectitem])、行 1「在巧克力塔上使用巧克力装饰」、行 2「和术古对话」 |
| 客户端页链 | `select_none`(accept=true) / `select1` 的 **CHECK_USER_HAS_QUEST_ITEM** / `check_user_item_ok` 的 **SETPRO2** / `check_user_item_fail` 的 FINISH_DIALOG / `select_success` 的 **SELECT_QUEST_REWARD** / `select_quest_reward1`（奖励窗） |
| 客户端 NPC 表 | 术古 **202549** = `ShugoL`；巧克力塔 **701466** = `Light_Chocolate_Tower`（Elyos）、**701467** = `Dark_Chocolate_Tower`（Asmodian） |
| 真端 authority | `event.xml` 的 `<monster_hunt start_npc_ids="202549" id="50019/51019">`；`quest.xml` 的 `collect quest_50011a/quest_51011a ×3`、`quest_work_item 50013a/51013a`、前置 `50010/51010`；本检出**无 Java handler**（事件驱动内容） |
| 旧定义 | `npc-item-report 202549 started -> reward`（交付直接置 REWARD、投影 0）+ 伪造的 `started --SET_SUCCEED(202549)--> reward`；巧克力塔物件从未接进 IR，行 1 永远拿不到状态 |

三个要点：

1. **`npc-item-report` 指令不能指向非 REWARD 节点**：编译期报
   `NPC_ITEM_REPORT_TARGET_STATUS ... node s1 must project REWARD`，因此交付落点必须改写成显式
   `started --CHECK_USER_HAS_QUEST_ITEM(202549)--> s1`（带 `has-item` / `remove-item`），不再用该指令做行阶梯。
2. **客户端 `check_user_item_ok` 的按钮是 SETPRO2（确认），不是推进两次**：交付本身（CHECK）就是行 0 的完成，
   SETPRO2 在 `s1` 上保留为就地 `close-dialog` 的确认路由，避免把任务推过“装饰巧克力塔”这一行。
3. **塔物件由活动系统刷出**：本检出内无 701466/701467 的静态 spawn，因此除了塔的 `USE_OBJECT` 推进路由外，
   另补“直接找术古对话”的防呆入口（`s1 --QUEST_SELECT(202549)--> reward(2)` 并显示 `DEFAULT_SUCCESS`），
   保证塔未刷出时不会卡在行 1。逐任务证据见 [batch35-evidence.tsv](batch35-evidence.tsv)。

### 三十九之二、落点（三行阶梯 + 交付改写 + 双自愈边）

- 节点：`unaccepted(0) / started(0) / s1(1) / reward(2) / complete(0)`（`var0` 原本已是 6 bit，无需放宽）。
- 行 0：`started --CHECK_USER_HAS_QUEST_ITEM(202549)--> s1`——条件 `has-item 182215172/182215178 ×3`、动作
  `remove-item ×3`、after-commit `LEVEL_AND_VISIBILITY_REFRESH` + `SHOW_QUEST_PAGE CHECK_USER_ITEM_OK`。
- 行 1：`s1 --TALK_TO_NPC(701466/701467) USE_OBJECT--> reward(2)`（`LEVEL_AND_VISIBILITY_REFRESH` + `close-dialog`）。
- 行 2：`reward + QUEST_SELECT(202549) -> DEFAULT_SUCCESS`（既有）+ `npc-complete` owner 202549（既有，
  `<preview actions="USE_OBJECT SELECT_QUEST_REWARD"/>` 展开 1009 奖励窗）。
- 自愈边：`REWARD && var0==0 -> 2` 与 `REWARD && var0==1 -> 2`（无 source、`enter-world`、
  `LEVEL_AND_VISIBILITY_REFRESH`、无 priority）。
- SETPRO2：`s1 --SETPRO2(202549)--> s1` + `close-dialog`（客户端 check_ok 页的确认按钮）。

### 三十九之三、验证（2026-09-22）

- **静态**：`xmllint --noout --schema quest_definition.xsd` 2/2 validates；`apply_batch35_valentine_tower_row_ladder.py
  --check` 幂等 2/2。
- **全库行号审计**：`MISSING_TAIL_ROWS 74 -> 72`、`ROW_BEHIND 172 -> 170`、`ROW_WITHOUT_STATE 504 -> 502`、
  `ROW_ALIGNED 2676 -> 2678`、`ROW_STATE_ALIGNED/ALIGNED 2448 -> 2450`；50019/51019 由
  `MISSING_TAIL_ROWS + ROW_BEHIND + ROW_WITHOUT_STATE(1 2) + visible=0` 转
  `ALIGNED + ROW_ALIGNED + ROW_STATE_ALIGNED + visible=0 1 2 + recovery=True`。
- **Maven（授权后执行）**：31 个 reward/row/ladder/owner/catalog 测试类 **183 例全绿**，含新增
  `Batch35ValentineTowerRowLadderContractTest`（8 例：三行投影 / 每行一个状态 / 交付推进（has-item + remove-item +
  check_ok 页）/ 塔装饰推进 + 防呆入口 / owner 唯一且塔不能领奖 / `10002` 入口与 `1009` 领奖路由 /
  0 与 1 两条自愈边 + planner 收敛 / 不再保留伪造的 `SET_SUCCEED` 且 SETPRO2 为就地关闭）；
  `PRODUCTION_COMPILE_OK=6191 / FAILURES=0 / INTERACTION_OBJECT_FAILURES=0 / WHITELIST_VIOLATIONS=0`。
  修复过程中先撞 `NPC_ITEM_REPORT_TARGET_STATUS`，改为显式 CHECK 路由后全绿。
- **客户端实机 PENDING_CLIENT**：① 接取后任务书停在行 0；② 交满 3 个巧克力并在术古处“拿出巧克力”后切到行 1
  （check_ok 页点确认即可关闭）；③ 在巧克力塔上使用装饰后切到行 2；④ 回术古处开奖励窗领奖；⑤ 旧存档
  （`REWARD + var0=0/1`）登录或切图后应直接落在行 2；⑥ 若活动塔未刷出，应能直接找术古进入领奖行（防呆入口）。

### 三十九之四、边界与后续

- **同族未收口成员**（同一页链形态、成员更多）：`50020`(Extra-Bitter Chocolate)、`50022`(Shove-l It，塔物件是
  701470 `Event_Cargobox`) 与 `80300-80309`（另一代情人节事件，术古 799763、物件 701774 `world_event_Cargobox` 等）
  —— 需要逐任务确认塔/箱物件与收集物 id 后再按本批模板收口，**不得**直接套用 701466/701467。
- `MISSING_TAIL_ROWS` 剩余 68 个仍在 [11] 节列出；事件族之外的大块是「legacy 递增但客户端行更多」形态，
  须先判 `var0` 是否行号（QE-051 vs QE-054）。
- 挂账不变：`STATES_BEYOND_ROWS 2622`、`INTERIOR_GAP 263`、`MISSING_LAST_ROW 78`、`section0 residual 837`、
  客户端隔离族 8 个（16984/26984/20015/18706/28706/3959/4963/29706）。

## 四十、批次 36：活动巧克力塔族 + 术古货箱族（13 任务，2026-09-22 用户授权后执行）

### 四十之一、族判据与证据（同一三行槽位下的两条页链）

批次 35 之后全库还剩 **72 个 `MISSING_TAIL_ROWS`**。本批按「同槽位 + 同页链 + 同客户端行文本形态」把情人节事件余族拆成两组，
13 个任务一次收口；两族都是客户端 `quest_summary` 三行、槽位 `%0/%3/%6`，且与 50019/51019 同属一个活动，
但**页链与收集进度不同，不得互相套用物件 id**。

#### A. 活动巧克力塔族（select1 / check_user_item_ok 页链，客户端 collect_progress=0）

| quest | NPC | 收集物（quest_data quest_drop） | 塔物件 | 活动怪 |
|---|---|---|---|---|
| 50020 | 202549 Single_Shugo | 182215173 ×3 | 701466 Light_Chocolate_Tower | 219316 |
| 80300 | 799763 | 182215288 ×3 | 831402 | 219639 |
| 80301 | 799763 | 182215289 ×3 | 831402 | 219640 |
| 80306 | 799763 | 182215294 ×3 | 831403 | 219639 |
| 80307 | 799763 | 182215295 ×3 | 831403 | 219640 |

客户端行：行 0「击杀活动怪并抢夺巧克力、交给术古」([collectitem])、行 1「在爱情巧克力塔上使用工作物」、行 2「和术古对话」。
页链：`select1`(1011) 的 `CHECK_USER_HAS_QUEST_ITEM` → `check_user_item_ok`(10000) 的 `SETPRO2` → 塔 `USE_OBJECT` →
`select_success`(10002) 的 `SELECT_QUEST_REWARD` → 奖励窗 1009。50019/51019（批次 35）是本族模板。

#### B. 术古货箱族（select4 / select5 页链，客户端 collect_progress=4）

| quest | NPC | 收集物（quest_drop npc_id=货箱，collecting_step=4） | 货箱 | 行 0 活动怪 |
|---|---|---|---|---|
| 50021 | 202549 | 182215176 ×3 | 701470 Event_Cargobox | Brownie_Solo |
| 51021 | 202549 | 182215182 ×3 | 701470 Event_Cargobox | Brownie_Solo |
| 50022 | 202549 | 182215177 ×3 | 701470 Event_Cargobox | Shulack_Couple |
| 51022 | 202549 | 182215183 ×3 | 701470 Event_Cargobox | Shulack_Couple |
| 80302 | 799763 | 182215292 ×3 | 701774 world_event_Cargobox | Brownie_Solo |
| 80303 | 799763 | 182215293 ×3 | 701774 world_event_Cargobox | Shulack_Couple |
| 80308 | 799763 | 182215298 ×3 | 701774 world_event_Cargobox | Brownie_Solo |
| 80309 | 799763 | 182215299 ×3 | 701774 world_event_Cargobox | Shulack_Couple |

客户端行：行 0「消灭活动怪 ([%2]/3)」、行 1「和术古对话」、行 2「在术古货物箱里找到道具并交给术古」([collectitem])。
页链：`select4`(2034) 的 `SETPRO2` → 行 1；同一 select4 的 `SETPRO2` → 行 2；`select5`(2375) 的 `CHECK_USER_HAS_QUEST_ITEM`
成功 → `select_success`(10002)、失败 → `check_user_item_fail`(10001)（`FINISH_DIALOG` 关闭）。

#### 共同证据与三个关键判读

1. **旧定义三种残留形态**（HEAD 审计，逐任务见 [batch36-evidence.tsv](batch36-evidence.tsv)）：
   - 塔族 5 个 + 货箱族的 50022/80302/80303/80308/80309：reward 节点 `var0=0`，`npc-item-report` 或交付直跳 REWARD，行 1/2 没有状态；
   - **50021/51021**：`a3`/`k15` 交付后落 reward `var0=15`，出现 `ROW_AHEAD / STATES_BEYOND_ROWS`：50021 的可见槽为
     `0 1 2 3 15`、51021 为 `0..15`。这与用户报障 10527 的「var0=15 被写成 14、下一步不显示」是同一类越界计数残留，
     本批按行号口径归零重建；
   - **51022**：reward `var0=1` 且缺末行（`MISSING_LAST_ROW`，可见槽 `0 1`）。
2. **客户端 `collect_progress` 决定页链**：塔族=0（检查收集物后切塔行）、货箱族=4（先走 select4 结束击杀行，
   再在 select5 交物）。零售 `quest_data.xml` 中货箱族的 `quest_drop collecting_step="4"` 对应客户端第二组 3 bit 槽位
   （3..5），即**行 1**；生产门禁 `QuestInteractionObjectValidator` 要求 `quest_use_item` 货箱的资格路由落在
   START 态且 `var0 == collecting_step`，因此本批把 701470/701774 的 `collecting_step` 统一收敛为 **1**（与 s1 的
   `var0=1` 对齐），而不是保留 4。
3. **活动内容由活动系统下发**：NPC 202549/799763、活动怪 219315/219316/219639/219640、塔 701466/701467/831402/831403、
   货箱 701470/701774 在本检出内都没有静态 spawn；`quest_data.xml` 的 `quest_drop` 是物品→活动怪的权威链接
   （例如 50020 的 182215173 钩在 219316、80300 的 182215288 钩在 219639）。整族按批次 35 口径只重建行阶梯、
   不新增「未击杀即锁死」的门。

### 四十之二、落点（两族三行阶梯 + 三处编译期门禁规避）

- **节点统一**：`unaccepted(0) / started(0) / s1(1) / reward(2) / complete(0)`；`var0` 宽度统一为 6 bit（max 63），
  51022 由原来的 width=1 放宽，50021/51021 由 a/k 计数形态收敛到行号。
- **塔族**：`started --CHECK_USER_HAS_QUEST_ITEM(has-item×3 + remove-item×3)--> s1`；`s1` 的 `SETPRO2` 只做
  `close-dialog`（不额外推进）；`s1 --塔 USE_OBJECT--> reward`；保留 `s1 --QUEST_SELECT(NPC)--> reward` 防呆入口
  （活动塔未刷出时仍能推进）；reward 行用 `QUEST_SELECT` 显示 `DEFAULT_SUCCESS`(10002)，1009 奖励窗由既有
  `USE_OBJECT SELECT_QUEST_REWARD` 结算路由展开。
- **货箱族**：`started --QUEST_SELECT--> SELECT4 页` + `started --SETPRO2--> s1`；`s1 --QUEST_SELECT--> SELECT4 页` +
  `s1 --SETPRO2--> reward`；`s1` 挂 `can-act template-id=货箱 ACTION_ITEM_USE`；reward 行用 `QUEST_SELECT` 显示 SELECT5 页，
  `CHECK_USER_HAS_QUEST_ITEM` priority=0 成功 → `DEFAULT_SUCCESS`(10002)，priority=1 失败 → `CHECK_USER_ITEM_FAIL`(10001)。
- **自愈边**：13 个任务全部补 `REWARD && var0==0 → reward(2)` 与 `REWARD && var0==1 → reward(2)` 两条无 source 的
  `enter-world` 边（`LEVEL_AND_VISIBILITY_REFRESH`、无 priority），覆盖旧存档停在 0/1；50021/51021 旧的 15
  不再作为可见槽，51022 旧的 1 会被自愈到行 2。
- **编译期门禁（已修，勿回退）**：
  1. `AMBIGUOUS_TRANSITION`：货箱族 reward 行一度同时挂 `QUEST_SELECT→SELECT5` 与 `QUEST_SELECT→DEFAULT_SUCCESS`
     两条无 priority 路由；最终去掉 reward 行直达 10002，领奖入口由交付成功 CHECK 路由打开的 `select_success`(10002) 承担。
  2. `QuestInteractionObjectValidator` / `PRODUCTION_INTERACTION_OBJECT_FAILURES`：`can-act` 必须落在 START 态且
     `var0 == collecting_step`，故把 can-act 放到 s1 并把 collecting-step 由 4 收敛为 1。
  3. `NPC_ITEM_REPORT_TARGET_STATUS`：删除 `npc-item-report`，交付改显式 CHECK 路由。

### 四十之三、验证（2026-09-22）

- **静态**：`xmllint --noout --schema quest_definition.xsd` 13/13 validates；
  `apply_batch36_event_row_ladder.py --apply` 后 `--check` 幂等 13/13 OK。
- **全库行号审计（脚本 [11] 节已改为批次 36）**：`MISSING_TAIL_ROWS 72 -> 62`（-10）、`MISSING_LAST_ROW 78 -> 77`（-1）、
  `ROW_BEHIND 170 -> 159`（-11）、`ROW_WITHOUT_STATE 502 -> 491`（-11）、`ROW_ALIGNED 2678 -> 2691`（+13）、
  `ROW_STATE_ALIGNED 2450 -> 2463`（+13）、`STATES_BEYOND_ROWS 2622 -> 2620`（-2，50021/51021 从越界计数形态转出）。
  13 个任务全部 `ALIGNED + ROW_ALIGNED + ROW_STATE_ALIGNED + visible=0 1 2 + recovery=True`；逐任务前后见
  [batch36-evidence.tsv](batch36-evidence.tsv)。
- **Maven（授权后执行）**：批次 35 的 31 类 + `Batch29RewardRowClosureContractTest`、
  `CutsceneHiddenQuestFamilyContractTest`、`Batch31`–`Batch36` 共 **35 个测试类全绿**，新增
  `Batch36EventRowLadderContractTest`（5 例：三行投影 + 禁直跳 / 塔族交付与装饰 / 货箱族 SETPRO2+can-act+
  collecting-step=1+1009 / reward owner 唯一 / 0/1 双自愈边 + planner 收敛）；
  `PRODUCTION_COMPILE_OK=6191 / FAILURES=0 / INTERACTION_OBJECT_FAILURES=0 / WHITELIST_VIOLATIONS=0`。
- **客户端实机 PENDING_CLIENT**：① 接取后任务书停在行 0；② 塔族交满 3 个收集物后切行 1（check_ok 页确认即关闭）；
  ③ 在爱情巧克力塔上使用工作物后切行 2；④ 回术古处开奖励窗领奖；⑤ 旧存档（`REWARD + var0=0/1`）登录或切图后落行 2；
  ⑥ 货箱族按 select4 结束 → 行 2 用 select5 交付；⑦ 塔/箱未刷出时应能走防呆路径；⑧ 50021/51021 旧存档
  `var0=15` 不得再出现，51022 旧存档 `var0=1` 应被自愈到行 2。

### 四十之四、边界与后续

- **活动内容无静态 spawn**：NPC/怪/塔/箱均由活动系统运行时下发，本批不做「未击杀即锁死」的门，避免活动未开时不可完成；
  整族行阶梯只依赖客户端页链与 `quest_data` 的 drop 记录。
- **同活动未收口**：`80304/80305`（Asmodian「Bitter or Sweet?」）是另一形态——三行 = 回答 → 去见坠入爱河的术古/
  单身部队成员，页链 `select1_1 / select2_1 / select2_2 / select2_1_1 / select3_1 / select3_2` + `SETPRO1/SETPRO2`，
  无 items/drop；本批未动，登记下一批。
- **`MISSING_TAIL_ROWS` 剩余 56 的族分组（批次 37 候选）**：
  - `3711/4711/18213/28213`（rows=4、slots=(0,3,6,9)、retail 双 TALK）——最干净的候选；
  - `2411/2922/4732`（rows=3，adv `FINISH_DIALOG/SELECT_QUEST_REWARD`）；
  - `3013/3217/4217`（rows=3，adv `CHECK_USER_HAS_QUEST_ITEM/SET_SUCCEED`）；
  - `11072/21081/24150`（rows=3，retail 双 TALK）；
  - `13918/18911/23918/28911`（rows=6，含 `STATES_BEYOND_ROWS`）；
  - `26905/26906/26908`（slots=(0,9,24)）、`3938/4942`（rows=11）、`14220/24220`、`17500/27500`。
  - 其余样例：1582(rows=4,缺=1 2 3)、1634(rows=5,缺=3 4)、1938(rows=3)、2223(rows=4)、2239(rows=3)、
    2289(rows=4)、2307(rows=3)、2372(rows=6)、2411(rows=3)、2922(rows=3)、3013(rows=3)、3217(rows=3)。
- **其它挂账**：`STATES_BEYOND_ROWS 2620`、`INTERIOR_GAP 263`、`MISSING_LAST_ROW 77`、
  `section0 residual 837`（沿用批次 35 专项口径，本批未重跑该专项）、客户端隔离族 8 个
  （3959/4963/16984/18706/20015/26984/28706/29706）、3 例 HEAD 即红
  （`MissionItemConsumptionBatchRegressionTest` 20529/29064、`QuestKillCounterRetailGateTest#singleCounterQuestsRequireExactlyTheClientGate` 15101）。

## 四十一、批次 37：交谈/情报 → 击杀 → 报告两族（26905/26906/26908 + 3711/4711，2026-09-22）

### 四十一之一、族判据与证据（客户端任务书 + legacy 合同双证）

批次 36 之后 `MISSING_TAIL_ROWS 72 -> 62`（其中 6 个已登记例外）。本批从剩余清单里取两组
“每一行都是对话/击杀/报告”的任务，共 5 个：

#### A. Asmodian bounty 三行族（26905/26906/26908，monster_hunt）

| quest | 任务书行 0 / 1 / 2 | start NPC（legacy） | end NPC（任务书点名） | 击杀目标 |
|---|---|---|---|---|
| 26905 | 和火山调查基地的 Gangleri 对话 / 消灭 DF2_1 Dark_Raider([%11]/1) / 向 Gangleri 报告 | 204301 Aegir | 204372 Gangleri | 231555 231556 |
| 26906 | 和 DF2_SUB_E1 的 Tyr 对话 / 消灭 DF2_2 Dark_Raider([%11]/1) / 向 Tyr 报告 | 204301 Aegir | 204369 Tyr | 231558 231559 |
| 26908 | 和 DF3_SZ_G2 的 Svafnir 对话 / 消灭 DF3_2 Dark_Raider([%11]/1) / 向 Svafnir 报告 | 204702 Nerthus | 204817 Svafnir | 231570 231571 |

客户端页链：`select2` 的 `SETPRO1`（行 0 → 行 1）→ 击杀（行 1 → 行 2）→ `select5` 的
`SELECT_QUEST_REWARD`（行 2 → REWARD + 奖励窗）。`docs/quest/client-dialog-mapping/legacy-quest-dialog-contracts.csv`
给出 `start_npc_ids=204301/204301/204702`、`end_npc_ids=204372/204369/204817`：**接取 NPC 与
任务书点名的进度/报告 NPC 是两个不同实体**。旧定义把两个实体都写成完整任务链（两边都有 kill、
SELECT_QUEST_REWARD 和 npc-complete），同时把 end NPC 的行 0 对话写成 `started -> reward` 直跳、
`reward` 投影 0，于是行 1/2 永远没有状态；start NPC 还能直接领奖（违反 QE-052 的 owner 收敛）。

#### B. Dredgion 舰长四行族（3711/4711，data_driven_quest）

| quest | 行 0 / 1 / 2 / 3 | 行 0 NPC | 行 1 NPC | 行 2 击杀 | 行 3 报告 NPC（合同 end） |
|---|---|---|---|---|---|
| 3711 | 和 Mias 对话 / 获取德雷得奇安结构情报 / 除掉 DrakanBoss [%8] / 向 Taranis 报告 | 279045 | 730196 术古 | 214823 | 278501 Taranis |
| 4711 | 和 Henir 对话 / 搜集德雷得奇安结构情报 / 清除 DrakanBoss [%8] / 向 Votan 报告 | 279042 | 730196 术古 | 214823 | 278001 Votan |

客户端页链：行 0 的 `select1/select1_1/select1_1_1` → `SETPRO1`；行 1 的 730196
`select2 → select2_1 → SETPRO2`；行 2 击杀 214823；行 3 的 `select3` 报告。legacy 合同
`start_npc_ids=end_npc_ids=278501/278001`、`report_source_status=REWARD`、
`report_open_action=QUEST_SELECT(31) -> DEFAULT_SUCCESS(10002)`、`report_action=SELECT_QUEST_REWARD(1009) -> REWARD`
（奖励页 `SHOW_SELECT_QUEST_REWARD_WINDOW1`）。旧定义在 `started` 上有 `started -> reward` 直跳、
末尾还有一个 `278501/278001 SETPRO2` 的跳行路由，因此行 1/2/3 都没有服务端状态。

### 四十一之二、落点（两族阶梯 + owner 收敛）

- **三行族**：`unaccepted(0) / started(0) / t1(1) / k2(2) / reward(2) / complete(0)`。
  - start NPC 只保留 `NPC_START(selection-sources=unaccepted)`；删除它的 `QUEST_SELECT`、
    `SETPRO1`、kill、`SELECT_QUEST_REWARD` 与 `npc-complete`（接取 NPC 不再能领奖）。
  - end NPC：`started --QUEST_SELECT--> SELECT2`（行 0 页）、`started --SETPRO1--> t1`（写 var0=1）、
    `t1 --kill--> k2`、`k2 --QUEST_SELECT--> SELECT5`（行 2 报告页）、
    `k2 --SELECT_QUEST_REWARD--> reward`（奖励窗）+ `npc-complete`（唯一 reward owner）。
  - 行 1 补 `t1 --QUEST_SELECT--> DEFAULT_SUCCESS` 的进行中反馈，避免击杀行对话无路由。
  - 自愈边：`REWARD && var0==0/1 -> 2`。
- **四行族**：`unaccepted(0) / started(0) / s1(1) / s2(2) / reward(3) / complete(0)`。
  - `279045/279042 --SETPRO1--> s1`（0→1）；`730196 --QUEST_SELECT/SELECT2_1--> s1` 页、
    `730196 --SETPRO2--> s2`（1→2）；`s2 --kill 214823--> reward`（2→3）；删除 `started -> reward`
    直跳与 `278501/278001 SETPRO2` 跳行路由。
  - `reward --QUEST_SELECT(278501/278001)--> DEFAULT_SUCCESS(10002)` 承担合同 `report_open_action`；
    1009 奖励窗由 `npc-complete` 预览展开。
  - 自愈边：`REWARD && var0==0/1/2 -> 3`。
- 两族都**不再允许** `started -> reward` 直跳；三行族额外禁止 start NPC 出现在任何 `target=complete`
  路由上（QE-052 owner 收敛）。

### 四十一之三、验证（2026-09-22）

- **静态**：`xmllint --noout --schema quest_definition.xsd` 5/5 validates；
  `apply_batch37_talk_kill_report_row_ladder.py --check` 幂等 5/5 OK。
- **全库行号审计**：`MISSING_TAIL_ROWS 62 -> 57`（-5）、`ROW_BEHIND 159 -> 154`（-5）、
  `ROW_WITHOUT_STATE 491 -> 486`（-5）、`ROW_ALIGNED 2691 -> 2696`（+5）、
  `ROW_STATE_ALIGNED 2463 -> 2468`（+5）；5 个任务全部 `ALIGNED + ROW_ALIGNED + ROW_STATE_ALIGNED +
  visible=行全量 + recovery=True`，逐任务前后见 [batch37-evidence.tsv](batch37-evidence.tsv)。
- **Maven（授权后执行）**：36 个 reward/row/ladder/owner/catalog 测试类全绿（批次 36 的 35 类 + 新增
  `Batch37TalkKillReportRowLadderContractTest` 4 例：每行一个状态 / 三行族 start-end 分离与 owner 收敛 /
  四行族 Mias+术古+击杀+报告链 / 0..领奖行自愈边 + planner 收敛）；
  `PRODUCTION_COMPILE_OK=6191 / FAILURES=0 / INTERACTION_OBJECT_FAILURES=0 / WHITELIST_VIOLATIONS=0`。
- **客户端实机 PENDING_CLIENT**：① 26905/26906/26908 从 Aegir/Nerthus 接取后任务书停在行 0
  （和 Gangleri/Tyr/Svafnir 对话）；② 对话结束切行 1，击杀对应 Dark Raider 后切行 2（报告）；
  ③ 向同一 NPC 报告开奖励窗领奖；④ 3711/4711 接取后停在行 0（Mias/Henir），对话后切行 1（情报），
  与 730196 术古对话后切行 2（击杀），击杀 214823 后切行 3（向 Taranis/Votan 报告）；
  ⑤ 旧存档（`REWARD + var0=0/1(/2)`）登录或切图后应直接落在领奖行；⑥ 接取 NPC 不应能直接领奖。

### 四十一之四、边界与后续

- 本批按 legacy 合同区分“接取 NPC”和“任务书报告 NPC”；仅当合同明确给出两个不同 `start_npc_ids/end_npc_ids`
  时才允许拆 owner（26905/26906/26908）。3711/4711 的 start=end 同为报告 NPC，owner 保持不变。
- 剩余 `MISSING_TAIL_ROWS 57`（含 6 个已登记例外）的下一批候选：镜像可直接取模板的 **11118**
  （镜像 1118 ALIGNED）、**14012/24012**（镜像 4012 ALIGNED）、**14053/24053**（镜像 4053 ALIGNED）、
  **26905/26906/26908 已完成**；此外还有 `2411/2922/4732`、`3013/3217/4217`、`11072/21081/24150`、
  `13918/23918`、`18213/28213`、`80298/80299/80304/80305`（活动“去见单身部队成员”族）等。
- 其它挂账不变：`STATES_BEYOND_ROWS 2620`、`INTERIOR_GAP 263`、`MISSING_LAST_ROW 77`、
  `section0 residual 837`（沿用批次 35 专项口径）、客户端隔离族 8 个、3 例 HEAD 即红。

## 四十二、批次 38：活动阵营选择族（80298/80299/80304/80305，2026-09-22）

### 四十二之一、族判据与证据（客户端任务书 + quest_data 前置条件）

批次 37 之后 `MISSING_TAIL_ROWS 57`（含 6 个已登记例外）。本批收口活动“Lover or Loner? /
Bitter or Sweet?”四个同构任务，客户端 `quest_summary` 都是三行、槽位 `%0/%3/%6`：

| quest | 阵营 | 行 0 | 行 1（完成索引 1） | 行 2（完成索引 2） | 后续任务 |
|---|---|---|---|---|---|
| 80298 | ELYOS | 坦坦荡荡的回答到底是有恋人，还是孤单一人吧！ | 去见坠入爱河的术古 | 去见单身部队成员 | 80300（mode 1）/ 80301（mode 2） |
| 80299 | ELYOS | 同上 | 同上 | 同上 | 80302（mode 1）/ 80303（mode 2） |
| 80304 | ASMODIANS | 同上 | 同上 | 同上 | 80306（mode 1）/ 80307（mode 2） |
| 80305 | ASMODIANS | 同上 | 同上 | 同上 | 80308（mode 1）/ 80309（mode 2） |

客户端页链（四个任务相同）：
`select_none(4762)` 的 `SELECT1_1` → `select1_1(1012)` 的 `SELECT2_1`/`SELECT2_2` →
`select2_1(1353)`/`select2_2(1438)`/`select2_1_1(1354)` 的 `SELECT3_1`/`SELECT3_2` →
`select3_1(1694)` 的 `SETPRO1` / `select3_2(1779)` 的 `SETPRO2`。
`docs/quest/client-dialog-mapping/quest-dialog-action-details.csv` 记录这四个任务是
`HACTION_SELECT1_1 → HACTION_SELECT2_1/2 → HACTION_SELECT3_1/2 → HACTION_SETPRO1/2`
的活动页链；`client_dialog_contract.tsv` 也把 1012/1353/1354/1438/1694/1779 六个页完整登记在
`80298/80299/80304/80305` 名下。

零售 `Quest_unpacked/quest.xml` 的 `finished_quest_cond1` 给出分流硬证据：
`80300/80302/80306/80308 = Q80298/Q80299/Q80304/Q80305:1`，
`80301/80303/80307/80309 = ...:2`；服务端迁移后的 `start-conditions`
已经保留为 `reward-mode=1/2`。因此 `SETPRO1`/`SETPRO2` 不是“接受”或“中间对话行”，
而是完成奖励索引（分支标志）1/2 的写入点。

旧定义把两行塌陷成：
`unaccepted --SETPRO2--> reward(var0=0)`、`unaccepted --SETPRO1--> started(var0=0)`，
于是审计判成 `MISSING_TAIL_ROWS | ROW_BEHIND | ROW_WITHOUT_STATE(1 2) | visible=0`。

### 四十二之二、落点（分支行 = REWARD 投影，完成索引 = 后续接取标志）

- **节点**：`unaccepted(0) / started(START,0) / reward1(REWARD,1) / reward(REWARD,2) / complete(0)`。
  行 0 由 `started` 承载；行 1、行 2 是两个互斥的 REWARD 投影，`reward` 仍作为审计和
  `npc-complete` 的末行 owner。
- **页链**：`unaccepted` 与 `started` 都保留完整客户端页链（未先走通用接取窗口时，
  `SETPRO1/SETPRO2` 带 `start-eligible` 跨过 NONE 边界；已接取后走 `started` 的同一页链）。
  `started --SETPRO1--> reward1` 显示 `SHOW_SELECT_QUEST_REWARD_WINDOW1`；
  `started --SETPRO2--> reward` 显示 `SHOW_SELECT_QUEST_REWARD_WINDOW2`。
- **完成索引**：两个分支各用一条 `npc-complete`：
  `reward1 + complete-reward-index=1`、`reward + complete-reward-index=2`，动作覆盖
  `SELECTED_QUEST_REWARD1..SELECTED_QUEST_NOREWARD`，固定奖励仍为物理组 0 的 EXP 1600。
  `QuestXmlBlockExpander.rewardGroup` 对“单物理奖励组 + 非零完成索引”保留状态语义，
  不会把索引 1/2 误当成物理奖励档位。
- **窗口防重映射**：显式补 `reward1 --USE_OBJECT/SELECT_QUEST_REWARD--> window1`、
  `reward --USE_OBJECT/SELECT_QUEST_REWARD--> window2` 四条预览路由；否则
  `restoreRewardPreviewContract` 会按 `rewardWindowForTier(1/2)` 自动推成窗口 2/3，
  其中窗口 3 不在本任务的客户端 HTML 里。
- **自愈边**：`REWARD && var0==0`（旧 SETPRO2 单人分支的唯一旧落盘值）无 source
  `enter-world` → `reward(var0=2)`；`REWARD && var0==1` 已天然就是 `reward1` 行，不做降级覆盖。

### 四十二之三、验证（2026-09-22）

- **静态**：`xmllint --noout --schema quest_definition.xsd` 4/4 validates；
  `apply_batch38_branch_choice_reward_index.py --apply` 后 `--check` 幂等 4/4 OK。
- **全库行号审计（脚本 [11] 节已改为批次 38，并补登批次 37/38 修复族）**：
  `MISSING_TAIL_ROWS 57 -> 53`（-4）、`ROW_BEHIND 154 -> 150`（-4）、
  `ROW_WITHOUT_STATE 486 -> 482`（-4）、`ROW_ALIGNED 2696 -> 2700`（+4）、
  `ROW_STATE_ALIGNED 2468 -> 2472`（+4）；4 个任务全部
  `ALIGNED + ROW_ALIGNED + ROW_STATE_ALIGNED + visible=0 1 2 + recovery=True`，逐任务前后见
  [batch38-evidence.tsv](batch38-evidence.tsv)。
- **Maven（授权后执行）**：新增 `Batch38BranchChoiceRewardIndexContractTest`（4 例：
  每行一个状态 + 两个 REWARD 分支 / 客户端页链和窗口 1/2 / `CompleteQuest(1/2)` 与后续
  `reward-mode=1/2` / 旧 `REWARD var0=0` 自愈）4/4 绿；同批回归
  `Batch36EventRowLadderContractTest` 5/5、`Batch37TalkKillReportRowLadderContractTest` 4/4；
  `ProductionCatalogWhitelistVerificationTest` 1/1，`PRODUCTION_COMPILE_OK=6191 / FAILURES=0 /
  INTERACTION_OBJECT_FAILURES=0 / WHITELIST_VIOLATIONS=0`；`QuestPageButtonAuditTest`(2)、
  `QuestHandoverContinuationAuditTest`(2)、`QuestE2eInfrastructureTest`(41)、
  `AcceptAndConfirmationEntryContractTest`(2) 共 51 例绿。
- **既有红登记（非本批引入）**：`QuestInteractionObjectCatalogTest#productionQuestUseItemTalkRoutesDeclareActionEligibility`
  仍列出 `13809/23809/30504/30554` 共 8 条 `TALK_TO_NPC dialog=-1` 路由缺少同 source 的
  `CanAct(ACTION_ITEM_USE)` 资格；四个本批任务（80298/80299/80304/80305）不在该清单内，
  工作树与 HEAD 对这 8 条路由均无 `can-act`，故按既有债务登记，不在本批扩围修改。
- **客户端实机 PENDING_CLIENT**：① 80298/80299（天族）与 80304/80305（魔族）接取后任务书停在行 0；
  ② 走 `select1_1 → select2_1 → select3_1 → SETPRO1`，任务书切行 1 并打开奖励窗口 1，确认后完成，
  应能接取 `80300/80302/80306/80308`（reward-mode=1）；③ 重复选择单身分支
  `select2_2 → select3_2 → SETPRO2`，任务书切行 2 并打开奖励窗口 2，确认后应能接取
  `80301/80303/80307/80309`（reward-mode=2）；④ REWARD 态再次点 NPC 用 `USE_OBJECT`/1009
  重开窗口时，情侣分支必须是窗口 1、单身分支必须是窗口 2，不能出现窗口 3；
  ⑤ 旧存档 `REWARD + var0=0` 登录或切图后应自愈到单身行 2；⑥ 完成一个分支后另一分支的后续任务不应可接取。

### 四十二之四、边界与后续

- 本族行 1/2 是互斥分支而非顺序阶梯；审计口径只要求每个客户端行号都有一个 START/REWARD
  状态，本批用 `reward1`/`reward` 两个 REWARD 节点满足，未把两条分支误接成可顺序全走。
- `complete-reward-index=1/2` 是后续任务的接取资格标志，不是物理奖励档位；当前元数据仍是
  单物理组 EXP 1600。后续若增加奖励组，必须同步重验 `rewardGroup`、窗口预览和 `reward-mode` 合同。
- 剩余 `MISSING_TAIL_ROWS 53`（其中 47 个待逐族收口 + 6 个已登记例外）。其它挂账不变：
  `STATES_BEYOND_ROWS 2620`、`INTERIOR_GAP 263`、`MISSING_LAST_ROW 77`、`section0 residual 837`
  （沿用批次 35 专项口径）、客户端隔离族 8 个
  （3959/4963/16984/18706/20015/26984/28706/29706）；已知 HEAD/既有红除原有 3 例外，
  另有本次登记的 `QuestInteractionObjectCatalogTest` 8 条资格缺口。

## 四十三、批次 39：三行「交付物品 → 对话/招供 → 向最终 NPC 报告并领奖」族（3013/3217/4217）

### 四十三之一、族判据与证据（2026-09-22）

本批收口 47 个残留下来的第一族：**3013（天族 23 级）/ 3217（天族 43 级，副本组队）/
4217（魔族 43 级，副本组队）**。三家任务书结构完全同型——三行、槽位 `%0/%3/%6`：

| 任务 | 行 0 | 行 1 | 行 2 | 交付/对话 NPC | 最终报告 NPC |
|---|---|---|---|---|---|
| 3013 | 搜 DigCherubimL/Cherubim2Wp 把撕碎信纸交给 Shugo_LF2a_1 | 让 Hecuba 招供 | 向 Shugo_LF2a_1 报告结果 | 798132 交付、798146 招供 | 798132 |
| 3217 | 找回侦察报告书交给 Nasuri | 和 Nasuri 对话 | 和 Gorgos 对话 | 798335 | 204590 |
| 4217 | 找回旧包袱交给 Parten | 和 Parten 对话 | 和 Savrina 对话 | 798336 | 204773 |

证据链：

- **客户端任务书行**（`Dialogs/quest_q3013.html` / `QUEST_Q3217.html` / `QUEST_Q4217.html`）：
  quest_summary 三行分别绑定 `visible="[%0]"`、`[%3]`、`[%6]`，与批次 33/38 锁定的
  “槽位 = 3 × 状态号”一致，即真实状态是 0/1/2。
- **客户端页链**：
  - 三家都有 `select1`（唯一按钮 `HACTION_CHECK_USER_HAS_QUEST_ITEM`）→
    `check_user_item_ok` / `check_user_item_fail`；`select_success` 的唯一按钮是
    `HACTION_SELECT_QUEST_REWARD`，映射 `HtmlPages.xml` 的 `DEFAULT_SUCCESS(10002)`。
  - 3013 行 1 是 Hecuba 的三段链：`select2`(1352) → `HACTION_SELECT2_1` → `select2_1`(1353) →
    `HACTION_SELECT2_1_1` → `select2_1_1`(1354) → `HACTION_SET_SUCCEED`；行 0 的
    `check_user_item_ok` 按钮是“结束对话”（`HACTION_FINISH_DIALOG`）。
  - 3217/4217 行 1 就在交付 NPC 的 `check_user_item_ok` 页上：唯一按钮是 `HACTION_SET_SUCCEED`。
- **客户端 NPC 名表**（`npcs_unpacked/client_npcs_npc.xml` + `strings_unpacked/client_strings_dic_people.xml`）：
  `STR_NPC_Gorgos = 204590`、`STR_NPC_Savrina = 204773`、`STR_NPC_Hecuba = 798146`、
  `STR_NPC_Nasuri = 798335`、`STR_NPC_Parten = 798336`——证实 3217 行 2 的对话 owner 是 204590
  而不是 798335，4217 行 2 是 204773 而不是 798336（4217 正文仍写 Anita，属客户端历史文本差异，
  任务书行号以 quest_summary 的 Savrina 为准）。
- **旧定义错位**：三家都是 `started(0) -> reward(0)` 直跳；3217/4217 还在“交付 NPC”和
  “报告 NPC”上各挂一份 `NPC_REPORT + SELECT_QUEST_REWARD`（同一进度两条领奖口），3013 把 798132
  的报告页写成 Hecuba 的 `SELECT2`，并把 `SET_SUCCEED` 挂在两个 NPC 上直跳 reward。
  审计因此判成 `MISSING_TAIL_ROWS | ROW_BEHIND | ROW_WITHOUT_STATE(1 2) | visible=0`。

### 四十三之二、落点（行阶梯 = START/START/REWARD）

- **节点**：`unaccepted(0) / started(START,0) / s1(START,1) / reward(REWARD,2) / complete(0)`。
- **行 0 → 行 1**：交付 NPC 的 `CHECK_USER_HAS_QUEST_ITEM`（`priority=0`，条件
  `has-item` 1×182208008 / 3×182209095 / 3×182209110）`started -> s1`，动作
  `set-variable var0=1` + `remove-item`，落点页 `CHECK_USER_ITEM_OK`；`priority=1` 的失败分支
  保持 `started -> started` 并显示 `CHECK_USER_ITEM_FAIL`。玩家重新对话走 `started` 的
  `QUEST_SELECT -> SELECT1`。
- **行 1 → 行 2**：Hecuba（3013）/ 交付 NPC（3217/4217）的 `SET_SUCCEED` `s1 -> reward`，
  客户端语义是 `check_user_item_ok` 页的结束按钮；3013 的 `select2` 三段链用
  `s1` 自环 + `SHOW_QUEST_PAGE` 逐页下发。
- **行 2（报告领奖）**：`NPC_REPORT` 改为挂在真正的报告 NPC（3013/798132、3217/204590、
  4217/204773）的 `reward -> reward`，展开成 `QUEST_SELECT -> DEFAULT_SUCCESS`（客户端
  `select_success`）+ `SELECT_QUEST_REWARD -> SHOW_SELECT_QUEST_REWARD_WINDOW1`；`npc-complete`
  也收敛到该 NPC（3013 固定索引 0/1/2 + 选择 3/4，3217/4217 固定 0/1）。
- **preview 收敛**：`preview` 只保留 `USE_OBJECT`。`SELECT_QUEST_REWARD` 已由 reward 态
  `NPC_REPORT` 展开提供，若 preview 再声明一次，同 NPC 同动作会触发
  `AMBIGUOUS_TRANSITION`（本批编译实测）。
- **自愈边**：`REWARD && var0==0`（旧直跳唯一落盘值，物品已被收走）无 source `enter-world`
  → `reward(var0=2)`，登录/切图后任务书落到“报告领奖”行。

### 四十三之三、验证（2026-09-22）

- **静态**：`xmllint --noout --schema quest_definition.xsd` 3/3 validates；
  `apply_batch39_turn_in_talk_report_rows.py --apply` 后 `--check` 幂等 3/3 OK；
  `git diff --check` 干净。
- **全库行号审计（脚本 [11] 节已改为批次 39，并补登本族）**：
  `MISSING_TAIL_ROWS 53 -> 50`（-3）、`ROW_BEHIND 150 -> 147`（-3）、
  `ROW_WITHOUT_STATE 482 -> 479`（-3）、`ROW_ALIGNED 2700 -> 2703`（+3）、
  `ROW_STATE_ALIGNED 2472 -> 2475`（+3）；3 个任务全部
  `ALIGNED + ROW_ALIGNED + ROW_STATE_ALIGNED + visible=0 1 2 + recovery=True`，
  逐任务前后见 [batch39-evidence.tsv](batch39-evidence.tsv)。
- **Maven（授权后执行）**：新增 `Batch39TurnInTalkReportRowContractTest`（5 例：每行一个状态 /
  交付行推进到行 1 并回收物品（含失败页）/ `SET_SUCCEED` 收口行 1 且行 2 由报告 NPC 打开窗口 1 +
  完成索引 / 交付页与 Hecuba 三段链路由 / 旧 `REWARD var0=0` 自愈）5/5 绿；同批回归
  `Batch38BranchChoiceRewardIndexContractTest` 4/4、`ProductionCatalogWhitelistVerificationTest` 1/1
  （`PRODUCTION_COMPILE_OK=6191 / FAILURES=0 / INTERACTION_OBJECT_FAILURES=0 /
  WHITELIST_VIOLATIONS=0`）、`QuestPageButtonAuditTest`(2)、`QuestHandoverContinuationAuditTest`(2)、
  `QuestE2eInfrastructureTest`(41)、`AcceptAndConfirmationEntryContractTest`(2)、
  `QuestClientContractGateTest` 全绿。
- **既有红登记（非本批引入）**：`QuestInteractionObjectCatalogTest#productionQuestUseItemTalkRoutesDeclareActionEligibility`
  仍为同一批 8 条（`13809/23809/30504/30554`）`TALK_TO_NPC dialog=-1` 路由缺少同 source 的
  `CanAct(ACTION_ITEM_USE)`；本批三个任务（3013/3217/4217）不在清单内，未扩围修改。
- **客户端实机 PENDING_CLIENT**：① 接取后任务书停在行 0，交付收集物后切行 1 并显示
  `check_user_item_ok`；② 3217/4217 点“结束对话”（SET_SUCCEED）切行 2，3013 需走完
  Hecuba 的 `select2 → SELECT2_1 → SELECT2_1_1 → SET_SUCCEED` 切行 2；③ 行 2 找
  Gorgos/Savrina/Shugo_LF2a_1 打开 `select_success`，点“报告”弹出奖励窗口 1 并完成；
  ④ 旧存档 `REWARD + var0=0` 登录或切图后自愈到行 2；⑤ 3217/4217 不应再出现
  “交付 NPC 处也能直接领奖”的双口。

### 四十三之四、边界与后续

- 三家的行 1 是“继续对话/招供”行，不是**第二个**可领奖口；3217/4217 旧定义在交付 NPC 上
  多挂的一份领奖合同本批删除，README/legacy 合同若仍记录两条领奖口应视为过期。
- 4217 的对话正文提到 Anita，但任务书行 2 与客户端 NPC 名表都是 Savrina(204773)；
  行号 owner 以 quest_summary + `client_npcs_npc.xml` 为准，文本差异不改行归属。
- 剩余 `MISSING_TAIL_ROWS 50`（44 个待逐族收口 + 6 个已登记例外）。其它挂账不变：
  `STATES_BEYOND_ROWS 2620`、`INTERIOR_GAP 263`、`MISSING_LAST_ROW 73`、客户端隔离族 8 个
  （3959/4963/16984/18706/20015/26984/28706/29706）；既有红除登记例外另有本次沿用的
  `QuestInteractionObjectCatalogTest` 8 条资格缺口。

## 四十四、批次 40：三行「接取 → 行 0 NPC → 行 1 NPC → 行 2 NPC 报告领奖」族（11072/21081/24150）

### 四十四之一、族判据与证据（2026-09-22）

本批收口第二族三行任务：**11072（天族 50 级）/ 21081（魔族 50 级，Gelkmaros 武器补给）/
24150（魔族，贝鲁斯兰要塞崩溃）**。三家客户端任务书都是三行、槽位 `%0/%3/%6`，且页链完全同型：

| 任务 | 接取 NPC | 行 0 NPC | 行 1 NPC | 行 2 NPC（报告/领奖） |
|---|---|---|---|---|
| 11072 | Seneca 798937 | Borriello 798907 | Delus 798960 | Seneca 798937 |
| 21081 | Hler 799225 | Agovard 799332 | Renato 799217 | Sepsi 799202 |
| 24150 | Nerthus 204702 | Bestla 204733 | Horu 204734 | Nerthus 204702 |

证据链：

- **客户端任务书行**（`Dialogs/10000_19999/quest_q11072.html`、
  `Dialogs/20000_29999/quest_q21081.html`、`quest_q24150.html`）：
  quest_summary 三行绑定 `[%0]` / `[%3]` / `[%6]`，行文本逐行点名上表的 NPC。
- **客户端页链**（三家一致）：
  `select1`（11072 还有 `select1_1`，按钮 `HACTION_SELECT1_1`）→ `ask_quest_accept` →
  `quest_accept_1`；
  行 0：`select2`（按钮 `HACTION_SELECT2_1`）→ `select2_1`（按钮 `HACTION_SETPRO1`）；
  行 1：`select3`（按钮 `HACTION_SELECT3_1`）→ `select3_1`（按钮 `HACTION_SETPRO2`）；
  行 2：`select5`（按钮 `HACTION_SELECT_QUEST_REWARD`）→ `select_quest_reward1` 领奖页。
  页 id 与 `HtmlPages.xml` 一致：SELECT2=1352 / SELECT2_1=1353 / SELECT3=1693 / SELECT3_1=1694 /
  SELECT5=2375。
- **客户端 NPC 名表**（`client_npcs_npc.xml`）：Borriello=798907、Delus=798960、Seneca=798937、
  Hler=799225、Agovard=799332、Renato=799217、Sepsi=799202、Bestla=204733、Horu=204734、
  Nerthus=204702；与 legacy 合同的 `start_npc_ids`/`end_npc_ids`
  （11072: 798937→798937、21081: 799225→799202、24150: 204702→204702）完全对上。
- **旧定义错位**：三家都是“每个任务 NPC 都能接取 + 都能领奖”的扁平模板，只保留
  `SELECT2 / SELECT2_1 / SETPRO1 -> reward`；`SELECT3 / SELECT3_1 / SETPRO2` 与 `SELECT5`
  完全缺失，行 1/行 2 永远拿不到状态；21081/24150 还会在行 0 的 NPC 处直接进入领奖态。
  审计判成 `MISSING_TAIL_ROWS | ROW_BEHIND | ROW_WITHOUT_STATE(1 2) | visible=0`。

### 四十四之二、落点（行阶梯 = START/START/REWARD + owner 收敛）

- **节点**：`unaccepted(0) / started(START,0) / s1(START,1) / reward(REWARD,2) / complete(0)`。
- **接取**：只在接取 NPC 上保留 `NPC_START`（`start-page=SELECT1`），11072 额外保留
  `unaccepted SELECT1_1 -> SELECT1_1` 第二页；21081 的 `accept-actions` 发补给品发放令
  182214017、24150 的发奥德增强装置 182215460 均留在接取 NPC 上。
- **行 0 → 行 1**：行 0 NPC 的 `QUEST_SELECT -> SELECT2`、`SELECT2_1 -> SELECT2_1`、
  `SETPRO1 -> s1`（`LEVEL_AND_VISIBILITY_REFRESH`）。
- **行 1 → 行 2**：行 1 NPC 的 `QUEST_SELECT -> SELECT3`、`SELECT3_1 -> SELECT3_1`、
  `SETPRO2 -> reward`。
- **行 2**：行 2 NPC 的 `NPC_REPORT source=reward target=reward page=SELECT5` 展开成
  `QUEST_SELECT -> select5` + `SELECT_QUEST_REWARD -> SHOW_SELECT_QUEST_REWARD_WINDOW1`；
  `npc-complete` 收敛到该 NPC（11072 固定索引 0/1/2、21081 固定 0、24150 固定 0/1/2/3 +
  选择 4/5）；preview 只保留 `USE_OBJECT`（避免与 `NPC_REPORT` 的 SELECT_QUEST_REWARD 撞车）。
- **自愈边**：`REWARD && var0==0` 无 source `enter-world` → `reward(var0=2)`。

### 四十四之三、验证（2026-09-22）

- **静态**：`xmllint --noout --schema quest_definition.xsd` 3/3 validates；
  `apply_batch40_three_npc_talk_ladder.py --apply` 后 `--check` 幂等 3/3 OK；
  `git diff --check` 干净。
- **全库行号审计（脚本 [11] 节已改为批次 40，并补登本族）**：
  `MISSING_TAIL_ROWS 50 -> 47`（-3）、`ROW_BEHIND 147 -> 144`（-3）、
  `ROW_WITHOUT_STATE 479 -> 476`（-3）、`ROW_ALIGNED 2703 -> 2706`（+3）、
  `ROW_STATE_ALIGNED 2475 -> 2478`（+3）；3 个任务全部
  `ALIGNED + ROW_ALIGNED + ROW_STATE_ALIGNED + visible=0 1 2 + recovery=True`，
  逐任务前后见 [batch40-evidence.tsv](batch40-evidence.tsv)。
- **Maven（授权后执行）**：新增 `Batch40ThreeNpcTalkLadderContractTest`（5 例：每行一个状态 /
  接取只留在接取 NPC / 行 owner 页链与 SETPRO1/SETPRO2 推进 + 行 2 开窗口 1 / 完成只在行 2 NPC
  且行 0/1 NPC 无领奖口 / 旧 `REWARD var0=0` 自愈）5/5 绿；同批回归
  `Batch39TurnInTalkReportRowContractTest` 5/5、`Batch38BranchChoiceRewardIndexContractTest` 4/4、
  `ProductionCatalogWhitelistVerificationTest` 1/1（`PRODUCTION_COMPILE_OK=6191 / FAILURES=0 /
  INTERACTION_OBJECT_FAILURES=0 / WHITELIST_VIOLATIONS=0`）、`QuestPageButtonAuditTest`(2)、
  `QuestHandoverContinuationAuditTest`(2)、`QuestE2eInfrastructureTest`(41)、
  `AcceptAndConfirmationEntryContractTest`(2)、`QuestClientContractGateTest` 全绿。
- **既有红登记（非本批引入）**：`QuestInteractionObjectCatalogTest` 的同一批 8 条
  `TALK_TO_NPC dialog=-1` 资格缺口（13809/23809/30504/30554），本批任务不在清单内。
- **客户端实机 PENDING_CLIENT**：① 只允许接取 NPC 开启接取对话（行 0 NPC 不再能接取）；
  ② 接取后行 0 提示去对应 NPC，`select2 → select2_1 → SETPRO1` 后任务书切行 1；
  ③ `select3 → select3_1 → SETPRO2` 后切行 2；④ 行 2 NPC 的 `select5` 点“报告/提交”弹出
  奖励窗口 1 并完成（24150 需能选择索引 4/5 的奖励）；⑤ 行 0/行 1 的 NPC 处不应再出现领奖窗口；
  ⑥ 旧存档 `REWARD + var0=0` 登录或切图后自愈到行 2。

### 四十四之四、边界与后续

- **21081 的可选武器未展开（既有差异，本批只登记不扩围）**：metadata 有 13 个
  `SELECTABLE_ITEM`（含 6 条 `class-rewards`），旧定义与本次收口都沿用
  `actions="SELECTED_QUEST_REWARD1..SELECTED_QUEST_NOREWARD"`（只发固定 EXP），客户端
  `select_quest_reward1` 的“选择一件武器”因此拿不到选择结果；是否改成显式 choice 或
  CLASS/CHOICE reported-reward 合同需要单独一轮奖励语义取证，不与行号收口混做。
- 11072/24150 的行 2 NPC 与接取 NPC 是同一人，路由靠 source 节点（unaccepted vs reward）区分，
  不能再把 `SELECT1` 链挂到 reward 态。
- 剩余 `MISSING_TAIL_ROWS 47`（41 个待逐族收口 + 6 个已登记例外）。其它挂账不变：
  `STATES_BEYOND_ROWS 2620`、`INTERIOR_GAP 263`、`MISSING_LAST_ROW 73`、客户端隔离族 8 个；
  既有红沿用 `QuestInteractionObjectCatalogTest` 8 条资格缺口。

## 四十五、批次 41：潘盖亚要塞战三行族（14220 天族 / 24220 魔族）

### 四十五之一、族判据与证据（2026-09-22）

| 任务 | 接取/报告 NPC | 行 0 NPC | 行 1 NPC（潘盖亚情报员） |
|---|---|---|---|
| 14220（天族） | Carley 802540 | Astarin 802541 | 4 个变体 802544/802545/802546/802547 |
| 24220（魔族） | Leaivink 802542 | Krondel 802543 | 同上 |

- **客户端任务书行**（`Dialogs/10000_19999/quest_q14220.html`、
  `Dialogs/20000_29999/quest_q24220.html`）：三行绑定 `[%0]`/`[%3]`/`[%6]`，
  行 1 是“参加潘盖亚要塞战，和 `STR_DIC_N_GAb1_Ag_all` 对话”。
- **客户端页链**（两任务一致）：`select_none`（按钮 QUEST_ACCEPT_SIMPLE/QUEST_REFUSE_SIMPLE）
  → 行 0 的 `select1`（唯一按钮 SETPRO1，文本“参加要塞战的时候，请一定要去见见 Ag_all”）
  → 行 1 的 `select2`（按钮 SELECT2_1）→ `select2_1`（按钮 SETPRO2，文本“点头”）
  → 行 2 的 `select_success`(10002)（按钮 SELECT_QUEST_REWARD）→ `select_quest_reward1`。
- **owner 证据**：
  - `client_npcs_npc.xml`：GAb1_Carley_E=802540、GAb1_Astarin_E=802541、GAb1_Leaivink_E=802542、
    GAb1_Krondel_E=802543、GAb1_01_BelosAg01_E=802544、02_AspidaAg01_E=802545、
    03_AthantosAg01_E=802546、04_DysilonAg01_E=802547。
  - `client_strings_dic_etc.xml` 的 `STR_DIC_N_GAb1_Ag_all`（“潘盖亚情报员”）明确说明按要塞战
    入口位置存在四个变体（Belos/Aspida/Athantos/Dysilon），即 802544-802547（race=BROWNIE，中立），
    两个任务共用。
  - 行 0/行 2 的归属由 quest_complete 文本锁定：14220“荣誉殿堂中的卡勒莱伊…去见见阿斯塔林”、
    24220“荣誉会堂中的雷斌克…去见见克伦德尔”——卡勒莱伊/雷斌克就是接取 NPC，也正是行 2 的报告人。
- **旧定义错位**：`NPC_REPORT 802540/802542 started->reward page=SELECT2` + `SETPRO2 -> reward`
  直跳，把行 1 的 select2 链错误地挂在接取 NPC 上；行 0 的 Astarin/Krondel 与 4 个情报员变体
  完全没有路由。审计判成 `MISSING_TAIL_ROWS | ROW_BEHIND | ROW_WITHOUT_STATE(1 2) | visible=0`。

### 四十五之二、落点（行阶梯 = START/START/REWARD）

- **节点**：`unaccepted(0) / started(START,0) / s1(START,1) / reward(REWARD,2) / complete(0)`。
- **接取**：接取 NPC 的 `NPC_START`（`start-page=SELECT_NONE`，保留 `selection-sources`）。
- **行 0 → 行 1**：行 0 NPC 的 `QUEST_SELECT -> SELECT1`、`SETPRO1 -> s1`（带
  `LEVEL_AND_VISIBILITY_REFRESH`）。
- **行 1 → 行 2**：4 个情报员变体各一组 `QUEST_SELECT -> SELECT2`、`SELECT2_1 -> SELECT2_1`、
  `SETPRO2 -> reward`（不同 NPC，编译无歧义）。
- **行 2**：接取 NPC 的 `NPC_REPORT source=reward target=reward page=DEFAULT_SUCCESS`
  展开 `QUEST_SELECT -> select_success` + `SELECT_QUEST_REWARD -> 奖励窗口 1`；
  `npc-complete` 收敛到该 NPC（固定索引 0/1，动作范围沿用迁移前定义）；preview 只保留 `USE_OBJECT`。
- **自愈边**：`REWARD && var0==0` 无 source `enter-world` → `reward(var0=2)`。

### 四十五之三、验证（2026-09-22）

- **静态**：`xmllint --noout --schema quest_definition.xsd` 2/2 validates；
  `apply_batch41_pangaia_fortress_three_row_ladder.py --apply` 后 `--check` 幂等 2/2 OK。
- **全库行号审计（脚本 [11] 节已改为批次 41，并补登本族）**：
  `MISSING_TAIL_ROWS 47 -> 45`（-2）、`ROW_BEHIND 144 -> 142`（-2）、
  `ROW_WITHOUT_STATE 476 -> 474`（-2）、`ROW_ALIGNED 2706 -> 2708`（+2）、
  `ROW_STATE_ALIGNED 2478 -> 2480`（+2）；两个任务全部
  `ALIGNED + ROW_ALIGNED + ROW_STATE_ALIGNED + visible=0 1 2 + recovery=True`，
  逐任务前后见 [batch41-evidence.tsv](batch41-evidence.tsv)。
- **Maven（授权后执行）**：新增 `Batch41PangaiaFortressRowContractTest`（5 例：每行一个状态 /
  接取页与行 0 推进 / 4 个情报员变体都能推进行 1 / 行 2 报告与窗口 1 只在接取 NPC 上 /
  旧 `REWARD var0=0` 自愈）5/5 绿；同批回归 `Batch40ThreeNpcTalkLadderContractTest` 5/5、
  `Batch39TurnInTalkReportRowContractTest` 5/5、`ProductionCatalogWhitelistVerificationTest` 1/1
  （`PRODUCTION_COMPILE_OK=6191 / FAILURES=0 / INTERACTION_OBJECT_FAILURES=0 /
  WHITELIST_VIOLATIONS=0`）、`QuestPageButtonAuditTest`(2)、`QuestHandoverContinuationAuditTest`(2)、
  `QuestE2eInfrastructureTest`(41)、`AcceptAndConfirmationEntryContractTest`(2)、
  `QuestClientContractGateTest` 全绿。
- **既有红登记（非本批引入）**：`QuestInteractionObjectCatalogTest` 同一批 8 条
  `TALK_TO_NPC dialog=-1` 资格缺口（13809/23809/30504/30554），本批不在清单内。
- **客户端实机 PENDING_CLIENT**：① 接取时只看到 `select_none`，行 0 提示去见 Astarin/Krondel；
  ② 行 0 对话点“结束对话”（SETPRO1）后任务书切行 1；③ 在要塞战入口找到对应变体的情报员
  （Belos/Aspida/Athantos/Dysilon 之一），`select2 → select2_1 → 点头`（SETPRO2）后切行 2；
  ④ 回接取 NPC 对话（`select_success`）点“报告结果”弹出奖励窗口 1 并完成；
  ⑤ 行 0 的 Astarin/Krondel 处不应出现领奖窗口；⑥ 旧存档 `REWARD + var0=0` 登录或切图后自愈到行 2。

### 四十五之四、边界与后续

- 行 1 的“参加潘盖亚要塞战”本身是战场入场行为，本批只收口对话/状态阶梯；要塞战的入场与
  战绩判定不在本批范围，若后续要做需要单独的门禁与实例取证。
- 情报员按入口位置有 4 个中立变体，本批给 4 个变体都挂了同一套页链，避免只在某一个营地可推进。
- 剩余 `MISSING_TAIL_ROWS 45`（39 个待逐族收口 + 6 个已登记例外）。其它挂账不变：
  `STATES_BEYOND_ROWS 2620`、`INTERIOR_GAP 263`、`MISSING_LAST_ROW 73`、客户端隔离族 8 个；
  既有红沿用 `QuestInteractionObjectCatalogTest` 8 条资格缺口。

## 四十六、批次 42：布鲁斯特豪宁献花族 4033（A Bloom in Brusthonin）

### 四十六之一、族判据与证据（2026-09-22）

- **客户端任务书行**（`Dialogs/QUEST_Q4033.html`）：三行绑定 `[%0]`/`[%3]`/`[%6]`：
  行 0「采集 DF2B_herb_d_n_c_40a 并交给 Heintz[collectitem]」、行 1「把花环献给
  OBJ_DF2A_Tombstone_Q4033」、行 2「与 Heintz 对话」。
- **客户端页链**：接取 `select1`（按钮 SELECT1_1）→ `select1_1`（ASK_QUEST_ACCEPT）→
  `ask_quest_accept` → `quest_accept_1`；行 0 `select2`（CHECK_USER_HAS_QUEST_ITEM）→
  `select2_1`（SELECT2_1_1）→ `select2_1_1`（SETPRO1，文本“请代我将花环送到 [墓碑]”），
  缺少水仙花时走 `select2_2`；行 1 墓碑物件 700379 的 `select3`（SELECT3_1）→
  `select3_1`（“将花环放在墓碑前。结束观察”）；行 2 Heintz 的 `select4`（SELECT_QUEST_REWARD）
  → `select_quest_reward1` 领奖页。页 id：SELECT2=1352 / SELECT2_1=1353 / SELECT2_1_1=1354 /
  SELECT2_2=1438 / SELECT3=1693 / SELECT3_1=1694 / SELECT4=2034。
- **物品合同**：行 0 交 5 个 152000463（水仙花）换工作物品 182209042（花环，SETPRO1 的 give-item），
  行 1 墓碑吃掉 182209042。
- **旧定义错位**：`SETPRO1` 是 `started -> started` 自环（只发花环、不推进），墓碑 `SELECT3_1`
  从 `started` 直接进 reward，行 2 用 `SELECT_QUEST_REWARD` 落回 reward/var0=0；行 1/行 2
  没有状态。审计判成 `MISSING_TAIL_ROWS | ROW_BEHIND | ROW_WITHOUT_STATE(1 2) | visible=0`。

### 四十六之二、落点（行阶梯 = START/START/REWARD）

- **节点**：`unaccepted(0) / started(START,0) / s1(START,1) / reward(REWARD,2) / complete(0)`。
- **行 0**：`started` 的 `select2` 链保持不变（CHECK 成功→`select2_1`，失败→`select2_2`），
  `SETPRO1` 改为 `started -> s1` 并保留 `give-item 182209042`。
- **行 1**：墓碑 700379 的 `USE_OBJECT -> SELECT3` 与 `SELECT3_1 -> reward`（条件 `has-item`
  花环 1 个 + 移除）都收敛到 `s1`；同 source 保留
  `can-act template-id=700379 action-type=ACTION_ITEM_USE` 资格声明。
- **行 2**：`reward` 态用显式路由 `QUEST_SELECT -> SELECT4`、`SELECT_QUEST_REWARD ->
  SHOW_SELECT_QUEST_REWARD_WINDOW1`（客户端行 2 的页是 `select4`，不在 `NPC_REPORT` 允许的
  SELECT2/SELECT5/DEFAULT_SUCCESS 集合里，因此不能用 npc-report 块）；`npc-complete` 留在
  `reward -> complete`，preview 只保留 `USE_OBJECT`。
- **自愈边**：`REWARD && var0==0` 无 source `enter-world` → `reward(var0=2)`。

### 四十六之三、验证（2026-09-22）

- **静态**：`xmllint --noout --schema quest_definition.xsd` 1/1 validates；
  `apply_batch42_tombstone_flower_row_ladder.py --apply` 后 `--check` 幂等 OK。
- **全库行号审计（脚本 [11] 节已改为批次 42，并补登本族）**：
  `MISSING_TAIL_ROWS 45 -> 44`（-1）、`ROW_BEHIND 142 -> 141`（-1）、
  `ROW_WITHOUT_STATE 474 -> 473`（-1）、`ROW_ALIGNED 2708 -> 2709`（+1）、
  `ROW_STATE_ALIGNED 2480 -> 2481`（+1）；任务转为
  `ALIGNED + ROW_ALIGNED + ROW_STATE_ALIGNED + visible=0 1 2 + recovery=True`，
  前后见 [batch42-evidence.tsv](batch42-evidence.tsv)。
- **Maven（授权后执行）**：新增 `Batch42TombstoneFlowerRowContractTest`（5 例：每行一个状态 /
  交花与制作花环推进行 1 / 墓碑献花限定在行 1 且带 ACTION_ITEM_USE 资格 / 行 2 的 select4 与
  窗口 1 / 旧 `REWARD var0=0` 自愈）5/5 绿；同批回归
  `Batch41PangaiaFortressRowContractTest` 5/5、`Batch40ThreeNpcTalkLadderContractTest` 5/5、
  `Batch39TurnInTalkReportRowContractTest` 5/5、`ProductionCatalogWhitelistVerificationTest` 1/1
  （`PRODUCTION_COMPILE_OK=6191 / FAILURES=0 / INTERACTION_OBJECT_FAILURES=0 /
  WHITELIST_VIOLATIONS=0`）、`QuestPageButtonAuditTest`(2)、`QuestHandoverContinuationAuditTest`(2)、
  `QuestE2eInfrastructureTest`(41)、`AcceptAndConfirmationEntryContractTest`(2)、
  `QuestClientContractGateTest` 全绿；`QuestInteractionObjectCatalogTest` 仍是既有 8 条
  （13809/23809/30504/30554），4033 不在其中。
- **客户端实机 PENDING_CLIENT**：① 接取后行 0 提示采集水仙花，交出 5 个后看到 `select2_1`
  与 `select2_1_1`，点“等待制作”拿到花环并切行 1；② 墓碑处必须能出现 `select3` 献花页，
  献花后切行 2；③ 回 Heintz 出现 `select4` 报告页，点“报告结果”弹奖励窗口 1 并完成；
  ④ 没有花环时点墓碑不应能进入 `select3_1`；⑤ 旧存档 `REWARD + var0=0` 登录/切图后自愈到行 2。

### 四十六之四、边界与后续

- 行 2 的客户端页是 `select4`（不是 `select_success`），因此本任务不能套用 `NPC_REPORT`
  模板，必须走显式 `QUEST_SELECT/SELECT_QUEST_REWARD` 路由——后续同类任务（页名非
  SELECT2/SELECT5/DEFAULT_SUCCESS）都要按这个模式处理。
- 剩余 `MISSING_TAIL_ROWS 44`（38 个待逐族收口 + 6 个已登记例外）。其它挂账不变：
  `STATES_BEYOND_ROWS 2620`、`INTERIOR_GAP 263`、`MISSING_LAST_ROW 73`、客户端隔离族 8 个；
  既有红沿用 `QuestInteractionObjectCatalogTest` 8 条资格缺口。
