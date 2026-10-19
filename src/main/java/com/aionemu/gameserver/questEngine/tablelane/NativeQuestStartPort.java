package com.aionemu.gameserver.questEngine.tablelane;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.gameserver.model.gameobjects.PersistentState;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.QuestStartRefusal;
import com.aionemu.gameserver.questEngine.retail.RetailQuestMetadataCompiler;
import com.aionemu.gameserver.network.aion.serverpackets.SM_QUEST_ACTION;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * 原版车道的**接取建档口**（计划 §6.2 的 native 状态端口）：不经过 typed 目录，也不依赖
 * {@code QuestService.startQuest}（后者要求 typed {@code QuestTemplate}，已切换行没有模板）。
 * <p>
 * 开始条件读原版 {@code quest.xml} 轴：等级（{@code minlevel_permitted}/{@code maxlevel_permitted}，
 * 原版 {@code Quest::CanAcquireQuest} 对 {@code level < minlevel} 一律拒绝，故 {@code 999} 是不可达值而非
 * "无限制"）、种族、职业、性别、重复上限（{@code max_repeat_count}）、已完成前置（{@code finished_quest_condN}，
 * 值形如 {@code Q50010}）。{@code bm_restrict_category} 按原版「类别 + 19」位查玩家账号限制位图
 * （{@code quest_acquire1..4}，见 {@link RestrictionBitmap}），本服无计费来源 ⇒ 位集为空。
 * <p>
 * Native start/state port: retail quest.xml axes only, no typed template; unproven axes fail closed.
 */
public final class NativeQuestStartPort {

	/** 接取结论。 / Start verdict. */
	public enum Outcome {
		STARTED,
		/** 已在进行/待领奖。 / Already in progress. */
		ALREADY_RUNNING,
		/** 原版行不可接取（等级轴不可达）。 / Row unreachable (level axis). */
		LEVEL_BLOCKED,
		RACE_BLOCKED,
		CLASS_BLOCKED,
		GENDER_BLOCKED,
		/** 重复次数用尽。 / Repeat budget exhausted. */
		REPEAT_LIMIT,
		/** 前置任务未完成。 / Prerequisite quests incomplete. */
		PREREQUISITE_MISSING,
		/** 账号限制位图命中该行的 {@code quest_acquireN} 位。 / Account restriction bitmap hit. */
		BM_RESTRICT_BLOCKED,
		/** 缺原版行。 / No retail row. */
		MISSING_ROW
	}

	/**
	 * 原版 opcode 127（{@code S_UPDATE_ZONE_QUEST} = AionEmu 的 {@code SM_NEARBY_QUESTS}）单行结论。
	 * <p>
	 * 原版 {@code Quest::CanAcquireQuest} 在区域任务清单形态（{@code param_5 = 0}：不短路、不提示）
	 * 下返回三值：{@code 2} = 全部轴通过；{@code 1} = **仅**等级轴不达且
	 * {@code minlevel_permitted <= level + 1}；{@code 0} = 其余一切失败（种族/职业/性别/头衔/军衔/
	 * 重复上限/前置/等级差 &gt; 1/超出等级上限）。原版调用方 {@code User::_UpdateQuestAcquireCondition}
	 * 把 {@code 2} 写成平条目、{@code 1} 写成 {@code questId | 0x20000} 软标记条目、{@code 0} 丢弃。
	 * <p>
	 * The retail zone-quest verdict (opcode 127): {@code 2} = every axis passes, {@code 1} = only the
	 * level axis is short and {@code minlevel_permitted <= level + 1}, {@code 0} = anything else.
	 */
	public enum ZoneVerdict {
		/** 原版 2：平条目。 / Retail 2: plain entry. */
		ACQUIRABLE,
		/** 原版 1：{@code questId | 0x20000} 软标记条目。 / Retail 1: the {@code questId | 0x20000} entry. */
		LEVEL_SOON,
		/** 原版 0：不入列表。 / Retail 0: not listed. */
		OMITTED
	}

