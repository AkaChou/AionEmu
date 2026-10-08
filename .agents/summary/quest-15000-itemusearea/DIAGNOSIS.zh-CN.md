# Q15000 维修工具无法使用 —— 使用区未注册（QE-043 同类）诊断与修复

- 日期：2026-10-08
- 任务：15000（天族 Cygnea 4.8 成长任务，成长线「오드 영역 확장」）
- 道具：`182215662`（`quest_15000b`，Artificial Aether Generator Repair Tool）
- 状态：**实现完成 / 待实机验收（PENDING）**——静态校验通过；门禁与测试未运行（无构建授权），实机需冷重启服务端加载 XML

---

## 1. 现象（用户实机日志）

```
[QUEST-TRACE][C->S] CM_USE_ITEM 玩家=Kk 道具=182215662 itemObj=151893
WARN c.a.gameserver.world.zone.ZoneName - 缺少区域：LF5_ITEMUSEAREA_Q15000
WARN c.a.gameserver.world.zone.ZoneName - 缺少区域：LF5_ITEMUSEAREA_Q15000
```

每次 `CM_USE_ITEM` 恒定两条「缺少区域」告警（12:31:45 / 12:32:13 / 12:32:14 / 12:32:18 四次尝试），道具无法使用。

## 2. 根因链（已逐环核实）

1. `item_template_182005539_190200002.xml:15640`：`182215662` 声明 `<uselimits usearea="LF5_ITEMUSEAREA_Q15000" .../>`；
2. `ZoneName.get("LF5_ITEMUSEAREA_Q15000")`：该名未在 `zones_*.xml` 注册 ⇒ 告警「缺少区域」并**回退 `NONE` 实例**（`ZoneName.java:88-99`；名字出口在 `ZoneTemplate#setXmlName → ZoneName.createOrGet`）；
3. `PlayerRestrictions#canUseItem`（`PlayerRestrictions.java:500-511`）：`hasAreaRestriction()`（第一次 `getUseArea()` → 第 1 条告警）为真 → 再取 `getUseArea()`（第 2 条告警）= `NONE` ≠ `_ABYSS_CASTLE_AREA_` ⇒ `!player.isInsideZone(NONE)`；
4. 全区名 `NONE` 无任何 `ZoneInstance` 匹配（全图默认区名 = `mapId.toString()`，见 `WorldZoneTemplate:40`）⇒ 判定失败 ⇒ 下发 `1300143`「无法在此处使用该物品」，物品使用被拦截（与 QE-043 同型）。双告警 = `hasAreaRestriction()` + `getUseArea()` 各调一次 `ZoneName.get`，可反推每次 `CM_USE_ITEM` 均被拦截，无一次通过。

## 3. 权威几何（真端）

`<真端根>/Map/XML/Subzones/source_sphere.csv:95696`

```
usearea_lf5_itemusearea_q15000,itemUseArea,lf5,0,2890.31,826.17,706.21,28.50,,0,0,-1,1
```

- 列序 `name,type,zone,layer,x,y,z,r,...`；与已核对的 `q30721`（152.65/1430.13/488.10/41.04）、`q13403a/b`（r=59.37/55.90）逐位一致，字段映射无歧义。
- `zone=lf5` → mapid 映射两路互证：`<真端根>/Map/XML/Subzones/WorldId.xml`（LF5 → 210070000）＋ 本仓既有 `LF5_ITEMUSEAREA_Q10502/Q30721` 均挂在 `mapid="210070000"`。
- 位置合理性：球心周边 30–90m 内有 Cygnea 采掘点（`spawns/Gather/210070000_Cygnea.xml`，z≈657–697），非孤立坐标。

## 4. 修复

`src/main/resources/aion/data/static_data/zones/zones_quest.xml`（Cygnea 4.8 段，Q30721 之后）：

```xml
<zone mapid="210070000" name="LF5_ITEMUSEAREA_Q15000" area_type="SPHERE" zone_type="ITEM_USE">
    <sphere x="2890.31" y="826.17" z="706.21" r="28.50"/>
</zone>
```

