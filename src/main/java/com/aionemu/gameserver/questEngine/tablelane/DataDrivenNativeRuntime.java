package com.aionemu.gameserver.questEngine.tablelane;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.PersistentState;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_DIALOG_WINDOW;
import com.aionemu.gameserver.network.aion.serverpackets.SM_QUEST_ACTION;
import com.aionemu.gameserver.questEngine.QuestEngine;
import com.aionemu.gameserver.questEngine.definition.QuestDialogContract;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.services.QuestService;
import com.aionemu.gameserver.questEngine.retail.RetailItemNameIndex;
import com.aionemu.gameserver.questEngine.retail.RetailStringIds;
import com.aionemu.gameserver.questEngine.tablelane.DataDrivenQuestTable.Kind;
import com.aionemu.gameserver.questEngine.tablelane.DataDrivenQuestTable.Row;
import com.aionemu.gameserver.questEngine.tablelane.DataDrivenQuestTable.Step;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.stats.AbyssRankEnum;

/**
 * 真端 DataDriven 原生**进度运行时**（计划 §10.2「P7 DataDriven」步 2 步 d / 步 d2）。
 * <p>
 * 按真端「每步一条 handler 记录」驱动进度：兴趣面（击杀/对话/FOBJ/进区/进世界/PvP/物品获得）按 DD 步载荷建索引，
 * 事件到达后走 {@link DataDrivenProgress} 的真端算术（Hunt/TalkFOBJ 多组计数；PvP 单组 + 军衔/等级闸门；
 * ItemPlay 单组物品获得计数；Talk/EnterArea/EnterWorld **直接步进**），末步转待领奖（真端 {@code SetQuestSuccess}）。
 * 真端逐 handler 原码：`FUN_180c46020`（Hunt，4 组 × 6 位、组内命中即 +1、全组达标才步进）、
 * `FUN_180c46980`（PvP，单组 + `killerLevel <= victimLevel + gap` 与军衔区间闸门）、
 * `FUN_180c47bf0`（EnterArea，名哈希同名区比对后直接步进）、`FUN_180c467b0`（EnterWorld，步号校验后直接步进）、
 * `FUN_180c478e0`（TalkFOBJ，组计数 0→1 二值）、`FUN_180c46e90`（ItemPlay，物品 id 键控事件 5，
 * 步 kind==3 校验 + 物品 id 匹配 + 组 1 计数步进）。
 * <p>
 * **步 d2 对话平面**（真端 `FUN_180c474b0`，kind 1 CollectItem / kind 4 Talk 的主对象共用）：
 * 对话打开（状态 0/10）→ 按当前步发阶段页 `select(K+1)`（1011/1352/…/7864，步 ≥15 不发）；
 * 页动作 `10000+K`（顺序守卫：K == 当前步 + 1，乱序静默零写）→ 步进写 K；
 * `1009` → 步进 + 报告通道（末步 = 待领奖 + 奖励窗页 5）；`1008` → 完成页原样回发；
 * `10255`（`0x280f`）→ 步进 + 完成页；其余 ≥1000 动作原样回发。Talk/CollectItem 载荷名允许真端
 * `quest_ai_name` 组（全组成员共担同一对话脚本，与 SimpleTalk 车道同轴）。
 * <p>
 * **本批不发运行期分流**（零行为变更）：DD 切换集行仍由旧 IR 车道 owns，生产 {@link #instance()} 的路由集为空
 * ——兴趣表不建、事件恒 false；字面切换（路由集 = switch set）随 P7 步 2 步 f 的原子切换批落地。
 * <p>
 * Native DataDriven progress runtime (plan §10.2 step 2d / 2d2). Interests are indexed from the DD step
 * payloads (kill / dialog / FOBJ / enter-zone / enter-world / PvP / item-acquire) and every event runs the
 * retail arithmetic of {@link DataDrivenProgress}. The dialog plane mirrors the shared retail stage-dialog
 * handler. The production singleton routes nothing yet (the switch set is still owned by the legacy IR
 * lane); the literal routing flip happens in the atomic switch batch (step 2f).
 */
public final class DataDrivenNativeRuntime {

	/** 真端 DD 表资源路径。 / The retail DD table resource. */
	public static final String TABLE_RESOURCE = "/aion/data/static_data/quest/retail/data_driven_quest.xml";

	/**
	 * 真端击杀/PvP 距离门（平方米）。8 批取证链：处理函数前奏（Hunt 槽 `FUN_180c46020` / Pvp 槽
	 * `FUN_180c46980`，注册于 `DAT_184720a50` / `DAT_184720a08`）按 def+0x70 案值取界
	 * {&lt;0:跳过, 0/1:2500, 2:10000, 5/6:40000, 其他:跳过}；DD 装载器对 def+0x70 **零写入**（解析记录
	 * 初始化恒 0，全库无存储点）⇒ 运行时恒走 case 0 = 2500（50m）。实参 = 成员与死亡对象的平方欧氏
	 * 距离（MainServer `AllianceBattleGroup::GetValidMember` 算 {@code dx²+dy²+dz²}，200m 外成员发包前
	 * 已剔除；包 NS_VALID_MEMBER_LIST 0xff78 每成员 {id, distSq} → NPCSvr64 `PacketValidMemberList`
	 * 原样作为第 6 参传入处理函数）。同图检查（{@code +0x30()==mapId}）与旗标链（+0x98/+0xa0/+0xa8，
	 * 仅跨世界可达）在本服拓扑不可达 ⇒ 不镜像。
	 * Retail kill/PvP distance gate (squared m): the shared handler preamble gates progress by the
	 * def+0x70 case table, but no loader ever stores that field (init 0) ⇒ case 0 always, i.e. 2500
	 * (50 m). The argument is the squared Euclidean distance between the credited member and the dead
	 * object (GetValidMember → packet 0xff78 → PacketValidMemberList → handler arg 6). The same-world
	 * and flag checks are cross-world-only in retail and unreachable on this topology ⇒ not mirrored.
	 */
	static final float RETAIL_KILL_DISTANCE_SQ = 2500.0f;

	/** 组标题后的尾整数（空格或逗号分隔）；名字内部的 `_N` 不算计数。 / Trailing count after a space or comma. */
	private static final Pattern TRAILING_INT = Pattern.compile("^(.*?)(?:\\s*,\\s*|\\s+)(\\d+)$");

	/**
	 * 真端 DD 阶段页（`FUN_180c474b0` 页表：switch 下标 0..14 → `select1..select15`；下标 ≥15 不发页）。
	 * Retail DD stage pages (`FUN_180c474b0` switch table); indexes ≥15 send no page.
	 */
	private static final int[] STAGE_PAGES = {1011, 1352, 1693, 2034, 2375, 2716, 3057, 3398, 3739, 4080, 6500,
		6841, 7182, 7523, 7864};

	/** 奖励窗页（真端报告通道打开的选择窗；与家族车道同页）。 / The reward-window page. */
	private static final int PAGE_REWARD_WINDOW = 5;
	/** 完成页（真端 `mgr+0x5d8` 完成通道；与家族车道同页）。 / The completion page. */
	private static final int PAGE_COMPLETE = 1008;
	/** 接取入口问询页（真端 `FUN_180c47220` 打开态字面量 `0x129a`）。 / The accept-entry ask page (retail 0x129a). */
	private static final int PAGE_ACCEPT_ENTRY = 4762;
	/** 接取确认页（真端 1002 成功后下发）。 / The accepted page (sent after retail 1002). */
	private static final int PAGE_ACCEPTED = 1003;
	/** 拒绝确认页（真端 1003 下发）。 / The confirm-refuse page (sent after retail 1003). */
	private static final int PAGE_REFUSE = 1004;
	/** 报告动作（真端 `0x3f1`）。 / The report action. */
	private static final int ACTION_REPORT = 1009;
	/** 完成动作（真端 `0x3f0`）。 / The complete action. */
	private static final int ACTION_COMPLETE = 1008;
	/** 步进 + 完成动作（真端 `0x280f`）。 / The advance-and-complete action. */
	private static final int ACTION_ADVANCE_COMPLETE = 10255;
	/** 接取动作（真端 0x3ea）。 / The accept action. */
	private static final int ACTION_ACCEPT = 1002;
	/** 接取确认动作（真端 0x3eb）。 / The accepted-ack action. */
	private static final int ACTION_ACCEPTED = 1003;
	/** 问询动作（真端 0x3ef → mgr+0x1a0 接取窗）。 / The ask action (retail mgr+0x1a0). */
	private static final int ACTION_ASK = 1007;
	/** 建档动作（真端 20000）。 / The book-entry accept action. */
	private static final int ACTION_BOOK = 20000;
	/** 拒绝动作（真端 20001）。 / The refuse action. */
	private static final int ACTION_REFUSE = 20001;

	/** 一次事件命中的步引用：任务 + 步号 + 组槽（直接步进类为 0）。 / One event hit: quest, step, group. */
	public record StepHit(int questId, int stepIndex, int group) {
	}

	/** 不可原生路由的冻结原因。 / Why a switch-set row is not natively routable yet. */
	public enum FreezeReason {
		/** 名字解析不到唯一 NPC / 怪物 / 物品 / 字符串键。 / A payload name or string key did not resolve. */
		NAME_UNRESOLVED,
		/** 进区别名在真端世界文件里没有定义（{@code NativeEnterAreaPort.RETAIL_ABSENT_ALIASES}）。 / Retail-absent zone alias. */
		ZONE_ABSENT,
		/** 进区别名未登记成区。 / Zone alias not registered. */
		ZONE_UNRESOLVED,
		/** 载荷语法不合法（空组 / 非整数世界 id / 非整数计数）。 / Malformed payload. */
		PAYLOAD_INVALID,
		/** 步携带仍未落面的附加动作（第九批后仅剩：真端世界文件本就缺落点别名的 ENTER_INSTANCE 行
		 * 〔20032，creation 2〕；col8 Message8 = `Npc::Die` 零 routed 人口不计）。A step carries an
		 * extra action still unfaced (after batch 9: only the Enter Instance row whose landing alias
		 * is intrinsically absent from the retail world files). */
		ACTION_UNFACED
	}

	/** 真端 PvP 闸门（`PvP Target Min Rank` / `Max Rank` / `Level Gap`；0 = 该轴无闸门）。 / PvP gate. */
	private record PvpGate(int minRank, int maxRank, int levelGap) {
	}

	/** 一步的原生执行计划。 / The native execution plan of one step. */
	private record StepPlan(Kind kind, boolean counter, List<DataDrivenProgress.Slot> slots, PvpGate gate,
			boolean lastStep) {
	}

	/** 挑战任务接取哨兵（真端 DD 表字面；P0c-58 四源裁定 = 接取 NPC 即交付 NPC 本人）。 */
	private static final String CHALLENGE_TASK_SENTINEL = "_challengetask_";

