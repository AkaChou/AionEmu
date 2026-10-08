# Silentera Canyon 活服入口门（730256/730260）修复

## 1. 需求
- 修复 730256 `LF4_UnderPass_In`（name=silentera westgate，name_id=371442，Inggison 210050000 内 spawn）：
  点击后对话窗出现、动作 104（简单传送）无任何响应（log/quests.log 2026-10-08 Kk ×7）。
- 同批收口孪生 730260 `DF4_UnderPass_In`（name=silentera eastgate，Gelkmaros 220070000 内 spawn）——同一缺陷、同一功能面（锡兰泰拉峡谷的两个入口门）。

## 2. 根因
`portal_template2.xml` 中只有**主服孪生对**的配置：
- 731824 `LF4_UnderPass_M_In`（spawn 在 210130000 Inggison [Master Server]，惰性镜像图）→ loc 6001100（world 600110000 Underpass_M）；
- 731828 `DF4_UnderPass_M_In`（spawn 在 220140000 Gelkmaros [Master Server]）→ loc 6001101。

**活服 NPC（210050000/220070000 内、玩家实际点到的那两个）没有任何配置**：`PortalDialogAI2.checkDialog` 默认页 1011 正常弹出（玩家看得到动作 104），`onDialogSelect` → `Portal2Data.getPortalDialog(730256, 104, race)` 返回 null → 全部静默。内部出口（730257-263，spawn 于 600010000）均已配置——即"能出不能进"。与 memory-bank SDJ-003 的"主服/活服分裂"（210130000 vs 210050000）同型：58Server 数据为主服血统，玩家面要用活服 ID。

## 3. 真端权威证据
- `id/worldid.xml`（`definitions/compact/id-mappings.xml`）：`600010000 = Underpass`（活）、`600110000 = Underpass_M`（主服）。玩家面目标世界 = **600010000**。
- 真端 `Map/Worlds/underpass/world.xml`（UTF-16）direct_portals：
  - `From_LF4_1_OP_To_UNDERPASS_S1` → (542.741211, 398.857178, 327.124481) dir=180（Inggison 侧落点）；
  - `From_DF4_2_OP_To_UNDERPASS_S2` → (487.650238, 1171.729248, 331.671967) dir=0（Gelkmaros 侧落点）。
  - 佐证：`id-mappings` 中 600010000 的 `Op_Risen_coord` light=(490,358,328) / dark=(531,1138,333) 与两落点同区。
- **dir→h 编码**：`portal_loc.h` 是 heading byte，`MathUtil.convertDegreeToHeading = (byte)(degrees / 3)`（`convertHeadingToDegree = h*3` 逆式；`RetailDirectPortalEngine:128` 对真端直传门落点同式换算）。故 S1 h=60、S2 h=0。
- emulator 侧：geo `600010000.geo.gz` 存在；spawn 范围 x 292-1418 / y 104-1440 覆盖两落点。

## 4. 改动（纯数据，两文件）
- `src/main/resources/aion/data/static_data/portals/portal_loc.xml:333-334`：新增
  - loc 6000100 world 600010000 (542.741211, 398.857178, 327.124481) h=60（Elyos 侧）；
  - loc 6000101 world 600010000 (487.650238, 1171.729248, 331.671967) h=0（Asmodians 侧）。
- `src/main/resources/aion/data/static_data/portals/portal_template2.xml:3383-3395`（SILENTERA CANYON 4.7 分组内、M 孪生之后）：
  - `730256` → `dialog="104" loc_id="6000100" race="ELYOS"` + `portal_req min_level="55" err_level="27"`（与 M 孪生同款）；
  - `730260` → `dialog="104" loc_id="6000101" race="ASMODIANS"` + 同款 req。
- xsd sequence 保持（portal_dialog 均在 dialog 段内）；模板 `teleportDialogId` 默认 1011，新增配置不改变开门页。

