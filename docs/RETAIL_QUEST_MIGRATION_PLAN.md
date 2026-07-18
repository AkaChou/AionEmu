# 真端 Quest 数据同步技术方案

> 状态：阶段 0/1、共有 Quest 的 `cannot_share` / `can_report` 同步、`can_report` 运行能力、首批 18 个 Quest 静态发布、第二批 6 个要塞自动领奖 Quest、第三批 10 个普通生产 Quest、第四批 `2585`、第五批 `21224`、C batch 01 `1871/2871`、C batch 02 `15098/25099`、C batch 03 `14210/24210`、C batch 04 `1867/1868/1869/2868` 运行闭包、护送/保护/动态生成 AI 审计，以及 `41600-41614`、`80315/80321` 隔离终审均已完成（2026-07-19）。当前已发布 46 个 Quest，17 个 Quest 已有明确隔离结论，AI 生产阻断为 0。本文方案仅涉及服务端 Quest 模板和服务端行为同步，不修改、生成或重新打包客户端 Quest.pak；真实入口与实际运行验收状态单独列出。

## 1. 结论

从 `/Users/mc/IdeaProjects/58Server/Map/XML/quest.xml` 同步真端 Quest，并继续使用当前 `quest_data.xml` / `QuestTemplate` 格式，技术上可行。

但可行的方案不是直接复制或覆盖 XML，而是把真端数据作为离线输入，经过确定性转换后输出当前格式：

```text
58Server 真端 Quest 模板与行为 XML
        │
        ├── 字段归一、名称转 ID、版本过滤、冲突分类
        ├── QuestTemplate 输出
        ├── quest_script_data / Java handler 行为闭包
        ├── 护送、保护、动态生成 AI 事件闭包审计
        └── 固定客户端兼容性报告（只读，不生成客户端产物）
        │
        ▼
AionEmu 当前 Quest 格式和现有 Quest 引擎
```

核心判断如下：

- 实际客户端以启动参数 `-cc:5 -lang:chs` 运行，因此权威模板是基础 `data/Quest/Quest.pak` 叠加 `data/China/Quest/quest.pak`，不是工作区中的其他地区 XML，也不是 `/Users/mc/IdeaProjects/5.8静态文件/XMLdata/China/quest.xml`。
- 权威客户端、基础客户端和 58Server 都包含同一组 10,035 个 Quest ID。迁移前服务端有 6,424 个，双方共有 6,410 个，客户端独有 3,625 个，另有 14 个服务器扩展 Quest；前五批与四个 C 类批次共发布 46 个后服务端为 6,470 个，双方共有 6,456 个，客户端剩余独有 3,579 个，服务器扩展仍为 14 个。
- 客户端独有 3,625 个 Quest 中，只有 219 个最低等级不高于 75；其中 86 个是 58Server `dev_name` 明确含“테스트”的测试任务，得到 133 个一级候选。其余 3,406 个被权威客户端明确设置为最低等级 999，应后置隔离而不是当作当前缺失内容批量补齐。
- 133 个一级候选按当前 handler 和可证明的行为转换规则分为 A=15、B=38、C=38、D=42；其中另有 39 个存在 `TEST_*` 行为引用、`DataDriven Empty` 等强测试/开发证据，应叠加 E 隔离。当前生产审计池上限为 94 个，分布为 A=15、B=36、C=12、D=31。
- 94 个生产审计 Quest 初始有 39 个含当前服务端未建模模板语义；`can_report` 完成后有 33 个命中其他未建模字段。`bm_restrict_category` 已证明为账号 BM/计费权限位，不加入 `QuestTemplate`，按当前部署“不实施 BM 账号包限制”的既有行为登记为 `compatible_noop`。第三批据此闭包 10 个 Quest，第四批闭包 `2585`，第五批闭包 `21224`；最后 15 个生产 A/B Quest `41600-41614` 已完成地图级终审并转为明确隔离，当前生产 A/B 开发队列为 0。隔离的 `9703` 可单独用于数据驱动转换器测试，但不能混入生产发布集。
- 133 个候选没有 `%Quest_*` 动态奖励宏，133/133 `nameId`、255 次模板物品引用和 67 次模板掉落 NPC 引用均可解析，也不涉及缺失的 faction ID。名称解析和动态宏不是当前首要堵点。
- 39 个疑似测试/开发任务已按“不猜值、可疑即隔离”规则叠加 E 隔离。第二批 6 个、第三批 10 个、第四批 `2585`、第五批 `21224` 及四个 C 批次的模板、行为、入口/目标 spawn 已闭包，不再属于堵点；`41600-41614` 已因固定客户端缺少 `Levels/LDF4b` 可加载关卡资产、服务端缺少 `600030000` world map/GEO/spawn 而正式隔离。`80315/80321` 的物品接取行为可复用现有模型，但四个活动 NPC 没有服务端 spawn，真端静态 Worlds 也无权威坐标，现已正式隔离。当前真正耗时点转为 31 个 D 类缺失行为、已发布 Quest 的真实运行验收和 `50019/51019/80341` 的入口取证。
- 首批 18 个 Quest 已生成并发布模板，其中 13 个通用行为已新增，5 个复用或修正现有 XML/Java handler。该批模板字段和行为引用已经闭包，但 `41615-41622`、`50019/51019`、`80341` 的真实接取入口仍有地图或动态出生阻塞，因此只能称为“静态发布完成”，不能称为“端到端迁移完成”。
- 双方共有 Quest 曾有 1,009 个 `cannot_share` 差异，现已全部按固定客户端同步；当前待同步为 0。模拟器仍显示灰色按钮时，应继续检查客户端自身限制和玩家组队/任务状态，不能再归因于这 1,009 个服务端字段漂移。
- 固定客户端共有 217 个 `can_report=true` Quest；当前服务端已存在其中 188 个，全部与客户端一致，待同步为 0。服务端支持客户端动作 `108` 和 `110-124` 的无 NPC 领奖协议；第二批 6 个要塞任务通过现有 `monster_hunt reward="true"` 在击杀后进入 `REWARD`，无需新增 Quest handler。
- AI 与 Quest 按事件链结合，不把 NPC AI 模板直接合并进 Quest。当前 18 个使用 `defaultStartFollowEvent` 的护送 handler 为 18/18 `closed`；保护类保留 1 条 `no_evidence` 信息状态；真端 57 条动态生成动作中生产范围 46 条全部 `closed`、等级 999 范围 11 条为 `isolated`；第二批要塞自动领奖击杀链为 `siege_report_hunt closed=6`，第三批 10/10、第四批 1/1、第五批 1/1、前三个 C 批次各 2/2、C batch 04 为 6/6 Quest 运行闭包 `closed`，生产阻断为 0。C batch 04 只含 EnterArea/PVP/Talk/Hunt，不涉及护送、保护或动态生成生命周期。
- 服务端完整补全必须同时处理 Quest 模板、服务端 Quest 行为、接取/交付 NPC 的实际出生路径。固定客户端缺少地图、怪物标记或寻路数据时只记录兼容限制，不通过修改客户端修复。

因此，建议采用“离线转换器 + 差异报告 + 分批发布”方案，保持现有运行时模型和 Quest 引擎不变。

## 2. 目标与非目标

### 2.1 目标

- 以 58Server 真端 XML 为迁移输入，生成当前 XSD 和 JAXB 模型可读取的 Quest 数据。
- 先修正双方共有的 6,410 个 Quest，再分级补全真端独有任务。
- 对真端 221 个物理字段建立完整、可审计的处理规则，禁止静默丢字段。
- 对物品、NPC、任务、称号、配方和阵营执行服务端联合外键校验。
- 保留当前服务器独有的 14 个 Quest，并将其明确标记为 server extension。
- 保证相同输入、相同转换器版本产生字节稳定或语义稳定的相同输出。
- 让任务分享、限制条件、奖励、掉落和服务端行为使用同一批版本数据。
- 为护送、保护和动态生成类 Quest 生成可重复的 AI 事件闭包报告，并作为发布门禁。
- 真端 XML、当前服务端和转换结果发生异常或冲突时，以固定 5.8 客户端数据为最终语义基准。

### 2.2 非目标

- 不重写现有 Quest 引擎。
- 不把真端 221 个字段全部直接加入 `QuestTemplate`。
- 不把 10,035 个真端 Quest 一次性覆盖到当前数据。
- 不根据相似名称猜测 NPC、物品、任务或配方 ID。
- 不把只有模板、没有服务端行为闭包的 Quest 标记为“已完成迁移”。
- 不修改、生成、重新打包或发布客户端 Quest.pak。
- 不修改玩家任务数据库，也不为通过验收而猜测 NPC、物品、Quest、世界或出生坐标。

## 3. 当前系统边界

当前 Quest 由三个彼此独立但必须一致的数据层组成。

### 3.1 Quest 模板层

主要文件和模型：

- `src/main/resources/aion/data/static_data/quest_data/quest_data.xml`
- `src/main/resources/aion/data/static_data/quest_data/quest_data.xsd`
- `src/main/java/com/aionemu/gameserver/model/templates/QuestTemplate.java`
- `DataManager.QUEST_DATA`

该层定义任务是否可接、等级和职业限制、是否可分享、奖励、掉落、前置条件、重复次数等静态属性。

### 3.2 服务端行为层

主要实现：

- `src/main/resources/aion/data/static_data/quest_script_data/*.xml`
- `src/main/resources/aion/data/static_data/quest_script_data/quest_script_data.xsd`
- `src/main/java/com/aionemu/gameserver/questEngine/handlers/**`

该层负责接取、对话、击杀、收集、使用物品、变量推进、完成和领奖。Quest 模板存在不代表行为已经存在。

### 3.3 Quest 与 AI 的结合边界

Quest 与 AI 的关系是事件级闭包，不是把 `NpcTemplate.ai` 字段复制进 Quest：

```text
Quest handler 启动事件
  -> NPC AI 执行动作或跟随
  -> QuestEngine 接收到达/丢失/击杀/交互事件
  -> handler 推进成功或失败状态
  -> 取消任务、停止 AI、删除或超时回收动态对象
```

普通对话、攻击、击杀由 NPC controller 进入 `QuestEngine`；护送由 `FOLLOW_ME` 启动并通过 `onNpcReachTargetEvent` / `onNpcLostTargetEvent` 回传；动态生成必须同时证明触发基准、生成目标、数量、生命周期和后续 Quest 状态。仅有 NPC AI 名称、仅有 `spawn` 调用或仅有 Quest 模板都不构成闭包。当前 `NpcTemplate.getAi()` 的其他兼容修改不属于本迁移审计输入。

### 3.4 固定客户端兼容边界

客户端不属于本方案的修改目标。实际启动脚本使用 `-cc:5 -lang:chs`，所以客户端权威视图按以下顺序合并：

```text
data/Quest/Quest.pak
  -> data/China/Quest/quest.pak 覆盖同名 Quest 字段
```

该合并结果作为固定、只读的最终语义基准，因为它仍会影响：

- Quest 基础显示和限制数据；
- 简单对话、击杀、收集、物品使用和串行击杀数据；
- 怪物头顶 Quest 标记；
- Quest 目标定位、地图查找和寻路提示。

客户端数据缺失时，服务端 Quest 即使能够推进，也可能没有正确 UI、怪物标记或定位信息。本方案只报告这类限制；需要修改客户端才能解决的问题不进入服务端实现范围。

## 4. 数据源与版本冻结

### 4.1 本轮审计输入

| 用途 | 路径 | 编码或格式 |
|---|---|---|
| 真端 Quest 模板 | `/Users/mc/IdeaProjects/58Server/Map/XML/quest.xml` | UTF-16 XML |
| 真端简单对话 | `/Users/mc/IdeaProjects/58Server/Map/XML/Quest_SimpleTalk.xml` | UTF-16 XML |
| 真端简单击杀 | `/Users/mc/IdeaProjects/58Server/Map/XML/Quest_SimpleHunt.xml` | UTF-16 XML |
| 真端简单收集 | `/Users/mc/IdeaProjects/58Server/Map/XML/Quest_SimpleCollectItem.xml` | UTF-16 XML |
| 真端物品使用 | `/Users/mc/IdeaProjects/58Server/Map/XML/Quest_SimpleUseItem.xml` | UTF-16 XML |
| 真端物品演出 | `/Users/mc/IdeaProjects/58Server/Map/XML/Quest_SimpleItemPlay.xml` | UTF-16 XML |
| 真端串行击杀 | `/Users/mc/IdeaProjects/58Server/Map/XML/Quest_SimpleSerialHunt.xml` | UTF-16 XML |
| 真端制作任务 | `/Users/mc/IdeaProjects/58Server/Map/XML/Quest_CombineTask.xml` | UTF-16 XML |
| 真端数据驱动行为 | `/Users/mc/IdeaProjects/58Server/Map/XML/data_driven_quest.xml` | UTF-16 XML |
| 真端挑战任务 | `/Users/mc/IdeaProjects/58Server/Map/XML/challenge_task.xml` | UTF-16 XML |
| 当前 Quest 模板 | `src/main/resources/aion/data/static_data/quest_data/quest_data.xml` | UTF-8 XML |
| 当前行为 Schema | `src/main/resources/aion/data/static_data/quest_script_data/quest_script_data.xsd` | UTF-8 XSD |
| 当前挑战任务 | `src/main/resources/aion/data/static_data/quest_data/challenge_tasks.xml` | UTF-8 XML |
| 当前制作任务 | `src/main/resources/aion/data/static_data/quest_script_data/work_order.xml` | UTF-8 XML |
| 固定客户端启动参数 | `/Users/mc/IdeaProjects/5.8客户端/单机启动.bat` | `-cc:5 -lang:chs`，只读 |
| 固定客户端基础 Quest | `/Users/mc/IdeaProjects/5.8客户端/data/Quest/Quest.pak` | Aion PAK，只读 |
| 固定客户端 China 覆盖 | `/Users/mc/IdeaProjects/5.8客户端/data/China/Quest/quest.pak` | Aion PAK，只读 |

### 4.2 当前快照摘要

| 文件 | SHA-256 |
|---|---|
| 固定客户端 `单机启动.bat` | `9ebe66649cedf2692982fc77e8044d618465ff1789b12b7371b58cb11d015837` |
| 固定客户端基础 `Quest.pak` | `456eb4625b55215c2a03ba569424f765b0ef27be88b30b7c67ceddf05ced11bb` |
| 固定客户端 China `quest.pak` | `d1440f1c31ceb144dfa5ab8f75b4362d9d677786a1030874461a86bac7e324fe` |
| 真端 `quest.xml` | `4d8c86074af7c7ae4cbe9e4d881a4715539b78b1745575fbaef3832163a0cb6b` |
| 真端 `data_driven_quest.xml` | `f74cecb5a8792b1c19f41ddb1beb3d2ec3911a7d1be9ec92c52e5bfe18f5edb0` |
| 第二批发布后当前 `quest_data.xml` | `665199a2616e52b4ee6f344f69606128418324957520d85ff978687c7a976990` |
| 第三批发布后当前 `quest_data.xml` | `0ee5d662f7b2f6ab0c9ec3a6e1fa73d0f66da639d62efa32d0476ace3554b9dd` |
| 第四批发布后当前 `quest_data.xml` | `9f15828e088fbf582bbbffeb2b1e250423bfc0f3fc2307e82da7d557d8a33175` |
| 第五批发布后当前 `quest_data.xml` | `77b9f8a141bd86b6900a91e1941bc1fd7fe7475fecdefa0b7b56badb95db7b93` |
| C batch 01 发布后当前 `quest_data.xml` | `57952af876886fed346dd003bc802cc141c8a91fc82fb0abbb16940f5d772f63` |
| C batch 02 发布后当前 `quest_data.xml` | `d8c267726a2f182c1a243c7235a272701b3b6141d015047246b9eebf442d48c5` |
| C batch 03 发布后当前 `quest_data.xml` | `a77295d18fc0b0c1358568ef9782d0943ed6858250c527ba7b7d32956d95b3fd` |
| C batch 04 发布后当前 `quest_data.xml` | `2e924d0720f254ace51b0f500d9ea2e0484fec8cbfdf1f5f74d8edf3ea18e990` |
| 当前 `quest_data.xsd` | `8b654bd4e2e5705780b43ad2cdf611abaaf0fabc907efd8176595f4d85509e4b` |
| 当前 `quest_script_data.xsd` | `f4772168f81989251c3fa4efd59701b263ff5ab36a4d1dda4d7a844eb8f2a484` |

实施时必须为全部输入生成一份 manifest，包含路径、大小、SHA-256、编码、客户端启动参数和转换器版本。启动参数、基础 PAK、China 覆盖 PAK或服务端输入任一摘要变化后，旧差异报告和生成结果全部失效，禁止增量复用。

本轮已只读解包基础和 China Quest PAK 完成统计，没有修改客户端文件。临时解包目录不是实施输入，正式转换必须从上述已冻结 PAK 重新生成只读审计视图。固定客户端 Nightmare Circus `Level.pak` 也只解包到 `/Users/mc/PycharmProjects/aion_drop/target/quest-migration/client-level-audit/` 用于出生证据核对，没有回写或重新打包客户端。

