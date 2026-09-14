# 剩余 NO_MATCH 分诊报告（基于 cfddeead8 后的旧 E2E 报告）

> 生成时间: 2026-08-21 11:01
> 报告来源: `target/quest-e2e/quest-e2e-report.jsonl` (mtime 2026-08-19, 269 NO_MATCH)
> 已修复: 135 qids (cfddeead8, COUNTER_SOURCE_PROJECTION_NO_LOCK) — 167 行 NO_MATCH, 与报告中 135 qids 全覆盖
> 剩余: 76 qids / 102 行 NO_MATCH — 本报告仅对这 76 分诊；报告整体 269 行未重跑，102 行为真实剩余，167 行为已修复但报告未刷新

## 关键结论

- 剩余 76 中，仅 **4 个** (4711, 30710, 49702, 49715) 属于与 cfddeead8 同型的 `started var0=0` 锁死 reward，可直接批量修；其余 72 不适用该补丁。
- 51 个 `KILL_COMPLETION_SIBLING` 是击杀完成分支与 continuing 分支的 sibling 归因差异，E2E fixture 在 `started {}` 无约束节点上用 `pack=目标所需值` 探测 completion 变体，但当前 transition 要求 `var0=目标值` 的同步/功绩完成条件时，prepare 的非击杀对话事件与 projection 锁混在一起；真实生产链需按 hit/quest 流程单任务取证，不宜批量改 var。
- 17 个 `DIALOG_COMPLETION` (3910/3912/3922-3927/4922-4933/19074/29075) 为 skill/物品完成后的 `SELECT_QUEST_REWARD` 对话，其 `started {}` 无投影，condition 要求 `var>=阈值`，E2E 用 `pack=20/10` 探测 REWARD 分支，但 dialog 协议准备未携带对应 var 变化，属于 fixture 探测与生产对话链差异，需个案核对客户端可见页。
- 2 个 `DIALOG_PROJECTION_LOCK` (1614/11216) 为 `started var0=0` 与 `QUEST_SELECT dlg=31 var0=1` 的对话投影冲突，E2E 用 `pack=1` 探测 var0=1 时的 SELECT2，但 source 仍为 started var0=0，属于对话可见性布局缺口，非计数锁。
- 21120 等 2 个为孤例，需单独核对 legacy 任务。

## 分类汇总

| 分类 | qids | 行数 | 建议 |
|---|---:|---:|---|
| `COUNTER_PROJECTION_LOCK` | 4 | 11 | 优先批量修（同 cfddeead8） |
| `KILL_COMPLETION_SIBLING` | 51 | 70 | 单任务取证 |
| `DIALOG_COMPLETION` | 17 | 17 | 单任务/小批对话链核对 |
| `DIALOG_PROJECTION_LOCK` | 2 | 2 | 单任务/小批对话链核对 |
| `USE_ITEM_COMPLETION` | 1 | 1 | 孤例 |
| `OTHER` | 1 | 1 | 孤例 |

## 详情

### COUNTER_PROJECTION_LOCK (4 qids, 11 行)

- Q4711: started->reward KILL_NPC dlg=0 pack=2 | locks=[('started', 'reward', 'variable-is', 'var0', 0, 2)]
- Q30710: started->reward TALK_TO_NPC dlg=1009 pack=1 | locks=[('started', 'reward', 'variable-at-least', 'var0', 0, 1)]
- Q49702: started->reward KILL_NPC dlg=0 pack=6; started->reward TALK_TO_NPC dlg=1009 pack=6; started->reward TALK_TO_NPC dlg=1009 pack=6 | locks=[('started', 'reward', 'variable-at-least', 'var0', 0, 6), ('started', 'reward', 'variable-at-least', 'var0', 0, 6)]
- Q49715: started->reward KILL_NPC dlg=0 pack=10; started->reward KILL_NPC dlg=0 pack=10; started->reward KILL_NPC dlg=0 pack=10 | locks=[('started', 'reward', 'variable-at-least', 'var0', 0, 10), ('started', 'reward', 'variable-at-least', 'var0', 0, 10)]

