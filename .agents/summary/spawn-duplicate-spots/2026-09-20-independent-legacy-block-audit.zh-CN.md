# 独立 legacy 出生块同族审计（2026-09-20）

- 触发案例：`301570000` 的 `857783` 同时存在 legacy 块与真端 `initial_delay` 变体块，客户端看到 2 个。
- 关联：`IR-007`、`2026-09-19-drakenspire-q-twin-duplicate.zh-CN.md`。
- 状态：14 个高置信同族 NPC 已删除 legacy 块；聚焦测试与客户端复验待执行。

## 加载语义

`SpawnsData2` 对同一 `npc_id` 的多个块不是无条件全部加载：

- 只有至少一个块带 `spawn_page` 或非零 `initial_delay` 时，才会把所有变体块加入结果；
- 否则只使用第一个登记组。

因此本次只处理“真端变体块确实参与加载”的重复，不把无变体触发属性的第二块误判为重复。
判定代码见 `SpawnsData2.java:673-681`，测试合同见
`RetailOpenWorldSpawnDataTest.java:161-165`。

## 判定链

对全量 `spawns/**/*.xml` 按以下条件筛选：

1. 同一 `map_id` / `npc_id` 有多个块；
2. 至少一个块有 `spawn_page` 或非零 `initial_delay`；
3. 本地总 spot 数大于真端 `58Server/Map/Worlds/<world>/world_N.xml` 的同名 count；
4. legacy 块内所有 spot 都没有 `resolve_z`，真端块内所有 spot 都有 `resolve_z`；
5. 真端块坐标集合等于真端出生记录，legacy 块数量恰好等于本地多出的数量；
6. 块级历史：legacy 块来自早期导入，真端块由 `a5e274fd0` 加入。

## 已修复

| 地图 | NPC / 模板 | 删除的 legacy spot 数 |
| --- | --- | ---: |
| `300540000` IDLDF5b_TD | `230753` IDF5_TD_Nor_Ri_N_65_Ae | 1 |
| `300540000` IDLDF5b_TD | `230756` IDF5_TD_Nor_Ba_N_65_Ae | 2 |
| `300540000` IDLDF5b_TD | `233312` IDF5_TD_Easy_Pr_65_Ae | 5 |
| `301210000` IDLDF5_Under_01_War | `233474` IDF5_U1_War_Vri_Def01_Re_Wi_65_Ae | 1 |
| `301210000` IDLDF5_Under_01_War | `233479` IDF5_U1_War_Vri_Def02_Re_Fi_65_Ae | 1 |
| `301210000` IDLDF5_Under_01_War | `233480` IDF5_U1_War_Vri_Def02_Wi_SN_65_Ae | 1 |
| `301210000` IDLDF5_Under_01_War | `233481` IDF5_U1_War_Vri_Def03_Re_Fi_65_Ae | 1 |
| `301210000` IDLDF5_Under_01_War | `233484` IDF5_U1_War_Vri_Def04_Re_Wi_65_Ae | 1 |
| `301210000` IDLDF5_Under_01_War | `233485` IDF5_U1_War_Vri_Def04_Re_Fi_65_Ae | 1 |
| `301210000` IDLDF5_Under_01_War | `233486` IDF5_U1_War_Vri_Def04_Re_As_65_Ae | 1 |
| `301390000` IDSeal | `236126` IDSeal_A_Single_Fi_65_Ae | 2 |
| `301390000` IDSeal | `236223` IDSeal_B_KeyMob_SN_65_Ah | 1 |
| `301400000` IDSweep | `235653` IDSweep_S1_Shulack_As_65_An_H2 | 4 |
| `301400000` IDSweep | `235660` IDSweep_S1_Shulack_Wi_Nmd | 1 |

合计：4 个实例、14 个 NPC、23 个 legacy spot。删除动作由
`apply_independent_legacy.py` 按坐标集合严格校验后执行；任何不匹配都会拒绝。

## 仍需人工判断

剩余候选按原因分组，均未在本次删除：

| 类别 | 数量 | 代表 | 处理边界 |
| --- | ---: | --- | --- |
| 混合块 legacy spot | 17 | `300540000` `230754/230784/231117/231118/231120/231126/231127/231144/233311`；`301720000` `248383/248385`；`302340000` `246808/246870/246993`；`310160000` `248035/248488`；`320160000` `248488` | legacy 与真端点同块，需逐 spot 比对后删点，不能整块删除 |
| 不同 `entity_id` 对象 | 15 | `301520000` `731580`；`301720000` `248427`；`130090000` / `140010000` Arena Lobby；`300540000` `831329/831335`；`400020000` `833268-833271` | 不同 `entity_id` 是不同对象，禁止按坐标删除 |
| 竞技场 `spawn_page` 变体 | 4 | `300350000` / `300420000` 的 `207047`、`243668` | 分页选择不同战斗布局，不是普通双刷 |
| 其他位置/映射异常 | 2 | `302340000` `246853`；`302340000` `247117`（真端 `..._wi` 与 `..._Wi` 大小写两套记录） | 需先解决模板/大小写映射，再做单独处理 |
| 其他已取证待定 | 2 | `301520000` `731580`；`301390000` `236128` | 继续保留在 `2026-09-19-drakenspire-q-twin-duplicate.zh-CN.md` |

跨文件的 `600080000`、`120010000` 同坐标没有 `initial_delay` / `spawn_page` 触发，
加载器只登记第一组；它们不是本次“加载器确实实例化两条块”的路径，若后续出现可见重复再按文件归属审计。

## 闸门与验证

- 新增 `IndependentLegacySpawnSurfaceTest`，把 14 个 NPC 每个都必须满足：
  1 个 spawn 块、坐标集合等于真端块、所有 spot 带 `resolve_z="true"`。
- 静态：4 个目标 XML 解析通过；测试等价脚本逐项通过；IDE errors=0；`git diff --check` 通过。
- 聚焦测试（2026-09-20 23:21 通过）：`mvn -B test -Dtest='IndependentLegacySpawnSurfaceTest,ArchivesOfEternityQSadoFiSpawnSurfaceTest,DrakenspireDepthsQTwinSpawnSurfaceTest'` → `Tests run: 5, Failures: 0, Errors: 0, Skipped: 0`（BUILD SUCCESS）；`IndependentLegacySpawnSurfaceTest` 1 项、`DrakenspireDepthsQTwinSpawnSurfaceTest` 2 项均全绿（对应提交 `058868cde`）。
- 客户端复验（待执行）：重建/重启后分别进入 `300540000`、`301210000`、`301390000`、`301400000`，相关 NPC 应只按真端 count 刷出。

## 涉及文件

- `src/main/resources/aion/data/static_data/spawns/Instances/300540000_Eternal_Bastion.xml`
- `src/main/resources/aion/data/static_data/spawns/Instances/301210000_Engulfed_Ophidan_Bridge.xml`
- `src/main/resources/aion/data/static_data/spawns/Instances/301390000_Drakenspire_Depths.xml`
- `src/main/resources/aion/data/static_data/spawns/Instances/301400000_The_Shugo_Emperor_Vault.xml`
- `src/test/java/com/aionemu/gameserver/dataholders/IndependentLegacySpawnSurfaceTest.java`
- `.agents/summary/spawn-duplicate-spots/apply_independent_legacy.py`
