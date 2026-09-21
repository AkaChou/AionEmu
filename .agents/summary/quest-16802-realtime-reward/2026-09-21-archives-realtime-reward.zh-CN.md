# 永恒档案馆 16802/16803/16804 实时奖励收口审计与修复（2026-09-21）

## 报障与症状

- 用户报障：`16802 It Belongs in A Museum`（天族，66+，知识书库 `301540000`）击杀目标完成后**无法点击实时奖励**；`16801` 可以。
- 客户端含义：任务窗「实时奖励」= `STR_QUEST_DIALOG__QUEST_GET_REWARD(912776)`，词典解释「完成任务的同时，可以在原地立即获得奖励的功能」；任务名前的「[实时奖励]」标记来自 `STR_QUEST_PREPARE_REWARD(901163)`。

## 证据层级（先证据后改码）

1. **客户端计数门控**：客户端 `quest_monster.csv`
   - `16802,Progress(SECTION_0==0; SECTION_1<30)` + 8 个 `ideternity_01_leibo_*_68_ae`
   - `16802,Progress(SECTION_0==0; SECTION_2<2)` + 6 个 `bideternity_01_leibo_*_68/69_ah`
   - 两条计数行都要求 `SECTION_0==0`，即**计数期间 var0 必须保持 0**；`SECTION_1/2` 分别是 var1/var2 计数。
   - 仓库内派生快照：`docs/quest/client-dialog-mapping/client-monster-progress-contracts.csv:282`（派生表只保留第一段）；客户端原始契约
     `/Users/mc/PycharmProjects/unpak/Quest_unpacked/quest_monster.csv:4378-4379` 明确给出 `SECTION_0==0; SECTION_1<30` 与
     `SECTION_0==0; SECTION_2<2` 两段；客户端数据 `data_driven_quest.xml` 16802 的两段 `value0_progress_`（`... 30; ... 2;`）与之一致。
2. **客户端领奖资质**：客户端 `quest.xml` Q16802 `can_report=1`、`use_class_reward=1`（16801/16803/16804 同形）；任务 HTML 页面集为 `select_none(4762) / select_success(10002) / select_quest_reward1(5) / quest_summary(9) / quest_complete(1008)`，`select_success` 上的唯一按钮是 `HACTION_SELECT_QUEST_REWARD(1009)`（`quest-dialog-action-details.csv:7057-7059`）。
3. **旧正式 handler**：`origin/history` 的 `_16802It_Belongs_In_A_Museum.java`
   - `onKillEvent`：var1 累加到 30、var2 累加到 2，**全部满足时在同一击杀事件内** `setQuestVar(0,1)` + `setStatus(REWARD)`；
   - `onDialogEvent` 只在 `REWARD` 状态处理 806148（31→10002、1009→5）。
4. **已被用户验收的魔族孪生**：`26802` 于 2026-08-20 客户端验收通过（`.agents/summary/quest-acceptance/26802-2026-08-20-client-accepted.md`），其结构为：单一 `started` 节点（**不投影实时字段**）+ 两组计数三路（priority 2/1/0）+ 最后一击 `started->reward` + 带门禁的 `SELECT_QUEST_REWARD` 恢复路线；`QuestCounterSourceProjectionProductionFlowTest`、`Quest26802ClientDialogAlignmentTest` 锁定该合同。
5. **模式卡**：`QE-018 MULTI_COUNTER_FINAL_EVENT_ENTERS_REWARD`（「多项实时计数已经全部达到上限但状态仍是 START」→ 每个计数字段成对 continuing/completing，最终事件 priority 0 直接进入 REWARD，`after-commit` 用 `LEVEL_AND_VISIBILITY_REFRESH`；满计数旧存档由带阈值的报告路线恢复）。

## 根因

天族侧 16802/16803/16804 仍是迁移期的「多节点中间态」写法（`started -> k1 -> k2`，`k1/k2` 是 START 状态节点）：

1. 击杀收口只把 var0 推进到 1/2 并停在 START；**最后一击不进入 REWARD**，客户端因此不进入可领奖状态，「实时奖励」按钮不可用（16801 因为最后一击直接进入 REWARD 而正常）。
2. var0=1/2 与客户端 `SECTION_0==0` 的计数门控直接冲突：第二段计数在客户端已不成立。
3. 迁移期（`7268098e8`）为通过对话审计补了**无门禁**的 `started -> reward`（806148/1009）旁路，以及 `started` 状态下的 `QUEST_SELECT -> DEFAULT_SUCCESS` 报告页，形成提前领奖旁路（未击杀即可点报告并进入奖励窗）。
4. `k1/k2 + NPC_REPORT` 声明使报告页挂在 START 中间态，而客户端合同要求 DEFAULT_SUCCESS 只在 REWARD 下发。

## 修复（3 个 XML + 4 个测试）