	/**
	 * 账号限制位图端口（原版 {@code quest_acquireN} 等 68 个限制位的玩家侧位集）。
	 * <p>
	 * 原版语义（已坐实，见 {@code p3/p3-prereqs/bm-restrict-category-semantics.md}）：
	 * {@code bm_restrict_category} 是**类别下标**（服务端存 1 字节，&lt;0 → 0、&gt;8 → 8），
	 * 判定为「玩家限制位图 = {(类别 + 19)} 时拒绝接取」；下标 20..23 即
	 * {@code quest_acquire1..4}。本服无计费/账号类型子系统 ⇒ 生产端口为空位集
	 * （原版全订阅账号的 restrict 列表同样为空），因此类别 1 的行按原版**可接取**。
	 * <p>
	 * The account restriction bitmap: the retail check denies acquisition when bit
	 * {@code category + 19} is set on the player (indices 20..23 are {@code quest_acquire1..4}).
	 * This server has no billing/account-type source, so the production port is the empty set —
	 * the same shape a full-subscription retail account has.
	 */
	@FunctionalInterface
	public interface RestrictionBitmap {
		boolean has(Player player, int bitIndex);

		/** 无计费来源的生产位集（空）。 / The production bitmap: empty (no billing source). */
		RestrictionBitmap EMPTY = (player, bitIndex) -> false;
	}

	/** 原版限制位图里 {@code quest_acquire1} 的下标（类别 1 + 19）。 / Retail bit index of quest_acquire1. */
	static final int QUEST_ACQUIRE_FIRST_BIT = 20;

	/** 结论 + 证据串。 / Verdict with an evidence detail. */
	public record StartResult(Outcome outcome, String detail) {
		public boolean started() {
			return outcome == Outcome.STARTED;
		}
	}

	/**
	 * 判定结果加上命中的等级轴细节：{@code LEVEL_BLOCKED} 同时覆盖「不足」与「超出」，拒绝提示
	 * 需要区分两者。非等级条件下 {@code minLevel} 为 0、{@code levelTooLow} 为 false。
	 * <p>
	 * The verdict plus the details of a {@code LEVEL_BLOCKED} hit: that outcome covers both directions,
	 * while the refusal message has to tell them apart.
	 */
	public record AcquireVerdict(StartResult result, int minLevel, boolean levelTooLow) {
		public boolean started() {
			return result.started();
		}

		static AcquireVerdict of(StartResult result) {
			return new AcquireVerdict(Objects.requireNonNull(result, "result"), 0, false);
		}

		static AcquireVerdict of(Outcome outcome, String detail) {
			return of(new StartResult(Objects.requireNonNull(outcome, "outcome"),
				Objects.requireNonNull(detail, "detail")));
		}

		static AcquireVerdict of(Outcome outcome, String detail, int minLevel, boolean levelTooLow) {
			return new AcquireVerdict(new StartResult(Objects.requireNonNull(outcome, "outcome"),
				Objects.requireNonNull(detail, "detail")), minLevel, levelTooLow);
		}

		static AcquireVerdict missingRow(String detail) {
			return of(Outcome.MISSING_ROW, detail);
		}
	}

	/** 任务追踪日志出口，路由到 logback 的 quest logger。 / Quest trace sink for the quest logger. */
	private static final Logger QUEST_TRACE_LOG = LoggerFactory.getLogger("quest");

	private static volatile NativeQuestStartPort instance;

	private final NativeQuestXmlTable questXml;
	private final RestrictionBitmap restrictions;

	public static NativeQuestStartPort instance() {
		NativeQuestStartPort local = instance;
		if (local == null) {
			synchronized (NativeQuestStartPort.class) {
				local = instance;
				if (local == null) {
					local = new NativeQuestStartPort(NativeQuestXmlTable.instance(), RestrictionBitmap.EMPTY);
					instance = local;
				}
			}
		}
		return local;
	}

	public NativeQuestStartPort(NativeQuestXmlTable questXml) {
		this(questXml, RestrictionBitmap.EMPTY);
	}

	public NativeQuestStartPort(NativeQuestXmlTable questXml, RestrictionBitmap restrictions) {
		this.questXml = questXml;
		this.restrictions = restrictions;
	}

	/**
	 * 替换进程级单例（仅供测试注入合成表；生产由 {@link #instance()} 懒加载）。
	 * Replaces the process-wide instance (test-only injection of a synthetic table; production lazily
	 * loads through {@link #instance()}).
	 */
	public static void overrideInstanceForTest(NativeQuestStartPort port) {
		instance = port;
	}

