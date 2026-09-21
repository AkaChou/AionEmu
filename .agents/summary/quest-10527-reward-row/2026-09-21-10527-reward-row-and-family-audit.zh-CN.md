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
  （现为 0），`ExternalRewardAdvanceReentryContractTest` 会直接校验；要修必须先改写入方并重刷
  `src/test/resources/quest/external-reward-advance-baseline.tsv`。
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
