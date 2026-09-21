# 埃雷修兰塔“空中听仇恨音 + 龙族监视者之眼放弃追踪”排查（2026-09-21）

## 1. 报障

- 世界 `400010000`（Reshanta / 埃雷修兰塔），克罗坦要塞（siege 1221）外围上空。
- 玩家 `//gps`（14:23:02）：`X=1548.9895, Y=1004.6653, Z=3021.8057, H=112`；`//geo z`（14:23:08）：`curZ=3021.8057, geoZ=2960.5042`（悬空约 61m，飞行中）。
- 现象：周围几乎看不到怪；垂直上下小幅飞动会连续听到多声“被怪物发现/仇恨”音效，并出现系统提示 `龙族监视者之眼放弃追踪`。

## 2. 已核实的事实

### 2.1 客户端字符串：`龙族监视者之眼` = name_id 305504

来源：`5.8客户端/L10N/CHS/Data/data.pak`（ZIP）→ `Strings/client_strings_monster.xml`（UTF-16 XML，58020 条，id 300000–2329091）：

| name_id | 客户端中文 | 模板 |
|---|---|---|
| 305496 | 天族监视者之眼 | 276217 `Ab1_1221_AirInterceptor_Li` |
| 305500 | 魔族监视者之眼 | 276221 `Ab1_1221_AirInterceptor_Da` |
| **305504** | **龙族监视者之眼** | **276225 `Ab1_1221_AirInterceptor_Dr`** |

对照：884091 `AB1_Boss_Dr_Sum02_A_75_Af` → name_id 2306233 → `第36部队强袭机`（不是监视者之眼）。

系统消息同样已核实（`Strings/client_strings_msg.xml`）：

| msg id | 常量 | 客户端中文 |
|---|---|---|
| 1300039 | `STR_UI_COMBAT_NPC_RETURN` | `%0%放弃追踪。` |

全库仅一处使用该消息：`EmoteManager.emoteStopAttacking`（参数为该 NPC 的 name_id）→ 玩家看到的 `龙族监视者之眼放弃追踪` 即 `STR_UI_COMBAT_NPC_RETURN(305504)`，发送者只能是 276225（或同模板同名的他要塞之眼）。

### 2.2 监视者之眼的刷点与参数

`src/main/resources/aion/data/static_data/spawns/Sieges/400010000_Reshanta.xml`（siege_id=1221, siege_race=BALAUR, siege_mod=PEACE）：

- 9 个点构成半径≈30m 的环 + 1 个圆心点，圆心≈(1446.5, 906)，`Z=3001–3006`（要塞飞行层高度），无 walker（固定）。
- 距报障点最近点 (1468.05, 926.39, 3004.4)：`2D=112.6m / 3D=113.9m`。
- 模板 `npc_template_270058_286320.xml`：`ai=siege_mine`、`sensory_range=25`、`tribe=GUARD_DRAGON`、`type=ABYSS_GUARD`、`walk/run_speed=0`。
- 零售 AI（`definitions/compact/ai/npc-ai-parts/npc-ai_270907_286549.xml`）：pattern `BGuard_AbyssTower`，`sensory_range=25 / short=10 / angle=360`，**`max_chase_time=8`**，`react_to_pathfind_fail=return_to_sp`，`decrease_sensory_range_return=40`。

### 2.3 引擎链路（静态确认）

- 感知：`CreatureEventHandler.isInAggroRange` = **3D 距离** + 零售 `sensory_range` + BoundRadius 偏移（上限 100）。276225 有效半径≈25.65m。
- 已知列表：`VisibleObject.VisibilityDistance=95`、`maxZvisibleDistance=95`；`KnownList.checkObjectInRange` 先判 `|dz|≤95` 再判 2D 距离。
- 音效：`AggroEventHandler.onAggro` / `onCreatureNeedsSupport` → `SM_ATTACK(owner, target, 0, 633, …)`，`broadcastPacket(owner, …)` **只发给该 NPC 已知列表内的玩家（≈95m 内）**。
- 放弃追踪文本：`AttackEventHandler.onFinishAttack` → `EmoteManager.emoteStopAttacking` →
  `PacketSendUtility.sendPacket(playerTarget, SM_SYSTEM_MESSAGE.STR_UI_COMBAT_NPC_RETURN(owner.nameId))`。
  **该消息没有任何距离/已知列表判定**，只要发送瞬间 NPC 的 target 仍是该玩家就会直接下发。
- 触发来源：`TargetEventHandler.returnToSpawn`/`onTargetGiveup`（ATTACK_FINISH）与 `AttackManager.stopRetailChase`（由 `max_chase_time` 驱动，在 `targetTooFar` 之前判定）。