### 4.3 数据权威优先级

发生字段冲突、版本异常或行为含义不一致时，按以下顺序裁决：

1. 固定 5.8 客户端数据：最终语义基准，只读，不修改。
2. 58Server 真端 Quest 模板和行为 XML：客户端没有表达的服务端执行细节来源。
3. 当前 AionEmu Quest 模板和 handler：用于发现已有兼容修复。
4. 人工兼容补丁：仅在前三项仍无法直接表达时使用，并保留证据。

具体规则：

- 客户端明确存在某个字段或目标关系时，转换结果必须与客户端一致。
- 客户端与 58Server XML 冲突时采用客户端值，并把差异写入报告。
- 客户端没有表达某个服务端字段，不等于该字段为 `false` 或 `0`；此时回退到 58Server 服务端数据。
- 掉落执行、spawn、handler 内部状态等纯服务端细节以真端服务端数据为主，但不得与客户端可见流程矛盾。
- 客户端证据缺失且不同服务端来源冲突时，对应 Quest 保持隔离，不通过相似名称或经验猜值。
- 权威客户端必须是当前实际使用的固定 5.8 客户端，不能拿其他地区或其他版本客户端覆盖。

### 4.4 客户端覆盖与 58Server 差异

基础客户端和 China 覆盖后的 Quest ID 集完全相同，都是 10,035 个。China 覆盖只改变 120 个 Quest，涉及奖励、等级、周期和少量条件字段：

| China 相对基础客户端的差异字段 | Quest 数 |
|---|---:|
| `reward_item1_1` | 57 |
| `reward_item1_2` | 51 |
| `reward_exp1` | 50 |
| `client_level` | 19 |
| `minlevel_permitted` | 19 |
| `quest_repeat_cycle` | 6 |
| `reward_item1_3` | 6 |
| `check_item1_1` | 4 |
| 其他单项条件或奖励字段 | 2 或更少 |

China 权威客户端与 58Server 在排除客户端全空的 `dev_name` 后，仍有 4,773 个 Quest 存在字段差异。主要集中在：

| 客户端与 58Server 差异字段 | Quest 数 |
|---|---:|
| `minlevel_permitted` | 3,737 |
| `client_level` | 2,065 |
| `maxlevel_permitted` | 1,639 |
| `reward_item1_1` | 57 |
| `reward_item1_2` | 51 |
| `reward_exp1` | 50 |

因此 58Server 仍是服务端行为和客户端未表达字段的来源，但不能再用它的等级字段直接筛选当前客户端候选。客户端 `dev_name` 全空，只能把 58Server `dev_name` 用作测试标记辅助，不能作为语义比较字段。

## 5. 数据覆盖基线

### 5.1 Quest ID 覆盖

迁移前候选基线与发布后运行数据基线必须分开使用，不能把已发布的 46 个 Quest 再次统计为候选。

| 项目 | 迁移前 | 首批发布后 | 第二批发布后 | 第三批发布后 | 第四批发布后 | 第五批发布后 | C batch 01 发布后 | C batch 02 发布后 | C batch 03 发布后 | C batch 04 发布后 |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| 固定客户端/真端 Quest | 10,035 | 10,035 | 10,035 | 10,035 | 10,035 | 10,035 | 10,035 | 10,035 | 10,035 | 10,035 |
| 当前 Quest | 6,424 | 6,442 | 6,448 | 6,458 | 6,459 | 6,460 | 6,462 | 6,464 | 6,466 | 6,470 |
| 双方共有 | 6,410 | 6,428 | 6,434 | 6,444 | 6,445 | 6,446 | 6,448 | 6,450 | 6,452 | 6,456 |
| 客户端独有 | 3,625 | 3,607 | 3,601 | 3,591 | 3,590 | 3,589 | 3,587 | 3,585 | 3,583 | 3,579 |
| 当前独有 | 14 | 14 | 14 | 14 | 14 | 14 | 14 | 14 | 14 | 14 |
| 最低等级不高于 75 | 219 | 201 | 195 | 185 | 184 | 183 | 181 | 179 | 177 | 173 |
| 一级候选 | 133 | 115 | 109 | 99 | 98 | 97 | 95 | 93 | 91 | 87 |
| 理论并集 | 10,049 | 10,049 | 10,049 | 10,049 | 10,049 | 10,049 | 10,049 | 10,049 | 10,049 | 10,049 |

当前独有 Quest 必须保留：

```text
50110, 50111, 50115, 50116, 50119, 50120, 50121,
50123, 50124, 51110, 51111, 51115, 51116, 51119
```

它们不能因“真端没有”而被删除，也不能伪装成真端记录；应作为独立 server extension 覆盖层参与最终合并。

### 5.2 真端独有任务过滤

| 过滤结果 | 数量 |
|---|---:|
| 权威客户端独有总数 | 3,625 |
| 客户端最低等级不高于 75 | 219 |
| 其中明确测试任务 | 86 |
| 75 级内排除字面测试标记后的一级候选 | 133 |
| 其中具有强测试/开发证据、叠加 E 隔离 | 39 |
| 当前生产审计池上限 | 94 |
| 客户端最低等级为 999 | 3,406 |
| 全部明确测试任务 | 120 |
| 有真端行为 XML | 2,479 |
| 无通用行为记录 | 1,146 |

第一层测试过滤通过 58Server `dev_name` 明确包含“테스트”识别；客户端 `dev_name` 本身为空。第二层再根据 `TEST_*` NPC/物品引用、`DataDriven Empty` 等强证据叠加 E 隔离，不把可疑任务伪装成生产内容。3,406 个最低等级 999 的 Quest 是客户端明确后置的内容，不是当前同步堵点，也不直接生成到正式运行数据。

`无通用行为记录=1,146` 是对 58Server 行为文件的原始覆盖统计。其中 5 个 Quest 已由当前 AionEmu handler 覆盖，因此采用 A 类优先的互斥分级后，D 类为 1,141 个。

### 5.3 工程分级

| 等级 | 全部缺失任务 | 133 个一级候选 | 94 个生产审计池 | 定义 |
|---|---:|---:|---:|---|
| A | 83 | 15 | 15 | 当前已有 XML 或 Java handler，只缺 `QuestTemplate`；A 优先于其他分类 |
| B | 1,631 | 38 | 36 | 真端行为可按已证明的严格规则映射到当前通用模板 |
| C | 770 | 38 | 12 | 有行为来源，但含复杂状态、未支持操作、未解析行为引用或不满足严格规则 |
| D | 1,141 | 42 | 31 | 当前无 handler，且没有可用真端通用行为记录 |
| 合计 | 3,625 | 133 | 94 | A 至 D 为互斥分类；E 可叠加 |

另设发布状态 E“隔离”，覆盖等级 999、测试、过期活动或版本不明的任务。E 是发布过滤状态，可以叠加在 A 至 D 上，不参与上表加总。

一级候选中的 A/B 共 53 个只是“行为侧有路径”，不是可直接发布数量。叠加原有 E 隔离后，生产池 A/B 为 51 个：首批发布 18 个，第二批发布 6 个，第三批发布 10 个，第四批发布 1 个，第五批发布 1 个，最后 15 个 `41600-41614` 经 LDF4b 终审转为地图级隔离。第二至五批已通过固定客户端行为、真端入口/出生证据和当前运行资源审计；生产 A/B 不再有未决开发项。C、D 类不应为了凑数量而自动降级生成。

### 5.4 堵点收敛与当前状态

本轮已经完成权威源、候选范围、测试隔离、名称解析、客户端目标覆盖、转换器实现、首批静态发布、第二批要塞自动领奖闭包、第三批普通生产 Quest、第四批 `2585` 前置兼容和第五批 `21224` 末次扩展奖励闭包。首批“静态发布完成”仍只表示模板/行为文件已加载并通过结构校验；第二至五批已额外闭包自然接取入口、目标、状态推进和运行所需 spawn，仍需在真实在线角色上做最终运行验收。

| 堵点 | 当前状态 | 结论 | 是否阻断当前发布 |
|---|---|---|---|
| 实际客户端及覆盖顺序不明 | 已关闭 | 已冻结 `-cc:5 -lang:chs`、基础 PAK、China 覆盖 PAK 及摘要 | 否 |
| 缺失 Quest 范围过大 | 已关闭 | 3,625 个缺失 ID 收敛为 133 个一级候选，3,406 个等级 999 Quest 后置 | 否 |
| 测试/开发任务混入生产 | 已隔离 | 39 个 Quest 叠加 E 隔离；没有用途证据前不进入生产发布集 | 否 |
| 名称、物品、掉落 NPC、动态宏、faction | 当前批次已关闭 | 133 个候选的相关引用均可解析，宏和缺失 faction 对候选影响为零 | 否 |
| 共有 Quest `cannot_share` | 已关闭 | 已按固定客户端修正 1,009 个，当前待同步 0 | 否 |
| `can_report` 模板与领奖协议 | 已关闭 | XSD/JAXB 已映射；共有 Quest 修正 182 个；动作 `108`、`110-124` 已支持 | 否 |
| `bm_restrict_category` | 已关闭 | 固定客户端与真端均为 3,477 个、值恒为 `1`；真端仅按 BM 权限位校验，当前服务端无对应账号包模型，登记为 `compatible_noop` | 否 |
| `reward_repeat_count` | 当前生产堵点已关闭 | 固定客户端 265 个非空值中，221 个满足“有限 `max_repeat_count` 相等、存在扩展奖励、不可周期重复”，可复用现有末次 `extended_rewards`；其余 44 个仍需独立周期语义。`21224` 属于前者并已发布 | 否；不满足兼容条件的 Quest 继续隔离 |
| 6 个 `can_report` 候选发布 | 已关闭 | 固定客户端证明 Talk 接取和单目标 Hunt；现有 `monster_hunt reward="true"` 完成 `START -> REWARD`；要塞 `PEACE` 阵营入口和三张副本首领出生已补齐 | 否，第二批已发布 |
| 第三批 10 个生产 Quest | 已关闭 | 10 个模板、2 个新击杀行为、8 个现有行为以及全部前置/入口/交付/掉落/击杀 NPC spawn 均已闭包 | 否，第三批已发布 |
| `2585` 前置分支 | 已关闭 | 客户端 `2055 OR 4542`；`2055` 为等级 999 且无行为，按现有 `2586-2588` 兼容模式只发布有效 `4542` 分支 | 否，第四批已发布 |
| `21224` 末次扩展奖励 | 已关闭 | `reward_repeat_count=max_repeat_count=10`；第 1-9 次只发普通奖励，第 10 次追加扩展二选一奖励并达到上限，第 11 次不可再接 | 否，第五批已发布 |
| C batch 01 `1871/2871` | 已关闭 | 固定客户端与真端均为相同三段顺序 Talk；复用现有 `report_to_many`，模板、行为、10 个 NPC spawn、世界与 GEO 全部闭包 | 否，已发布 |
| C batch 02 `15098/25099` | 已关闭 | 固定客户端与真端均为同 NPC 接取/领奖且无进度节点；复用现有 `report_to`，模板、行为、NPC spawn、世界与 GEO 全部闭包 | 否，已发布 |
| C batch 03 `14210/24210` | 已关闭 | 真端为普通 Talk 接取/领奖；两个 `quest_ai_name` 分别展开为四个已验证战场变体，复用现有 `report_to`；8 个 NPC 模板/spawn 与 5 个 world/GEO 全部闭包 | 否，已发布 |
| C batch 04 `1867/1868/1869/2868` | 已关闭 | 固定客户端与真端的 PVP 数量、探索顺序和两组击杀目标完全一致；4 个新模板、2 个配对旧模板、4 个 Java handler、2 条 `monster_hunt`、8 个球形区域、NPC/怪物 spawn、世界与 GEO 全部闭包 | 否，已发布 |
| C 类活动物品任务 `80315/80321` | 已隔离 | `report_to_many start_item_id` 可表达行为，但 `831423/831424/831427/831428` 无服务端 spawn，真端静态 Worlds 无坐标证据 | 否，不发布、不猜坐标 |
| C 类复杂状态机 | 已关闭 | 最后四个生产 C 类已在 C batch 04 恢复；26 个疑似测试/开发 C 类和 `80315/80321` 继续按证据隔离 | 否 |
| D 类行为缺失 | 未关闭 | 生产池 31 个，需要其他真端脚本、反编译或实测证据 | 否，属于后续阶段 |
| 护送/保护/动态生成/要塞击杀 AI 闭包 | 已关闭当前生产阻断 | 护送 18/18、动态生成生产动作 46/46、要塞自动领奖击杀 6/6 为 `closed`；11 条等级 999 动作为 `isolated`；保护类未发现具体任务证据 | 否 |
| 固定客户端标记或定位证据缺失 | 服务端不可消除 | 记录兼容限制，不修改客户端，不伪装成服务端迁移失败 | 否，按 Quest 单独验收 |
| 首批模板和行为生成 | 已关闭 | 18 个模板、13 个通用行为已发布；首批未解析字段和引用均为 0 | 否 |
| `41600-41614` LDF4b 发布判定 | 已隔离 | 固定客户端权威世界 ID 为 `600030000`，有 WorldId/116 个 subzone 和周边资源，但没有 `Levels/LDF4b`、`Level.pak`、`PathFind.pak`；AionEmu 无启用地图、GEO 和入口 spawn | 否，不进入发布集；转入明确隔离 |
| `41615-41622` 真实入口 | 地图级隔离 | 行为入口为 `800328/800329`；与 `41600-41614` 受同一 `600030000/LDF4b` 关卡资产限制。模板和行为已静态发布，但不可标记为端到端完成 | 是，阻断这 8 个 Quest 的真实流程 |
| `50019/51019` 真实入口 | 证据阻塞 | 现有 `event.xml` 沿用 `202549/ShugoL`，但当前 spawn、真端 Worlds XML 和活动注入代码均未找到权威出生位置 | 是，阻断这 2 个 Quest 的自然接取 |
| `80341` 真实入口 | 证据阻塞 | 真端 `Quest_SimpleTalk.xml` 明确接取/交付 NPC 为 `831709/IDAsteria_IU_WORLD_IN_NPC01`；当前地图 `301200000` 可运行，但固定客户端场景只出现相邻的 start/teleport/ending/IU/invisible NPC，未出现同名 `IN_NPC01` 实体，不能借相似名字猜坐标 | 是，阻断自然接取与交付 |

因此 18 个首批 Quest 已具备静态发布条件并已发布，但只有接取 NPC 已存在且流程实测通过的 Quest 才能升级为“端到端迁移完成”。上述 11 个入口受阻 Quest 必须保持明确状态，不能用 GM 命令强制接取后的成功推进替代自然接取验收；其中 `41615-41622` 已按地图级隔离登记，不再等待服务端单方面“修复”。第二批 6 个、第三批 10 个、第四批 `2585` 和第五批 `21224` 不再命中这些入口堵点，其静态和运行资源闭包已完成。

LDF4b 终审已由 `/Users/mc/PycharmProjects/aion_drop/scripts/quest_migration_audit.py` 固化为 `target/quest-migration/reports/ldf4b_isolation.json`，当前 v14 仍保持该隔离基线。结论证据如下：

- 固定客户端 `data/world/world.pak` SHA-256 为 `6df7689e899505de9996f8a72de9d5b30b7ad13fd82f4f41b4b4e95b741e7e3f`；`WorldId.xml` 唯一登记 `600030000/LDF4b`，`client_world_ldf4b.xml` 含 116 个 subzone。
- 固定客户端保留加载图、UI 地图、天空盒、声音和对话资源，但不存在 `Levels/LDF4b`，因此不存在可加载的 `Level.pak` 和 `PathFind.pak`。准确结论是“保留世界元数据和周边资源，但缺少关卡资产”，不能再表述为“客户端完全没有 LDF4b 资产”。
- AionEmu 聊天枚举认识 `600030000`，但 `world_maps.xml` 未启用该地图，无 `600030000.geo.gz`，接取 NPC `205910/205912/205913/205965` 模板存在而 spawn 覆盖为 `0/4`。compact 派生数据中的 `600031000` 与固定客户端冲突，只作为历史漂移证据，不得覆盖客户端世界 ID。
- 固定客户端 Quest 模板和 `quest_monster.csv` 对 `41600-41614` 均为 `15/15` 覆盖，共 73 个目标名且全部唯一解析；真端 `Quest_SimpleHunt.xml` 为 `15/15`、53 个基础目标。客户端在 `41611` 增加 4 个目标、在 `41614` 增加 16 个目标，最终行为目标必须以客户端 73 个目标为准。
- `41600-41614` 不生成模板或行为，不启用服务端地图，不补造 GEO，不修改客户端；`41615-41622` 保留“静态发布、地图级隔离”状态。生产 A/B 队列因此终结；随后进入的 12 个生产 C 类 Quest 已完成四个发布批次，`80315/80321` 已隔离，生产 C 类开发队列为 0。

