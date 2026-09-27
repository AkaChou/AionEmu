# P5-3 DataDriven 纯对话落驱动（talk 简单 70 + talk 链 13；DataDriven 1508 = 719 已驱动）

日期：2026-09-25　|　切片：P5-3 wave A/B（goal 台账见 `GOAL-retail-driver-progress.zh-CN.md`）

## 交付

1. **wave A 简单报告流（70 行，`RetailDataDrivenTalkCompiler.build`）**：语义按 1919 合同锁定——
   QUEST_SELECT 在 started 态显示完成页但**停留 started**（不推进），`SELECT_QUEST_REWARD`(1009)
   才进领奖（`LEVEL_AND_VISIBILITY_REFRESH` + 领奖窗 5）；完成流 + 任务簿修复边同构。
2. **wave B 链式信件（13 行，`RetailDataDrivenTalkCompiler.buildChain`）**：stage i 配对客户端
   select{i} 页梯（`RetailClientTalkChainPages` 登记表，按钮图推导）；导航按钮沿梯下行（动作 id =
   下一页 id，`SELECT2_1` 系协议动作）；中间步 SETPRO{i+1} → `SetVariable(var0,i+1)` +
   `PACKET_ONLY` + 任务簿页；末步 SET_SUCCEED 进领奖；`var0` = 阶梯值；**单条治愈边**
   （stale = lastRow−1 → lastRow，11323 合同锁 1 条——首版 5 条被合同打回）。
3. **链注册表生成器**（`build_quest_client_talk_chain_pages.py`，124 行）：按钮图从
   `quest-dialog-action-details.csv` 推导；**推进动作优先于导航归类**——SETPRO3=10002 与模板页
   DEFAULT_SUCCESS 同 id 碰撞（11323 第三段 `1693>10002:1009` 被误判为导航，真端 XML
   `SETPRO3→s3` 为证），先查 ADVANCE_IDS 再查页 id。
4. **家族续接信件偏移收口（诚实暂缓）**：15402/25402/25000 的 QUEST_SELECT 落在 select2 起
   （简报链行 `1352 …:10001` 与真端 XML `QUEST_SELECT→SELECT2` 一致可证——信件页本身与
   select1 链并存、入口页按钮无区分信号），**阶段边界不可从客户端信件推导**，按
   `CHAIN_CONTRACT_LOCK` 退回 XML（`p52b_defer_handin_quests.py` 幂等执行：恢复 XML + 目录
   按 id 序回插 + 台账累计 61 行）。连同此前 1876/2876/15550/25550，链形状暂缓共 7 行。
5. **步骤表示统一（用户指令）**：`RetailDataDrivenTable.Entry` 以 `stepCategories` + 派生
   `noProgress()/allHunt()/allCollect()/allTalk()`（同质判定 helper）取代叠加式 all* 标志，
   混合步骤扩展不再逐形状加分支。
6. **登记表/数据面**：`quest_client_talk_pages.tsv`（1340×3，wave A 词汇）、
   `quest_client_talk_chain_pages.tsv`（124 行，wave B 梯）、漂移 ADOPTED 719、
   裁定 `p5-datadriven-decisions.tsv`（DD_TALK_SIMPLE 70 / DD_TALK_CHAIN 13）、冻结指纹 719、
   `RetailDataDrivenGateTest` 地板 719。

## 证据（命令 → 结果）

- 生成器修正后：11323 第三段 `1693:10002`，127→124 行（偏移家族三行按台账排除）。
- `ReportToManySetSucceedAlignmentTest`：11323/21323 五段链 + 15401/25401/18970/28970 单段
  全绿；15402/25402/25000 偏移形状退回后零失败。
- 退回管线：decisions=719（verdicts：hunt 428 / collect 208 / talk 简单 70 / talk 链 13 /
  skip 789）；`verify_retirement` = `catalog 2321 + retired 3903 = 6224 — OK`，裸悬空引用 0。
- `RetailDataDrivenGateTest` 5 例绿（漂移登记 1508 行全覆盖 + 冻结指纹恰覆盖 719 退役 + 地板）。
- 链合同集 8 类（ReportToMany / ThreeStage / Batch29 / MirrorPair / MigratedRepair / 1919 /
  HandoverAudit / Gate）全绿。

## 对拍与判例

- **推进动作 id 与页 id 空间重叠**：SETPRO1..12=10000..10011 与模板页
  （QUEST_COMPLETE 10000 / DEFAULT_SUCCESS 10002 等）同 id。信件图遍历必须**先按推进语义
  归类**——推进动作终结梯子，剩余按钮才是页面导航。入 Playbook（图遍历类）。
- **信件无阶段边界**：客户端信件只给页与按钮，QUEST_SELECT 落点（阶段边界）是 ScriptDLL
  家族语义。简报链登记表行可作"续接落点"信号（15402 行 `1352 1353:1353 10001:0` 与真端一致），
  但多段拆分不可推导 → 合同锁定形状宁可退回 XML，不虚报 RETAIL_TABLE。
- **11323 单治愈边**：链形任务簿修复边只允许 stale=lastRow−1 → lastRow 一条。

## 未验证 / 阻塞

- 全量 T1/T3 未在本切片内复跑：并发会话 SimpleTalk/SimpleHunt 大批在飞（生产视图
  `missing=1213`、wrongOwner 集持续变化），等其落地后统一复跑（跨会话台账已有登记）。
- 1876/2876/15550/25550/15402/25402/25000 保持 XML 车道，合同测试继续锁其真端形状。

## 下一步

- P5-1 hunt 合同重塑（`QuestLegacyMonsterHuntProductionFlowTest` (状态,投影) 定位 + 领奖态
  重谈路由补齐）——进行中，见台账 P5-1 归责段。
- P5-4 EnterArea/pvp 系 → P5-5 变体 → P6 终局报告。