	/**
	 * NPC 接取（原版 {@code Quest::CanAcquireQuest} 等价判定）→ 建档/复位到 START。
	 * NPC acquisition: the {@code CanAcquireQuest}-equivalent adjudication, then create/reset the row.
	 */
	public StartResult start(Player player, int questId) {
		StartResult verdict = evaluateNpcAcquire(player, questId);
		return verdict.started() ? commit(player, questId) : verdict;
	}

	/**
	 * NPC 接取并打拒绝追踪（五个表车道接取面共用）：结论与 {@link #start(Player, int)} 完全一致，
	 * 仅在拒绝时向 {@code quest} logger 输出结论轴 detail 并给玩家下发原版拒绝提示，消除「点了接受
	 * 零反馈零日志」的静默面。提示只覆盖玩家可补救且原版有对应字符串的轴（见
	 * {@link com.aionemu.gameserver.questEngine.QuestStartRefusal}），其余轴保持原版零消息。
	 * <p>
	 * Acquire with a refusal trace shared by the five table-lane accept faces: the verdict is
	 * identical to {@link #start(Player, int)}; refusals are traced to the {@code quest} logger and
	 * answered with the retail refusal message for the axis (only axes the player can remedy and
	 * retail has a string for; the rest stay silent exactly like retail).
	 */
	public StartResult startTraced(Player player, int questId, int dialogId) {
		AcquireVerdict verdict = evaluateAcquire(player, questId);
		if (!verdict.started()) {
			StartResult result = verdict.result();
			QUEST_TRACE_LOG.info(I18n.get("log.quest_trace.acquire_refused",
				player == null ? "unknown" : player.getName(), questId, dialogId,
				result.outcome() + ": " + result.detail()));
			QuestStartRefusal.notifyIfRefused(player, questId, verdict);
			return result;
		}
		return commit(player, questId);
	}

	/**
	 * 系统发放（阵营轮换等）：资格由调用方按同一 quest.xml 轴判定，这里只做状态面（进行中/重复上限）+ 建档。
	 * System grants: eligibility is adjudicated by the caller on the same axes; this only writes state.
	 */
	public StartResult grant(Player player, int questId) {
		if (player == null || player.getQuestStateList() == null) {
			return new StartResult(Outcome.MISSING_ROW, "player unavailable");
		}
		NativeQuestXmlTable.QuestRow row = questXml.find(questId).orElse(null);
		if (row == null) {
			return new StartResult(Outcome.MISSING_ROW, "no retail quest.xml row");
		}
		QuestState state = player.getQuestStateList().getQuestState(questId);
		if (state != null && (state.getStatus() == QuestStatus.START || state.getStatus() == QuestStatus.REWARD)) {
			return new StartResult(Outcome.ALREADY_RUNNING, state.getStatus().name());
		}
		StartResult repeat = repeatVerdict(state, row);
		return repeat.started() ? commit(player, questId) : repeat;
	}

	/** 原版 {@code CanAcquireQuest} 等价判定（不发状态、不改存档）。 / Retail acquisition adjudication, no writes. */
	public StartResult evaluateNpcAcquire(Player player, int questId) {
		return evaluateAcquire(player, questId).result();
	}

	/**
	 * 与 {@link #evaluateNpcAcquire(Player, int)} 同一判定，额外返回命中的拒绝轴（供拒绝提示映射）。
	 * The same adjudication as {@link #evaluateNpcAcquire(Player, int)}, also reporting the hit axis so
	 * refusals can be mapped to their retail message.
	 */
	public AcquireVerdict evaluateAcquire(Player player, int questId) {
		if (player == null || player.getQuestStateList() == null) {
			return AcquireVerdict.missingRow(player == null ? "player unavailable" : "quest state unavailable");
		}
		NativeQuestXmlTable.QuestRow row = questXml.find(questId).orElse(null);
		if (row == null) {
			return AcquireVerdict.missingRow("no retail quest.xml row");
		}
		QuestState state = player.getQuestStateList().getQuestState(questId);
		if (state != null && (state.getStatus() == QuestStatus.START || state.getStatus() == QuestStatus.REWARD)) {
			return AcquireVerdict.of(Outcome.ALREADY_RUNNING, state.getStatus().name());
		}
		StartResult levelVerdict = levelVerdict(player, row);
		if (!levelVerdict.started()) {
			int minLevel = intOr(row, "minlevel_permitted", 0);
			return AcquireVerdict.of(Outcome.LEVEL_BLOCKED, levelVerdict.detail(), minLevel,
				player.getLevel() < minLevel);
		}
		StartResult eligibility = eligibilityVerdict(player, row);
		if (!eligibility.started()) {
			return AcquireVerdict.of(eligibility.outcome(), eligibility.detail());
		}
		return AcquireVerdict.of(repeatVerdict(state, row));
	}

