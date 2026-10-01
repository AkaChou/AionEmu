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

import com.aionemu.gameserver.model.gameobjects.PersistentState;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_QUEST_ACTION;
import com.aionemu.gameserver.questEngine.QuestEngine;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.questEngine.tablelane.DataDrivenQuestTable.Kind;
import com.aionemu.gameserver.questEngine.tablelane.DataDrivenQuestTable.Row;
import com.aionemu.gameserver.questEngine.tablelane.DataDrivenQuestTable.Step;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.stats.AbyssRankEnum;

/**
 * 真端 DataDriven 原生**进度运行时**（计划 §10.2「P7 DataDriven」步 2 步 d）。
 * <p>
 * 按真端「每步一条 handler 记录」驱动进度：兴趣面（击杀/对话/FOBJ/进区/进世界/PvP）按 DD 步载荷建索引，
 * 事件到达后走 {@link DataDrivenProgress} 的真端算术（Hunt/TalkFOBJ 多组计数；PvP 单组 + 军衔/等级闸门；
 * Talk/EnterArea/EnterWorld **直接步进**），末步转待领奖（真端 {@code SetQuestSuccess}）。
 * 真端逐 handler 原码：`FUN_180c46020`（Hunt，4 组 × 6 位、组内命中即 +1、全组达标才步进）、
 * `FUN_180c46980`（PvP，单组 + `killerLevel <= victimLevel + gap` 与军衔区间闸门）、
 * `FUN_180c466a0`（Talk，直接写目标步）、`FUN_180c47bf0`（EnterArea，名哈希同名区比对后直接步进）、
 * `FUN_180c467b0`（EnterWorld，步号校验后直接步进）、`FUN_180c478e0`（TalkFOBJ，组计数 0→1 二值）。
 * <p>
 * **本批不发运行期分流**（零行为变更）：DD 切换集行仍由旧 IR 车道 owns，生产 {@link #instance()} 的路由集为空
 * ——兴趣表不建、事件恒 false；字面切换（路由集 = switch set）随 P7 步 2 步 f 的原子切换批落地。
 * <p>
 * Native DataDriven progress runtime (plan §10.2 step 2d). Interests are indexed from the DD step payloads
 * (kill / dialog / FOBJ / enter-zone / enter-world / PvP) and every event runs the retail arithmetic of
 * {@link DataDrivenProgress}. The production singleton routes nothing yet (the switch set is still owned by
 * the legacy IR lane); the literal routing flip happens in the atomic switch batch (step 2f).
 */
public final class DataDrivenNativeRuntime {

	/** 真端 DD 表资源路径。 / The retail DD table resource. */
	public static final String TABLE_RESOURCE = "/aion/data/static_data/quest/retail/data_driven_quest.xml";

	/** 组标题后的尾整数（空格或逗号分隔）；名字内部的 `_N` 不算计数。 / Trailing count after a space or comma. */
	private static final Pattern TRAILING_INT = Pattern.compile("^(.*?)(?:\\s*,\\s*|\\s+)(\\d+)$");

	/** 一次事件命中的步引用：任务 + 步号 + 组槽（直接步进类为 0）。 / One event hit: quest, step, group. */
	public record StepHit(int questId, int stepIndex, int group) {
	}

	/** 不可原生路由的冻结原因。 / Why a switch-set row is not natively routable yet. */
	public enum FreezeReason {
		/** 名字解析不到唯一 NPC / 怪物。 / A payload name did not resolve. */
		NAME_UNRESOLVED,
		/** 进区别名在真端世界文件里没有定义（{@code NativeEnterAreaPort.RETAIL_ABSENT_ALIASES}）。 / Retail-absent zone alias. */
		ZONE_ABSENT,
		/** 进区别名未登记成区。 / Zone alias not registered. */
		ZONE_UNRESOLVED,
		/** 载荷语法不合法（空组 / 非整数世界 id / 非整数计数）。 / Malformed payload. */
		PAYLOAD_INVALID,
		/** 该步类别尚未接线（本批：CollectItem / ItemPlay）。 / Step kind not wired in this batch. */
		KIND_NOT_WIRED
	}

