# QuestWiki 小步骤 GM 命令验证记录（2026-09-21）

仓库：/Users/mc/IdeaProjects/AionEmu-QuestWiki（未提交）
需求：采集进度等小步骤也要有各自的 GM 命令。

## 实现

- `scripts/markdown.ts`
  - 新增 `collectProgressRows(model, itemNames)` / `collectProgressRowText` / `collectProgressRowCommand`：
    采集块按 `metadata/items` 一项物品一行，命令为 `//add <物品ID> <数量>`。
  - `renderClientSteps`：每个子目标/采集行下方输出小步骤命令（缩进 5）：
    物品行 = `- GM（获取物品）：\`//add <物品ID> <数量>\``；其余子目标继承所在阶段的 `//quest set` 命令。
  - 新增区块说明「小步骤 GM：…」（`//add` 默认发给执行命令的 GM，可加玩家名指定目标）。
- `scripts/generate-questwiki.ts`：有 `[%collectitem]` 的任务把采集物品行加入搜索文本与 `entityIds`，
  按物品 ID 搜索可反查任务（已验证 `/search?q=182215897` 命中 15400）。
- `src/lib/quest-sections.ts`：`subItems` 由字符串改为 `{text, gmCommand, gmKind}`，按缩进分层解析（3 = 步骤子行，5 = 该子行的命令）。
- `src/components/QuestStepsPanel.tsx`：小步骤渲染独立 GM 入口（默认隐藏），展开后显示命令标签 + 命令 + 复制按钮；
  `显示全部 GM` 现在覆盖小步骤命令。
- `src/styles.css`：新增 `.quest-steps-gm-kind`；修正窄屏下 `.quest-steps-instruction` 指令正文溢出面板的问题（`flex: 1 1 auto; min-width: 0`）。

## 证据

- 生成：`pnpm gen:data` 6222 个 Markdown；`pnpm check:data` 通过；`pnpm lint`、`pnpm test`（7 文件 / 51 用例）、`pnpm build` 通过。
- 任务 15400（用户举例）步骤 4：
  `采集进度：布里特拉补给团证物（物品 182215897） 当前/1` + `GM（获取物品）：//add 182215897 1`（另两项齿轮/军团标记同理）。
- 浏览器（preview 5190 / dev 5173，Chrome DevTools）：
  - 15400 页面 10 步 / 13 个 GM 入口；展开采集行 → 标签「获取物品」+ `//add 182215897 1`；复制 → 「已复制」，`pbpaste` = `//add 182215897 1`。
  - 10525：8 步 / 15 个 GM 入口；步骤 3 的 4 个证言子目标各带 `//quest set 10525 START 2`；步骤 5 三行采集分别给 `//add 182216069 20`、`//add 182216070 20`、`//add 182216071 5`。
  - 390×844 移动视口全部展开：13 个 GM 行、13 个 ID 展开、无面板/行/页面横向溢出。
- 上游并发改动：AionEmu-test 的 15604/16821/26821.xml 在 15:22-15:23 被追加 `设置 var1=0`，首次 `check:data` 因文件仍是旧 XML 生成而报 3 处不一致，重新生成后通过（与本次前端改动无关）。

## 已知边界

- 步骤行内联计数（如 15400 步骤 7 的 `([%20]/2)`，条件 `var1 >= 1`）没有单独命令：
  只有所在阶段的 `//quest set 15400 START 6`。若要补「把计数预置为 N」的命令，
  可按 XML `<variable-at-least field="var1" value="1"/>` 生成 `//quest set 15400 START 1 1`（`setQuestVarById(varNum=1, value=1)`），需用户确认后再加。

---

# 追加：客户端计数占位符 `([%20]/2)`（2026-09-21 15:47）

## 结论（证据）

- `[%N]` 是客户端 `quest_summary` 的**运行时槽位**编号，不是服务端 var：
  第 k 步（0 基）占 3 个槽 —— `3k` = `visible`、`3k+1` = `color`、`3k+2` = 该步「当前计数」。
  15400 第 7 步（k=6）→ `visible="[%18]"`、`color="[%19]"`、正文 `([%20]/2)`。
- 全库 6430 个 `quest_q*.html` 统计：`visible`/`color` 属性里的索引 100% 落在 mod 3 ∈ {0,1}（14058/14060 次）；
  正文内联的 `[%N]` 4051/4052 次落在 mod 3 = 2（计数槽），仅 1 处例外（`"font color=""[%10]"""` 脏数据）。