## 5. 验证
- 静态（已完成）：`xmllint --noout` 两文件通过；IDE lint 两文件无新增问题（既有告警仅为 `max_level="75"` 冗余默认值）。
- focused 测试（IDEA MCP，2026-10-08 用户授权）：KahrunEntryRequirementTest 4/4、AbyssTeleporterQuestRequirementTest 3/3、BalaureaTeleporterQuestRequirementTest 6/6、AbyssInstanceEntryRequirementTest 3/3、FissureOfOblivionInstanceTest 3/5——合计 19/21，0 新增红。该组测试均以真实 `portal_template2.xml`/`portal_loc.xml` 做 JAXB/XPath 解析，本批数据改动通过。
  - FissureOfOblivionInstanceTest 的 2 项失败均为**既有红（HEAD 既有，非本批）**：① `hiddenRoomControllersCannotInterceptBossAttacks:102`——断言的 AI 源文本 `spawn(244490, …)` 在 HEAD 位于 `schedule(() -> spawn(…), 2000)` 闭包内（已登记 `.agents/summary/scriptdll-quest-driver/reports/2026-09-24-P0c8b-simple-hunt-gap-buckets.zh-CN.md:142`）；② `entranceKeepsOnlyTheQuestAndPortalOwnedExit:117`——`quest/definitions/quests/17510.xml` 已被 `9321e7663`（批量物理退役 29 个遗留 XML）删除、登记于 `retail-xml-retention.xml:22499`，测试未随退役更新。注意其 :116 的 portal 断言（读本批改动的 `//portal_dialog[@npc_id='834194']`）**通过**。
- 实机（待用户冷重启后验证）：Inggison 点击 730256 → 动作 104 → 落入 600010000 (542.74, 398.86)；Gelkmaros 对称验证 730260。

## 6. 边界与遗留
- `min_level="55"` 镜像 M 孪生配置（真端 4.7 区域 55+）；若验证角色 <55 会被 `checkEnterLevel` 拦截，属预期口径。
- 更广的主服/活服孪生漏配审计（其他世界对是否存在同型"_M 有配置、活服无配置）本次未做——如有需要可扩展。

## 7. 第二轮：出口门 730270/730271（2026-10-08 用户实机反馈「730270 交互后不能回到英吉斯温」）
- 同型缺陷在出口侧重复：live 世界 600010000 内玩家可见的出口门是 **730270 `LF4_UnderPass_out`「inggison gateway」**（466.747, 465.266, 330.493，S1 侧）与 **730271 `DF4_UnderPass_out`「gelkmaros gateway」**（471.539, 1078.136, S2 侧），二者 **ai="portal"**（走 `<portal_use>`，`PortalAI2.handleUseItemFinish`：portalUse==null 且 teleportTemplate==null 时零发包静默）。
- 它们的 **M 孪生 731860/731861 有配置**（→ loc 2101307/2201407），但 M 孪生只 spawn 于 600110000 [Master Server]（live 文件 0 命中）——又一处"配置挂在惰性主服孪生、活服漏配"。
- 全家族扫描（live 600010000 内全部 73xxxx）：730257-259/730261-263（各 Pass 出口）与 730731（Hexway）均 cfg=1；**唯 730270/730271 cfg=0**。
- 修复（`portal_template2.xml:669-672, 689-692`，紧邻各自 M 孪生）：`730270 → loc_id 2101307`（live Inggison 210050000，与 731860 同点）；`730271 → loc_id 2201407`（与 731861 及全部 Asmodian 侧出口 2201408-2210 同口径）。
- 边界：`portalUse` 在 `PortalAI2.handleSpawned()` 装载 ⇒ 需（冷）重启后生效。Gelkmaros 侧 loc 2201400/2201405-2212（含 2201407，共 19 条）world_id 均为 220140000（Gelkmaros [Master Server]），与 Inggison 迁移（a7da0ad67 把 21013xx 折入 210050000）不同——SDJ-003 边界已登记"Gelkmaros mirror 220140000 is not folded"，且 20031（已验收 2026-09-14）任务传送与 `custom.properties` 保护者/征服者世界表均使用 220140000 ⇒ 本批按家族既有口径镜像 2201407，不单方面改世界 ID（如后续裁定活服 Gelkmaros 为 220070000，应整批迁移 22014xx 一并处理）。DB 实查：本服无魔族角色（全部 11 个角色为天族），Gelkmaros 侧暂无实机验证面。

