# W5-g4 · 页码类 TSV 退役第四张：`quest_client_briefing_chains.tsv`（零受理变化；不变量迁构建期）


> **生成物清理提示（2026-09-30）**：本文件正文提到的 `*.tsv` / `*.log` / `*.txt` / `*.xml` 等中间转储已随 `chore(agents)` 清理删除；原文与留痕仍可从 git 历史取回，被删清单与再生成入口见 `.agents/summary/CLEANUP-LEDGER.zh-CN.md`。

> 车道：`quest-native-dispatch`；面：**W5 页码类 TSV 退役（g4，零形状片）**。
> 上游：`GOAL.zh-CN.md` §3 W5；`2026-09-27-w5g3-w5g4-prep-and-rulings.zh-CN.md` §4-5。
> 裁定（用户，2026-09-27）：**a 退役 + 构建期校验**（否决 b 直接删门不留校验 / c 保持表与门）。
> 纪律：未 commit；未启停服务；**未新增任何 TSV**（本片退役 1 张、冻结 1 份测试夹具）；T3 在仓库外副本、跑完即删。

## 0. 一句话结论

简报链登记表（3230 行 = 4 头 + **3225** 数据行）**今日对全宇宙零拒绝**（retention 全表零 `RETAIL_BRIEFING_*`；
47 行真端 `talk_npc1` 的宇宙内 23 行全部有登记且末跳 SETPRO1/SETPRO2；六族 389/389 交叉覆盖）⇒ 退役是
**零受理变化**。两处生产门子句（hunt `:197-206`、collect `:161-167`）删除，原受保护**结构不变量**改为
**构建期常设门禁** `RetailBriefingChainEvidenceGateTest`（冻结登记快照 + 全量断言 + 基数冻结），fail-closed 保留。

## 1. 裁定与被否方案

| 方案 | 内容 | 结论 |
|---|---|---|
| **a（采用）** | 退役表 + 两处门子句删除；不变量迁 **构建期**（冻结夹具 + 常设门） | 采用：生产不再读客户链页数据；数据修订若新增简报行而客户端无链，CI 立刻红 |
| b 退役 + 直接删门 | 不留构建期校验 | 否决：未来数据修订无网兜底 |
| c 保持表与门 | 不退役 | 否决：保留一张**载荷已死**的表（hunt `build` 形参零读、collect 仅 null 判定、`entry_page` 列全仓零读） |

**零受理变化的实测依据**：retention 全表 `RETAIL_BRIEFING_*` / `RETAIL_TALK_NPC_*` 零命中（仅 2 行 SimpleTalk 族
`RETAIL_TALK_NPC_UNRESOLVED` 属 talk 编译器自己的码）；47 行审计（`45×SETPRO1 + 2×SETPRO2`，末跳 `pageId=0`）；
六族交叉覆盖 `Hunt 23/23、Collect 2/2、Talk 324/324、UseItem 33/33、ItemPlay 5/5、SerialHunt 2/2`。
24 行差集**不进管线**（23 行 `minlevel_permitted=999` 砍内容 + 9621 开发者测试行；`definition()` 只收
`owner=RETAIL_TABLE` ⇒ 它们连走到门前的机会都没有）。

## 2. 处置清单

