# P0c-31：SimpleTalk 保留行"陈旧拒绝码"假设证伪 + 调页注册归属定性（风暴期静态切片）

> 日期：2026-09-26 ｜ 切片：P0c-31 ｜ lane：活跃（DataDriven wave + 调页注册在飞）｜ 前序：P0c-30

## 发现

1. **假设与证伪**：风暴期自查猜想"154 保留行中部分拒绝码已陈列旧（编译器通道已扩展）→ 新采纳机会"。
   用 gate 的 `-Dretail.talk.equivOut` 活体分类直证：**60 行当前可编译**（57
   DIFF:TRANSITION_SET + 3 DIFF:NODE_PROJECTION）——但逐桶证据核对后**全部为已裁定 KEEP**：
   - 28 `SEMANTIC_GAP:RETAIL_CRAFT_AXIS_UNDECLARED`：P0c-10n 裁定 = 老 XML 载 craft 语义
     （can-grant/grant-craft-skill + movie-end 续边）而真端表零 craft 轴来源；**该码是 retention
     侧 KEEP 标签，从未是编译器拒绝**（"编译器虽可合成，retention 闸住退役"）——grep 主源代码
     零命中即证。
   - 17 `CLIENT_REPORT_VARIANT`：逐行带客户端按钮证据（HACTION_SELECT4 ×11 / SELECT4_1 ×2 /
     SELECT_QUEST_REWARD ×2 / NO_SELECT5_PAGE ×2 / CHECK_USER_HAS_QUEST_ITEM / HACTION_SELECT4_1）
     = 真实客户端报告页变体，族形不建模。
   - 11 `CLIENT_ROUTE`：80016 事件轴 / 26930 selectable 轴 / 事件 9 KEEP（真端缺口，已裁定）。
   - 4 `RETAIL_TALK_CHAIN`：p0c10f/10h 决定（CHAIN_SHAPE_UNSUPPORTED / NPC_COMPLETE_VARIANT）。
   **结论：保留行的采纳资格与"能否编译"正交；61 个活体 REJECTED 码与 retention 理由一致。**
2. **调页注册归属**：5 行 DEFERRED（15601/25606 hunt 链 + 10530/15606/20530 collect 链）的
   "talk/collect 信件页梯页注册"缺口——`quest_client_talk_collect_chain_pages.tsv` 由 lane
   在 09:14 重生成（hot 文件），5 行仍未注册（71 行数据）。工作归 lane 在飞域，本 lane 不触碰。

## 判例（勿重推）

1. **"能编译"≠"可采纳"**：采纳=retention 理由 + decisions 文件的逐任务裁定记录；drift/gate 的
   REJECTED 码只测可编译性。用编译状态反推"陈旧理由"会得出假候选（本片 60 行全为已裁定 KEEP）。
2. **KEEP 码可能是 retention 侧标签而非编译器码**：`RETAIL_CRAFT_AXIS_UNDECLARED` 只在
   retention/报告出现——查码的归属层（编译 vs 保留）再谈"码被移除=能力已到"。
3. **原地生成器纪律**：`build_quest_client_*.py` 直写 OUT 路径——运行前先看 target/main 副本
   mtime（双新鲜=hot 文件）；本片误跑一次（确定性生成器 + 输入未变 → md5 与 lane 09:14 版
   逐字节相同，零影响），教训入册。

## 状态

- 风暴复测 missing=664/wrongOwner=0 持稳；正式窗口（判官复跑+T2 ×7+T3 债池 diff）继续待窗。
- 本片零翻转、零主源码改动（仅活体分类 dump 到 /tmp + 本报告 + 台账登记）。
