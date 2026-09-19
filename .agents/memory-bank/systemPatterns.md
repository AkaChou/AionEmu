# System Patterns & Router (系统模式与知识路由器)

本文档是 AionEmu 的**全局排查与模式顶级索引**。所有 Agent 在排查问题或修改核心代码前，先通过本索引快速定位对应业务领域的避坑指南。

> role: router-only
> status: ACTIVE
> last_reviewed: 2026-09-19
> 说明：本文件只保留短摘要；证据、适用范围和失效条件以对应模式卡片为准。
> symptom_index: [symptom-index.md](symptom-index.md) (generated from Pattern metadata)

---

## 一、全局通用铁律 (Global Invariants)

1. **`ENV-001` 运行时生效基准**：先确认启动模式；IDEA 运行比对 `target/classes/`，打包运行比对实际 JAR/资源目录，不能把某一种模式的产物当成通用依据。
2. **`SDJ-001` 动态加载红线**：严禁仅凭 IDE 无直接静态调用删除 `gameserver.ai` 等动态反射包树中的类。
3. **`QE-001` Quest 对比基准**：当 `origin/history` 中存在 Git commit `911440146` 时，以其旧 Handler 对照；引用不可用时必须明确记录替代证据和限制。

---

## 二、细分业务域模式路由器 (Domain Patterns Index)

