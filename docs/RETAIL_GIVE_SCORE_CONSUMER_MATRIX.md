# 真端 `give_score` 副本消费者矩阵

数据口径：`coverage.xml` 登记的 139 个 world，合并静态出生与条件出生，按 `npc-ai.xml` 解析 Pattern，再连接 `npc-scores.xml`。自动锁定见 `RetailGiveScoreClosureTest`。

当前基线：13 个 world、38 个唯一 NPC、98 条 world/event 绑定；事件为 64 `on_killed_by_user`、25 `on_talked_by_user`、6 `on_die`、3 `on_user_enter_sensory_area`。分值类型全部为 `NPC`；96 条 apply type 0、1 条 type 1、1 条 type 2；`equalizing_score` 全部为 0。

## 消费者结论

| 地图族 | world | 结论 | 既有计分所有者 / 风险 |
|---|---|---|---|
| Harmony | 300450000 / 300570000 / 301100000 | 25 个可达计分 NPC 全部接入 | lever、技能 buff、感知 coin 和击杀 NPC 走队伍积分、稳定出生键/对象 ID 幂等、运行时持久化和封顶结算；`RetailPatternAI2` 在 `on_talked_by_user` 技能完成后续接 `give_score → despawn_self`；`onDie` 与 `handleUseItemFinish` 已对全部 Pattern 所有对象退出。 |
| Chaos / Discipline / Glory | 300350000 / 300360000 / 300420000 / 300430000 / 300550000 | 7 个非零 Pattern 计分 NPC 已接入 | `207102`、`243675/243676` 与 `701173/701174/701187/701188` 走个人积分、稳定出生键/对象 ID 幂等、运行时持久化和封顶结算；`onDie` 仅对已实际装配 `RetailPatternAI2` 的计分对象退出，保留 Pattern 回退和不重叠的旧击杀桥接；`701169..701172`、`701212` 及采集/飞行环继续由既有 Handler 消费。 |
| Sealed Danuar Mysticarium | 300480000 | 无有效分值 | `831147` 的真端值为 0；没有接入收益，不新增消费者。 |
| Kamar | 301120000 | 8 个交互计分 NPC 已接入 | `730861/730878/801766/801767/801818/801819/801820/801821` 仅由 Pattern `on_talked_by_user` 触发，走战斗期校验、稳定出生键/对象 ID 幂等、运行时持久化和封顶结算；`onDie` 继续负责没有 Pattern 计分入口的击杀 NPC。 |
| Idgel Dome Landmark | 301680000 | 2 个终局计分 NPC 已接入 | 条件链 `OperCompleted_Li_04/Da_04 >= 7` 生成 `833914/833922`，分别固定 apply type 1/2、30,000 分；消费者按稳定出生键/对象 ID 幂等并持久化队伍分。旧 `onDie` 仅处理不重叠的 `243965/243966` 普通怪各 50 分，继续保留。 |
| Tournament | 302380000 / 302410000 | 继续回退 | world 由锦标赛系统而非普通 Instance Handler 所有；当前没有可验证的 Pattern 计分消费者。 |

## 完整绑定

字段：`NPC / Pattern / event / target / score type / apply type / value`。

### 300350000

- `207102 / IDArena_pvp02_S1_buff01_46 / on_talked_by_user / USERI_TALKER / NPC / 0 / 400`

### 300360000

- `207102 / IDArena_pvp02_S1_buff01_46 / on_talked_by_user / USERI_TALKER / NPC / 0 / 400`
- `243675 / IDArena_pvp02_S3_Brax_03 / on_die / USERI_EVENT_MAKER / NPC / 0 / 100`
- `243676 / IDArena_pvp02_S3_Tog_03 / on_die / USERI_EVENT_MAKER / NPC / 0 / 100`

### 300420000

- `207102 / IDArena_pvp02_S1_buff01_46 / on_talked_by_user / USERI_TALKER / NPC / 0 / 400`

### 300430000

