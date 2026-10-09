# 任务 10529/20529「保护之实体2」客户端验收记录(ACCEPTED)

```text
quest: 10529(ELYOS)+ 20529(ASMODIANS 镜像,同日同修同测);修复面:计数纯净(reported marker)+ 领奖行回滚(journal blank)
user acceptance confirmation: 用户 2026-10-09 回复「实机验证成功,提交」;验证形态 = GM 置 REWARD/var0=10 后任务书显示正确 + 实机验证成功;REWARD/11 空白形态与 REWARD/10 正确形态同日均由实机取证(报障 trace + 对照验证)
server launch mode: not captured;服务端由用户管理,重启/测试均由用户执行
repository commit: 本记录随本次修复提交(quest 分支)
working tree: dirty;并行任务改动并存,本次提交仅显式暂存本任务文件
Aion 5.8 client/data provenance: 客户端解包 ~/PycharmProjects/unpak(data_unpacked/Dialogs/10000_19999/quest_q10529.html、20000_29999/quest_q20529.html 的 quest_summary 12 行与行 0/行 11 复述;Quest_unpacked/quest.xml 10529 条目 collect_progress=2);本轮未新采集客户端包 SHA-256
npc template/object: 报告代理人 806075(天)/ 806079(魔);倒下贤者 806294/806299;targetObj=15436 对应 806075(2026-10-09 trace)
map/instance: 地下矿山 301690000;报告回城 210100000(天)/220110000(魔);运行时 instance ID not captured

steps:
1. 2026-10-09 上午用户报「向维达报告,任务都正确,和维达汇报也没问题,就是维达头上没有任务标记显示」,附 QUEST-TRACE(packed 12296/12297 = var0=8/9, var1=0, var2=3);诊断为 s6 计数 var2=3 残留污染 packed 步数(QE-044 同根因),修复 = 成功链清零 + enter-world 自愈边。
2. GM 跳步验证(//quest delete 10529 → set 10529 START 10/0/0)与维达走完报告链:SET_SUCCEED → REWARD/11 纯净(trace 状态=4 步数=11),但「任务书对话步骤没了」;取证裁决该形态为批次 4 误推「末行 11」(行 11 = 行 0 复述行,legacy 落盘 10),修复 = reward 投影回滚 10、SET_SUCCEED 不回写、恢复边反转 REWARD/11 → 10。
3. 用户以 //quest set 10529 reward 10 对照实机:任务书显示正确;随后回复「实机验证成功,提交」——视为 10529 REWARD 形态 + 头顶标记两修复面整体验收通过;20529 镜像同日同修,契约测试同绿(未单独实机走查,按同构镜像登记 PENDING_CLIENT→镜像以本次门禁与 10529 实机背书,不推断独立实机验收)。

source state/status/vars: 报障 packed=12297(START/var0=9,var1=0,var2=3);SET_SUCCEED 后 REWARD/var0=11(状态=4 步数=11,trace 11:54:53);GM 对照 REWARD/10;验收后的权威任务状态包 not captured
action/page/button: s10 在 806075 首次 USE_OBJECT(-1) → SELECT11(6500) → 6501 → 6502 → SET_SUCCEED(10255) → REWARD/var0=10(修复后);npc-complete 领奖链保留
expected response: SET_SUCCEED 提交 REWARD/var0=10(legacy 落盘值),任务书高亮行 10(实义末行「带上陷入沉睡的德扎波波,向代理人维达报告」),行 11 为复述行永不点亮(真端行为,10528 勘误同型);维达头顶任务标记在 s10(START/10)与 REWARD/10 下均显示
actual response: 用户确认「实机验证成功,提交」;REWARD/10 对照由用户主动 GM 置值并确认显示正确;成功路径的逐包轨迹/终态截图 not captured

startup health: 用户重启服务端加载新 XML(时间 not captured);聚焦测试经 IDEA MCP 全绿(JournalReportRowSplitContractTest 9/9、Quest10529BossKillCounterContractTest 2/2,exitCode 0)
runtime logs: 报障 QUEST-TRACE 摘录由用户文本提供(11:13-11:14 与 11:54:50-53 两段);完整日志/SHA-256 not captured
protocol trace: CM_DIALOG_SELECT/SM_DIALOG_WINDOW 页链 6500→6501→6502→10255 由 QUEST-TRACE 留档;其余 not captured
screenshots/recordings and SHA-256: not captured(用户口头确认 + GM 对照结果)

acceptance status: ACCEPTED_NEW_PATTERN(10529 QE-044 计数纯净变体 + QE-054 复述行回滚判例);20529 PENDING_CLIENT(同修同门禁,未单独实机走查)
matched Pattern: QE-054(LEGACY_REWARD_STEP_IS_AUTHORITATIVE,2026-10-09 增补)+ QE-044(计数残留污染整型步数,杀怪变体);representative evidence = .agents/summary/quest-10529-counter-residue-marker/2026-10-09-packed-counter-residue-hides-agent-marker.zh-CN.md
remaining risks: 20529 未单独实机走查;同歧义带 34 个任务(10525/10526 等)仍待逐个实机复核;重登自愈边(REWARD/11→10)的实机单测面由契约测试锁定,未单独走查
```

## 证据引用

- 修复与裁决留档:`.agents/summary/quest-10529-counter-residue-marker/2026-10-09-packed-counter-residue-hides-agent-marker.zh-CN.md`。
- 同型勘误先例:`.agents/summary/quest-10527-reward-row/2026-09-21-10527-reward-row-and-family-audit.zh-CN.md`(10528 勘误与 36 任务歧义带清单)。
- memory-bank:QE-051 第 8 条修订、QE-054 2026-10-09 增补(`.agents/memory-bank/patterns/quest-engine.md`)。