- `16802.xml`：节点改为 `unaccepted / started(无投影) / reward{var0=1,var1=30,var2=2} / complete{0,0,0}`；管理员与元素首领各三路（priority 2 累加、1 封顶在客户端门控值、0 最后一击 `-> reward` + `LEVEL_AND_VISIBILITY_REFRESH`）；保留 `started` 状态带门禁的 1009 恢复路线（满计数旧存档，与 26802 同形）；删除 `started` 报告页与无门禁 1009 旁路；新增领奖态 `QUEST_SELECT -> DEFAULT_SUCCESS`；新增两条**无 source 的 enter-world 自愈路线**（满计数旧 k2 -> reward；未满旧 k1 -> 归零回计数）。
- `16803.xml`：同 `26803`（单计数 30，priority 1 自环 + priority 0 收口 `set var1=30` + `set var0=1` -> reward），删除 `k1/NPC_REPORT` 与无门禁 1009 旁路，新增领奖态报告页与两条自愈路线。
- `16804.xml`：同 `26804`（击杀任一区域首领 `var0==0 -> set var0=1` 直接进入 reward），删除 `k1/NPC_REPORT` 与无门禁 1009 旁路，新增领奖态报告页与一条自愈路线。
- 新增测试：`Quest16802ClientDialogAlignmentTest`（含双计数合同、恢复路线、自愈路线与「未完成不得进入领奖/奖励窗」扫描）、`Quest16803ClientDialogAlignmentTest`、`Quest16804ClientDialogAlignmentTest`；扩展 `QuestCounterSourceProjectionProductionFlowTest`，把 16802 与 26802 一起锁进「两组计数任意顺序、最后一击进入 REWARD」。
- 家族审计脚本：同目录 `audit_archives_realtime_reward.py`（16801-16804 / 26801-26804 的击杀收口、计数 var0、报告页状态、1009 来源、自愈路线）。

## 家族审计结果（脚本输出，修复后）

| 任务 | 击杀收口 | 计数期 var0 | DEFAULT_SUCCESS 来源状态 | 1009 来源状态 | 上线自愈 |
|---|---|---|---|---|---|
| 16801 | `started->reward(p=0)` | 0 | NONE / REWARD | REWARD | 无 |
| 26801 | `started->reward(p=0)` | 0 | NONE / REWARD | REWARD | 无 |
| 16802 | `started->reward(p=0)` ×2 | 无投影 | REWARD | REWARD + START(带门禁恢复) | reward(p=0) / started(p=1) |
| 26802 | `started->reward(p=0)` ×2 | 无投影 | NONE / REWARD | START(带门禁恢复) | 无 |
| 16803 | `started->reward(p=0)` | 0 | REWARD | REWARD | reward(p=0) / started(p=1) |
| 26803 | `started->reward(p=0)` | 0 | REWARD | 无 | 无 |
| 16804 | `started->reward` | — | REWARD | REWARD | reward(p=0) |
| 26804 | `started->reward` | — | REWARD | 无 | 无 |

## 验证状态

- 已完成（本机静态）：
  - 三个 XML 的 XML 良构检查通过（`xml.etree` 解析）；
  - `git diff --check` 通过；
  - IDE 检查（`lint_files`）：三个测试与两个 XML 无 error；仅剩「参数取值恒定」一类风格提示；
  - 家族审计脚本输出见上表。
- 聚焦测试（用户授权后已执行；最后一轮 2026-09-21 18:29，**全绿**）：
  - 命令：`rtk mvn -Dtest=Quest16802ClientDialogAlignmentTest,Quest16803ClientDialogAlignmentTest,Quest16804ClientDialogAlignmentTest,QuestCounterSourceProjectionProductionFlowTest,QuestArchivesMissionCounterProductionFlowTest,QuestDataDrivenHuntProductionFlowTest,QuestReportedRewardCoverageTest,QuestClientContractGateTest,QuestProductionAcceptProtocolRegressionTest,QuestPrematureRewardRouteExclusionTest,QuestDefinitionCatalogManifestTest,ProductionCatalogWhitelistVerificationTest test`
  - 结果：`Tests run: 62, Failures: 0, Errors: 0, Skipped: 0` / `BUILD SUCCESS`；含 6189 条生产 catalog 编译白名单门禁、客户端页面/按钮契约门禁、档案馆双计数/单计数生产流与天族对齐测试。
  - 早前一轮（未含 16803 家族合同与击杀变体补齐）为 `Tests run: 54, Failures: 0, Errors: 0, Skipped: 0`。
  - 隔离说明：主工作区在 18:04 起被**并发编辑**的 `26820.xml` 打断（`26820: value out of range for progress field: var0`），生产 catalog 因而无法编译；该文件与本任务无关，按 AGENTS worktree 纪律分别在 `/tmp/aionemu-16802-verify`（HEAD + 7 个任务文件，54 项）与 `/tmp/aionemu-16803-verify`（HEAD + 8 个任务文件，62 项）隔离复验，两轮均为 `BUILD SUCCESS`，复验后均已 `git worktree remove --force` + `git worktree prune` 清理，未遗留 worktree 或构建产物。
  - 期间修正两处测试问题：① 16802 对齐测试最初断言「`started` 状态不得存在 1009 路线」，与刻意保留的
    **带门禁满计数恢复路线**冲突；已改为断言「`started` 仅保留 1 条条件非空的恢复路线」，无门禁提前领奖旁路仍由负向扫描锁定。
    ② `QuestArchivesMissionCounterProductionFlowTest.transition()` 直接解引用 `candidate.sourceNode()`，遇到 16803 新增的
    无 source `enter-world` 自愈路线会 NPE；已改为常量在前的空安全比较（`source.equals(candidate.sourceNode())`）。
