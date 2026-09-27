# P0c-15：门复终判——P0c-14 批 3 行确认采纳 / 17 行回退 KEEP；26930 选择性奖励轴真端缺口定性

> 日期：2026-09-26 ｜ 切片：P0c-15 ｜ lane：SimpleTalk ｜ 前序：P0c-14（36 行采纳待验）

## 交付

1. **门复窗口捕捉**：并行 DataDriven lane 于 02:30-02:45 间完成 SimpleHuntPlan 重构落地 +
   32 行指纹冻结（frozen=1036 = retired=1036，轻量轮询信号：清单 DataDriven RETAIL 行数
   vs `retail-data-driven-ir-fingerprints.tsv` 行数），生产门短暂开启——立即执行全部门复。
2. **P0c-14 批终判（flip-and-test 闭环）**：
   - **确认采纳 3**：30314（专属对齐测试绿）、80018（MovieRandomDispatch + DialogOrder 17/17
     + CoreCapability 绿）、80294（Daevanion 仅 80290 红）。
   - **回退 KEEP 17**（协议"翻红即回退"，全部断言级红/错，非装载错）：
     2150（完成路由 16 vs 独占 2）、3103（前置反向：retail 多出 [3102]）、4913/18802/23902/
     28802（前置元数据差：契约期望 [4912]/[18801]/[23900]/[28801]，retail 未派生或不同）、
     28800（接取路由 798459 QUEST_SELECT 缺失）、2110（接取发物 182203110 差）、30312
     （掉落语义 GROUP/scope vs NONE）、30315（认证交付 2 vs 3）、1526（journal 末行轴
     reward var0=0 vs 1——P0c-14 静态疑点证实）、80290（stale-row heal 路由缺）、1351
     （阶段转移缺，transition orElseThrow）、18505/18509/28509（Haramel 空报告应答
     CloseDialog vs ShowQuestSelectionDialog + 对象门批方法错）、80016（事件轴：levelUp
     激活变体 + MOVIE 奖励轴丢 + 元数据名 [Event] Sock Hop vs Q80016）。
   - **ReportToMany 16 增量干净**：回归测试仅剩在先 3914 一处（本批 16 路由全过）；
   - **CharmedEvent 4/4 原状绿**（9 事件行未翻，语义保持）。
3. **回退落地**：`p0c14-clean-decisions.tsv` 17 行移除（头部记终判）+ 重建清单 + 17 XML
   `git checkout` 恢复 + catalog 17 条目自 HEAD 重插（排序保持，1490 = 文件数）——与
   01:09 T2 基线（16 契约类全绿，XML 态）字节级同一，绿由状态同一性成立。
4. **26930 细裁定性（缓议 8 行首行）**：**KEEP——真端缺口**。真端 `quest.xml` 自身声明
   `<use_class_reward>1` + fighter/knight/ranger/assassin/wizard/elementalist/priest…
   每职业 `selectable_reward`（wrap_q_idruneweapon_* 系列）——选择性职业奖励是**零售轴**，
   老 XML 的 `AdvancedClassIs` 门路线是它的忠实实现，而当前 SimpleTalk 编译无通道表达
   （探针 missing 全部类别门奖励路由）。未来通道 = 编译器 selectable-reward 轴
   （ManifestTest 的 selectable-reward 修复族已有先例系统）。M3-d 降级维持。
   契约方法断言的 SIMPLE CHECK 路由（HasItem 186000257×10 + 精确扣 10）两态相等不在分歧内。

## 结论（实测）

- SimpleTalk 族：**2223 = 2046 RETAIL_TABLE + 177 XML_RETENTION**（CLIENT_ROUTE 34 =
  17 回退 + 9 事件轴 KEEP + 8 缓议）
- 全库：retired 4726 + catalog 1490 = 6216（**差 8 = 并行 lane 第 N 波在飞悬挂**——其
  02:45 冻结后又一波删除未落裁定；非本片）
- 门禁：`RetailSimpleTalkGateTest` 3/3 + `RetailSimpleTalkChainGateTest` 2/2 终态绿；
  门复窗口内取证：ReportToMany 增量干净 + CharmedEvent 4/4 + DialogOrder 17/17 +
  CoreCapability/MovieRandomDispatch 绿
- 本片零新增失败：3 行采纳有窗口内绿证据；17 行回退有 01:09 基线 + 状态同一性

## 排障记

- lane 波次节奏：01:00（26 行翻转）→ 01:21（trio 同步）→ 01:21-01:45（删 40 未落）→
  02:30-02:45（SimpleHuntPlan 重构 + 32 指纹冻结落定）→ 02:45 后（又一波 8 悬挂）。
  **协作结论**：其门禁（frozen vs retired 计数）是可靠的收口信号；本片用轻量轮询
  （不经 maven）捕捉窗口，窗口内完成全部取证。
- `RetailSimpleHuntPlan.java` 02:30-02:33 两次保存均带"递归构造器调用"编译错——写入
  中间态，等待自愈（02:36 编译恢复），未触碰。
- 26930 的 ManifestTest 注释（"已由真端 SimpleTalk 驱动（XML 退役）"）与清单现状矛盾
  ——历史翻转残留注释，M3-d 降级后未回改；以清单为准（XML_RETENTION）。

## 下一步

- 缓议余 7 行同法细裁（80356/80365/2654/11003/1141/1117/1353：先查真端数据是否声明
  对应轴——26930 判例：真端 quest.xml 声明即真端缺口 KEEP，无声明且契约属客户端派生
  则评估编译器通道）
- CLIENT_ROUTE 余 34 行的编译器通道建设方向：①selectable-reward 轴（26930 系）
  ②事件轴（EventQuest 家族，17 行）③前置元数据派生（18802 系）④掉落语义（30312 系）
  ⑤journal/stale-row 轴（1526/80290 系）
- TALK_CHAIN 21 通道缺口 + CRAFT 28 续档；SimpleHunt gap 47
