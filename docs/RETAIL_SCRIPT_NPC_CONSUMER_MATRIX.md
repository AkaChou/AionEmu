# Retail ScriptNpc 消费者矩阵

本矩阵锁定 139 图审计范围内仍由副本 Handler 消费的 `handleUseItemFinish`。分类只决定审计顺序，不授予迁移权限；只有真端生产者、动作参数、消费者和生命周期全部闭合后，才能删除旧所有者。

分类：

- `RETAIL_SCORE_FALLBACK`：交互与持久化计分、战斗期或封顶结算耦合，保留 Handler，并对已被 Pattern 接管的对象退出。
- `STATEFUL_LEGACY`：涉及物品事务、门、阶段、动态出生、消息、奖励或调度，缺少完整真端链时保留 Handler。
- `SCRIPT_NPC_CANDIDATE`：当前外形接近通用数据动作；仍需逐对象证明真端技能、物品、变量、传送端点及失败语义。
- `NON_PRODUCTION`：世界未纳入 139 张生产副本，不为清零矩阵而扩大范围。

| Handler | 分类 | 当前保留边界 / 下一证据门 |
|---|---|---|
| `idgelDome/IdgelDomeLandmarkInstance.java` | `RETAIL_SCORE_FALLBACK` | 战斗期、稳定键幂等、终局计分与结算 |
| `idgelDome/IdgelDomeInstance.java` | `RETAIL_SCORE_FALLBACK` | 交互计分、持久化、封顶与结算 |
| `KamarBattlefieldInstance.java` | `RETAIL_SCORE_FALLBACK` | 战斗期计分、随机物品、持久化与结算 |
| `pvparenas/PvPArenaInstance.java` | `RETAIL_SCORE_FALLBACK` | 普通 Arena 零分对象与竞技场阶段计分 |
| `pvparenas/HarmonyArenaInstance.java` | `RETAIL_SCORE_FALLBACK` | Pattern 对象先退出；仅保留未接管对象的回退计分、技能与封顶 |
| `SeizedDanuarSanctuaryInstance.java` | `STATEFUL_LEGACY` | 物品、门、消息与奖励 |
| `DanuarSanctuaryInstance.java` | `STATEFUL_LEGACY` | 背包容量、宝箱奖励、门消息与对象生命周期 |
| `EsoterraceInstance.java` | `STATEFUL_LEGACY` | 门 `39` 与区域消息 |
| `SmolderingFireTempleInstance.java` | `STATEFUL_LEGACY` | 按阵营选择变身、旧效果清理及副本计时状态 |
| `BeshmundirTempleInstance.java` | `STATEFUL_LEGACY` | 任务状态、任务物品、缺失演员生产与失败消息 |
| `SealedArgentManorInstance.java` | `STATEFUL_LEGACY` | 计时、阶段、状态与结算 |
| `OccupiedRentusBaseInstance.java` | `STATEFUL_LEGACY` | 动态出生、门、影片和未被 Pattern 支持的对象 |
| `RentusBaseInstance.java` | `STATEFUL_LEGACY` | 动态出生、门、影片和任务流程 |
| `EngulfedOphidanBridgeInstance.java` | `STATEFUL_LEGACY` | 消耗品、阵营变身、消息与炮击调度 |
| `TreasureIslandOfCourageInstance.java` | `STATEFUL_LEGACY` | 阶段、奖励、条件变量与对象生命周期 |
| `OphidanWarpathInstance.java` | `STATEFUL_LEGACY` | 消耗品、消息、炮击调度与战场状态 |
| `TiamatStrongholdInstance.java` | `STATEFUL_LEGACY` | `701523` 为 `NoAction`，仅旧所有者能开启门 `22` |
| `KumukiCaveInstance.java` | `STATEFUL_LEGACY` | `703424` 的真端 `IDEvent_Solo_jail_door` 继承 `NoAction`；钥匙事务、失败消息与对象删除仅有旧所有者 |
| `AdmaStrongholdInstance.java` | `STATEFUL_LEGACY` | `700396/700397` 的真端两个 `NPC_AI_Dispel_*Debuff` 均继承 `NoAction`；效果 `18462/18463` 仅有旧所有者清理 |
| `TrialsOfEternityInstance.java` | `STATEFUL_LEGACY` | `731736` 的真端 `IDEternity_03_Teleport_6` 仅继承通用 `NPC`，地图数据只有自身出生点；缺传送端点，保留物品事务、失败消息和传送旧所有者 |
| `CradleOfEternityInstance.java` | `STATEFUL_LEGACY` | `834007` 的真端 `IDEternity_02_D_button` 仅继承通用 `NPC`，Pattern 无按钮动作；物品事务、失败消息与条件变量写入仅有旧所有者 |
| `ElementisForestInstance.java` | `NON_PRODUCTION` | 禁用世界，不属于 139 张生产图；保持旧所有者 |

已收口：`KromedesTrialInstance` 的旧 `19248/19247` 交互桥接已删除。真端 `282093/282095` 是死亡触发对象，Pattern 分别生成 `282085/282084`，由其施放 `19274/19273` 后自销毁。

已收口：`TransidiumAnnexInstance` 的八个载具对象由 Pattern 按阵营 tribe 校验后使用唯一技能槽并自销毁；四个 `277225..277228` 是仅有 `20378/20381/20383` 的战斗炮台，不存在旧 `21652` 交互。旧 Handler 消费者已删除。

闭包测试：`RetailScriptNpcConsumerClosureTest` 精确锁定 22 个文件和 `5/16/1` 分类计数；`SCRIPT_NPC_CANDIDATE` 已清零。迁移一个消费者时，同一提交必须删除对应 Handler 方法或分支，并更新本矩阵与测试。