- 游戏内客户端把 `[%20]` 替换成当前击杀数（`0/2`、`1/2`…）；wiki 之前原样保留了占位符。

## 改动

- `scripts/sources.ts`：新增 `resolveCounters()`，步骤行与指令行在 `resolvePlaceholders()` 之后调用；
  规则 = `[%N]` 紧跟 `/\s*数字` 时替换为 `0`（即 `([%20]/2)` → `(0/2)`），没有 `/需求` 的裸占位符保持原样。
- `scripts/markdown.ts`：区块说明改成「计数占位符按客户端初始显示写成 `(0/需求)`，其余占位符保留原始文本」。
- `README.zh-CN.md`：补充槽位编号规则与显示约定。
- `test/sources.test.ts`（新增）：`(0/需求)` 换算 + 裸占位符不动 + 词典名先解析；`test/generated-data.test.ts`：15400 第 7 步 `(0/2)`、10525 的 `[%8]` 保持原样。

## 数据影响与验证

- 换算后：`(0/需求)` 样式 5135 行；指令行里同类计数 17 处全部换算（残留 0）；全库仍有 249 处裸 `[%n]`（调试任务 quest_9693x、可见性槽位等），按原样保留。
- 浏览器：`/quest/15400` 第 7 步显示 `消灭克罗坦要塞周围的艾莱休奇卡巡逻队下级搜索兵（NPC 885101）(0/2)`；`/quest/1400` 指令显示 `(0/3)`。
- `pnpm gen:data` / `check:data` / `lint` / `test`（8 文件 55 用例）/ `build` 全部通过。
- 注意：`(0/M)` 的 M 取客户端自身的显示上限（15400 var1 max=2），而服务端推进条件是 `var1 >= 1`（杀 1 只即可推进）——wiki 按客户端显示，未改成服务端需求值。

---

# 追加：计数需求显式化 + 修正「服务端条件 = 1」的误读（2026-09-21 15:57）

## 结论

- 客户端 `([%N]/M)` 里的 **M 就是完成该步骤需要的次数**（= 需求），不是位域上限的巧合。
  项目自身的证据：`quests/25304.xml` 的 `<progress>` 注释写「var1：哥尔哈精气计数（SECTION_1，客户端 ([%8]/60)）」，
  该任务确实要求积蓄 60 点精气；wiki 现在显示 `(0/60 · 需求 60)`。
- 全库对照（客户端 `/M` vs 服务端 varN 条件，113 处可对照）：
  - 102 处 `服务端条件值 = M - 1`（15400 `/2` ↔ `var1 >= 1`；15304 `/60` ↔ `var1 >= 59`；20035 `/10` ↔ `var1 >= 9`）
  - 11 处直接相等（25304 `/60` ↔ `var1 >= 60`；20528 `/10` ↔ var2 段）
  - 12 处为打包/多段计数（计数写在 var2 等其它段，如 20528/20529）
- 因此前一版回复里「15400 var1>=1 所以杀 1 只就推进」是错的：服务端在事件处理时先按**当前快照**判条件、再执行动作
  （`QuestProductionDispatcher` 选路 → `QuestMutationPlanner` 校验条件 → 执行 `increment-variable`），
  第 1 只击杀走自循环 var1: 0→1，第 2 只击杀才满足 `var1 >= 1` 推进并清零 → 需要 2 只，与客户端 `/2` 一致。

## 改动

- `scripts/sources.ts`：`resolveCounters()` 输出 `(0/M · 需求 M)`（客户端缺左括号时同样写在计数后）。
- `scripts/markdown.ts` / `README.zh-CN.md`：说明改为「`(0/需求 · 需求 N)`，N 是完成该步骤需要的次数」。
- 测试：`test/sources.test.ts`、`test/generated-data.test.ts` 期望值同步。

## 验证

- `grep` 统计：`(0/N · 需求 N)` 5140 处；剩余裸 `[%n]` 232 处（没有 `/需求`，如调试任务 quest_9693x、可见性槽位）。
- 浏览器（preview 5190）：15400 第 7 步 `(0/2 · 需求 2)`、采集行 `当前/1`；1400 指令 `(0/3 · 需求 3)`；25304 `(0/60 · 需求 60)`。
- `pnpm gen:data` / `check:data` / `lint` / `test`（8 文件 55 用例）/ `build` 全部通过。

---

# 追加：分支步骤（任务 1114 两种奖赏）与阶段编号按打印顺序（2026-09-21 16:15）

## 问题（1114）