### DIALOG_PROJECTION_LOCK (2 qids, 2 行)

- Q1614: started->started TALK_TO_NPC dlg=31 pack=1 | locks=[('started', 'started', 'variable-is', 'var0', 0, 1)]
- Q11216: started->started TALK_TO_NPC dlg=31 pack=1 | locks=[('started', 'started', 'variable-is', 'var0', 0, 1)]

### DIALOG_COMPLETION (17 qids, 17 行)

- Q3910: started->reward TALK_TO_NPC dlg=10002 pack=20
- Q3912: started->reward TALK_TO_NPC dlg=10002 pack=20
- Q3922: started->reward TALK_TO_NPC dlg=10002 pack=10
- Q3923: started->reward TALK_TO_NPC dlg=10002 pack=10
- Q3924: started->reward TALK_TO_NPC dlg=10002 pack=10
- Q3925: started->reward TALK_TO_NPC dlg=10002 pack=10
- Q3926: started->reward TALK_TO_NPC dlg=10002 pack=10
- Q3927: started->reward TALK_TO_NPC dlg=10002 pack=10
- Q4922: started->reward TALK_TO_NPC dlg=10002 pack=10
- Q4923: started->reward TALK_TO_NPC dlg=10002 pack=10
- Q4924: started->reward TALK_TO_NPC dlg=10002 pack=10
- Q4925: started->reward TALK_TO_NPC dlg=10002 pack=10
- Q4926: started->reward TALK_TO_NPC dlg=10002 pack=10
- Q4929: started->reward TALK_TO_NPC dlg=10002 pack=10
- Q4933: started->reward TALK_TO_NPC dlg=10002 pack=20
- Q19074: started->reward TALK_TO_NPC dlg=10002 pack=10
- Q29075: started->reward TALK_TO_NPC dlg=10002 pack=10

### KILL_COMPLETION_SIBLING (51 qids, 70 行)

