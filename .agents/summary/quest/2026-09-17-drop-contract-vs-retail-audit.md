# 2026-09-17 掉落契约真端基线审计与修复（QE-030）

## 1. 判定口径

真端 `Quest_unpacked/quest.xml` 的掉落字段使用**名称**（`drop_item_N = quest_1367a`、`drop_monster_N = Rhinoce_32_An`），
离线无法把名称映射成数字 ID，因此采用**按怪物加权的结构比对**（名称无关）：

- 真端每组 `drop_monster_N / drop_item_N / drop_prob_N` → 展开为“每只怪一次掉落机会，概率 = drop_prob_N”；
- 生产 XML 按 `<drop npc-id item-id chance>` 逐条统计；
- 比对项：`{概率 → 怪物只数}` 直方图 + 掉落道具种数。生产把同一批怪拆成多行视为等价；**任一概率档怪物数
  低于真端、或掉落道具种数低于真端**即为丢失来源。

可复算脚本：`.agents/summary/quest/twin-pair-scan/audit_drop_shape_vs_retail.py`；基线生成脚本：
`.agents/summary/quest/twin-pair-scan/generate_drop_retail_contract.py`。

> 首版以旧版 `quest_data.xml` 为基准曾命中 489 条，其中大量“100 → 80”实为**旧数据被修正过**：
> `1402/1403/1482` 真端 `drop_prob=80` 与生产一致、旧版写 100。旧数据只能作为迁移漂移旁证。

## 2. 全库结果与逐任务判定

初次扫描：1,158 个有真端掉落契约的任务中 84 条不一致。

| 任务 | 真端 | 旧 quest_data | 生产（修复前） | 判定与处置 |
|---|---|---|---|---|
| `15400` | 3 个野外箱 @100，3 件 | 702830/702831/702832 | **0 条** | **真缺陷**：三个 `<drop>` 整体丢失，s3 交付 3 件道具永远无法满足 → 已恢复（collecting-step=3，与 `s3 -> s3 USE_OBJECT` 对齐） |
| `51022` | Event_Cargobox @100 | 701470 | **0 条** | **真缺陷**：货箱掉落丢失，`npc-item-report` 交付无法满足 → 已恢复 |
| `50019` | ValentineEvent_Brownie_Solo @100 | 无（活动任务） | **0 条** | **真缺陷**：活动怪掉落丢失（npc 模板名核对 = 219315）→ 已恢复 |
| `14016` | 2 怪 @100 | 1 怪 | 1 怪 | **保真缺口**：补回 210753（`IDAbGateL1_Noble_Vritra_High_As_20_Q_An`） |
| `21107` | 2 怪 @80 | 1 怪 | 1 怪 @100 | **保真缺口**：215897 概率改 80，补回 216535（`DF4_Hiiv_DR_53_An`） |
| `1127` | OldCube_Q19 @100 | 无掉落 | 0 条 | **非缺陷**：生产由同一对象(700001) `USE_OBJECT -> give-item` 直接发放，等价获得路径（列豁免） |
| `2927` | 4 怪 @50 + 1 怪 @100 | 9 怪 @100 | 6 怪 @100 | 生产含真端 5 只 + 1 只额外来源；概率已按真端逐怪收敛（4 只改 50） |
| `25604` | 6 只 Fanatic @100 | 6 怪 1 道具 | 1 只祭品守护者 | **刻意重构**：703125 同时是击杀计数目标与掉落来源、each-member 且可完成（列豁免） |
| `1419` / `2631` / `14014` | 7 / 1 / 14 怪 | 8 / 1 / 16 怪 | 8 / 2 / 16 怪 | 生产为真端**超集**，非阻断（门禁按“不得低于基线”放行） |

概率收敛：**75 个任务**由 100% 改为真端档位——67 个统一档（80×53、60×6、50×4、75×2、85×1、33×1）+ 8 个修正档，
6 个混合档（`1160/1342/20502/20525/20529/80136`）先按 **npc 模板名 → ID** 核对（如 `DF5_C_FrillFaimam_Baby_57_n = 219681`）
再逐行对齐，避免“整任务统一改概率”把不同档位的怪改错。

修复后形态审计仅剩 6 条：4 条为超集（门禁放行）、2 条为有证据豁免。

## 3. 新增全库门禁（QE-030）

1. **基线资源** `src/test/resources/quest/quest-drop-retail-contract.tsv`（1,158 行）
   列：`questId` / 掉落道具种数 / `概率:怪物只数,...`；由真端 `quest.xml` 生成，脚本入库可复算。
2. **豁免清单** `src/test/resources/quest/quest-drop-contract-exceptions.tsv`（仅 `1127`、`25604`，逐条写明证据理由）。
3. **门禁测试** `QuestDropContractGateTest#productionDropsKeepTheRetailPerMonsterChanceContract`
   编译全量目录后逐任务比对：任一概率档怪物数或掉落道具种数低于基线即失败；豁免必须存在于基线中（防错配）。

## 4. 验证结果

```bash
mvn test -Dtest=QuestDropContractGateTest,QuestMovieAndDialogLoopRegressionTest,QuestDefinitionDirectoryLoaderTest,CompletedQuestPrerequisiteRegressionTest,QuestClientContractGateTest,ProductionCatalogWhitelistVerificationTest
```

`Tests run: 31, Failures: 0, Errors: 0, Skipped: 0`；`PRODUCTION_COMPILE_OK=6186, FAILURES=0, WHITELIST_VIOLATIONS=0`。

首轮运行曾因 XSD 约束失败并即时修正：`<drops>` 必须位于 `metadata` 序列的 `rewards/reward-groups` 之后、
`drop` 的 `collecting-step` 为必填属性（`15400/51022/50019` 三处），修正后全绿。

## 5. 边界与后续

- 名称→ID 无法离线映射，本门禁是“按怪物加权的结构契约”，不是逐道具逐怪精确比对；若拿到真端
  quest item / monster 名称表，可升级为精确比对；
- `each-member` 组合语义、以及任务目录之外的来源（商店/合成/其他系统）不在本门禁范围；
- `51022`/`50019` 的活动 NPC 依赖活动配置生成（本检出无 spawn 记录），掉落行已按真端补齐，
  活动期间仍需实机确认活动怪确实生成；
- 未启动/停止/重启服务器进程。
