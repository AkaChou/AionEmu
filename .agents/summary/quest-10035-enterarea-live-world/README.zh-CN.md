# 10035 安格利浦关卡进区断链：DD 感官区 `_M` 大师服世界 → 活图归一（R5，2026-10-08）

> 主题：任务 10035「실렌테라 회랑 진격 준비 / 前往安格利浦关卡」第 5 步进区永不触发。根因 =
> 真端把**英吉斯温/格国的感官区只写在 `_M`（大师服）世界文件里**，而 DD 进区按同名绑定把这些区
> 的注册宿主落到了镜像世界 `210130000`；本服活图口径（`a7da0ad67`）里镜像世界不是玩家可达目标
> ⇒ 活图上无此区 = 死步。修复 = 生成器新增 **R5 活图归一**（只换注册宿主 mapid、几何/胞数/摘要
> 逐字不动）+ 台账 `normalized_from` 留痕 + 门禁 ⑥ 回归钉。

## 1. 现象与根因

- **现象（实机 2026-10-08，玩家 Kk）**：10035 推进到步数=4（`log/quests.log` 12:52:56
  `SM_QUEST_ACTION 10035 状态=3 步数=4`）后，到达「安格利浦关卡」区域任务不更新；期间
  `//movetonpc 702663 / 730256`（12:52/12:54）去的是**回廊入口**（702663 = 第 7 步 FOBJ
  「Corridor Entry Controller」、730256 = silentera westgate，坐标 y≈2299），**不是第 5 步的
  目标区**（多边形 y∈[1584,1744]，中心约 (1333,1664)）。
- **触发判据对照（为什么"XML 时代能过、DD 时代卡死"）**：
  - XML（typed，`9321e7663^:quest_definition/quests/10035.xml`）：`s4→s5` = `<enter-zone
    zone="ANGRIEF_GATE_210050000"/>`——活图旧壳区名，区就注册在 210050000（`zones_210050000.xml`）。
  - DD（`data_driven_quest.xml` 10035 第 5 个 data）：`EnterArea` + 载荷 `LF4_SensoryArea_Q10035A`；
    绑定 = 真端名哈希逐值比对（**同名**，QE-130）。
- **真端实读（`58Server/Map/Worlds/<world>/world.xml`，UTF-16）**：
  `lf4`=**0** / `LF4_M`=**15**（含 `LF4_SensoryArea_Q10035A`，14 个不同任务名：Q10024A/Q11040/
  Q11076A-C/Q11147A/Q11149A-C/Q14062/Q36500/Q36506/Q36512/IDTemple_Q30003）；`df4`=**0** /
  `DF4_M`=**13**。其余 254 个世界目录的感官区都写在**普通**世界文件里（对照 LF5=23、df5=25）——
  **英吉斯温/格国是"感官区只在 `_M` 大师服变体里"的两例**。关卡锚点 NPC 206363（"Angrief Gate"）
  的刷怪同样只在镜像刷怪数据（`210130000_Inggison [Master Server].xml`）里。
- **台账证据**：`qe-enterarea-retail-zone-resolution.tsv` 该行 = `R1_SAME_NAME / world_dir=LF4_M /
  mapid=210130000`；DD 全表 207 个进区步里**唯一**来自 `_M` 世界的别名。
- **断链**：活图 210050000 上不存在名为 `LF4_SensoryArea_Q10035A` 的区
  （`ZoneData` 按 mapid 分组、进区事件只按注册区派发）⇒ `QuestEngine.onEnterZone` 永不带上该名
  ⇒ DD `dispatch` 不命中 ⇒ `var0=4→5` 永不发生。

## 2. 修复面（R5 活图归一）

| 文件 | 变更 |
|---|---|
| `p7/tools/enterarea-retail-zone-probe.py` | 新增 `LIVE_WORLD_MAPID = {"210130000":"210050000","220140000":"220070000"}`（与运行时 `TeleportService2.resolveInggisonWorldId` / `HotspotTeleportService.resolveLiveWorldId` **同一张映射**）；`main()` 里对 OK 行做 mapid 级归一（归一目标不在 `id-mappings` 即 fail-closed）；台账追加 `normalized_from` 列、JSON 增 `live_world_normalized`；emit 头注释记录 R5 依据 |
| `zones_retail_enterarea.xml`（重生成） | `LF4_SensoryArea_Q10035A`：`210130000 → 210050000`；其余 104 区逐字节不变；几何/胞数/摘要零改动 |
| 台账 TSV/JSON（重生成） | 逐字段对拍旧版：**唯一差异 = 该行 mapid + 新列**（其余 120 行零漂移）；`--check` → OK |
| `src/test/resources/quest/retail-enterarea-zone-resolution.tsv` | 冻结台账副本同步（逐字节一致） |
| `DataDrivenEnterAreaPortGateTest.java` | 新增门面 ⑥ `mirrorAuthoredEnterAreasRegisterOnTheLiveWorld`：所有 OK 行宿主世界不得是镜像世界（按 `world_maps.xml` 的 `[Master Server]` 命名判别）+ 归一行数冻结 =1 + 归一行必须留痕真端镜像宿主 + 10035 第 5 步端到端（同名绑定 + 活图英吉斯温注册）；`enterAreaStepOf` 泛化为双参 |

**归一语义**：R1/R2/R3/R4 解析级联不变（别名 → 同名区）；R5 只改**注册宿主 mapid**（几何逐字
来自真端世界文件，胞数/摘要不动）。镜像世界不再承载该区（本就不可达；Silentera 大师服变体不在
运行时映射内、无 DD 引用，不预置）。

## 3. 门禁与验证（2026-10-08，IDEA MCP 已授权）

- `DataDrivenEnterAreaPortGateTest` **7/7**（含新 ⑥；首跑即命中新方法 = 新字节码）；
- `RetailEnterAreaZoneRegistrationGateTest` **2/2**、`DataDrivenNativeRuntimeGateTest` **50/50**、
  `QuestProductionStartupGateTest` **2/2** —— 合计 **61 项，0 失败 0 错误**。
- 静态：IDE 检查 0 error；工具 `--check` OK；全表 105 个 OK 行 mapid 全在 `world_maps.xml`、
  镜像承载数 = 0、归一行 = 1。
- **实机 PENDING（服务端重启由用户管理）**：重启后站到安格利浦关卡区（约 (1330,1660)，z 322–422）
  ⇒ 应见 `SM_QUEST_ACTION 10035 步数 4→5` 与步 5（击杀 Drakan ×10）指引。

## 4. 残留 / 风险

1. `LF4_M` 其余 14 个感官区（Q10024A/Q11040/Q11076A-C/Q11147A/Q11149A-C/Q14062/Q36500/Q36506/
   Q36512/IDTemple_Q30003）当前无 DD 别名引用（对应任务在 typed 车道用各自旧壳区）；若未来迁 DD，
   R5 规则自动覆盖（门禁归一行数会 +1 即红色复核）。
2. 镜像世界（210130000）不再有该区——按 `a7da0ad67` 口径镜像不是玩家目标；若将来实装大师服拓扑
   另案处理（当前镜像刷怪数据有裁剪，如 730256 无刷出点）。
3. R5 映射只对齐运行时白名单两对（英吉斯温/格国）；Silentera 大师服变体（600110000）不在映射内，
   当前无 DD 进区别名引用它，出现即按门禁红色复核。
