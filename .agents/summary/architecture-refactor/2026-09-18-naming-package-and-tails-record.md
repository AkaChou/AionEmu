# 阶段三（类名）、阶段五（package）与收尾尾巴（2026-09-18）

承接 `.agents/summary/architecture-refactor/2026-09-18-split-batch-record.md` 的分阶段纪律
（结构 → 验证 → 命名 → 验证 → package → 验证），本轮按同一顺序执行，最后补上 QuestService 计时器域。

## 阶段三：类名优化（只 rename，不动 package）

### 已执行

| 原结构 | 修改后 | 依据 |
|---|---|---|
| `configs/main/AdvCustomConfig`（4 字段：`CUBE_SIZE` + `GAMESHOP_LIMIT/CATEGORY/LIMIT_TIME`） | **类删除**：`GAMESHOP_*` 并入 `InGameShopConfig`，`CUBE_SIZE` 并入 `CustomConfig` | 类名与内容都不对应（既不是"高级"也不是一个领域），属"宽泛命名 + 多个独立能力"；属性键与默认值不变，`Config.load()` 两处装载同步收敛 |
| gameserver `GameConnectionFactoryImpl` | `GameClientConnectionFactory` | 唯一实现 `NettyConnectionFactory`（commons 的通用接口，无同名接口），`Impl` 无信息量；新名说明它造 `AionConnection` |
| loginserver `AionConnectionFactoryImpl` | `LoginClientConnectionFactory` | 同上，造 `LoginConnection` |
| loginserver `GsConnectionFactoryImpl` | `GameServerConnectionFactory` | 同上，造 `GsConnection` |
| chatserver `GsConnectionFactoryImpl` | `GameServerConnectionFactory` | 同上 |

隐式引用同步核查：`NetConnector`、chatserver `NettyServer`、`GameServerNetworkRuntimeBridge` 的 import/构造点全部更新；
`LoginProtectionServicesTest` 用**源码路径字符串**读 `AionConnectionFactoryImpl.java`，路径随改名更新；全仓库
（java/properties/xml/scripts/docs）搜索 `ConnectionFactoryImpl` 无残留。

### 刻意保留（不为了改名而改名）

| 候选 | 保留理由 |
|---|---|
| `gameserver/loginserver/chatserver` 各自的 `configs.Config` | 模块内唯一入口且无歧义；改名要动 56 处调用点与测试，纯噪声 |
| `network/PacketWriteHelper` | 抽象基类 + 3 个真实子类（iteminfo blob），名字与职责一致 |
| `*RuntimeBridge`(20) / `*Fallbacks`(11) / `*Lifecycle`(44) / `*Gateway`(40) | 描述真实角色（生命周期相位、迁移期桥、门面），不在用户列的模糊后缀清单里；批量改名=100+ 类 churn |
| `AStationConfig` | 抽查确认内容与名字一致（A-Station 跨服开关/等级/服务器 ID） |

## 阶段五：package 结构（结论：**不动**，证据如下）

用户候选清单里的每一项都用证据复核过，结论是现有布局正确：

| 候选 | 复核结论 |
|---|---|
| `services/{conquestservice,svsservice,agentservice,zorshivdredgionservice}`（各 2 类） | 不是碎片：每包是「开关/单据类(45 行) + 状态持有者(113–223 行)」，且被同域 `model/<domain>` 包（`model/conquest`、`model/svs`、`model/agent`、`model/zorshivdredgion`）与对应 `XxxService` 双向引用；同样的 `anohaservice/beritraservice/moltenusservice/towerofeternityservice/...` 共 10+ 个包同构，单独合并 4 个只会破坏约定 |
| `configs/schedule/**`（15 个 JAXB 类） | 是**运维可编辑配置**：XML 就在 `src/main/resources/aion/config/schedule/*.xml`（`Config` 解析该目录），包名与落盘位置一致 |
| `configs/ingameshop/InGameShopProperty` | 同样由 `Config.configFile("ingameshop/in_game_shop.xml")` 从 `aion/config/ingameshop/` 载入，属配置而非静态数据 |
| `chatserver/network/factories` | 与 `loginserver/network/factories` 同构约定（两个 `*PacketHandlerFactory`），保留 |
| `chatserver/model/message`、`network/netty/handler`、`utils` | 单文件但包语义清晰、命中率高（handler 有 16 个引用方）；移动只产生 import 噪声 |
| `services/item` 与 `services/drop` | 分别承载物品域服务与掉落分发，是两个独立领域，边界清晰，保留 |
| `gameserver/ai/**` 的 1–2 文件包 | SDJ-001 红线（动态反射包树），不可动 |

## 收尾尾巴

- 5 个类级配置样板（`ChatRestartServicesTest`、`RestartServiceTest`、`PlayerAggroLevelTest`、
  `ProtectorConquerorServiceTest`、`PlayerLimitServiceTest`）改用 `ConfigSnapshot` 字段；非配置清理
  （instance-aggro / handled-worlds / sell-limit 映射）保持原顺序。
- `.agents/summary/**` 的 AI 中间产物（审计脚本与生成的表格）按仓库约定继续作为**未追踪的工作区证据**保留，
  只提交策展后的文档；不写入 `.gitignore`，避免掩盖误加的产物。

## QuestService 计时器域（结构收尾，最后一批）

`QuestService` 原本把任务事件入口与计时器注册表混在一起（两张静态表 + 注册表锁 + 启动/取消/清理 + 3 个私有类型）。
现拆出包级私有 `services/QuestTimers`：

- 状态与并发语义**逐字搬运**：单锁保护注册表、替换时先取消旧任务、超时回调仅在真正移除表项时投递；
- `QuestService` 保留同签名的公开静态门面（`questTimerStart` / `invisibleTimerStart` / `questTimerEnd` /
  `cleanup*QuestTimers` / `hasQuestTimers`），全部是单行委派，任务脚本、`PlayerQuestTimerPort`、GM 命令无需改动；
- `QuestServiceManagedTimerTest` 改为直接驱动 `QuestTimers`（同包）；`QuestService` 1560 → 1475 行。

## 验证

| 阶段 | 命令 | 结果 |
|---|---|---|
| 配置解散 | `mvn -B -q compile` + 6 个聚焦类 | 47 例全绿 |
| 类名 | `mvn -B -q compile` + 5 个聚焦类 | 31 例全绿 |
| 尾巴 | 5 个聚焦类 | 11 例全绿 |
| 计时器 | 4 个聚焦类 | 36 例全绿 |
| 收口 | `mvn -B clean test` | **3456 例 / 0 失败 / 0 错误 / 2 跳过，BUILD SUCCESS** |