- `207102 / IDArena_pvp02_S1_buff01_46 / on_talked_by_user / USERI_TALKER / NPC / 0 / 400`
- `243675 / IDArena_pvp02_S3_Brax_03 / on_die / USERI_EVENT_MAKER / NPC / 0 / 100`
- `243676 / IDArena_pvp02_S3_Tog_03 / on_die / USERI_EVENT_MAKER / NPC / 0 / 100`

### 300450000 / 300570000 / 301100000

以下每条在三个 world 中各出现一次：

- `207099 / IDArena_Team01_S1_lever_01 / on_talked_by_user / USERI_TALKER / NPC / 0 / 200`
- `207101 / IDArena_Team01_S2_Coin_01 / on_user_enter_sensory_area / USERI_EVENT_MAKER / NPC / 0 / 100`
- `207102 / IDArena_pvp02_S1_buff01_46 / on_talked_by_user / USERI_TALKER / NPC / 0 / 400`
- `207116 / IDArena_Team01_S1_lever_02 / on_talked_by_user / USERI_TALKER / NPC / 0 / 400`
- `207117 / IDArena_Team01_S1_lever_03 / on_talked_by_user / USERI_TALKER / NPC / 0 / 400`
- `219277,219278,219279,219648,243678 / IDArena_team01_S5_Roamer / on_killed_by_user / USERI_EVENT_MAKER / NPC / 0 / 400`
- `219280,219281,219282,219649,243679 / IDArena_team01_S5_Named / on_killed_by_user / USERI_EVENT_MAKER / NPC / 0 / 100`
- `219283,219284,219285,219650,243680 / IDArena_team01_S6_BatsA / on_killed_by_user / USERI_EVENT_MAKER / NPC / 0 / 50`
- `219328 / Test_Basic_Monster_AI_KSG_1 / on_killed_by_user / USERI_EVENT_MAKER / NPC / 0 / 50`
- `219481,219485,219486,219652 / Test_Basic_Monster_AI_KSG_1 / on_killed_by_user / USERI_EVENT_MAKER / NPC / 0 / 400`

### 300480000

- `831147 / IDLDF5Re_solo_scoreNPC / on_talked_by_user / USERI_TALKER / NPC / 0 / 0`

### 300550000

- `243675 / IDArena_pvp02_S3_Brax_03 / on_die / USERI_EVENT_MAKER / NPC / 0 / 100`
- `243676 / IDArena_pvp02_S3_Tog_03 / on_die / USERI_EVENT_MAKER / NPC / 0 / 100`

### 301120000

- `730861 / IDKamar_IdgelMachine / on_talked_by_user / USERI_TALKER / NPC / 0 / 200`
- `730878 / IDKamar_IdgelMachine_25 / on_talked_by_user / USERI_TALKER / NPC / 0 / 200`
- `801766,801767,801818,801819,801820,801821 / Rune_FrostNmd_PazeCheck_NoShow_Sum4 / on_talked_by_user / USERI_TALKER / NPC / 0 / 225`

### 301680000

- `833914 / IDLDF5_Fortress_Re_N_MaBattery_Li_04_Comp / on_killed_by_user / USERI_KILLER / NPC / 1 / 30000`
- `833922 / IDLDF5_Fortress_Re_N_MaBattery_Da_04_Comp / on_killed_by_user / USERI_KILLER / NPC / 2 / 30000`

### 302380000 / 302410000

以下绑定在两个 world 中各出现一次：

- `219328 / Test_Basic_Monster_AI_KSG_1 / on_killed_by_user / USERI_EVENT_MAKER / NPC / 0 / 50`

## 接入规则

- Pattern 结构支持不代表可接入；`supportsRetailNpcScore` 必须按 world 的 Handler 显式白名单放行。
- 击杀事件在 Handler 已读取 `npc-scores.xml` 时，Pattern 必须继续回退，直到旧击杀桥接在同一地图族中删除。
- 交互/感知事件必须以 NPC 对象 ID 或稳定出生键幂等，并持久化到 `InstanceRuntimeState`。
- apply type 必须由地图既有积分模型明确解释；未知类型或 `equalizing_score != 0` 一律拒绝。
- 每个地图族按测试锁定、实现、删除旧桥接独立提交；无法证明的绑定保持 `missing score consumer` 回退。
