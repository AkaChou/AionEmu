# 任务 3058 客户端验收记录

quest: 3058「Stone Of Mabolo / 马波路之石」（天族，Theobomos；Oileus 交付链：Oileus→Lavirintos→Siraus）

user acceptance confirmation: 用户 2026-10-08 原话「实机验证成功」；未限定分支或步骤，按规则视为 3058 整条任务可玩（复测口径：旧档进世界自愈 / Oileus 链 步数=1 / Lavirintos 链 步数=2 + 移除 3058A / Siraus 领奖）。

server launch mode: IDEA（常驻 Spring Boot 进程，classpath=target/classes）；服务端重启由用户管理（本次修复需重启后生效，用户已重启）。

repository commit: `f84393452`（修复：SimpleUseItem 族中继面按 QE-141 同形重写）；沉淀提交 `98ba6fc69`；验收记录与本提交同批。

working tree: dirty；并行会话的物件 owner 收敛批次（1582/21105/2232/2237/2307/2664/28302/28303/30211/4004/4012 等 XML）保留未暂存，未纳入本任务提交。

Aion 5.8 client/data provenance: Aion 5.8 客户端解包；`Dialogs` 下 `QUEST_Q3058.html` 的 quest_summary 三行（`[%0]`→Oileus / `[%3]`→Lavirintos / `[%6]`→Siraus）；任务页契约 `client_dialog_contract.tsv`（3058：4/1352/1353/1693/1694/2375）；本轮未新采集客户端包 SHA-256。

npc template/object: talk_npc1 = Oileus（798189，GUARD）、talk_npc2 = Lavirintos（203701）、reward_npc = Siraus（798213）；运行时 object ID not captured；接取/工作道具 ITEM_QUEST_3058A = 182208041。

map/instance: Theobomos（world 210060000；由 spawns/Npcs/210060000_Theobomos.xml 的 798189 出生点推断）；运行时 world/instance ID not captured。

steps:

1. 使用 ITEM_QUEST_3058A（182208041）接取 → 接取窗页 4 → 确认。
2. 与 Oileus（798189）：任务行/对话（31/26）→ 页 1352（select2）→ 翻页（1353/select2_1）→「结束对话」（SETPRO1=10000）→ 关窗 + 步数=1。
3. 与 Lavirintos（203701）：31 → 页 1693（select3）→ 翻页（1694/select3_1）→ SETPRO2（10001）→ 关窗 + 步数=2 + 移除 ITEM_QUEST_3058A。
4. 向 Siraus（798213）报告 → 领奖（REWARD → COMPLETE）。
5. 旧存档自愈：此前已与 Oileus 对话过（bit16 私编 65536）的角色进世界，由 `SimpleUseItemHandler.onEnterWorld` 归一为 var0=1 并重发状态。
6. 重登、重复接取、死亡、跨角色/职业未复测。

source state/status/vars: `unaccepted/NONE/var0=0` → 接取 `started/START/var0=0` → Oileus 后 `var0=1` → Lavirintos 后 `var0=2` → Siraus `REWARD` → `COMPLETE`；旧档 `65536` 进世界归一 `1`。

action/page/button: 中继面 31/26 → 该步页（1352/1693，带 questId）；1353/1694 = 客户端声明的子页动作原样回发；SETPRO1/2（10000/10001）= 「结束对话」按钮，推进写 var0=K + 关窗（真端 cabb10 0x5d8、零发页）。

expected response: 任务书三行随 `var0=0/1/2` 逐行切换（Oileus→Lavirintos→Siraus），无步骤空白（旧私编 65536 会整块空白）；每次「结束对话」一次点击即关窗；第 2 步移除接取道具。

actual response: 用户确认「实机验证成功」；逐步 trace、截图与协议抓包 not captured。

startup health: not captured（服务端由用户重启管理）；未报告 typed quest engine 初始化失败或 `QuestCompilationException`；离线门禁 `QuestProductionStartupGateTest` 2/2、族门 `SimpleUseItemNativeFamilyGateTest` 14/14（2026-10-08，IDEA MCP）。

runtime logs: not captured。

protocol trace: not captured。

screenshots/recordings and SHA-256: not captured。

acceptance status: ACCEPTED_EXISTING_PATTERN

matched Pattern: `RELAY_DIALOG_OPEN_AND_SUBPAGE_ECHO`（QE-141）；matched fields = 31/26/-1 → 该步页、子页动作原样回发、SETPRO{K} → `var0=K` + 关窗（0x5d8 零发页）、跳步零响应、重复/乱序关窗兜底、进世界自愈；differing fields = 用物族的**链满让位交付面**（1559 的 talk_npc1 = reward_npc 边界；本任务 relay 与 reward 不相交，无实机面）；representative commit = `d12e4236e`（14120）；本次测试 = `SimpleUseItemNativeFamilyGateTest`（14 例，含 `SimpleUseItemNativeFamilyGateTest#quest3058RelayWritesVar0AndRemovesItsItemAtStepTwo`）。

remaining risks: ① 用物族其余 talk 行（54 行：1 步 28 / 2 步 16 / 3 步 10，含换物与 item_check 门面）未逐一实机——族门已锁；② 重登/重复接取（本任务 max_repeat_count=1 不可重行）/死亡路径未复测；③ 旧存档自愈的逐步 trace not captured（用户确认为整链验收）；④ 独立观察候选：SimpleSerialHunt 简报位 0x40000000 接取下发（9622/30600/30610），与本任务无关联，观察口径见 `.agents/summary/quest-3058-relay-dialog/README.zh-CN.md`。
