# P0c-17：staged flip-and-test 执行——3 行确认采纳 / 3 行入口页路由轴红回退

> 日期：2026-09-26 ｜ 切片：P0c-17 ｜ lane：SimpleTalk ｜ 前序：P0c-16（6 行 staged）

## 交付

1. **跨多窗口完成 staged 执行序**（预同步 target + `surefire:test` 直跑把窗口需求压到
   ~90 秒，与 lane 波次节奏共存）：
   - **确认采纳 3**：2654（契约方法真端形状 212314 下绿——仅剩在先 11139）、1117
     （ointment 方法组绿，EarlyElyos 3 错全为既有三元组 flowerDelivery/undeliveredArmour/
     bollvig）、1353（八类契约全绿：Mosbears/Aturam/Collapsed 梯队 + GoldenJourney +
     80312/80318 + Dialog31 + Quest1007Movie + JavaHandlerFamily）。
   - **红回退 3**：11003、80356、80365——同一新增分歧类：**assertPage("started", npc, 2375)
     页路由缺失**（11003@798933、80356@831815、80365@831827 各缺 QUEST_SELECT→SELECT5
     入口页路由）。逐步回退（每回退一行重跑 judge 以解方法中止），第三轮对称确认。
2. **新分歧类登记（第六类）**：入口页路由轴（entry-page route axis）——真端 SimpleTalk
   编译对这三行不合成 QUEST_SELECT→2375 接取入口路由（quest_client_entry_pages 通道
   未覆盖 legacy-template 形态）；已裁定的 2654/1117/1353 同形态却有路由——说明是
   per-row 数据形态差异而非全族缺口，通道建设时按 entry_pages TSV 行核对。
3. **排障新判例**：回退周期内恢复的 XML 必须**回拷 target/classes**——`surefire:test`
   跳过资源阶段，恢复件不在 target 会令 `QuestXmlFixtures` NPE（"no longer has a
   production XML"）；P0c-13 陈旧副本判例的反向补充（删要删、还要会还）。

## 结论（实测）

- SimpleTalk 族：**2223 = 2049 RETAIL_TABLE + 174 XML_RETENTION**
  （CLIENT_ROUTE 31 = 3 采纳 + 9 事件 KEEP + 26930 + 1141 + 11003/80356/80365 回退 + 4 缓议已清 →
  复験战役累计：73 → 31，采纳 42）
- 全库：catalog 1469 = 目录 1469；retired 4737；**sum 差 18 = 并行 lane 在飞悬挂**（不变，
  其所有物）；族门 `RetailSimpleTalkGateTest` 3/3 + `RetailSimpleTalkChainGateTest` 2/2
  终态绿；`QuestBatchReportNpcAlignmentTest` 2654 方法绿（窗口内取证）
- 本 lane 零新增失败；所有翻转行均有窗口内 judge 绿证据，所有红行均已回退且基线同一

## 排障记

- 窗口节奏：lane 波次 ~10-20 分钟、窗内时长 ~2-5 分钟——`mvn test` 全周期（~3 分钟）经常
  跨窗；`surefire:test` + 预同步 target（catalog/manifest/quests 差集 + 恢复件回拷）把
  窗口需求压到 ~90 秒后三连窗全部命中。
- judge 方法中止效应：ItemMirror 循环在首红行抛出即中止，后续行未判——对策 = 逐行回退 +
  重跑（11003 → 80356 → 80365 三轮对称判决），不可凭同形推断跳过实测。
- verify FAIL-fast 再次立功：拆层语法错、retired-in-catalog、target 恢复件缺失均为其首报。

## 下一步

- 入口页路由轴（第六类）通道建设：三行 KEEP 待通道；与五类分歧（前置派生/事件轴/
  selectable-reward/掉落语义/journal 轴）并列为 CLIENT_ROUTE 余量的编译器方向
- CLIENT_ROUTE 31 + TALK_CHAIN 21 通道缺口 + CRAFT 28 续档
- 运行时行为与客户端目检：待启动服务授权
