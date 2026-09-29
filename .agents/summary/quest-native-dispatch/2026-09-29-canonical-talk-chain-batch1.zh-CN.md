# SimpleTalk 规范形多步对话链落地与第一批 8 个任务物理退役

## 一、 背景与根因定性

1. **伪代码本质排查**：
   `RetailClientTalkChainSteps.java` 与测试目录下的 `quest_client_talk_chain_steps.tsv`（4,990 行）实质是把旧 XML 状态机的一步一连线、微观页码阶梯（1011、1352 等）进行了逐字转录，是用 TSV 模拟旧 XML 的伪驱动；
   并且在生产打包时，测试资源文件不入包，导致运行时回退为空；
   此外，TSV 内写死的固定奖励索引（如 `fixed=0 1 2`）与真端 `quest.xml` 实际奖励数量脱节，导致 `2231` 等任务在 `buildChain` 中直接抛出 `IndexOutOfBoundsException`。

2. **真端规范生命周期模型（Canonical Talk Chain）**：
   对于带 `talk_npc1..talk_npcM` 的多步任务，摆脱对页码 TSV 的逐字拟合，按真端规范模型驱动：
   - 布局：单一 `var0`（6-bit，0..63）；
   - 节点：`unaccepted` (NONE, 0) → `started` (START, 0) → `step1` (START, 1) → ... → `stepM` (START, M) → `reward` (REWARD, lastRow) → `complete` (COMPLETE, 0)；
   - 接取：`canonicalAcceptFlow(acquiredNpc, "started", ...)`（直发接取窗页 4，1002/20000 提交建档，接取发物）；
   - 推进：第 $i$ 步 NPC（$i=0..M-1$）在 `source` 节点监听 `QUEST_SELECT`（从客户端契约动态推导开窗）及 `10000 + i`（`SETPRO{i+1}`，推进 `var0 = i + 1`，目标切至 `step{i+1}`），末步同时支持 `SET_SUCCEED` (10255)；
   - 交付与完成：第 $M$ 步 `step{M}` 对话 `rewardNpc` 直达 `reward` 交付分档奖励窗，挂接 `completeFlow`。

## 二、 改造范围与变更清单

1. **核心编译器** `RetailSimpleTalkDefinitionCompiler.java`：
   - 实现 `buildCanonicalChain(...)`；
   - 调整 `precheck`：未在 `chainSteps` 登记的多步任务，只要 NPC 链解析无歧义且非复合轴，放行走规范形对话链合成；
   - 调整 `compile`：未在 `chainSteps` 的多步任务切入 `buildCanonicalChain`。

2. **测试门禁** `RetailSimpleTalkGateTest.java`：
   - 增加 `inspectCanonicalChain`，严格断言规范形多步任务的进度域、4 态节点集、状态与 var0 投影、接取入口、逐步推进及交付完成分支。

3. **第一批退役 8 个多步 SimpleTalk 任务**：
   - 任务列表：`1314, 1471, 2428, 3201, 4201, 21244, 80479, 80483`；
   - 物理删除 `src/main/resources/aion/data/static_data/quest_definition/quests/<id>.xml`；
   - 从 `quest_definition_catalog.xml` 移除上述 8 个可执行 XML 定义声明；
   - 更新 `retail-xml-retention.tsv`（主目录及测试目录），标记为 `RETAIL_TABLE SimpleTalk OK retired-xml-in-git-history`；
   - 同步更新 `retail-simple-talk-drift.tsv` 实际分类（`DIFF:TRANSITION_SET` / `DIFF:NODE_PROJECTION`）。

## 三、 验证结果

- `mvn test -Dtest=RetailSimpleTalkGateTest`：4/4 PASSED
- `mvn test -Dtest=RetailSimpleTalkChainGateTest`：8/8 PASSED
- `mvn test -Dtest=RetailOwnershipGateTest`：4/4 PASSED
- `mvn test -Dtest=RetailQuestDriverOverlayTest`：5/5 PASSED
- 全量 21 个相关门禁全部绿灯。