## 8. 第三轮：入口门任务门禁（用户实机反馈「730256 没完成任务也可以进入回廊，应该和 702663 一样」）
- **语义（用户裁定）**：进入回廊（Silentera 峡谷）须先**完成任务** 10035「실렌테라 회랑 진격 준비 / Silentera 回廊进军准备」（天族）/ 20035「실렌테라 회랑 진격을 위한 준비」（魔族镜像）；未完成 → 与 702663 同款提醒（1300690）且不传送。
- **库内依据**：
  - 10035/20035 为回廊故事任务：DD 行 `data_driven_quest.xml`（10035 dev_name=실렌테라 회랑 진격 준비，reward_npc=Cainus）；20035 XML 现存（`definitions/quests/20035.xml`，dev=진격을 위한 준비，reward_npc=Hler）。
  - 10035 全程在 Inggison——感官区 `LF4_SensoryArea_Q10035A` 注册于 210050000（R5 归一，见 `.agents/summary/quest-10035-enterarea-live-world/`）；第 6 步 Drakan（220021/216775）spawn 于 210050000；第 7 步 702663 在回廊口 ⇒ **任务本身不需要进回廊**，门禁=完成后开放不会断任务链。
- **机制**：沿用现成 `quest_req`（`PortalService.checkQuestsReq`）：`quest_step="0"` = 仅 `QuestStatus.COMPLETE` 通过；失败且未设 `err_quest` → `STR_CANNOT_MOVE_TO_AIRPORT_NEED_FINISH_QUEST`(1300690)——与 702663 提醒同一条消息。先例：Teminon Landing 门（730059-730063，对 1044/2042 用 `quest_step="0"`）。
- **改动**（`portal_template2.xml` 730256/730260 块）：`<portal_req min_level="55" err_level="27">` 内新增 `<quest_req quest_id="10035" quest_step="0"/>`（730256）/ `<quest_req quest_id="20035" quest_step="0"/>`（730260）。
- **激活**：730256/730260 为 portal_dialog（点击时实时查 `PORTAL2_DATA`、`port()` 时校验 req）⇒ **`//reload portal` 一条即可**，无需 `//reload_spawn`。
- **权限口径**：任务门禁仅对普通玩家生效（access level < `gameserver.administration.instancereq`=3 且 membership < `gameserver.instances.quest.requirement`=10）；GM/高会员旁路。
- **待验证（实机）**：未完成 10035 点 730256 → 1300690 且不传送；完成后 → 正常传送入 600010000。

## 9. 第四轮：门禁落代码（硬门禁）+ 物件了结静默（2026-10-08 用户二次反馈）
- **反馈 ①「未完成 10035 依然能进、无提示」根因（决定性）**：`admin.properties:36` → `gameserver.administration.instancereq = 0` ⇒ `PortalService.port()` 的 `instance*Req` 检查块（含 `min_level`/`quest_req`）对**所有玩家**关闭（`accessLevel < 0` 恒假）——第三轮加的 XML `quest_req` 在此部署整体空转；且 Kk 账号 `access_level=5`，即便 `instancereq>0` 也会按管理员旁路。⇒ 改为**代码硬门禁**（按 Kahrun/Balaurea 硬门禁成式，不受权限旁路）：
  - `TeleportService2`：新增 `SILENTERA_CANYON_WORLD_ID=600010000`、`ELYOS/ASMODIAN_SILENTERA_ENTRY_QUEST_ID=10035/20035`、`isSilenteraCorridorEntryWorld(int)`、`meetsSilenteraCorridorEntryRequirement(Player)` + `(Race, QuestState)` 重载、`getSilenteraCorridorEntryQuestId(Race)`；语义 = `meetsQuestRequirement(qs, 0)` = **仅 COMPLETE**（START/REWARD 不放行）。
  - `PortalService.port()`：在欧比斯硬门禁之后新增回廊硬门禁，失败 → 1300690（同 702663 提醒文案）。XML 侧 `quest_req` 保留为双保险（服务 `instancereq>0` 的部署口径）。
