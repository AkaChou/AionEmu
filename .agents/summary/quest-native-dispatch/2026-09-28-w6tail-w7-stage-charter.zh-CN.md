# 阶段立项：W6 尾清 + W7 终局收口（quest-native-dispatch · 2026-09-28）

> 立项背景：缺口批阶段已收口（SEMANTIC_GAP 595→0、DoD ①—④ 过、红集三口恒等
> `3b92439d…`/`57bb0621…`/`ce4673c7…`、总收口报告已交付）。README「下一面」核对结论：
> **Phase 2 规范形全家族已由并行车道收口**（SimpleHunt / CombineTask / DD 四子面 / ItemPlay /
> UseItem / SerialHunt / CollectItem / SimpleTalk S1–S3，canonical 在码可证），
> D 类加窗已裁定维持现状、W5 g1–g4 与 W6-a 已收口。真正剩余 = **W6 尾三项 + W7 终局文档**。
> 本阶段即对这两块立项，用户侧两事（实机复验 24 行 / push）并行不阻塞。

## 范围与批次

### 批 N1：EarlyElyos 3 条在册红修复 + 红集基线重冻（唯一行为面在测试侧）

- 对象（期望形规格 = `2026-09-27-w5c-w6-recon-pack.zh-CN.md` §4-2）：
  1. `EarlyElyosQuestRegressionTest:67` — 1131 `started→shugo` 交付边；
  2. `:252` — 1561 `CanAct` 自环门；
  3. `:311` — 1691 `spoken-to-diana→returned-to-sneaker`。
- 判据：三测试锁的是**迁移前旧 XML 形**，生产新形已由迁移取代 ⇒ 逐条取证现形后把断言
  迁到新形（若语义已被其他在册测试覆盖则退役该断言并登记理由）；**零生产定义变化**
  （只动测试），故无新增实机复验项。
- 基线重冻（本阶段与缺口批阶段的本质差异：红集**有意漂移**）：
  预期 T1 = 1 条不变；**T2 51→48、T3 97→94**（各移除 3 条 EarlyElyos，逐条登记）。
  移除项必须逐字节可解释（diff 恰为三条），新 sha 入册替换 `T1/T2/T3-*-baseline` 口径。
- 风险与回退：修复若牵出非 EarlyElyos 的新增红 ⇒ 当批回退、逐条归因后重排
  （判据冲突类 → 记录+跳过，维持旧基线不重冻）。

### 批 N2：FailurePage 命名拆分（零形状片）

- 对象（recon §4-3）：4 载体中**唯一名实相反点** `RetailSimpleTalkMigrationReviewContractTest.java:48`
  改名；Java 侧 `hasFailurePage` 一类符号改 SELECT6 专名
  （如 `showsSelect6TurnInFailurePage`），贯穿消歧注释（批 0 R3 已写）。
- 边界：XML `failure-page` 属性 / TSV `failurePage` token 是**数据格式**，不动
  （动表 = 破 `EXPECTED_TSV_COUNT`/指纹纪律，无收益）。
- 判据：零 IR、零 retention、零指纹漂移；聚焦门快筛绿。

### 批 N3：W7 终局收口（文档批，无 Maven）

- README 全片台账终审（13 个已收口小节 + 缺口批 8 批 + 本阶段，交叉引用提交号齐全）。
- 记忆库 QE 覆盖核对（QE-082…QE-098 与各判例一一对应；如有新不变量 → 新 QE 条目 +
  `sync_memory_bank.py` + `verify_memory_bank.py`）。
- 迁移总量终局复算：6224 = 4983 RETAIL_TABLE + 1241 保留（缺口 0、ADJUDICATED=571），
  与 DoD 自检脚本输出互证。
- 生成器停写移交终版清单（scriptdll 车道 owner 消费）：`build_quest_client_report_pages.py` /
  `build_quest_client_dialog_exits.py` 停写 + `build_retention_list.py` ADJUDICATED 语义裁定
  （改脚本仍属兄弟车道，本车道只交单）。

## 门禁与提交纪律（沿用缺口批契约）

- 单条命令 ≤600s，长命令走 `tools/gate_bg.sh`；完成判定只看日志尾部 +
  `pgrep -fl surefirebooter` 为空，禁 sleep 轮询。
- 批 N1 收口跑一轮 T1∥T2∥T3（T2 必跑：EarlyElyos 在 T2 选择器内）；批 N2 快筛聚焦门；
  批 N3 无 Maven。终点一轮全量对拍对**新基线**。
- 每批收口落两条提交：`fix(quest)`（测试/命名）+ `docs(quest)`（台账）；显式路径、禁 `-A`、
  不 push、不启停服务、不创建 worktree、兄弟车道只读；`Co-Authored-By` 结尾。

## 阶段 DoD

1. EarlyElyos 3 红清零且逐条登记；T2/T3 红集 diff **恰为**各自 3 条移除，新基线 sha 入册并
   复跑一轮字节恒等。
2. FailurePage 名实相符，零形状三零（IR/retention/指纹）。
3. W7 终局三件落账（README / QE 覆盖 / 总量复算）+ 移交终版清单。
4. `EXPECTED_TSV_COUNT` = 22 不变；SEMANTIC_GAP = 0 不回退；实机复验清单 24 行维持不变
   （本阶段不新增）。

## 并行与阻塞

- 用户侧：24 行实机复验、push 授权——与本阶段并行，结果不入本阶段 DoD。
- 判据冲突 / 需改兄弟车道 / 3 次无进展 ⇒ 记录+跳过+继续；仅执行环境故障才停。