| 业务领域 | Pattern IDs | 核心避坑点速记 (Summary) | 细分指南文档 |
|---|---|---|---|
| **任务系统 (Quest Engine)** | `QE-001`, , , , `QE-002`, `QE-003`, `QE-004`, `QE-005`, `QE-006`, `QE-007`, `QE-008`, `QE-009`, `QE-010`, `QE-011`, `QE-012`, `QE-013`, `QE-014`, `QE-015`, `QE-016`, `QE-017`, `QE-018`, `QE-019`, `QE-020`, `QE-021`, `QE-022`, `QE-023`, `QE-024`, `QE-025`, `QE-026`, `QE-027`, `QE-028`, `QE-029`, `QE-030`, `QE-031`, `QE-032`, `QE-033`, `QE-034`, `QE-035`, `QE-036`, `QE-037`, `QE-038`, `QE-039`, `QE-040`, `QE-041`, `QE-042`, `QE-043`, `QE-044`, `QE-045` | target 投影会覆盖 action 变量自环计数；多 NPC 连续汇报严禁删中间 var 节点；终态自动清理 work-items；分支交付物在领奖阶段必须用 `count="ALL"` 避免阻断；无任务上下文的 NPC 选择保持普通对话；欧比斯准入只允许阵营任务完成态；工作物品可回收性以 quest_data.xml 声明为准；客户端 SECTION 计数必须使用固定 6-bit 位段；影片 self-loop 必须下发后续页；同 NPC 同阶段严禁无优先级动作重叠与计数节点投影重合；区域任务结束广播目标必须真实拥有该事件路由且不得包含自身；实例回退边只覆盖旧 handler 声明的阶段区间；未处理的任务动作不得回显成对话页，只能关窗；多段计数最后一个事件必须 priority 0 进入 REWARD；接取转换必须继承元数据前置条件事实需求；状态机禁止除 COMPLETE 外无出边死胡同且 kill 后 report 源必须对齐；前置有向图严禁自指与循环死锁；状态机禁止除COMPLETE外无出边死胡同且kill后report源必须对齐；前置有向图严禁自指与循环死锁；影片播放与SETPRO推进严禁同节点无actions死循环；掉落收集步数非0必须存在于节点有效var0集合；多档奖励必须声明reward-groups且每档下发对应奖励窗口、严禁死档；任务计时器必须有同id取消或同类型到期路由；奖励窗口页面只能按档位查表（第5/6档为页面45/46），预览与交付分支必须同档；任务 slot 幂等以权威handle仍可用为准，已死亡/已离开世界的NPC必须允许重建；掉落契约不得低于真端基线（按怪物加权），收集目标必须有掉落或give-item获得路径；quest_use_item掉落必须有START ACTION_ITEM_USE资格路由，并由生产目录门禁逐任务校验；接取元数据与档位1数值奖励对齐真端合同（sentinel语义、base死条目、整族一致差异按intentional记例外台账、npc-complete索引禁止位移）；多杀任务的击杀数只认客户端门控/旧handler/计数器三方一致，`<kills>` 不许反推；可重复任务 COMPLETE 重开局镜像必须带 `start-eligible`；transitions 块级属性（reported-reward-mode）重写必须保留；阵营日常必须声明 npc-faction-id 且轮换星期位不得全 0；迁移不得用 enter-zone 自动接取顶替 legacy NPC addOnQuestStart 的接取 owner，否则客户端点任务行会死循环；击杀计数结束时必须把任务说明行索引 SECTION_0 推进到报告行，只写计数字段会让客户端停在击杀行；`HACTION_FINISH_DIALOG(1008)` 只本地关窗、不产生服务端任务动作，ok 页只剩 1008 按钮的上交成功分支必须直接下发续接页；metadata `inventory-items` 是接取携带门禁、只能来自真端 `inventory_item_name`，`check_item`/`collect_item` 不得充当接取前置；道具使用区域必须在 zones_quest.xml 中声明以避免 1300143 拦截，剧本袭击怪需在后续交互清理，npc-complete 预览严禁含 USE_OBJECT 避免跳过故事页；旧 handler 进入 REWARD 不写 packed var，迁移后的 reward 投影必须等于进入前的 packed step 并补旧存档恢复边，否则领奖阶段任务书空白） | [patterns/quest-engine.md](patterns/quest-engine.md) |
| **副本与运行时 (Instance & Runtime)** | `IR-001`, `IR-002`, `IR-003`, `IR-004`, `IR-005`, `IR-006`, `IR-007`, `IR-008`, `IR-009`, `IR-010`, `IR-011`, `IR-012` | 副本特殊掉落注册时按物品 ID 去重；特殊属性绕过错误的 short 上限；GM 命令必须同时检查类扫描与有效运行时配置；巡逻编队按组锚点距离分组；事件日志与客户端崩溃不能仅凭时间相邻归因；重叠 NPC 先证明加载归属再删除；spot 身份判定禁用含 z 的坐标哈希，改用块级历史取证；NPC 死亡链路的玩家入参（getMostPlayerDamage）可为 null，必须判空；pattern 临时子对象自带 live_time 时只保留登记、由自己的到期任务清理，不随生成者状态重置被删除；真端对齐删除实例脚本硬编码副作用后必须保留幂等适配器（由实例生命周期驱动 RetailConditionSpawnEngine 条件变量与 RetailDynamicAreaEngine 动态区域，坐标仍取真端数据）；死亡/消失事件链（on_die/on_killed_by_user/on_killed_by_npc/on_despawn）里生成的子对象不随生成者状态重置删除（否则击杀 Boss 后应当现身的 NPC/传送门会被同一调用栈删掉） | [patterns/instance-runtime.md](patterns/instance-runtime.md)；副本销毁后残留的延迟任务必须自行收口（实例层统一在 spawn/killNpc 入口判 isInstanceDestroyed 与 null），否则会在拆除后的实例上刷 NullPointerException |
| **静态数据与 JAXB** | `SDJ-001`, `SDJ-002`, `SDJ-003` | JAXB 实体实例字段严禁声明为 `final`；动态反射包树与技能/NPC XML 存在隐式映射关系；玩家可见英吉斯温目标固定 `210050000`，`210130000` 仅保留为镜像服地图定义/归一兜底 | [patterns/static-data-jaxb.md](patterns/static-data-jaxb.md) |
| **核心架构与运行时** | `AR-001`, `AR-002`, `AR-003`, `AR-004`, `AR-005`, `AR-006`, `AR-007`, `AR-008`, `AR-009`, `AR-010`, `AR-011`, `AR-012`, `AR-013` | 移除 SPI 机制改为显式注入 Provider；高频服务门面热路径严禁裸调 `getIfAvailable` 查容器；传 NIO 读缓冲给 Netty 前必须收敛 limit；进程内直连必须复刻 initialized/onDisconnect、writeData 后禁止再 flip 且载荷须小端、跨服务交付必须切到接收方 ServiceContext；KnownList 遍历必须保留快照语义（闸门测试守护）；`IntObjectHashMap` 等「假原始类型容器」在热路径会装箱，实测占游戏内分配 74%；`Map.of`/`Map.copyOf` 结果的 `entrySet()` 迭代每条目新建 KeyValueHolder，实例级广播不得物化整表（改用 `doOnAllNpcs` 快照访问者）；XML 注入的模板字符串禁止在热路径重复 `split`/正则解析（首次使用解析缓存）；几何预剪枝只取标量 t 区间并用线程本地/池化 scratch，切勿为一次判定构造结果集合；实例级容器懒物化：占位符变更语义不一致（map 的 put/remove(k,v) 会抛、remove(k)/clear 安全）、并发首写必须收敛同一实例、锁与表一起懒物化时先发布锁、物化后容器类型保持不变；全局配置来源只由单例 Bean 在上下文装配期发布一次，EnvironmentPostProcessor 禁止写全局状态，Bean 构造期严禁读静态配置字段（`Config.load()` 在 ApplicationRunner 相位）；遗留配置镜像的原始键严禁跨服务同名不同值——登录服的 `database.url` 曾因此覆盖游戏服连库目标，冲突键必须退出镜像并由各服务文件自行决定；本地化日志的参数契约：异常必须作为日志调用的最后一个参数（`I18n.get` 只收模板占位符能消费的参数），传参个数必须等于模板占位符最大序号 + 1 且两套语言包一致 | [patterns/architecture-runtime.md](patterns/architecture-runtime.md) |
| **构建、环境与工具** | `ENV-001`, `ENV-002`, `ENV-003`, `ENV-004`, `ENV-005` | IDEA/打包运行产物边界；Lombok 同名同参重载陷阱；standalone javac 不可信；批量文本编辑需保护代码结构；配置/数据目录来源只看 `aion.home`——IDE（未设置）取 `src/main/resources/aion/**`，打包（`-Daion.home=<部署目录>`）配置固定 `<aion.home>/config` | [patterns/build-and-env.md](patterns/build-and-env.md) |
| **AI 选型与 NPC 移动** | `AIM-001`, `AIM-002`, `AIM-003`, `AIM-004`, `AIM-005`, `AIM-006`, `AIM-007` | Retail Pattern 会覆盖脚本跟随 AI；跟随停步半径产消两侧必须同源；非凸几何下用足迹队列 + 拉回兜底；不可移动 NPC 不因“够不着”放弃目标（真端数据无该驱动，放弃会与受击重新仇恨形成脱战抖动）；受击回调内严禁同步重排攻击（会顺着 attackTarget 回打调用方形成无限递归，必须线程池异步 + 按 objectId 去重）；真端直接交互模式（on_talked_by_user 的 use_skill / teleport_target(_alias)）与 gauge 模式不得下发默认 HTML 对话，空规则 retail pattern 不得覆盖 useitem 回退；阈值变身必须与死亡路径共用同一个一次性生成闸门（受击回调读到的是本次伤害前的 HP，一击/爆发致死会跳过阈值分支） | [patterns/ai-movement.md](patterns/ai-movement.md) |

---

## 三、知识沉淀与追加准则 (How to Append)

当完成新任务或解决新 Bug 时，按 [README.md](README.md) 的信息源边界和 Pattern schema 判断：
1. 先在 `.agents/summary/<topic>/` 保留单次证据。
2. 形成可复用根因时，追加到对应卡片的 Pattern 元数据块并分配 Pattern ID。
3. 只有形成跨域新不变量时，才更新本文件的一行路由摘要。
4. 运行 `sync_memory_bank.py` 更新症状索引，再运行 `check_memory_bank.py`。
5. 已关闭且不再参与日常排查的历史专项，按 [archive/](archive/) 规则归档。