	/**
	 * 接取计划（真端 LoadBasicInfo 注册面）。返回 {@code null} = 接取参数名解析失败（fail-closed 冻结）。
	 * The per-row acquire plan; {@code null} = the acquire parameter name did not resolve (fail closed).
	 */
	private static AcquirePlan acquirePlan(Row row, NativeNpcNameResolver nameResolver,
			RetailItemNameIndex itemIndex, Set<String> unresolved) {
		String kind = row.acquireKind() == null ? "" : row.acquireKind().trim();
		return switch (kind) {
			case "talk" -> {
				Set<Integer> npcIds = new LinkedHashSet<>();
				boolean ok = true;
				// 挑战任务哨兵：接取参数不是 NPC 名，接取 NPC = 交付 NPC 本人（reward_npc_name）。
				// Challenge sentinel: the acquire npc is the reward npc itself (P0c-58 four-source ruling).
				if (CHALLENGE_TASK_SENTINEL.equals((row.acquireParam() == null ? "" : row.acquireParam()).trim())) {
					ok = resolveMonsters(row.rewardNpc() == null ? "" : row.rewardNpc(), nameResolver, npcIds,
						new ArrayList<>());
					if (!ok) {
						unresolved.add((row.rewardNpc() == null ? "" : row.rewardNpc()).trim()
							.toLowerCase(java.util.Locale.ROOT));
					}
				} else {
					for (PayloadGroup group : parseGroups(row.acquireParam() == null ? "" : row.acquireParam())) {
						for (String name : group.names()) {
							if (!resolveMonsters(name, nameResolver, npcIds, new ArrayList<>())) {
								unresolved.add(name.trim().toLowerCase(java.util.Locale.ROOT));
								ok = false;
								break;
							}
						}
						if (!ok) {
							break;
						}
					}
				}
				yield ok && !npcIds.isEmpty()
					? new AcquirePlan(4, 0, 0, 0, List.copyOf(npcIds), "") : null;
			}
			case "itemplay" -> {
				String text = (row.acquireParam() == null ? "" : row.acquireParam()).trim();
				Integer itemId = itemIndex.resolve(text);
				if (itemId == null) {
					unresolved.add(text.toLowerCase(java.util.Locale.ROOT));
					yield null;
				}
				yield new AcquirePlan(3, 0, 0, itemId, List.of(), "");
			}
			case "enterworld" -> {
				int worldId = parseWorldId(row.acquireParam() == null ? "" : row.acquireParam());
				yield worldId > 0 ? new AcquirePlan(7, 0, worldId, 0, List.of(), "") : null;
			}
			case "levelup", "leveluplogin" -> {
				int level = parseCount(row.acquireParam() == null ? "" : row.acquireParam());
				yield level > 0
					? new AcquirePlan("leveluplogin".equals(kind) ? 10 : 8, level, 0, 0, List.of(), "") : null;
			}
			case "enterarea" -> {
				// 真端接取侧独立名字哈希树：别名登记原文，区未注册时永不命中（真端自身死边）。
				// The retail acquire-side name-hash tree keys on the alias; an unregistered zone
				// name is never dispatched, mirroring the retail-dead edge.
				String alias = (row.acquireParam() == null ? "" : row.acquireParam()).trim();
				yield alias.isEmpty() ? AcquirePlan.NONE : new AcquirePlan(6, 0, 0, 0, List.of(), alias);
			}
			default -> AcquirePlan.NONE;
		};
	}

	/** 一个载荷组：名字列表 + 尾整数（Hunt = 计数，TalkFOBJ = 动作类型）。 / One payload group. */
	private record PayloadGroup(List<String> names, int trailing) {
	}

	/** 步 e2 已落面的附加动作类型（真端执行器 case 1/2/3/4/5/7）。 / The extra-action types faced in step e2. */
	private enum ActionType {
		/** case 1：发物品（`Give/Remove Items` 发半边）。 / Give items (executor case 1). */
		GIVE_ITEMS,
		/** case 2：扣物品。 / Remove items (executor case 2). */
		REMOVE_ITEMS,
		/** case 4：过场/电影（`Cutscene|Cutscene2|Movie|Movie2 N`）。 / Cutscene or movie (executor case 4). */
		CUTSCENE,
		/** case 3：传送（`世界 x y z heading`，真端 z+1 落地）。 / Teleport (executor case 3). */
		TELEPORT,
		/** case 5：刷怪（`Absolute|Relative 名, 数量, 时间[, x y z heading]`）。 / Spawn npcs (executor case 5). */
		SPAWN,
		/** case 7：播报（`STR_*` 键 → 字符串表 id）。 / Say (executor case 7, string-table id). */
		SAY,
		/** case 10：任务计时（`秒, 目标步, 旗标`，旗标 0=到期推进 / 1=到期弃任）。
		 * Quest timer (executor case 10: `seconds, destStep, flag`; flag 0 = advance / 1 = abandon). */
		TIMER,
		/** case 9：进副本（`creationId, worldId, leaveProgress[, 成员名…]`；落点 =
		 * `NativeInstanceEntryPort` 真端别名坐标；movieId 槽 = creationId）。Enter instance
		 * (executor case 9; landing point from the retail alias table; movieId slot = creationId). */
		ENTER_INSTANCE
	}

	/** 一个已解析的附加动作（未用字段 = -1/0）。 / One resolved extra action (unused fields = -1/0). */
	private record ActionPlan(ActionType type, int itemId, int count, int movieId, boolean movieToken, int worldId,
			float x, float y, float z, int heading, boolean relative, int stringId) {
	}

	/**
	 * 接取计划（真端 LoadBasicInfo 注册面：Talk=0x640 对话面 / ItemPlay=事件 5 / EnterWorld=事件 0x12 /
	 * LevelUpLogIn=vec0+vec3 等级等值；EnterArea=e1 缺面（区几何未导入，接取缺席镜像真端注册树未填）；
	 * none/其余 = 无接取注册）。
	 * The per-row acquire plan (retail LoadBasicInfo registration face). EnterArea is unfaced in e1
	 * (zone geometry not imported); none/other kinds have no acquire registration.
	 */
	/**
	 * 接取计划（真端 6 类接取 kind 的解析产物）。
	 * <p>
	 * kind 6（EnterArea）只带区别名：真端接取侧是**独立的名字哈希注册树**（`FUN_180c47bf0` 尾段，
	 * 0x1003F 逐值比较），区不存在时注册项永不命中（真端自身即死边）⇒ 镜像 = 兴趣键登记别名原文，
	 * 进区事件按注册区名派发，未注册区名永不派发。
	 * The acquire plan parsed from one retail acquire kind. Kind 6 (EnterArea) carries only the zone
	 * alias: retail registers it in a separate name-hash tree (tail of FUN_180c47bf0) that can only be
	 * hit by an entered zone of the same name, so an unregistered alias is a dead edge at retail itself
	 * — mirrored by keying the interest on the alias text.
	 */
	private record AcquirePlan(int kind, int level, int worldId, int itemId, List<Integer> npcIds,
			String zoneAlias) {

		static final AcquirePlan NONE = new AcquirePlan(0, 0, 0, 0, List.of(), "");
	}

	private static volatile DataDrivenNativeRuntime instance;

	private final Map<Integer, List<StepPlan>> plansByQuestId;
	private final Map<Integer, List<StepHit>> killsByNpcId;
	private final Map<Integer, List<StepHit>> talksByNpcId;
	private final Map<Integer, List<StepHit>> fobjsByNpcId;
	private final Map<Integer, List<StepHit>> itemPlaysByItemId;
	private final Map<String, List<StepHit>> zonesByName;
	private final Map<Integer, List<StepHit>> worldsByWorldId;
	private final Map<Integer, List<Integer>> pvpStepsByQuestId;
	/** 接取兴趣面：Talk 对话 NPC → 任务。 / Acquire interest: talk npc → quests. */
	private final Map<Integer, List<Integer>> acquireTalksByNpcId;
	/** 接取兴趣面：物品获得（kind 3，真端事件 5 双角色）→ 任务。 / Acquire interest: item acquire → quests. */
	private final Map<Integer, List<Integer>> acquireItemsByItemId;
	/** 接取兴趣面：进世界（kind 7，真端事件 0x12 双角色）→ 任务。 / Acquire interest: enter-world → quests. */
	private final Map<Integer, List<Integer>> acquireWorldsByWorldId;
	/** 接取兴趣面：等级等值（kind 8/10）→ 任务。 / Acquire interest: exact level → quests. */
	private final Map<Integer, List<Integer>> acquireLevelsByLevel;
	/** 接取兴趣面：进区别名（kind 6，真端接取侧名字哈希树）→ 任务。 / Acquire interest: zone alias (kind 6). */
	private final Map<String, List<Integer>> acquireZonesByName;
	/** 已落面附加动作（quest → 逐步 ActionPlan，与步序对齐）。 / Faced extra actions per quest and step. */
	private final Map<Integer, List<List<ActionPlan>>> actionsByQuestId;
	/** 接取计划（quest → plan，kind 过滤与接取面用）。 / Acquire plans by quest. */
	private final Map<Integer, AcquirePlan> acquireByQuestId;
	private final Set<Integer> ownedQuestIds;
	private final Set<Integer> routedQuestIds;
	private final Map<Integer, FreezeReason> frozenQuestIds;
	private final Set<String> unresolvedNames;
	private final NativeInventoryPort inventoryPort;
	private final NativeMoviePort moviePort;
	private final NativeTeleportPort teleportPort;
	private final NativeSpawnPort spawnPort;
	private final NativeSayPort sayPort;
	private final NativeTimerPort timerPort;

	private DataDrivenNativeRuntime(Map<Integer, List<StepPlan>> plansByQuestId,
			Map<Integer, List<StepHit>> killsByNpcId, Map<Integer, List<StepHit>> talksByNpcId,
			Map<Integer, List<StepHit>> fobjsByNpcId, Map<Integer, List<StepHit>> itemPlaysByItemId,
			Map<String, List<StepHit>> zonesByName, Map<Integer, List<StepHit>> worldsByWorldId,
			Map<Integer, List<Integer>> pvpStepsByQuestId, Map<Integer, List<Integer>> acquireTalksByNpcId,
			Map<Integer, List<Integer>> acquireItemsByItemId, Map<Integer, List<Integer>> acquireWorldsByWorldId,
			Map<Integer, List<Integer>> acquireLevelsByLevel, Map<String, List<Integer>> acquireZonesByName,
			Map<Integer, List<List<ActionPlan>>> actionsByQuestId,
			Map<Integer, AcquirePlan> acquireByQuestId, Set<Integer> ownedQuestIds,
			Set<Integer> routedQuestIds, Map<Integer, FreezeReason> frozenQuestIds, Set<String> unresolvedNames,
			NativeInventoryPort inventoryPort, NativeMoviePort moviePort, NativeTeleportPort teleportPort,
			NativeSpawnPort spawnPort, NativeSayPort sayPort, NativeTimerPort timerPort) {
		this.plansByQuestId = plansByQuestId;
		this.killsByNpcId = killsByNpcId;
		this.talksByNpcId = talksByNpcId;
		this.fobjsByNpcId = fobjsByNpcId;
		this.itemPlaysByItemId = itemPlaysByItemId;
		this.zonesByName = zonesByName;
		this.worldsByWorldId = worldsByWorldId;
		this.pvpStepsByQuestId = pvpStepsByQuestId;
		this.acquireTalksByNpcId = acquireTalksByNpcId;
		this.acquireItemsByItemId = acquireItemsByItemId;
		this.acquireWorldsByWorldId = acquireWorldsByWorldId;
		this.acquireLevelsByLevel = acquireLevelsByLevel;
		this.acquireZonesByName = acquireZonesByName;
		this.actionsByQuestId = actionsByQuestId;
		this.acquireByQuestId = acquireByQuestId;
		this.ownedQuestIds = ownedQuestIds;
		this.routedQuestIds = routedQuestIds;
		this.frozenQuestIds = frozenQuestIds;
		this.unresolvedNames = unresolvedNames;
		this.inventoryPort = inventoryPort;
		this.moviePort = moviePort;
		this.teleportPort = teleportPort;
		this.spawnPort = spawnPort;
		this.sayPort = sayPort;
		this.timerPort = timerPort;
	}

	/**
	 * 生产单例：装载真端 DD 表并按切换集（retention 台账 owner=RETAIL_TABLE ∧ family=DataDriven = 1467
	 * 候选行）建立路由视图；行级冻结在 {@link #create} 内逐行裁定（2026-10-03 偏差修复第七批后 =
	 * 可路由 1455 / 冻结 12）。
	 * Production singleton: loads the retail DD table and builds the routing view for the switch set
	 * (retention ledger owner RETAIL_TABLE ∧ family DataDriven = 1467 candidates); per-row freezing is
	 * adjudicated inside create (routed 1455 / frozen 12 after the deviation-fix batch 7, 2026-10-03).
	 * Startup ordering: static data (incl. the zone registry) loads before engines
	 * (GameStartupSequenceLifecycle phase order).
	 */
	public static DataDrivenNativeRuntime instance() {
		DataDrivenNativeRuntime local = instance;
		if (local == null) {
			synchronized (DataDrivenNativeRuntime.class) {
				local = instance;
				if (local == null) {
					local = loadProduction();
					instance = local;
				}
			}
		}
		return local;
	}

	/** retention 台账资源（owner/family 列 = 切换集权威来源）。 / The retention ledger (switch-set source). */
	private static final String RETENTION_RESOURCE = "/aion/data/static_data/quest/retail/retail-xml-retention.tsv";