当前 133 个候选的完整分级如下：

| 类别 | Quest ID |
|---|---|
| A（15） | `2421, 2451, 2511, 2585, 2599, 2605, 2611, 2667, 21015, 21025, 21120, 21224, 50019, 51019, 80341` |
| B（38） | `1770, 2105, 2768, 9664, 9703, 13861, 13865, 13869, 23861, 23865, 23869, 41600-41622, 80585, 80586, 80590, 80591` |
| C（38） | `1867, 1868, 1869, 1871, 2868, 2871, 9639, 9640, 9641, 9642, 9643, 9646, 9647, 9648, 9651, 9653, 9654, 9655, 9656, 9657, 9658, 9659, 9660, 9665, 9666, 9667, 9668, 9669, 9670, 9671, 9672, 9801, 14210, 15098, 24210, 25099, 80315, 80321` |
| D（42） | `2010, 9510, 9615, 9652, 9673-9683, 10070-10073, 11295, 12999, 14080, 14081, 14090, 14091, 15097, 18412, 18604, 18744, 20015, 20070-20073, 24080, 24081, 24090, 24091, 28412, 28604, 28744, 30810` |

以下 39 个一级候选具有强测试/开发证据，必须叠加 E 隔离：

```text
9639-9643, 9646-9648, 9651, 9653-9660,
9664-9683, 9703, 9801
```

证据包括行为中直接引用 `TEST_*` NPC/物品、`DataDriven Empty` 命名或明确的测试数据形状。它们在 A/B/C/D 中分别占 0/2/26/11 个；除非获得产品用途或实际客户端流程证据，否则不得进入生产发布集。

已静态发布的首批生产验证集为以下 18 个 A/B Quest。它们没有未建模模板字段、动态宏、模板外键、faction 或测试证据堵点，但仍须按上表完成真实入口和实际流程验收：

```text
2421, 2451, 50019, 51019, 80341,
2105, 41615, 41616, 41617, 41618, 41619,
41620, 41621, 41622, 80585, 80586, 80590, 80591
```

首批接取入口审计结果：

| Quest | 接取 NPC | 当前出生状态 | 当前结论 |
|---|---|---|---|
| `2105` | `203502` | 当前 spawn 存在 | 未发现出生阻塞，待真实流程验收 |
| `2421` | `204309`，交付 `204187` | 当前 spawn 均存在 | 未发现出生阻塞，待真实流程验收 |
| `2451` | `204312` | 当前 spawn 存在 | 未发现出生阻塞，待真实流程验收 |
| `80585/80586/80590/80591` | `832267` | 当前 spawn 存在 | 未发现出生阻塞，待真实流程验收 |
| `41615-41622` | `800328/800329` | 当前 spawn 不存在，且地图 `600030000/LDF4b` 无可加载客户端关卡和服务端运行资源 | 地图级隔离 |
| `50019/51019` | `202549` | 当前 spawn 和活动注入证据均不存在 | 活动出生证据阻塞 |
| `80341` | `831709` | 当前 spawn 不存在；客户端同地图场景无同名实体 | 生成路径/坐标证据阻塞 |

第二批 6 个要塞自动领奖 Quest 已完成以下闭包：

| Quest | 阵营入口 NPC | 要塞/状态 | 击杀目标 | 副本地图 | 状态 |
|---|---:|---|---:|---:|---|
| `13861` | `268080` | `1221 / ELYOS / PEACE` | `233633` | `300140000` | `closed` |
| `13865` | `270165` | `1241 / ELYOS / PEACE` | `233719` | `300130000` | `closed` |
| `13869` | `269265` | `1231 / ELYOS / PEACE` | `233676` | `300120000` | `closed` |
| `23861` | `268081` | `1221 / ASMODIANS / PEACE` | `233633` | `300140000` | `closed` |
| `23865` | `270166` | `1241 / ASMODIANS / PEACE` | `233719` | `300130000` | `closed` |
| `23869` | `269266` | `1231 / ASMODIANS / PEACE` | `233676` | `300120000` | `closed` |

入口 NPC 不是普通静态 spawn，而是由 `400010000_Reshanta.xml` 根据 `siege_id`、`PEACE` 状态和当前要塞归属阵营注入。三张副本的首领出生和同 ID GEO 均存在；击杀行为统一复用 `monster_hunt reward="true"`，不新增 Java handler。离线报告把这 6 条链记为 `siege_report_hunt closed=6`。

第二批确定性产物摘要为：模板 `d93974ed004de25c242fa607be79cf5ddccb7dc71cd1eb19620c93527f8344d9`，行为 `91b0a73f426bd552ef4bf86962c8dcd86d5d9ac18ad7597b47822b32d28ac3b1`，报告 `531e44f8ecea8cfe8caffd2bf3fcb97dd8a61be8666e18ddb960374f62c080f4`。相同冻结输入连续运行两次摘要一致。

第三批 10 个 Quest 已完成模板、行为和运行资源闭包：

| Quest | 行为来源 | 必需 NPC 闭包 | 状态 |
|---|---|---|---|
| `1770` | `reshanta.xml/monster_hunt`，本批新增 | `278531, 256674` | `closed` |
| `2511` | `beluslan.xml/item_collecting`，复用现有 | `204711, 213914-213917` | `closed` |
| `2599` | `beluslan.xml/item_collecting`，复用现有 | `204725, 213029-213031` | `closed` |
| `2605` | `beluslan.xml/report_to`，复用现有 | `204732, 204826` | `closed` |
| `2611` | Java handler `_2611Consulting_The_Leaders`，复用现有 | `204763, 204783, 204784, 204700` | `closed` |
| `2667` | `beluslan.xml/report_to`，复用现有 | `204814, 204749` | `closed` |
| `2768` | `reshanta.xml/monster_hunt`，本批新增 | `278031, 256674` | `closed` |
| `21015` | `gelkmaros.xml/item_collecting`，复用现有 | `799246, 700721` | `closed` |
| `21025` | `gelkmaros.xml/item_collecting`，复用现有 | `799252, 700723` | `closed` |
| `21120` | `gelkmaros.xml/monster_hunt`，复用现有 | `799291, 216102, 216103` | `closed` |

第三批没有新增 Java 运行逻辑；只新增固定客户端模板和 `1770/2768` 两条现有通用类型行为。`2511` 的前置 `2528`、`2605` 的前置 `2513` 均已存在。迁移器逐 Quest 校验模板等价、前置引用、行为注册以及入口、交付、掉落和击杀 NPC spawn，结果为 `released=true`、`common_diff_rows=0`、未解析字段/引用 `0/0`、运行闭包 `10/10 closed`。

第三批确定性产物摘要为：模板 `37a8bc938458fc03d097097c6d148b41257b3b439ee908e1b77066db6da9c4f7`，行为 `5ced607efe08c1a5a1b7f6f54f5a3d76897fa13059944f6cada9715bd163a2cc`，报告 `67a4316a3fd2fde5edba9fc0c464d671e0a1db69e60c1688eb965a3918de2e9c`。`bm_restrict_category` 兼容报告摘要为 `c486b3c1064397bd95ad68ce37979a2b0b9c02aa2a42ff3b88a2ab30016b36a2`。

第四批只发布 `2585`。固定客户端原始前置为 `2055 OR 4542`，不是两个任务都必须完成：当前同组 `2586/2587/2588` 已按相同客户端数据只保留 `4542` 分支。`2055` 在固定客户端为 `minlevel=999`，当前没有模板，58Server 通用行为文件和当前 Java/XML handler 也均无可执行行为；同时当前 `QuestService` 会跳过 `minlevel=999` 的普通最低等级拒绝，若只加入模板反而可能产生可接但无法完成的任务。因此 `2055` 继续作为客户端停用分支隔离，禁止为解锁 `2585` 强行发布。

`2585` 复用现有 `beluslan.xml/item_collecting`，接取 NPC `204739`、交互对象 `700331` 和前置 `4542` 均存在。迁移器验收为 `released=true`、`common_diff_rows=0`、未解析字段/引用 `0/0`、运行闭包 `1/1 closed`。第四批模板摘要为 `b2bcf26880c382f796fd102e7bde1801e06d33c02d2b0226aa471a597f4bd0c6`，报告摘要为 `49b3301cb53efe2004286719f8661b2518f7f11c75833da85b10492647d937a5`。

第五批只发布 `21224`。固定客户端同时给出 `max_repeat_count=10`、`reward_repeat_count=10` 和两个 `selectable_reward_item_ext_*`。当前 `QuestService` 已在 `completeCount == maxRepeatCount - 1` 时发放 `extended_rewards`，因此该 Quest 不需要新增字段或 Java 领奖分支：第 1-9 次发放经验、2 枚秘银铸币和普通包裹，第 10 次额外发放项链二选一并把完成次数推进到 10，第 11 次由现有 `QuestState.canRepeat()` 拒绝。

`21224` 复用 `gelkmaros.xml/item_collecting`；真端 `Quest_SimpleTalk.xml` 同样证明 `Batalrion/799318` 为接取和交付 NPC。5 个基础掉落目标 `216124-216128`、接取 NPC、地图 `220140000`、GEO、25 个客户端掉落目标模板和全部奖励/收集物品模板均存在。迁移器验收为 `released=true`、`common_diff_rows=0`、未解析字段/引用 `0/0`、运行闭包 `1/1 closed`。第五批模板摘要为 `febb952b70b9e35e249c959aaca97c9dbd3a13bdaa3d86adf4482ff2a283489d`，报告摘要为 `74d0ec101bf73bea4273d429f8ed0767328ab910c2de686d7368f773aabbe980`，全局奖励重复兼容报告摘要为 `ecc3bc7ca0f3d9aed715380832950cd75762fe5e92881e31e7c99dcfe650c471`。

C batch 01 发布 `1871/2871`。固定客户端与真端 `data_driven_quest.xml` 的行为完全一致，均为 Talk 接取、三段顺序 Talk、最终 NPC 领奖；服务端直接复用现有 `report_to_many`，未新增 Java 或 Quest 引擎模型。`1871` 的 NPC 链为 `278501 -> 278506 -> 278513 -> 278514 -> 805351`，`2871` 为 `278001 -> 278006 -> 278014 -> 278015 -> 805356`；10 个 NPC 均有 spawn，世界 `400010000` 与 GEO 存在。模板与行为生成物分别为 `f5ab64da936d554fe7913e3e18dc52eed47f00c6646a8958c1875b8355acb647`、`59e498cf96712bc9d62451cb7330e6df5785e1e7c3b33e140110b906daa5c1ea`，均通过当前 XSD；规范化比较与正式 XML 一致，完整审计为 `released=true`、`common_diff_rows=0`、运行闭包 `2/2 closed`、未解析字段/引用 `0/0`。报告摘要为 `349f763f0fae90666d55e6a909f96d963f3e194c52aeaef9b7a73f480ec09a8c`，发布清单摘要为 `7601b4cfffaad18bcebb03fb764ef7d108b01e78c44ca158e33b5f8b658c2149`。

C batch 02 发布 `15098/25099`。固定客户端与真端 `data_driven_quest.xml` 的行为完全一致：Talk 接取、无 `progress_info`、接取与领奖为同一 NPC。服务端分别在 `cygnea.xml` 和 `enshar.xml` 复用现有 `report_to`，未新增 Java 或 Quest 引擎模型。`15098` 使用 `LF5_Ship_ZoneTeleport_L/804964`、世界 `210080000` 与 `Npcs/210080000_Griffoen.xml`；`25099` 使用 `DF5_Ship_ZoneTeleport_D/804963`、世界 `220090000` 与 `Npcs/220090000_Habrok.xml`；NPC 模板、spawn 和 GEO 均存在。模板与行为生成物分别为 `be598b5e6dafc0d6834b3b95f58fafb7437df71069d3d73ca4e8e230a0b3066a`、`aa97801fb21af9f6da4fa5fee5fe68580dddfe082f35056ed79319ed2bf8b683`，均通过当前 XSD；忽略排版空白后的结构与正式 XML 一致。完整审计为 `released=true`、`common_diff_rows=0`、运行闭包 `2/2 closed`、未解析字段/引用 `0/0`；报告摘要为 `b5ffc7ae23a87a413fc0e822dc802190f6b1a3d2cda9e38ce39b176e241bc37f`，发布清单摘要为 `a82dde9bcb902d2d4f27a1b078d4793e5bc8afb5767d33329e1c86af9a76ad02`。

C batch 03 发布 `14210/24210`。固定客户端提供模板，真端 `Quest_SimpleTalk.xml` 证明两者均为 `GAb1_Sloan_E` 接取、`GAb1_Sub_Geowin_E` 交付且无进度节点。两个名字是 `npcs.xml/quest_ai_name` 的多义战场别名：起点固定展开为 `802464/802660/802661/802662`，终点固定展开为 `802465/802663/802664/802665`；当前迁移器 v14 继续从真端重新校验该集合，不修改普通 NPC 名称索引。服务端复用现有多 ID `report_to`，8 个 NPC 模板与 spawn、`400020000/400030000/400040000/400050000/400060000` 五张 world/GEO 全部闭包，未新增 Java 或 Quest 引擎模型。模板与行为生成物 SHA-256 分别为 `1fb387015d4a9bfce2cfdbd57310e81261c610da535ddd72d62f8499f80807cf`、`70ad3b183acef1e4f69681a58698c791374b01f441ab05500655052347c2e459`，均通过当前 XSD；完整审计为 `6466 / 6452 / 3583 / 91`、`released=true`、`common_diff_rows=0`、运行闭包 `2/2 closed`、未解析字段/引用 `0/0`、AI 生产阻断 `0`。

C batch 04 发布 `1867/1868/1869/2868`，并补齐其固定客户端配对模板 `2852/2869` 的行为与显示字段。固定客户端和真端 `data_driven_quest.xml` 对六条行为完全一致：`1867/2852` 进入 `400010000/Reshanta` 自动接取并击杀 5 名玩家；`1868/2868` 先与 `278503/278003` 对话，再分别按 `A-H-D-G-C-F-B-E`、`D-H-A-E-B-F-C-G` 的顺序进入八个精确球形区域；`1869/2869` 对两组目标各击杀 6。服务端以两个最小共享 Java handler 基类承载 PVP 和连续区域状态机，以两条现有 `monster_hunt` 承载击杀行为；`1867` 的 `quest_permitted_worlds=400010000` 由进入世界和世界内击杀注册显式承载，没有扩展 Quest 模板或 Quest 引擎核心。4 个新模板、2 个配对旧模板、4 个具体 Java handler、2 个共享基类、2 条击杀行为、8 个球形区域、NPC/怪物模板与 spawn、世界和 GEO 均闭包，完整审计为 `6470 / 6456 / 3579 / 87`、`released=true`、`common_diff_rows=0`、运行闭包 `6/6 closed`、未解析字段/引用 `0/0`、AI 生产阻断 `0`。本批只含 `EnterArea/PVP/Talk/Hunt`，AI 生命周期审计为 `not_applicable`。候选模板、C batch 04 报告和发布清单 SHA-256 分别为 `42c883440de14455bffa661ecf611eba5a36530d2c8ba5e354411c233f8bc45d`、`131b50853a3f5b0c16786fea62af0837ee7d245554abb54176f607fbe7806f9d`、`84740dd9aa98d0bdcbda6d04d3a5a08784c65251d72b55703b707e2175779814`。

`80315/80321` 不发布。真端 `Quest_SimpleUseItem.xml` 已证明物品接取、一次 Talk 和交付关系，现有 `report_to_many start_item_id` 足以表达；但必需 NPC `831423/831424/831427/831428` 只有模板，没有服务端 spawn，58Server 静态 `Map/Worlds` 也没有权威坐标。迁移器 v14 将两者固化到 `event_item_isolation.json`，禁止借相邻 `80314/80316/80320/80322` 的行为猜出生位置。

`80341` 的源证据必须区分：真端 `Quest_SimpleTalk.xml` 已明确 `831709/IDAsteria_IU_WORLD_IN_NPC01` 是接取与交付 NPC，这足以修正 handler 的 NPC ID；但固定客户端 `Levels/IDAsteria_iu_World/Level.pak` 场景只包含 `START_NPC01`、`TELEPORT_NPC01`、`ENDING_NPC*`、`IU_WORLD_NPC01` 和 `inviNPC*` 等相邻实体，没有同名 `IN_NPC01`。这些实体名称、用途和 ID 均不同，不能用其中任一坐标代替 `831709`。

隔离的 `9703` 没有已知模板字段堵点，可仅在测试环境用于验证严格 `data_driven_quest` 转换规则。

133 个候选的模板语义堵点如下；同一 Quest 可能命中多个字段：

