# 任务 1131「未送达的防具」中继推进关窗 客户端验收记录（ACCEPTED_EXISTING_PATTERN）

```text
quest: 1131（ELYOS「未送达的防具 / Undelivered Armour」，category important，zone02，min-level 10）；镜像/后续分支未单独验收
user acceptance confirmation: 用户 2026-10-05 回复「客户端验证成功 提交」（未限定分支或步骤），按 quest-repair.md 规则 4 视为 1131 整条任务可玩
server launch mode: IDEA（常驻 Spring Boot 进程，classpath=target/classes）；本次复测进程由用户于 17:16:25 重启（此前 14:59 进程为旧字节码，曾致 17:13 一次假阴性）
repository commit: f60f97633（quest 分支：SimpleTalk/SimpleItemPlay 中继推进 after-commit → 关窗 + 族门断言；记忆库 QE-141/142 更新随 6a888b801 入库）
working tree: dirty；并行任务（接取面双边收口、minion-contract 等）改动保留未暂存；本任务文档单独提交
Aion 5.8 client/data provenance: Aion 5.8 客户端；页面面 = 客户端任务页契约 src/main/resources/aion/definitions/quest_dialog/client_dialog_contract.tsv（1131：4/1003/1004/1011/1012/1352/1353/2375）；本轮未新采集客户端包 SHA-256
npc template/object: 接取 203097（运行时 object 26025）；中继/交付 799093（运行时 object 24164）；修复前 15:17 旧进程轨迹为 22194/21355（同模板，换线后 object 变化）
map/instance: not captured（Verteron zone02 任务；运行时 world/instance ID 未采集）

steps:
1. 17:19:19 与 203097 接取 1131：1011 → 1012 → 1007 → 页 4 → 1002 → SM_QUEST_ACTION 状态=3 步数=0 + 页 1003。
2. 17:19:26 打开中继 NPC 799093 任务列表（页 10）→ 动作 31 → select2(1352) → 1353 → 17:19:28 点「结束对话。」（动作=10000＝SETPRO1，交付交易凭证 doc_quest_1131b）。
3. 重登、重复推进、第 2/3 中继步、魔族侧分支未单独复测。

source state/status/vars: 点击前 START/var0=0；点击后写步号 1
action/page/button: select2_1(1353) 页按钮「结束对话。」= HACTION_SETPRO1(10000)
expected response: 步进 SetProgress(1) + SM_QUEST_ACTION 状态=3 步数=1 + 关窗（SM_DIALOG_WINDOW targetObj=0/questId=0/页=0），零发页
actual response: 17:19:28,958 SM_QUEST_ACTION 状态=3 步数=1；17:19:28,959 SM_DIALOG_WINDOW targetObj=0 questId=0 下发页=0；用户确认第一次点击即关窗（修复前旧进程为下发页 10 + 第二个「结束对话」）

startup health: 服务端启动日志 not captured（生命周期由用户管理）；本轮授权门禁：QuestProductionStartupGateTest 2/2 通过（生产目录 707 行无契约违规）
runtime logs: log/quests.log 2026-10-05 17:19:19-17:19:28（7 行修复后轨迹；仓库相对路径，SHA-256 not captured）
protocol trace: 上述 SM_QUEST_ACTION/SM_DIALOG_WINDOW 行为片段来自仓库日志文本（行 2604-2611 区间），完整附件与 SHA-256 not captured
screenshots/recordings and SHA-256: 修复前两张截图（双「结束对话」现象）为临时缓存、不可作稳定附件；修复后未采集

acceptance status: ACCEPTED_EXISTING_PATTERN
matched Pattern: 记忆库 QE-141（RELAY_DIALOG_OPEN_AND_SUBPAGE_ECHO，推进 after-commit 2026-10-05 回调为关窗）+ QE-142（0x5d8 关窗语义）；Playbook `MULTI_STEP_QUEST_GOLDEN_RULE`（代表 772809b10，MissionFamilyDefinitionTest#barringTheGateAdvancesThroughEightLinearStopsThenTheSentinel）——matched fields = SELECT(N+1)→SETPRO(N+1)→sync→页 0 关窗闭环；differing fields = 表车道中继面（真端 cabb10）而非 XML 车道；representative test = SimpleTalkNativeFamilyGateTest#relayRowSelectionOpensTheStepDialog、SimpleItemPlayNativeFamilyGateTest#activatedRelayRowsRunTheWholeChainEndToEnd
remaining risks: ① 复测前须确认服务端已重启（14:59 旧进程曾致 17:13 假阴性；重启前该批 XML/字节码改动均不生效）；② 第 2/3 步（10001/10002）与重复推进未单独实机（族门已锁）；③ 1115/1118 等其它 SimpleTalk 中继任务按同族修复，未逐一实机；④ 魔族侧与重复完成分支未复测
```

## 证据引用

- 修复证据与普查：`.agents/summary/quest-1131-relay-close-dialog/README.zh-CN.md`（含 15:17 报障 trace、真端 FUN_180cabb10 取证、退役 XML 旁证、17:13 旧进程假阴性记录）。
- 修复提交：`f60f97633`；记忆库卡更新：`6a888b801`（QE-141/QE-142 与派生索引）。
- Playbook 指纹：`docs/quest/repair-playbook/PATTERNS.zh-CN.md` 的 `MULTI_STEP_QUEST_GOLDEN_RULE`。