- 客户端 6 个步骤是平铺的；服务端 `v3` 有两个后继：`reward4`（纳姆斯 203075，SELECT_QUEST_REWARD）与 `reward3`（阿斯泰罗斯 203058，SETPRO3）。
- `reward3` 的 var0 与 `v3` 相同（都是 3），按 var0 回退的规则让步骤 4/5/6 都拿到 `//quest set 1114 START 3` —— 命令相同且没有分支感。

## 改动

- `scripts/stages.ts`
  - 新增 `buildStepBranches(list, model, stepTexts, questId)`：分叉阶段的每个后继，用「进入该后继的 `<event>` NPC」对上「步骤文案里被标注出来的 NPC ID」；
    要求每个后继恰好命中一个未被占用的、位于分叉步骤之后的步骤，否则整组放弃（不推测）。
  - 分支步骤给各自目标阶段的命令（`status` + `var0`）。
  - 阶段列表编号改为按**打印顺序**分配（原来用 BFS 扁平序，分支子树会出现 5 → 7 → 6），回环引用同步用打印序号。
- `scripts/markdown.ts`：分支步骤输出 `- 分支：1/2（与同组分支二选一）`，分叉步骤输出 `- 分支点：步骤 5、6 二选一`，并替换成对应分支命令。
- `src/lib/quest-sections.ts` + `QuestStepsPanel.tsx` + `styles.css`：解析并在页首渲染分支组标题「分支（二选一）」、`分支 1/2` 标记与分叉点提示。
- 测试：`stages.test.ts`（分支命中/无证据放弃/候选不唯一放弃）、`quest-sections.test.ts`、`quest-steps-panel.test.tsx`、`generated-data.test.ts`（1114）。

## 覆盖与验证

- 覆盖：当前证据规则命中 8 个任务（1114、1467、18990、18992、28990、28992、30155 等）；全库有服务端分支的任务是 804 个，
  其余多数分支是「放弃任务 / 进入世界自愈」这类非步骤分叉，或分支证据里没有 NPC（758 条分支描述无 NPC），因此按规则放弃。
  （试验过把「目标阶段自身转移的 NPC」也算作证据：可覆盖 24 个任务，风险是可能配错分支，暂未启用。）
- 浏览器（dev 5173）：1114 步骤 4 显示分支点提示 + `START 3`；步骤 5 `分支 1/2` + `//quest set 1114 REWARD 3`；步骤 6 `分支 2/2` + `//quest set 1114 REWARD 4`；
  18990 同样按分支显示（步骤 2 → START 1、步骤 4 → REWARD 3）。窄屏 390×844 无溢出。
- 全库阶段编号连续性校验：0 个任务出现跳号；`pnpm lint` / `test`（8 文件 61 用例）/ `check:data` / `build` 全部通过。

---

# 追加：奖励/来源/导航搜索/中英对照（2026-09-21 17:30）

本轮 6 条需求（全部在 **AionEmu-QuestWiki** 仓库实现，AionEmu-test 只读）：