- 待客户端点验（天族角色，知识书库）：
  1. 老存档自愈：`//quest set 16802 START 2`（var0=2 等同旧 k2）后重新登录/进出副本 → 任务应直接进入领奖态；
  2. 击杀收口：清空重做 30 名图书管理员 + 2 名元素首领，任意顺序，最后一击后任务窗应出现「实时奖励」并可直接领取职业奖励；
  3. 报告路径：与 806148 对话应显示报告页（10002），点「报告结果。」打开奖励窗（5）；
  4. 16803/16804 同链路复测（16803 当前击杀集合仍为 8 个 id，见下）。

## 家族击杀目标变体覆盖（本次一并修复）

- **16803 击杀目标集合补齐第 9 个变体**（原「未纳入本次修复的风险项」，现已修复）：
  - 客户端契约：`quest_monster.csv:4380`（16803）该行 `num=9`，但名单只列 8 个 `ideternity_01_leibo_*_70_ae`；
    `data_driven_quest.xml` Q16803 的第 9 个变体是 `IDEternity_01_Leibo_Cube_Artifact_As_70_Ae`。
  - 目标身份：NPC 模板 `src/main/resources/aion/data/static_data/npcs/npc_template_216189_235748.xml:27178` 中
    `npc_id="220411"` 的 `name_desc`/`name` 正是 `IDEternity_01_Cube_Artifact_As_70_Ae`（level 69 / rank=SEASONED），
    即客户端名单里第 9 个变体。
  - 同族一致：魔族孪生 `26803`（2026-08-20 客户端验收）与 `16807/26807` 的击杀集合均含 `220411`，仓库既有测试
    （`QuestArchivesMissionCounterProductionFlowTest.RELIQUARIANS_ASMODIANS`、
    `QuestDataDrivenHuntProductionFlowTest.RELIQUARIAN_NPC_IDS`）也把 9 个 id 锁定为家族契约；只有 `16803` 少一个。
  - 全目录 sweep（命令见下）确认唯一缺口就是 `16803`：
    - 正向：按客户端 `num=9` 行扫描 → 命中 16803/16807/26803/26807，只有 `16803.xml` 不含 `220411`；
    - 反向：按「XML 含 `220331`」扫描 → 命中同样四个任务，结论一致。
  - 刷怪现状：`spawns/Instances/301540000_Archives_Of_Eternity.xml` 中 `IDEternity_01_Cube_As_70_Ae`=220328 有刷怪，
    而 `IDEternity_01_Cube_Artifact_As_70_Ae`=220411 目前没有刷怪记录；因此补齐后与客户端 9 变体契约一致，
    当前不改变实际可击杀目标数量，刷怪数据补齐后即自动计入。
  - 修复：`16803.xml` 两条击杀路线（priority 1 累积 / priority 0 收口）加入 `220411`；
    `Quest16803ClientDialogAlignmentTest` 的击杀集合同步为 9 个 id（测试断言的就是路线上的完整 kill-npc 集合）。
  - 家族门禁补齐：`QuestArchivesMissionCounterProductionFlowTest` 原只覆盖 `16801/26801/26803`，导致天族 `16803`
    长期无人锁定；现已把 `16803` 加入同一份 9 变体合同（常量 `RELIQUARIANS_ASMODIANS` 更名为天魔共用的 `RELIQUARIANS`），
    氏族两侧任一缺少变体都会直接失败。
  - 复现命令（仓库根目录）：
    ```bash
    python3 - <<'PY'
    from pathlib import Path
    qdir = Path('src/main/resources/aion/data/static_data/quest_definition/quests')
    for p in sorted(qdir.glob('*.xml')):
        s = p.read_text(encoding='utf-8')
        if '220331' in s:
            print(f'{p.stem}  220411={"yes" if "220411" in s else "NO"}')
    PY
    ```
- 本次未改动任何怪点、奖励、metadata 或客户端页面数据。
