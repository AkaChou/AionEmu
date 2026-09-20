# 10520/20520 高阶守护者 65→66 经验门禁与伊鲁玛/诺斯珀德入场门禁

日期: 2026-09-20
范围: ELYOS 10520 `Covert Communiques`（遗失的记忆）与 ASMODIAN 20520 `Lost Destiny` 的同一合同
状态: 实现完成，验证 PENDING（未获 Maven 构建授权）

## 玩家需求

1. 未完成 10520 前不得升到 66 级；10520 完成会在同一事务内晋升到 66 级起始状态。
2. 10520 的 `SETPRO4` 把玩家传送到 210100000（Iluma，任务唯一的新大陆传送目标）后，本阵营才允许进入该世界；此前任何常规路径都不得进入。

## 证据链

- `src/main/resources/aion/data/static_data/quest_definition/quests/10520.xml`: `s3 -> s4` 的 `SETPRO4` 执行 `set-variable var0=4`、移除信物，并在 after-commit 中 `teleport-player-current-or-default world-id="210100000"`；20520 对称使用 `world-id="220110000"`（Norsvold）。
- `src/main/resources/aion/data/static_data/player_experience_table.xml`: 66 级起始经验为 `2066885620`（注释 `Level 65` 的上一条是 65 级起始经验 `1926765410`）。
- `PlayerCommonData.setExp` 旧逻辑把 65 级未完成任务的 `maxExp` 设为 `2066885620`，恰好等于 66 级起始经验；随后的 while 用 `exp >= startExp(level+1)` 升级，导致上限本身就能把玩家升到 66。
- `npc_teleporter.xml` 已有部分数据门禁：203726 的 loc 444（伊鲁玛）为 `requiredQuest=10520 requiredQuestStep=4`，204191 的 loc 438（诺斯珀德）为 `requiredQuest=20520 requiredQuestStep=4`；804561/802452 等更严格路线仍为完成后才开放。
- 仍存在旁路：伊鲁玛/诺斯珀德返回卷轴 `164000405/164000406`（`return_world=210100000/220110000`）、`multi_returns.xml` 的 6/7 号目的地、`PortalService`、通用 `TeleportService2.teleportTo` 和副本/AI 直达传送，原先只校验欧比斯/巴劳雷亚/哥尔哈。

## 修复

- `PlayerCommonData`: 在线且本阵营入场任务未完成时，把 65→66 边界上限改为 `startExp(66) - 1`；离线加载不参与，避免任务状态恢复前改写存量经验。新增 `isArchDaevaLevelCapped()` 统一判断。
- `TeleportService2`: 新增 210100000/220110000 与本阵营 10520/20520 step 4 的合同；`isArchDaevaEntryWorld`、`meetsArchDaevaEntryRequirement`，并在 NPC 传送员、完整 `teleportTo`（GM 放行）、传送门路径三处校验。敌对阵营沿用裂隙既有规则，不在本门禁中拦截。
- `PortalService`、`CM_USE_ITEM`、`MultiReturnAction`: 在欧比斯/巴劳雷亚/哥尔哈校验旁补齐本阵营新大陆门禁，封住卷轴与多目的地返回的旁路。

## 测试

- `PlayerCommonDataArchDaevaTest`: 新增 `incompleteArchDaevaQuestCapsExpBelowLevel66`（66 级起始经验及更高经验均停留在 65 级与 `startExp(66)-1`）和 `onlyTheOwnRacialQuestCompletionReleasesTheLevelCap`。
- `ArchDaevaEntryRequirementTest`: 新增本阵营 step 4/REWARD/COMPLETE 放行、早期步骤/错任务/null 拒绝、敌对阵营不被误拦、203726/204191 数据门禁、返回卷轴目标世界锁定。
- IDE 静态检查（scope 为本次修改的 7 个文件，errors only）: 0 项。
- Maven: **未执行**，等待授权。建议最小命令:
  `mvn -Dtest=PlayerCommonDataArchDaevaTest,ArchDaevaEntryRequirementTest,QuestArchDaevaPromotionDefinitionTest,Quest10520ClientDialogAlignmentTest,TeleportServiceQuestRequirementTest,KahrunEntryRequirementTest test`

## 风险与未闭环项

- 存量数据：DB 中已存在 `is_archdaeva=false` 且 `exp >= startExp(66)` 的角色，登录加载发生在种族/任务状态恢复前，仍会按经验重建为 66；本次修复阻止新的越界，但不自动降级存量角色，如需归一切记另行审计确认。
- 客户端/运行期验收仍待完成：65 级上限提示、10520 `SETPRO4` 自身传送仍成功、传送前返回卷轴被拒、传送后（step 4）返回卷轴可用。
- 可复用 Pattern 候选: `ARCHDAEVA_ENTRY_WORLD_AND_LEVEL_GATE`；在 focused tests 与客户端验收通过前不写入 Playbook。
