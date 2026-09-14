# Inggison Live World Migration

## Symptom

点击英吉斯温地图中的“移动到据点”会传送到 `210130000`（Inggison [Master Server]），实际应传送到 `210050000`（Inggison）。

## Rule

- 玩家可见的英吉斯温传送目标统一为 `210050000`。
- `210130000` 只保留为旧镜像服地图定义和兼容性防护常量，不再作为玩家传送目标。
- `TeleportService2` 对英吉斯温镜像服目标做最终归一，热点、门户、任务和物品路径即使误配也会落到 `210050000`。

## Main Changes

- `hotspot_location.xml`: 英吉斯温热点模板全部改为 `210050000`。
- `portal_loc.xml`: 英吉斯温门户坐标全部改为 `210050000`，并补齐 `loc_id=2100500`。
- `instance_exit.xml`: 英吉斯温副本出口改为 `210050000`。
- 任务、区域、风轨、活动、攻城、天气和 AI 区域引用同步迁移到正常英吉斯温。
- `TeleportService2` 与 `HotspotTeleportService` 增加镜像服到正常世界的运行时归一。

## Verification

- 静态 XML 解析通过。
- `git diff --check` 通过。
- IDEA 静态检查无错误。
- 排除 `world_maps.xml`、`id-mappings.xml`、`zones_210130000.xml` 与镜像服 spawn 资源后，运行数据中无 `210130000` 引用。
- 用户已在真实客户端确认“点击地图传送”验证通过。
- 本次未执行 Maven；服务端运行状态由用户管理。