| 未建模字段族 | 133 个一级候选 | 94 个生产审计池 | 当前决策 |
|---|---:|---:|---|
| `bm_restrict_category` | 32 | 32 | 已证明为真端 BM 账号/包权限位；当前部署无该权限模型，登记为 `compatible_noop`，不加入 `QuestTemplate` |
| `quest_permitted_worlds` | 8 | 1 | 当前模板无世界白名单，交由行为层或最小模型表达 |
| `can_report` | 6 | 6 | 已完成字段映射、通用领奖和第二批行为/入口闭包；当前剩余 87 个一级候选中为 0 |
| `areas` | 5 | 0 | 当前模板无区域集合，交由行为层表达 |
| `reward_repeat_count` | 4 | 1 | 条件兼容：仅当其等于有限 `max_repeat_count`、存在扩展奖励且 `can_repeat_reward=false` 时复用末次 `extended_rewards`；`21224` 已闭包 |
| `can_repeat_reward` | 3 | 0 | 需要独立奖励重复语义 |
| `finished_quest_count1/2` | 3 | 0 | 当前前置条件不能表达指定完成次数 |
| 合计去重 | 初始 51，当前 44 | 初始 39，`can_report`、BM 兼容 no-op 和 `21224` 条件兼容均已关闭 | 最后 15 个生产 A/B 已按 `LDF4b/600030000` 地图级限制正式隔离，不再是未决字段或开发堵点 |

已经排除的伪堵点：

| 项目 | 133 个候选结果 |
|---|---:|
| `%Quest_*` 动态宏 | 0 个 Quest、0 次引用 |
| `nameId` | 133 / 133 可解析 |
| 模板普通物品引用 | 255 / 255 可解析，无歧义 |
| 模板掉落 NPC 引用 | 67 / 67 可解析 |
| 缺失 faction ID `10-13` | 0 个候选 |
| B 类行为 NPC/物品引用 | 全部可解析 |

行为堵点现状：

- 一级候选的 38 个 C 类中有 26 个已因测试/开发证据隔离；`1871/2871`、`15098/25099`、`14210/24210`、`1867/1868/1869/2868` 已发布，`80315/80321` 已因动态活动出生证据缺失隔离，生产 C 类剩余 0 个。
- 全部 38 个 C 类中，31 个来自 `data_driven_quest.xml`，其余是 3 个复杂收集、2 个简单对话和 2 个物品使用任务。
- C 类 `14210/24210` 的两个 `quest_ai_name` 已按真端一对四别名集合完成权威 ID 展开，不再属于堵点。
- C 类 `80315/80321` 所需物品接取语义可由现有 `report_to_many start_item_id` 表达，但 `831423/831424/831427/831428` 当前没有服务端 spawn；找到权威动态出生证据前维持隔离。
- C 类 `1868/2868` 已按固定客户端各自八段顺序区域事件恢复，使用 `var0` 连续推进；`1869/2869` 的两组目标各击杀 6，`1867/2852` 在进入 Reshanta 时接取并击杀 5 名玩家。`1867` 的 `quest_permitted_worlds=400010000` 由进入世界和世界内击杀注册显式承载，没有扩展 Quest 模板字段。
- 一级候选的 42 个 D 类中有 11 个 `DataDriven Empty` 任务已隔离；生产池剩余 31 个 D 类没有当前 handler，也没有 58Server 通用行为记录，必须从其他真端脚本、反编译证据或可验证流程恢复。
- A 类 `50019`、`51019` 没有 58Server 通用行为记录，当前 AionEmu handler 是其服务端行为证据，必须结合客户端和运行测试确认。
- B 类 `41611`、`41614`、`41620`、`41622` 的客户端怪物映射是真端行为目标的超集；24 个额外客户端目标内部名都能解析到当前 NPC ID，生成服务端目标时按客户端扩展，不修改客户端。

因此候选范围、39 个疑似测试/开发任务的 E 隔离、名称解析、客户端目标核对、`cannot_share` 同步、`can_report` 通用能力、BM 兼容 no-op、第二至五批与 C batch 01/02/03/04 Quest 运行闭包，以及 `41600-41614`、`80315/80321` 的隔离终审已经完成。当前最大耗时转为 31 个 D 类行为恢复、已发布 Quest 的真实运行验收，以及 `50019/51019/80341` 的入口取证；LDF4b 相关 23 个 Quest 维持已记录的隔离/静态发布限制，不消耗当前开发阶段时间。`2055` 和其余 3,406 个等级 999 Quest 全部后置。

## 6. 当前 Quest 格式的承载能力

当前 `QuestTemplate` / XSD 已能表达以下逻辑：

- Quest ID、名称、文本 ID、等级范围、军衔和分类；
- 最大重复次数、重复周期、冷却、限制次数恢复；
- 是否可分享、是否可放弃、是否支持无 NPC 上报领奖、目标组类型、导师类型；
- 职业、种族、性别、称号、NPC 阵营限制；
- 收集物品、背包检查、装备检查、任务工作物品；
- 普通奖励、扩展奖励、职业选择奖励和奖励选项；
- EXP、金币、AP、GP、CP、DP、称号、背包和烙印扩展；
- Quest 掉落、Quest 击杀数据；
- 已完成、未完成、未接取、已接取和已装备前置条件；
- 制作任务和挑战任务所需的大部分关联数据，但它们分别属于其他当前文件。

这意味着真端标准模板字段大多可转换，而不需要替换现有运行时模型。真正的缺口集中在少数服务端未建模限制、动态宏和行为层。

### 6.1 Schema 与 Java 模型漂移

当前 XSD 定义了 `mentor` 和 `mentor_type`，但 `QuestTemplate.java` 只实现 `mentor_type`。运行时也实际读取 `mentor_type`。

生成器必须只输出 `mentor_type`，不能因为 XSD 接受 `mentor` 就认为运行时会生效。后续如需清理该漂移，应作为独立任务处理，不与 Quest 数据迁移混合。

## 7. 221 个物理字段的处理矩阵

真端 `quest.xml` 有 221 个直接子字段。大量字段只是固定槽位展开，例如 `reward_item1_1`、`reward_item1_2`、`reward_item2_1`，归并后约四十多个逻辑字段族。

转换器必须维护“物理字段 -> 逻辑字段族 -> 输出位置 -> 处理状态”的完整注册表。新增未知物理字段必须使生成失败，不能忽略。

以下矩阵描述字段如何落入当前服务端结构，不改变第 4.3 节的数据权威优先级。固定客户端已经表达的字段必须先取基础 PAK 与 China PAK 合并后的客户端值；只有客户端未表达的服务端执行细节才回退到 58Server。表中的“直映”表示结构可直接承载，不表示 58Server 值可以覆盖客户端值。

### 7.1 基础、限制与分类

| 真端字段族 | 当前输出 | 规则 |
|---|---|---|
| `id` | `quest/@id` | 必须与 `name=Q{id}` 一致，禁止重编号 |
| `name` | 内部 Quest 名 | `Q1234` 规范化并校验 ID |
| `desc` | `quest/@nameId` 的查表键 | 通过目标版本字符串表解析，缺失则阻断发布 |
| `dev_name` | `quest/@name` 的可读名称 | 仅作为生成辅助；优先使用目标版本正式本地化文本 |
| `minlevel_permitted` | `minlevel_permitted` | 整数直映，99/999 进入隔离 |
| `maxlevel_permitted` | `maxlevel_permitted` | 保留 0 的无限制语义 |
| `abyss_rank` | `rank` | 枚举/数值语义确认后转换 |
| `category1` | `category` | 显式枚举表，未知值阻断 |
| `max_repeat_count` | `max_repeat_count` | 不与 `quest_repeat_count` 混用 |
| `max_count_limitedquest` | `max_count_limited_quest` | 规范化字段名，保留限制次数语义 |
| `count_recover_limitedquest` | `count_recover_limited_quest` | 规范化字段名，保留恢复次数语义 |
| `cannot_share` | `cannot_share` | 布尔结构直映；值按第 4.3 节裁决，客户端未表达时才回退真端默认语义 |
| `cannot_giveup` | `cannot_giveup` | 布尔结构直映；值按第 4.3 节裁决 |
| `can_report` | `can_report` | 布尔结构直映；为 `true` 时仅开放合法自动领奖动作，仍要求任务已经进入 `REWARD` |
| `class_permitted` | `class_permitted` | 内部职业名转当前 `PlayerClass` 枚举 |
| `race_permitted` | `race_permitted` | `pc_light`/`pc_dark` 转当前种族枚举 |
| `gender_permitted` | `gender_permitted` | `all` 不输出限制；其他值显式映射 |
| `mentor_quest_type` | `mentor_type` | 转 `NONE`、`MENTOR`、`MENTE` |
| `target_type` | `target_type` | 转当前目标组枚举 |
| `quest_repeat_cycle` | `repeat_cycle` | 规范化为当前周期枚举列表 |
| `quest_cooltime` | `quest_cooltime` | 保持单位一致后输出 |
| `npcfaction_name` | `npcfaction_id` | 先查阵营表，再校验当前运行时支持 |
| `title` | `titleId` | 称号内部名转 ID，表示接取所需称号，不是奖励称号 |
| `combineskill` | `combineskill` | 制作技能内部名转当前制作技能 ID |
| `combine_skillpoint` | `combine_skillpoint` | 与 `combineskill` 成组输出 |
| `use_class_reward` | `use_class_reward` | 与职业奖励槽位联合校验 |

### 7.2 物品、掉落与前置条件

| 真端字段族 | 当前输出 | 规则 |
|---|---|---|
| `collect_item1...4` | `collect_items/collect_item` | 拆分内部名和数量，名称转物品 ID |
| `collect_progress` | `quest_drop/@collecting_step` 或行为数据 | 必须结合对应收集槽位解释，不能孤立复制 |
| `check_itemN_M` | 收集、背包或行为条件 | 根据真端用途归类，不能全部当作 `collect_items` |
| `inventory_item_name1...3` | `inventory_items/inventory_item` | 名称转物品 ID |
| `equiped_item_name1...5` | `start_conditions/equipped` | 保留真端拼写含义，输出当前正确标签 |
| `quest_work_item1...4` | `quest_work_items/quest_work_item` | 名称和数量转当前结构 |
| `drop_monster_1...4` | `quest_drop/@npc_id` | 内部 NPC 名转 ID；多结果必须有明确展开规则 |
| `drop_item_1...4` | `quest_drop/@item_id` | 内部物品名转 ID |
| `drop_prob_1...4` | `quest_drop/@chance` | 保持百分比语义，`100` 表示 100% |
| `drop_each_member_1...4` | `quest_drop/@drop_each_member` | 与同编号掉落槽位绑定 |
| `finished_quest_cond1...6` | `start_conditions/finished` | `Qxxxx` 转 Quest ID |
| `unfinished_quest_cond1...6` | `start_conditions/unfinished` | 转 Quest ID 列表 |
| `noacquired_quest_cond1...6` | `start_conditions/noacquired` | 转 Quest ID 列表 |
| `acquired_quest_cond1...2` | `start_conditions/acquired` | 转 Quest ID 列表 |

槽位必须按编号成组读取。存在 `drop_monster_2` 却缺少同槽 `drop_item_2` 或概率时，应报告结构错误，不能跨槽拼接。

### 7.3 奖励

| 真端字段族 | 当前输出 | 规则 |
|---|---|---|
| `reward_exp1...6` | 第 1 至 6 个 `rewards/@exp` | 奖励组按编号稳定排序 |
| `reward_gold1...6` | `rewards/@gold` | 与同编号奖励组绑定 |
| `reward_abyss_point1...4` | `rewards/@ap` | 保持数值语义 |
| `reward_glory_point1` | `rewards/@gp` | 直映 |
| `reward_cp1` | `rewards/@cp` | 直映 |
| `reward_dp1` | `rewards/@dp` | 直映 |
| `reward_abyss_op_point1` | `rewards/@abyssOp` | 直映 |
| `reward_exp_boost1` | `rewards/@expBoost` | 直映 |
| `reward_extend_inventory1` | `rewards/@extend_inventory` | 直映 |
| `reward_extend_stigma1` | `rewards/@extend_stigma` | 直映 |
| `reward_title1...2` | `rewards/@title` | 称号内部名转 ID |
| `reward_itemN_M` | 第 N 个 `rewards/reward_item` | 物品名和数量转 ID/数量 |
| `selectable_reward_itemN_M` | 第 N 个 `rewards/selectable_reward_item` | 保留同组选择关系和顺序 |
| `reward_exp_ext`、`reward_gold_ext` | `extended_rewards` | 组成扩展奖励组 |
| `reward_item_ext_M` | `extended_rewards/reward_item` | 名称转 ID |
| `selectable_reward_item_ext_M` | `extended_rewards/selectable_reward_item` | 保留选择关系 |
| `reward_title_ext` | `extended_rewards/@title` | 称号名称转 ID |
| 各职业 `*_selectable_reward` | 当前职业选择奖励节点 | `gunner -> gunslinger`、`bard -> songweaver`、`rider -> aethertech` |

普通物品奖励和动态宏必须分开处理。3,625 个客户端独有 Quest 中有 177 个含 `%Quest_*` 动态奖励宏，共 261 次引用、141 种宏；但当前 133 个一级候选中宏数量为零。宏不是当前批次堵点，后续处理等级 999 Quest 时仍必须建立目标版本宏字典，禁止把宏送入普通物品表查询。

### 7.4 应输出到其他当前文件的字段

| 真端字段 | 当前目标 | 处理方式 |
|---|---|---|
| `recipe_name` | `quest_script_data/work_order.xml` | 配方名转配方 ID |
| `Quest_CombineTask.xml` 的制作 NPC、产品和材料 | `work_order.xml` | 生成 `work_order` 和 `give_component` |
| `reward_challenge_task1` | `quest_data/challenge_tasks.xml` | 与真端 `challenge_task.xml` 联合转换 |

这些字段不应硬塞进 `quest_data.xml`。模板、制作行为和挑战任务必须在同一发布批次验证。

### 7.5 当前主格式不能完整表达的字段

下表使用权威客户端数据。数量按出现该字段族的 Quest 去重统计；字段之间会重叠。

| 客户端字段 | 3,625 个缺失 Quest | 133 个一级候选 | 处理决策 |
|---|---:|---:|---|
| `bm_restrict_category` | 1,489 | 32 | 真端仅用于 BM 账号/包权限位；当前服务端无对应模型，登记为 `compatible_noop`，不加入模板 |
| `areas` | 126 | 5 | 当前模板不能表达区域集合，交由行为层或新增最小模型 |
| `quest_permitted_worlds` | 92 | 8 | 当前模板不能表达世界白名单，交由行为层或新增最小模型 |
| `mobile_event` | 84 | 0 | 平台/活动限定，默认隔离 |
| `reward_repeat_count` | 99 | 4 | 条件兼容而非直接等同：有限次数相等且存在扩展奖励时复用末次 `extended_rewards`；其他形态需独立语义 |
| `quest_repeat_count` | 30 | 0 | 与最大重复次数语义不同，禁止合并 |
| `package_permitted` | 16 | 0 | 服务端账号包限制未建模，默认隔离 |
| `can_repeat_reward` | 3 | 3 | 奖励重复语义未建模 |
| `finished_quest_count1/2` | 3 | 3 | 当前前置结构不能表达指定完成次数 |
| `pcguild_level` | 1 | 0 | 当前模板没有军团等级限制 |
| `reward_score1`、`reward_score_ext` | 1 | 0 | 先判断是否属于挑战贡献或其他积分系统 |

真端还存在少量 `burningreward_*` 活动奖励字段。它们当前没有影响真端独有候选集，但仍必须进入全字段注册表，不能因本批数量为零而永久忽略。

固定客户端全部 10,035 个 Quest 中共有 265 个 `reward_repeat_count` 非空。迁移器已将其中 221 个判定为“现有末次扩展奖励可完整表达”，条件是 `reward_repeat_count` 等于有限 `max_repeat_count`、存在 `*_ext` 奖励且 `can_repeat_reward=false`；其余 44 个仍保留为未建模语义，不允许用 `max_repeat_count` 猜测替代。`21224` 满足该条件并已通过 1-10 次及超限矩阵验证。

`bm_restrict_category` 的固定客户端和真端集合完全一致，均命中 3,477 个 Quest，所有非空值均为 `1`。真端反编译路径只在 `Quest::CanAcquireQuest` 中将该分类与用户 BM restriction bitset 比较；它不是普通 Quest 条件、会员等级或世界限制。当前 AionEmu 没有 BM account/pack entitlement mask，并且第三批发布前已有 1,988 个双方共有 Quest 长期在不实施该权限位的情况下运行。为保持当前部署语义，迁移器将它输出到 `reports/compatibility_noops.json`，状态固定为 `compatible_noop`；禁止把它猜测映射为会员、VIP 或 `package_permitted`。

`can_report` 原本影响 35 个客户端独有 Quest，其中 6 个属于一级候选。该字段现已由 `QuestTemplate`、XSD、转换器和服务端领奖协议完整承载，因此不再列入“不能完整表达”的字段；但字段可表达不等于 Quest 行为已闭包，6 个候选的隔离原因见第 10.1 节。

