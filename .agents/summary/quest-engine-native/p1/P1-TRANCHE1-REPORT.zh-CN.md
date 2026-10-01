# P1 第一横切报告：共享组件（codec / 相机 / 注册表）

> 状态：**完成（17/17 测试绿）**。授权链：用户「继续完成计划」+ 计划 §7「跨家族共用组件可在该批先行」。
> 本横切 = P1 声明范围（§7 批门表 P1 行）中**不依赖家族切换决策**的共享件：纯新增、零删除、零路由接入、零 owner 变更。
> 日期：2026-10-01。

## 1. 范围声明（§4.7 纪律）

| 项 | 内容 |
|---|---|
| 新增生产类（3 个，符合"≤3 类"止损线） | `tablelane/RawQuestVarsCodec`、`tablelane/ProgressCamera`、`tablelane/CameraRegistry` |
| 新增测试 | `RawQuestVarsCodecTest`(5)、`ProgressCameraTest`(7)、`CameraRegistryTest`(5) |
| 聚焦命令 | `mvn -Dtest='RawQuestVarsCodecTest,ProgressCameraTest,CameraRegistryTest' test` |
| 不触碰 | 全部既有生产路径、QE-112 文件、XML 车道、HtmlPagesRegistry |

## 2. 组件与真端锚点

1. **RawQuestVarsCodec**：6 位 ×5 槽 / 10 位 ×3 槽（bit0..29），`vars<0x40000000` 守卫区，槽越界/超掩码/守卫位 fail-closed。不复用 `QuestVars`（不变式 8）。
2. **ProgressCamera**：纯函数相机。三重守卫（status==START(3)、守卫区、槽未满）任一不满足 = `NO_ACTION`（超杀零副作用）；一次事件只加一；`flag && newVars==fullValue` → `ADVANCE_WRITE`（真端 +0x100），否则 `NORMAL_WRITE`（+0xf0）。持久化/同步不在本类（state port 属切换批）。含真端 Status 枚举（0/3/4/5/6，P0a §4.2 行为证据）。
3. **CameraRegistry**：启动期可构建的不可变行注册表；重复注册 = `NATIVE_CAMERA_WIDTH_MIXED`、required 超掩码 = `NATIVE_CAMERA_REQUIRED_EXCEEDS_MASK`、fullValue ≠ 槽组合 = `NATIVE_CAMERA_FULL_VALUE_INVALID`、空/非正 spec = `NATIVE_CAMERA_ROW_MISSING`（错误码语义与计划 §6.3 一致）。**13912/23912 形态（fullValue 含未声明槽）直接拒绝**——由数据层显式登记，本类不兜底（D.3-3 反漂移）。

测试锚点全部取自 P0a 相机矩阵（带 文件:行号）：1143=6位{1:10}/0xa（fun_773.cpp:13）、1842=10位{1:80,2:1}/0x450（fun_763.cpp:958/fun_760.cpp:643）、2354=6位{1:6,2:4}/0x106（fun_773.cpp:1003/949）。

## 3. 测试结果

```
mvn -Dtest='RawQuestVarsCodecTest,ProgressCameraTest,CameraRegistryTest' test
Tests run: 17, Failures: 0, Errors: 0 — BUILD SUCCESS
```
- 首轮 1 红 = 测试自身夹具算错（`flagFalseForcesNormalChannel` 起点 vars 误写 0x105，slot2 已满触发守卫）；修正为 0xc6 后全绿。产品代码零改动。

## 4. 同批追加调查：方案 B 数据源（sql.rar）探测

- bsdtar 可解 `sql.rar`：内容为 4 个 MSSQL `.bak`（Aion_gm 317MB / AionWorldLive 59MB / AionAccountDB / CacheDB 日志）。
- 二进制探针（UTF-16LE + ASCII）：`bountyhunter`/`soglo`/`utisda`/`NpcTemplate` 等在两个库备份中**零命中** ⇒ 真端 NPC 模板表不在这些备份中（可能在 CacheD 私有数据文件）。**方案 B 数据源仍不可得**，已记入 `../p1-prereqs/name-resolution-decision.md` §5。

## 5. P1 剩余部分（仍被门禁，待用户决策）

- `NativeQuestTableLoader`（SimpleHunt 表模型）+ CameraRegistry 的**表行数据接线**（含 13912/23912 两行脚本权威登记）；
- state port（status/raw vars 持久化与客户端同步，按 P0a 双通道矩阵）；
- SimpleHunt handler + owner/router 接入 + **同批删除** SimpleHunt 编译器与狩猎 TSV 读取；
- 全部以上绑成一个完整的 P1 切换批，等 `p1-prereqs/name-resolution-decision.md` 的 A/B/C 裁决 + 批清单确认后开工。

## 6. 边界自查（D.4 收口段）

- [x] 零 quest id 常量（测试锚点 id 属测试证据，生产类无任务常量）；
- [x] 零客户端数据进 `src/main`；零台账；
- [x] 既有生产路径零改动（diff 仅新增 3 类 + 3 测试）；未 stage、未 commit、未 push。
