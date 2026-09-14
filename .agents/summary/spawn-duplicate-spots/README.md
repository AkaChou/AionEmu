# 非真端残留 spot 去重 / Non-retail leftover spawn de-duplication

## 症状 / Symptom

- 真端反馈：`278628` (Sileni)、`278630` (Lachesis)、`278633` (Larentia) 在同一位置出现 2 个。
- 触发位置：`400010000_Reshanta.xml` 的三处 `<spawn>` 块各含 2 个坐标重合的 `<spot>`。

## 加载语义 / Load semantics

`SpawnGroup2` 对块内每个 `<spot>` 各构造一个 `SpawnTemplate`
（`SpawnGroup2.java:104` 等处遍历 `spawn.getSpawnSpotTemplates()`），
`SpawnEngine.spawnObject` 逐模板实例化。因此同块内 2 个 spot = 刷出 2 个 NPC。

## 根因 / Root cause

同一 `<spawn>` 块内，旧 legacy spot 与真端 spot（`resolve_z="true"`）平面坐标重合，
两者都被加载。残余由两个提交引入：

| 提交 | 日期 | 引入数量 |
|---|---|---|
| `01f4ec0bb` | 2026-07-13 | 94 |
| `a5e274fd0` | 2026-08-14 | 44 |

`a5e274fd0`（"synchronize retail NPC spawn surfaces"）**并未新增点**，它只是给已存在的
第二个点补上 `resolve_z="true"`。以 `278628` 为例，父版本 `231bc3517`（2026-07-27）
起该块已是 2 个 spot；重复在 `01f4ec0bb` 就已产生。

## 处置 / Change

保留真端 spot（`resolve_z="true"`），删除同块内与之平面重合的非真端 legacy spot。

- 删除 137 个 legacy spot，涉及 33 个文件，纯删除、零新增。
- 逐行内容校验（`apply_final.py`）：行号与 `<spot>` 文本必须完全匹配，否则拒绝；
  含 `resolve_z` 的行一律拒绝删除。
- 保留 1 处待人工判断：`210130000_Inggison [Master Server].xml:6918`
  （`entity_id="2392"` 与真端 `entity_id="3719"` 不一致，注释标注 `find the right ID`）。

判据文件：`apply_list.json`（最终清单）、`confirmed.json`（历史取证确认记录）。

## 判据 / Criterion

对每个候选对 `(L, R)`：找到引入真端点 `R` 的提交，检查其**父版本**中同一 `npc_id`
块内是否已存在与 `L` 平面重合的 legacy 点。

- 父版本已有 `L` → 该提交在 `L` 旁新增重合的 `R`，制造重复 → 确认删除。
- 父版本无 `L` → `L` 与 `R` 同批引入 → 排除。

平面重合阈值 `dxy <= 2.0`；**z 不参与判据**——真端 z 由地形重投影
（`SpawnSurfaceResolver.resolve`），与旧值天然不同。

判定结果：确认 138，排除 0，无法判定 0（初判 24 个不确定项经块级复核全部确认为真阳性）。

## 陷阱 / Pitfall

**不要用含 z 的坐标哈希判断"某点是否为新引入"。** 首次判据用
`(x, y, z)` 三元组比对，而真端化会把 z 从旧值改为重投影值
（如 `278628` 的 z 由 `1513.9066` 变为 `1515.0`），导致 `R` 被误判为"提交新增的点"，
进而把该块误判为"新增点但漏删旧点"。此缺陷使候选数只有 68 而非 138，
且把 `278628` 等案例错误归因给 `a5e274fd0`。

## 佐证 / Evidence

- 删除的 8 个 `walker_id` 均为死引用：`ai-waypoints.xml` 中 0 命中；
  各自的真端替代点均带同名可解析的 `retail:` 路径。
- 删除的 `random_walk` 点在真端替代点上均保留了该属性。
- 33 个改动文件 XML 语法校验通过。

## 测试 / Tests

`NormalBalaureaSpawnDataTest` 的 Reshanta 基线 631 → 630：被计数的
`278045` 的 `walker_id="Tern"` 点是死引用，属修正而非放宽。

既有失败（与本次改动无关，改动前即失败，已用 `git stash` 对照确认）：
`InstanceWalkerFormationsPositionGroupingTest.separatesExtraSoloUnitFromTheobomosKrallOffsetFormation`
——涉及路线 `NPCPathLF2B_NPC_Town3`，与本次删除的 `LF2B_NPCPath_Sanctuary_Guard_M1` 无关。

## 验证边界 / Validation boundary

- Static：XML well-formed 校验通过；`dataholders`、`spawnengine` 测试通过（除上述既有失败）。
- 未执行服务端或真端客户端复验。
