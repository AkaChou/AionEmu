# 2026-09-17 任务影片无限循环与推进假死重复对话专项根治总结

## 1. 背景与排查维度

根据用户反馈与指令：
> “继续挖掘其他维度，例如看完电影没有下一步，一直无限看电影，对话一直重复无法下一步”

结合历史修复记录与全服 6,222 个任务 XML 静态数据，我们通过编写数学拓扑与契约图遍历审计脚本，对全服任务展开了多维度的全局深挖，锁定了两大核心致命缺陷模式：

### 维度一：看完电影没有下一步 / 无限看电影死循环
- **病灶特征**：
  1. 玩家与 NPC 对话触发 `<play-movie>` 播放剧情影片，但该转换的 `source == target` 且没有 `<actions>`（未变更任何变量）、没有后续的 `<movie-end>` 事件推进、也没有下发目标客户端对话页面（直接 `<close-dialog/>`）。玩家看完影片后任务状态与变量纹丝未动，再次点击该 NPC 又重新从头开始播放电影，陷入无限看电影死循环；
  2. 任务使命中在非击杀目标阶段错配了怪物击杀播放电影事件（如 14047），导致只要击杀该怪（或队友击杀、或交付前怪物刷新被杀），就会被无条件强制拉入观影循环，且步数毫无推进。

### 维度二：对话一直重复 / 无法下一步（伪推进假死）
- **病灶特征**：
  1. 玩家点击业务推进选项（如 `SETPRO*`、“我知道了”、“继续”等客户端强推进动作），或者在未接取阶段点击“接受任务”（`QUEST_ACCEPT_1`），服务端接收到动作后，转换配置为 `source == target` 且没有任何 `actions`，甚至直接 `<close-dialog/>`。
  2. 玩家交互完毕后对话关闭，但客户端的任务进度、步骤提示和勾选状态完全停在原地；玩家再次点击该 NPC，由于当前状态未流转，NPC 又完完整整重新下发最初的对话，形成永远无法完成这一步的重复对话假死；
  3. 后续击杀怪或收集物品的逻辑被倒挂在初始状态，破坏了客户端界面步骤提示与真端节奏。

---

## 2. 本轮根治任务清单与真端依据

