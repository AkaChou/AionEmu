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