	/**
	 * 状态提交后的可见性刷新（原版 {@code sync-quest-state mode=LEVEL_AND_VISIBILITY_REFRESH} 的
	 * {@code updateZone + updateNearbyQuests} 轴）：NPC 头顶任务标记随状态/步数变化重算。接取建档、
	 * 网格族击杀推进、tablelane 各交付面翻 REWARD 与 DataDriven 收口/终步共用
	 * （{@code DataDrivenNativeRuntime.refreshLevelAndVisibility} 先前是它的私有同名副本）。
	 * The visibility refresh after a committed state change (the retail LEVEL_AND_VISIBILITY_REFRESH
	 * axis) so NPC overhead quest markers follow status/step changes. Shared by the accept path, the
	 * kill-advance lanes, every tablelane hand-in that flips REWARD and the DataDriven closure and
	 * terminal step. Best effort in headless environments.
	 * <p>调用合同：**在状态包下发之后调用**（对齐 GM 路径 Quest.java：状态包 → updateZone +
	 * updateNearbyQuests）——客户端按已收到的任务状态重算头顶标记，先刷新后发包会拿旧状态重算；
	 * 顺序合同实机 2026-10-13 确认（1102 击杀 / 1103 报告确认：均不重登即出标记；0x7f 清单本身不含
	 * 进行中/REWARD 行，只作重算触发器）。 / Call contract: invoke it AFTER the state packet,
	 * mirroring the GM path; the order was confirmed live on 2026-10-13 (the 0x7f list itself never
	 * carries in-progress/REWARD rows — it only triggers the recompute).
	 * <p>等级任务重评估轴（{@code onQuestStateChanged}）在 tablelane 无先例，未镜像（沿用
	 * DataDriven 私有副本的登记口径）。 / The level-quest reevaluation axis is not mirrored here
	 * (the registered scope of the former DataDriven private copy).
	 */
	public static void refreshVisibility(Player player) {
		if (player == null) {
			return;
		}
		try {
			if (player.getController() != null) {
				player.getController().updateZone();
				player.getController().updateNearbyQuests();
			}
		} catch (RuntimeException ignored) {
			// 单测/无控制器环境：状态已提交，刷新为尽力而为。
			// Headless environments: the state is committed; the refresh is best effort.
		}
	}

	/**
	 * 区域任务清单（原版 opcode 127 / {@code SM_NEARBY_QUESTS}）单行结论。
	 * <p>
	 * 轴与 {@link #evaluateNpcAcquire(Player, int)} 同源，差别只有原版在清单形态下**不短路**：等级轴
	 * 失败时其余轴仍全部判定，因此软结论 {@code 1} 只在「其余轴全通过 + 恰好只差 1 级」出现；任一非
	 * 等级轴失败（含超出等级上限、重复上限用尽、进行中）都是硬 {@code 0}。
	 * <p>
	 * The zone-quest verdict: the same retail axes as {@link #evaluateNpcAcquire(Player, int)}, but
	 * the retail list form never short-circuits, so the soft verdict {@code 1} needs every other axis
	 * to pass. Any non-level failure (over {@code maxlevel}, repeat budget spent, in progress) is a
	 * hard {@code 0}.
	 */
	public ZoneVerdict zoneVerdict(Player player, int questId) {
		if (player == null || player.getQuestStateList() == null) {
			return ZoneVerdict.OMITTED;
		}
		NativeQuestXmlTable.QuestRow row = questXml.find(questId).orElse(null);
		if (row == null) {
			return ZoneVerdict.OMITTED;
		}
		QuestState state = player.getQuestStateList().getQuestState(questId);
		if (state != null && (state.getStatus() == QuestStatus.START || state.getStatus() == QuestStatus.REWARD)) {
			// 原版调用方要求 {@code UserQuestData_GetQuestState(id)[0] == 0}（无进行中记录）才入清单。
			// The retail caller requires the player's quest-state byte to be zero (nothing in progress).
			return ZoneVerdict.OMITTED;
		}
		if (!eligibilityVerdict(player, row).started() || !repeatVerdict(state, row).started()) {
			return ZoneVerdict.OMITTED;
		}
		int minLevel = intOr(row, "minlevel_permitted", 0);
		int maxLevel = intOr(row, "maxlevel_permitted", 0);
		int level = player.getLevel();
		if (maxLevel != 0 && level > maxLevel) {
			return ZoneVerdict.OMITTED;
		}
		if (minLevel != 0 && level < minLevel) {
			return minLevel - level == 1 ? ZoneVerdict.LEVEL_SOON : ZoneVerdict.OMITTED;
		}
		return ZoneVerdict.ACQUIRABLE;
	}

