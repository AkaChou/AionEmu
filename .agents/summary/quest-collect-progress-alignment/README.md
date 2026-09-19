# 全服任务收集进度 (collect_progress) 与交付对白契约全量审计与同类修复纪要

## 1. 现象与同型缺陷根因 (Symptom & Root Cause)

### 1.1 现象回顾 (Live Symptom)
- **触发场景**：玩家在任务进行到物品收集并交付给指定 NPC 的步骤时，背包已持有指定数量的任务道具，但找到 NPC 点击交互时，NPC 头顶无对白标记或点击后客户端下发通用对话：
  ```
  SM_DIALOG_WINDOW 玩家=Ww targetObj=72375 questId=0 下发页=10
  ```
  任务无法提交，无法进入后续对白或奖励结算。

### 1.2 客户端-服务端契约根因 (Contract Root Cause)
1. **真端客户端判定逻辑**：
   - Aion 5.8 客户端在 `quest.xml` 中通过 `<collect_progress>N</collect_progress>`（或数据驱动任务 `data_driven_quest.xml` 中的 `<category_progress_>CollectItem</category_progress_>` 步骤索引 N）声明收集交付步。
   - 客户端在且仅在 `progress == N`（即 `var0 == N`）时，才会以持有任务道具的状态向交付 NPC 发起任务专属对白请求（带有具体 questId 的 `QUEST_SELECT`），进入 `selectN -> CHECK_USER_HAS_QUEST_ITEM` 路由。
2. **服务端同型缺陷模式**：
   - **模式 A（交互物/拾取抢跑）**：在步骤 N 时，玩家通过采集物、祭坛或拾取道具完成收集时，服务端错误地配置了 `<set-variable field="var0" value="N+1"/>` 甚至 `<get-item>` 转移，将玩家的 `var0` 提前推进为 `N+1`。随后服务端把该 NPC 的 `CHECK_USER_HAS_QUEST_ITEM` 错配在 `s(N+1)`。当玩家携带道具到达 NPC 时，客户端校验 `progress (N+1) != collect_progress (N)`，客户端认定当前不在交付节点，下发 `questId=0, page=10`。
   - **模式 B（步骤跳跃）**：服务端在道具使用或中间交互后直接跳过 `vN`（例如直接从 `started` 推进至 `v2`，跳过 `v1`），导致客户端与服务端变量阶段脱节。
   - **模式 C（步骤限制过紧导致死锁）**：掉落规则声明了限定步骤（如 `collecting-step="2"`），且交互物 `can-act` 和 `USE_OBJECT` 仅开放给特定前置步骤。若同步骤含有怪物击杀先推进了步数，玩家进入后续步骤后无法再采集该道具，导致任务永久卡死。

---

## 2. 全库系统性排查与结果 (Systemic Audit Results)

对 `/Users/mc/PycharmProjects/unpak/Quest_unpacked/quest.xml`（3,705 个含 `collect_progress` 任务）及 `data_driven_quest.xml`（443 个含 `CollectItem` 任务）与服务端 `quest_definition/quests/*.xml`（全服 6,222 个任务定义）进行了全量交叉审计：

