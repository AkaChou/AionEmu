# 帕休曼迪尔寺院入口门（730231）单人进入修复

## 1. 需求
- 实机反馈：副本「帕休曼迪尔寺院」（Beshmundir Temple，world **300170000**）入口门 NPC **730231**
  （`IDCatacombs_door_In`，AI `beshmundirswalk`，`spawns/Npcs/210050000_Inggison.xml:465`、主服镜像
  `600010000`/`600110000` 两 spawn 文件亦有）GM 单人点击后收到系统提示 1390256
  「此区域仅小队可进入 / This area is only accessible to groups」，无法进入。

## 2. 根因
- 门 AI `BeshmundirsWalkAI2.onDialogSelect` 的 `case 65` 分支对**无队伍玩家**硬发 1390256 并 return，
  **未参考任何副本进入豁免**。该 AI 的完整通路：队长 → 难度选择页 4762；队员（非队长）→ 队长须已在副本内
  （1400361）；无队伍 → 1390256。
- 同族检查在 `PortalService.port()` 中本属**可豁免项**：`instanceGroupReq = !player.havePermission(
  MembershipConfig.INSTANCES_GROUP_REQ)`；且当 `accessLevel >= AdminConfig.INSTANCE_REQ` 时整块 req 检查跳过。
  当前部署 `admin.properties:36` 为 `gameserver.administration.instancereq = 0` ⇒ `accessLevel < 0` 恒假
  ⇒ PortalService 的**全部**副本进入条件（含 `checkPlayerSize` 的 6 人组队要求）对**所有玩家**关闭。
- ⇒ AI 层的硬检查成为唯一拦截点，与部署意图（禁用副本进入限制）相矛盾，是缺陷（与 `.agents/summary/
  silentera-underpass-live-gates/README.zh-CN.md` 第 9 节发现的 `instancereq=0` 部署事实同源）。
- 佐证：`InstanceService.resetPlayerInstances` 的 Javadoc/实现已明确支持「单人进入的组队副本」（`soloPlayerObj`
  登记面）；`PortalService.port()` 单人 `case 6` 对豁免者已有 `getNextAvailableInstance` 通路 —— 下游早已为
  豁免者单人进组队副本设计好路径，唯独门 AI 是硬拦截。

## 3. 改动
- `src/main/java/com/aionemu/gameserver/services/teleport/PortalService.java`：
  新增 `public static boolean canBypassInstanceGroupRequirement(int accessLevel, boolean hasMembershipPerk)`
  （= `accessLevel >= AdminConfig.INSTANCE_REQ || hasMembershipPerk`），Javadoc 注明与 `port()` 进入流程的
  豁免口径一致（双语文档）。
- `src/main/java/com/aionemu/gameserver/ai/instance/beshmundirTemple/BeshmundirsWalkAI2.java`：
  `case 65` 无队伍分支先咨询共享豁免；豁免 → 弹难度选择 4762（与队长同口径）；非豁免 → 保留 1390256。
  有队伍玩家（队长/队员）路径不变。
- 新增 `src/test/java/com/aionemu/gameserver/ai/instance/beshmundirTemple/BeshmundirsWalkAI2Test.java`：
  ① 豁免判定两向断言（含会员特权 + 低于管理员等级）；② `case 65` 源码断言（走共享豁免 + 保留 1390256 +
  弹 4762）；③ portal 数据断言（730231 `player_count=6`、`instance=true`）。

## 4. 验证
- 静态（已完成）：IDE lint 三文件 0 新增问题（PortalService 既有告警与本次无关）；`git diff --check` 通过。
- 单元测试（已完成）：`BeshmundirsWalkAI2Test` 3/3 通过（IDEA MCP，2026-10-08）。
- 实机（已完成，2026-10-08 用户确认）：GM 单人点 730231 → 弹难度选择 → 确认 → 成功进入 300170000。
- 沉淀：已升级为 `patterns/instance-runtime.md` [IR-014]（DOOR_AI_ENTRY_CHECK_SHARES_PORTAL_EXEMPTION）。

## 5. 边界与遗留
- 当前部署 `instancereq=0` ⇒ 本项豁免对**所有玩家**生效（与「禁用副本进入限制」一致）；若改回 3 ⇒
  仅 GM（accessLevel>=3）与会员（membership>=10）豁免，普通玩家恢复真端组队口径。
- 未普查其他副本门 AI 是否存在同类「硬编码组队检查缺席豁免」；本次仅修用户报告的 BT 门。
  （`isInGroup2` 在 `ai/` 包的其余命中为宝箱/任务 NPC 场景，非副本门。）
- 会员豁免者（非 GM）若处于副本冷却且门户使用被禁用，单人仍会被 `STR_MSG_CANNOT_MAKE_INSTANCE_COOL_TIME`
  拦截（既有口径，未改）。