### 2.4 要塞状态（2026-09-21 周一）

- DB `al_server_gs.siege_locations`：id=1221 → `race=BALAUR`。
- `config/schedule/siege_schedule.xml`：1221 = `SAT 18:00`、`SUN 18:00`、`TUE,WED 18:00` → 周一为 **PEACE**。
- `SiegeService.initSieges` 只刷 `race + PEACE` 集合 → **mod=SIEGE 的 NPC（含 884091）此时不存在**。
- 另行确认：全库仅 9 个刷怪文件包含 `map_id="400010000"`（Bases / Beritra / Gather / Instance_Rift / Landing / Landing_Special / Moltenus / Npcs / Sieges），已全部纳入扫描。

### 2.5 报障坐标可仇恨性（PEACE 过滤后）

在报障点 95m 已知列表内共 33 个对象（含 8 个空中采集点 401045），全部 NPC 的 `3D 距离 - 有效感知` 最小值为 **+58.4m**（885101 `Ab1_Mission_Eresh_Ra_65_Ae`，3D=68.0m / 有效感知 9.6m）。

=> **在报障坐标本身，垂直小幅飞动不可能触发任何 NPC 仇恨**；276225 更是在 112.6m（2D）之外，连已知列表都进不去，**不可能在该坐标产生“放弃追踪”。**

（备注：SIEGE-only 的 884091 `AB1_Boss_Dr_Sum02_A_75_Af` 感知 70、距报障点 3D=71.1m，刚好卡在边界上——若要塞处于 SIEGE，这就是“垂直微动即反复触发仇恨”的现成来源。可作对照实验。）

## 3. 结论（当前证据下的解释）

1. `龙族监视者之眼放弃追踪` 只可能来自 276225（或其同模板同名的他要塞眼）。玩家必然在 ≤8s 前进入过它 ~25m 的感知球体（即飞到要塞内部环上）。
2. 该消息是**延迟消息**：`max_chase_time=8` 到期后 `stopRetailChase → returnToSpawn → ATTACK_FINISH → emoteStopAttacking`，此时玩家即使已在 112m 外的空中、周围无怪，消息照样直发给玩家（`sendPacket` 无距离判定）。这解释了“空无一人的位置却收到放弃追踪”。
3. “连续多声仇恨音效”属于 633 广播：它只要求玩家在该 NPC 已知列表内（≤95m 2D、≤95m |dz|），**不要求玩家是仇恨目标**——同一 NPC 的支援连锁（`onCreatureNeedsSupport`）也会再广播一次。要塞内大量 60m 之下的守卫（基据点守卫、Ereshkigal 守卫、Protection Tower 等）都在玩家已知列表内，只要其中任意一只进入战斗就会向玩家广播该音效。
4. 仍需一次运行时抓取来锁定“音效到底是哪只怪、在哪个坐标”：

## 4. 运行时抓取步骤（无需构建/重启，全部为 GM 指令）

### A. 先确认要塞状态
```
//siege
```
看 1221 是 PEACE 还是 SIEGE（若是 SIEGE，884091 立刻成为首要嫌疑）。

### B. 抓「眼」的脱战链（定向日志）
```
//movetonpc 276225      ; 传送到其中一只眼
（选中它，确认目标框里的名字）
//info                  ; 记录 ObjectId / AI 名 / AgroList
//ai2 info              ; AI 名（retail_pattern 还是 siege_mine）与状态
//ai2 log               ; 打开这只眼的 AI 日志
（飞回报障点附近上下小幅飞动，复现「放弃追踪」，记住时间）
```
之后读 `log/aidebug.log`：会出现该 ObjectId 前缀的 `Creature event …` / `General event TARGET_TOOFAR / TARGET_GIVEUP / ATTACK_FINISH` / `Setting AI state to …`。

### C. 抓「音效」来源（全局、临时）
```
（先飞到远处，让要塞区域卸载）
//ai2 createlog         ; 打开“新建 AI 即记日志”（AIConfig.ONCREATE_DEBUG）
（飞回报障点复现音效，记录时间点）
//ai2 createlog         ; 立刻关掉
```
`log/aidebug.log` 中找该时间点的 `Creature event CREATURE_AGGRO: …` 行 → 行首 ObjectId 即制造音效的 NPC：
```
//movetoobj <ObjectId>
（选中并 //info）
```
即可得到 npc_id / AI / 位置 / 仇恨列表。

> 说明：`//ai2 createlog` 是全局开关且只对**新建** AI 生效（区域激活时才 spawn），因此必须先飞远卸载再开启；日志量按世界活跃 NPC 计，分钟级通常数十万行，属可接受范围。

