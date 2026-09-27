# P0c-21：前置派生五行归属裁定——真端不表达桶归属，3103/4913/18802/23902/28802 ADOPT_RETAIL

> 日期：2026-09-26 ｜ 切片：P0c-21 ｜ lane：SimpleTalk（另在飞 DataDriven）｜ 前序：P0c-20（NPC 分工词汇）

## 交付

1. **五行 ADOPT_RETAIL（前置派生批量面清零）**：真端 quest.xml 只声明条件族
   （finished_quest_cond1 / unfinished_quest_cond / noacquired_quest_cond），**不表达桶归属**
   （prerequisites vs start-conditions）——mapper 固定规则（无后缀且无非 finished 族共存 →
   prerequisites；带 reward 后缀或共存 → startConditions）是已采纳通道（912:600 两种表达
   先例），分歧 RETAIL_COND_PLACEMENT 早已登记。遗留 XML 的桶选择是两种表达之一，随 XML 退役。
2. **逐行归属（判官 metadata 断言改真端归属）**：
   - 3103：finished Q3102 → prerequisites={3102}（原判官钉 start-conditions——判官竟是
     XML 归属形，翻转后 mapper 归 prerequisites 桶即红）
   - 18802/23902/28802：finished Q18801/Q23900/Q28801 → prerequisites={同 id}（原判官
     `startConditions().stream()...questId()` 钉 XML 归属；路由断言已是族形）
   - 4913：unfinished/noacquired 族 Q24260/24261（**双重否定 = 需两者已完成**）→
     startConditions 四项（unfinished×2 + noacquired×2）；遗留 XML 零条件 = 手工缺漏。
     P0c-15 报告的"契约期望 [4912]"经查**无真端/客户端条件关联**（4912 仅在
     QuestA03ShardRetailAlignmentTest 作数字巧合出现），属报告笔误级线索，未进裁定。
3. **完成路由奖励契约实跑核对全过**：五行真端奖励（EXP 1347585/7667186/12951×2/1723277、
   GOLD 37665、TITLE light87/dark87、技能书+手册+卷轴+硬币物品）与判官 GrantReward 契约
   逐字一致——静态符号解析疑虑被实跑排除。
4. 落地：裁定表 `p0c21-prereq-bucket-decisions.tsv` + b1 元组接线 + 外科手术清单补丁
   （五行）+ XML 退役×5 + catalog −5 + target 四件同步。

## 验证（实测）

- **五判官全绿**（一次 `mvn -Dtest=五类` 实跑；首个类 8.5s 装载生产视图，余 0.002s 复用缓存）。
- **verify_retirement：catalog=1439 directory=1439 retired=4785 sum=6224 OK**（含 lane
  同时窗内退役的 12 行——catalog 基线取 lane 最新态，外科补丁只叠我方 5 行）。
- **T2（gates/T2-050812.log）**：64 tests，两红定性：① client-contract count=50 集与基线
  **逐条 diff 相同**（既有）；② `RetailOwnershipGateTest` "catalog must not keep retired
  quests: [29900, 19900]" = **lane 中间态**（lane 删 XML 未及移 catalog 行，其"删 XML→翻
  清单→重 dump"流程序进行中）——**等待 90s 自愈后复跑 4/4 绿**，不触碰。
- 我方五行清单态翻转后稳定（四副本 md5 归一 77c73d9e...）。

## 结论（实测）

- SimpleTalk 族：**2223 = 2058 RETAIL_TABLE + 165 XML_RETENTION**（本片 5 行翻转）；
  CLIENT_ROUTE 27 → **22**；本车道采纳累计 46 → **51**
- 全库：catalog 1439 = 目录 1439；retired 4785（含 lane 窗口 12 行）；verify 6224 OK
- 前置派生分歧面（五类分歧之一）**余量清零**：CLIENT_ROUTE 22 行不再含前置派生行
  （余 = Haramel 空报告应答 3 + 2150 完成路由聚合 + 30312 掉落语义 + 30315 认证计数 +
  80290/1351/1526 journal-stale-row 轴 + 事件轴 80016 + 1141/26930 固守）
- 本片净变更 = +5 行采纳；判官 metadata 断言改 5 文件；裁定表 + 本报告；prod 代码零改动

## 判例（勿重推）

- **真端不表达桶归属**：finished_quest_cond 的 prerequisites/start-conditions 归属是生产
  内部建模选择（912:600 两种表达），mapper 固定规则为准；判官/契约钉"桶"时先对照登记表
  RETAIL_COND_PLACEMENT，不构成翻转阻塞。
- **双重否定条件族**：unfinished+noacquired 同族列出 = 零售习惯法"需已完成"（4913 形），
  mapper 的 hasNonFinishedFamilies 分支正是为此共存设计。
- **P0c-15 报告的压缩归因可含笔误级线索**（"契约期望 [4912]"无出处）——翻转前对报告
  红签名逐条溯源到具体断言行，不直接采信压缩表述。
- **Ownership 门红先查 lane 中间态**（"catalog must not keep retired quests"指向的 id
  不在我方裁定集 = lane 删 XML 未及移 catalog 行）：等待自愈复跑，不触碰不回滚。
- T2 聚合 "Failures: 1, Errors: 1" 可为 surefire 聚合伪影，逐类行才是证据。

## 下一步

- CLIENT_ROUTE 22 续裁（Haramel 三行空报告应答 / 2150 聚合 / 30312 掉落 / 30315 认证 /
  journal-stale-row 80290 + 1351/1526 / 事件轴 80016 / 固守 1141+26930）
- 五类既有分歧通道余四面（事件轴/selectable-reward/掉落语义/journal 轴）；TALK_CHAIN 21 + CRAFT 28
- 运行时行为与客户端目检：需启动服务授权，未执行
