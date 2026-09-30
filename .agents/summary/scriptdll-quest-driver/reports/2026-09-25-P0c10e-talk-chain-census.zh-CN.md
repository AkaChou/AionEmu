# P0c-10e：SimpleTalk RETAIL_TALK_CHAIN 322 行链式合成普查（census 切片，无落地）


> **生成物清理提示（2026-09-30）**：本文件正文提到的 `*.tsv` / `*.log` / `*.txt` / `*.xml` 等中间转储已随 `chore(agents)` 清理删除；原文与留痕仍可从 git 历史取回，被删清单与再生成入口见 `.agents/summary/CLEANUP-LEDGER.zh-CN.md`。

- 日期：2026-09-25
- 切片：P0c-10e（普查 + 波次切分 + 机制原料；**无生产代码改动、无退役**）
- 产出：`p0c10e_build_talk_chain_census.py` → `p0c10e-talk-chain-census.tsv`（322 行 × 9 列）

## 1. 关键发现

1. **波次切分**：322 = **145 纯链（wave A：talk_npc1..N 链 + 无物品/过场轴）** +
   **177 复合（wave B：164 带物品轴、33 item_check、9 过场，取并集）**。wave A 先行。
2. **名字证据（编译器级三通道：name_desc 裸查 + `npc_` 去前缀别名 + 客户端交付登记）**：
   **308/322 全名解析**；14 行 NAME_EVIDENCE_NEEDED（`_faction_` 势力哨兵 → 走系统发放形状
   先例 M5-b2c；个别名字如 kistig/Aeolus 需补显示名族证据）。
3. **客户端证据**：quest-dialog-pages.csv 的 select2 系页 **322/322 全覆盖**——
   链 NPC 的对话页在客户端任务书全部有登记（推进按钮图推导留给合成器波，按
   `build_quest_client_talk_chain_pages.py` 的推进优先判据复用）。
4. **XML 对照形状**：链的退役前 XML 模型 = **stage 阶梯**（`started var0=0 → sK var0=K → reward`，
   链 NPC `SETPRO1..k` 推进 + `[SyncQuestState, SHOW_SELECTION_PAGE SELECT_QUEST]` after-commit，
   QUEST_SELECT/SELECT2_x 纯页视图自环）——与 P5-3 wave B DataDriven 链语义同构，
   合成器可按同构扩展（样本 1115：单链步 + v1 节点）。
   签名初分类：LADDER_CANONICAL 若干 / LADDER_NONE 20（XML 无 SETPRO 阶梯，链用页链表达）/
   **DEVIATION 302（分类器偏严，逐行偏差原文已留证）**——合法页视图（SELECT1_x 等）与
   非严格 +1 阶梯都被计入，裁定波需按 P0c-8c 判据机方式逐机制解释。

## 2. 工具与教训

- 普查脚本 v2 三通道名字解析；**v1 教训**：`resolve()` 用 `ids.pop()` 破坏性消费集合，
  同名第二次解析即误报 UNRESOLVED（495 假阳性）→ 改 `next(iter(ids))` 非破坏读取，
  假阳性清零（308 全解析）。留 `# 不得用 ids.pop()` 头注防回归。

## 3. 下一步（合成器波，独立切片）

1. **wave A 合成器扩展**：`RetailSimpleTalkDefinitionCompiler` 增链式路径（stage 阶梯 +
   SETPROk 推进 + 页视图自环 + SELECT5 报告 + npc-complete），客户端按钮图按推进优先判据接线；
2. **等价对拍**：145 行 vs 退役前 XML（302 处 DEVIATION 原文作逐机制解释的裁定原料，
   按 P0c-8b/8c 判例：真端优先或留 XML）；
3. **裁定 + 落地**：ADOPT/KEEP 逐行 + retention 翻转 + XML 退役 + 门禁（族门禁扩链不变量 +
   T1 + T2 + T3 clean）；
4. wave B（链+物品复合 177 行）随后续波次。