	/** 真端 PvP 闸门（`PvP Target Min Rank` / `Max Rank` / `Level Gap`；0 = 该轴无闸门）。 / PvP gate. */
	private record PvpGate(int minRank, int maxRank, int levelGap) {
	}

	/** 一步的原生执行计划。 / The native execution plan of one step. */
	private record StepPlan(Kind kind, boolean counter, List<DataDrivenProgress.Slot> slots, PvpGate gate,
			boolean lastStep) {
	}

	/** 一个载荷组：名字列表 + 尾整数（Hunt = 计数，TalkFOBJ = 动作类型）。 / One payload group. */
	private record PayloadGroup(List<String> names, int trailing) {
	}

	private static volatile DataDrivenNativeRuntime instance;

	private final Map<Integer, List<StepPlan>> plansByQuestId;
	private final Map<Integer, List<StepHit>> killsByNpcId;
	private final Map<Integer, List<StepHit>> talksByNpcId;
	private final Map<Integer, List<StepHit>> fobjsByNpcId;
	private final Map<String, List<StepHit>> zonesByName;
	private final Map<Integer, List<StepHit>> worldsByWorldId;
	private final Map<Integer, List<Integer>> pvpStepsByQuestId;
	private final Set<Integer> ownedQuestIds;
	private final Set<Integer> routedQuestIds;
	private final Map<Integer, FreezeReason> frozenQuestIds;
	private final Set<String> unresolvedNames;

	private DataDrivenNativeRuntime(Map<Integer, List<StepPlan>> plansByQuestId,
			Map<Integer, List<StepHit>> killsByNpcId, Map<Integer, List<StepHit>> talksByNpcId,
			Map<Integer, List<StepHit>> fobjsByNpcId, Map<String, List<StepHit>> zonesByName,
			Map<Integer, List<StepHit>> worldsByWorldId, Map<Integer, List<Integer>> pvpStepsByQuestId,
			Set<Integer> ownedQuestIds, Set<Integer> routedQuestIds, Map<Integer, FreezeReason> frozenQuestIds,
			Set<String> unresolvedNames) {
		this.plansByQuestId = plansByQuestId;
		this.killsByNpcId = killsByNpcId;
		this.talksByNpcId = talksByNpcId;
		this.fobjsByNpcId = fobjsByNpcId;
		this.zonesByName = zonesByName;
		this.worldsByWorldId = worldsByWorldId;
		this.pvpStepsByQuestId = pvpStepsByQuestId;
		this.ownedQuestIds = ownedQuestIds;
		this.routedQuestIds = routedQuestIds;
		this.frozenQuestIds = frozenQuestIds;
		this.unresolvedNames = unresolvedNames;
	}