### 7.6 客户端或编辑器元数据

以下字段主要用于客户端展示、编辑器或数据管理，不直接进入 `QuestTemplate`：

- `category2`
- `client_level`
- `dev_name`
- `__type_desc__`
- `extra_category`
- `f_mission`

处理原则不是删除，而是保留在差异报告中。只有证明不影响服务端行为后，才可标记为“服务端忽略”。

## 8. 名称到 ID 的解析规则

真端大量使用内部名称，当前格式主要使用数值 ID。转换器必须建立版本锁定的名称索引。

### 8.1 已验证覆盖

| 引用类型 | 本轮结果 |
|---|---:|
| 客户端独有 Quest 的 `nameId` | 3,625 / 3,625 可解析 |
| 普通物品内部名 | 3,624 个唯一值全部可解析 |
| 掉落 NPC 内部名 | 2,094 个唯一值全部可解析 |
| 称号内部名（接取条件和奖励） | 87 个全部可解析 |
| 配方数据总量 | 真端与当前均为 14,540 条 |
| 简单行为引用 | 2,020 个缺失 Quest 中，1,907 个 Quest 的 NPC、对象和物品引用全部可解析 |
| 133 个一级候选模板引用 | 133/133 `nameId`、255/255 物品、67/67 掉落 NPC 可解析 |
| 38 个 B 类行为引用 | NPC、对象和物品引用全部可解析 |

该覆盖率说明自动转换具有现实基础，但解析成功不等于行为正确；仍需校验版本、歧义和服务端行为闭包。

### 8.2 规范化规则

- Quest 引用：只接受 `Q` 加数字或明确数值 ID，并校验最终 Quest 集存在。
- 物品引用：拆分内部名和尾部数量，保持原始大小写用于报告，使用规范化键查询。
- 行为物品：真端 `Quest_SimpleUseItem.xml` 中的 `ITEM_` 前缀需按已验证规则去除后再查物品表。
- NPC/怪物引用：内部名可能对应多个数值 ID。只有当前行为结构允许同一目标组包含全部变体时才可展开；否则必须人工确认。
- 称号、配方、阵营：只允许精确索引映射，不使用模糊匹配。
- 动态宏：`%Quest_*` 进入独立宏解析器，不参与普通名称索引。
- 空值、字段缺失、显式 `0` 和显式 `false` 必须保持区别。

### 8.3 NPC 阵营缺口

客户端使用的 18 种 NPC faction 名称都能通过 58Server 阵营表解析为 ID，但当前运行数据缺少 faction ID `10`、`11`、`12`、`13`，影响 30 个客户端独有 Quest。

这 30 个 Quest 全部不在当前 133 个一级候选中，因此 faction 不是当前批次堵点。后续处理对应等级 999 Quest 时，在 faction 数据补齐或证明限制可安全移除前不得发布；禁止将未知 faction 自动改为 0，否则会放宽接取条件。

## 9. Quest 模板转换规则

### 9.1 中间表示

转换器先把每个真端 Quest 归一为不依赖 JAXB 的中间记录：

```text
QuestRecord
  identity
  eligibility
  repeatPolicy
  sharePolicy
  prerequisites
  inventoryRequirements
  rewards[]
  extendedRewards[]
  classRewards{}
  drops[]
  externalRefs
  unsupportedFields
  sourceTrace
```

中间表示仅用于离线生成和报告，不进入运行时，不新增第二套 Quest 引擎模型。

### 9.2 默认值

- 真端字段缺失时使用真端版本的默认语义，不直接套用当前 XSD 默认值。
- 只有证明两边默认值一致后，输出时才可省略属性。
- 为减少不可见差异，关键布尔字段如 `cannot_share`、`cannot_giveup` 建议在生成结果中显式输出。
- 数值 0、空字符串、未出现和无效枚举分别记录，禁止统一转换成空值。

### 9.3 奖励组

- 奖励组按真端编号 1 至 6 输出，不能按内容排序。
- 同一编号下的固定物品和可选物品必须保持组关系。
- 职业奖励必须与 `use_class_reward` 联合检查。
- 扩展奖励只能进入 `extended_rewards`。
- 动态宏未解析时，整个 Quest 不得进入可发布集，而不是只丢掉该奖励。

### 9.4 前置条件

- 所有 Quest 外键必须在最终合并集、当前 server extension 或批准的外部任务集中存在。
- 前置关系构建有向图，检查不存在由迁移错误产生的自引用和循环。
- `finished_quest_count` 不能降级成一次完成条件。
- 过滤掉某个隔离 Quest 时，依赖它的下游 Quest 同时转为阻断状态。

### 9.5 掉落

- `drop_monster_N`、`drop_item_N`、`drop_prob_N`、`drop_each_member_N` 作为原子槽位转换。
- `chance=100` 的语义固定为 100%，不再乘以或换算成 0 至 1 小数。
- Quest 基础概率与当前服务器全局掉率倍率分开报告，生成器不写入运行倍率。
- NPC 或物品名称存在多义时阻断，不选择索引第一项。

## 10. 任务分享专项

当前服务端对分享请求的关键判断位于 `CM_QUEST_SHARE`：

- Quest 模板必须存在；
- `cannot_share` 必须为 `false`；
- 分享者必须已接取且任务不能是 `COMPLETE`；
- 分享者必须处于队伍或联盟；
- 接收者需要在线、在配置距离内、满足等级要求；
- 非重复任务的接收者不能已经拥有有效状态；
- 重复任务的接收者不能处于 `START` 或 `REWARD`；
- 目标组类型还会限制联盟或更大队伍语义。

`QuestTemplate.isCannotShare()` 对缺失属性返回 `false`。完成共有 Quest 同步和 C batch 04 发布后，当前 XML 中有 3,952 个 Quest 显式设置 `cannot_share="true"`、764 个显式为 `false`、1,754 个缺失该属性并在服务端模板层默认允许分享；客户端按钮是否点亮还取决于客户端自己的 Quest 数据和 UI 判断。

模拟器中“共享任务”按钮可用的任务必须同时满足两层条件：固定客户端将该 Quest 标记为可分享且玩家当前处于可分享状态；服务端收到请求后还会执行上面的模板、任务状态、队伍/联盟、距离、等级、重复状态和飞行状态校验。按钮一直灰色首先说明客户端没有开放当前选中任务的分享请求，并不等价于服务端没有任何可分享 Quest。

当前双方共有的 6,456 个 Quest 中，迁移前已有范围曾有 1,009 个 `cannot_share` 差异，现已全部同步；全部已发布批次也与固定客户端一致：

| 漂移方向 | 数量 |
|---|---:|
| 固定客户端允许、当前服务端禁止 | 732 |
| 固定客户端禁止、当前服务端允许 | 277 |
| 当前待同步 | 0 |

这 1,009 个历史漂移曾足以解释一部分“应可分享但当前被禁止”的现象，但现在已不再是待处理项。不能仅凭灰色按钮断定服务端字段错误：客户端会先根据自身 Quest 数据决定按钮状态；服务端只在收到 `CM_QUEST_SHARE` 后再次校验。

133 个迁移前一级候选中，客户端允许分享 89 个、禁止分享 44 个；叠加 E 隔离后的 94 个生产审计 Quest 中允许 52 个、禁止 42 个。18 个首批生产验证 Quest 中允许 13 个，禁止分享的是 `2105`、`2421`、`50019`、`51019`、`80341`。禁止分享的 Quest 按钮保持灰色是预期行为，不能为了点亮按钮而把服务端字段改成允许。

因此分享功能的修复验收必须同步检查：

1. 固定客户端 `cannot_share` 权威值；
2. 服务端最终 `QuestTemplate`；
3. 固定客户端 `Quest.pak` 中同一 Quest 的分享限制（只读核对）；
4. Quest 当前状态和组队环境；
5. 实际是否发送并通过 `CM_QUEST_SHARE`。

只改服务端 `quest_data.xml` 可能仍然看到灰色按钮。本方案可以修正服务端是否接受分享请求，但如果固定客户端本身禁用了按钮，服务端同步不会让按钮点亮；该情况记录为客户端兼容限制，不修改客户端。

### 10.1 无 NPC 上报领奖（`can_report`）

固定客户端共有 217 个 `can_report=true` Quest。共有 Quest 的原始 182 个差异已全部同步；第二批新增 6 个后，当前服务端已存在其中 188 个且全部与固定客户端一致，仍有 29 个客户端独有 Quest 后置。217 个 Quest 中 16 个含可选奖励，0 个含多个普通奖励组，因此当前协议不需要猜测奖励组切换语义。

服务端继续使用现有 `DialogAction`，没有新增客户端协议：

| 客户端动作 | 服务端奖励选择 | 语义 |
|---|---:|---|
| `108 / AUTO_REWARD` | `0` | 无可选奖励时直接领取默认奖励 |
| `110-124 / QUEST_AUTO_REWARD_1-15` | `8-22` | 对应现有 15 个奖励选择编号 |

无 NPC 领奖只在以下条件全部成立时执行：Quest 模板存在且 `can_report=true`、玩家存在该 Quest 状态、状态已经是 `REWARD`、动作属于 `108` 或 `110-124`。对于已识别的自动领奖动作，封包路径会被专用分支消费；资格校验或领奖失败时不会继续落入普通 Quest 对话或转职分支。

以下 6 个 B 类 Quest 已作为第二批发布：

```text
13861, 13865, 13869, 23861, 23865, 23869
```

它们均为客户端 75 级、最低等级 66、`can_report=true` 的每日单奖励组 Quest，奖励为经验 `26591285` 和物品 `188100391 × 50`。固定客户端 `data_driven_quest.xml` 证明从对应要塞 NPC Talk 接取并击杀 1 个指定首领；当前服务端复用 `monster_hunt reward="true"`，击杀后直接从 `START` 进入 `REWARD`，再走现有自动领奖协议。

入口 NPC `268080/270165/269265/268081/270166/269266` 通过要塞 `1221/1231/1241` 的 `PEACE` 状态和归属阵营动态注入，不是普通静态出生。目标首领 `233633/233719/233676` 已按真端坐标加入 `300140000/300130000/300120000`，三张地图的 GEO 均存在。固定客户端行为与真端证据一致；如未来出现异常冲突，仍以固定客户端为准。

## 11. 服务端行为转换

### 11.1 真端行为输入规模

| 文件 | 记录数 |
|---|---:|
| `Quest_SimpleTalk.xml` | 3,152 |
| `Quest_SimpleHunt.xml` | 1,863 |
| `Quest_SimpleCollectItem.xml` | 262 |
| `Quest_SimpleGather.xml` | 0 |
| `Quest_SimpleUseItem.xml` | 160 |
| `Quest_SimpleItemPlay.xml` | 43 |
| `Quest_SimpleSerialHunt.xml` | 16 |
| `Quest_CombineTask.xml` | 574 |
| `data_driven_quest.xml` | 2,155 |
| `challenge_task.xml` | 123 |

这些文件之间会引用同一 Quest，不能把记录数直接相加作为行为 Quest 数量。

### 11.2 候选映射

| 真端行为 | 当前候选目标 | 发布条件 |
|---|---|---|
| `Quest_SimpleTalk` | `report_to`、`report_to_many` 或 `xml_quest` | 起止 NPC、对话和奖励节点完整 |
| `Quest_SimpleHunt` | `monster_hunt` 或现有专用 handler | 击杀组、数量、变量位和起止 NPC 可无损表达 |
| `Quest_SimpleCollectItem` | `item_collecting` 或 `xml_quest` | 对象、物品和变量推进可解析 |
| `Quest_SimpleUseItem` | `xml_quest` 的物品使用事件/操作或现有 handler | 使用物品、目标、场景和变量条件完整 |
| `Quest_SimpleItemPlay` | `xml_quest` 或 Java handler | 多对话/演出顺序可表达 |
| `Quest_SimpleSerialHunt` | `xml_quest` 或 Java handler | 串行阶段不能被错误降级为并行击杀 |
| `Quest_CombineTask` | `work_order` | NPC、配方、产品、材料全部解析 |
| `data_driven_quest` | `xml_quest` 或 Java handler | 所有 category/value 组合均在支持表中 |

映射目标由行为结构决定，不能只按来源文件名决定。例如一个 `SimpleTalk` Quest 可能包含多 NPC 或特殊结束条件，应进入 `report_to_many` 或 C 类，而不是强行输出最简单模板。

### 11.3 A 至 D 分类判定

- A：最终 Quest ID 已由当前 XML handler 或 Java handler 注册，且只缺模板。发布前仍需确认 handler 引用的物品、NPC 和奖励与真端模板一致。
- B：真端行为的全部字段都能进入当前通用模型，且转换前后状态机、变量编号和结束条件一致。
- C：有行为数据，但存在多阶段、串并行差异、区域/世界条件、特殊物品演出、动态生成 NPC 或当前模型未覆盖操作。
- D：真端通用行为文件没有该 Quest，必须继续从其他真端脚本、反编译逻辑、客户端流程或人工实现中恢复。

B 类必须由规则证明，而不是“字段较少”或“看起来像简单任务”。任一未解释行为字段都会把 Quest 降为 C 类。

本轮 B 类严格规则只接受以下已验证形状：

- `Quest_SimpleTalk`：字段仅由起止 NPC、`item_check`、单一 `give_item` 和前置组成，引用全部可解析；
- `Quest_SimpleHunt`：最多五组计数/怪物目标，允许前置，不能含额外对话、发放/回收物品或未解释字段；
- `Quest_SimpleCollectItem`：最多四个对象、允许 `party_drop` 和前置，不能含未解释操作；
- `data_driven_quest`：仅接受 Talk 接取、单一 Hunt 进度节点、单一进度值且全部目标可解析的形状。

按该规则，全量互斥分类为 A=83、B=1,631、C=770、D=1,141；客户端一级候选为 A=15、B=38、C=38、D=42；叠加测试/开发 E 隔离后的生产审计池为 A=15、B=36、C=12、D=31。改变严格规则或 E 证据规则必须重新生成分类快照，不能手工移动数字。

首批运行修正还覆盖 `ReportTo` 的同 NPC 双角色场景：当同一个 NPC 同时属于接取和交付集合时，`START` 状态必须优先进入交付分支。聚焦 Quest `80586/80591`、NPC `832267` 的回归测试已固定该行为，避免任务在交付时重新进入接取选择。

### 11.4 行为外键

每个可发布 Quest 至少满足：

- 恰好有一个有效 handler 注册路径，或有明确的组合行为设计；
- 起始和结束 NPC 可解析且存在于目标版本 NPC 数据；
- 击杀 NPC、交互对象、物品和技能引用存在；
- 变量编号、起止值和完成状态不会超出当前任务变量表示；
- 任务模板的收集数量、掉落数量与行为推进数量一致；
- 领奖 NPC、奖励组和完成条件一致；
- 对应世界有实际 spawn，或任务明确使用动态 spawn。

## 12. 固定客户端权威边界（不修改客户端）

本方案不生成、修改、重新打包或发布任何客户端资源。实际客户端基础 `Quest.pak` 只有以下 6 个文件：

```text
challenge_task.xml
combine_task.xml
data_driven_quest.xml
quest.xml
quest_monster.csv
quest_script_monster.csv
```

China `quest.pak` 只包含覆盖用 `quest.xml`。两层合并后的客户端视图以只读方式作为最终语义基准，核对内容包括：

- Quest ID 和基础文本是否已经存在；
- 固定客户端是否允许显示和操作该 Quest；
- 分享按钮状态是否由客户端数据限制；
- 怪物标记和定位数据是否已经存在。

如果固定客户端完全不存在某个 Quest 的基础定义，导致任务名称、对话或必要操作不可用，该 Quest 不进入发布集。怪物标记或寻路提示缺失但不影响服务端完成流程时，作为已知兼容限制报告，不在本方案中修复。

### 12.1 怪物头顶 Quest 标记

怪物头顶的 Quest 标记由客户端数据决定。服务端 Quest 列表和 NPC 信息封包没有专用的“该怪物是 Quest 目标”标志；客户端根据 `quest_monster.csv` 和 `quest_script_monster.csv` 关联当前 Quest 与怪物内部名。

已验证快照中，当前服务端共有 958 个 `monster_hunt` Quest：

| 项目 | 数量 |
|---|---:|
| 客户端存在映射 | 843 |
| 客户端缺少映射 | 115 |
| 有映射但至少一个目标名不一致 | 76 |

因此新增击杀 Quest 时，服务端可以正确生成 `monster_hunt`，但固定客户端缺少映射的任务可能没有头顶标记。该差异只读统计，不生成或修改客户端映射。

38 个 B 类候选中有 33 个击杀行为。固定客户端对这 33 个 Quest 全部存在怪物映射：29 个与 58Server 行为目标集合完全一致，`41611`、`41614`、`41620`、`41622` 的客户端集合更大且不缺少真端目标。客户端额外的 24 个目标内部名全部能解析到当前 NPC ID，因此生成行为时采用客户端超集作为兼容覆盖。

