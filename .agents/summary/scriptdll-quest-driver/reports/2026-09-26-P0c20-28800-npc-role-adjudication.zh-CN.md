# P0c-20：28800 NPC 分工裁定——双生子先例 + 客户端登记无背书，ADOPT_RETAIL

> 日期：2026-09-26 ｜ 切片：P0c-20 ｜ lane：SimpleTalk ｜ 前序：P0c-19（第六类证伪，镜像裁定词汇）

## 交付

1. **28800（페르논에 놀러 오세요(마)，魔族版）ADOPT_RETAIL**：
   - 真端表 `acquired_npc_name=Randiten(798459)` / `reward_npc_name=Harretion(830532)` =
     形状权威；模板索引 start/end 列同对（798459/830532）。
   - 遗留 XML 额外把 started 进度对话（QUEST_SELECT→SELECT5）与 1009 上交挂在 798459 与
     第三 NPC **204191(Doman)** 上——**客户端登记无背书**：dic 链交付 NPC 集登记表无 28800 行
     （Harretion 唯一解析、无复合引用）、交付型对话页登记与信件型登记均无此任务
     （仅 summary_rows=1 行 → reward var0=0 与编译一致）。
   - **双生子先例**：18800（天族版）/18801/28801 已按同形（单接取/单交付）采纳、XML 退役、
     无运行时异常登记。
2. **P0c-15 红的定性（镜像词汇第二应用）**："接取路由 798459 QUEST_SELECT 缺失" = 判官断言
   遗留 XML 的 started 进度对话形状（select5 双 NPC 出现的页面级证据被推到路由级），
   非真端编译缺口——SimpleTalk 单步族形接取 NPC 的 started 态只有 FINISH_DIALOG 出口，
   2052 行已采纳族同形。
3. **翻转态探针直证全量形状（30 路由）**：接取 NPC started 态唯一路由 = FINISH_DIALOG →
   ShowQuestSelectionDialog(SELECT_QUEST)；进度对话与 1009 上交只在报告 NPC；16 条完成路由
   （dialogId 8..23）契约与判官现有断言**逐字一致**（GrantReward EXP 12951 QUEST_BASE +
   CompleteQuest(0)）；判官重写只动 started 块 + 上交独占方向反转（原注释掉的
   assertNoRewardPathForNpc 以真端方向正面锁死：started 1009 只在报告 NPC、204191 零路由）。
4. 落地：裁定表 `p0c20-28800-adjudication.tsv` + b1 元组接线 + 外科手术清单补丁（三副本
   md5 归一 2eafd792bf686852784a5dce75964132）+ XML 退役 + catalog 1459→1456 + target 四件同步。

## 验证（实测）

- **判官绿**：`Quest28800ClientDialogAlignmentTest` 重写后对生产视图实跑通过。
- **verify_retirement：catalog=1456 directory=1456 retired=4768 sum=6224 OK**（无悬挂）。
- **T2（gates/T2-045439.log）**：63 tests，1 失败 = 既有 client-contract count=50 集，
  **失败集与 T3-035139 基线逐条 diff 相同**，28800 不在清单。
- **族门**：SimpleTalkGate 3/3 + ChainGate 2/2 + Ownership 4/4 + CatalogManifest 10/10 全绿。
- 探针已归档 `Retail28800ShapeProbeTest.java.txt`（临时 JUnit 源已删）。

## 结论（实测）

- SimpleTalk 族：**2223 = 2053 RETAIL_TABLE + 170 XML_RETENTION**（本片 1 行翻转）；
  CLIENT_ROUTE 28 → **27**；本车道采纳累计 45 → **46**
- 全库：catalog 1456 = 目录 1456；retired 4768（含 lane 窗口）；verify 6224 OK
- 本片净变更 = +1 行采纳；判官重写 1 文件；裁定表 + 探针存档 + 本报告；prod 代码零改动

## 判例（勿重推）

- **页面级证据 ≠ 路由级契约**：客户端 HTML 某页在多 NPC 对话链出现，不等于服务端路由表
  应在多 NPC 挂该路由——NPC 分工以真端表 acquired/reward + 模板索引 start/end 为权威；
  页面证据要升级成路由需过客户端登记表（dic 链交付集等）背书，否则按遗留手工形裁。
- **双生子先例是最便宜的裁定证据**：同设计系列（18800/28800）已按某形运行且无异常登记时，
  直接引用为家族先例，不重推形状。
- P0c-19 镜像词汇的第二应用：红签名"某 NPC 的 X 路由缺失"先核该 NPC 角色与族形——
  接取 NPC 的 started 态本来就只有 FINISH_DIALOG 出口。

## 下一步

- CLIENT_ROUTE 27 行续裁（前置派生 3103/4913/18802/23902/28802 五行是下一个同族批量面；
  Haramel 三行 18505/18509/28509 空报告应答轴；2150 完成路由聚合；30312 掉落语义；
  30315 认证交付计数；80290 stale-row heal；80016 事件轴）
- 五类既有分歧通道；TALK_CHAIN 21 + CRAFT 28 续档
- 运行时行为与客户端目检：需启动服务授权，未执行