	/** 等级轴（原版 {@code minlevel_permitted}/{@code maxlevel_permitted}）。 / The level axis. */
	private StartResult levelVerdict(Player player, NativeQuestXmlTable.QuestRow row) {
		int minLevel = intOr(row, "minlevel_permitted", 0);
		int maxLevel = intOr(row, "maxlevel_permitted", 0);
		int level = player.getLevel();
		if (minLevel != 0 && level < minLevel) {
			return new StartResult(Outcome.LEVEL_BLOCKED, "level " + level + " < minlevel " + minLevel);
		}
		if (maxLevel != 0 && level > maxLevel) {
			return new StartResult(Outcome.LEVEL_BLOCKED, "level " + level + " > maxlevel " + maxLevel);
		}
		return new StartResult(Outcome.STARTED, "level " + level);
	}

	/** 等级以外的接取资格轴：种族/职业/性别/账号限制位/已完成前置。 / Every non-level eligibility axis. */
	private StartResult eligibilityVerdict(Player player, NativeQuestXmlTable.QuestRow row) {
		if (!racePermitted(row.text("race_permitted"), player.getRace() == null ? null : player.getRace().name())) {
			return new StartResult(Outcome.RACE_BLOCKED, "race " + player.getRace());
		}
		String playerClass = player.getCommonData() == null || player.getCommonData().getPlayerClass() == null
				? null
				: player.getCommonData().getPlayerClass().name().toLowerCase(Locale.ROOT);
		// 职业词表与生产元数据同源（原版 token → PlayerClass，基础职业 ≥10 展开进阶线）；
		// 逐字比较 token 会让 fighter/knight/wizard 这类原版名永远不匹配。
		// Class tokens share the production metadata mapping; a literal token comparison would never
		// match retail names such as fighter/knight/wizard.
		java.util.Set<String> permittedClasses = RetailQuestMetadataCompiler.permittedClassNames(
				row.text("class_permitted"), intOr(row, "minlevel_permitted", 0));
		if (!permittedClasses.isEmpty()
				&& (playerClass == null || !permittedClasses.contains(playerClass.toUpperCase(Locale.ROOT)))) {
			return new StartResult(Outcome.CLASS_BLOCKED, "class " + playerClass);
		}
		String gender = player.getGender() == null ? null : player.getGender().name().toLowerCase(Locale.ROOT);
		if (!tokenPermitted(row.text("gender_permitted"), gender)) {
			return new StartResult(Outcome.GENDER_BLOCKED, "gender " + gender);
		}
		// bm_restrict_category：原版按「类别 + 19」位查玩家限制位图（NPCServer Quest::CanAcquireQuest，
		// 见 p3-prereqs/bm-restrict-category-semantics.md）；类别仅 1..4 落在 quest_acquire1..4。
		// bm_restrict_category is the account-restriction category: the retail check denies when the
		// player's bitmap has bit (category + 19); only 1..4 land on quest_acquire1..4.
		int restrictCategory = restrictCategory(row);
		if (restrictCategory != 0 && restrictions.has(player, restrictCategory + 19)) {
			return new StartResult(Outcome.BM_RESTRICT_BLOCKED,
					"quest_acquire" + restrictCategory + " set");
		}
		List<Integer> missing = missingPrerequisites(player, row);
		if (!missing.isEmpty()) {
			return new StartResult(Outcome.PREREQUISITE_MISSING, "unfinished prerequisites " + missing);
		}
		return new StartResult(Outcome.STARTED, "eligibility");
	}