	private static DataDrivenNativeRuntime loadProduction() {
		Set<Integer> switchSet = retentionSwitchSet();
		DataDrivenQuestTable table;
		try (InputStream input = DataDrivenNativeRuntime.class.getResourceAsStream(TABLE_RESOURCE)) {
			if (input == null) {
				throw new IllegalStateException("DATA_DRIVEN_TABLE_MISSING: " + TABLE_RESOURCE);
			}
			table = DataDrivenQuestTable.load(input);
		} catch (IOException e) {
			throw new IllegalStateException("DATA_DRIVEN_TABLE_UNREADABLE: " + TABLE_RESOURCE, e);
		}
		NativeEnterAreaPort enterAreaPort = NativeEnterAreaPort.create(table, switchSet, registeredZoneNames());
		try {
			return create(table, switchSet, NativeNpcNameResolver.instance(), enterAreaPort,
				RetailItemNameIndex.loadItemTemplates(), NativeInventoryPort.live(), NativeMoviePort.live(),
				NativeTeleportPort.live(), NativeSpawnPort.live(), NativeSayPort.live(), NativeTimerPort.live());
		} catch (IOException e) {
			throw new IllegalStateException("DATA_DRIVEN_PRODUCTION_WIRING_FAILED", e);
		}
	}

	/**
	 * 切换集 = retention 台账 owner RETAIL_TABLE ∧ family DataDriven（与 RetailQuestDriver 同源解析，
	 * 1467 行；行级可路由性由 create 逐行裁定）。
	 * The switch set: retention rows with owner RETAIL_TABLE ∧ family DataDriven (same ledger the
	 * driver parsed; per-row routability is adjudicated row by row in create).
	 */
	private static Set<Integer> retentionSwitchSet() {
		try (InputStream input = DataDrivenNativeRuntime.class.getResourceAsStream(RETENTION_RESOURCE)) {
			if (input == null) {
				throw new IllegalStateException("DATA_DRIVEN_RETENTION_MISSING: " + RETENTION_RESOURCE);
			}
			Set<Integer> ids = new java.util.TreeSet<>();
			for (String line : new java.io.BufferedReader(new java.io.InputStreamReader(input,
					java.nio.charset.StandardCharsets.UTF_8)).lines().toList()) {
				if (line.isBlank() || line.startsWith("#")) {
					continue;
				}
				String[] parts = line.split("\t", -1);
				if ("RETAIL_TABLE".equals(parts[1]) && "DataDriven".equals(parts[2])) {
					ids.add(Integer.parseInt(parts[0]));
				}
			}
			if (ids.isEmpty()) {
				throw new IllegalStateException("DATA_DRIVEN_SWITCH_SET_EMPTY: " + RETENTION_RESOURCE);
			}
			return ids;
		} catch (IOException e) {
			throw new IllegalStateException("DATA_DRIVEN_RETENTION_UNREADABLE: " + RETENTION_RESOURCE, e);
		}
	}

	/**
	 * 已注册区名（ZoneData 全量 zones_*.xml；进区事件只按注册区派发，别名必须落在注册面内）。
	 * 单测环境静态数据未装载 ⇒ 回退扫描工作树同一 zones 目录（与 ZoneData 同一文件集，口径一致）。
	 * Registered zone names from ZoneData (all zones_*.xml); enter-zone events dispatch by registered
	 * name only. Unit tests without static data fall back to scanning the same zones directory in the
	 * working tree (the identical file set, one semantic).
	 */
	private static Set<String> registeredZoneNames() {
		Set<String> names = new java.util.TreeSet<>(String.CASE_INSENSITIVE_ORDER);
		if (com.aionemu.gameserver.dataholders.DataManager.ZONE_DATA != null) {
			for (java.util.List<com.aionemu.gameserver.model.templates.zone.ZoneInfo> infos : com.aionemu.gameserver.dataholders.DataManager.ZONE_DATA
					.getZones().values()) {
				for (com.aionemu.gameserver.model.templates.zone.ZoneInfo info : infos) {
					names.add(info.getZoneTemplate().getName().name());
				}
			}
		} else {
			java.util.regex.Pattern zoneName = java.util.regex.Pattern.compile("<zone\\b[^>]*\\bname=\"([^\"]+)\"");
			java.nio.file.Path dir = java.nio.file.Path.of("src/main/resources/aion/data/static_data/zones");
			try (java.util.stream.Stream<java.nio.file.Path> files = java.nio.file.Files.list(dir)) {
				for (java.nio.file.Path path : files.sorted().toList()) {
					String file = path.getFileName().toString();
					if (!file.startsWith("zones_") || !file.endsWith(".xml")) {
						continue;
					}
					java.util.regex.Matcher matcher = zoneName.matcher(java.nio.file.Files.readString(path,
						java.nio.charset.StandardCharsets.UTF_8));
					while (matcher.find()) {
						names.add(matcher.group(1));
					}
				}
			} catch (IOException e) {
				throw new IllegalStateException("DATA_DRIVEN_ZONE_REGISTRY_UNREADABLE: " + dir, e);
			}
		}
		if (names.isEmpty()) {
			throw new IllegalStateException("DATA_DRIVEN_ZONE_REGISTRY_EMPTY");
		}
		return names;
	}

	/**
	 * 按路由集建立原生运行时视图。
	 * Builds the native runtime view for one routing set.
	 *
	 * @param table          真端 DD 原生行模型 / the native DD row model
	 * @param routedQuestIds 本车道接管的 quest id（P7 步 f 的切换集）/ the quest ids this lane owns
	 * @param nameResolver   真端名字解析器（路由集非空时必需）/ retail name resolver (required when routing)
	 * @param enterAreaPort  真端同名区端口（路由集非空时必需）/ retail same-name zone port (required when routing)
	 * @param itemIndex      真端物品名索引（路由集非空时必需；ItemPlay 载荷解析）/ retail item-name index
	 *                       (required when routing; resolves ItemPlay payloads)
	 * @param inventoryPort  发扣物品端口（路由集非空时必需；步 e1 动作面）/ inventory port (required when
	 *                       routing; the e1 give/remove action face)
	 * @param moviePort      过场端口（路由集非空时必需；步 e1 CUTSCENE 动作面）/ movie port (required when
	 *                       routing; the e1 cutscene action face)
	 */
	public static DataDrivenNativeRuntime create(DataDrivenQuestTable table, Set<Integer> routedQuestIds,
			NativeNpcNameResolver nameResolver, NativeEnterAreaPort enterAreaPort, RetailItemNameIndex itemIndex,
			NativeInventoryPort inventoryPort, NativeMoviePort moviePort, NativeTeleportPort teleportPort,
			NativeSpawnPort spawnPort, NativeSayPort sayPort, NativeTimerPort timerPort) {
		if (table == null) {
			throw new IllegalArgumentException("DATA_DRIVEN_TABLE_MISSING");
		}
		if (routedQuestIds == null || routedQuestIds.isEmpty()) {
			return new DataDrivenNativeRuntime(Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of(),
				Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Set.of(), Set.of(),
				Map.of(), Set.of(), null, null, null, null, null, null);
		}
		Objects.requireNonNull(nameResolver, "DATA_DRIVEN_NAME_RESOLVER_MISSING");
		Objects.requireNonNull(enterAreaPort, "DATA_DRIVEN_ENTER_AREA_PORT_MISSING");
		Objects.requireNonNull(itemIndex, "DATA_DRIVEN_ITEM_INDEX_MISSING");
		Objects.requireNonNull(inventoryPort, "DATA_DRIVEN_INVENTORY_PORT_MISSING");
		Objects.requireNonNull(moviePort, "DATA_DRIVEN_MOVIE_PORT_MISSING");
		Objects.requireNonNull(teleportPort, "DATA_DRIVEN_TELEPORT_PORT_MISSING");
		Objects.requireNonNull(spawnPort, "DATA_DRIVEN_SPAWN_PORT_MISSING");
		Objects.requireNonNull(sayPort, "DATA_DRIVEN_SAY_PORT_MISSING");

		Map<Integer, List<StepPlan>> plans = new LinkedHashMap<>();
		Map<Integer, List<StepHit>> kills = new LinkedHashMap<>();
		Map<Integer, List<StepHit>> talks = new LinkedHashMap<>();
		Map<Integer, List<StepHit>> fobjs = new LinkedHashMap<>();
		Map<Integer, List<StepHit>> itemPlays = new LinkedHashMap<>();
		Map<String, List<StepHit>> zones = new LinkedHashMap<>();
		Map<Integer, List<StepHit>> worlds = new LinkedHashMap<>();
		Map<Integer, List<Integer>> pvpSteps = new LinkedHashMap<>();
		Map<Integer, List<Integer>> acquireTalks = new LinkedHashMap<>();
		Map<Integer, List<Integer>> acquireItems = new LinkedHashMap<>();
		Map<Integer, List<Integer>> acquireWorlds = new LinkedHashMap<>();
		Map<Integer, List<Integer>> acquireLevels = new LinkedHashMap<>();
		Map<String, List<Integer>> acquireZones = new LinkedHashMap<>();
		Map<Integer, List<List<ActionPlan>>> actionPlans = new LinkedHashMap<>();
		Map<Integer, AcquirePlan> acquirePlans = new LinkedHashMap<>();
		Set<Integer> owned = new TreeSet<>();
		Set<Integer> routed = new TreeSet<>();
		Map<Integer, FreezeReason> frozen = new TreeMap<>();
		Set<String> unresolved = new TreeSet<>();

		for (int questId : new TreeSet<>(routedQuestIds)) {
			Row row = table.find(questId).orElse(null);
			if (row == null) {
				continue;
			}
			owned.add(questId);
			RowPlan rowPlan = planRow(questId, row, nameResolver, enterAreaPort, itemIndex);
			FreezeReason freeze = rowPlan.freezeReason();
			// 接取轴在路由裁定前解析：接取参数名失败同样整行冻结（routed ∪ frozen 互斥闭合）。
			// The acquire axis resolves before the routing decision: a failed acquire parameter
			// freezes the whole row (routed/frozen stay disjoint).
			AcquirePlan acquire = freeze == null ? acquirePlan(row, nameResolver, itemIndex, unresolved) : null;
			if (freeze == null && acquire == null) {
				freeze = FreezeReason.NAME_UNRESOLVED;
			}
			if (freeze != null) {
				frozen.put(questId, freeze);
				rowPlan.unresolvedNames().forEach(unresolved::add);
				continue;
			}
			routed.add(questId);
			plans.put(questId, rowPlan.steps());
			rowPlan.kills().forEach(hit -> kills.computeIfAbsent(hit.npcKey(), key -> new ArrayList<>()).add(hit.hit()));
			rowPlan.talks().forEach(hit -> talks.computeIfAbsent(hit.npcKey(), key -> new ArrayList<>()).add(hit.hit()));
			rowPlan.fobjs().forEach(hit -> fobjs.computeIfAbsent(hit.npcKey(), key -> new ArrayList<>()).add(hit.hit()));
			rowPlan.itemPlays().forEach(hit -> itemPlays.computeIfAbsent(hit.npcKey(), key -> new ArrayList<>())
				.add(hit.hit()));
			rowPlan.zones().forEach(hit -> zones.computeIfAbsent(hit.key(), key -> new ArrayList<>()).add(hit.hit()));
			rowPlan.worlds().forEach(hit -> worlds.computeIfAbsent(hit.npcKey(), key -> new ArrayList<>()).add(hit.hit()));
			if (!rowPlan.pvpSteps().isEmpty()) {
				pvpSteps.put(questId, List.copyOf(rowPlan.pvpSteps()));
			}
			if (!rowPlan.facedActions().isEmpty()) {
				actionPlans.put(questId, rowPlan.facedActions());
			}
			acquirePlans.put(questId, acquire);
			switch (acquire.kind()) {
				case 4 -> acquire.npcIds().forEach(
					npcId -> acquireTalks.computeIfAbsent(npcId, key -> new ArrayList<>()).add(questId));
				case 3 -> acquireItems.computeIfAbsent(acquire.itemId(), key -> new ArrayList<>()).add(questId);
				case 7 -> acquireWorlds.computeIfAbsent(acquire.worldId(), key -> new ArrayList<>()).add(questId);
				case 8, 10 -> acquireLevels.computeIfAbsent(acquire.level(), key -> new ArrayList<>()).add(questId);
				case 6 -> acquireZones.computeIfAbsent(acquire.zoneAlias().toUpperCase(Locale.ROOT),
					key -> new ArrayList<>()).add(questId);
				default -> {
				}
			}
		}
		return new DataDrivenNativeRuntime(Map.copyOf(plans), Map.copyOf(kills), Map.copyOf(talks), Map.copyOf(fobjs),
			Map.copyOf(itemPlays), Map.copyOf(zones), Map.copyOf(worlds), Map.copyOf(pvpSteps),
			Map.copyOf(acquireTalks), Map.copyOf(acquireItems), Map.copyOf(acquireWorlds), Map.copyOf(acquireLevels),
			Map.copyOf(acquireZones), Map.copyOf(actionPlans), Map.copyOf(acquirePlans), Set.copyOf(owned),
			Set.copyOf(routed), Map.copyOf(frozen), Set.copyOf(unresolved), inventoryPort, moviePort, teleportPort,
			spawnPort, sayPort, timerPort);
	}