- **反馈 ②「10035 完成后点 702663 仍弹提醒」根因**：QuestItemNpcAI2 失败分支只看"引擎未认领"即提醒（第三轮前的已知边界，用户以角色 Ww 实证 10035=COMPLETE 仍提醒）。修复：新增 `SILENT_FINISHED_QUEST` 形态 + `relatedQuestsFinished(player)`（`onTalkEvent` 非空且全部 COMPLETE/REWARD → 零发包；空列表维持既有通用提醒口径）；分类器改三参 `failedInteractionReply(dialogNpc, collectObject, relatedQuestsFinished)`。
- **DB 实证（al_server_gs / al_server_ls，只读）**：Kk(151512) `access_level=5`、10035=START step4；Ww(153807) 10035=COMPLETE step8；小碗削面(152844) 10035=LOCKED。⇒ 反馈①以 Kk 测、反馈②以 Ww 测，均可复现。
- **测试**：新增 `SilenteraCorridorEntryRequirementTest`（COMPLETE 放行 / START/REWARD/异族/PC_ALL/null 拒绝 / 世界判定 / XML 门禁断言）；`QuestItemNpcAI2Test` 更新为三参并断言新形态。
- **生效**：代码改动需（冷）重启（JRebel 结构变更口径）；XML 的 `quest_req` 可由 `//reload portal` 热载（硬门禁不依赖它）。
- **待验证（实机）**：Kk（step4）点 730256 → 1300690 不传送；Ww（COMPLETE）→ 正常传入 600010000，且点 702663 变为静默（零反馈）。

## 10. 第五轮：回廊出口的龙界门禁豁免（2026-10-08 用户反馈「730270 也提示未完成任务，从回廊进英吉斯温不应设卡」）
- **现象与定性**：角色 **Zz**（10031=无——DB 实证；10035 于 13:42 经 GM `quest set 10035 complete` 强通）从回廊点 730270「inggison gateway」→ 被**既有龙界入场硬门禁**弹 1300690（`adminaudit.log` 实证：13:42:28 `Zz: quest set 10035 complete 9` → 13:43:18 `Zz → inggison gateway`）。**根因 = 潜伏的既有门禁**：出口门（730270/730271/各 Pass）此前无配置=全静默，本批让它们首次真正走进 `PortalService.port()`，暴露"从回廊回龙界仍要求 10031"（Kk 因 10031 已 COMPLETE 不受影响）。
- **修复（一度实施后又撤销，以用户裁定为准）**：曾按"回廊已在龙界区域内"给 `PortalService.isSameBalaureaRegion` 加回廊起点豁免（`TeleportService2.isSilenteraCorridorWorld` 含 600010000/600110000；Hexway AI `ShiningMagicWardAI2` 会传玩家进 600110000，其出口 731860/731861 同型）。**用户随即裁定「要求 10031，和真端要一致 → 撤销出口豁免」**：从回廊回英吉斯温/格尔克马罗斯继续走真端既有的龙界入场硬门禁（无 10031 即被拦——Zz 被拦属真端口径），已全部还原（`isSameBalaureaRegion` 恢复原状、删除 `isSilenteraCorridorWorld` 与大师服变体常量）。
- **测试**：`SilenteraCorridorEntryRequirementTest` 以 `corridorOriginsKeepTheBalaureaEntryGate` **锁定裁定**（回廊起点不豁免：`isBalaureaEntryAllowed(210050000/220140000, 600010000/600110000, false, false)` 全 false；同世界/实例/已持任务三例仍 true）；既有 `BalaureaTeleporterQuestRequirementTest` 四例断言不受影响。
- **生效**：纯代码改动，JRebel 热更即生效（出口门 NPC 的 `portalUse` 缓存无需刷新——变更在 port() 内部判断）。