	/** 重复上限（原版 {@code finishedcount < max_repeat_count}）与时间轴。 / Repeat budget and time axis. */
	private StartResult repeatVerdict(QuestState state, NativeQuestXmlTable.QuestRow row) {
		if (state == null || state.getStatus() != QuestStatus.COMPLETE) {
			return new StartResult(Outcome.STARTED, "fresh state");
		}
		int maxRepeat = intOr(row, "max_repeat_count", 0);
		int completed = state.getCompleteCount();
		if (maxRepeat <= 0 || completed >= maxRepeat) {
			return new StartResult(Outcome.REPEAT_LIMIT,
					"completed " + completed + " / max_repeat_count " + maxRepeat);
		}
		if (!repeatTimeElapsed(state, row)) {
			return new StartResult(Outcome.REPEAT_LIMIT, "next repeat time " + state.getNextRepeatTime());
		}
		return new StartResult(Outcome.STARTED, "repeatable " + completed + "/" + maxRepeat);
	}

	/**
	 * COMPLETE 态重复窗：完成次数未达 {@code max_repeat_count}，且时间轴的 {@code nextRepeatTime} 已到。
	 * 各族 handler 的接取窗与 {@link #repeatVerdict} 共用本判定（单一事实来源，避免第二套重复口径）。
	 * <p>
	 * The repeat window at COMPLETE: the completion count is under {@code max_repeat_count} and the
	 * time axis has elapsed. Shared by the family accept windows and {@link #repeatVerdict}.
	 * @param state 玩家该行的状态（可为 null）/ the player's row state, nullable
	 * @param questId 任务 id / the quest id
	 * @return 是否可再次接取 / whether the row re-opens
	 */
	public static boolean repeatWindowOpen(QuestState state, int questId) {
		if (state == null) {
			return false;
		}
		NativeQuestXmlTable.QuestRow row = NativeQuestXmlTable.instance().find(questId).orElse(null);
		if (row == null) {
			return false;
		}
		int maxRepeat = intOr(row, "max_repeat_count", 0);
		return maxRepeat > 1 && state.getCompleteCount() < maxRepeat && repeatTimeElapsed(state, row);
	}

	/**
	 * 时间轴判定：行声明了重复周期（{@code quest_repeat_cycle}）或冷却（{@code quest_cooltime}）时，
	 * {@code nextRepeatTime} 未到即不开窗；未声明时间轴的行（含 2717 行仅靠次数的原版行）不受该项约束——
	 * 否则位置/次数驱动的重复会被 {@code nextRepeatTime} 的陈旧值误锁。
	 * <p>
	 * The time axis: a row declaring a repeat cycle or a cooldown stays closed until
	 * {@code nextRepeatTime}; rows without a time axis are never constrained by it, so count-driven
	 * repeats are not locked out by a stale value.
	 */
	static boolean repeatTimeElapsed(QuestState state, NativeQuestXmlTable.QuestRow row) {
		var nextRepeatTime = state.getNextRepeatTime();
		if (nextRepeatTime == null || !repeatTimeGated(row)) {
			return true;
		}
		return nextRepeatTime.getTime() <= System.currentTimeMillis();
	}

	/** 行声明时间轴（{@code quest_repeat_cycle} 或 {@code quest_cooltime}）。 / The row declares a time axis. */
	private static boolean repeatTimeGated(NativeQuestXmlTable.QuestRow row) {
		return intOr(row, "quest_cooltime", 0) > 0 || !row.numbered("quest_repeat_cycle").isEmpty()
				|| !row.text("quest_repeat_cycle").isBlank();
	}

