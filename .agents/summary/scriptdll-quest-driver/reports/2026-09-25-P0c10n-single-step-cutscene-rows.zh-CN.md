# P0c-10n：SimpleTalk 单步过场 13 行采纳退役 + 28 craft 行缺口归档（TALK_CUTSCENE 桶收口）

> 日期：2026-09-25 ｜ 切片：P0c-10n ｜ lane：SimpleTalk ｜ 前序：P0c-10m（4573/5554）

## 交付

1. **生产代码（真端 cutscene 轴落地，10k 判例扩展到单步行）**：
   - `RetailSimpleTalkTable.Entry`：`boolean cutscene` 升维为 `cutsceneMovieId` + `cutsceneTrigger`
     （`cutsceneid1` / `cs1_haction` 严格整数轴，畸形值炸出；`cutscene()` 派生 = movieId ≥ 0）；
   - `RetailSimpleTalkDefinitionCompiler`：precheck 放行 `singleStep && !removes && give && trigger ∈
     {1009=SELECT_QUEST_REWARD, 1007=ASK_QUEST_ACCEPT}`（1009×item_check 组合未见真端行集，仍拒）；
     `build()` 新 `attachMovie` 助手——把 `PlayMovie(movieId, CUTSCENE)` 插到触发路由开/关窗动作前
     （after 序 = 老 craft 行编码：Sync → PlayMovie → 开窗；1007 挂接取流 ASK 路由（判例 3020 同轴），
     1009 挂报告流 SELECT_QUEST_REWARD 路由）。
2. **裁定与退役**：13 行 ADOPT 退役（12 纯过场 trigger=1009 + 4056 trigger=1007/item_check/同 NPC）；
   **28 行 craft 母系列 KEEP 归档**（1941/2931/19009/29009 四系）。
3. **登记翻转**：drift 41 行两档口径——13 采纳行冻结 `DIFF:NODE_PROJECTION`（退役时点证据）+
   28 craft 行活体 `DIFF:TRANSITION_SET`；retention 新增 `KEEP_XML:<码>` 精确缺口码通道
   （build_retention_list.py），28 行标 `SEMANTIC_GAP:RETAIL_CRAFT_AXIS_UNDECLARED`。

## 证据链（10k 方法论）

- **movie id** = 真端表 `cutsceneid1`（3943→93 … 4962→130、4056→403），无外部登记；
- **触发** = `cs1_haction` ∈ QuestDialogAction canonical 路由（40×1009 SELECT_QUEST_REWARD、
  1×1007 ASK_QUEST_ACCEPT；1941 行 `cs1_haction=1009` 与报告页按钮对位，判例 3020 同轴）；
- **探针 `RetailTalkCutsceneProbeTest`（已删，源存 .java.txt）**：53 行 TALK_CUTSCENE 全量编译 +
  老 XML 对拍——13 采纳行 `movie=1/xml=0`（真端有、老 XML 缺：老 XML **零** play-movie 元素，合成是
  补真端语义，非等价路线）；28 craft 行 `movie=1/xml=1`（老 craft 行自带 play-movie + movie-end 续边）；
  12 无触发行（10k 传送门口径 + M3-b 残留）精确拒绝 `movie=N trigger=-1`；
- **4056 组成分解**（唯一复合行）：投影差 QE-051 + 过场边（新）+ 老 XML 文档门冗余
  （HasItem quest_4056b = work item，规划器完成清理覆盖——10m B 类判例）+ collect_item1=quest_4056a
  解析通过（itemCheckReportFlow 通道）。

## 28 行 craft KEEP 判据（真端缺口 → 保留 XML）

老 XML 载真实语义：`can-grant-craft-skill` 条件门 + `grant-craft-skill` 动作 + `movie-end movie-id`
续边（工艺大师授予链）；而真端表（SimpleTalk）与真端 quest.xml **均无 craft 轴声明**（全文件 0 工艺
字段）——真端文件无法表达，按信条 ③ 保留 XML。编译器虽已能合成其 canonical 形状（precheck 放行），
retention 层 KEEP_XML 闸住退役：生产仍由 XML 驱动，零回归。

## 结论（实测）

- SimpleTalk 族：**2223 = 1932 RETAIL_TABLE + 291 XML_RETENTION**（TALK_CUTSCENE 桶收口：
  53 = 13 采纳 + 28 craft 归档 + 12 无触发留档）
- 全库 retention：**6224 = 4586 RETAIL_TABLE + 1638 XML_RETENTION**
- 门禁：`RetailSimpleTalkGateTest` 3/3 + `RetailSimpleTalkChainGateTest` 2/2 绿（退役前后各一轮）
- catalog：1630 条，13 退役 id 零残留、28 KEEP id 零误删、craft XML 28/28 在盘

## 归账 / 排障记

- **verify_retirement = 1630/1630/4586，sum 差 8**：与 10m 同源——并行 DataDriven lane 8 行
  （13945/15306/15316/18994/23945/25306/25316/28994）已删 XML+catalog 但 p5 裁定未翻转；本 lane
  delta 恒等（6224−8=6216 精确闭合）。`RetailOwnershipGateTest` 同一 8 差归并行 lane。
- **排障判例（重要）**：retention 先于 drift 翻转落盘时，`RetailSimpleTalkGateTest.classify()` 对
  已标 RETAIL_TABLE 的行走「冻结证据」路径（`RetiredQuestIds` 读 retention 主资源副本），活体导出
  对这批行显示**旧冻结值而非现算值**——本片 13 行曾因此疑似「gate 拒绝 / 探针接受」矛盾；判据：
  同 JVM 双通道对拍（探针内以 gate 口径重载资源再编一次）零分歧 + 失败信息仅 drift 同步类 →
  定位为冻结证据路径，裁定表驱动翻转脚本按两档口径分别断言落盘。
- 家务：P0c-10f wave A 遗留探针 `RetailTalkChainProbeTest` 补归档删除（源存 .java.txt）。
- 运行时行为（过场播放/电影结束续边在运行时的表现）与客户端目检：需启动服务授权，未执行。

## 下一步

- 并行 lane 收口后复跑 verify_retirement + ownership + manifest 门禁（8 差归零）
- SimpleTalk 余量：TALK_CHAIN 52 + 复合残组、ITEM_CHECK_UNRESOLVED 5、craft 28（等 craft 轴
  真端来源出现再裁）、12 无触发行（触发机制未定）
- 其他族：SimpleHunt gap 47 / reject 108 / spawn 2；DataDriven 随并行 lane