### 12.2 定位和寻路提示

当前固定客户端的 Quest PAK 中没有 `Quest_SimpleHunt.xml` 或 `Quest_SimpleSerialHunt.xml`，因此不能把 `/Users/mc/IdeaProjects/58Server/Map/XML` 或其他静态文件目录中的同名文件伪装成客户端证据。固定客户端可直接证明的是 `quest_monster.csv`、`quest_script_monster.csv` 和 `data_driven_quest.xml` 中的内部名关系；其他定位表现必须通过实际客户端请求和服务端 spawn 查找验证。

服务端地图查找路径为：

```text
CM_OBJECT_SEARCH
  -> SpawnsData2.getFirstSpawnByNpcId(...)
  -> SM_SHOW_NPC_ON_MAP
```

只读兼容性核对包括：

- 固定客户端 Quest 行为记录是否包含正确内部名；
- 内部名能解析到服务端 NPC ID；
- 目标世界存在可用 spawn；
- 多世界或多变体目标有明确选择规则。

### 12.3 服务端发布边界

发布单元只包含服务端模板、服务端行为、兼容补丁和迁移 manifest。禁止以下混合状态：

- 新模板 + 旧行为；
- 新行为 + 旧模板；
- 只回滚模板或只回滚行为。

客户端 Quest.pak 保持不变，不属于发布和回滚单元。

## 13. 离线转换器设计

### 13.1 最小组件

只需要一个离线转换器，不新增运行时加载器、缓存服务或第二套 Quest 模型。所有 Quest 迁移脚本统一放在 `/Users/mc/PycharmProjects/aion_drop`，使用该仓库现有 Python/标准库实现；AionEmu 只作为当前格式输入和最终服务端发布目标，不存放迁移脚本。

```text
读取并冻结输入
  -> UTF-16/DTD 安全解析
  -> 221 物理字段注册检查
  -> 归一化 QuestRecord
  -> 名称和外键解析
  -> 与当前 Quest 双基线合并和比较
  -> A/B/C/D/E 分类
  -> 护送/保护/动态生成 AI 闭包审计
  -> 生成当前格式暂存文件
  -> XSD/JAXB/服务端外键校验
  -> 固定客户端兼容性只读报告
  -> 输出差异报告和发布 manifest
```

### 13.2 建议输出

生成过程先写入构建目录，不直接覆盖正式资源：

```text
/Users/mc/PycharmProjects/aion_drop/target/quest-migration/
  quest_data.generated.xml
  quest_data.batch_02.generated.xml
  quest_scripts.generated/
    retail_batch_01.xml
    retail_batch_02.xml
  challenge_tasks.generated.xml
  work_order.generated.xml
  reports/
    coverage.json
    common_diff.csv
    missing_quests.csv
    unresolved_fields.csv
    unresolved_refs.csv
    ai_closure.csv
    ai_closure.json
    fixed_client_compatibility.csv
    first_batch.json
    second_batch.json
    release_manifest.json
```

只有全部门槛通过后，人工审查生成差异，再替换正式资源。

### 13.3 确定性要求

- Quest 按数值 ID 排序。
- 奖励、条件和掉落按原始槽位稳定排序。
- XML 缩进、属性顺序和换行固定。
- 输出不包含当前时间等不稳定内容；时间只写报告元数据。
- 相同输入摘要和转换器版本必须产生相同输出摘要。
- 未知字段、未知枚举、重复 ID、歧义引用和缺失外键一律失败。

### 13.4 兼容覆盖层

最终合并顺序建议为：

```text
真端转换基础
  -> 已批准的最小兼容补丁
  -> 当前 14 个 server extension
```

兼容补丁只允许按明确 Quest ID 或可证明的规则覆盖单个字段，并记录原因、证据和到期条件。禁止复制整条当前 Quest 覆盖真端结果，否则真端将失去权威性。

### 13.5 已实现的迁移工具

当前已在数据转换仓库实现 `/Users/mc/PycharmProjects/aion_drop/scripts/quest_migration_audit.py`。它复用现有 `/Users/mc/PycharmProjects/unpak/aion_pak.py`，把基础和 China PAK 解包到系统临时目录，完成摘要校验后自动删除临时文件；正式客户端资源始终只读。后续 Quest 迁移脚本也必须继续放在 `aion_drop`，不得回写到 AionEmu 的 `scripts/`。

运行方式：

```bash
cd /Users/mc/PycharmProjects/aion_drop
scripts/quest_migration_audit.py
```

自检方式：

```bash
cd /Users/mc/PycharmProjects/aion_drop
scripts/quest_migration_audit.py --self-test
```

当前生成以下确定性产物：

```text
/Users/mc/PycharmProjects/aion_drop/target/quest-migration/
  quest_data.generated.xml
  quest_data.batch_02.generated.xml
  quest_data.batch_03.generated.xml
  quest_data.batch_04.generated.xml
  quest_data.batch_05.generated.xml
  quest_data.c_batch_01.generated.xml
  quest_data.c_batch_02.generated.xml
  quest_data.c_batch_03.generated.xml
  quest_data.c_batch_04.generated.xml
  quest_data.common_share.generated.xml
  quest_data.common_report.generated.xml
  quest_scripts.generated/retail_batch_01.xml
  quest_scripts.generated/retail_batch_02.xml
  quest_scripts.generated/retail_batch_03.xml
  quest_scripts.generated/retail_c_batch_01.xml
  quest_scripts.generated/retail_c_batch_02.xml
  quest_scripts.generated/retail_c_batch_03.xml
/Users/mc/PycharmProjects/aion_drop/target/quest-migration/reports/
  release_manifest.json
  coverage.json
  field_registry.json
  base_china_diff.csv
  client_retail_diff.csv
  common_diff.csv
  common_diff.json
  common_share.json
  common_report.json
  missing_quests.csv
  current_extensions.csv
  first_batch.json
  second_batch.json
  third_batch.json
  fourth_batch.json
  fifth_batch.json
  c_batch_01.json
  c_batch_02.json
  c_batch_03.json
  c_batch_04.json
  event_item_isolation.json
  compatibility_noops.json
  reward_repeat_compatibility.json
  unresolved_fields.csv
  unresolved_refs.csv
  ai_closure.csv
  ai_closure.json
```

工具已实现并验证：

- 固定客户端启动参数、基础 PAK、China PAK、58Server Quest、当前 Quest/XSD 和真端行为文件摘要；
- 基础与 China 客户端字段级覆盖差异；
- 客户端与 58Server 的同 Schema 字段差异；
- 221 个物理字段全部注册并具有处理状态；
- 迁移前 `10,035 / 6,424 / 6,410 / 3,625 / 14`、首批后 `10,035 / 6,442 / 6,428 / 3,607 / 14`、第二批后 `10,035 / 6,448 / 6,434 / 3,601 / 14`、第三批后 `10,035 / 6,458 / 6,444 / 3,591 / 14`、第四批后 `10,035 / 6,459 / 6,445 / 3,590 / 14`、第五批后 `10,035 / 6,460 / 6,446 / 3,589 / 14`、C batch 01 后 `10,035 / 6,462 / 6,448 / 3,587 / 14`、C batch 02 后 `10,035 / 6,464 / 6,450 / 3,585 / 14`、C batch 03 后 `10,035 / 6,466 / 6,452 / 3,583 / 14`、C batch 04 后 `10,035 / 6,470 / 6,456 / 3,579 / 14` 十个基线；
- 迁移前一级候选 133、首批后 115、第二批后 109、第三批后 99、第四批后 98、第五批后 97、C batch 01 后 95、C batch 02 后 93、C batch 03 后 91、C batch 04 后 87 的基线；
- 名称、物品、NPC、Quest、称号和 faction 引用解析；
- 固定客户端与当前共有 Quest 的全量逻辑字段差异；
- 共有 Quest 的 `cannot_share` 和 `can_report` 确定性同步产物、同步前冻结数量和同步后待办数量；
- 首批 18 个 Quest 当前格式模板和 13 个通用行为生成；
- 第二批 6 个 Quest 当前格式模板、固定客户端数据驱动击杀行为和运行入口闭包生成；
- 第三批 10 个 Quest 当前格式模板、2 个新行为、8 个现有行为和运行 NPC 闭包生成；
- 第四批 `2585` 模板、OR 前置分支兼容和现有行为/NPC 闭包生成；
- 第五批 `21224` 模板、条件兼容奖励重复语义、现有收集行为、NPC/spawn/GEO 和 1-10 次领奖矩阵生成；
- C batch 01 `1871/2871` 模板、三段顺序 Talk 行为和 NPC/spawn/world/GEO 闭包生成；
- C batch 02 `15098/25099` 模板、零进度同 NPC Talk 行为和 NPC/spawn/world/GEO 闭包生成；
- C batch 03 `14210/24210` 模板、真端 `quest_ai_name` 一对四变体校验、多 NPC `report_to` 和五张 world/GEO 闭包生成；
- C batch 04 `1867/1868/1869/2868` 模板、`2852/2869` 配对行为修复、Reshanta PVP、双路线八区域状态机、两组击杀和 6 条运行闭包生成；
- `80315/80321` 的可表达行为与缺失动态出生证据隔离报告生成；
- `bm_restrict_category` 客户端/真端集合和值域校验，以及 `compatible_noop` 兼容报告；
- `reward_repeat_count` 全量条件兼容分类报告：265 个中 221 个复用现有末次扩展奖励语义，44 个继续隔离；
- 当前 Java handler、Quest 行为 XML 与真端动态生成动作的 AI 闭包审计；
- 输入变化、重复 Quest ID、重复字段、非法布尔值和基线漂移立即失败；
- 相同输入重复运行得到全部输出摘要完全一致。

生成器不直接覆盖正式资源；生成 XML 经人工审查和验证后发布到 AionEmu。首批报告固定为 18 个模板、13 个生成行为；第二批报告固定为 6 个模板、6 个生成行为、6 条运行闭包；第三批报告固定为 10 个模板、2 个生成行为、10 条运行闭包；第四批和第五批各为 1 个模板、0 个新行为、1 条运行闭包；C batch 01/02/03 各为 2 个模板、2 个现有通用类型行为、2 条运行闭包；C batch 04 为 4 个新模板、4 个具体 Java handler、2 条 XML 击杀行为和 6 条运行闭包；未解析字段和引用均为 0。`51019` 已按客户端/模板收集语义把现有不兼容的 `monster_hunt` 修正为 `item_collecting`；`80341` handler 已从旧的 `831541-831548` 改为真端行为明确的 `831709`，但这两项仍须通过真实入口运行验收。

出生审计复用现有 `/Users/mc/PycharmProjects/aion_drop/staticdata_converter/generate_retail_npc_spawns.py`，只向 `/Users/mc/PycharmProjects/aion_drop/target/quest-migration/spawn-audit/` 生成临时副本。该工具只处理可证明的普通静态出生，不能把活动、条件、Party 或缺失地图降级为普通 `spawn`；因此本轮没有发布任何猜测出生点。

### 13.6 AI 闭包审计规则与当前结果

审计产物为：

- `/Users/mc/PycharmProjects/aion_drop/target/quest-migration/reports/ai_closure.csv`
- `/Users/mc/PycharmProjects/aion_drop/target/quest-migration/reports/ai_closure.json`

CSV 固定字段为 `quest_id, scope, kind, source, evidence, status, blocker`；JSON 额外保留真端动作、解析后的 NPC ID、坐标、Java 调用位置、XML 证据和汇总。相关 handler 树、Quest 行为 XML 树、公共跟随实现和保护回调文件摘要已写入 `release_manifest.json`，避免代码或数据变化后复用旧结论。

护送闭包要求：

1. handler 明确调用 `defaultStartFollowEvent` 或等价 `FOLLOW_ME` 启动；
2. 注册并实现 `onNpcReachTargetEvent`；
3. 注册并实现 `onNpcLostTargetEvent`；
4. 公共链包含 `TaskId.QUEST_FOLLOW`、`FollowingNpcCheckTask`、到达/丢失分发、取消任务和 `STOP_FOLLOW_ME` 清理。

当前扫描到 18 个护送 handler，18/18 为 `closed`。这证明现有护送公共闭包完整，不表示未来新增护送任务可跳过逐 handler 审计。

保护闭包要求具体 Quest handler 同时实现成功和失败回调，并存在从运行事件进入这些回调的分发入口。当前只有 `AbstractQuestHandler.onProtectEndEvent/onProtectFailEvent` 两个返回 `false` 的默认方法；具体 handler 为 0，分发入口为 0。报告状态为 `no_evidence`，不能因类名或任务标题含 `Protect` 就推断保护语义，更不能自动生成 handler。

`no_evidence` 是全局信息哨兵，不对应具体可发布 Quest，因此不计生产阻断。未来一旦出现具体保护 handler，该 Quest 必须同时证明成功回调、失败回调和运行分发入口，缺一项即进入 `blocked`。

第二批要塞自动领奖 Quest 另设 `siege_report_hunt` 审计类型，逐 Quest 检查 `can_report` 模板、`reward="true"` 状态推进、接取 NPC、首领目标、要塞归属阵营入口、副本目标出生和地图 GEO。当前结果为 `closed=6`、`blocked=0`。

动态生成动作从 `data_driven_quest.xml` 的嵌套 `progress_info/data/category_progress_/value5_progress_` 读取。只把以 `Absolute ` 或 `Relative ` 开头的 `value5_progress_` 当作生成动作；纯数字 `value5` 是类别参数，禁止误判。当前真端数据统计为：

| 项目 | 数量 |
|---|---:|
| `quest_data_driven` | 2,155 |
| 进度节点 | 3,061 |
| 含生成动作的进度节点 | 49 |
| 拆分后的生成动作 | 57 |
| 涉及 Quest | 40 |

每条动态生成动作必须证明：

- `Absolute/Relative` 格式、数量和存活时间合法；
- NPC 内部名唯一解析到当前 NPC ID；
- `Absolute` 的 X/Y/Z 与服务端调用一致，或 `Relative` 明确使用事件 NPC/玩家坐标作为基准；
- 服务端 Java handler 或同 Quest 的 `kill_spawned` XML 对精确 NPC ID 有实现；
- 生成数量可从静态实现证明；
- 真端 `lifetime_seconds` 有等价的超时回收，不以“可被击杀”替代失败/放弃路径的回收。

当前 57 条结果为：

| 状态 | 数量 | 含义 |
|---|---:|---|
| `closed` | 46 | 生产范围的位置、数量和秒级超时回收全部可证明 |
| `isolated` | 11 | 固定客户端最低等级 999 且当前模板不存在，不进入生产实现范围 |
| `partial` | 0 | 无剩余部分闭包 |
| `blocked` | 0 | 无剩余生产阻断 |

11 条隔离动作来自 9 个固定客户端等级 999 Quest：`9693`、`9696`、`9697`、`10036`、`15692`、`18395`、`20038`、`25692`、`28395`。其中 `15692`、`20038` 各有两条动作。它们不为清零统计而补当前模板或 handler；只有固定客户端版本策略变化时才重新进入生产审计。

生产修复复用现有 Quest handler 和 `QuestService`，没有新增第二套 Quest 引擎。`QuestService.addNewSpawnForSeconds(...)` 明确以秒为单位，返回生成对象，并在普通地图和副本中统一调度删除；原分钟重载保留兼容性，改为委托秒级接口，同时修复旧实现丢失 `heading` 和实例地图不回收的问题。所有 46 条生产动作现均能证明精确 NPC、Absolute/Relative 基准、数量和真端存活时间。

补齐范围包括原有 23 条部分闭包调用的秒级化和坐标/数量校正，以及 23 条缺失生产动作的精确实现；新增完整 handler 的 Quest 为 `25082`、`30721`、`30771`。`30721/30771` 使用真端目标 `236654`，各生成 2 个、存活 120 秒，并显式建立对玩家的仇恨，不沿用旧参考实现的错误目标 `217424`。

发布门禁按具体行生效：`blocked`、`partial` 均阻断对应 Quest；`isolated` 不进入生产发布集；没有具体保护任务证据的全局 `no_evidence` 哨兵只作信息提示。不得用永久静态 spawn、相似 NPC、猜测坐标或放宽固定客户端隔离规则规避审计。

## 14. 双方共有 Quest 的数据漂移

不能只追加 3,625 个缺失 ID。以下统计使用 China 覆盖后的固定客户端与当前服务端比较，不再使用 58Server 值直接裁决；`cannot_share` 和 `can_report` 已同步，因此不再出现在当前待办中：

| 对比项 | 有差异的 Quest 数 |
|---|---:|
| 最低等级 | 21 |
| 最高等级 | 1,216 |
| 最大重复次数 | 100 |
| 分类 | 71 |
| 职业限制 | 234 |
| 种族限制 | 24 |
| 性别限制 | 0 |
| 普通奖励（排除动态宏） | 219 |
| 扩展奖励 | 59 |
| 核心掉落 NPC/物品/概率 | 705 |
| 收集物品 | 18 |
| 任务工作物品 | 14 |
| 背包条件 | 52 |
| `finished` 条件 | 234 |
| `unfinished` 条件 | 133 |
| `noacquired` 条件 | 191 |
| `acquired` 条件 | 2 |

