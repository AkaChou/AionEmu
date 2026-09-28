# Phase 3 勘测报告：页码/登记类 TSV 全量退役可行性（只读取证 · 2026-09-28）

> 授权：用户「phase 3」。范围 = 只读取证，不碰形状、不解冻红集；执行立项另行决定。
> 背景口径：manifest 政策「页码类 TSV 只允许退役（删文件+删清单行+改常量），不允许静默新增」；
> W5-g1..g4 已退役 4 张（report_pages / entry_pages / talk_pages / briefing_chains），
> 批 0 R1 已缩 dialog_exits（token 13429→443）。本勘测回答：**剩余 22 张还剩多少退役面**。

## 方法

`census_readers.py`（本目录）：对 22 张注册表做①src/main、src/test 全树文件名直接引用；
②清单 note 点名消费者类的类名引用；③重点表人工核对读取点与所在编译路径。
辅证：recon-pack §1-6（W5/g3 退役时已逐点核实过 talk_pages 等）、批 0 R1 普查、
README Phase 2 各切片的退场记录。

## 结论总表（22 张 → 三分类）

| 分类 | 张数 | 表 | 判据 |
|---|---|---|---|
| **可退役** | **1** | `retail-quest-ai-name-groups-rejected.tsv`（18 行） | main/test **双零引用**；清单 note 自证「零生产消费者」；生成器在兄弟车道（`p0c52_quest_ai_name_groups.py:313`，`if emit:` 内）。退役 = 删文件 + 删清单行 + `EXPECTED_TSV_COUNT` 22→21 + 兄弟车道生成器停写移交（改脚本仍越界，只登记）。注意：role=retention 审计账，删除丢审计史（生成器可重算再生）——**需车道 owner 认可** |
| **需缩表（行级普查 v2）** | **1** | `quest_client_dialog_exits.tsv`（3938 行 / 443 token） | 7 个读取点全活（`RetailSimpleTalkDefinitionCompiler.buildChain:1212/1411/1438/1465`、`reportFlowChain:1701/1727/1733`、`RetailDataDrivenCollectCompiler:64`、`RetailDataDrivenDefinitionCompiler:163`），但 Phase 2 后行级死亡面扩大：SELECT1_1 系只挂 buildChain 非 canonical 块，S3c 报告页退场（39 行）+ S2 规范段（203 行）后 SELECT5_CHECK*/SELECT6 的实际命中行需按**新编译路径**重算。方法 = 批 0 R1 同法（行×旗标可达性普查 + 前后指纹逐字节 + 快照恒等），判不了的行保守保留 |
| **仍活** | **20** | 其余全部 | 证据：`census-readers.tsv`。要点——`summary_rows`（8931 行）被 9 个编译器消费（REWARD var0=rows-1 投影）；`handin_exceptions`（7606 行）经 `withExceptions` 并入 `handin_pages` 同生共死（handin 家族三票否决不迁移 ⇒ 仍活）；`talk_chain_pages` 活于 DD allTalk 链与 TalkCompiler 阶段梯（QE-080 页 id 源）；`use_item_report` 活于 UseItem 报告模式；`talk_chain_steps`/`hunt_*`/`kill_targets*` 是真端语义合同（非页码补丁，不在退役政策射程）；`retention`/`name_string_ids`/`use_item_npcs`/`enterarea_zone_resolution`/`legacy_heal_rows`/两张 quest_dialog 契约账全是活通道 |

## 判读

1. **老 Goal 的「废除 18 张 quest_client_* 页码 TSV」已基本完成使命**：4 张已退役（W5），
   其余 survivor 大多已不是页码补丁，而是语义登记（击杀合同、链步骤、区域解析）——
   「页码类只退役不新增」政策的自然终点就是现在这个形态。Phase 3 的真实剩余执行面很小。
2. **可执行批次（如立项）**：
   - 批 P1：退役 `ai-name-groups-rejected`（1 文件 + 1 清单行 + 计数 21 + 移交登记），
     零代码变化、门禁 = 清单门绿 + 全量红集恒等；前置 = 你/车道 owner 对删除审计账的认可。
   - 批 P2：dialog_exits 普查 v2 + 缩表（预期删 token 数待普查；判据 = 家族门定义快照逐键相同
     + 红集字节恒等）；零风险回退（少删只损整洁）。
   - 批 P3（可选）：`EXPECTED_TSV_COUNT` 冻结于新值；生成器移交清单并入 README 终局章节。
3. **不建议扩大**：其余 20 张的「仍活」判据已逐表落 census；在没有新的家族迁移（如 handin
   翻案）前，无进一步退役面。

## 证物

- `census_readers.py` + `census-readers.tsv`（22 行 × 8 列全量引用矩阵）
- 读取点人工核对：`RetailSimpleTalkDefinitionCompiler`（buildChain/reportFlowChain 行号见上）、
  `RetailQuestDriver.java:479-490`（handin_pages/exceptions 装载）、
  `RetailDataDrivenDefinitionCompiler:163-165`（selectNoneLadder）