	/** 一行计划的中转结构（构建期）。 / Mutable per-row plan during construction. */
	private record RowPlan(List<StepPlan> steps, List<NpcHit> kills, List<NpcHit> talks, List<NpcHit> fobjs,
			List<NpcHit> itemPlays, List<KeyHit> zones, List<NpcHit> worlds, List<Integer> pvpSteps,
			List<List<ActionPlan>> facedActions, FreezeReason freezeReason, List<String> unresolvedNames) {
	}

	/** NPC 键命中（击杀/对话/FOBJ/世界按 int 键，进区按字符串键）。 / An interest hit bound to a key. */
	private record NpcHit(int npcKey, StepHit hit) {
	}

	private record KeyHit(String key, StepHit hit) {
	}

	private static RowPlan planRow(int questId, Row row, NativeNpcNameResolver nameResolver,
			NativeEnterAreaPort enterAreaPort, RetailItemNameIndex itemIndex) {
		List<StepPlan> steps = new ArrayList<>();
		List<NpcHit> kills = new ArrayList<>();
		List<NpcHit> talks = new ArrayList<>();
		List<NpcHit> fobjs = new ArrayList<>();
		List<NpcHit> itemPlays = new ArrayList<>();
		List<KeyHit> zones = new ArrayList<>();
		List<NpcHit> worlds = new ArrayList<>();
		List<Integer> pvpSteps = new ArrayList<>();
		List<List<ActionPlan>> facedActions = new ArrayList<>();
		List<String> unresolved = new ArrayList<>();
		FreezeReason freeze = null;
		int stepCount = row.steps().size();
		for (Step step : row.steps()) {
			boolean lastStep = step.index() == stepCount - 1;
			switch (step.kind()) {
				case HUNT -> {
					List<PayloadGroup> groups = parseGroups(step.payload());
					if (groups.isEmpty()) {
						freeze = FreezeReason.PAYLOAD_INVALID;
						break;
					}
					List<DataDrivenProgress.Slot> slots = new ArrayList<>();
					for (int index = 0; index < groups.size(); index++) {
						PayloadGroup group = groups.get(index);
						int groupNumber = index + 1;
						slots.add(new DataDrivenProgress.Slot(groupNumber, Math.max(1, group.trailing())));
						Set<Integer> npcIds = new LinkedHashSet<>();
						for (String name : group.names()) {
							if (!resolveMonsters(name, nameResolver, npcIds, unresolved)) {
								freeze = FreezeReason.NAME_UNRESOLVED;
								break;
							}
						}
						if (freeze != null) {
							break;
						}
						for (int npcId : npcIds) {
							kills.add(new NpcHit(npcId, new StepHit(questId, step.index(), groupNumber)));
						}
					}
					if (freeze != null) {
						break;
					}
					steps.add(new StepPlan(step.kind(), true, List.copyOf(slots), null, lastStep));
				}
				case TALK, COLLECT_ITEM -> {
					// 真端 kind 4（Talk）/ kind 1（CollectItem）：载荷 = NPC 名（组名展开全组成员），
					// 推进面 = 共享对话平面（`FUN_180c474b0`：开页 select(K+1) + 页动作/1009）。
					// Retail kinds 4/1: the payload names (quest_ai_name groups expand to every member)
					// feed the shared dialog plane.
					Set<Integer> npcIds = new LinkedHashSet<>();
					for (PayloadGroup group : parseGroups(step.payload())) {
						for (String name : group.names()) {
							if (!resolveMonsters(name, nameResolver, npcIds, unresolved)) {
								freeze = FreezeReason.NAME_UNRESOLVED;
								break;
							}
						}
						if (freeze != null) {
							break;
						}
					}
					if (freeze != null) {
						break;
					}
					if (npcIds.isEmpty()) {
						freeze = FreezeReason.PAYLOAD_INVALID;
						break;
					}
					for (int npcId : npcIds) {
						talks.add(new NpcHit(npcId, new StepHit(questId, step.index(), 0)));
					}
					steps.add(new StepPlan(step.kind(), false, List.of(), null, lastStep));
				}
				case TALK_FOBJ -> {
					List<PayloadGroup> groups = parseGroups(step.payload());
					if (groups.isEmpty()) {
						freeze = FreezeReason.PAYLOAD_INVALID;
						break;
					}
					// 真端 TalkFOBJ 组计数是 0→1 二值（`FUN_180c478e0`：仅 `counter == 0` 时置 1），
					// 载荷尾整数是**动作类型**（0/1/2），不是计数 ⇒ 目标恒 1。
					// The retail TalkFOBJ counter is binary (0→1) and the trailing integer is the action type.
					List<DataDrivenProgress.Slot> slots = new ArrayList<>();
					for (int index = 0; index < groups.size(); index++) {
						PayloadGroup group = groups.get(index);
						slots.add(new DataDrivenProgress.Slot(index + 1, 1));
						Set<Integer> npcIds = new LinkedHashSet<>();
						for (String name : group.names()) {
							if (!resolveMonsters(name, nameResolver, npcIds, unresolved)) {
								freeze = FreezeReason.NAME_UNRESOLVED;
								break;
							}
						}
						if (freeze != null) {
							break;
						}
						for (int npcId : npcIds) {
							fobjs.add(new NpcHit(npcId, new StepHit(questId, step.index(), index + 1)));
						}
					}
					if (freeze != null) {
						break;
					}
					steps.add(new StepPlan(step.kind(), true, List.copyOf(slots), null, lastStep));
				}
				case ITEM_PLAY -> {
					// 真端 kind 3：载荷 = 物品名（可选「, 计数」，装载器缺省计数 1）；进度 = 物品获得事件
					// （事件 5，`FUN_180c46e90`：物品 id 匹配 + 组 1 计数 + 步进/收口）。
					// Retail kind 3: payload = item name with an optional count (loader default 1); the
					// progress event is the item-acquire event keyed by the resolved item id.
					String text = step.payload().trim();
					int count = 1;
					Matcher matcher = TRAILING_INT.matcher(text);
					if (matcher.matches()) {
						count = Integer.parseInt(matcher.group(2));
						text = matcher.group(1).trim();
					}
					if (text.isEmpty()) {
						freeze = FreezeReason.PAYLOAD_INVALID;
						break;
					}
					Integer itemId = itemIndex.resolve(text);
					if (itemId == null) {
						freeze = FreezeReason.NAME_UNRESOLVED;
						unresolved.add(text);
						break;
					}
					if (count <= 0) {
						freeze = FreezeReason.PAYLOAD_INVALID;
						break;
					}
					itemPlays.add(new NpcHit(itemId, new StepHit(questId, step.index(), 1)));
					steps.add(new StepPlan(step.kind(), true,
						List.of(new DataDrivenProgress.Slot(1, count)), null, lastStep));
				}
				case ENTER_AREA -> {
					Optional<String> zone = enterAreaPort.zoneName(questId, step.index());
					if (zone.isEmpty()) {
						freeze = enterAreaPort.absentAliases(questId).contains(step.payload().trim())
							? FreezeReason.ZONE_ABSENT : FreezeReason.ZONE_UNRESOLVED;
						unresolved.add(step.payload().trim());
						break;
					}
					zones.add(new KeyHit(zone.get().toUpperCase(Locale.ROOT),
						new StepHit(questId, step.index(), 0)));
					steps.add(new StepPlan(step.kind(), false, List.of(), null, lastStep));
				}
				case ENTER_WORLD -> {
					int worldId = parseWorldId(step.payload());
					if (worldId <= 0) {
						freeze = FreezeReason.PAYLOAD_INVALID;
						break;
					}
					worlds.add(new NpcHit(worldId, new StepHit(questId, step.index(), 0)));
					steps.add(new StepPlan(step.kind(), false, List.of(), null, lastStep));
				}
				case PVP -> {
					int target = parseCount(step.payload());
					if (target <= 0) {
						freeze = FreezeReason.PAYLOAD_INVALID;
						break;
					}
					PvpGate gate = new PvpGate(parseOptionalCount(step.column(1)), parseOptionalCount(step.column(2)),
						parseOptionalCount(step.column(3)));
					steps.add(new StepPlan(step.kind(), true,
						List.of(new DataDrivenProgress.Slot(1, target)), gate, lastStep));
					pvpSteps.add(step.index());
				}
			}
			if (freeze == null) {
				freeze = scanFacedActions(step, nameResolver, itemIndex, facedActions, unresolved);
			}
			if (freeze != null) {
				break;
			}
		}
		return new RowPlan(List.copyOf(steps), List.copyOf(kills), List.copyOf(talks), List.copyOf(fobjs),
			List.copyOf(itemPlays), List.copyOf(zones), List.copyOf(worlds), List.copyOf(pvpSteps),
			List.copyOf(facedActions), freeze, List.copyOf(unresolved));
	}