| 需求 | 实现 | 证据 |
| --- | --- | --- |
| 1. 任务步骤显示奖励，标注单选/多选 | `scripts/quest-xml.ts` 解析 `metadata/reward-groups`（并保留 `metadata/rewards` 简写为唯一档）；新增 `scripts/rewards.ts`：`resolveStageRewards` 把阶段 → 完成块（`npc-complete@complete-reward-index`、`complete-quest@reward-index`、`grant-selected-reward@reward-index`、`action/grant-reward` 兜底）解析成档位；步骤行输出 `奖励（单选）：…` / `奖励（多选，N 选 1）：…` / `奖励（随机，从抽取表发放）：…` | 1114 步骤 5/6 分别显示 `基纳 ×1920、经验 ×4367` 与 `基纳 ×960、经验 ×3120`（与 XML 注释、真端档位一致）；1001 显示 `奖励（多选，3 选 1）` |
| 2. 不显示「来源：客户端 …」引用行 | `renderClientSteps` 不再写该引用行；`parseClientSteps` 同时过滤 `来源：` 前缀引用行（双保险） | 全库 6222 个 Markdown 的步骤区 `> 来源：` 计数为 0 |
| 3. 顶部导航搜索框 + `*` 模糊搜索 | `RootLayout` 新增 `role="search"` 表单（回车跳 `/search?q=…`）；`src/lib/search.ts` 新增 `compileWildcard`：按 `*` 拆词 → 各词取索引命中 → 同字段取交集 → 顺序正则复核 → 按命中位置截片段；目录页关键词筛选复用同一规则 | 浏览器实测 `佣兵*呼叫` → 仅命中 1100「佣兵团长的呼叫」；`德拉克纽特*头盔` → 1510、2658，片段落在 `steps` 字段 |
| 4. 阵营显示天族/魔族 | 新增 `src/lib/labels.ts` 的 `RACE_LABELS` + `raceLabel()`；目录表与筛选下拉、基本信息「种族」行、限制条件行都显示 `ELYOS（天族）` / `ASMODIANS（魔族）` / `PC_ALL（全种族）` | 目录页首行 `ELYOS（天族）`；1114 基本信息 `| 种族 | ELYOS（天族） |` |
| 5. 类别显示中文 | `CATEGORY_LABELS`（QUEST 普通任务、EVENT 活动任务、MISSION 使命任务、FACTION 势力任务、IMPORTANT 重要任务、CHALLENGE_TASK 挑战任务、PUBLIC 公共任务、SEEN_MARKER 标记任务、SIGNIFICANT 重点任务、TASK 任务、NON_COUNT 不计入统计、PRIMARY 主线任务、LEGION 军团任务）+ `categoryLabel()`；服务端 `QuestCategory` 枚举本身没有中文名，这里是 Wiki 侧显示名 | 1114 `| 类别 | QUEST（普通任务） |` |
| 6. ITEM → 物品、EXP → 经验 | `REWARD_KIND_LABELS`：ITEM 物品、SELECTABLE_ITEM 可选物品、RANDOM 随机奖励、GOLD/KINAH 基纳、EXP 经验、AP 欧比斯点数、GP 荣誉点数、DP 神圣力、TITLE 称号（货币名取自客户端 `data_unpacked/Strings/client_strings_msg.xml`：基纳 / 经验 / 欧比斯点数 / `%gchar:glory_point` / 神圣力） | 1510 奖励清单 `- 物品：奇怪的红色口袋（物品 188050586）×1`、`- 随机奖励（抽取表 10204）×1` |

细节与边界：

- 档位展示用**整档**而不是 `fixed-reward-indices`：后者只列固定发放的下标（服务端会拒绝把 `SELECTABLE_ITEM` 写进固定奖励），
  客户端奖励窗口展示的是「固定奖励 + 可选项」；抓取全库后确认 `fixed-reward-indices` 是严格子集的 790 条、其中 747 条属于含 `SELECTABLE_ITEM` 的档。
- 领奖阶段没有落在 BFS 推进链上时（紧凑 DSL 块如 `<kill-chain>` 不写 source/target，例如 1001）按 `list.unconnected` 里 `status=REWARD` 的节点补齐，
  再挂到客户端最后一步；档位无法落到任何步骤时（约 59 个任务，含无 StateGraph 的 9554/1993 类）改为步骤区末尾的 `> 奖励档位（共 N 档…）` 引用行，逐档列出（物品只写名字，ID 只在「基本信息」）。
- 覆盖：5455/6222 个任务在步骤里直接带奖励行；有奖励定义的任务共 5428 个（另多为 metadata 无奖励）。
- 前端：`QuestStepItem.isReward` 标记奖励行，`.quest-steps-sub > li.is-reward` 用金色左边框与 `✦` 项目符号区分；物品 ID 仍折叠为点击才显示的 ID 按钮。
- 验证命令：`pnpm gen:data` → `pnpm check:data`（一致）→ `pnpm lint` → `pnpm test`（10 文件 79 用例）→ `pnpm build`（`dist/assets/index-DOEsU9ar.js` 715.64 KB / gzip 226.97 KB，CSS `index-Za_WIRl-.css`）全部通过。
- 浏览器（dev 5173）：1114 分支标题 `display:flex` 单行（上一轮错位已修，桌面 1280×900 与窄屏 390×844 均不竖排）、两种奖赏分别显示、无 `来源：` 引用行；1510 奖励行中物品 ID 折叠成 ID 按钮；1687 三档走引用行 + 档位明细；搜索与目录 `*` 通配均实测命中。

---

# 追加：步骤 GM 命令阶段对齐修复（2026-09-21 17:50）

报障：`/quest/1192` 每一步的 GM 都是 `//quest set 1192 START 0`，且「类似的任务很多」——命令重复、末步领奖命令丢失。

## 根因