	/**
	 * 生产单例：装载真端 DD 表，路由集为空（DD 切换集仍由旧 IR 车道 owns ⇒ 零行为变更）。
	 * Production singleton: loads the retail DD table with an empty routing set (zero behavior change).
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

	private static DataDrivenNativeRuntime loadProduction() {
		try (InputStream input = DataDrivenNativeRuntime.class.getResourceAsStream(TABLE_RESOURCE)) {
			if (input == null) {
				throw new IllegalStateException("DATA_DRIVEN_TABLE_MISSING: " + TABLE_RESOURCE);
			}
			DataDrivenQuestTable table = DataDrivenQuestTable.load(input);
			return create(table, Set.of(), null, null);
		} catch (IOException e) {
			throw new IllegalStateException("DATA_DRIVEN_TABLE_UNREADABLE: " + TABLE_RESOURCE, e);
		}
	}

	/**
	 * 按路由集建立原生运行时视图。
	 * Builds the native runtime view for one routing set.
	 *
	 * @param table          真端 DD 原生行模型 / the native DD row model
	 * @param routedQuestIds 本车道接管的 quest id（P7 步 f 的切换集）/ the quest ids this lane owns
	 * @param nameResolver   真端名字解析器（路由集非空时必需）/ retail name resolver (required when routing)
	 * @param enterAreaPort  真端同名区端口（路由集非空时必需）/ retail same-name zone port (required when routing)
	 */
	public static DataDrivenNativeRuntime create(DataDrivenQuestTable table, Set<Integer> routedQuestIds,
			NativeNpcNameResolver nameResolver, NativeEnterAreaPort enterAreaPort) {
		if (table == null) {
			throw new IllegalArgumentException("DATA_DRIVEN_TABLE_MISSING");
		}
		if (routedQuestIds == null || routedQuestIds.isEmpty()) {
			return new DataDrivenNativeRuntime(Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of(),
				Set.of(), Set.of(), Map.of(), Set.of());
		}
		Objects.requireNonNull(nameResolver, "DATA_DRIVEN_NAME_RESOLVER_MISSING");
		Objects.requireNonNull(enterAreaPort, "DATA_DRIVEN_ENTER_AREA_PORT_MISSING");

		Map<Integer, List<StepPlan>> plans = new LinkedHashMap<>();
		Map<Integer, List<StepHit>> kills = new LinkedHashMap<>();
		Map<Integer, List<StepHit>> talks = new LinkedHashMap<>();
		Map<Integer, List<StepHit>> fobjs = new LinkedHashMap<>();
		Map<String, List<StepHit>> zones = new LinkedHashMap<>();
		Map<Integer, List<StepHit>> worlds = new LinkedHashMap<>();
		Map<Integer, List<Integer>> pvpSteps = new LinkedHashMap<>();
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
			RowPlan rowPlan = planRow(questId, row, nameResolver, enterAreaPort);
			if (rowPlan.freezeReason() != null) {
				frozen.put(questId, rowPlan.freezeReason());
				rowPlan.unresolvedNames().forEach(name -> unresolved.add(name));
				continue;
			}
			routed.add(questId);
			plans.put(questId, rowPlan.steps());
			rowPlan.kills().forEach(hit -> kills.computeIfAbsent(hit.npcKey(), key -> new ArrayList<>()).add(hit.hit()));
			rowPlan.talks().forEach(hit -> talks.computeIfAbsent(hit.npcKey(), key -> new ArrayList<>()).add(hit.hit()));
			rowPlan.fobjs().forEach(hit -> fobjs.computeIfAbsent(hit.npcKey(), key -> new ArrayList<>()).add(hit.hit()));
			rowPlan.zones().forEach(hit -> zones.computeIfAbsent(hit.key(), key -> new ArrayList<>()).add(hit.hit()));
			rowPlan.worlds().forEach(hit -> worlds.computeIfAbsent(hit.npcKey(), key -> new ArrayList<>()).add(hit.hit()));
			if (!rowPlan.pvpSteps().isEmpty()) {
				pvpSteps.put(questId, List.copyOf(rowPlan.pvpSteps()));
			}
		}
		return new DataDrivenNativeRuntime(Map.copyOf(plans), Map.copyOf(kills), Map.copyOf(talks), Map.copyOf(fobjs),
			Map.copyOf(zones), Map.copyOf(worlds), Map.copyOf(pvpSteps), Set.copyOf(owned), Set.copyOf(routed),
			Map.copyOf(frozen), Set.copyOf(unresolved));
	}

	/** 一行计划的中转结构（构建期）。 / Mutable per-row plan during construction. */
	private record RowPlan(List<StepPlan> steps, List<NpcHit> kills, List<NpcHit> talks, List<NpcHit> fobjs,
			List<KeyHit> zones, List<NpcHit> worlds, List<Integer> pvpSteps, FreezeReason freezeReason,
			List<String> unresolvedNames) {
	}

	/** NPC 键命中（击杀/对话/FOBJ/世界按 int 键，进区按字符串键）。 / An interest hit bound to a key. */
	private record NpcHit(int npcKey, StepHit hit) {
	}

	private record KeyHit(String key, StepHit hit) {
	}

	private static RowPlan planRow(int questId, Row row, NativeNpcNameResolver nameResolver,
			NativeEnterAreaPort enterAreaPort) {
		List<StepPlan> steps = new ArrayList<>();
		List<NpcHit> kills = new ArrayList<>();
		List<NpcHit> talks = new ArrayList<>();
		List<NpcHit> fobjs = new ArrayList<>();
		List<KeyHit> zones = new ArrayList<>();
		List<NpcHit> worlds = new ArrayList<>();
		List<Integer> pvpSteps = new ArrayList<>();
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
				case TALK -> {
					String name = step.payload().trim();
					NativeNpcNameResolver.Match match = nameResolver.resolve(name);
					if (match.resolution() != NativeNpcNameResolver.Resolution.UNIQUE) {
						freeze = FreezeReason.NAME_UNRESOLVED;
						unresolved.add(name);
						break;
					}
					int npcId = match.npcIds().getFirst();
					talks.add(new NpcHit(npcId, new StepHit(questId, step.index(), 0)));
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
							NativeNpcNameResolver.Match match = nameResolver.resolve(name);
							if (match.resolution() != NativeNpcNameResolver.Resolution.UNIQUE) {
								freeze = FreezeReason.NAME_UNRESOLVED;
								unresolved.add(name);
								break;
							}
							npcIds.add(match.npcIds().getFirst());
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
				case COLLECT_ITEM, ITEM_PLAY -> {
					// 本批不接线：CollectItem 的宿主事件源（mgr+0x268+5*0x10 采集/交付事件）与
					// ItemPlay 的动作码驱动（>=10000 → SetQuestProgress(code-9999)）留给步 d2。
					freeze = FreezeReason.KIND_NOT_WIRED;
				}
			}
			if (freeze != null) {
				break;
			}
		}
		return new RowPlan(List.copyOf(steps), List.copyOf(kills), List.copyOf(talks), List.copyOf(fobjs),
			List.copyOf(zones), List.copyOf(worlds), List.copyOf(pvpSteps), freeze, List.copyOf(unresolved));
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
	 * 击杀事件（Hunt 组计数）。 / Kill event (Hunt group counters).
	 */
	public boolean onKill(Player player, int npcId) {
		return dispatch(player, killsByNpcId.get(npcId));
	}

	/**
	 * 对话事件（Talk 直接步进 + TalkFOBJ 组计数）。 / Dialog event (Talk advance + TalkFOBJ groups).
	 */
	public boolean onDialog(Player player, int npcId) {
		boolean talk = dispatch(player, talksByNpcId.get(npcId));
		boolean fobj = dispatch(player, fobjsByNpcId.get(npcId));
		return talk || fobj;
	}

	/**
	 * 进区事件（EnterArea 直接步进）。 / Enter-zone event (EnterArea advance).
	 */
	public boolean onEnterZone(Player player, String zoneName) {
		if (zoneName == null) {
			return false;
		}
		return dispatch(player, zonesByName.get(zoneName.toUpperCase(Locale.ROOT)));
	}

	/**
	 * 进世界事件（EnterWorld 直接步进）。 / Enter-world event (EnterWorld advance).
	 */
	public boolean onEnterWorld(Player player, int worldId) {
		return dispatch(player, worldsByWorldId.get(worldId));
	}

	/**
	 * PvP 击杀事件（单组计数 + 军衔/等级闸门）。 / Ranked kill event (single group + rank/level gate).
	 */
	public boolean onKillRanked(Player killer, Player victim, AbyssRankEnum victimRank) {
		if (killer == null || victim == null || victimRank == null || pvpStepsByQuestId.isEmpty()) {
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
	}

	// ------------------------------------------------------------------ 执行面

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
		return apply(player, state, result);
	}

	private boolean advance(Player player, QuestState state, int stepIndex, boolean lastStep) {
		int vars = state.getQuestVars().getQuestVars();
		if (!DataDrivenProgress.guardClear(vars) || DataDrivenProgress.step(vars) != stepIndex) {
			return false;
		}
		DataDrivenProgress.Result result = new DataDrivenProgress.Result(
			lastStep ? DataDrivenProgress.Outcome.STEP_COMPLETE : DataDrivenProgress.Outcome.STEP_ADVANCE,
			DataDrivenProgress.advance(vars));
		return apply(player, state, result);
	}

	private boolean apply(Player player, QuestState state, DataDrivenProgress.Result result) {
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
}
