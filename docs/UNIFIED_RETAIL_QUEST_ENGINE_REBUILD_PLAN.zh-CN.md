# AionEmu 统一任务引擎迁移计划

状态：`ACTIVE`
最后更新：2026-08-03

## 目标

使用 XML 或类型化 Java DSL 定义任务，两者编译为同一不可变 IR，并通过唯一中央执行链替换全部旧
Java/XML Handler。运行时状态继续使用现有 `QuestStatus + quest_vars`，不建立第二套玩家任务状态。

当前阶段以本项目实际加载的 Handler、公共 helper、服务调用和外部任务 writer 为行为基线。真端资料只用于
后续校正任务数据和补充通用能力，不得作为本项目资源路径或运行时依赖。

## 已确定路线

```text
盘点共享机制
  -> 补齐 typed IR / DSL / XML / compiler / production port / composition
  -> 用真实代表任务验证能力
  -> 直接编写正式任务 XML 或 Java DSL
  -> 加入 production catalog
  -> 同时删除该 owner 的 legacy Handler
  -> 单元测试和 GM 真实流程验证
```

能力完成后没有中间任务阶段，不生成待转正任务，不建立 readiness、owner-switch 或 shadow 比对门禁。
Python 只做盘点、抽取、shape 聚合和校验，不能生成生产任务定义，也不能修改 catalog 或删除 Handler。

## 架构合同

- XML 与 Java DSL 必须降低为同一 `QuestDefinition` / `QuestTransition` IR。
- 事件统一经过 `QuestEventRouter`，状态与 required action 统一经过 `QuestExecutionCoordinator` 和
  `QuestUnitOfWork`。
- required action 与任务状态使用调用方持有的同一数据库事务；失败整体回滚。
- 内存任务状态只能在数据库提交成功后发布。
- 协议、传送、影片、刷怪、AI 和定时器等外部动作只能在提交后通过 typed port 执行。
- 同一 quest ID 只能有一个生产 owner；typed 执行失败不得回退 legacy。
- 禁止反射动作、字符串表达式、任意 service 调用、per-quest executor 和静默默认成功。

## 通用能力完成标准

一项能力只有同时具备以下内容才算完成：

1. 封闭的 typed IR 类型和参数校验。
2. Java DSL 与 XML/XSD 的等价表达。
3. compiler 降低、冲突检查和引用闭包。
4. 明确的 production port 与 composition root 装配。
5. 失败、事务、恢复、幂等、cleanup、credit 和协议时序中适用的合同。
6. 至少一个真实代表任务的正向路径和适用负例。
7. 接入 `QuestEngine` 对应的中央生产事件入口。

仅存在 enum/XSD、mock port、测试 fixture 或离线报告不算能力完成。

## 单 owner 直接迁移

每个 owner 必须在一个改动中完成：

1. 审查全部可达事件、条件、状态变化、奖励、副作用和客户端协议路径。
2. 确认只使用已完成的通用能力；若发现缺口，先补通用能力，再回来迁移。
3. 写入正式 `quest_definition/quests/<questId>.xml`，或注册正式 Java DSL definition。
4. 更新 `quest_definition_catalog.xml` 及必要事件索引。
5. 删除 `quest_script_data` 中的同 owner 项或完整 Java Handler。
6. 校验 XML/XSD、compiler、引用、唯一 owner、事务时序和任务业务路径。

正式任务 XML 只能包含 metadata、progress、nodes、events、conditions、actions 和 after-commit protocol。
evidence、源码 locator、迁移状态、比对覆盖率和 blocker 等过程字段只能存在于文档或离线报告。

## 当前落地

- 正式 catalog：`src/main/resources/aion/data/static_data/quest_definition/quest_definition_catalog.xml`。
- 正式 owner：任务 `1101`、`1102`。
- 正式定义位于 `src/main/resources/aion/data/static_data/quest_definition/quests/`。
- 1101、1102 已从 `quest_script_data/poeta.xml` 删除，不存在同 owner fallback。
- 当前生产入口已接通 `TalkToNpc` 和 `KillNpc`；其他事件需按通用入口逐类接入后再迁移对应 owner。
- 旧的任务输入 manifest、候选 importer/manifest、owner-switch 和 shadow-coverage gate 已删除。

## 推进顺序

1. 完成本轮 1101、1102 的全量自动验证和 1102 GM 回归。
2. 接通 `CollectItem` 的正式中央事件入口。
3. 直接迁移任务 1103 并删除 `poeta.xml` 中的 legacy owner。
4. 依次接通 item、zone/world、movie、timer、escort/AI、craft、PvP 等事件入口。
5. 每接通一种入口就迁移真实 owner，避免只建设不接管。
6. 持续重复，直到所有 legacy Handler 与过时 loader 全部删除。

## 工作区约束

- 从当前 `quest` 分支和当前工作树继续，不回滚无关修改。
- `src/main/resources/aion/config/main/quest-shadow.properties` 是用户本地配置，不覆盖、不暂存、不提交。
- `docs/quest/` 是本地执行记录，不得成为生产依赖。
- 所有 shell 命令使用 `rtk`，手工文件修改使用 `apply_patch`。
- 未经用户明确要求，不提交、不推送、不改写历史。