## 5. 候选修复（需授权构建）

1. **幽灵消息（推荐）**：`EmoteManager.emoteStopAttacking` 下发 `STR_UI_COMBAT_NPC_RETURN` 前加“玩家仍在 NPC 已知列表 / 感知距离内”判定（否则该消息描述的怪玩家根本看不见）。族级影响：所有走 `max_chase_time` / `return_to_sp` 脱战的 NPC。
2. **不可移动防空怪反复仇恨→脱战**：与 AIM-004 同族（0 移速 NPC 不因 targetTooFar 放弃），但 `max_chase_time=8` 是真端数据里的脱战驱动，改前需真端证据（PK/PvE 录像或 retail 行为确认）。
3. **音效广播范围**：若确认 633 是“仅目标玩家应听到”的提示，可把 `AggroEventHandler` 的广播收紧为目标玩家（或 30m 内）。需先确认真端行为。

## 6. 参考文件

- 客户端串表：`5.8客户端/L10N/CHS/Data/data.pak!Strings/client_strings_monster.xml`
- 刷点：`src/main/resources/aion/data/static_data/spawns/Sieges/400010000_Reshanta.xml`（1221 BALAUR/PEACE 与 SIEGE）
- 模板：`src/main/resources/aion/data/static_data/npcs/npc_template_270058_286320.xml`（276223/276225/276226）
- 零售 AI：`src/main/resources/aion/definitions/compact/ai/npc-ai-parts/npc-ai_270907_286549.xml`（276221/276225）
- Pattern：`src/main/resources/aion/definitions/compact/ai/npcaipatterns.xml`（`BGuard_AbyssTower`）
- 引擎：`ai2/handler/AggroEventHandler.java`、`ai2/handler/CreatureEventHandler.java`、`ai2/handler/AttackEventHandler.java`、`ai2/manager/EmoteManager.java`、`ai2/manager/AttackManager.java`、`ai2/handler/TargetEventHandler.java`、`taskmanager/tasks/MovementNotifyTask.java`、`world/knownlist/KnownList.java`、`services/SiegeService.java`
- 复现脚本：本目录 `scan_peace_boundary.py`（按 1221=BALAUR/PEACE 过滤后重算 95m 内感知边界与“垂直进入仇恨所需的下降量”）

## 7. 本轮修复（代码已改，验证 PENDING）

### 7.1 改动

| 文件 | 内容 |
|---|---|
| `src/main/java/com/aionemu/gameserver/ai2/manager/EmoteManager.java` | `emoteStopAttacking` 增加可见性闸门：只有目标玩家已知列表仍包含该 NPC 时才下发 `STR_UI_COMBAT_NPC_RETURN`；新增包内静态判定 `canSeeDisengagingNpc(Npc, Player)` |
| `src/test/java/com/aionemu/gameserver/ai2/manager/EmoteManagerTest.java` | 新增：玩家仍看得见→放行、已离开已知列表→拦下、已知列表缺失→不抛异常，外加一条 `emoteStopAttacking` 源码闸门 |

族级影响：所有经由 `AttackEventHandler#onFinishAttack`（含 `max_chase_time` / `return_to_sp` 脱战、以及 7 个副本 AI 直接调用）的脱战提示都受同一闸门约束；战斗状态机、仇恨清理、表情包全部不变。

### 7.2 未执行的验证（等待授权）

```
mvn -q -Dtest=EmoteManagerTest test
```

后续仍需服务端重启 + 客户端实测复验（当前进程由用户管理，本仓库禁止 agent 启停）：
1. 飞到克罗坦要塞环上触发监视者之眼仇恨，再飞离 100m+ 等 8 秒 → 不再出现 `龙族监视者之眼放弃追踪`；
2. 站在眼旁边让它脱战 → 提示仍应正常出现（闸门不得误杀近身场景）。

### 7.3 仍未定案的开放项

“垂直小幅飞动听到多声仇恨音效”尚未定位到具体 NPC（报障坐标本身无任何可仇恨对象）。候选：
- 要塞内圈的玩家阵营据点守卫（如 base 54 ASMODIANS 的 883346/883349）与 BALAUR 要塞 NPC（883643、276226 防护塔，感知 25+3）距离仅 27–35m，可能长期互相仇恨/支援，而 633 广播对“已知列表内玩家”生效、不要求玩家是目标；
- 玩家 10 倍速（`//speed 1000`）漂移进要塞环 25m 感知球。
按第 4 节 B/C 步骤做一次运行时抓取即可定案。
