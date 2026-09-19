# 任务 15001 SECTION_0 报告行闭环修复：客户端验收记录（ACCEPTED）

```text
quest: 15001（同族结构 sweep：15020/15073/15100/15104/15203/15406/15407/15408/15580/15671/25671/25060/18952）
user acceptance confirmation: 用户 2026-09-19 明确回复“客户端验证完成，已修复”，未限定分支或步骤，按项目规则视为该任务的完整游玩验收
server launch mode: 用户管理的服务端实例（本轮未启动、停止或重启服务端）
repository commit: c34458083（修复提交；本次验收文档与 Playbook 记录随后单独提交）
working tree: 本任务路径已提交；工作区仍有其他并行任务脏文件（不属本任务范围）
Aion 5.8 client/data provenance: Aion 5.8 客户端解包数据 quest_monster.csv（15001 SECTION_0==0 + SECTION_1/SECTION_2<5）与 Dialogs/10000_19999/quest_q15001.html（quest_summary %5/%8/%9）；仓库内映射证据见 docs/quest/client-dialog-mapping/client-monster-progress-contracts.csv
npc template/object: 报告 NPC 804698（努贝斯）；修前运行时 objectId=59592（用户提供的 SM_DIALOG_WINDOW/CM_DIALOG_SELECT 追踪）
map/instance: 绿雾湿地野外任务；world ID 本轮未采集
```

## 验收内容（用户确认）与修前追踪

- 玩家报告：两组各 5 只击杀完成后没有进入下一步，任务窗口计数显示 `(/5)`。
- 修前服务端追踪（用户提供，2026-09-19 09:41–09:42）：
  - 接取后 `SM_QUEST_ACTION 任务=15001 状态=3 步数=0`；
  - 第一组：`64 → 128 → 192 → 256 → 320`（`SECTION_1=1..5`）；
  - 第二组：`4416 → 8512 → 12608 → 16704 → 20800`（`SECTION_2=1..5`，`SECTION_1` 保持 5）；
  - 终态：`状态=4 步数=20800`，即 `var0=0, var1=5, var2=5`。
- 修复后用户回复“客户端验证完成，已修复”：最终击杀直接切到报告步骤（`SECTION_0=1`，wire `20801`），任务说明显示“和努贝斯对话”，不再停在击杀行。

## 预期与实际的字段级合同

- source state/status/vars：`START`，`var0=0`，`var1/var2` 计数中。
- action/page/button：击杀最后一组怪物的 `kill-npc` 事件。
- expected response：priority 0 的 `started -> reward` 路线在同一事务内写 `var0=1`（并保留计数上限），after-commit 顺序 `LEVEL_AND_VISIBILITY_REFRESH`；目标 `reward` 投影 `var0=1, var1=5, var2=5`。
- actual response（用户实机）：状态进入 `REWARD` 且客户端任务说明切换到报告行；旧 `REWARD/var0=0` 存档由无 source 的 `enter-world` 迁移修复路线在同一次登录/换图时纠正为 `var0=1`。

## 验证边界与门禁

- 已执行（用户授权，2026-09-19 同型 sweep 后复跑）：`mvn -q -Dtest='QuestSection0ReportRowContractTest,QuestMonsterProgressContractAuditTest,ClientQuestSectionAlignmentTest,ProductionCatalogWhitelistVerificationTest,QuestDefinitionCatalogManifestTest' test` → PASS；（15001 修复当轮执行的是不含新测试的同一组合，同样 PASS）`PRODUCTION_COMPILE_OK=6189`、`PRODUCTION_COMPILE_FAILURES=0`、`PRODUCTION_INTERACTION_OBJECT_FAILURES=0`、`PRODUCTION_WHITELIST_VIOLATIONS=0`。
- startup health：本轮未采集服务端启动日志（用户管理进程）；如出现 `Can't initialize typed quest engine` / `QuestCompilationException`，本验收不成立。
- runtime logs / protocol trace / screenshots：本任务记录引用用户提供的修前追踪（未落库）；附件 `not captured`（用户提供的临时图片缓存路径已失效）。
- 同族任务：14 个任务是同型批量结构修复，本次实机验收覆盖代表任务 15001；其余 13 个按同合同静态与门禁覆盖，未逐个实机复验。
- 同类排查：2026-09-19 后续 sweep 已修复 246 个同型任务（244 个批量 + 18994/28994），残余 15101/24153 需额外路线重建；见 `.agents/summary/quest-15001-multicounter-step/2026-09-19-section0-report-row-sweep.zh-CN.md` 与 `section0-report-row-closure-residual.csv`。

acceptance status: ACCEPTED
matched Pattern: `KILL_COUNTER_COMPLETION_ADVANCES_JOURNAL_ROW`（Playbook 新增，代表提交 c34458083）；相关跨域模式 `QE-012`（说明行索引固定读 SECTION_0）与 `QE-018`（最终计数事件进入 REWARD）
matching fields: 多计数器/单计数器 step-0 击杀任务、`reward` 投影、最终击杀路线、`LEVEL_AND_VISIBILITY_REFRESH`、旧存档迁移
differing fields: 15001 为双计数器同步饱和；同族含三段（15203/25060）与四段（18952）计数

remaining risks: 未逐任务实机复验 14 个同族任务；「一组已满、另一组未满」时的计数行显示未用修后版本单独截图；250 个同类候选任务待授权 sweep。