	/**
	 * 步 e2 附加动作面：逐步扫描附加动作列。已落面（真端执行器 case 1/2/3/4/5/7）解析成
	 * {@link ActionPlan}；case 6/8 只在 EnterArea/TalkFOBJ 步活（def 侧槽未定名 ⇒ 这两类步冻结，
	 * 其余 kind 真端执行器无该 case = 装载即死列，镜像忽略）；case 9/10（EnterInstance/Timer）
	 * 宿主渲染面未坐实 ⇒ ACTION_UNFACED；物品符号/字符串键解析失败返回 NAME_UNRESOLVED，
	 * 成功返回 {@code null}。
	 * e2 extra-action face: parse the faced executor cases (1/2/3/4/5/7); cases 6/8 are live only on
	 * EnterArea/TalkFOBJ steps (unnamed def-side slots ⇒ those freeze, other kinds mirror-ignore the
	 * retail-dead column); cases 9/10 stay ACTION_UNFACED; an unresolved item symbol or string key
	 * yields NAME_UNRESOLVED, success yields {@code null}.
	 */
	private static FreezeReason scanFacedActions(Step step, NativeNpcNameResolver nameResolver,
			RetailItemNameIndex itemIndex, List<List<ActionPlan>> facedActions, List<String> unresolved) {
		List<ActionPlan> plans = new ArrayList<>();
		for (DataDrivenQuestTable.ExtraAction action : step.extraActions()) {
			int columnIndex = actualColumnOf(step, action);
			String text = step.column(columnIndex);
			switch (action) {
				case GIVE_ITEMS, REMOVE_ITEMS -> {
					// 真端 `符号 数量`（可多对，`,`/空格分隔）；符号经物品名索引解析。
					// Retail `symbol count` pairs separated by commas/spaces; symbols resolve via the item index.
					String[] tokens = text.trim().split("[,\\s]+");
					if (tokens.length % 2 != 0) {
						return FreezeReason.ACTION_UNFACED;
					}
					for (int index = 0; index < tokens.length; index += 2) {
						int count = parseRetailInt(tokens[index + 1]);
						if (tokens[index + 1].isEmpty() || count <= 0) {
							return FreezeReason.ACTION_UNFACED;
						}
						Integer itemId = itemIndex.resolve(tokens[index]);
						if (itemId == null) {
							unresolved.add(tokens[index].toLowerCase(java.util.Locale.ROOT));
							return FreezeReason.NAME_UNRESOLVED;
						}
						plans.add(new ActionPlan(
							action == DataDrivenQuestTable.ExtraAction.GIVE_ITEMS
								? ActionType.GIVE_ITEMS : ActionType.REMOVE_ITEMS,
							itemId, count, 0, false, -1, 0, 0, 0, 0, false, -1));
					}
				}
				case CUTSCENE -> {
					// 真端 `Cutscene|Cutscene2|Movie|Movie2 N`（+可选 HACTION 链接，忽略）；
					// 未知词形 = 真端 Wrong Type!!（记日志跳过），不冻结。
					// Retail `Cutscene|Cutscene2|Movie|Movie2 N` (+ optional HACTION links, ignored);
					// an unknown token is the retail Wrong Type!! (log + skip), mirrored without freezing.
					String[] tokens = text.trim().split("[,\\s]+");
					if (tokens.length < 2) {
						return FreezeReason.ACTION_UNFACED;
					}
					String token = tokens[0];
					int movieId = parseRetailInt(tokens[1]);
					if (tokens[1].isEmpty()) {
						return FreezeReason.ACTION_UNFACED;
					}
					if (token.equalsIgnoreCase("Cutscene") || token.equalsIgnoreCase("Cutscene2")) {
						plans.add(new ActionPlan(ActionType.CUTSCENE, 0, 0, movieId, false, -1, 0, 0, 0, 0, false,
							-1));
					} else if (token.equalsIgnoreCase("Movie") || token.equalsIgnoreCase("Movie2")) {
						plans.add(new ActionPlan(ActionType.CUTSCENE, 0, 0, movieId, true, -1, 0, 0, 0, 0, false,
							-1));
					}
				}
				case TELEPORT -> {
					// 真端 case 3 → `IUserImp::Teleport(world, x, y, z+1, heading, 1)`；载荷 = 5 个整数。
					// Retail case 3 → IUserImp::Teleport; the payload is five integers.
					String[] tokens = text.trim().split("[,\\s]+");
					if (tokens.length < 5) {
						return FreezeReason.ACTION_UNFACED;
					}
					int worldId = parseRetailInt(tokens[0]);
					float x = parseRetailInt(tokens[1]);
					float y = parseRetailInt(tokens[2]);
					float z = parseRetailInt(tokens[3]);
					int heading = parseRetailInt(tokens[4]);
					if (tokens[0].isEmpty() || worldId <= 0) {
						return FreezeReason.ACTION_UNFACED;
					}
					plans.add(new ActionPlan(ActionType.TELEPORT, 0, 0, 0, false, worldId, x, y, z, heading, false,
						-1));
				}
				case SPAWN -> {
					// 真端 case 5 → `IUserImp::Spawn`：`Absolute|Relative 名, 数量, 时间[, x y z heading]`
					// 可 `;` 连多组；名字经 NPC 名空间解析。
					// Retail case 5 → IUserImp::Spawn: multi-group spawn declarations.
					String[] tokens = text.trim().split("[,\\s;]+");
					int index = 0;
					while (index < tokens.length) {
						boolean relative;
						if (tokens[index].equalsIgnoreCase("Relative")) {
							relative = true;
						} else if (tokens[index].equalsIgnoreCase("Absolute")) {
							relative = false;
						} else {
							return FreezeReason.ACTION_UNFACED;
						}
						index++;
						if (index >= tokens.length) {
							return FreezeReason.ACTION_UNFACED;
						}
						Set<Integer> npcIds = new LinkedHashSet<>();
						if (!resolveMonsters(tokens[index], nameResolver, npcIds, unresolved)) {
							return FreezeReason.NAME_UNRESOLVED;
						}
						index++;
						int fields = relative ? 2 : 6;
						if (index + fields > tokens.length) {
							return FreezeReason.ACTION_UNFACED;
						}
						int count = parseRetailInt(tokens[index]);
						if (count <= 0) {
							return FreezeReason.ACTION_UNFACED;
						}
						int lifeSeconds = parseRetailInt(tokens[index + 1]);
						float x = fields == 6 ? parseRetailInt(tokens[index + 2]) : 0;
						float y = fields == 6 ? parseRetailInt(tokens[index + 3]) : 0;
						float z = fields == 6 ? parseRetailInt(tokens[index + 4]) : 0;
						int heading = fields == 6 ? parseRetailInt(tokens[index + 5]) : 0;
						for (int npcId : npcIds) {
							plans.add(new ActionPlan(ActionType.SPAWN, npcId, count, lifeSeconds, false, -1, x, y, z,
								heading, relative, -1));
						}
						index += fields;
					}
				}
				case MESSAGE -> {
					// case 7（列 7）→ `IUserImp::Say`（字符串表 id，键经真端字符串表解析）；
					// case 8（列 8）= 宿主 `Npc` 槽 +0x3a0 = `Npc::Die`（宿主 NPC 死亡，真端唯一载荷行
					// 9696 = `_TEST_` 串且不路由）⇒ fail-closed 维持冻结（`defSideAction`）。
					// Column 7 → IUserImp::Say with a string-table id; column 8 targets the host-Npc
					// vtable +0x3a0 = Npc::Die (the sole retail payload row 9696 is a _TEST_ string on
					// a non-routed quest) ⇒ fail-closed freeze stays (defSideAction).
					if (columnIndex == 8) {
						FreezeReason reason = defSideAction(step);
						if (reason != null) {
							return reason;
						}
						continue;
					}
					Integer stringId = RetailStringIds.instance().resolve(text);
					if (stringId == null) {
						unresolved.add(text.trim().toLowerCase(java.util.Locale.ROOT));
						return FreezeReason.NAME_UNRESOLVED;
					}
					plans.add(new ActionPlan(ActionType.SAY, 0, 0, 0, false, -1, 0, 0, 0, 0, false, stringId));
				}
				case DELAY -> {
					// case 6（列 6，2026-10-03 落面 = 零效果镜像）：真端装载器 `FUN_180c49610` case 6 读
					// "DataDrivenQuest - Delay Time" 整数入动作向量；运行时应用器 `FUN_180c4c8d0` case 6 =
					// `(*param_3 + 0x270)(param_3, 延迟值)`，param_3 = 宿主侧 `Npc`（EXE `Npc::vftable`
					// 199 槽 @0x12b8470，槽 +0x3a0 = `Npc::Die` 交叉验证），槽 +0x270 = **空桩**
					// （`FUN_140094480` = `return;`）⇒ 真端 Delay = 装载存储、调用空桩、零效果。
					// 镜像 = 忽略该动作（真端一致，不冻结）；真端仅 10035/25606 携带（值 8/3/2）。
					// Retail case 6 (2026-10-03, zero-effect mirror): the loader FUN_180c49610 stores the
					// "Delay Time" int; the applier FUN_180c4c8d0 case 6 calls vtable +0x270 on the
					// host-side Npc (EXE Npc::vftable, 199 slots, +0x3a0 = Npc::Die cross-check) whose
					// +0x270 is an EMPTY STUB (FUN_140094480 = `return;`) ⇒ retail Delay = stored,
					// invoked, no effect. Mirror = ignore the action (retail-faithful, no freeze);
					// only 10035/25606 carry it (values 8/3/2).
				}
				case TIMER -> {
					// 真端 case 10（`FUN_180c49610` case 10，2026-10-02 取证落面）：载荷 =
					// `秒, 目标步, 旗标` 三整数（缺项 = 真端装载失败日志 "Add Timer - Timer Time /
					// Dest Progress is Not Exists"）；旗标 0 = 到期推进到目标步 / 1 = 到期弃任
					// （到期面 `FUN_180c46d80`：状态 3 ∧ `0 < 当前步 < 目标步` ⇒ `+0xf0` 直写 /
					// `+0x160` 弃任；300~14100 数值域 = 秒，与 NPCServer 计时中转一致）。
					// 字段复用先例同 SPAWN：movieId 槽 = 目标步、itemId 槽 = 旗标、count 槽 = 秒。
					// Retail case 10 (FUN_180c49610 "Add Timer", adjudicated 2026-10-02): payload =
					// `seconds, destStep, flag` (missing tokens are retail load failures); flag 0 =
					// advance to destStep on expiry / 1 = abandon (expiry face FUN_180c46d80: status 3
					// ∧ `0 < cur < dest` → +0xf0 direct write / +0x160 abandon). Field-slot reuse per
					// the SPAWN precedent: movieId = destStep, itemId = flag, count = seconds.
					String[] tokens = text.trim().split("[,\\s]+");
					if (tokens.length < 3 || tokens[0].isEmpty() || tokens[1].isEmpty() || tokens[2].isEmpty()) {
						return FreezeReason.ACTION_UNFACED;
					}
					int seconds = parseRetailInt(tokens[0]);
					int destStep = parseRetailInt(tokens[1]);
					int flag = parseRetailInt(tokens[2]);
					if (seconds <= 0 || destStep <= 0 || destStep > DataDrivenProgress.STEP_MASK
						|| (flag != 0 && flag != 1)) {
						return FreezeReason.ACTION_UNFACED;
					}
					plans.add(new ActionPlan(ActionType.TIMER, flag, seconds, destStep, false, -1, 0, 0, 0, 0, false,
						-1));
				}
				case ENTER_INSTANCE -> {
					// 真端 case 9（`FUN_180c49610` case 9，2026-10-03 第九批落面）：载荷 =
					// `creationId, worldId, leaveProgress[, 成员名…]`；立即执行面 = 完成步应用器
					// `FUN_180c4c8d0` case 9 = `(*param_2+0x220)(param_2, creationId)` 单参虚调
					// `User::EnterInstance` → 落点 = `instance_creation.xml` 的 `start_point_alias`
					// → 真端 `Map/Worlds/<world>/world.xml` `location_alias_list` 坐标（解析器
					// `WorldDb::LoadInstanceCreation`）。别名在真端 world.xml 缺失时真端自身只记
					// 错误日志、落点空置（creation 2 = IDElim 即此内在缺失）⇒ 镜像 fail-closed
					// 冻结该行。离场检查面（`FUN_180c46d80` 块 1：`def+0x40 锚 < 步 < def+0x44`
					// ⇒ `+0xf0` 写回锚值）的宿主触发链停在 DLL 数据段（mgr+0x48 表全库无读者）
					// 且写锚 param_7 身份未定 ⇒ 不实现，本批登记为携带行上的残余偏差；成员名尾巴
					// （仅冻结行 20032 携带）语义未证 ⇒ 解析忽略。
					// Retail case 9 (batch 9, 2026-10-03): payload = `creationId, worldId,
					// leaveProgress[, names]`; the immediate face enters the instance world at the
					// start point resolved from the retail world files. An intrinsically absent
					// alias (creation 2 = IDElim) keeps that row fail-closed frozen. The
					// leave-check face stays unmirrored (the host-side trigger and the write
					// anchor are unresolved); trailing member names (frozen row 20032 only) are
					// parsed and ignored.
					String[] tokens = text.trim().split("[,\\s]+");
					if (tokens.length < 3) {
						return FreezeReason.PAYLOAD_INVALID;
					}
					int creationId = parseRetailInt(tokens[0]);
					int payloadWorldId = parseWorldId(tokens[1]);
					int leaveProgress = parseRetailInt(tokens[2]);
					if (creationId <= 0 || payloadWorldId <= 0 || leaveProgress < 0) {
						return FreezeReason.PAYLOAD_INVALID;
					}
					NativeInstanceEntryPort.EntryPoint entry = NativeInstanceEntryPort.instance()
						.entry(creationId).orElse(null);
					if (entry == null || !entry.resolved() || entry.worldId() != payloadWorldId) {
						// 真端注册表缺行 / 别名真端本就缺失 / 载荷与注册表 worldId 不符 ⇒ fail-closed。
						// Missing registry row / intrinsically absent alias / payload-vs-registry
						// world mismatch ⇒ fail closed.
						return FreezeReason.ACTION_UNFACED;
					}
					plans.add(new ActionPlan(ActionType.ENTER_INSTANCE, 0, 0, creationId, false, payloadWorldId,
						entry.x(), entry.y(), entry.z(), entry.heading(), false, -1));
				}
				default -> {
					// 其余未落面动作一律 fail-closed 冻结。
					// Anything else unfaced stays fail-closed frozen.
					return FreezeReason.ACTION_UNFACED;
				}
			}
		}
		facedActions.add(List.copyOf(plans));
		return null;
	}

	/**
	 * case 8（MESSAGE 列 8）的活面判定：EnterArea/TalkFOBJ 步 = 宿主 `Npc` 槽 +0x3a0 = `Npc::Die`
	 * （真端语义 = 宿主 NPC 死亡；EXE `Npc::vftable` 199 槽交叉验证）⇒ 未落面维持冻结；
	 * 其余 kind = 真端执行器变体（`FUN_180c4cd50`/`FUN_180c4d190`）无 case 8 = 装载即死列 ⇒ 忽略。
	 * （case 6 DELAY 已另案落面 = 空桩零效果镜像忽略，不经此函数。）
	 * Kind-8 (MESSAGE column 8) live-face test: EnterArea/TalkFOBJ steps target the host-Npc vtable
	 * +0x3a0 = Npc::Die (retail semantics: the host NPC dies; EXE Npc::vftable cross-check) ⇒ the
	 * face stays unfaced/frozen; other kinds: dead columns in the executor variants. (case 6 DELAY
	 * is adjudicated separately as an empty-stub no-op and no longer routes here.)
	 */
	private static FreezeReason defSideAction(Step step) {
		return switch (step.kind()) {
			case ENTER_AREA, TALK_FOBJ -> FreezeReason.ACTION_UNFACED;
			default -> null;
		};
	}

