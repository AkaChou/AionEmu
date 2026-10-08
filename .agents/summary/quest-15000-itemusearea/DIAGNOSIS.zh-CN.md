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
