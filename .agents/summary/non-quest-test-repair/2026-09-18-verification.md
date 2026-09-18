# 非 quest 全量失败修复

## 范围与根因

- `SMPlayerSpawnTest`：夹具仍向 `World.worldMaps` 注入 `IntObjectHashMap`，而生产代码已使用 `WorldMap[]` 与并行 ID 数组。改为测试内的 `World.getWorldMap(int)` 查询替身，继续断言 44 字节出生包的地图、位置及尾字段；不修改生产 World 或协议。
- `NpcAbnormalImmunityTest`：类级 Lombok `@Getter` 为掩码生成 `int getAbnormalImmunity()`，与 JAXB 注解的 `setAbnormalImmunity(String)` 类型不匹配，XML 属性未生效。提交 `8a1371be6` 的类级注解调整引入了该 getter；禁用这一个字段的 getter 后恢复原 String 写入属性。免疫分组掩码、命名状态与 XML 数据保持不变。
- 修正规则中“类级 Getter 对实体安全”的绝对表述：需要检查框架属性发现与 getter/setter 类型配对；不维护类型名单。

## 验证证据

1. 修改前，串行执行 `mvn -q test -Dtest=SMPlayerSpawnTest,NpcAbnormalImmunityTest -DfailIfNoSpecifiedTests=false`：2 例，1 failure、1 error，复现两个原始问题。
2. 添加四组真实 JAXB 解码参数用例；事件处理器拒绝绑定错误，同时以免疫查询断言捕获无异常却忽略属性的情况。
3. 反证：仅暂时移除 `@Getter(AccessLevel.NONE)`，执行 `NpcAbnormalImmunityTest`，5 例全部失败；随后恢复修复。没有修改预期免疫分组以使测试通过。
4. IDEA 指定文件编译通过。
5. 已授权串行全量复跑 `mvn -q test`，完整日志为本目录 `full-test.log`。执行时 HEAD 和工作区快照为 `run-head.txt`、`run-status.txt`；并行 quest 修改保持原样，因此本次不是隔离提交基线对照。

## 最终全量复跑结果

- `mvn -q test`：3419 例，76 Failures、20 Errors、2 Skipped，退出码 1。
- `NpcAbnormalImmunityTest`：5 例全部通过；`SMPlayerSpawnTest`：1 例通过。
- 失败报告共 42 个类，全部位于 `questEngine`；没有剩余非 quest 失败类。
- Player 六个拆分测试类共 18 例全部通过。
- 相比上一轮 3413 例、77F/21E，两个非 quest 问题已经消除；本次新增 4 个免疫用例，另有并行 quest 测试变动，因此不能将测试总数变化作为独立回归证明。
- 原有 Lombok Unsafe 警告及测试夹具 final 字段反射警告仍在；本次未通过关闭 JVM 警告掩盖问题。

## 验证边界

- 本次不修改 quest 代码/数据，也不将 quest 失败一概归因为数据欠账。
- 上一轮“非 quest 失败与重构无关”的说法不成立：NPC 免疫加载问题有 Lombok 注解调整及去除/恢复注解的直接证据。
- 未启动、停止或重启用户服务器。全量测试内的生命周期测试不等于实机验收。
- 运行日志和快照为原始中间产物，不随源码提交。