| 任务 ID | 任务名称 | 所属阵营/类型 | 缺陷病灶 | 真端规范与修复策略 |
|---|---|---|---|---|
| **16942** | Foundry Finds (巴鲁纳次元研究所) | 天族 / 重要 | `started` 下 NPC 206361 的 `SETPRO1` 播 movie 899，原写为 `started -> started` 且直接 `close-dialog`，无 actions 推进；打怪收集误写为 `collecting-step="0"` | 补充 `s1` 节点（`var0=1`）；SETPRO1 播放影片后推进到 `s1`；打怪掉落收集步数修正为 `collecting-step="1"`，千夫长交付报告源由 `started` 调整为 `s1`；在 `s1` 增加 206361 防重复观影保护 |
| **26942** | A Jump Ahead (巴鲁纳次元研究所) | 魔族 / 重要 | `started` 下 NPC 206362 的 `SETPRO1` 播 movie 900，原写为 `started -> started` 且直接 `close-dialog`，无 actions 推进；打怪收集误写为 `collecting-step="0"` | 补充 `s1` 节点（`var0=1`）；SETPRO1 播放影片后推进到 `s1`；打怪收集与千夫长交付调整为 `s1`；在 `s1` 增加 206362 防重复观影保护 |
| **14047** | Chaining Memories (束缚的记忆) | 天族 / 使命 | 在 `started, s1, s2, s3, s4, s6` 共 6 个非目标阶段错配了 `kill-npc 214599` 播放 movie 422（target=source 无推进） | 仅在目标击杀阶段 `s5 -> s6`（消灭伊卡罗尼斯 214599）保留合法的状态流转与电影 422 播放，彻底清除其余 6 个阶段错配的观影垃圾路由 |
| **14112** | Pollution Resolution (解决污染) | 天族 / 普通 | 接取后第 1 步在 NPC 203148（莱费奥斯）处选 `SETPRO1`，原写为 `started -> started` 直接关窗，任务目标无法从“和莱费奥斯对话”推进，再次点击重复对话；击杀斯拉希被倒挂在 started | 将 203148 的 `SETPRO1` 修正为流转至 `k1`（`var0=1`）并发放任务工作道具 182215455；在 `k1` 消灭斯拉希召唤卡托并流转至 reward，彻底打通剧情 |
| **24155** | Leather Wings and Shiny Things (修复结界塔) | 魔族 / 普通 | 接取后第 1 步在 NPC 204785（格温杜尔林）处选 `SETPRO2`，原写为 `started -> started` 直接关窗，目标无法划勾推进；杀怪链错误从 started 启动 | 将 204785 的 `SETPRO2` 修正为流转至 `k1`（`var0=1`）；杀怪链调整为 `k1 k2 k3`；在 `k1` 增加 204785 提示去破坏装置的防重复对话 |
| **28301** | Power On (启动动力) | 魔族 / 重要 | 水晶球打完进入 `k7` 后，面对破坏兵器胡根掉落的动力装置（730373/730374/799530），点击 `SETPRO2`（“把动力装置捡起来”），XML 居然在 started 下写了关窗且 k7 缺失路由，导致玩家面对动力装置无法捡起、无法下一步 | 对齐天族 18301，在 `k7` 为动力装置 NPC 配置 `SETPRO2` -> `reward`，发放动力装置道具 182212110 并下发领奖窗口，彻底解决动力装置捡不起来的卡死 |
| **50010** | [Event] Lover or Loner? ([活动] 恋爱还是单身？) | 天族 / 活动 | 在 `unaccepted` 选单身阵营（`SETPRO2`），原 XML 居然写为 `target="unaccepted" close-dialog`，导致单身玩家永远接不上任务，无限重复对话问“恋爱还是单身” | 将 `unaccepted` 下选择单身（`SETPRO2`）修正为接取并流转至 `v2`（`var0=2`）；同时将情侣（`SETPRO1`）直接接取流转至 `v1`（`var0=1`），彻底修复单身路线死锁 |
| **15301** | Two Steps Closer (更近一步) | 天族 / 圣灵装备 | `unaccepted` 下手写覆盖了错误的显式 transition：点 `QUEST_ACCEPT_1` 目标居然是 `unaccepted` 并试图跳向不存在的页面 `QUEST_1_2` | 将 `QUEST_ACCEPT_1` 修正为转移到 `started` 并展示 `QUEST_ACCEPT_1` 页面；将 `QUEST_REFUSE_1` 修正为展示 `QUEST_REFUSE_1` 页面 |
| **25301** | Two Steps Closer (更近一步) | 魔族 / 圣灵装备 | 同 15301，手写显式 transition 破坏了接取流程 | 将 `QUEST_ACCEPT_1` 修正为转移到 `started` 并展示 `QUEST_ACCEPT_1` 页面；将 `QUEST_REFUSE_1` 修正为展示 `QUEST_REFUSE_1` 页面 |
| **1423** | Expert Advice (专家的忠告) | 天族 / 重要 | 玩家听完上级飞行术说明后在接取页点击 `SETPRO1` 结束对话，原写为 `started -> started` 且无 actions，导致第 1 步无法划勾，无法向马拉娜交付领奖 | 将 `started` 下马拉娜（203983）的 `SETPRO1` 修正为转移至 `reward`（`var0=1`），听完说明后可直接进入领奖交付 |

---

## 3. 全局防线与回归门禁建设

为从根本上杜绝“看完电影没有下一步、无限看电影”与“对话重复无法下一步”同类问题再次产生，新增专项测试门禁：
`src/test/java/com/aionemu/gameserver/questEngine/definition/QuestMovieAndDialogLoopRegressionTest.java`

门禁覆盖点包括：
1. **全服纯电影死循环防护 (`executableQuestsHaveNoPureMovieLoops`)**：
   - 遍历全服所有生产可执行任务（除白名单 24053 历史 fallback 兜底外）；
   - 断言：任何包含 `PlayMovie` 的转换，严禁出现 `source.equals(target)` 且 `actions` 为空、且无后续 `MovieEnd` 推进、且无对话页展示（`ShowQuestDialog`）的孤立播放循环；
   - 经本轮修复后，全服 6,222 个任务全量扫描结果：**0 纯电影死循环**！
2. **专项目标与击杀/推进契约断言**：
   - 覆盖 16942/26942（影片后步数进位至 s1/var0=1）；
   - 覆盖 14047（非目标阶段严禁错配 214599 乱播电影）；
   - 覆盖 14112, 24155, 28301, 50010, 15301, 25301, 1423 的业务推进与奖励流转。

---

## 4. 架构沉淀

沉淀架构模式 `[QE-024]` 二十二、任务影片循环与推进假死重复对话防线 (MOVIE_LOOP_AND_PROGRESS_STALL_FREE) 至：
- `.agents/memory-bank/patterns/quest-engine.md`
- `.agents/memory-bank/systemPatterns.md`
- `.agents/memory-bank/symptom-index.md`