	/** MESSAGE 动作的实际列号（列 7 优先；仅声明列 8 时取 8）。 / The actual column of a MESSAGE action. */
	private static int actualColumnOf(Step step, DataDrivenQuestTable.ExtraAction action) {
		if (action == DataDrivenQuestTable.ExtraAction.MESSAGE && step.column(action.column()) == null) {
			return 8;
		}
		return action.column();
	}

	/**
	 * 真端数字 token 解析镜像（`FUN_18107c0f0` = `wcstoul` base 10：前导整数截断，
	 * `83.9`→83、无数字→0）。
	 * The retail numeric-token parse mirror (wcstoul: leading integer truncated, no digits → 0).
	 */
	private static int parseRetailInt(String token) {
		String text = token.trim();
		int start = text.startsWith("+") || text.startsWith("-") ? 1 : 0;
		int end = start;
		while (end < text.length() && Character.isDigit(text.charAt(end))) {
			end++;
		}
		if (end == start) {
			return 0;
		}
		try {
			return Integer.parseInt(text.substring(0, end));
		} catch (NumberFormatException e) {
			return 0;
		}
	}


	/**
	 * 解析组内怪物名：先整体解析；失败且名字含空白时按空白拆分逐一解析（真端表里有
	 * `LF2A_StatueT_49_An LF2A_StatueT_50_An` 这类空格分隔名单），仍失败则登记未解析名。
	 * Resolves one monster name; a whitespace-joined name list falls back to per-token resolution.
	 */
	private static boolean resolveMonsters(String name, NativeNpcNameResolver resolver, Set<Integer> out,
			List<String> unresolved) {
		List<Integer> direct = resolver.resolveMonsterIds(name);
		if (!direct.isEmpty()) {
			out.addAll(direct);
			return true;
		}
		if (name.chars().anyMatch(Character::isWhitespace)) {
			Set<Integer> split = new LinkedHashSet<>();
			for (String token : name.trim().split("\\s+")) {
				List<Integer> part = resolver.resolveMonsterIds(token);
				if (part.isEmpty()) {
					unresolved.add(token);
					return false;
				}
				split.addAll(part);
			}
			if (!split.isEmpty()) {
				out.addAll(split);
				return true;
			}
		}
		unresolved.add(name);
		return false;
	}

	/** 解析 `A, B, C 3; D 1;` 形的载荷组（组内名字逗号分隔，尾整数空格或逗号分隔）。 */
	private static List<PayloadGroup> parseGroups(String payload) {
		List<PayloadGroup> groups = new ArrayList<>();
		if (payload == null || payload.isBlank()) {
			return groups;
		}
		for (String raw : payload.split(";")) {
			String text = raw.trim();
			if (text.isEmpty()) {
				continue;
			}
			int trailing = 0;
			Matcher matcher = TRAILING_INT.matcher(text);
			if (matcher.matches()) {
				trailing = Integer.parseInt(matcher.group(2));
				text = matcher.group(1);
			}
			List<String> names = new ArrayList<>();
			for (String name : text.split(",")) {
				String trimmed = name.trim();
				if (!trimmed.isEmpty()) {
					names.add(trimmed);
				}
			}
			if (!names.isEmpty()) {
				groups.add(new PayloadGroup(List.copyOf(names), trailing));
			}
		}
		return groups;
	}

	private static int parseWorldId(String payload) {
		try {
			return Integer.parseInt(payload.trim());
		} catch (NumberFormatException e) {
			return -1;
		}
	}

	private static int parseCount(String payload) {
		try {
			return Integer.parseInt(payload.trim());
		} catch (NumberFormatException e) {
			return -1;
		}
	}

	private static int parseOptionalCount(String column) {
		if (column == null || column.isBlank()) {
			return 0;
		}
		return parseCount(column);
	}

	// ------------------------------------------------------------------ 事件面

	/**
	 * 击杀事件（Hunt 组计数；生产入口，带真端距离门）。50m 外死亡对象不产生进度（真端处理函数前奏
	 * case 0：{@code 2500.0 < param_6 → return}；param_6 = 成员↔死亡对象平方欧氏距离，见
	 * {@link #RETAIL_KILL_DISTANCE_SQ}）。
	 * Kill event (Hunt group counters; the production entry with the retail distance gate). Dead
	 * objects beyond 50 m yield no progress (retail handler preamble case 0); see the constant for
	 * the evidence chain.
	 */
	public boolean onKill(Player player, Npc npc) {
		if (npc == null) {
			return false;
		}
		return onKillAt(player, npc.getNpcId(), npc.getX(), npc.getY(), npc.getZ());
	}

	/**
	 * 带死亡位点坐标的击杀派发（{@link #onKill(Player, Npc)} 的坐标形；包内测试面）。
	 * Kill dispatch with the dead-object coordinates (package-visible test seam of the Npc overload).
	 */
	boolean onKillAt(Player player, int npcId, float x, float y, float z) {
		if (player == null || !withinRetailKillDistance(player.getX() - x, player.getY() - y,
				player.getZ() - z)) {
			return false;
		}
		return dispatch(player, killsByNpcId.get(npcId));
	}

	/**
	 * 击杀事件（Hunt 组计数；已过距离门的裸派发——仅供既有门禁测试沿用，生产必须走
	 * {@link #onKill(Player, Npc)}）。
	 * Kill event (Hunt group counters; the bare post-gate dispatch, kept for the existing gate tests —
	 * production must go through {@link #onKill(Player, Npc)}).
	 */
	boolean onKill(Player player, int npcId) {
		return dispatch(player, killsByNpcId.get(npcId));
	}

	/**
	 * 真端距离门判定（平方欧氏 ≤ {@link #RETAIL_KILL_DISTANCE_SQ}）。
	 * The retail gate test (squared Euclidean distance within the bound).
	 */
	static boolean withinRetailKillDistance(float dx, float dy, float dz) {
		return dx * dx + dy * dy + dz * dz <= RETAIL_KILL_DISTANCE_SQ;
	}

	/**
	 * 物品获得事件（ItemPlay 组计数；真端事件 5，`FUN_180c46e90`：当前步 kind==3 + 物品 id 匹配 +
	 * 组 1 计数步进）+ 接取双角色（状态非 START 且接取 kind==3 → 接取 + 步 0 动作）。
	 * Item-acquire event (ItemPlay group counters; retail event 5) plus the dual-role acquire
	 * branch (no START state + acquire kind 3 → acquire + step-0 actions).
	 */
	public boolean onItemAcquired(Player player, int itemId) {
		if (acquire(player, acquireItemsByItemId.get(itemId), 3)) {
			return true;
		}
		return dispatch(player, itemPlaysByItemId.get(itemId));
	}

	/**
	 * 对话事件（真端 `FUN_180c474b0` 共享对话平面：打开 → 阶段页；顺序动作/1009/10255 → 步进；
	 * 1008/其余 ≥1000 动作 → 原样回发。TalkFOBJ 组计数与动作无关，保持步 d 语义）。
	 * Dialog event: the shared retail stage-dialog plane for Talk/CollectItem steps, plus the
	 * action-agnostic TalkFOBJ binary groups.
	 *
	 * @param player         玩家 / the player
	 * @param npcId          对话 NPC 模板 id / the dialog npc template id
	 * @param dialogId       对话动作 id（31/26/-1 = 打开）/ the dialog action id (31/26/-1 = open)
	 * @param objectId       对话对象句柄（页下发用）/ the dialog object handle (for page sends)
	 * @param requestedOwner 客户端携带的任务上下文（0 = 无；非 0 时只服务该任务）/ the client quest context
	 */
	public boolean onDialog(Player player, int npcId, int dialogId, int objectId, int requestedOwner) {
		if (dispatchAcquireDialog(player, acquireTalksByNpcId.get(npcId), dialogId, objectId, requestedOwner)) {
			return true;
		}
		boolean fobj = dispatch(player, fobjsByNpcId.get(npcId));
		boolean plane = dispatchDialog(player, talksByNpcId.get(npcId), dialogId, objectId, requestedOwner);
		return plane || fobj;
	}

