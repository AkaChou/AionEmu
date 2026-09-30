# 批 N1 收口台账：EarlyElyos 3 在册红修复 + T2/T3 基线重冻（2026-09-28）


> **生成物清理提示（2026-09-30）**：本文件正文提到的 `*.tsv` / `*.log` / `*.txt` / `*.xml` 等中间转储已随 `chore(agents)` 清理删除；原文与留痕仍可从 git 历史取回，被删清单与再生成入口见 `.agents/summary/CLEANUP-LEDGER.zh-CN.md`。

> 阶段宪章：`2026-09-28-w6tail-w7-stage-charter.zh-CN.md`。三红 = 锁迁移前旧 XML 形的
> `EarlyElyosQuestRegressionTest` 断言（任务均已 RETAIL_TABLE/OK 真端驱动），缺口批阶段因
> DoD④ 字节恒等延期，本批按宪章重锚。

## 取证（探针 dump）

临时探针 `EarlyElyosReanchorProbeTest`（跑后即删）dump 生产视图全转换，留档
`w6tail-earlyelyos/probe-dump-1131-1561-1691.txt`；三红全是 `route(...)` NoSuchElementException：

| 任务 | 旧断言 | 现形根因 |
|---|---|---|
| 1131 | `started→shugo`（10000 交接） | 节点名改 `s1`（SimpleTalk 阶段腿重建，QE-080）；`started→started` 31→1352 页与 Give/Remove 动作不变 |
| 1561 | `assertObjectGate`（CanAct 自环）+ `-1→2375` + `8→complete CloseDialog` | SimpleUseItem 规范形：CanAct 门与 2375 页退场；宝箱 700188 `QUEST_SELECT(31)` 从 started 直翻 REWARD+窗 5；`-1/1009→窗 5` 重开自环保留；CloseDialog 随 `108` 完成边 |
| 1691 | `spoken-to-diana` / `returned-to-sneaker` 长名 + `-1→2034` + `10002→reward` | 阶梯 `s1/s2/s3`；2034 页挂 `700563` 的 `QUEST_SELECT(31)` 自环；`10002` 交接边（s2→s3）新增 `GiveItem(182201826)`；领奖 = `798386` 的 `31` 直达窗 5 |

## 修复（只动测试，零生产变化）

三处重锚 + 逐处双语注释（引用 QE-080 / 规范形）；语义意图逐条保留（1131 交付前对话页、
1561 领奖态可重开、1691 鞋匠对话页与交接推进）。聚焦复跑 **17/17 绿**。

## 门禁与基线重冻

| 门 | 结果 | 红集 | sha256（前 16） |
|---|---|---|---|
| T1 | 88 例 1 红 | 1 条不变 | `3b92439da8052988` |
| T2 | 48 条（51→48） | diff 恰 3 条 EarlyElyos 移除 | `fb70bc91281c124c` |
| T3 | 94 条（97→94） | diff 恰 3 条 EarlyElyos 移除 | `5e3acdb9dcaf255c` |

- 新基线入册：`gates/T2-n1-baseline-reds.txt` / `gates/T3-n1-baseline-reds.txt`；
  T1 基线沿用 `T1-baseline-reds.txt`（未变）。
- 纪律：`EXPECTED_TSV_COUNT`=22 不变、SEMANTIC_GAP=0 不回退、零 retention 变化、
  零新增实机复验项（纯测试重锚）；探针已删、暖副本已清。
- 证物：`gates/n1-earlyelyos-focus.log`（修复前 3 Errors）、`gates/n1-earlyelyos-fix.log`
  （17/17）、`gates/T1-073737.log` / `T2-072910.log` / `n1-t3-1/2/3.log`。