	/** {@code finished_quest_cond1..7}（值形如 {@code Q50010}）→ 必须已 COMPLETE。 */
	private List<Integer> missingPrerequisites(Player player, NativeQuestXmlTable.QuestRow row) {
		List<Integer> missing = new ArrayList<>();
		for (String tag : List.of("finished_quest_cond1", "finished_quest_cond2", "finished_quest_cond3",
				"finished_quest_cond4", "finished_quest_cond5", "finished_quest_cond6", "finished_quest_cond7")) {
			for (String value : row.list(tag)) {
				for (String token : value.trim().split("[\\s,]+")) {
					if (token.isBlank()) {
						continue;
					}
					int questId = prerequisiteId(token);
					if (questId <= 0) {
						missing.add(questId);
						continue;
					}
					QuestState prerequisite = player.getQuestStateList().getQuestState(questId);
					// 带 {@code :n} 后缀（原版奖励分支）的行还要比对该前置的奖励档；
					// 档位解析与生产元数据同源。未完成的存档尚无奖励档（读取会打误导性警告），
					// 先判状态再取档位。 / A {@code :n} suffix also pins the prerequisite's reward
					// slot, parsed through the shared production rule. An unfinished save has no
					// reward slot yet (reading it logs a misleading warning), so status gates the read.
					boolean rewardMatches = token.indexOf(':') < 0
							|| (prerequisite != null && prerequisite.getStatus() == QuestStatus.COMPLETE
								&& prerequisite.getReward()
									== RetailQuestMetadataCompiler.prerequisiteRewardMode(questId, token));
					if (prerequisite == null || prerequisite.getStatus() != QuestStatus.COMPLETE || !rewardMatches) {
						missing.add(questId);
					}
				}
			}
		}
		return missing;
	}

	/**
	 * {@code unfinished_quest_cond1..N}：被引用行必须**未** COMPLETE 才算通过（原版该列的"未进行"语义，
	 * 与 typed {@code UnfinishedQuest} 同轴：{@code state == null || status != COMPLETE}）。
	 * <p>
	 * 只服务 DD 链式接取面（{@code DataDrivenNativeRuntime} 对 {@code acquire=none} 后继行的发放行走）；
	 * {@link #start(Player, int)} 的判定面刻意不含该轴——其它 native 接取面（Talk/ItemPlay/EnterWorld/
	 * EnterArea/LevelUp 与七族）口径不变，避免扩大影响面。缺原版行或条件名解析失败一律 fail-closed。
	 * <p>
	 * The unfinished-quest axis: every referenced row must NOT be COMPLETE. Scoped to the DD
	 * chain-acquire face only — {@code start()}'s verdicts deliberately stay unchanged for every other
	 * native acquire face. A missing retail row or an unresolvable condition token fails closed.
	 */
	public boolean unfinishedConditionsPass(Player player, int questId) {
		if (player == null || player.getQuestStateList() == null || questId <= 0) {
			return false;
		}
		NativeQuestXmlTable.QuestRow row = questXml.find(questId).orElse(null);
		if (row == null) {
			return false;
		}
		for (String value : row.numbered("unfinished_quest_cond")) {
			for (String token : value.trim().split("[\\s,]+")) {
				if (token.isBlank()) {
					continue;
				}
				int conditionId = prerequisiteId(token);
				if (conditionId <= 0) {
					return false;
				}
				QuestState state = player.getQuestStateList().getQuestState(conditionId);
				if (state != null && state.getStatus() == QuestStatus.COMPLETE) {
					return false;
				}
			}
		}
		return true;
	}