	/**
	 * Talk 接取对话面（真端 `FUN_180c47220`：打开 → 页 4762；1002 → 接取 + 页 1003；1003 → 页 1004；
	 * 1007 → 接取窗（客户端契约 fail-closed）；20000 → 接取 + 完成通道；20001/1008 → 完成通道；
	 * 其余 ≥1000 原样回发）。只服务「尚未开始该任务」的玩家（真端按 0x640 条件路由到接取对象）。
	 * The Talk acquire dialog face of retail FUN_180c47220, served only to players without the quest.
	 */
	private boolean dispatchAcquireDialog(Player player, List<Integer> questIds, int dialogId, int objectId,
			int requestedOwner) {
		if (player == null || questIds == null || questIds.isEmpty()) {
			return false;
		}
		for (int questId : questIds) {
			if (requestedOwner != 0 && requestedOwner != questId) {
				continue;
			}
			if (state(player, questId) != null) {
				continue;
			}
			switch (dialogId) {
				case 31, 26, -1 -> {
					PacketSendUtility.sendPacket(player,
						new SM_DIALOG_WINDOW(objectId, PAGE_ACCEPT_ENTRY, questId));
					return true;
				}
				case ACTION_ACCEPT -> {
					if (!NativeQuestStartPort.instance().start(player, questId).started()) {
						return false;
					}
					// 真端 1002 接取收尾 = FUN_180c4d5b0(user,-1,…,-1)，其 -1 路径跑步 0 动作
					// （门 = 接取 kind==4，本面只服务 talk 接取行 ⇒ 结构性满足）。
					// Retail 1002 accept tail = FUN_180c4d5b0(user,-1,…,-1) whose -1 path runs
					// step-0 actions (gated on acquire kind 4, structurally true on this face).
					runActions(player, questId, 0);
					PacketSendUtility.sendPacket(player,
						new SM_DIALOG_WINDOW(objectId, PAGE_ACCEPTED, questId));
					return true;
				}
				case ACTION_ACCEPTED -> {
					PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(objectId, PAGE_REFUSE, questId));
					return true;
				}
				case ACTION_ASK -> {
					int askWindow = QuestDialogContract.loadDefault().askWindowPage(questId);
					if (askWindow < 0) {
						return false;
					}
					PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(objectId, askWindow, questId));
					return true;
				}
				case ACTION_BOOK, ACTION_REFUSE, ACTION_COMPLETE -> {
					if (dialogId == ACTION_BOOK
						&& !NativeQuestStartPort.instance().start(player, questId).started()) {
						return false;
					}
					if (dialogId == ACTION_BOOK) {
						// 真端 20000 接取收尾同样走 FUN_180c4d5b0(-1,-1) ⇒ 步 0 动作（同 1002 面）。
						// The retail 20000 accept tail funnels into FUN_180c4d5b0(-1,-1) too: step-0
						// actions, same as the 1002 face.
						runActions(player, questId, 0);
					}
					// 真端 `mgr+0x5d8` 完成通道（20001 的 +0x2a8 演出面未坐实，e1 只回完成页）。
					// The retail mgr+0x5d8 completion channel (20001's +0x2a8 play face is
					// un-adjudicated; e1 sends the completion page only).
					PacketSendUtility.sendPacket(player,
						new SM_DIALOG_WINDOW(objectId, PAGE_COMPLETE, questId));
					return true;
				}
				default -> {
					if (dialogId >= 1000) {
						PacketSendUtility.sendPacket(player,
							new SM_DIALOG_WINDOW(objectId, dialogId, questId));
						return true;
					}
					return false;
				}
			}
		}
		return false;
	}

	/**
	 * 双角色接取（真端 progress handler 的 state!=3 分支）：接取成功即执行步 0 的已落面动作
	 * （真端 `FUN_180c4cd50(def+0x10)`）。
	 * The dual-role acquire of the retail progress handlers; a successful acquire runs step 0's
	 * faced actions (retail FUN_180c4cd50 on the first handler's action list).
	 */
	private boolean acquire(Player player, List<Integer> questIds, Integer requiredKind) {
		if (player == null || questIds == null || questIds.isEmpty()) {
			return false;
		}
		boolean handled = false;
		for (int questId : questIds) {
			AcquirePlan plan = acquireByQuestId.get(questId);
			if (plan == null || (requiredKind != null && plan.kind() != requiredKind)) {
				continue;
			}
			if (state(player, questId) != null) {
				continue;
			}
			if (!NativeQuestStartPort.instance().start(player, questId).started()) {
				continue;
			}
			runActions(player, questId, 0);
			handled = true;
		}
		return handled;
	}

	/**
	 * 执行一步的已落面动作（真端执行器 case 1/2/3/4/5/7：发/扣物品对、Cutscene/Movie、传送、刷怪、播报）。
	 * Runs one step's faced actions (retail executor cases 1/2/3/4/5/7).
	 */
	private void runActions(Player player, int questId, int stepIndex) {
		if (inventoryPort == null || moviePort == null) {
			return;
		}
		List<List<ActionPlan>> perStep = actionsByQuestId.get(questId);
		if (perStep == null || stepIndex < 0 || stepIndex >= perStep.size()) {
			return;
		}
		for (ActionPlan action : perStep.get(stepIndex)) {
			switch (action.type()) {
				case GIVE_ITEMS -> inventoryPort.give(player, action.itemId(), action.count());
				case REMOVE_ITEMS -> inventoryPort.remove(player, action.itemId(), action.count());
				case CUTSCENE -> {
					if (action.movieToken()) {
						moviePort.playMovie(player, action.movieId());
					} else {
						moviePort.play(player, action.movieId());
					}
				}
				case TELEPORT -> teleportPort.teleport(player, action.worldId(), action.x(), action.y(), action.z(),
					action.heading());
				case SPAWN -> spawnPort.spawn(player, action.itemId(), action.count(), action.relative(), action.x(),
					action.y(), action.z(), action.heading(), lifeSeconds(action));
				case SAY -> sayPort.say(player, action.stringId());
				case TIMER -> timerPort.schedule(player, questId, action.count(), action.movieId(),
					action.itemId() == 1);
				case ENTER_INSTANCE -> {
					// 真端 case 9 立即面 = `+0x220(creationId)` → `User::EnterInstance` → 落点 =
					// 别名坐标（NativeInstanceEntryPort）；传送端口 = Java 侧等价面（与 case 3 同
					// 通道，heading 度）。
					// Retail case 9 immediate face: enter the instance world at the alias-resolved
					// start point (the teleport port is the Java-side equivalent, degree heading).
					teleportPort.teleport(player, action.worldId(), action.x(), action.y(), action.z(),
						action.heading());
				}
			}
		}
	}

	/** 刷怪存活秒（真端刷怪单 time 参数；0 = 不定时回收）。 / The spawn lifetime seconds (0 = no despawn). */
	private static int lifeSeconds(ActionPlan action) {
		// ActionPlan 未用字段回收：SPAWN 的 movieId 槽存 time（见 scanFacedActions）。
		// SPAWN reuses the movieId slot for the time parameter (see scanFacedActions).
		return action.movieId();
	}

	/**
	 * 任务计时到期判定（真端 `FUN_180c46d80` 镜像，2026-10-02 落面）：任务仍进行中（状态 START）
	 * ∧ 守卫位正常 ∧ `0 < 当前步 < 目标步` ⇒ 旗标 0 = 直写步号到目标步（`+0xf0` SetQuestProgress
	 * 面，纯进度写、不触发步动作执行器）/ 旗标 1 = 弃任（`+0x160` 面）。范围外/非进行中 = 无操作
	 * （真端同款：玩家已过目标步或已完成时到期回调零动作）。
	 * Quest-timer expiry verdict (mirror of retail FUN_180c46d80): while the quest is still in
	 * progress (START) with clear guard bits and `0 < current step < dest`, flag 0 writes the step
	 * directly to dest (the +0xf0 SetQuestProgress face — a pure progress write, no executor run)
	 * and flag 1 abandons (+0x160). Out-of-range or non-active = a no-op, exactly like retail.
	 */
	public void onQuestTimerExpired(Player player, int questId, int destStep, boolean abandonOnExpiry) {
		if (player == null || player.getQuestStateList() == null || !routedQuestIds.contains(questId)) {
			return;
		}
		QuestState state = player.getQuestStateList().getQuestState(questId);
		if (state == null || state.getStatus() != QuestStatus.START) {
			return;
		}
		int vars = state.getQuestVars().getQuestVars();
		if (!DataDrivenProgress.guardClear(vars)) {
			return;
		}
		int currentStep = DataDrivenProgress.step(vars);
		if (currentStep <= 0 || currentStep >= destStep) {
			return;
		}
		if (abandonOnExpiry) {
			QuestService.abandonQuest(player, questId);
			return;
		}
		int newVars = DataDrivenProgress.jumpTo(vars, destStep);
		state.getQuestVars().setVar(newVars);
		state.setPersistentState(PersistentState.UPDATE_REQUIRED);
		PacketSendUtility.sendPacket(player, new SM_QUEST_ACTION(questId, state.getStatus(), newVars));
	}

	/**
	 * 进区事件：接取双角色（真端区 handler `FUN_180c47bf0` 尾段的独立接取侧名字哈希树：接取 kind==6 且
	 * 区名哈希命中 → `(+0xd8)` 接取 + 步 0 动作）+ EnterArea 直接步进。未注册的别名（如真端无区定义的
	 * `DF6_QuestArea_Q25674`）在本服永不派发进区事件 ⇒ 兴趣键登记原文 = 镜像真端死边。
	 * Enter-zone event: the dual-role acquire of the retail zone handler's separate acquire-side
	 * name-hash tree (kind 6 → SetQuestAcquired + step-0 actions), then the EnterArea advance.
	 */
	public boolean onEnterZone(Player player, String zoneName) {
		if (zoneName == null) {
			return false;
		}
		String key = zoneName.toUpperCase(Locale.ROOT);
		if (acquire(player, acquireZonesByName.get(key), 6)) {
			return true;
		}
		return dispatch(player, zonesByName.get(key));
	}

	/**
	 * 进世界事件（EnterWorld 直接步进）+ 接取双角色（状态非 START 且接取 kind==7 → 接取 + 步 0 动作）。
	 * Enter-world event (EnterWorld advance) plus the dual-role acquire branch (kind 7).
	 */
	public boolean onEnterWorld(Player player, int worldId) {
		if (acquire(player, acquireWorldsByWorldId.get(worldId), 7)) {
			return true;
		}
		return dispatch(player, worldsByWorldId.get(worldId));
	}

	/**
	 * 登录/升级接取（kind 8/10，真端 vec0/vec3 遍历：`ctx+8 == def+8` **等级等值**才接取；
	 * 登录面只服务 kind 10，升级面 8 与 10 都服务）。真端登录面另有 +0x138 否决槽（拒绝/删除簿），
	 * 本服无对应簿面 ⇒ 不镜像（接取仍受 `NativeQuestStartPort.start` 条件面约束）。
	 * Login/level-up acquire (kinds 8/10; the retail walks acquire on **exact level equality**;
	 * login serves kind 10 only, level-up serves both). The retail login veto slot +0x138 has no
	 * counterpart on this server ⇒ not mirrored (the start-port conditions still gate).
	 */
	public boolean onLevelReached(Player player, int level, boolean loginWalk) {
		return acquire(player, acquireLevelsByLevel.get(level), loginWalk ? Integer.valueOf(10) : null);
	}

	/**
	 * PvP 击杀事件（单组计数 + 军衔/等级闸门）。 / Ranked kill event (single group + rank/level gate).
	 */
	public boolean onKillRanked(Player killer, Player victim, AbyssRankEnum victimRank) {
		if (killer == null || victim == null || victimRank == null || pvpStepsByQuestId.isEmpty()) {
			return false;
		}
		// 真端 Pvp 处理函数（`FUN_180c46980`，槽 +0x528）与 Hunt 共用同一距离前奏：成员↔死亡玩家
		// 平方距离 > 2500（50m）⇒ 该次进度零动作（def+0x70 恒 0 ⇒ 恒 case 0，见
		// RETAIL_KILL_DISTANCE_SQ；真端 GetValidMember 同样以死亡对象为距离基准）。
		// The retail PvP handler shares the Hunt distance preamble: beyond 50 m squared the progress
		// is a no-op (def+0x70 is uniformly 0 ⇒ case 0 always; retail measures against the dead object).
		if (!withinRetailKillDistance(killer.getX() - victim.getX(), killer.getY() - victim.getY(),
				killer.getZ() - victim.getZ())) {
			return false;
		}
		boolean handled = false;
		for (QuestState state : activeStates(killer)) {
			List<Integer> steps = pvpStepsByQuestId.get(state.getQuestId());
			if (steps == null) {
				continue;
			}
			int vars = state.getQuestVars().getQuestVars();
			if (!DataDrivenProgress.guardClear(vars)) {
				continue;
			}
			int stepIndex = DataDrivenProgress.step(vars);
			if (!steps.contains(stepIndex)) {
				continue;
			}
			StepPlan plan = plansByQuestId.get(state.getQuestId()).get(stepIndex);
			if (plan.gate() != null && !gatePasses(plan.gate(), killer, victim, victimRank)) {
				continue;
			}
			handled |= hitCounter(killer, state, stepIndex, 1);
		}
		return handled;
	}

	private static boolean gatePasses(PvpGate gate, Player killer, Player victim, AbyssRankEnum victimRank) {
		int rank = victimRank.getId();
		if (gate.minRank() > 0 && rank < gate.minRank()) {
			return false;
		}
		if (gate.maxRank() > 0 && rank > gate.maxRank()) {
			return false;
		}
		// 真端 `killerLevel <= victimLevel + levelGap`（`FUN_180c46980`：`uVar5 <= uVar1 + def+0x14`）。
		return killer.getLevel() <= victim.getLevel() + gate.levelGap();
	}

	private List<QuestState> activeStates(Player player) {
		if (player == null || player.getQuestStateList() == null) {
			return List.of();
		}
		List<QuestState> active = new ArrayList<>();
		for (QuestState state : player.getQuestStateList().getAllQuestState()) {
			if (state != null && state.getStatus() == QuestStatus.START
				&& routedQuestIds.contains(state.getQuestId())) {
				active.add(state);
			}
		}
		return active;
	}

	/**
	 * 注册兴趣面（击杀/对话/FOBJ 的 NPC 级注册，供 QuestEngine 的 NPC 索引派发）。
	 * Installs the NPC-keyed interests (kill / dialog / FOBJ) into the quest-engine registries.
	 */
	public void installInterest(QuestEngine engine) {
		if (engine == null || routedQuestIds.isEmpty()) {
			return;
		}
		for (Map.Entry<Integer, List<StepHit>> entry : killsByNpcId.entrySet()) {
			for (StepHit hit : entry.getValue()) {
				engine.registerQuestNpc(entry.getKey()).addOnKillEvent(hit.questId());
			}
		}
		for (Map.Entry<Integer, List<StepHit>> entry : talksByNpcId.entrySet()) {
			for (StepHit hit : entry.getValue()) {
				engine.registerQuestNpc(entry.getKey()).addOnTalkEvent(hit.questId());
			}
		}
		for (Map.Entry<Integer, List<StepHit>> entry : fobjsByNpcId.entrySet()) {
			for (StepHit hit : entry.getValue()) {
				engine.registerQuestNpc(entry.getKey()).addOnTalkEvent(hit.questId());
			}
		}
		// 接取 NPC 也要进对话注册（玩家无任务状态时客户端才能打开对话）。
		// Acquire NPCs join the dialog registry too (clients can only open the dialog of a
		// registered npc while the player has no quest state).
		// 同批把接取 NPC 注册进 onQuestStart：附近任务提示轴（SM_NEARBY_QUESTS）的候选集 =
		// WorldMapInstance 从 NPC 的 onQuestStart 并集枚举，缺该注册则 DD talk 接取任务永远
		// 进不了候选集（提示面只覆盖七族，§10.3-#18 的 DD 侧残余）。对话接取流不经该注册
		// （onDialog 直派），故零双重接取风险。
		// Acquire NPCs also join the onQuestStart registry: the nearby-quests hint axis
		// (SM_NEARBY_QUESTS) enumerates candidates from each NPC's onQuestStart union in
		// WorldMapInstance — without this registration DD talk-acquire quests can never enter
		// the candidate set (the hint face would cover the six families only; the DD-side
		// residual of §10.3-#18). The dialog acquire flow does not read this registry
		// (onDialog dispatches directly), so no double-acquire risk.
		for (Map.Entry<Integer, List<Integer>> entry : acquireTalksByNpcId.entrySet()) {
			for (int questId : entry.getValue()) {
				engine.registerQuestNpc(entry.getKey()).addOnTalkEvent(questId);
				engine.registerQuestNpc(entry.getKey()).addOnQuestStart(questId);
			}
		}
	}

	// ------------------------------------------------------------------ 执行面

	/**
	 * 共享对话平面（真端 `FUN_180c474b0`）：只服务「命中步 == 当前置步」的对话兴趣；
	 * 打开动作发阶段页 `select(K+1)`，顺序页动作（`10000+K`，K == 当前步 + 1）步进，乱序静默零写；
	 * `1009` = 步进 + 报告通道（末步转待领奖并发奖励窗页 5）；`10255` = 步进 + 完成页；
	 * `1008` 与其余 ≥1000 动作原样回发（不写状态）。
	 * The shared retail dialog plane: only the hit matching the quest's current step is served.
	 */
	private boolean dispatchDialog(Player player, List<StepHit> hits, int dialogId, int objectId,
			int requestedOwner) {
		if (player == null || hits == null || hits.isEmpty()) {
			return false;
		}
		for (StepHit hit : hits) {
			if (requestedOwner != 0 && requestedOwner != hit.questId()) {
				continue;
			}
			QuestState state = state(player, hit.questId());
			if (state == null) {
				continue;
			}
			int vars = state.getQuestVars().getQuestVars();
			if (!DataDrivenProgress.guardClear(vars) || DataDrivenProgress.step(vars) != hit.stepIndex()) {
				continue;
			}
			StepPlan plan = plansByQuestId.get(hit.questId()).get(hit.stepIndex());
			if (dialogId == 31 || dialogId == 26 || dialogId == -1) {
				sendStagePage(player, objectId, hit.questId(), DataDrivenProgress.step(vars));
				return true;
			}
			if (dialogId == ACTION_REPORT) {
				advance(player, state, hit.stepIndex(), plan.lastStep());
				sendPostAdvancePage(player, objectId, state, plan);
				return true;
			}
			if (dialogId == ACTION_ADVANCE_COMPLETE) {
				advance(player, state, hit.stepIndex(), plan.lastStep());
				PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(objectId, PAGE_COMPLETE, hit.questId()));
				return true;
			}
			if (dialogId == ACTION_COMPLETE) {
				PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(objectId, PAGE_COMPLETE, hit.questId()));
				return true;
			}
			if (dialogId >= 10000 && dialogId < 10000 + STAGE_PAGES.length) {
				int target = dialogId - 9999;
				// 真端顺序守卫：`code-9999 != 当前置 + 1 ⇒ return`（乱序/重复零写、不回发）。
				if (target != DataDrivenProgress.step(vars) + 1) {
					return false;
				}
				advance(player, state, hit.stepIndex(), plan.lastStep());
				sendPostAdvancePage(player, objectId, state, plan);
				return true;
			}
			if (dialogId >= 1000) {
				// 真端缺省分支：其余动作原样回发（`mgr+0x188`，如 1012/1013 翻页）。
				PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(objectId, dialogId, hit.questId()));
				return true;
			}
			return false;
		}
		return false;
	}

	/** 阶段页（真端 `select1..15` 页表；步 ≥15 不发页）。 / The retail stage page for one step. */
	private static void sendStagePage(Player player, int objectId, int questId, int stepIndex) {
		int page = stagePage(stepIndex);
		if (page > 0) {
			PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(objectId, page, questId));
		}
	}

	private static int stagePage(int stepIndex) {
		return stepIndex >= 0 && stepIndex < STAGE_PAGES.length ? STAGE_PAGES[stepIndex] : -1;
	}

	/** 阶段页表（门禁复算用）。 / The stage-page table (for gate recomputation). */
	static int stagePageForGate(int stepIndex) {
		return stagePage(stepIndex);
	}

	/** 步进后的页：末步 = 待领奖 + 奖励窗页 5，否则新当前步的阶段页。 / Page after an advance. */
	private void sendPostAdvancePage(Player player, int objectId, QuestState state, StepPlan plan) {
		if (state.getStatus() == QuestStatus.REWARD) {
			PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(objectId, PAGE_REWARD_WINDOW,
				state.getQuestId()));
		} else {
			sendStagePage(player, objectId, state.getQuestId(), DataDrivenProgress.step(
				state.getQuestVars().getQuestVars()));
		}
	}

	private boolean dispatch(Player player, List<StepHit> hits) {
		if (player == null || hits == null || hits.isEmpty()) {
			return false;
		}
		boolean handled = false;
		for (StepHit hit : hits) {
			QuestState state = state(player, hit.questId());
			if (state == null) {
				continue;
			}
			StepPlan plan = plansByQuestId.get(hit.questId()).get(hit.stepIndex());
			handled |= plan.counter() ? hitCounter(player, state, hit.stepIndex(), hit.group())
				: advance(player, state, hit.stepIndex(), plan.lastStep());
		}
		return handled;
	}

	private boolean hitCounter(Player player, QuestState state, int stepIndex, int group) {
		StepPlan plan = plansByQuestId.get(state.getQuestId()).get(stepIndex);
		int vars = state.getQuestVars().getQuestVars();
		DataDrivenProgress.Result result = DataDrivenProgress.hit(vars, stepIndex, plan.slots(), group,
			plan.lastStep());
		return apply(player, state, result, stepIndex);
	}

	private boolean advance(Player player, QuestState state, int stepIndex, boolean lastStep) {
		int vars = state.getQuestVars().getQuestVars();
		if (!DataDrivenProgress.guardClear(vars) || DataDrivenProgress.step(vars) != stepIndex) {
			return false;
		}
		DataDrivenProgress.Result result = new DataDrivenProgress.Result(
			lastStep ? DataDrivenProgress.Outcome.STEP_COMPLETE : DataDrivenProgress.Outcome.STEP_ADVANCE,
			DataDrivenProgress.advance(vars));
		return apply(player, state, result, stepIndex);
	}

	private boolean apply(Player player, QuestState state, DataDrivenProgress.Result result, int actionStepIndex) {
		if (result.outcome() == DataDrivenProgress.Outcome.NO_ACTION) {
			return false;
		}
		state.getQuestVars().setVar(result.newVars());
		if (result.outcome() == DataDrivenProgress.Outcome.STEP_COMPLETE) {
			state.setStatus(QuestStatus.REWARD);
		}
		state.setPersistentState(PersistentState.UPDATE_REQUIRED);
		PacketSendUtility.sendPacket(player,
			new SM_QUEST_ACTION(state.getQuestId(), state.getStatus(), state.getQuestVars().getQuestVars()));
		// 真端动作执行矩阵（步 f 逐 handler 修正）：推进/收口分支对 Hunt/EnterArea/TalkFOBJ **和 Talk**
		// 调执行器——对话平面推进/完成统一汇入 `FUN_180c4d5b0(…,-1)`，其 -1 路径在 C:2075066 直调
		// `FUN_180c4c8d0(完成步动作)`，门 = 完成步 kind==4（Talk）；CollectItem 拾取分支无执行器、
		// 门也只放行 kind 4 ⇒ 排除；ItemPlay/EnterWorld 推进边零执行器调用（e2 取证 §1）。
		// Retail executor matrix (step-f per-handler fix): the advance edge runs the executor for
		// Hunt/EnterArea/TalkFOBJ **and Talk** — the dialog plane funnels into FUN_180c4d5b0(…,-1)
		// whose -1 path calls FUN_180c4c8d0 on the completed step's actions, gated on step kind == 4.
		if (result.outcome() == DataDrivenProgress.Outcome.STEP_ADVANCE
			|| result.outcome() == DataDrivenProgress.Outcome.STEP_COMPLETE) {
			switch (plansByQuestId.get(state.getQuestId()).get(actionStepIndex).kind()) {
				case HUNT, ENTER_AREA, TALK_FOBJ, TALK -> runActions(player, state.getQuestId(), actionStepIndex);
				default -> {
				}
			}
		}
		return true;
	}

	private static QuestState state(Player player, int questId) {
		if (player == null || player.getQuestStateList() == null) {
			return null;
		}
		QuestState state = player.getQuestStateList().getQuestState(questId);
		if (state == null || state.getStatus() != QuestStatus.START) {
			return null;
		}
		return state;
	}

	// ------------------------------------------------------------------ 视图

	/** DD 表里且被路由集覆盖的行。 / Table rows covered by the routing set. */
	public Set<Integer> ownedQuestIds() {
		return ownedQuestIds;
	}

	/** 真正可原生路由的行（未冻结）。 / Rows that are natively routable (not frozen). */
	public Set<Integer> routedQuestIds() {
		return routedQuestIds;
	}

	/** 被冻结的行 → 原因。 / Frozen rows by reason. */
	public Map<Integer, FreezeReason> frozenQuestIds() {
		return frozenQuestIds;
	}

	/** 解析不到的名字（冻结行的证据面）。 / Names that failed to resolve. */
	public Set<String> unresolvedNames() {
		return unresolvedNames;
	}

	/** 该行当前步的原生计划（非路由行返回空）。 / The native plan of one step. */
	public Optional<StepPlan> plan(int questId, int stepIndex) {
		List<StepPlan> steps = plansByQuestId.get(questId);
		if (steps == null || stepIndex < 0 || stepIndex >= steps.size()) {
			return Optional.empty();
		}
		return Optional.of(steps.get(stepIndex));
	}

	/** 该行是否被本运行时接管。 / Whether the row is owned by this runtime. */
	public boolean owns(int questId) {
		return ownedQuestIds.contains(questId);
	}

	/** 该行是否可原生路由。 / Whether the row is natively routable. */
	public boolean routes(int questId) {
		return routedQuestIds.contains(questId);
	}

	/** 击杀兴趣面（npcId → 步引用），供门禁复算。 / Kill interests for gate recomputation. */
	public Map<Integer, List<StepHit>> killInterests() {
		return killsByNpcId;
	}

	/** 对话兴趣面（npcId → 步引用）。 / Talk interests. */
	public Map<Integer, List<StepHit>> talkInterests() {
		return talksByNpcId;
	}

	/** FOBJ 兴趣面（npcId → 步引用）。 / FOBJ interests. */
	public Map<Integer, List<StepHit>> fobjInterests() {
		return fobjsByNpcId;
	}

	/** 物品获得兴趣面（itemId → 步引用）。 / Item-acquire interests. */
	public Map<Integer, List<StepHit>> itemPlayInterests() {
		return itemPlaysByItemId;
	}

	/** 进区兴趣面（区名大写 → 步引用）。 / Enter-zone interests. */
	public Map<String, List<StepHit>> zoneInterests() {
		return zonesByName;
	}

	/** 进世界兴趣面（worldId → 步引用）。 / Enter-world interests. */
	public Map<Integer, List<StepHit>> worldInterests() {
		return worldsByWorldId;
	}

	/** PvP 行 → 步号。 / PvP rows by step index. */
	public Map<Integer, List<Integer>> pvpSteps() {
		return pvpStepsByQuestId;
	}

	/** 接取对话兴趣面（npcId → 任务）。 / Acquire talk interests (npc → quests). */
	public Map<Integer, List<Integer>> acquireTalkInterests() {
		return acquireTalksByNpcId;
	}

	/** 接取物品兴趣面（itemId → 任务）。 / Acquire item interests (item → quests). */
	public Map<Integer, List<Integer>> acquireItemInterests() {
		return acquireItemsByItemId;
	}

	/** 接取进世界兴趣面（worldId → 任务）。 / Acquire enter-world interests (world → quests). */
	public Map<Integer, List<Integer>> acquireWorldInterests() {
		return acquireWorldsByWorldId;
	}

	/** 接取等级兴趣面（level → 任务）。 / Acquire level interests (level → quests). */
	public Map<Integer, List<Integer>> acquireLevelInterests() {
		return acquireLevelsByLevel;
	}

	/** 接取进区兴趣面（区别名大写 → 任务）。 / Acquire zone interests (alias upper → quests). */
	public Map<String, List<Integer>> acquireZoneInterests() {
		return acquireZonesByName;
	}
}
