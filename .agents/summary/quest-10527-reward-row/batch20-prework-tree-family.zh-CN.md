# 批次 20 开工证据：焦树族 23809 / 13809（三棵 DeadTree + 领奖 owner 收敛）

> 状态：**EVIDENCE_ONLY（未改动任何 XML）**，2026-09-22。批次 19（15000/15670）已提交 `ee90f9391`。

## 一、族级判据命中情况

`audit-output.tsv` 与镜像列交叉：`23809`（天族侧落后）`ROW_BEHIND / MISSING_TAIL_ROWS / ROW_WITHOUT_STATE`（`visible=0`、缺行 1 2 3）；
镜像 `13809` 审计为 `ROW_ALIGNED / ALIGNED / ROW_STATE_ALIGNED`（`visible 0 1 2 3`、reward=3）。两侧客户端 `quest_summary` 均 4 行且同形。

## 二、迁移前 legacy handler（`git show '7e9f0316c^:...'`，两侧逐行同形）

`kaldor/_23809Scar_Of_The_Past.java`（天）与 `kaldor/_13809Tree_Is_Company.java`（魔）：

```
register: 802429(Vidarr) / 802427(Caetess) 接取+对话；730969 Scorched / 730970 Cindery / 730971 Burnt 三棵树
START:
  730969 USE_OBJECT -> useQuestObject(env, 0, 1, false, 0)   // 只写 var0，不置 REWARD
  730970 USE_OBJECT -> useQuestObject(env, 1, 2, false, 0)
  730971 USE_OBJECT -> useQuestObject(env, 2, 3, false, 0)
  802429/802427 START_DIALOG -> sendQuestDialog(env, 2375)
  802429/802427 SELECT_REWARD -> changeQuestStep(env, 3, 4, true)  // REWARD + packed step 留在 3
REWARD: 802429/802427 -> sendQuestEndDialog
```

结论：**阶梯 = started(0) --树a--> s1(1) --树b--> s2(2) --树c--> reward(3)**，领奖行 3 = 向 Bella/守卫报告，
owner 唯一 = 802429（23809）/ 802427（13809）；`changeQuestStep(3->4, true)` 与 QE-051 同形（`to` 被旧 helper 丢掉，领奖态保持 packed 3）。

## 三、客户端页动作（`Dialogs/**/quest_q23809.html` / `quest_q13809.html`，两侧同形）

| 页面 | 按钮 | 语义 |
|---|---|---|
| `select1`（accept=true） | `HACTION_QUEST_ACCEPT_SIMPLE` / `HACTION_QUEST_REFUSE_SIMPLE` | 接取/拒绝 |
| `select2` | `HACTION_SETPRO1`(10000) | 调查烧焦的树（730969）后“结束调查” |
| `select3` | `HACTION_SETPRO2`(10001) | 调查烧剩的树（730970） |
| `select4` | `HACTION_SETPRO3`(10002) | 调查烧成灰烬的树（730971） |
| `select5` | `HACTION_SELECT_QUEST_REWARD`(1009) | 向领奖 NPC 报告并领奖 |

客户端 `quest_summary` 行 0/1/2/3 = 调查树 a / 树 b / 树 c + 采集 `quest_13809a|b|c`、行 3 = 向
`STR_DIC_N_LDF5_Fortress_Village_Guard01_D`（23809）/ `..._Guard01_L`（13809）报告。三棵树 = **同一组世界物件 730969/730970/730971**
（客户端 `client_npcs_npc.xml` 的 `LDF5_Fortress_FOBJ_B1_DeadTree_a|b|c`，天/魔共用）。

## 四、当前 XML 的缺陷（两侧都要改，属“阶梯 + QE-052 owner 收敛”复合）

1. **23809 塌陷**：`NPC_START` 挂在三棵树（730969/970/971）上，每棵树既有 `started -> reward` 的 `SETPRO1` 直跳、
   又有 `SELECT_QUEST_REWARD` 的整包交付（条件 3 件采集物 182215493/494/495），并且每棵树各有一条 `npc-complete`
   ——即“行 0 交互对象兼任接取与领奖”的典型 QE-052 冗余；行 1/行 2 没有状态（审计缺行 1 2 3）。
2. **13809 阶梯已对，但 owner 同样冗余**：`NPC_START` 与 `npc-complete` 各 4 条（802427 + 三棵树），
   需按 QE-052 收敛到 802427 一条；另有 `started -> stage1` 经 `802427 SETPRO1` 的重复边，需按客户端页动作链去重。

## 五、建议的改动模板（照批次 19 的做法）

* 节点：`unaccepted(0) / started(0) / stage1(1) / stage2(2) / reward(3) / complete(0)`，progress `var0` 保持 `width=3`（0..4）。
* 路由（每棵树两条：`USE_OBJECT` 显示该树页面 + `SETPROx` 推进一格）：
  * `started --TalkToNpc(730969, USE_OBJECT)--> started`（`SHOW_QUEST_PAGE SELECT2`）
  * `started --TalkToNpc(730969, SETPRO1)--> stage1`（`PACKET_ONLY`）
  * `stage1 --TalkToNpc(730970, USE_OBJECT)--> stage1`（`SELECT3`）+ `--SETPRO2--> stage2`
  * `stage2 --TalkToNpc(730971, USE_OBJECT)--> stage2`（`SELECT4`）+ `--SETPRO3--> reward`
* 领奖：`npc-complete npc-id=802429(23809)/802427(13809) source=reward target=complete`，
  `reward --SELECT_QUEST_REWARD--> reward` 由 preview 展开，**不要显式重复声明**（批次 19 的 AMBIGUOUS_TRANSITION 教训）。
* 旧存档自愈：`REWARD && var0==0/1/2 -> 3` 的 enter-world 边（塌陷定义会把“在三棵树处一次性交付”的玩家写成 `REWARD + var0=0`）。
* 采集物 182215493/494/495 的 `drops` 需按行号核对（当前 23809 的交付条件挂在树上的整包判定要拆到行 1/行 2）。

## 六、开工前必须确认的两点

1. `LDF5_Fortress_Village_Guard01_D` / `..._L` 的 npc id 与 802429/802427 是否为同一实体
   （legacy 里只有 802429/802427 参与 `setStatus(REWARD)`，客户端行 3 却写 Guard01；若不同体，
   需按 QE-052 用客户端行内命名收敛 owner，并在回归门禁里锁死）。
2. 13809 侧“已对齐”是否依赖树上的 `npc-complete`（`RewardNpcOwnershipContractTest` 现有断言会不会覆盖这两个任务）；
   收敛 owner 前先跑一次该门禁确认基线。