	/**
	 * {@code finished_quest_condN} 取值 → 前置任务 ID。
	 * <p>
	 * 原版该列写的是**目标行的 {@code <name>}**，并可选带 {@code :n} 奖励分支后缀：绝大多数为
	 * {@code Q<id>}，574 行（CombineTask 全族）为符号（{@code ws_q5015} = 行 5015），因此除
	 * {@code Q<digits>} 之外一律回落到 {@link NativeQuestXmlTable#findByName(String)}；两者都
	 * 解不出才算表数据破损（fail-loud）。
	 * <p>
	 * The column holds the referenced row's retail {@code <name>} (plus an optional {@code :n} reward
	 * branch): {@code Q<id>} for most rows and a symbol such as {@code ws_q5015} for the 574
	 * CombineTask rows, so anything that is not {@code Q<digits>} falls back to the name index.
	 */
	private int prerequisiteId(String value) {
		if (value == null || value.isBlank()) {
			return 0;
		}
		String digits = value.trim().toUpperCase(Locale.ROOT);
		int colon = digits.indexOf(':');
		if (colon >= 0) {
			digits = digits.substring(0, colon);
		}
		if (digits.startsWith("Q")) {
			digits = digits.substring(1);
		}
		try {
			return Integer.parseInt(digits);
		} catch (NumberFormatException e) {
			String name = value.trim();
			int nameColon = name.indexOf(':');
			if (nameColon >= 0) {
				name = name.substring(0, nameColon);
			}
			return questXml.findByName(name).map(NativeQuestXmlTable.QuestRow::questId)
				.orElseThrow(() -> new IllegalStateException(
					"NATIVE_TABLE_PARSE_FAILED: unresolvable finished_quest_cond '" + value + "'"));
		}
	}

	/** 建档/复位到 START + 同步客户端。 / Create or reset the row to START and sync the client. */
	private StartResult commit(Player player, int questId) {
		QuestState state = player.getQuestStateList().getQuestState(questId);
		if (state == null) {
			player.getQuestStateList().addQuest(questId, new QuestState(questId, QuestStatus.START, 0, 0, null, 0, null));
		} else {
			state.setStatus(QuestStatus.START);
			state.getQuestVars().setVar(0);
			state.setPersistentState(PersistentState.UPDATE_REQUIRED);
		}
		PacketSendUtility.sendPacket(player, new SM_QUEST_ACTION(questId, QuestStatus.START.value(), 0));
		refreshVisibility(player);
		return new StartResult(Outcome.STARTED, "START");
	}

	/** 该表内原始行的整数字段（供拒绝提示读取显示名/上限等展示轴）。 / A raw integer field of this table's row. */
	public int displayInteger(int questId, String tag, int fallback) {
		NativeQuestXmlTable.QuestRow row = questXml.find(questId).orElse(null);
		return row == null ? fallback : intOr(row, tag, fallback);
	}

	/** 该表内原始行的首个文本字段（供拒绝提示读取任务名）。 / The first text field of this table's row. */
	public String displayText(int questId, String tag) {
		return questXml.find(questId).map(row -> row.text(tag)).orElse("");
	}

	private static int intOr(NativeQuestXmlTable.QuestRow row, String tag, int fallback) {
		Integer value = row.integer(tag);
		return value == null ? fallback : value;
	}

	/**
	 * {@code bm_restrict_category} → 类别下标（与原版同形：缺列/负值 → 0、&gt;8 → 8）。
	 * The retail restriction category, clamped exactly like the retail loaders.
	 */
	public static int restrictCategory(NativeQuestXmlTable.QuestRow row) {
		Integer raw = row.integer("bm_restrict_category");
		if (raw == null || raw < 0) {
			return 0;
		}
		return Math.min(raw, 8);
	}

	/** {@code class_permitted}/{@code gender_permitted} 空格分隔词表；缺声明或 {@code all} 不限制。 */
	static boolean tokenPermitted(String field, String token) {
		if (field == null || field.isBlank() || "all".equalsIgnoreCase(field.trim())) {
			return true;
		}
		if (token == null) {
			return false;
		}
		for (String candidate : field.trim().toLowerCase(Locale.ROOT).split("[\\s,]+")) {
			if (candidate.equals(token)) {
				return true;
			}
		}
		return false;
	}

	/** {@code race_permitted} → 种族判定（pc_light→ELYOS、pc_dark→ASMODIANS、pc_all/双族→任意）。 */
	static boolean racePermitted(String field, String raceName) {
		if (field == null || field.isBlank()) {
			return true;
		}
		boolean light = false;
		boolean dark = false;
		for (String token : field.trim().toLowerCase(Locale.ROOT).split("[\\s,]+")) {
			if ("pc_all".equals(token)) {
				return true;
			}
			light |= "pc_light".equals(token);
			dark |= "pc_dark".equals(token);
		}
		if (light && dark) {
			return true;
		}
		if (raceName == null) {
			return false;
		}
		return light ? "ELYOS".equals(raceName) : dark && "ASMODIANS".equals(raceName);
	}
}
