# 击杀计数与重复开局修复：客户端/真机验收包（PENDING）

```text
quest: 13758/13761/13764/13767（5 杀族）、25640、25698、2677、13841、26930
user acceptance confirmation: not provided yet（尚未收到用户客户端验收确认）
server launch mode: not started（本轮未启动服务端）
repository commit: 464df58bb（修复提交）；本轮门禁提交随本次门禁改动一并落地
working tree: 门禁与账本文件为本轮新增，未改动他人并行工作
Aion 5.8 client/data provenance: Aion 5.8 客户端解包数据 quest_monster.csv / data_driven_quest.xml
  （客户端 SECTION_1<N 门控与 data_driven 数量）；仓库证据见
  .agents/summary/quest-counter-audit/2026-09-18-counter-kill-and-repeat-gate-alignment.zh-CN.md
npc template/object: 13765 报告 NPC 805272/805273/805274；19636/19640 报告 NPC 798155/798991；
  25640 报告 NPC 806101；25698 报告 NPC 806804；2677 报告 NPC 204817；26930 报告 NPC 804627
map/instance: 13758 族 Levinshor 野外；2677 为实例/组队任务（副本内开局与再开局）
```

## 待验收步骤（每步记录 expected / actual）

1. **13758 族 5 杀**：接取 13758/13761/13764/13767 任一，击杀目标怪物 **正好 5 只** → 客户端狩猎步骤显示完成，可与报告 NPC 对话进入领奖（修复前需要 15/12 只）。
2. **13765 同族对照**：确认同族 5 杀行为一致（本任务未改其计数，作为对照）。
3. **25640 / 25698 满计数恢复**：击杀未满时与报告 NPC 对话 → **不得**出现报告/领奖页；击杀满（30 / 5）后再对话 → 可报告并进入领奖窗口。
4. **2677 完成后重开局**：完成一轮后与 NPC 204817 对话 → 开局页 SELECT1_1 可再次打开并能重新接取；若处于不可重复状态则不显示开局页。
5. **13841 无目标自动领奖**：进入 REWARD 后在客户端使用“无目标自动领奖”（action 108）→ 奖励到账且任务完成。
6. **26930 交付扣量**：携带 10 个 `186000257` 交付 → 精确扣除 10 个（背包若有多余同名道具应保留）。

## 验收边界

- 本轮只完成静态编译门禁、结构门禁与 planner 模拟门禁；**未**启动服务端、**未**做客户端实操。
- 未验证分支：真端 UI 上 `SECTION_1` 计数显示与击杀数的对应关系（决定 `quest-kill-counter-overkill-pending.tsv`
  中 34 个“引擎多要一只”任务是真缺陷还是客户端计数偏移）。
- 附件：not captured（无截图/录屏/抓包）。