当前全量报告为 `4,757` 条逻辑字段差异、涉及 `3,300` 个共有 Quest；CSV 含表头共 `4,758` 行。相对上一快照减少的差异包括已经正式同步的 `cannot_share`；`can_report` 的 182 个共有 Quest 也已同步且当前待办为 0。当前差异报告是后续“双方共有 Quest 同步”的工作清单，不表示剩余 4,757 条已自动写回正式资源。

每个差异必须归入以下状态：

| 状态 | 处理 |
|---|---|
| 客户端修正 | 采用固定客户端值 |
| 目标版本差异 | 以固定 5.8 客户端为准；客户端未表达的服务端细节再参考真端 XML |
| 服务器兼容 | 保留为显式补丁 |
| 纯表示差异 | 证明运行语义等价后归一 |
| 未解释 | 阻断该 Quest 发布 |

同步顺序必须先处理共有 Quest。否则新增任务使用真端规则，而旧任务继续使用漂移规则，会形成两套不一致语义，尤其影响分享、等级限制、掉落和奖励。

## 15. 分阶段实施

### 阶段 0：冻结基线

- 生成全部真端、当前服务端、客户端启动脚本、基础 Quest PAK 和 China 覆盖 PAK 的只读输入摘要。
- 按 `基础 -> China 覆盖` 生成权威客户端审计视图。
- 固定 10,035 / 6,424 / 6,410 / 3,625 / 14 基线。
- 输出当前共有、真端独有和服务器独有 ID 清单。

状态：已完成。重复运行得到相同统计和摘要。

### 阶段 1：只读转换与差异报告

- 实现 221 个物理字段注册表和归一化记录。
- 完成名称到 ID 索引。
- 只生成 `/Users/mc/PycharmProjects/aion_drop/target/quest-migration` 暂存结果，不修改正式资源。
- 输出共有 Quest 和缺失 Quest 全量差异。

状态：已完成。未知字段为零，221 个字段全部有处理状态，差异和候选报告已生成。

### 阶段 2：同步双方共有 Quest（迁移前 6,410，当前 6,456）

- 优先关闭 `cannot_share`、等级、分类、职业、种族、性别、前置、奖励和掉落差异。
- 以固定客户端与当前服务端的差异表为工作清单；客户端未表达的服务端执行细节再回退到 58Server。
- 为服务器人工修复建立最小兼容补丁。
- 不增加新 Quest ID，先证明转换规则不会破坏已有任务。

验收：共有 Quest 的“未解释”差异为零；代表性现有 Quest 行为回归通过。

状态：进行中。`cannot_share` 已修正 1,009 个、`can_report` 已修正 182 个，两项当前待同步均为 0；其余仍有 4,757 条差异，涉及 3,300 个共有 Quest，因此阶段 2 尚不能整体标记完成。

### 阶段 3：18 个首批生产验证集

- 先处理 5 个无已知模板语义堵点的 A 类和 13 个无已知模板语义堵点的生产 B 类。
- 隔离的 `9703` 仅在测试环境验证数据驱动转换，不进入生产发布单元。
- 对 `41620`、`41622` 应用客户端目标超集；`41611`、`41614` 因同时含 `bm_restrict_category` 留到下一阶段。
- 5 个客户端禁止分享的 Quest 保持 `cannot_share=true`，不以按钮是否点亮作为迁移成功条件。

状态：静态发布已完成，端到端验收未完成。18 个模板、13 个生成行为和 5 个现有行为闭包已经进入正式资源；`51019` 行为类型与 `80341` handler NPC 已修正。`41615-41622`、`50019/51019`、`80341` 仍受真实接取入口阻塞，不能标记为流程完成。

验收：18 个 Quest 均能通过真实 NPC 自然接取、推进、完成和领奖；模板、行为、客户端目标关系和分享语义一致。GM 强制接取只能用于定位行为问题，不计入正式验收。

### 阶段 4：第二批 6 个 `can_report` B 类 Quest

- 从固定客户端 `data_driven_quest.xml` 读取 Talk 接取、奖励 NPC 和单目标 Hunt，模板与行为以客户端为准。
- 复用 `monster_hunt reward="true"`，使击杀后直接进入 `REWARD`，不新增 Java handler。
- 按真端证据将入口 NPC 放入 `1221/1231/1241` 要塞的 `PEACE` 阵营分支，并补齐三张副本首领出生。
- 对模板、行为、阵营入口、目标出生和 GEO 生成 `siege_report_hunt` 闭包报告。

状态：已完成。6 个模板、6 个行为和 6 条运行资源闭包均已发布，`closed=6`、`blocked=0`。

### 阶段 5：第三批 10 个生产 A/B Quest

- 证明 `bm_restrict_category` 的真实用途和值域，不把 BM 权限位猜测映射为会员或 VIP。
- 发布 `1770, 2511, 2599, 2605, 2611, 2667, 2768, 21015, 21025, 21120`。
- 复用当前已有 8 个行为，只为 `1770/2768` 新增两条 `monster_hunt`。
- 逐 Quest 校验模板、前置、行为、入口、交付、掉落和击杀 NPC spawn。

状态：已完成。10 个模板已发布，2 个行为已加入 `reshanta.xml`，10/10 运行闭包为 `closed`，未解析字段/引用为 0/0。

### 阶段 6：第四批 `2585` 前置兼容闭包

- 将客户端 `finished_quest_cond1=2055`、`finished_quest_cond2=4542` 按两个 OR 分支处理，不合并成 AND。
- 证明 `2055` 为固定客户端等级 999 且无可执行行为，保持隔离。
- 按现有 `2586-2588` 兼容模式只发布有效 `4542` 分支。
- 复用 `beluslan.xml/item_collecting`，检查 `204739/700331` spawn。

状态：已完成。`2585` 已发布，运行闭包 `1/1 closed`，`2055` 未发布，未解析字段/引用为 0/0。

### 阶段 7：第五批 `21224` 奖励重复兼容闭包

- 证明 `reward_repeat_count=10` 与有限 `max_repeat_count=10` 相等，且两个 `selectable_reward_item_ext_*` 可由现有末次 `extended_rewards` 完整表达。
- 复用 `gelkmaros.xml/item_collecting` 和 `799318` 同 NPC 接取/交付行为。
- 校验 25 个客户端掉落目标、5 个基础目标 spawn、地图 `220140000`、GEO、收集物品和全部奖励物品模板。
- 验证第 1-9 次只发普通奖励、第 10 次追加扩展二选一奖励、第 11 次不可接取。

状态：已完成。`21224` 已发布，运行闭包 `1/1 closed`，未解析字段/引用为 0/0；未新增 Java 领奖逻辑。

### 阶段 8：15 个 LDF4b 生产 A/B Quest 隔离终审

- 固定客户端确认权威世界 ID 为 `600030000`，不是真端派生数据中的 `600031000`。
- 核对 `world.pak` 元数据、116 个 subzone、周边资源和缺失的 `Levels/LDF4b` 关卡资产。
- 核对客户端 15/15 Quest、73 个目标，真端 15/15 行为和 53 个基础目标；目标冲突时以客户端集合为准。
- 核对 AionEmu world map、GEO、入口 NPC 模板与 spawn，不因模板可生成而绕过地图或运行资源闭包。

状态：已完成。`41600-41614` 全部为 `isolated`，不进入正式数据；`41615-41622` 继续保持“静态发布、地图级隔离”。下一阶段直接进入 C 类任务。

### 阶段 9：C 类任务

- C batch 01 已发布 `1871/2871`，两者固定客户端与真端行为完全一致，直接复用 `report_to_many`。
- C batch 02 已发布 `15098/25099`，两者固定客户端与真端行为完全一致，直接复用 `report_to`。
- C batch 03 已发布 `14210/24210`，按真端 `quest_ai_name` 的四战场变体复用多 ID `report_to`。
- C batch 04 已发布 `1867/1868/1869/2868`，并闭包配对旧模板 `2852/2869`；以最小 Java handler 承载 Reshanta PVP 和两条八区域顺序状态机，以现有 `monster_hunt` 承载两组击杀。
- `80315/80321` 已因动态活动 NPC 缺少权威出生证据隔离，不生成模板或猜测 spawn。
- 26 个疑似测试/开发 C 类维持 E 隔离；生产 C 类剩余开发项为 0。
- 护送、保护或动态生成任务必须先进入 `ai_closure`；具体生产 Quest 的对应行必须为 `closed`。`isolated` 不进入发布集，全局保护 `no_evidence` 哨兵不代表存在可发布保护任务。
- 只有多个 Quest 共享同一真实语义时才扩展通用模型；单个特殊 Quest 优先使用现有 XML 能力或最小 Java handler。

状态：已完成。前三个 C 批次各自运行闭包 `2/2 closed`，C batch 04 为 `6/6 closed`，`80315/80321` 为 `2/2 isolated`；生产 C 类没有未决模板、行为或 AI 闭包。

### 阶段 10：D 类与 E 隔离任务

- 对生产池剩余 31 个 D 类继续从其他真端脚本、客户端数据或反编译证据恢复；11 个 `DataDriven Empty` D 类维持 E 隔离。
- 3,406 个客户端最低等级 999 Quest 和全部明确测试任务保持 E 隔离，不进入当前实施排期。
- 不将无行为任务以“不可接取模板”形式混入正式数据，除非项目明确需要仅展示记录。

验收：不存在被统计为已迁移但无法完成的 Quest。

## 16. 验证方案

### 16.1 静态结构

- 生成 XML 通过当前 XSD。
- JAXB 能加载全部生成记录。
- Quest ID 唯一且顺序稳定。
- `QuestTemplate` 实际字段覆盖与 XSD 一致；特别检查 `mentor_type`。
- 未知字段、未知枚举、动态宏和歧义引用数量为零，或对应 Quest 明确处于隔离状态。
- `ai_closure.csv/json` 可重复生成，且发布 Quest 的 AI 行全部为 `closed`。

### 16.2 外键完整性

- Quest 前置引用存在。
- 物品、NPC、对象、技能、称号、配方和 faction 引用存在。
- 行为 handler 恰好注册一次。
- 掉落 NPC 和行为击杀 NPC 与固定客户端内部名一致；发生异常时采用客户端目标关系。
- 定位目标存在 spawn；动态 spawn 的精确 NPC、位置基准、数量和超时回收均有对应行为实现。
- 挑战任务和制作任务与 Quest 模板同批一致。

### 16.3 差异快照

每次转换固定输出以下摘要：

- 总 Quest 数、共有/独有/扩展数；
- A/B/C/D/E 数量；
- 一级候选 133、原 E 隔离 39、生产审计池 94、首批 18、第二批 6、第三批 10、第四批 1、第五批 1、C batch 01/02/03 各 2、C batch 04 新模板 4、当前剩余一级候选 87，以及新增 LDF4b 地图级隔离 15、活动物品任务隔离 2 的基线；
- B 类 33 个击杀行为中客户端映射 33/33、完全一致 29、客户端超集 4 的统计；
- 各字段差异数量；
- 未解析字段、引用、宏、客户端冲突和兼容限制数量；
- 输出文件摘要。

任何数量变化必须由输入摘要变化或显式规则修改解释。

当前已验证快照：

| 项目 | 结果 |
|---|---:|
| C batch 04 发布后覆盖 | `10035 / 6470 / 6456 / 3579 / 14` |
| C batch 04 发布后剩余一级候选 | 87 |
| 客户端最低等级 999 / 全部字面测试 | `3406 / 120` |
| 共有逻辑字段差异 | 4,757 |
| 有差异的共有 Quest | 3,300 |
| `cannot_share` 已同步 / 待同步 | 1,009 / 0 |
| `can_report` 当前存在 / 待同步 | 188 / 0 |
| 首批模板 / 生成行为 | 18 / 13 |
| 首批未解析字段 / 引用 | 0 / 0 |
| 第二批模板 / 生成行为 / 运行闭包 | 6 / 6 / 6 |
| 第三批模板 / 生成行为 / 运行闭包 | 10 / 2 / 10 |
| 第三批未解析字段 / 引用 | 0 / 0 |
| 第四批模板 / 新行为 / 运行闭包 | 1 / 0 / 1 |
| 第四批未解析字段 / 引用 | 0 / 0 |
| 第五批模板 / 新行为 / 运行闭包 | 1 / 0 / 1 |
| 第五批未解析字段 / 引用 | 0 / 0 |
| C batch 01 模板 / 生成行为 / 运行闭包 | 2 / 2 / 2 |
| C batch 01 未解析字段 / 引用 | 0 / 0 |
| C batch 02 模板 / 生成行为 / 运行闭包 | 2 / 2 / 2 |
| C batch 02 未解析字段 / 引用 | 0 / 0 |
| C batch 03 模板 / 生成行为 / 运行闭包 | 2 / 2 / 2 |
| C batch 03 未解析字段 / 引用 | 0 / 0 |
| C batch 04 新模板 / 具体 Java handler / 运行闭包 | 4 / 4 / 6 |
| C batch 04 XML 击杀行为 / 球形区域 | 2 / 8 |
| C batch 04 未解析字段 / 引用 | 0 / 0 |
| 活动物品任务明确隔离 | 2 |
| `reward_repeat_count` 条件兼容 / 独立语义 | 221 / 44 |
| `bm_restrict_category` 固定客户端 / 真端 | `3477 / 3477`，值域均为 `{1}` |

2026-07-18 第五批收尾复核：`quest_data.batch_05.generated.xml` 通过当前 `quest_data.xsd`；相同冻结输入连续执行两次完整迁移审计，摘要和第五批模板、第五批报告、奖励重复兼容报告、发布清单的 SHA-256 均一致；JDK 25 编译成功，四个定向测试合计 `99/99` 通过；AionEmu 与 `aion_drop` 两个工作树的 `git diff --check` 均通过。

2026-07-19 C batch 01 复核：生成模板与行为通过当前 XSD，规范化内容与正式 XML 一致，`1871/2871` 的模板、行为、必需 NPC spawn、世界 `400010000` 和 GEO 均为 `closed`；JDK 25 `XmlDataLoaderTest` 通过。固定客户端当前目录为 `/Users/mc/IdeaProjects/5.8客户端`，基础 Quest、China Quest 和 world PAK 摘要与冻结基线一致；启动脚本仍为 `-cc:5 -lang:chs`，其当前摘要已显式刷新。完整迁移审计已按该只读输入重新生成 `c_batch_01.json` 和 `release_manifest.json`。

2026-07-19 C batch 02 复核：生成模板与行为通过当前 XSD，忽略排版空白后的结构与正式 XML 一致；`15098/25099` 的模板、`report_to` 行为、必需 NPC spawn、世界与 GEO 均为 `closed`。完整迁移审计结果为 `6464 / 6450 / 3585 / 93`，`released=true`、`common_diff_rows=0`、运行闭包 `2/2 closed`，AI 生产阻断为 0；JDK 25 `XmlDataLoaderTest` 通过。

2026-07-19 C batch 03 复核：生成模板和 `panesterra.xml` 通过当前 XSD，`14210/24210` 的模板、多 NPC `report_to`、8 个 NPC 模板/spawn、五张 world/GEO 均为 `closed`。完整迁移审计结果为 `6466 / 6452 / 3583 / 91`，`released=true`、`common_diff_rows=0`、运行闭包 `2/2 closed`，AI 生产阻断为 0；`80315/80321` 以缺少权威动态出生证据正式隔离。C batch 03 报告、活动物品隔离报告和发布清单摘要分别为 `29958a4debfd24fb8579ae54f378c842277990d6d2aac93ee03317c646c95406`、`3110372920577a9dd0682bc52c87fb1033c946b07c9c1901446c1d8ba44d0542`、`c18aaee43a08d81daa5797dde81673fc66df1f2762532d05da04b97ed031f4a5`；相同冻结输入复跑为 `determinism: OK`。JDK 25 `XmlDataLoaderTest` 为 20/20 通过；全量 `quest_data.xml` 的 XSD 校验仍仅被既有第 3396、3399 行 `exp="581250 "` 尾空格阻断，本批候选模板单独校验通过。

2026-07-19 C batch 04 复核：迁移器 v14 自检通过，相同冻结输入连续两次完整审计输出均为 `6470 / 6456 / 3579 / 87`，候选模板、C batch 04 报告和发布清单摘要逐字节一致。候选模板、`reshanta.xml` 和 `zones_quest.xml` 均通过对应 XSD；JDK 25 `ReshantaQuestMigrationTest` 为 2/2、`XmlDataLoaderTest` 为 20/20。`1867/2852` 的 5 次 PVP、`1868/2868` 的两条八区域顺序、`1869/2869` 的两组各 6 次击杀、NPC/spawn/world/GEO 全部为 `closed`，运行闭包 `6/6`，AI 生产阻断为 0。