- `zone_type="ITEM_USE"` 与 Q30721 先例一致；`ZoneService#getZoneInstancesByWorldId` 的 default 分支按普通 `ZoneInstance` 注册，`MapRegion#isInsideZone(ZoneName)` 按名身份匹配即可命中。
- 生效路径：重启后 `ZoneData.afterUnmarshal` 建区并注册区名 ⇒ `ZoneName.get` 命中 ⇒ 玩家移动进区（`CreatureController` 的 `revalidateZones`）后 `isInsideCreature` 为真 ⇒ 道具可用 ⇒ DD `ItemPlay(QUEST_15000B)` 步进（`DataDrivenNativeRuntime#onItemUsed`，物品名索引 `quest_15000b → 182215662`）。

## 5. 已验证 / 未验证

已验证（静态，无构建）：

- `xmllint --noout --schema zones.xsd zones_quest.xml` → **validates**；
- 覆盖复扫：usearea 注册数 82 → **83**，`LF5_ITEMUSEAREA_Q15000` GAP 关闭；
- 既有门 `MultiCellSensoryZoneRegistrationTest` 只按区名断言（含 XSD 校验），无总量断言 ⇒ 新增区不破坏该门。

未验证（待授权/待用户操作）：

- `mvn` 聚焦测试与生产目录/白名单门禁（未执行，无构建授权）；
- 实机：需**重启服务端**加载新 XML，随后在生成器处使用维修工具（3D 距离 ≤28.5），任务应从 ItemPlay 步入 REWARD。

## 6. 全库同类审计（QE-043 类问题普查）

工具：本目录 `audit_itemusearea_zone_registry.py`；输出 `itemusearea-zone-registry.tsv`（逐 usearea：注册文件 / 真端 source_sphere / 真端 world.xml / 使用它的物品）。

全库 133 个 usearea：已注册 83、**GAP 50**。分组：

| 分组 | 数量 | 说明 |
|---|---|---|
| 真端 `world.xml` 有定义（需世界对齐） | 35 | ldf4b=22、ldf4a=4、tiamat_down=2，其余为 ab1 / IDRaksha_solo / IDSweep / IDStation / ldf5b 单件；多数所属世界（Danaria 等）不在本仓 5.8 世界表内，需先做短名→mapid 对齐 |
| 真端两源均无定义 | 15 | 含 `_ABYSS_CASTLE_AREA_`（代码特判，非 GAP）、2017 事件道具、`LF5_ITEMUSEAREA_Q10503`（物品名即 "(Unused)"）等疑似退役内容；真端是否同样不可用未证 |
| 本次修复 | 1 | `LF5_ITEMUSEAREA_Q15000` |

**未批量修复的理由**：剩余两类分别缺「世界短名→mapid 证据」与「真端缺区语义证据」，按规则不做猜测；待用户决定是否继续做批量对齐与修复（可作为独立任务，含注册覆盖棘轮测试）。

## 7. 同类批量排查与修复（2026-10-08，用户「排查类似问题修复」）

### 7.1 方法

1. 审计脚本升级为可行动分类（`audit_itemusearea_zone_registry.py`）：`REGISTERED` / `GAP:fixable` / `GAP:world_absent` / `GAP:no_retail_def`；判定「世界是否存在」用真端 `WorldId.xml` 的 id 对本仓 `world_maps.xml` 求交（**剔除注释行**——`[Master Server]` 等 map 是注释掉的），600x 段真端 id 与客户端 id 不同号，故仅对内容反查过的 `ldf5a ↔ 600050000`（`LDF5A_ITEMUSEAREA_*` 区名同时出现在两处）做显式覆盖。
2. 几何取源沿用 QE-043 口径：优先真端 `source_sphere.csv`（球心+外接半径），无球值时用真端 `Map/Worlds/<world>/world.xml` 的 `<item_use_area>` 多边形（含 bottom/top）。
3. 落面复核：目标 map 必须存在于本仓 `world_maps.xml`；坐标须落在该图 world_size 内；区内不得与既有区重名。

### 7.2 已修复（4 个 usearea / 6 处落面）

