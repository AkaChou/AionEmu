# 缺口批 5–8 收口台账（客户端契约 40 + 领奖/计数 33 + 余量 16 + 裁定族 6 = 95 行 · quest-native-dispatch）

> SEMANTIC_GAP：95 → **0**（DoD 判据 ① 达成）；全部零行为裁定改名 + drift 对码；里程碑 M2（批3+4）、
> M3（批5+6）、M4（批7+8）逐轮 T1∥T3 验收。

## 批 5：客户端契约族（40 行）

| 裁定码（ADJUDICATED:） | 行数 | 判据 |
|---|---|---|
| `DIFF:TRANSITION_SET` / `DIFF:NODE_PROJECTION`（内嵌） | 28 | CLIENT_REPORT_VARIANT 17 + CLIENT_ROUTE 11 的 drift 权威归属：真端合成与 XML 转写的形状 DIFF **在册登记**（等价类），XML 侧保留客户端 authored 变体页/路由 ⇒ 裁定 = XML 是客户端合同的正确供货者 |
| `CLIENT_BUTTON_UNWIRED` | 11 | 修复期 XML 按钮集（80751-80770 等）在真端合成形中无路由；家族门 `RETAIL_TABLE_GAPS` 类（行必须仍合成） |
| `REPORT_NPC_DIVERGENCE` | 1 | 2237：客户端点名不同报告人（collect 族登记在册） |

## 批 6：领奖/计数族（33 行）

| 裁定码（ADJUDICATED:） | 行数 | 判据 |
|---|---|---|
| `RETAIL_COUNTER_EXCEEDS_6BIT` | 14 | 客户端计数超过 6-bit SECTION 字段上限（63）——固定 6-bit 布局是客户端不变量（QE 既有口径），XML 带自定义计数布局 |
| `RETAIL_REWARD_NPC_UNRESOLVED` | 13 | 交付 NPC 本服缺失（无法合成交付边） |
| `RETAIL_REWARD_NPC_FACTION_COMPOSITE` | 4 | 复合势力交付名在客户端登记表无交付 NPC 集 |
| `RETAIL_REWARD_NPC_NPC_UNRESOLVED` | 2 | 30720/30723（use-item 族）交付 NPC 缺失 |

## 批 7：余量（16 行）

| 裁定码（ADJUDICATED:） | 行数 | 判据 |
|---|---|---|
| `RETAIL_ENTERAREA_ZONE_UNRESOLVED` | 4 | 进区域 zone 本服不可解析 |
| `RETAIL_ITEMPLAY_ACQUIRE_EVENT_DEFERRED` | 3 | 世界事件接取轴延期（交接包既登记：真端缺口不从物品轴攻） |
| `QUEST_SPAWN_UNEXPRESSED` | 2 | 任务刷怪轴不可表达（14112/14123，P0c-6 裁定、测试证据锁在册） |
| `RETAIL_ADVANCE_UNEXPRESSED` | 2 | 80255/80256（ItemPlay 门白名单在册） |
| `RETAIL_COLLECT_ITEM_SHAPE` | 2 | 采集形不被真端合成支持 |
| `WORK_ITEM_GRANT_AND_COLLECT_ROUTE` | 1 | 1137：工作物品发放+采集复合路由 |
| `XML_EXTRA_REWARD` | 1 | 16961：XML 多出客户端任务书点名奖励（RETAIL_TABLE_GAPS 类，行必须仍合成） |
| `COLLECT_PROGRESS_ROUTE` | 1 | 28503：collect_progress 进度行投影路由 |

## 批 8：裁定族（6 行）

| 裁定码（ADJUDICATED:） | 行数 | 判据 |
|---|---|---|
| `CURATED_LEGACY_CONTRACT_LOCK` | 6 | DD 门 `CURATED_DEFERRED` 守卫在册的暂缓行（重整形裁定未落）：保留 XML + 分类码逐字（`curatedDeferralsStayOnXmlUntilReshaped` 持续强制） |

## 记录

- 全部 95 行零生产代码变化、零形状变化、零 flip ⇒ 无新增实机复验项（复验清单维持批 1 的 24 行）。
- 生成器停写/同步移交项汇总：`build_quest_client_report_pages.py`（W5-g1 起）、
  `build_quest_client_dialog_exits.py`（批 0 起）——均属 scriptdll-quest-driver 车道，本车道不改；
  `build_retention_list.py` 已滞后于本阶段全部手工裁定（ENV-004 规则 5：禁整表重跑，重跑会回退
  裁定行）——**移交项：车道 owner 决定是否把 ADJUDICATED 语义补进生成器**。
- 未 push；未启停服务；未新增/退役 TSV（`EXPECTED_TSV_COUNT` = 22 保持）。

## 批 5–8 收口记录（终态）

- **批 7+8 执行**：`tools/b78_apply_renames.py`（双副本 22 行 ×2=44 处翻转，旧码逐行断言
  `SEMANTIC_GAP:<码>` 相符后改 `ADJUDICATED:<码>` + 裁定 evidence；未命中/重复即 fail-closed）。
  翻转后双副本逐字节恒等：`SEMANTIC_GAP=0`、`ADJUDICATED=571`。
- **唯一源码联动**：`RetailSimpleItemPlayGateTest.ADJUDICATED_CODES` 补
  `RETAIL_ADVANCE_UNEXPRESSED`（80255/80256 批 7 翻转后，门内 reason 前缀断言必须同步；
  其余 5 个受影响门的前缀不变式经 `stripReasonPrefix` 中立，逐码语义继承无需改动）。
- **快筛（批 7+8 末）**：6 门一次 mvn（ItemPlay / HuntFamily / DataDriven / CollectItem /
  Ownership / HuntEquivalence）——唯一红 = 在册 T1 基线红
  `RetailDataDrivenGateTest.driftVersusShellsIsRegistered`（字节恒等红集的那一条），其余全绿。
- **M3 里程碑（批 5+6 后）**：T1 红集 1 条 `3b92439d…`、T3 红集 97 条 `ce4673c7…`
  ——与基线逐字节恒等（`gates/T1-m3-reds.txt` / `gates/T3-m3-reds.txt`）。
- **缺口曲线总账**：595 → 412（批 0+1）→ 304（批 2）→ 95（批 3+4）→ 22（批 5+6）→ **0**（批 7+8）。
- 全程零 flip、零形状变化、零生产编译单元变化 ⇒ 无新增实机复验项（复验清单维持批 1 的 24 行）。
