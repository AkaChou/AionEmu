# 缺口批 3+4 收口台账（闲聊链族 115 + 交付/制造/步骤族 94 = 209 行 · quest-native-dispatch）

> SEMANTIC_GAP：304 → **95**（−209，全部零行为裁定改名 + 滞后码对齐）；里程碑 M2 验收。

## 批 3：闲聊链族（115 行 = 基线 106 + 批 1 码对齐 9）

逐行以 **drift fixture 为权威**（§5-4 retention 滞后纪律；`retail-simple-talk-drift.tsv` /
`retail-data-driven-drift.tsv` 逐行对码）：

| 裁定码（ADJUDICATED:） | 行数 | 判据 |
|---|---|---|
| `RETAIL_TALK_HUNT_CHAIN_DEFERRED` | 32 | talk+hunt 混合链轴对"阶梯/阶段不足"形未实现（drift detail `ladders=0 talks=N stages=0`） |
| `RETAIL_TALK_CHAIN_DEFERRED` | 23 | talk 链轴未覆盖形 |
| `RETAIL_TALK_CHAIN` | 21 | 深层链形分歧（15 行经 drift 对码修正归属） |
| `RETAIL_TALK_CHAIN_COMPOUND` | 17 | 复合链轴（12 行由 TALK_CHAIN 对码修正 + 原 5） |
| `RETAIL_TALK_CHAIN_NO_START` | 10 | 无 NPC_START 块（3 行对码修正 + 原 7） |
| `RETAIL_TALK_CUTSCENE` | 12 | 过场轴行（P0c-10k/10n metadata 隔离形） |
| `RETAIL_TALK_JOURNAL_MISSING` | 5 | 客户端任务书缺行 ⇒ 奖励投影不可派生（QE-051 + W5-g3 fail-closed 门，设计性拒绝） |
| `RETAIL_TALK_COLLECT_CHAIN_DEFERRED` | 4 | talk+collect 混合链轴 |
| `RETAIL_TALK_NPC_UNRESOLVED` | 2 | 对话 NPC 本服缺失 |
| `DIFF:TRANSITION_SET`（内嵌） | 4 | 真端合成与 XML 转写转移集不同（drift DIFF 等价类登记） |
| `RETAIL_TALK_CHAIN`（ItemPlay 18213/28213） | 2 | itemplay+talk 链轴；ItemPlay 门白名单在册 |

## 批 4：交付/制造/步骤族（94 行）

| 裁定码（ADJUDICATED:） | 行数 | 判据 |
|---|---|---|
| `RETAIL_HANDIN_VOCABULARY_UNSUPPORTED` | 48 | **与 HandinDialogFlow 三票否决同源**（客户端五页词汇逐页镜像 = 其合同本意；被 DD 链编译器共享消费；HandoverContinuationContract 独立锁定）⇒ 交接包 §7.4 预判成立，属"有理由的例外"族 |
| `RETAIL_CRAFT_AXIS_UNDECLARED` | 28 | 制作进度轴（CombineTask 族，游戏内制作产物即进度）真端表无列可表达；无 drift 行（族拒绝登记在 CombineTask 门） |
| `RETAIL_TALK_HUNT_CHAIN_DEFERRED` | 16 | 由 `STEP_UNSUPPORTED` 对码修正（drift 权威） |
| `RETAIL_ITEMPLAY_ITEM_UNRESOLVED` | 2 | 由 `STEP_UNSUPPORTED` 对码修正 |

## 记录

- 全部 209 行零生产代码变化、零形状变化、零 flip ⇒ 无新增实机复验项。
- 9 行批 1 码对齐行（TALK_HUNT_CHAIN_DEFERRED 8 + TALK_COLLECT_CHAIN_DEFERRED 1）在本批一并裁定。
- 未 push；未启停服务；未新增/退役 TSV。
