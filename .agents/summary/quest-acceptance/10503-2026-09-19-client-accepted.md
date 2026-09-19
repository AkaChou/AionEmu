# 任务 10503「Guard Down, Secrets Out / 提亚马特结界守护者」客户端验收记录（ACCEPTED）

```text
quest: 10503（同批同因 8 个主线任务：10504、20504、10506、10507、10527、20527、10528、20528）
user acceptance confirmation: 用户 2026-09-19 回复“10503 验证成功，提交”，确认实机游玩验证成功
server launch mode: 用户管理的服务端实例（本轮未启动、停止或重启服务端）
repository commit: 随本交付批次提交落地
working tree: dirty；严格隔离并保留并行的 mesh/geoEngine/startup-perf 修改，仅提交本次任务与同批同因任务的 XML、门禁测试与沉淀文档
Aion 5.8 client/data provenance: Aion 5.8 客户端解包数据 Dialogs/10000_19999/quest_q10503.html、docs/quest/client-dialog-mapping/quest-dialog-action-details.csv:664-674；collect_progress=2
npc template/object: 尤碧亚 804705；前置击杀怪物 236252（提亚马特结界守护者）；采集物 702670（古老龙族文件）
map/instance: Cygnea (希哥尼亚 / 210070000)

steps:
1. 前置接取任务 10503，进入步骤 1 (s1)。
2. 击杀 3 只提亚马特结界守护者 (236252)：
   - 击杀第 1 只：步数由 1 变为 65 (var0=1, var1=1)
   - 击杀第 2 只：步数变为 129 (var0=1, var1=2)
   - 击杀第 3 只：触发 s1->s2 推进，修复前 var0=2 但 var1 残留为 2（步数变 130，污染高位）；修复后 actions 显式清零 var1=0，步数下发为纯净的 2。
3. 采集古老龙族文件 (702670)：
   - 读条完成获取道具 182215603，修复后下发 PACKET_ONLY 同步包并关闭对话窗口。
4. 前往龙地外庭院与尤碧亚 (804705) 对话：
   - 修复前：客户端记录步数 130，判定不在 collect_progress=2，头顶无任务气泡，点击下发通用第 10 页。
   - 修复后：客户端整型步数为 2，尤碧亚头顶亮起任务对话标记，点击正常展示 select3(1693) 并触发 check_user_has_quest_item(39) 交付。
5. 交付成功后扣除 182215603，展示 check_user_item_ok(10000)，续接 select4(2034) 解读对白，领物 SETPRO4(10003) 推进至 s4。

source state/status/vars: s1 (var0=1, var1=2) -> s2 (var0=2, var1=0)
action/page/button: kill-npc 236252 -> set var0=2, var1=0; 702670 USE_OBJECT -> sync PACKET_ONLY; 804705 QUEST_SELECT -> SELECT3(1693)
expected response: 击杀满 3 只怪进入 s2 时下发纯净步数 2；采集 702670 后持物前往 804705 正常触发对白并完成交付
actual response: 用户实机游玩验证通过，确认“10503 验证成功，提交”

acceptance status: ACCEPTED
matched Pattern: QE-044 (COLLECT_PROGRESS_PREMATURE_ADVANCE_DIALOG_DROPPED & MULTI_STAGE_COUNTER_LEAK)
remaining risks: 10503 实机验证通过；同批同因 8 个主线任务已由自动化契约门禁 QuestCollectProgressAlignmentGateTest 严格锁定并全绿，未逐个登录客户端实机跑通。
```

## 证据引用
- 根因排查与复盘分析：`.agents/summary/quest-10503/README.md`
- 契约门禁测试：`QuestCollectProgressAlignmentGateTest` (6/6 PASS)、`Quest10503ClientDialogAlignmentTest` (1/1 PASS)、`Quest10504ClientDialogAlignmentTest` (1/1 PASS)
- 生产 Catalog 门禁：`QuestDefinitionCatalogManifestTest` (10/10 PASS)、`QuestItemSourceContractGateTest` (3/3 PASS)