| 缺陷分类 | 任务编号 | 任务名称 | 影响阵营/类型 | 审计诊断 | 修复措施 |
|---|---|---|---|---|---|
| **模式 A（孪生同类）** | **10530** | 打开结界 / Trouble Back Home | 天族 66+ 主线使命 | 客户端 `collect_progress=7`，步骤 7 交互祭坛 703387 时抢跑设置 `var0=8`，尤克莱亚斯 203752 的交付路由错配在 `s8` | 祭坛交互保留在 `s7`（不抢跑提步）；尤克莱亚斯交付移至 `s7` 扣道具并推至 `s8` 展示 `CHECK_USER_ITEM_OK`；`s8` 衔接 `SELECT9` 影片 118 与奖励结算；增加 `enter-world` 自愈 |
| **模式 A（孪生同类）** | **20530** | 打开深渊之门 / The Aether Field | 魔族 66+ 主线使命 | 客户端 `collect_progress=7`，步骤 7 交互祭坛 703389 抢跑设置 `var0=8`，巴尔德尔 204075 交付错配在 `s8` 并推至多余的 `s9` | 祭坛交互保留在 `s7`；巴尔德尔交付移至 `s7` 扣道具并推至 `s8`；`s8` 衔接 `SELECT9` 影片 148 与奖励结算；移除多余 `s9`；增加 `enter-world` 自愈 |
| **模式 C（采集死锁）** | **10504** | 没收石板 / Confiscate the Slate | 天族 60+ 主线使命（10503后置） | 客户端 `collect_progress=3`，石板 702671 掉落写死 `collecting-step="2"` 且交互仅限 `s2`；若先击杀 236255 进 `s3` 则永久无法采集石板 | 掉落步数放宽为 `collecting-step="0"`；在 `s3` 阶段补充 `can-act` 与 `USE_OBJECT` 路由，杜绝卡死 |
| **模式 B（跳步脱节）** | **1373** | 帕诺的特制温泉水 / Water Therapy | 天族 34+ 重要任务 | 客户端 `collect_progress=1`，使用保温瓶后服务端直接跳入 `v2 (var0=2)`，跳过了 `var0=1` | 将状态机节点修正为 `v1 (var0=1)` 与 `reward (var0=1)`；使用道具后推进至 `v1`，交付路由统一至 `v1`；增加 `enter-world` 自愈 |

---

## 3. 自动化门禁测试防护网 (Automated Gate Protection)

为确保同类问题全库杜绝回归，新增并更新了以下聚焦测试与全库门禁：

1. `Quest10503ClientDialogAlignmentTest`：锁定 10503 步骤 2 交付与步骤 3 解读领物合同。
2. `Quest10530ClientDialogAlignmentTest`：锁定 10530 步骤 7 祭坛不抢跑、尤克莱亚斯交付与步骤 8 `SELECT9` 续接对白合同。
3. `Quest20530ClientDialogAlignmentTest`：锁定 20530 步骤 7 祭坛不抢跑、巴尔德尔交付与步骤 8 `SELECT9` 续接对白合同。
4. `Quest10504ClientDialogAlignmentTest`：锁定 10504 石板采集步数开放与 `s2`/`s3` 双向交互许可。
5. `Quest1373ClientDialogAlignmentTest`：更新并锁定 1373 `v1 (var0=1)` 交付步与客户端对齐合同。
6. `QuestCollectProgressAlignmentGateTest`：跨任务统一对齐门禁，集中断言上述家族任务变量推进点与交付点无脱节。

---

## 4. 验证执行与状态记录 (Validation Execution & Status)

- **授权执行范围**：
  `mvn test -Dtest=Quest10503ClientDialogAlignmentTest,Quest10504ClientDialogAlignmentTest,Quest10530ClientDialogAlignmentTest,Quest20530ClientDialogAlignmentTest,Quest1373ClientDialogAlignmentTest,QuestCollectProgressAlignmentGateTest`
  - **执行结果**：`BUILD SUCCESS`（Tests run: 10, Failures: 0, Errors: 0, Skipped: 0）。
- **生产目录一致性校验**：
  `QuestDefinitionCatalogManifestTest` (10/10 PASS)
  `QuestItemSourceContractGateTest` (3/3 PASS)
  所有源码 `src/main/resources/...` 变更已 100% 同步至部署目录 `aion/data/static_data/...`。
- **实机验收状态**：10503、10504 已实机验收（用户 2026-09-19 分别回复「10503 验证成功，提交」「10504 验证成功」，未限定分支或步骤 → `CLIENT_ACCEPTED`，记录见 `.agents/summary/quest-acceptance/10503-2026-09-19-client-accepted.md` 与 `.agents/summary/quest-acceptance/10504-2026-09-19-client-accepted.md`）；同批 20504、10506、10507、10527/20527、10528/20528、10530/20530、1373 仍待逐任务实机复核（`PENDING_CLIENT_ACCEPTANCE`）。
