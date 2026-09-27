# P0c-16：缓议 7 行细裁——1141 铁 KEEP（validator 合同）+ 6 行裁定就绪待窗执行

> 日期：2026-09-26 ｜ 切片：P0c-16 ｜ lane：SimpleTalk ｜ 前序：P0c-15（门复终判）

## 交付

1. **1141 = KEEP（铁）**：探针直证真端编译缺 `CanAct(700122, ACTION_ITEM_USE)` 对象交互边
   （真端表行 reward_npc=LF1a_Barrel 即 700122 木桶）——drop/交互物任务的 validator 合同
   （2119 实机先例：缺边炸启动）。真端缺口，归对象交互通道；降级维持。
2. **2654 = 真端对、XML 错（NPC 漂移铁证）**：真端表行 `reward_npc_name=AkanNamed_50_Al`
   (=212314，渗透目标怪) 是交付权威；老 XML 挂 204655 = `Lasberg_Normal_Q1647`——
   **别家任务 Q1647 的 NPC**，典型遗留漂移。契约方法 `quest2654ReportsAndCompletesAtEndNpc`
   按 Kaliga 先例改到真端形状（212314 + 双语注释）；因本片门窗未开、翻转回退，该测试编辑
   **同步暂撤**（已恢复与基线字节同一），改回动作记入待窗执行序第②步。
3. **1117/1353/80356/80365/11003 = 裁定就绪**（探针差全部落在已裁定类）：
   - 1117/1353：NONE 10000 入口页 + SET_SUCCEED(10255) + 1008 变体——P0c-12 已裁定类
     （canonical = 39/20002）；work-item 边两态相等（P0c-10o 通道已表达）。
   - 80356/80365/11003：COMPLETE 态页链（1003/1004/1007）/ NONE 态接取链（1002/1003/1004）
     被真端 canonical 丢弃——超出 ItemMirror CHECK 对契约断言范围，judge 方法可判。
   - judge 方法集：EarlyElyos ointment 组（1117）、梯队五契约 + GoldenJourney +
     80312/80318 + Dialog31 + Quest1007Movie + JavaHandlerFamily（1353）、
     itemCollectingMirrors（11003/80356/80365）、quest2654（2654）。
4. **flip-and-test 尝试与协议回退**：捕捉到一次门窗（sum=6224），6 行翻转 + 退役 + target
   清理完成；judge 运行前窗口即闭合（lane 落 18 悬挂新波，56 装载错）——按"不重蹈 P0c-14
   采纳未验"协议**回退 6 行到 XML**（git checkout + catalog 重插 6 条 + 重建），裁定文件
   保持 staged（下窗三步执行序写入文件头：接线 → 改回 2654 测试 → retire+清 target →
   judge，红即回退）。

## 结论（实测）

- SimpleTalk 族：**2223 = 2046 RETAIL_TABLE + 177 XML_RETENTION**（不变——6 行回退；
  CLIENT_ROUTE 维持 34 = 17 回退 + 9 事件 KEEP + 26930 + 1141 + 6 staged）
- 全库：catalog 1472 = 目录 1472；retired 4734；**sum 差 18 = 并行 lane 第 N+1 波在飞悬挂**
  （其 p5 层 03:06 后仍在动）；族门 `RetailSimpleTalkGateTest` 3/3 +
  `RetailSimpleTalkChainGateTest` 2/2 绿
- 本片零新增失败：2654 测试编辑已撤（字节同一）；翻转已全部回退；探针/裁定证据全部落档

## 排障记

- **门窗极短化**：lane 波次间隔缩至 ~10 分钟（02:45 → 03:06 → 03:3x → 03:5x），单窗不足以
  完成"翻转→退役→judge"全程。对策：flip-and-test 改为**预 staged 模式**（裁定文件 + 执行序
  + judge 清单全部落档，窗口一开按序执行，全程 ≤5 分钟）。
- builder 拆层时吃掉元组冒号（SyntaxError）——verify 的 FAIL-fast（retired in catalog）当场
  暴露，1 字符修复；拆层编辑后必须立即重建验证。
- 2654 测试编辑与翻转的**原子性教训**：契约测试改真端形状必须与翻转同进同退，单独回退
  会让测试对 XML 红——已作为执行序第②步固化。

## 下一步

- 下窗执行 6 行 staged flip-and-test（执行序在裁定文件头）
- 缓议清零后：CLIENT_ROUTE 34 → 编译器通道建设按五类分歧优先级（前置派生/事件轴/
  selectable-reward/掉落语义/journal 轴）；TALK_CHAIN 21 通道缺口 + CRAFT 28 续档
- 运行时行为与客户端目检：待启动服务授权