| # | 动作 | 对象 |
|---|---|---|
| 1 | 门子句退场 | hunt `requireBriefing` 删 `CHAIN_MISSING` / `TERMINAL_UNEXPECTED` 两段 + `isFlagClearingAction` + `Optional` 导入；**保留** `TALK_NPC_UNRESOLVED` / `TALK_NPC_AMBIGUOUS` / `BRIEFING_SLOT_CONFLICT` |
| 2 | 穿线删除 | hunt：`briefingChains` 形参从 `compile` / `compileCanonical` / `compileSequentialStages` / 私有 `compile` / `build` 全部移除（+ nonnull）；collect：`compile` 形参 + `BriefingStep.chain` 字段 + `RETAIL_BRIEFING_CHAIN_UNREGISTERED` 子句（**保留** `TALK_NPC_WITHOUT_COLLECT_STEP`）；DD 编译器（2 签名 + 转发 + 2 调用实参）；driver（常量 + 字段 + 构造参 + 赋值 + 加载块 + 4 实参） |
| 3 | 删读取者类 | `RetailClientBriefingChains.java` |
| 4 | 删表 | `quest_client_briefing_chains.tsv`（3230 行）——快照 `retired-tsv/quest_client_briefing_chains.tsv.retired-20260927`（sha256 `35c38ad56eef5e2fc90b1b409ca2f7aeb4edf110ecffec561c7b41f0135c0762`） |
| 5 | **冻结夹具** | 同内容副本落 `src/test/resources/quest/retail-client-briefing-chains.tsv`（同 sha256，sha 级同源） |
| 6 | **新增常设门** | `RetailBriefingChainEvidenceGateTest`：登记行数冻结 3225；SimpleHunt 非空 `talk_npc1` 行**恰 47**、SimpleCollectItem **恰 5**；每行必须有登记且**末跳为 SETPRO 家族终点（`pageId=0`）**；并注册进 T1（`affected_quest_tests.py` `T1_GATE_CLASSES`） |
| 7 | 清单三步 | 删清单行 + `EXPECTED_TSV_COUNT` **23 → 22** |
| 8 | 测试夹具 | 4 个门测（Equivalence / Family / DataDriven / CollectItem）删字段 + 加载块 + 实参 |

## 3. 门禁

| 门 | 结果 | 证据 |
|---|---|---|
| 聚焦（新证据门 / 清单门 / 等价门 / 采集族门 / DD 门） | **18 测试 1 红**：新门 **1/1 绿**、清单门 **3/3**、等价门 **2/2**、采集族门 **6/6**、DD 门唯一红 = foreign 在册红（20035） | `/private/tmp/w5g4-focus1.log` |
| **T1**（固定套件 **87** 测试，含本片新门；`QUEST_FORK_COUNT=2`） | **ADDED 0 / REMOVED 0**；红身份集 sha256 `3b92439da8052988f0acd3a23a646786a78c3ac58e7c64a9c1dcabdd1d5cd137` 与基线**逐字节相同**（唯一红 = foreign `20035`；**新门在 T1 内实跑并绿**） | `gates/T1-230021.log` + `gates/T1-w5g4-reds.txt` |
| **T2 / T3** | 合并到 W5-g3+W5-g4 收口后一次跑（用户指示：聚焦 + 加速；两片的切片级证据已由等价门/家族门/新门 + 指纹恒等式承担） | — |

## 4. 沉淀

- 记忆库：**QE-095**（W5-g3 派生判据）之外，本片补 **QE-096**（"零拒绝守卫"退役法：先证零拒绝 + 覆盖不变量 →
  门子句删除 + 不变量迁构建期常设门 + 基数冻结）。
- 与 W5-g1/g2 的差异：那两张是**值死门死**（纯机械退役）；本张是**值死门活但零拒绝**（守卫型），因此多了
  "不变量迁构建期"一步。

## 5. 最终门禁（收口时回填）

| 门 | 结果 | 证据 |
|---|---|---|
| T1（含新常设门，87 测试） | **ADDED 0 / REMOVED 0**，sha256 同上 | `gates/T1-230021.log` |
| T2（链选择器 285 id） | 合并收口扫的一部分（T3 覆盖全树，主树 T2 按“聚焦 + 加速”指示未单独重跑） | — |
| T3（仓库外全树副本，`forkCount=2`） | **2017 测试**；红集 98 = 基线 97 + 1 条**副本假红**（`QuestDialogMigrationGateTest` 读 `.agents/summary/quest/generate_quest_dialog_enums.py`，首跑副本 `--exclude .agents` 致 NoSuchFile；主树 4/4 绿）⇒ **含 `.agents` 副本重跑**：`gates/T3-w5g34b.log` | `gates/T3-w5g34.log` / `T3-w5g34b.log` |

## 6. 纪律回执

未 commit/push；未启停服务；**未新增页码类 TSV**（新增物 = 测试侧冻结夹具 + 常设门测试类）；未创建 worktree；
兄弟车道文件只读（`affected_quest_tests.py` 仅按 W5-g1 既有惯例追加 1 行 T1 注册）；AI 中间产物落
`.agents/summary/quest-native-dispatch/`。