| usearea | 落面 | 几何（真端源） | 备注 |
|---|---|---|---|
| `AB1_ItemUseArea_Q2060` | 400010000（Reshanta） | SPHERE 1524.98/1590.73/1599.15 r=77.80（source_sphere，zone=ab1 layer=1） | 任务 2060 道具「Empty Glass Bottle」 |
| `IDRaksha_ItemUseArea_Q28703` | 300610000（Raksang Ruins） | SPHERE 676.68/667.03/527.06 r=19.57（source_sphere，zone=idraksha_solo layer=2） | 任务 18703/28703 道具（Abandoned Balaur Egg） |
| `IDStation_ItemUseArea_3F` | 300240000 + 300241000 | 既有 POLYGON 原样保留（更名） | 旧名 `IDSTATION_ITEM_USE_AREA_1`（300240000）/`_2`（300241000）**无任何 item 引用**，且几何与真端 `Map/Worlds/idstation(_event)/world.xml` 的同名区**逐点相同** ⇒ 直接更名，不新增重复区 |
| `IDSWEEP_ITEMAREA_SUMMON` | 301400000（Shugo Emperor's Vault）+ 301590000（Emperor Trillirunerk's Safe） | POLYGON 4 点（world.xml，bottom=391.304840 top=441.304840；两世界同形） | 商城召唤道具（cash summon，无球值源） |

### 7.3 未修复（46 - 4 = 42 个 GAP，均列明理由，不做猜测）

| 分类 | 数量 | 理由 | 例 |
|---|---|---|---|
| `GAP:world_absent` | 29 | 所属世界不在本仓 5.8 世界表（Danaria/LDF4b、LDF4a、Tiamat's Down、LDF5b —— 真端 id 600031000/600021000/600041100/600061000 均不存在），注册到不存在的 map 无意义 | `LDF4B_ItemUseArea_Q*` × 23、`LDF4a_ItemUseArea_Q*` × 4、`TDown_ItemUseArea_Q*` × 2 |
| `GAP:no_retail_def` | 16 | 真端两源（source_sphere + 全 worlds world.xml）均无该区名定义；含 `_ABYSS_CASTLE_AREA_`（代码特判 FORT 区，非真 GAP）与 `LF5_ITEMUSEAREA_Q10503`（物品名即 "(Unused)"）等退役内容 | `DF6_ITEMUSEAREA_Q15692`、`LF4_ITEMUSEAREA_Q10035`、`IDCatacome_ItemUseArea_Q20025` 等 |
| 事件区（刻意不修） | 1 | `F6_EVENT_ITEM_USEAREA`（真端 LF6/df6 定义为**整图**多边形事件区，供 2017 开发者日变身/刷怪道具）——注册=放行整图使用事件道具，属产品决策而非数据修复，留待用户定夺 | item 188010012/188010013 |

补充证据（宽松子串扫描，排除「拼写差异」）：`Q10035` 处只有 `LF4_QuestArea_Q10035`/`LF4_FOBJ_Q10035A`/`LF4_SensoryArea_Q10035A`，无 `LF4_ITEMUSEAREA_Q10035`；`Q10503` 处只有 FOBJ `LF5_FOBJ_Tiamat_SealWatcherN_Q10503a` 等；`Q20025` 处只有 `IDCatacome_SensoryArea_Q20025`（Sensory≠ItemUse 族，不可互替）——故这些 item 的 usearea 在真端 5.8 已无对应区。

### 7.4 验证与状态

- 静态：三份改动文件 `xmllint --schema zones.xsd` 全通过；审计复扫 **133 / 87 注册 / 46 GAP**（fixable 仅剩事件区 1 个）；新增区与既有区无重名。
- 回归门：`ItemUseAreaZoneRegistrationTest`（锁 4 个新区的区名/地图/区类/球心半径/多边形首点与底顶 + Aturam 更名不回退 + 三文件 XSD）。
- 测试（IDEA MCP，2026-10-08 用户授权）：`ItemUseAreaZoneRegistrationTest` 4/4、`MultiCellSensoryZoneRegistrationTest` 5/5、`QuestProductionStartupGateTest` 2/2，合计 **11/11 全绿**。首跑 3/4：新门「环数」断言误把 `getPoints()`（环列表）当顶点数，已改「1 环 × 4 顶点」后复跑通过（首跑第二轮的相同失败文本为旧字节码，重跑即刷新）。
- 待办：本批 4 个区的实机验收未做（需冷重启后分别验证对应道具：任务 2060、任务 18703/28703、Aturam 技能道具、商城召唤道具）。
