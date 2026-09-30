# QE-111：台账裁定码必须与真端漂移登记同源（4 行滞后修正 + 常设门）

> 用户 Goal（2026-09-30）：「摒弃当前的任务引擎，完全按真端来，当前任务引擎只负责现有的任务 XML 运行。」
> QE-110 收尾时顺带发现：DD 族的保留台账 `ADJUDICATED:<码>` 与冻结漂移登记 `REJECTED:<码>` 在 4 行上不同源。
> 本片把码归一，并把「台账码 = 漂移登记码」做成常设门（retention 是派生视图，禁止反过来按台账改判）。

## 1. 判据：码权威在冻结漂移登记，台账只能跟随

1. **码权威** = `src/test/resources/quest/retail-data-driven-drift.tsv`（每行 `REJECTED:<现行编译拒绝码>` / `ADOPTED`），
   它由 `RetailDataDrivenGateTest.driftVersusShellsIsRegistered` 与**现行编译结果逐行**锁死（任何漂移立刻红）。
2. **retention 台账是派生视图**：QE-098 已立规则——「码权威在冻结 drift fixture（retention 是滞后派生视图，
   禁从 retention 反推）」。本仓也已有同型先例：`.agents/summary/quest-native-dispatch/b1-acquire-sentinel/b1_apply.py`
   对 9 行做过同样的事（注释原文 `retention-lag fix: code realigned to frozen drift`）。
3. **双向一致**：DD 族 1508 行的台账必须是 `RETAIL_TABLE/OK` ⟺ 漂移登记 `ADOPTED`；
   `XML_RETENTION/ADJUDICATED:<码>` ⟺ 漂移登记 `REJECTED:<码>`。二者之间不存在第三种合法状态。

## 2. 逐行修正（4 行，两个副本同片改）

| quest | 旧台账码 | 新台账码（= 漂移登记） | 依据 |
|---|---|---|---|
| 25051 | `ADJUDICATED:RETAIL_TALK_HUNT_CHAIN_DEFERRED` | `ADJUDICATED:CURATED_LEGACY_CONTRACT_LOCK` | 该行已在 `RetailDataDrivenGateTest.CURATED_DEFERRED` 登记（TalkFOBJ 相对刷怪轴超出 talk+hunt 词汇）；页梯退役后旧拒绝成因消失，现行分类 = curated 覆盖码 |
| 80885 | `ADJUDICATED:RETAIL_ACQUIRE_NPC_UNRESOLVED` | `ADJUDICATED:RETAIL_ITEMPLAY_ACQUIRE_EVENT_DEFERRED` | `category_acquire_=ItemPlay`（`value0=quest_80885a`）+ `category1=event` ⇒ `RetailDataDrivenDefinitionCompiler.java:94` 在**事件轴先于道具解析**延后；旧码「接取 NPC 解析失败」与数据不符 |
| 80940 | 同上 | 同上 | `value0=quest_80940a`，同形 |
| 80961 | 同上 | 同上 | `value0=doc_quest_80961a`，同形 |

编译器源码里对该轴的注释已经把性质写清（`RetailDataDrivenDefinitionCompiler.java:85-94`）：
「…与本轴的『使用道具触发』冲突…按更准确的码暂缓（**不再谎报「接取 NPC 解析失败」**）…事件行整体延后，
不因道具表变动在 ITEM_UNRESOLVED/EVENT_DEFERRED 之间漂移」——即旧台账码正是被编译器作者判定为误报的那个码。

机理数据（逐行证据见同目录 `qe-111-adjudication-code-realign.tsv`）：

```text
/80885/ acquire=ItemPlay value0=quest_80885a reward=world_event_Repe_01    dev=[이벤트] 교육용_내키지 않는 전달 1(천)
/80940/ acquire=ItemPlay value0=quest_80940a reward=world_eduevent_blackbroker dev=[이벤트] 교육용_수상한 쪽지02
/80961/ acquire=ItemPlay value0=doc_quest_80961a reward=World_TEST_padet_dryarare_01 dev=[이벤트/교육] 크로메데 처치하기(아이템/사냥)
```

## 3. 新增常设门（本片唯一的代码改动）

`RetailDataDrivenGateTest#adjudicatedRetentionCodesMatchTheDriftRegistry`（新用例，`RetailDataDrivenGateTest.java:504-549`）：