`scripts/stages.ts` 的 `buildStageList` 用「不同 var0 的数量 == START 节点数」判定 `alignment`。分职业/分阵营的平行实现会给同一个
var0 挂多个变体节点（如 11002 的 8 套节点共用 var0 0..3），这类任务因此被判成 `alignment: none`，`buildStepGmCommands` 整体退回
「统一指向起始阶段」，末步的领奖命令也被同一条 `START 0` 覆盖（1192 只有 `started(START var0=0)` + `reward(REWARD var0=0)` 两个非 NONE 节点，属于同一形态）。

## 改动（AionEmu-QuestWiki 仓库；AionEmu-test 只读）

- `scripts/stages.ts`
  - `buildStageList`：`alignment` 只看**不同 var0 值**是否从 0 连续（`distinctVar0`）；同一 var0 的多个变体只给第一个节点标 `clientStepIndex`，阶段列表不再被同一步骤刷屏。
  - `buildStepGmCommands` 新增/重排四条判定：① `applyLastStepReward`（领奖阶段完成 NPC 出现在末步文案里，或该任务只有一个领奖阶段且没有别的步骤指到它 → 末步给领奖命令）；
    ② var0 跳号但连续前缀正好等于「步骤数 - 1」时按前缀顺次对应（1004：0..5 + 11 → `START 0..5` + `REWARD 5`）；
    ③ 推进链上没有进行中阶段时，用 `unconnected` 里 var0 轴上的进行中阶段兜底（80298 一族、14123）；
    ④ 状态全是领奖阶段的任务按「领奖阶段完成 NPC == 步骤文案标注 NPC」做一一对应（1205 / 2132 每个教官一档 → 逐步 `REWARD 1..6`），
    一档对不上或对上多个步骤就整组放弃；状态轴不是 var0（如 `step`）的任务直接跳过，不推测。
  - 新增/导出 `annotatedNpcIds`、`stageCompletionNpcIds`、`rewardStageRefs`；`StepGmOptions` 接收 `model` 与每步 NPC 证据。
- `scripts/markdown.ts`：把 `model` 与每步标注出的 NPC 传进 `buildStepGmCommands`，按命令分布生成提示语（全同 / 部分同 / 起始阶段兜底）。
- 测试：`test/stages.test.ts`（同 var0 多变体分组、多领奖档按完成 NPC 选中、单领奖阶段末步领奖、var0 跳号前缀、未连接阶段兜底、领奖档位缺证据整组放弃）、
  `test/generated-data.test.ts`（1192 / 11002 / 1002 / 1004 / 1205 / 80298 + 全库「无多步骤任务共用同一条命令」「阶段列表客户端步骤编号不越界」）。

## 全库结果（生成产物统计，脚本 /tmp/questwiki-analysis/gm-stats.mjs）

- 客户端步骤 10677 条，带命令 10619 条（**99.46%**，改前 10599 = 99.27%）；有命令的步骤按状态分：START 5057、REWARD 5562。
- 多步骤任务 3098 个：**所有步骤共用同一条命令的任务 0 个**（改前 916 个，其中 909 个末步本该领奖）。
- 抽样核对：1192 → `START 0`、`START 0`、`REWARD 0`；11002 → `START 0`、`REWARD 3`；1004 → `START 0..5` + `REWARD 5`；
  10525 → `START 0..6` + `REWARD 7`（上游 10525.xml 17:39 把 reward 的 var0 由 6 改成 7，已随本次重生成同步）；1205/2132 → 逐步 `REWARD 1..6`；80298 → `START 0`、`START 0`、`REWARD 0`。
- 仍不给命令的 26 个任务：20 个任务定义没有 `<nodes>`（1489、9554-9557、51031/51038/51040/51041 等）、
  4 个只有领奖阶段而客户端有 2 步（13952、15563、23952、25563）、2 个状态轴是 `step` 而不是 `var0`（1929、2900，`//quest set` 只能写 var0）。这 26 个在页面里保留「无法自动生成跳转命令」的说明，不写推测命令。

## 验证

- `pnpm gen:data` → `pnpm lint` → `pnpm test`（10 文件 86 用例）→ `pnpm check:data`（与生成器输出一致）→ `pnpm build`（`dist/assets/index-DOEsU9ar.js` 715.64 KB / gzip 226.97 KB）全部通过。
- 浏览器（dev 5173，1280×900）：/quest/1192 三步分别显示 `START 0`、`START 0`、`REWARD 0`；/quest/1205 六步分别 `REWARD 1..6`；
  /quest/11002、/quest/1004 与生成产物一致；控制台无报错。
