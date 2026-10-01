# SimpleHunt 专属 raw vars 存档结论（计划不变式 7）

日期：2026-10-01　工具：`p1/tools/simplehunt_rawvars_archive.py`（只读 SELECT，产出
`p1/tools/simplehunt-rawvars-unexpected.tsv`——本次为空头文件仅表头）

## 结论：CLEAN —— 无需迁移

- 范围：入仓 `Quest_SimpleHunt.xml` 全表 1863 行（含 4 休眠零计数行 + 47 休眠有计数行）。
- 生产库 `al_server_gs` 实测：SimpleHunt 任务共 **213 行玩家存档、94 组 (quest, vars) 值**，
  **100% canonical**（每组的 vars 都落在该表行相机可达值集合：宽度按「任一 count>63 ⇒ 10 位」
  规则推导，各槽值 0..required 的组合）。零 high_bit（≥0x40000000）、零 packed_unexpected、
  零 invalid、零休眠行非零。

## 迁移/运行时策略（据此定案）

1. **迁移**：无需任何存档迁移或规范化；native 直接按 raw int 接管。
2. **运行时守卫**：native handler 在写入前校验快照 vars 落在可达集合（或至少：守卫位清零 +
   各槽 ≤ required）；不满足 → `NATIVE_RAW_VARS_INVALID` fail-closed，不静默规范化。
   当前实测违例为 0，该守卫是纯保险丝。
3. **休眠行**：51 行无脚本接线（对拍已证），native 不接 handler，其 vars（实测全 0）不动。

## 注记

- 样本量：213 行覆盖 94 组值，相对全族 939 个 production 行偏小——结论按"现有数据 CLEAN +
  运行时保险丝"表述，不宣称穷尽。若后续库数据增长出现 unexpected，按
  `simplehunt-rawvars-unexpected.tsv` 口径补录并逐条归因。
- 分类器与 P0a 全局探针（`p0a/tools/raw_vars_probe.py`）同口径；差异 = 相机规约改从入仓表推导
  （不依赖 camera-params.tsv 的 1812 接线集），范围按 SimpleHunt 全表。
