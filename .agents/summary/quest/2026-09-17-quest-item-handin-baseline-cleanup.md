# 2026-09-17 任务道具交付基线 42 条既有无条件分支深度攻坚总结

## 1. 背景与目标
在提交 `cd38b7dea` 中，全库针对 `NPC_REPORT` 简写展开导致的无条件交付路由修复了 51 个任务的 79 条交付路由，并建立了全库门禁 `QuestItemSourceContractGateTest#everyRewardEntryBranchVerifiesTheQuestsOwnCollectedItems`。
同时将全库存在的 30 个任务共 42 条既有无条件/未完全校验交付分支记录在 `src/test/resources/quest/quest-item-handin-baseline.tsv` 中作为待评审基线。

本次任务目标：对这 42 条既有无条件分支进行逐一排查、根因归类与彻底修复，使基线收敛至仅包含经确认合法的业务分支。

---

## 2. 根因分类与修复方案

### 类别 A：拥有 `CHECK_USER_HAS_QUEST_ITEM` 却残留无条件 `SELECT_QUEST_REWARD` 旁路（20 条分支，12 个任务）
- **涉及任务**：
  - `1870` (started@278501)
  - `2870` (started@278001)
  - `10530` (s8@203752)
  - `15478` (started@805799, started@805801, started@805803)
  - `15479` (started@805800, started@805802, started@805804)
  - `25478` (started@805816, started@805818, started@805820)
  - `25479` (started@805817, started@805819, started@805821)
  - `80795` (started@833543)
  - `80796` (started@833545)
  - `80797` (started@833543)
  - `80798` (started@833545)
  - `80888` (started@834368)
- **根因**：历史模板生成或补齐对话时，在 `started` 节点既生成了带 `has-item` 与 `remove-item` 的 `CHECK_USER_HAS_QUEST_ITEM`，又多留了一条无条件的 `SELECT_QUEST_REWARD` 直通 `reward`。玩家或封包若发送 `SELECT_QUEST_REWARD` 可直接绕过道具校验零进度领奖。
- **修复**：对全部 20 条 `SELECT_QUEST_REWARD` 路由严格补齐本任务收集道具的 `conditions (has-item)` 与 `actions (remove-item)`，并补齐 `priority="1"` 失败页 `CHECK_USER_ITEM_FAIL` 与 `FINISH_DIALOG` 路由。

### 类别 B：模板名大小写遗漏的简写任务（4 条分支，2 个任务）
- **涉及任务**：
  - `3217` (started@798335, started@204590) - 道具 182209095 需求 3 个
  - `4217` (started@798336, started@204773) - 道具 182209110 需求 3 个
- **根因**：在上一批自动扫描中因真端模板大小写未命中，沿用了 `NPC_REPORT` 简写展开。
- **修复**：对两个任务共 4 个 NPC 变体展开显式三路由（priority 0 带 `has-item` 与 `remove-item` 校验交付 + priority 1 `CHECK_USER_ITEM_FAIL` 失败页 + `FINISH_DIALOG` 关闭对话）。

### 类别 C：显式交付边漏校验与镜像对齐缺陷（8 条分支，8 个任务）
- **涉及任务**：
  - `1482` (started2@203337)：`started2` 阶段虽有 `SETPRO3` 传送与扣除，但下方多留了无条件 `SELECT_QUEST_REWARD` 旁路；补齐 `has-item 182201399 count 3` 与扣除及失败回落。
  - `2430` (s8@204300)：分支 C 交付边定义了 `remove-item 182204222 count 1`，但 `conditions` 缺失 `has-item`；补齐 `has-item` 校验与 `SELECT9_2` 失败回落。
  - `20530` (s9@204075)：`s9 -> reward` 定义了移除 182216166 与 182216168，但条件仅判断了 `var0==9`，遗漏了 `has-item`；补齐两件道具校验及失败回落。
  - `26930` (started@804627)：定义了 `npc-item-report`，但下方显式手写了无条件 `SELECT_QUEST_REWARD` 绕过了校验；补齐 `has-item 186000257 count 10` 与扣除及失败回落。
  - `26977` (started@801765)：上一批仅修复了 801764，漏掉了 801765（NPC_START NPC）；为 801765 补齐相同的校验、扣除、失败页及关闭路由。
  - `28739` (started@804732) 与 `28740` (started@804732)：魔族镜像对齐天族 `18739`/`18740`，将错误的 `DEFAULT_SUCCESS` + 无条件 `SELECT_QUEST_REWARD` 改为标准的 `SELECT1` + `CHECK_USER_HAS_QUEST_ITEM`（带 182215695 x5 / 182215696 x8 校验与扣除）。
  - `50021` (a3@202549)：击杀满后进入 `a3`，交付边直接无条件进奖励且从未扣除掉落的 182215176 x3；补齐 `has-item` 校验、扣除、失败页与关闭路由。

---

## 3. 基线留存评审结果（剩余 10 条合法业务分支）
经过逐条人工业务语义确认，剩余 10 条分支均属于真端既定流程，不能在最终 REWARD 边重复要求掉落物：
1. `1922` (`s7@203901`)：副本竞技场终点宝箱收集物，整叠移除保持可选（`ce4690091`），避免玩家因未开箱卡死主线使命。
2. `2430` (`s2@204377`, `s6@798082`)：多分支结局任务，`s2` 为分支 A（仅需工作道具）、`s6` 为分支 B（调查无需掉落道具），只有 `s8` 完整收集才交付掉落道具。
3. `2919` (`bookInserted@204206`)：收集物 182207010..012 已在步骤 `talk3` 校验并扣除，`bookInserted` 交付的是中间书籍道具。
4. `10010` (`s5@806585`) / `20010` (`s5@806594`)：收集物已在步骤 `s1` 校验并扣除，`s5` 仅为最终向司令官汇报。
5. `15306` (`s9@805327`) / `15316` (`s9@805327`) / `25306` (`s9@805339`) / `25316` (`s9@805339`)：收集物已在步骤 `s1` 校验并在后续步骤扣除，`s9` 仅为最终完成对话。

---

## 4. 门禁与验证
- 全库门禁测试 `QuestItemSourceContractGateTest`：
  - 新增 `1870`, `2870`, `3217`, `4217`, `28739`, `28740` 严格断言测试。
  - `everyRewardEntryBranchVerifiesTheQuestsOwnCollectedItems` 跑通 6,222 个任务 XML，违规数为 0。
- 基线文件 `src/test/resources/quest/quest-item-handin-baseline.tsv` 从 42 行收敛至 10 行。
- Memory Bank 模式 `[QE-032]` 自动同步完成。
