# 任务 10529「保护之实体2」客户端验收记录（ACCEPTED）

```text
quest: 10529（ELYOS 任务）；同形镜像 20529 未单独客户端验收
user acceptance confirmation: 用户 2026-09-24 回复「客户端验证成功，提交」（未限定步骤或分支），按 quest-repair.md 规则 4 视为 10529 整条任务可玩；不推断 20529 已验收
server launch mode: not captured；服务端由用户管理，本次未启动、停止或重启
repository commit: 5bf5b70a2（本地 quest 分支，10529/20529 XML 与回归测试）
working tree: dirty；有大量并行暂存与未暂存改动，本任务仅提交 3 个修复文件；验收记录、案例及本任务总结单独随文档提交
Aion 5.8 client/data provenance: Aion 5.8 客户端 active HTML 映射见 docs/quest/client-dialog-mapping/quest-dialog-action-details.csv（10529 SELECT10/SELECT11 页链）；旧 handler 来源 origin/history；本轮未新采集客户端包 SHA-256
npc template/object: 倒下贤者 806294、报告代理人 806075；报障时日志 targetObj=175632 对应模板 ID 未由日志确认，验收时 object ID not captured
map/instance: 10529 XML 中地下矿山 world-id=301690000，倒下贤者交接后目标 world-id=210100000；验收运行时 instance ID、坐标与服务器模式 not captured

steps:
1. 用户先提供 boss 击杀后 10529 的 START/var0=9 日志，倒下贤者反复显示通用 page 10；补 s9 直接入口后任务书高亮带贤者向代理人报告。
2. 用户截图显示代理人报告行已高亮，但在 806075 处没有任务；补 s10 直接入口并通过聚焦/生产门禁后，用户确认客户端验证成功，视为整条任务完成。
3. 重登、断线、副本重建、镜像任务 20529 和重复完成分支的单独步骤 not captured。

source state/status/vars: 报障日志 19:42:33 的 SM_QUEST_ACTION 状态 3 / packed=12297，解包 START/var0=9,var1=0,var2=3；验收后的权威任务状态包 not captured
action/page/button: s9 首次 USE_OBJECT(-1) -> SELECT10(4080) -> 4081 -> 4082 -> SETPRO10(10009)；s10 在 806075 首次 USE_OBJECT(-1) -> SELECT11(6500) -> 6501 -> 6502 -> SET_SUCCEED(10255)；reward preview 与完成动作保留
expected response: s9 交接提交 START/var0=10 并给工作物品、传送、同步和关窗；s10 交接提交 REWARD/var0=11 后状态同步与关窗；随后在 806075 奖励预览与领取完成
actual response: 用户 2026-09-24 确认「客户端验证成功，提交」，未提供成功路径的逐包轨迹/终态截图，不虚构后续 packet/status 数值

startup health: actual startup logs not captured；2026-09-24 聚焦 Maven 20/20 通过、BUILD SUCCESS；生产目录门禁 PRODUCTION_COMPILE_OK=2441、PRODUCTION_COMPILE_FAILURES=0、PRODUCTION_INTERACTION_OBJECT_FAILURES=0、PRODUCTION_WHITELIST_VIOLATIONS=0；未观察到运行时启动 WARN/ERROR 的日志
runtime logs: 修复前 2026-09-23 用户粘贴 QUEST-TRACE 摘录（包含 12297 与反复 questId=0/page=10）；成功后的完整日志/附件及 SHA-256 not captured
protocol trace: 修复前 19:42 片段来自用户文本；成功后的 CM_DIALOG_SELECT / SM_DIALOG_WINDOW / SM_QUEST_ACTION 顺序及 SHA-256 not captured
screenshots/recordings and SHA-256: 2026-09-24 用户提供报告行高亮截图，原件是临时缓存不可作为稳定附件；成功后的截图/录屏 not captured

acceptance status: ACCEPTED_NEW_PATTERN（10529）；20529 PENDING_CLIENT
matched Pattern: ACTIVE_NPC_DIALOG_MISSING_DIRECT_ENTRY；matched fields = START 阶段 NPC 首次 -1 入口缺失、通用 page10/任务行不可见、客户端页面与动作链原本存在、XML source 限域补路由；differing fields = s9 为实例倒下 NPC、s10 为主城报告代理人；representative commit 5bf5b70a2、JournalReportRowSplitContractTest#agentInitialTalkOpensTheReportPageWithoutAClientQuestRow
remaining risks: 镜像 20529 未实机验收；重登/副本恢复/异常断线未单独复测；成功路径与服务端启动日志未采集，验收只对用户本次实际路径负责
```

## 证据引用

- 修复与运行时定位：`.agents/summary/quest-10529-fallen-npc/2026-09-23-fallen-sage-dialog-entry.zh-CN.md`。
- Playbook 代表案例：`docs/quest/repair-playbook/CASES.zh-CN.md` 8.49；指纹：`docs/quest/repair-playbook/PATTERNS.zh-CN.md`。
- 修复提交：`5bf5b70a2`；Maven 门禁结果来自该提交代码与并行脏工作区的 2026-09-24 运行，后续日志未采集。