- Q1842: started->m1 KILL_NPC dlg=0 pack=80
- Q3205: started->s16 TALK_TO_NPC dlg=10001 pack=15
- Q3725: started->started TALK_TO_NPC dlg=31 pack=61824; started->reward TALK_TO_NPC dlg=1009 pack=61824
- Q4205: started->s16 TALK_TO_NPC dlg=10001 pack=15
- Q4725: started->started TALK_TO_NPC dlg=31 pack=61824; started->reward TALK_TO_NPC dlg=1009 pack=61824
- Q10035: s5->s6 KILL_NPC dlg=0 pack=581
- Q15314: s2->s3 KILL_NPC dlg=0 pack=3778
- Q16805: started->reward KILL_NPC dlg=0 pack=1856
- Q16806: started->reward KILL_NPC dlg=0 pack=10048; started->reward KILL_NPC dlg=0 pack=6016
- Q16807: started->reward KILL_NPC dlg=0 pack=1856
- Q16823: started->k1 KILL_NPC dlg=0 pack=256
- Q18314: started->reward KILL_NPC dlg=0 pack=7
- Q18951: started->reward KILL_NPC dlg=0 pack=25
- Q18972: started->reward KILL_NPC dlg=0 pack=6
- Q18973: started->reward KILL_NPC dlg=0 pack=6
- Q18974: started->reward KILL_NPC dlg=0 pack=6
- Q24030: s6->s7 KILL_NPC dlg=0 pack=3142
- Q25314: s2->s3 KILL_NPC dlg=0 pack=3778
- Q25406: h2->reward TALK_TO_NPC dlg=1009 pack=258; h1->h2 KILL_NPC dlg=0 pack=193
- Q25407: h2->reward TALK_TO_NPC dlg=1009 pack=258; h1->h2 KILL_NPC dlg=0 pack=193
- Q25408: h2->reward TALK_TO_NPC dlg=1009 pack=258; h1->h2 KILL_NPC dlg=0 pack=193
- Q25580: h2->reward TALK_TO_NPC dlg=1009 pack=1282; h1->h2 KILL_NPC dlg=0 pack=1217
- Q26803: started->reward KILL_NPC dlg=0 pack=1856
- Q26805: started->reward KILL_NPC dlg=0 pack=1856
- Q26806: started->reward KILL_NPC dlg=0 pack=10112
- Q26807: started->reward KILL_NPC dlg=0 pack=1856
- Q26823: started->s1 KILL_NPC dlg=0 pack=256
- Q26828: started->reward KILL_NPC dlg=0 pack=256
- Q26829: started->reward KILL_NPC dlg=0 pack=576
- Q26988: started->reward KILL_NPC dlg=0 pack=256
- Q27160: started->reward KILL_NPC dlg=0 pack=2662976; started->reward KILL_NPC dlg=0 pack=2658944; started->reward KILL_NPC dlg=0 pack=2400896
- Q27161: started->reward KILL_NPC dlg=0 pack=2662976; started->reward KILL_NPC dlg=0 pack=2658944; started->reward KILL_NPC dlg=0 pack=2400896
- Q29631: started->reward KILL_NPC dlg=0 pack=9
- Q29632: started->reward KILL_NPC dlg=0 pack=9
- Q29633: started->reward KILL_NPC dlg=0 pack=9
- Q29635: started->reward KILL_NPC dlg=0 pack=9
- Q29636: started->reward KILL_NPC dlg=0 pack=9
- Q29637: started->reward KILL_NPC dlg=0 pack=9
- Q29638: started->reward KILL_NPC dlg=0 pack=9
- Q29639: started->reward KILL_NPC dlg=0 pack=9
- Q29640: started->reward KILL_NPC dlg=0 pack=9
- Q29641: started->reward KILL_NPC dlg=0 pack=9
- Q29642: started->reward KILL_NPC dlg=0 pack=9
- Q29691: started->reward KILL_NPC dlg=0 pack=2
- Q30005: started->reward KILL_NPC dlg=0 pack=24; started->reward TALK_TO_NPC dlg=1009 pack=25
- Q30514: started->started KILL_NPC dlg=0 pack=1; started->reward TALK_TO_NPC dlg=1009 pack=2
- Q30516: started->reward TALK_TO_NPC dlg=1009 pack=1; started->reward TALK_TO_NPC dlg=1009 pack=1
- Q30564: started->started KILL_NPC dlg=0 pack=1; started->reward TALK_TO_NPC dlg=1009 pack=2
- Q30708: started->started KILL_NPC dlg=0 pack=1; started->started KILL_NPC dlg=0 pack=6; started->reward TALK_TO_NPC dlg=1009 pack=7
- Q30715: started->reward TALK_TO_NPC dlg=1009 pack=1
- Q30758: started->started KILL_NPC dlg=0 pack=1; started->started KILL_NPC dlg=0 pack=6; started->reward TALK_TO_NPC dlg=1009 pack=7

### USE_ITEM_COMPLETION (1 qids, 1 行)

- Q15042: started->reward USE_ITEM dlg=0 pack=2

### OTHER (1 qids, 1 行)

- Q21120: started->m1 KILL_NPC dlg=0 pack=10

## 下一批建议（Playbook 5.6/9.1）

1. 仅将 `COUNTER_PROJECTION_LOCK` 4 个作为下一批批量候选：4711 (var0=0->2 KILL 无 increment)、30710 (var0=0->1)、49702 (var0=0->6)、49715 (var0=0->10, Danaria 600060000 已标注暂缓，需确认是否跳过)。
2. 其余 72 不进入批量脚本，按 Playbook 5.3 单任务取证：对每个 qid 核对 legacy handler/客户端页/世界可达性，缺证据则归入 EVIDENCE_REQUIRED，不猜测改动。
3. 修复该批次后必须重跑 `mvn -Dtest=QuestE2eBatchAudit ...` 刷新 `quest-e2e-report.jsonl`，否则 269 行旧数会持续误导；本次分诊已通过 `fixed135 ∩ NO_MATCH=135` 交叉校验，但仍需新报告校正。