1. 遍历冻结漂移登记的全部 1508 行：`ADOPTED` ⇒ 台账必须 `RETAIL_TABLE/OK`；
   `REJECTED:<码>` ⇒ 台账必须 `XML_RETENTION/ADJUDICATED:<同码>`，逐字相等；漂移行在台账缺失也报错。
2. 同片锁住 retention **双副本逐字节一致**（生产 `src/main/resources/.../retail-xml-retention.tsv`
   ↔ 门测试 `src/test/resources/quest/retail-xml-retention.tsv`）——双副本漂移是本仓反复踩过的坑。
3. **反证（本片实测，非纸面断言）**：临时把 `80885` 改回旧码并重跑该用例 →
   门红并给出精确信息
   `80885: 台账码与漂移登记不同源，台账=ADJUDICATED:RETAIL_ACQUIRE_NPC_UNRESOLVED 漂移=REJECTED:RETAIL_ITEMPLAY_ACQUIRE_EVENT_DEFERRED`；
   恢复后该用例绿。说明门有牙，不是同义反复。

## 4. 验证

| 项 | 结果 |
|---|---|
| 聚焦 | `RetailDataDrivenGateTest` 8 用例：新增用例绿；仅 `frozenFingerprintsCoverExactlyTheRetiredQuests`（80817 既存红）红 |
| T1 | `run_quest_gates.sh T1`（`QUEST_FORK_COUNT=2 QUEST_LOG_DIR=target/agent-logs/qe111`）82 例红 2 例 = 同一组既存红（日志 `T1-225543.log`） |
| T3 | 1857 例红身份集 `target/agent-logs/qe111/t3.ids` = **198 条**，对 `target/agent-logs/qe107b/t3.ids` = **ADDED 0 / REMOVED 0**（日志 `T3-225606.log`） |
| 退役守恒 | `verify_retirement.py` → `catalog=746 directory=746 retired=5478 sum=6224` / `OK: 目录一致，无悬空生产引用` |
| 双副本 | `diff -q` 相等（并由新增门常设锁定） |

## 5. 既存红（本片未引入、未顺手回写）

1. `RetailDataDrivenGateTest.frozenFingerprintsCoverExactlyTheRetiredQuests`：`80817` 指纹冻结值 `f5245215…` vs 实际 `00a65835…`（QE-102 起刻意保留）。
2. `RetailNonIrAxisGateTest.cappedQuestsAreNeverRetailDriven`：26 条封顶登记行在 HEAD 上已是 `RETAIL_TABLE`（需单独成片 + 用户裁定）。
3. `CollectTurnInClientActionAlignmentBatchTest#quest1137…` / `#quest18745…`：HEAD 既存红。

## 6. 边界与未做

- **无行为变更**：只改台账原因列与门禁；owner、任务 XML、编译器、漂移/指纹登记全部未动 ⇒ 不含真机验收项。
- 本门只管 **DataDriven 族（1508 行）**；其它族的 `ADJUDICATED:` 码由各自家族门负责
  （如 `RetailSimpleItemPlayGateTest.ADJUDICATED_CODES`、`RetailSimpleHuntFamilyGateTest` 的拒绝登记表）。
- 未重跑生成器 `build_retention_list.py`（该脚本会把 evidence 列重写为生成文本，属另一条流水线；本片手工同片改双副本并加门兜底）。

## 7. 剩余 DD 桶（下一片候选）

`ADOPTED` 1463 / `CURATED_LEGACY_CONTRACT_LOCK` 14 / `RETAIL_ITEMPLAY_ACQUIRE_EVENT_DEFERRED` 6 /
`RETAIL_TALK_JOURNAL_MISSING` 5 / `RETAIL_HUNT_MULTI_STAGE_DEFERRED` 4 / `RETAIL_TALK_COLLECT_CHAIN_DEFERRED` 4 /
`RETAIL_ACQUIRE_GRANT_UNSUPPORTED` 2（HOLD）/ `RETAIL_REWARD_NPC_UNRESOLVED` 2 / `RETAIL_ITEMPLAY_ITEM_UNRESOLVED` 2 /
`RETAIL_ACQUIRE_NPC_SENTINEL_AREA_PENDING` 2（QE-110 已封板）/ `RETAIL_COLLECT_ITEM_SHAPE` 2 /
`RETAIL_ITEMPLAY_OUTPUT_UNRESOLVED` 1 / `RETAIL_TALK_HUNT_CHAIN_DEFERRED` 1。
