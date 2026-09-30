# P0c-29：宽松窗口执行——80290 翻转 + armour 自愈边落地 + 全部待窗断言 lenient 回放绿


> **生成物清理提示（2026-09-30）**：本文件正文提到的 `*.tsv` / `*.log` / `*.txt` / `*.xml` 等中间转储已随 `chore(agents)` 清理删除；原文与留痕仍可从 git 历史取回，被删清单与再生成入口见 `.agents/summary/CLEANUP-LEDGER.zh-CN.md`。

> 日期：2026-09-26 ｜ 切片：P0c-29 ｜ lane：静默 1h+（在飞态仍冻结，全量门仍阻）｜ 前序：P0c-28

> **性质**：风暴仍持稳（missing=664），但本 lane 各任务均为真端 owner 且可编译——把窗口判官的
> 断言以 lenient overlay 逐任务回放（临时探针），把 P0c-24~28 的翻转与通道**全部落地并验证**。
> 正式窗口收窄为"判官复跑 + T2/T3 门禁"。

## 落地（本片净变更）

1. **armour 自愈边发射落地**：`RetailSimpleTalkDefinitionCompiler.build()` 末插入 P0c-28 草案的
   4 行发射边（登记驱动 + 登记与投影不一致 fail-closed）——P0c-28 窗口包的第 6 件（唯一 lane 热
   触碰，mtime 复核 05:57 未动后落笔）。
2. **80290 翻转落地**：`p0c28_retire_80290_row.py` 运行 + verify_retirement =
   **catalog=1430 directory=1430 retired=4794 sum=6224 — OK**（与 P0c-28 预期逐值一致）。
3. **判官修正一处（探针首跑抓真 bug）**：1351 判官（P0c-27 重写）的"203965 无 QUEST_SELECT
   描述页"断言**漏了 started 态限定**——unaccepted 态的 QUEST_SELECT 接取流是合法族形，
   started 态钉描述页才是 XML 手工形；过宽断言在 lenient 回放首跑即红，已改
   `"started".equals(sourceNode)` 限定（判官与探针同步修）。判例：**否定式路由断言必须带
   source 态限定**；我方探针验证 started 出口时（带过滤）与判官写法（无过滤）不一致 = 写判官
   时丢了过滤器语义。

## lenient 回放绿（`RetailLenientWindowReplayProbeTest.java.txt` 存档，临时源已删）

**5/5 绿**，覆盖 P0c-24~28 全部待窗断言：

- **Daevanion**（80290/80294）：自愈边唯一且逐字段=判官形状（[REWARD,var0=1]→var0:=0 +
  SyncQuestState(LEVEL_AND_VISIBILITY_REFRESH)、priority null、target reward）；armour 投影
  var0=0；完成 owner 831384/831387——**单步修复边通道落地实证，Daevanion 既有债实质清偿**
  （80291/80295 由在册 XML 供给 weapon 侧，正式判官窗口复跑即应全绿）。
- **1141**：接取/报告/完成三段族形 + 无交互门（掉落驱动判据）。
- **30312/30315**：GROUP 缺省掉落 + 39/20002 双路逐件 HasItem/RemoveItem。
- **1351**：族形 started + **203983 交付块首次真端验证**（judge-abort 掩蔽块：入口 SELECT5、
  prio-0 成功路 HasItem/RemoveItem+奖励窗、prio-1 空条件→SELECT6）。
- **1526**：投影 var0=1（alignedSiblings 值）。
- **族门**：`RetailSimpleTalkGateTest` 3/3 绿（新边零扰家族不变量/漂移登记/冻结规模）。

## 未验证（正式窗口余量，大幅收窄）

- 正式判官对生产视图复跑：1141+EarlyElyos+交互门禁全目录 + 30312/30315 + 批量 7 方法 +
  Daevanion 7 方法 + Haramel——断言实质已 lenient 验证，窗口复跑=正式记录。
- T2 ×(1141,30312,30315,1351,1526,80290,80294) + T3 债池快照 diff + Ownership/verify 复跑。

## 判例（勿重推）

1. **否定式路由断言必须带 source 态限定**：unaccepted 的 QUEST_SELECT=接取流（合法族形），
   started 的 QUEST_SELECT=描述页（XML 手工形）——漏限定把合法接取流也禁了。
2. **lenient 回放 = 风暴期的窗口等价物**：全量 fail-closed 门是唯一被阻项时，逐任务 overlay
   回放判官断言能兑现 ~90% 窗口价值（翻转质量即刻暴露、判官 bug 提前抓出），正式窗口收窄为
   复跑+门禁。
3. **翻转→发射边的落地序实证**：flip（数据）与 4 行发射边（编译器）同片落地，overlay 探针
   一次跑覆盖两件——"通道先行"的约束满足且验证零窗口依赖。

## 下一步

- 正式窗口（等 lane 落地）：判官复跑清单 + T2/T3 + verify + 债池 diff。
- CLIENT_ROUTE 11 续裁：80016（事件轴 KEEP）/26930（selectable 轴 KEEP）/事件 9 KEEP——
  全 KEEP 性质，无需窗口。
