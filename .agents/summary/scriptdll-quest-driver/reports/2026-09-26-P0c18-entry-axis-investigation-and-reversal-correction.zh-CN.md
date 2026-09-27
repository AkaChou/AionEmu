# P0c-18：入口页轴调查——更正"陈旧资源伪影"误判；第六类缺口实锤=单步 report 流发射缺失

> 日期：2026-09-26 ｜ 切片：P0c-18 ｜ lane：SimpleTalk ｜ 前序：P0c-17（3 采纳 / 3 回退）

## 交付

1. **更正 P0c-17 的"陈旧资源伪影"反转（方法学错误自纠）**：
   - P0c-18 初判曾认为 11003/80356/80365 的红是 target 客户端 TSV 陈旧所致，并以 lenient
     探针"全绿"翻案——**该翻案错误**：探针用的 `definitionInOverlay` 在清单未翻转
     （XML_RETENTION）时**原样返回 XML 编译**，"绿"验证的是 XML 自身，不是真端编译。
   - 翻转态直证（XML 已删、清单 RETAIL_TABLE）：三行真端编译**只有接取流**
     （unaccepted 链 1011/1007/1002→1003→1004/20000/20001/1008 + started 出口 1008），
     **报告流整体缺失**——QUEST_SELECT→2375、SELECT_QUEST_REWARD 1009 对、CHECK 39/20002
     对全无。P0c-17 的红是真实的；三行维持 KEEP。
   - **方法学判例**：lenient 视图探针验证"真端编译"的前提 = 该行已翻转（retail-owned）；
     未翻转行探针绿 = XML 绿，毫无证明力。
2. **第六类缺口定性收紧**：缺的不是"入口页轴"而是**单步 report 流发射**——对照组
   2654/1117/1353（talk-chain 注册表行）经 `reportFlowChain` 有完整报告流（31→SELECT5 +
   1009/39 对），11003/80356/80365（单步 item_check 行）的 build 路径未进入任何 report
   流分支。客户端模板索引（`legacy-quest-dialog-template-index.csv`）对三行明确声明
   `31,QUEST_SELECT,2375,SELECT5,START,39,CHECK_COLLECTED_ITEMS` 契约——通道建设方向 =
   单步 item_check 行的 report 流发射（对齐模板索引声明），而非新增页数据。
3. **自伤事故与恢复（如实记录）**：拆层操作把 p0c16 文件里的 3 个已采纳行
   （2654/1117/1353）一并退回降级态，因其 XML 已退役而短暂悬挂（verify 6203）+
   族门两红（漂移 实际=null + 家族规模 2220）——**根因是 target 陈旧清单**（接线重建后
   未回拷 manifest，门读中间态）。回接层 + 回拷三副本后族门 5/5 复绿、verify 6224 OK。
   判例：b1 层文件是"裁定登记"而非"待办暂存"——文件在层中即生效，拆层会撤销已采纳行。
4. **窗口时刻取证**：lane 落地 18 行悬挂后（verify 6224 OK 瞬间）完成 family 门 5/5 与
   全树一致确认；正式 judge 复跑被下一波窗口闭合打断（55 装载错，非新增失败——秒前族门
   同树全绿），以 P0c-17 窗口内实跑绿为 trio 权威判决。

## 结论（实测）

- SimpleTalk 族：**2223 = 2049 RETAIL_TABLE + 174 XML_RETENTION**（P0c-17 终态维持：
  3 采纳 2654/1117/1353 + 3 KEEP 11003/80356/80365）
- 全库：catalog 1469 = 目录 1469；retired 4737；**verify 6224 OK**（lane 落地其 18 行后
  精确闭合时刻）；族门 `RetailSimpleTalkGateTest` 3/3 + `RetailSimpleTalkChainGateTest`
  2/2 绿（回拷清单后）
- 本片净变更 = 0 行翻转（调查/更正/恢复切片）；prod 代码零改动；探针源存
  `RetailTalkEntryAxisProbeTest.java.txt`

## 排障记

- **lenient 探针的证明力边界**（本轮核心教训）：`definitionInOverlay` 对 XML_RETENTION
  行返回 XML——翻转前后探针结果语义完全不同；比较"翻转态编译"必须在翻转态跑。
- b1 层拆装的粒度陷阱：裁定文件一旦有已采纳行，就**永远不能整体出层**；作废个别行应
  改文件内容而非拆层。
- target 陈旧清单判例再次变形：不止 quests/ XML——**manifest 与 catalog 的回拷同样
  必须紧跟每次重建**，否则门禁读到中间态产出幽灵失败（漂移 null + 规模缩水）。
- 运行时行为与客户端目检：需启动服务授权，未执行。

## 下一步

- 第六类通道建设：单步 item_check 行 report 流发射（模板索引契约对齐）→ 解锁
  11003/80356/80365 复験
- 五类既有分歧通道（前置派生/事件轴/selectable-reward/掉落语义/journal 轴）
- TALK_CHAIN 21 通道缺口 + CRAFT 28 续档