### 16.4 运行测试矩阵

每个发布批次至少覆盖：

1. NPC 对话接取和自动接取；
2. 击杀、收集、物品使用和多阶段变量推进；
3. 固定奖励、可选奖励、职业奖励和扩展奖励；
4. 前置、等级、职业、种族、性别、军衔和 faction 限制；
5. 单次、重复、周期、冷却和完成次数；
6. Quest 掉落概率及队伍成员掉落；
7. 放弃、重接、重登恢复和领奖；
8. 固定客户端现有的怪物标记、地图定位和寻路表现只读核对；
9. 服务器重启后 Quest 状态不丢失；
10. 已有当前 Quest 的回归样本。

`can_report=true` 的批次还必须覆盖无可选奖励动作 `108`、可选奖励动作 `110-124`、非 `REWARD` 状态、模板字段为 false、非法动作和领奖失败不落入其他对话分支。

### 16.5 分享专项验收

至少选择以下样本：

- 固定客户端允许、当前服务端禁止；
- 固定客户端禁止、当前服务端允许；
- 单次任务；
- 重复任务；
- 普通队伍和联盟目标类型；
- 接收者等级不足、已接取、已完成、距离过远和飞行状态。

同时记录客户端按钮状态、是否发送 `CM_QUEST_SHARE`、服务端模板值和接收者结果。客户端分享字段与真端 XML 冲突时采用客户端语义，但客户端文件保持不变。

### 16.6 运行目录一致性

正式验证必须比较：

- 源码资源摘要；
- 构建产物摘要；
- 实际运行目录或 JAR 内资源摘要；
- 固定客户端启动脚本、基础 Quest PAK 和 China Quest PAK 的只读摘要；
- 运行 JVM 启动时间与资源生成时间。

这样可以排除“源码已更新，但运行实例仍加载旧 XML”或“分析时使用了错误客户端版本”的假故障。

## 17. 发布与回滚

### 17.1 发布单元

每批发布至少包含：

- Quest 模板变更；
- 对应服务端行为变更；
- 兼容补丁；
- 迁移 manifest 和差异摘要；
- 聚焦测试结果。

发布前先在离线环境完整加载，再在测试服验证真实任务状态；客户端只使用现有固定版本做兼容性观察。

### 17.2 玩家任务状态

- Quest ID 永不重编号。
- 新增 Quest 不需要修改数据库 Schema。
- 修改已有 Quest 的重复次数、前置、奖励或行为时，要验证已有 `START`、`REWARD`、`COMPLETE` 状态。
- 不自动删除玩家 Quest 行；回滚时保留状态，让旧模板重新解释同一 ID。
- 若某字段变化会让进行中 Quest 无法继续，必须提供按 Quest ID 的兼容策略或选择维护窗口。

### 17.3 回滚步骤

1. 停止接受新连接或进入维护状态。
2. 同时恢复上一批服务端 Quest 模板和行为数据。
3. 删除本批派生构建产物，不删除玩家 Quest 数据。
4. 验证上一批 manifest 摘要。
5. 启动并测试代表性旧 Quest、分享和领奖。

禁止只回滚模板或只回滚行为。客户端在整个过程保持不变。

## 18. 风险与控制

| 风险 | 影响 | 控制方式 |
|---|---|---|
| 58Server 数据与固定 5.8 客户端版本不一致 | 显示、限制或行为漂移 | 客户端优先、输入摘要和冲突报告 |
| 把静态文件目录或其他地区 Quest 当成实际客户端 | 候选数量和等级过滤失真 | 冻结 `-cc:5 -lang:chs`、基础 PAK 和 China 覆盖 PAK |
| 只同步 `quest.xml` | 可接但无法推进的半成品 | 服务端模板和行为两路闭包 |
| 动态 `%Quest_*` 宏被当作物品名 | 奖励缺失或生成错误 | 独立宏字典，未解析即阻断 |
| 内部名一对多时选择首个 ID | 击杀、掉落或寻路目标错误 | 歧义阻断，显式展开规则 |
| `cannot_share` 服务端和客户端不一致 | 按钮灰色或点击后无结果 | 采用固定客户端语义修正服务端并做封包验收 |
| `can_report` 只有模板字段、没有状态推进 | 客户端可发起自动领奖但任务仍停在 `START` | 字段能力与 Quest 行为分别验收；未证明 `REWARD` 推进的任务保持隔离 |
| 区域/世界/faction 限制丢失 | 错误接取或跨地图推进 | 未建模字段阻断，先补语义 |
| 共有 Quest 被直接覆盖 | 服务器人工修复丢失 | 全量差异分类、最小兼容补丁 |
| 复杂任务被降级成简单模板 | 任务卡进度或提前完成 | A/B/C/D 严格分级和状态机测试 |
| 护送只有启动、没有到达/丢失或清理 | NPC 永久跟随、任务无法成功或失败 | `ai_closure` 强制启动、双回调和 `FollowingNpcCheckTask` 清理链 |
| 保护任务按名称猜测 handler | 成功/失败事件永远不触发 | 只接受具体回调和分发入口；当前无证据即隔离 |
| 动态生成只实现 `addNewSpawn` | 坐标/数量漂移，失败或放弃后对象残留 | 逐动作校验精确 NPC、Absolute/Relative 基准、数量和超时回收 |
| 把 3,406 个最低等级 999 Quest 当成当前缺失 | 大量无效工作和版本污染 | 维持 E 隔离，除非客户端版本策略明确变化 |
| 固定客户端怪物映射缺失 | 目标无头顶标记 | 记录既有限制，不修改客户端 |
| 客户端定位名与服务端 NPC ID 不一致 | 找不到目标或定位错误 | 以客户端内部名为准修正服务端 ID 和 spawn 关联 |
| 回滚只恢复模板或行为 | 新旧服务端数据混用 | manifest 原子发布和两层回滚 |
| 运行实例仍使用旧资源 | 误判转换失败 | 源码、产物、运行目录和 JVM 时间核对 |

## 19. 发布验收门槛

任一项失败，对应 Quest 或整批不能发布：

- [ ] 输入 manifest 完整，所有摘要与目标版本一致。
- [ ] 启动脚本仍为 `-cc:5 -lang:chs`，基础和 China PAK 摘要与 manifest 一致。
- [ ] 58Server `quest.xml` 的 221 个物理字段全部登记，未知字段为零；客户端同名字段已按权威优先级裁决。
- [x] 共有 Quest 差异已完整生成并分类到报告；当前为 4,757 条、3,300 个 Quest。
- [x] 共有 Quest 的 `cannot_share` 已按固定客户端修正 1,009 个，当前待同步 0。
- [x] 共有 Quest 的 `can_report` 原始 182 个差异已修正，第二批 6 个新增后当前服务端共有 188 个 `can_report=true` Quest，与固定客户端待同步为 0；动作 `108`、`110-124` 的服务端领奖协议已实现。
- [ ] 共有 Quest 的未解释差异为零后，才允许宣称阶段 2 完成。
- [ ] 当前独有 14 个 Quest 全部保留为 server extension。
- [x] 迁移前一级候选 133、A/B/C/D 为 15/38/38/42、E 隔离 39、生产审计池 94 已冻结；首批后剩余 115，第二批后剩余 109，第三批后剩余 99，第四批后剩余 98，第五批后剩余 97，C batch 01 后剩余 95，C batch 02 后剩余 93，C batch 03 后剩余 91，C batch 04 后剩余 87。
- [ ] 3,406 个最低等级 999 Quest 未混入当前发布集。
- [ ] 发布 Quest 的模板字段全部可表达或有已实现、已测试的最小扩展。
- [ ] 发布 Quest 的普通名称引用和动态宏全部解析。
- [ ] Quest、物品、NPC、称号、配方、faction、世界和 spawn 外键完整。
- [ ] 每个发布 Quest 有可执行行为，且 handler 注册无缺失、无冲突。
- [x] AI 审计已覆盖当前 18 个护送 handler，18/18 为 `closed`。
- [x] 保护类保留审计能力；当前具体 handler/分发均为 0，`no_evidence` 只作信息状态，未伪造保护任务。
- [x] 动态生成生产范围 46 条全部为 `closed`；11 条等级 999 动作为 `isolated`；`partial=0`、`blocked=0`。
- [x] 第二批 6 个要塞自动领奖 Quest 的 `siege_report_hunt` 全部为 `closed`，模板、行为、阵营入口、副本目标和 GEO 阻断为 0。
- [x] `bm_restrict_category` 已证明为 BM 账号/包权限位并登记为 `compatible_noop`；固定客户端/真端均为 3,477 个、值域均为 `{1}`，未伪造会员或 VIP 映射。
- [x] 第三批 10 个 Quest 的模板、前置、行为和必需 NPC spawn 均闭包，`released=true`、`closed=10`、未解析字段/引用为 0/0。
- [x] 第四批 `2585` 的客户端 OR 前置已正确保留有效 `4542` 分支，`2055` 维持等级 999 隔离；模板、现有行为和 NPC spawn 闭包为 `1/1 closed`。
- [x] 第五批 `21224` 的 `reward_repeat_count=max_repeat_count=10` 已按现有末次 `extended_rewards` 语义闭包；第 1-9 次、第 10 次和超限行为矩阵通过，模板、行为、NPC/spawn/GEO 闭包为 `1/1 closed`。
- [x] C batch 01 `1871/2871` 的固定客户端与真端三段 Talk 行为一致；复用 `report_to_many`，模板、行为、10 个 NPC spawn、世界和 GEO 闭包为 `2/2 closed`。
- [x] C batch 02 `15098/25099` 的固定客户端与真端零进度同 NPC Talk 行为一致；复用 `report_to`，模板、行为、NPC spawn、世界和 GEO 闭包为 `2/2 closed`。
- [x] C batch 03 `14210/24210` 的真端普通 Talk 行为和两个 `quest_ai_name` 四变体集合已验证；复用多 ID `report_to`，模板、行为、8 个 NPC spawn、五张 world/GEO 闭包为 `2/2 closed`。
- [x] C batch 04 `1867/1868/1869/2868` 及配对旧模板 `2852/2869` 的固定客户端/真端行为一致；5 次 PVP、两条八区域顺序、两组各 6 次击杀、模板、NPC/spawn、世界和 GEO 闭包为 `6/6 closed`，AI 生命周期为 `not_applicable`。
- [x] `80315/80321` 的物品接取行为可表达，但四个必需活动 NPC 无服务端 spawn 或真端静态坐标，已固化为 `2/2 isolated`。
- [ ] 服务端行为与模板的数量、变量和完成条件一致。
- [ ] 发布 Quest 与固定客户端的明确字段和目标关系无冲突。
- [ ] 固定客户端缺少的怪物标记、定位等兼容限制已列出且不伪装为服务端故障。
- [ ] 分享允许/禁止语义以固定客户端为准，服务端校验结果一致。
- [x] 已发布批次的候选 Quest XML、候选行为 XML、`event.xml` 和正式行为 XML 通过 XSD；当前 Quest 总数为 6,470，第二批新增行为 6 条、第三批新增行为 2 条、第四和第五批复用现有行为，C batch 01 复用 `report_to_many`，C batch 02/03 复用 `report_to`，C batch 04 新增 4 个具体 Java handler、2 条 `monster_hunt` 和 8 个区域。
- [x] JDK 25 下 `mvn -DskipTests compile` 通过；`XmlDataLoaderTest` 20/20、`RetailPatternAI2Test` 76/76、`QuestReportEligibilityTest` 2/2、`ReportToTest` 1/1，共 99/99 成功。
- [x] C batch 04 在 JDK 25 下复跑 `ReshantaQuestMigrationTest` 2/2、`XmlDataLoaderTest` 20/20；迁移器自检与两次完整审计均通过，关键生成物摘要一致。
- [x] 已发布批次的生成 Quest 模板、生成行为、`event.xml` 和 `reshanta.xml` 通过对应 XSD；正式全量 `quest_data.xml` 仅被 `HEAD` 已存在的第 3396、3399 行两处 `exp="581250 "` 尾空格阻断，本轮未修改该历史数据，`XmlDataLoaderTest` 已证明当前运行加载通过。
- [x] 相同输入重复生成得到相同输出摘要。
- [x] `41600-41614` 完成 LDF4b 隔离终审；固定客户端世界 ID、关卡资产、目标覆盖和服务端 world/GEO/spawn 证据已固化。
- [ ] 非 LDF4b 的 `50019/51019/80341` 获得不猜值的出生证据并完成真实流程验收；`41615-41622` 保持地图级隔离，不以 GM 强制流程替代自然入口。
- [ ] 实际运行资源摘要与发布 manifest 一致。
- [ ] 回滚包和上一版 manifest 已验证可用。

## 20. 推荐实施顺序

按风险和收益，推荐顺序为：

1. 冻结实际客户端启动参数、基础 Quest PAK、China Quest PAK、58Server 和当前服务端摘要。
2. 实现只读字段注册、名称索引和差异报告，先关闭共有 6,410 个 Quest 的客户端权威差异。
3. 以 18 个无已知模板堵点和测试证据的 A/B Quest 作为首批生产验证集；`9703` 只做隔离环境转换测试。
4. 已完成 6 个 `can_report` Quest 的固定客户端模板/行为、`START -> REWARD`、要塞阵营入口、副本首领和 GEO 闭包，作为第二批发布。
5. 动态生成生产动作已经全部闭包；继续保持 11 条等级 999 动作隔离，保护类在出现具体 handler 前只保留审计能力。
6. 已证明 `bm_restrict_category` 为部署兼容 no-op，并完成第三批 10 个 Quest 的模板、行为和运行 NPC 闭包。
7. 已按客户端 OR 前置语义完成第四批 `2585`，保留有效 `4542` 分支并继续隔离无行为的等级 999 Quest `2055`。
8. 已完成第五批 `21224`：复用现有末次 `extended_rewards` 语义并验证 1-10 次及超限行为。
9. 已完成 `41600-41614` 的 `LDF4b/600030000` 隔离终审；15 个 Quest 不进入发布集，`41615-41622` 保留静态发布限制。
10. 已完成四个 C 批次，C batch 04 的 PVP、八区域状态机和两组击杀均已闭包；`80315/80321` 已按缺失动态出生证据隔离，26 个疑似测试/开发 C 类维持 E 隔离，生产 C 类剩余开发项为 0。
11. 对生产池 31 个 D 类恢复行为证据；11 个 `DataDriven Empty` D 类维持 E 隔离。
12. 完成已发布 Quest 的真实在线角色运行验收，并继续寻找 `50019/51019/80341` 的不猜值入口证据。
13. 3,406 个最低等级 999 Quest 和 120 个明确测试任务不进入当前排期。

## 21. 最终判定

保持当前 Quest 格式同步真端数据是正确方向，也比引入第二套 Quest 引擎或直接读取真端 221 字段风险更低。

可以较高自动化完成的是 Quest 模板字段、常规奖励、限制、掉落、前置和严格 B 类行为；四个生产 C 批次已经全部闭包，当前不能自动承诺完整的是 31 个缺失行为证据的 D 类任务。最后 15 个生产 A/B Quest 已按固定客户端关卡资产缺失正式隔离，不再属于待开发承诺。动态宏、名称索引、faction、`cannot_share`、`can_report`、`bm_restrict_category`、`quest_permitted_worlds` 的本批行为门控和满足条件的末次 `reward_repeat_count` 已确认不再阻塞当前发布。

因此“补全 Quest”应定义为：

```text
模板可加载
+ 服务端行为可执行
+ 全部外键可解析
+ 真实流程可接取、推进、完成、领奖
```

只有同时满足这四项，才计为服务端已迁移 Quest。固定客户端是只读的最终语义基准：发生异常时按客户端修正服务端；客户端自身缺少标记或定位证据时记录限制，不修改客户端。

当前结论是：技术路线可行，转换器、分阶段覆盖基线、共有 Quest 分享/上报字段同步、自动领奖通用能力、BM 兼容 no-op、首批静态发布、第二批 6 个要塞自动领奖 Quest、第三批 10 个普通生产 Quest、第四批 `2585`、第五批 `21224` 和四个 C 批次运行闭包、护送和动态生成 AI 审计，以及 LDF4b/活动物品任务隔离终审均已落地。动态生成 46 条生产动作、`siege_report_hunt` 6 条链、第三批 10 条、第四批 1 条、第五批 1 条、前三个 C 批次各 2 条和 C batch 04 的 6 条运行闭包均已清零阻断，`2055`、`41600-41614`、`80315/80321` 与其余等级 999 内容按证据隔离。46 个 Quest 已从候选转为正式模板，发布后剩余一级候选为 87。下一步处理 31 个 D 类缺失行为证据；并行完成已发布 Quest 的真实在线运行验收和 `50019/51019/80341` 的入口取证。保护类在具体 handler 和事件分发入口均为 0 的现状下不得猜测实现；3,406 个等级 999 Quest 全部后置。
