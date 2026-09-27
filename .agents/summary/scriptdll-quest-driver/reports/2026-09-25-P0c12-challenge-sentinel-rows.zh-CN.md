# P0c-12：SimpleTalk 挑战任务哨兵 60 行采纳退役（SENTINEL_NO_GRANT_PATH 桶归零）

> 日期：2026-09-25/26 ｜ 切片：P0c-12 ｜ lane：SimpleTalk ｜ 前序：P0c-11（碰撞收口）

## 交付

1. **生产代码（挑战任务哨兵受理形状归位）**：
   - `requireAcquire`：`CHALLENGE_TASK` 哨兵不再走「无受理入口」拒绝——受理 NPC = 交付 shugo
     本人（`requireNpc(rewardNpc)`）；
   - `build()`：同口径解析 `acquiredNpc`；`systemGrant` 排除 `CHALLENGE_TASK`
     （走对话接取，**不合成 SystemGrant 边**——挑战任务在本服无发放 caller，
     `RetailSystemGrantDispatcher` 只有 faction 调用点）。
2. **证据链（老 XML 全 60 行实证）**：接取 NPC == 交付 NPC——55 行同名体（判例 17100：
   accept 831222 == reward Town_shugo_Worker_05），5 行（27100 系）老 XML 接 E 侧 shugo、
   真端表交付 D 侧变体 = 5.8 种族归一（真端对、XML 错）。客户端 select1 在册（对话接取页），
   item_check 全部有 `collect_item`（CHECK 按钮对 + collect 门，判例 4056/17100：
   metal_n_c_30a 5 ↔ 152000204）。
3. **裁定与退役**：60 行 ADOPT（`p0c12-sentinel-decisions.tsv`）；差异轴 = ①老 XML 的
   SETPRO1 接取额外路由 / SET_SUCCEED(10255) 交付按钮（canonical = 39/20002 CHECK 对）+
   ②奖励模式 EXACT→QUEST_BASE（族规范）+ ③拒绝路由源归位 + ④QE-051 投影差——全族 canonical
   形状判例，逐行 `p0c12-17100-canonical.txt` 过渡级直证。
4. **门禁对齐**：`RetailSimpleTalkGateTest` 增 P0c-12 分支——CHALLENGE_TASK 走对话接取形状
   （`hasAccept(acquireName)`），`acquireName` 镜像编译器（= rewardNpc），系统发放断言
   （无接取路由/必须有 SystemGrant 边/报告 NPC 关窗出口）不再套用挑战任务行。

## 结论（实测）

- SimpleTalk 族：**2223 = 2007 RETAIL_TABLE + 216 XML_RETENTION**（SENTINEL_NO_GRANT_PATH
  桶 60 → 0 归零）
- 全库 retention：**6224 = 4687 RETAIL_TABLE + 1537 XML_RETENTION**
- **verify_retirement = 1537/1537/4687 sum=6224 — OK**（全库精确闭合）
- 门禁：`RetailSimpleTalkGateTest` 3/3 + `RetailSimpleTalkChainGateTest` 2/2 +
  `RetailOwnershipGateTest` 4/4 全绿
- catalog 1537 条；60 退役 id 零残留（迁移脚本断言 + 迁移后抽查）

## 归账 / 排障记

- **manifest 门禁 6E（非本片）**：DataDriven 族 15548/25548 的 XML 零节点（0-node 文件在
  catalog 中挂 EXECUTABLE，mtime Sep 25 05:16 起 20+ 小时未动）——全部 6 个 catalog 级测试
  方法共享整目录编译因此同因失败。lane-owned 文件，按不触碰纪律未修补；**建议 lane owner
  修复或把两行转 `mode="REFERENCE"`/补节点**——该阻塞影响所有 catalog 全量测试。
- verify 首跑 FAIL（sum 6198）为 **陈旧 .agents 副本**（00:22 快照，lane 并发 +26 行期间）；
  重新生成三副本后 OK——副本读取路径注意（教训：verify 前确认三副本 mtime 一致）。
- 过渡级取证方法：节点投影差会短路 irEquivalenceProblem（不比对过渡集），本片用
  signature 级探针（`RetailTalkSentinelProbeTest`，已删源存 .java.txt）+ 全量转储直证。
- 运行时行为（挑战任务对话接取→交付→完成链）与客户端目检：需启动服务授权，未执行。

## 下一步

- SimpleTalk 余量 216：CLIENT_ROUTE 73、TALK_CHAIN 42（21 通道缺口续波）、CRAFT 28、
  名字未解 27、CLIENT_REPORT_VARIANT 17、CUTSCENE 无触发 12 等
- 其他族：SimpleHunt gap 47 / reject 108；DataDriven 随并行 lane（含 15548/25548 修复）
