# 阶段立项：批 P2 · `dialog_exits` 行级可达性普查 v2 + 缩表（2026-09-28）

> 上游：`phase3-recon/2026-09-28-phase3-feasibility.zh-CN.md`（22 张三分类：可退役 1 / 需缩表 1 / 仍活 20）
> + `phase3-provenance/2026-09-28-authority-provenance-audit.zh-CN.md`（21 张权威血缘；G1–G4 已冻结/补通道）。
> 用户决定「1、2、3、4」= 执行 G1–G3 冻结/补通道 + 立项 P2 + G4 说明 + 审计提交与 push。
> 性质：**只缩行、不删表**；零 IR 变化；manifest 行与 `EXPECTED_TSV_COUNT`（21）不变。

## 1. 对象与现状

- 表：`src/main/resources/aion/data/static_data/quest_retail/quest_client_dialog_exits.tsv`
  （批 0 R1 后 3938 行 / 443 token；role=client-registry）。
- 7 个读取点（Phase 3 勘测实测）：
  ① `RetailSimpleTalkDefinitionCompiler.buildChain:1212/1411/1438/1465`；
  ② `RetailSimpleTalkDefinitionCompiler.reportFlowChain:1701/1727/1733`；
  ③ `RetailDataDrivenCollectCompiler:64`；
  ④ `RetailDataDrivenDefinitionCompiler:163`。
- Phase 2 后的行级死亡面变化：SELECT1_1 系只挂 `buildChain` 非 canonical 块；S3c 报告页退场（39 行）
  + S2 规范段（203 行）——须按**新编译路径**重算，不能沿用批 0 R1 的旧结论。

## 2. 判据（本次新增一条，来自血缘审计）

1. **可达性**：逐（行 × 旗标）判定其读取点所在编译路径是否仍可达（singleStep / precheck / canonical /
   链式），方法体无条件读的旗标（SELECT2_CONTINUE / SELECT5_CHECK* / SELECT6）**禁删**。
2. **客户端证据（新增）**：每个保留/删除裁定都必须挂客户端出口证据
   （`docs/quest/client-dialog-mapping/` 的页面/按钮/动作 CSV：`client-html-pages.csv`、
   `page-action-map.csv`、`client-hyperlinks.csv` 等，已入库 17 文件）。
   **无客户端证据的行不得因"看起来没人用"而删**——保守保留并在报告标记。
3. **判不了 = 保留**：少删只损失整洁，多删直接变形 IR。

## 3. 批次划分

### P2a · 只读普查 v2（先出报告，不改数据）

- 脚本：`phase3-provenance/census_dialog_exits_v2.py`（新，只读；输入 = 表 + 编译器读取点 + 客户端 CSV）。
- 产出：`phase3-provenance/census-dialog-exits-v2.tsv`（逐 token：旗标 / 读取点 / 可达路径 /
  客户端证据指针 / 裁定(删·留·判不了) / 理由）+ 报告段。
- 完成判据：7 个读取点逐点复核在案；每个"删"候选都有客户端证据指针；候选删 token 总数与预估行数落账。

### P2b · 缩表执行（P2a 通过后另起一批）

- 动作：按候选清单删 token（行级快照 `retired-tsv/quest_client_dialog_exits.tsv.rows-<date>`）。
- 判据：前后逐行指纹 dump **逐字节相同** + 家族门绿 + T1/T3 红集 sha 恒等
  （T1 `3b92439da8…` / T3 `5e3acdb9dcaf…`）+ manifest 行/计数不变（**不删表**）。
- 生成器移交：`build_quest_client_dialog_exits.py` 不感知缩表（已在 README 终版移交清单第 ② 项）——
  本车道只登记，若重跑把行写回则按新基线重新普查（fail-closed 由 T1/T3 红集对拍兜底）。

## 4. DoD

1. census v2 报告落账（含客户端证据列与三态裁定）；
2. 缩表前后快照 + sha256 入册；
3. 前后指纹逐字节相同（SimpleTalk 链门 + DD 家族门）；
4. T1/T3 红集 sha 恒等；清单门 3/3 绿；
5. `EXPECTED_TSV_COUNT` = 21、manifest 行数不变；SEMANTIC_GAP = 0 不回退。

## 5. 纪律与回退

- 每批两条提交（`fix(quest)` + `docs(quest)`）；显式路径、禁 `-A`、不 push（本次 P2 立项书随
  血缘审计提交一并落）；单条命令 ≤600s，长命令走 `tools/gate_bg.sh`；T3 在仓库外全树副本跑、跑完即删；
  不创建 worktree；兄弟车道生成器**只读**。
- 回退：缩表若牵出任何形状/指纹漂移 ⇒ 当批回退、逐条归因后重排；客户端证据缺失的行一律保留。
- 禁止新增任何 TSV（manifest 政策：页码类只退役不新增）。
