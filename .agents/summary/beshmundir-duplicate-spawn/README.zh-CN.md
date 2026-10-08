# 帕休曼迪尔寺院同坐标重叠怪修复（真端双页展平）

## 1. 需求
- 实机反馈：BT（world 300170000）同位置两只「brutal soulwatcher」叠在一起（截图查询到 216584，用户报 216588，
  坐标均为 788.9853/442.9633/222）。

## 2. 根因
- **真端 BT 是"双页"副本**：`Map/Worlds/idcatacombs/world.xml` 的生成组（`SPG_C_Lichkey2_55_Ae` 等，
  `max_page="2"`）在同组内并列两个候选 —— `IDCatacombsN_*`（`spawn_page start=1 end=1`，普通页）与
  `IDCatacombsH_*`（`start=2 end=2`，困难页），同一位置**只会刷当前页那一套**。
- **emu 静态数据把两页展平**：`spawns/Instances/300170000_Beshmundir_Temple.xml` 把 N/H 两套**都**刷了出来 ——
  全副本共 **24 对、130 处精确同坐标重复**（不止用户看到的那一只）。
- emu 侧其实已有页机制：`SpawnEngine.matchesInstance(spawn, difficultId, spawnPage)` +
  `SpawnGroup2.spawnPageRestricted`（先例 = 竞技场/训练场副本按 `spawn_page` 分波次；实例页由
  `AutoGroupType.getSpawnPage()` 驱动）。BT 的实例页恒为 0（`InstanceService.getNextAvailableInstance`
  传 `0, 0`），且 BT 数据未做页标注 → 两套全刷。
- **副本主干 = H 套**：`BeshmundirTempleInstance` 的 onDie 只引用 `216583-216586`（H_Lichkey1-4）与部分
  **单套 N 名 Boss**（216238 Lakhara、216245 Macunbello 等，无重叠）；玩家推进副本（摆渡人/开门/电影）
  依赖的就是 H 套。N 套在 Handler 中零引用。
- 掉落对照：同名 H/N 怪掉落一致（216583 与 216587 的 npc_drops 逐条相同），消除 N 侧无掉落损失。

## 3. 改动（纯数据，单文件）
`src/main/resources/aion/data/static_data/spawns/Instances/300170000_Beshmundir_Temple.xml`：
- **14 个"全部刷点精确重叠"的 N 元素** → 标 `spawn_page="1"`（页 0 实例跳过；数据保留，未来接入
  BT 难度页时按真端语义复现）。216240/216268/216285/216287/216289/216290/216291/216293/216294/216312/
  216317/216587/216588/216589。
- **10 个"部分刷点重叠"的 N 元素** → 仅删除与 H 精确同坐标的 95 行 spot，保留其 **28 个独有位置**
  （216265 有 6 个在 400m+ 外的另一区域；216304/216307/216308 等有若干 12–65m 同区点）。
  216265/216266/216267/216288/216303/216304/216307/216308/216309/216310。
- diff：14 insertions / 109 deletions（14 标页行改 + 95 spot 行删）。

## 4. 工具（保留于本目录）
- `scan_duplicate_spots.py <spawn file> [page=0]`：按 `SpawnEngine.matchesInstance` 页语义扫描同坐标重复。
- `report_dual_page_pairs.py <spawn file> <npc templates...>`：关联同坐标对与模板 `IDCatacombs[NH]_` 命名，
  输出保留（H）/标注（N）清单。
- `audit_pair_spot_distance.py <spawn file> [tol=1.0]`：逐点计算 N→H 最近距离，识别"无 H 孪生"的 N 独有位置
  （这是**不能整元素标注**的判据）。
- `prune_duplicate_n_spots.py <spawn file>`：执行清理（全重叠标页 / 部分重叠剪除 spot）。

## 5. 验证
- 静态（已完成）：`xmllint --noout` 通过；页 0 重扫 **0 duplicate coordinates**；28 个 N 独有 spot 保留；
  diff 审查逐段正确（剪除行均与对应 H spot 精确同坐标）。
- 实机（待用户验收）：进入 BT 新实例，确认同位置仅一只（如 788.9853/442.9633 处仅剩 216584）。
- 生效方式：`//reload_spawn <任意大地图>` 会全量重载 spawn 数据（副本图不在 reload 名单，无需重启即可让
  新数据进内存）；**已存在的 BT 实例需重新进入**（新实例）才可见效果。

## 6. 全库普查（遗留，未处理）
按页 0 扫描 `spawns/Instances/*.xml`：**39 个文件存在同坐标重复**（约 500 处）。重点：
301700000_IDRun 60、302200000/302300000 Dredgion Defense 54/52、320130000_Adma_Stronghold 52、
300040000_Dark_Poeta 41、310110000_Theobomos_Lab 38、300100000_Steel Rake 12、300150000/300160000 Udas 13/15、
320080000_Draupnir_Cave 16（与 memory-bank IR-006 的未决重叠案例同图）等。
**处置纪律（IR-006）**：同坐标重复只触发归属审计，不能批量删除；需逐副本按真端双页/波次/编队语义对照后再修。
本次仅处理用户报告的 BT。

## 7. 边界
- 只处理"与 H 精确同坐标"的 N 侧；N 侧 28 个独有位置保留（其真端页归属未逐一核对）。
- 14 个标页元素在页 0 消失；若未来实现 BT 普通/困难页（实例页 1/2 + H 套补 `spawn_page="2"`），按真端语义恢复。
- 未改动 Handler、任务注册与掉落数据；`//reload_spawn all` 会重刷全部大地图，生产上建议用轻量地图名作参数。
