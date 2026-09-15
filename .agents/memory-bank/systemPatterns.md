# System Patterns & Router (系统模式与知识路由器)

本文档是 AionEmu 的**全局排查与模式顶级索引**。所有 Agent 在排查问题或修改核心代码前，先通过本索引快速定位对应业务领域的避坑指南。

> role: router-only
> status: ACTIVE
> last_reviewed: 2026-09-14
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
| **任务系统 (Quest Engine)** | `QE-001`, `QE-002`, `QE-003`, `QE-004`, `QE-005`, `QE-006`, `QE-007`, `QE-008`, `QE-009`, `QE-010`, `QE-011`, `QE-012`, `QE-013`, `QE-014` | target 投影会覆盖 action 变量自环计数；多 NPC 连续汇报严禁删中间 var 节点；终态自动清理 work-items；分支交付物在领奖阶段必须用 `count="ALL"` 避免阻断；无任务上下文的 NPC 选择保持普通对话；欧比斯准入只允许阵营任务完成态；工作物品可回收性以 quest_data.xml 声明为准；客户端 SECTION 计数必须使用固定 6-bit 位段；影片 self-loop 必须下发后续页；同 NPC 同阶段严禁无优先级动作重叠与计数节点投影重合 | [patterns/quest-engine.md](patterns/quest-engine.md) |
| **副本与运行时 (Instance & Runtime)** | `IR-001`, `IR-002`, `IR-003`, `IR-004`, `IR-005`, `IR-006`, `IR-007` | 副本特殊掉落注册时按物品 ID 去重；特殊属性绕过错误的 short 上限；GM 命令必须同时检查类扫描与有效运行时配置；巡逻编队按组锚点距离分组；事件日志与客户端崩溃不能仅凭时间相邻归因；重叠 NPC 先证明加载归属再删除；spot 身份判定禁用含 z 的坐标哈希，改用块级历史取证 | [patterns/instance-runtime.md](patterns/instance-runtime.md) |
| **静态数据与 JAXB** | `SDJ-001`, `SDJ-002`, `SDJ-003` | JAXB 实体实例字段严禁声明为 `final`；动态反射包树与技能/NPC XML 存在隐式映射关系；玩家可见英吉斯温目标固定 `210050000`，`210130000` 仅保留为镜像服地图定义/归一兜底 | [patterns/static-data-jaxb.md](patterns/static-data-jaxb.md) |
| **核心架构与运行时** | `AR-001`, `AR-002`, `AR-003`, `AR-004`, `AR-005` | 移除 SPI 机制改为显式注入 Provider；高频服务门面热路径严禁裸调 `getIfAvailable` 查容器；传 NIO 读缓冲给 Netty 前必须收敛 limit；进程内直连必须复刻 initialized/onDisconnect、writeData 后禁止再 flip 且载荷须小端、跨服务交付必须切到接收方 ServiceContext；KnownList 遍历必须保留快照语义（闸门测试守护） | [patterns/architecture-runtime.md](patterns/architecture-runtime.md) |
| **构建、环境与工具** | `ENV-001`, `ENV-002`, `ENV-003`, `ENV-004` | IDEA/打包运行产物边界；Lombok 同名同参重载陷阱；standalone javac 不可信；批量文本编辑需保护代码结构 | [patterns/build-and-env.md](patterns/build-and-env.md) |
| **AI 选型与 NPC 移动** | `AIM-001`, `AIM-002`, `AIM-003` | Retail Pattern 会覆盖脚本跟随 AI；跟随停步半径产消两侧必须同源；非凸几何下用足迹队列 + 拉回兜底 | [patterns/ai-movement.md](patterns/ai-movement.md) |

---

## 三、知识沉淀与追加准则 (How to Append)

当完成新任务或解决新 Bug 时，按 [README.md](README.md) 的信息源边界和 Pattern schema 判断：
1. 先在 `.agents/summary/<topic>/` 保留单次证据。
2. 形成可复用根因时，追加到对应卡片的 Pattern 元数据块并分配 Pattern ID。
3. 只有形成跨域新不变量时，才更新本文件的一行路由摘要。
4. 运行 `sync_memory_bank.py` 更新症状索引，再运行 `check_memory_bank.py`。
5. 已关闭且不再参与日常排查的历史专项，按 [archive/](archive/) 规则归档。
