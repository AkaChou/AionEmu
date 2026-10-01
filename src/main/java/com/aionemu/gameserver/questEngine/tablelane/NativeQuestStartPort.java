package com.aionemu.gameserver.questEngine.tablelane;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.aionemu.gameserver.model.gameobjects.PersistentState;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.retail.RetailQuestMetadataCompiler;
import com.aionemu.gameserver.network.aion.serverpackets.SM_QUEST_ACTION;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * 真端车道的**接取建档口**（计划 §6.2 的 native 状态端口）：不经过 typed 目录，也不依赖
 * {@code QuestService.startQuest}（后者要求 typed {@code QuestTemplate}，已切换行没有模板）。
 * <p>
 * 开始条件读真端 {@code quest.xml} 轴：等级（{@code minlevel_permitted}/{@code maxlevel_permitted}，
 * 真端 {@code Quest::CanAcquireQuest} 对 {@code level < minlevel} 一律拒绝，故 {@code 999} 是不可达值而非
 * "无限制"）、种族、职业、性别、重复上限（{@code max_repeat_count}）、已完成前置（{@code finished_quest_condN}，
 * 值形如 {@code Q50010}）。{@code bm_restrict_category} 的 128 位地图位集语义尚未坐实 ⇒ **fail-closed**
 * （{@code BM_RESTRICT_UNRESOLVED}），不得用放行兜底。
 * <p>
 * Native start/state port: retail quest.xml axes only, no typed template; unproven axes fail closed.
 */
public final class NativeQuestStartPort {

	/** 接取结论。 / Start verdict. */
	public enum Outcome {
		STARTED,
		/** 已在进行/待领奖。 / Already in progress. */
		ALREADY_RUNNING,
		/** 真端行不可接取（等级轴不可达）。 / Row unreachable (level axis). */
		LEVEL_BLOCKED,
		RACE_BLOCKED,
		CLASS_BLOCKED,
		GENDER_BLOCKED,
		/** 重复次数用尽。 / Repeat budget exhausted. */
		REPEAT_LIMIT,
		/** 前置任务未完成。 / Prerequisite quests incomplete. */
		PREREQUISITE_MISSING,
		/** {@code bm_restrict_category} 未坐实 ⇒ fail-closed。 / Unproven BM restriction axis. */
		BM_RESTRICT_UNRESOLVED,
		/** 缺真端行。 / No retail row. */
		MISSING_ROW
	}

	/** 结论 + 证据串。 / Verdict with an evidence detail. */
	public record StartResult(Outcome outcome, String detail) {
		public boolean started() {
			return outcome == Outcome.STARTED;
		}
	}

	private static volatile NativeQuestStartPort instance;

	private final NativeQuestXmlTable questXml;

	public static NativeQuestStartPort instance() {
		NativeQuestStartPort local = instance;
		if (local == null) {
			synchronized (NativeQuestStartPort.class) {
				local = instance;
				if (local == null) {
					local = new NativeQuestStartPort(NativeQuestXmlTable.instance());
					instance = local;
				}
			}
		}
		return local;
	}

	public NativeQuestStartPort(NativeQuestXmlTable questXml) {
		this.questXml = questXml;
	}

	/**
	 * NPC 接取（真端 {@code Quest::CanAcquireQuest} 等价判定）→ 建档/复位到 START。
	 * NPC acquisition: the {@code CanAcquireQuest}-equivalent adjudication, then create/reset the row.
	 */
	public StartResult start(Player player, int questId) {
		StartResult verdict = evaluateNpcAcquire(player, questId);
		return verdict.started() ? commit(player, questId) : verdict;
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

	/** 真端 {@code CanAcquireQuest} 等价判定（不发状态、不改存档）。 / Retail acquisition adjudication, no writes. */
	public StartResult evaluateNpcAcquire(Player player, int questId) {
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
		int minLevel = intOr(row, "minlevel_permitted", 0);
		int maxLevel = intOr(row, "maxlevel_permitted", 0);
		int level = player.getLevel();
		if (minLevel != 0 && level < minLevel) {
			return new StartResult(Outcome.LEVEL_BLOCKED, "level " + level + " < minlevel " + minLevel);
		}
		if (maxLevel != 0 && level > maxLevel) {
			return new StartResult(Outcome.LEVEL_BLOCKED, "level " + level + " > maxlevel " + maxLevel);
		}
		if (!racePermitted(row.text("race_permitted"), player.getRace() == null ? null : player.getRace().name())) {
			return new StartResult(Outcome.RACE_BLOCKED, "race " + player.getRace());
		}
		String playerClass = player.getCommonData() == null || player.getCommonData().getPlayerClass() == null
				? null
				: player.getCommonData().getPlayerClass().name().toLowerCase(Locale.ROOT);
		// 职业词表与生产元数据同源（真端 token → PlayerClass，基础职业 ≥10 展开进阶线）；
		// 逐字比较 token 会让 fighter/knight/wizard 这类真端名永远不匹配。
		// Class tokens share the production metadata mapping; a literal token comparison would never
		// match retail names such as fighter/knight/wizard.
		java.util.Set<String> permittedClasses = RetailQuestMetadataCompiler.permittedClassNames(
				row.text("class_permitted"), minLevel);
		if (!permittedClasses.isEmpty()
				&& (playerClass == null || !permittedClasses.contains(playerClass.toUpperCase(Locale.ROOT)))) {
			return new StartResult(Outcome.CLASS_BLOCKED, "class " + playerClass);
		}
		String gender = player.getGender() == null ? null : player.getGender().name().toLowerCase(Locale.ROOT);
		if (!tokenPermitted(row.text("gender_permitted"), gender)) {
			return new StartResult(Outcome.GENDER_BLOCKED, "gender " + gender);
		}
		if (!row.text("bm_restrict_category").isBlank()) {
			// 未决项：真端按 128 位地图位集判定（NPCServer fun_052.cpp:3726 写入 / CanAcquireQuest 消费），
			// 本服尚无该位集来源 ⇒ fail-closed，禁止放行兜底。
			return new StartResult(Outcome.BM_RESTRICT_UNRESOLVED,
					"bm_restrict_category=" + row.text("bm_restrict_category"));
		}
		List<Integer> missing = missingPrerequisites(player, row);
		if (!missing.isEmpty()) {
			return new StartResult(Outcome.PREREQUISITE_MISSING, "unfinished prerequisites " + missing);
		}
		return repeatVerdict(state, row);
	}

	/** 重复上限（真端 {@code finishedcount < max_repeat_count}）。 / Repeat budget. */
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
		return new StartResult(Outcome.STARTED, "repeatable " + completed + "/" + maxRepeat);
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
					// 带 {@code :n} 后缀（真端奖励分支）的行还要比对该前置的奖励档；
					// 档位解析与生产元数据同源。 / A {@code :n} suffix also pins the prerequisite's
					// reward slot, parsed through the shared production rule.
					boolean rewardMatches = token.indexOf(':') < 0
							|| (prerequisite != null && prerequisite.getReward()
								== RetailQuestMetadataCompiler.prerequisiteRewardMode(questId, token));
					if (prerequisite == null || prerequisite.getStatus() != QuestStatus.COMPLETE || !rewardMatches) {
						missing.add(questId);
					}
				}
			}
		}
		return missing;
	}

	/** {@code Q50010} / {@code Q1007:1} → 50010 / 1007（冒号后缀是奖励分支，不属 id）。 /
	 * {@code Q50010} / {@code Q1007:1} → quest id; the colon suffix is a reward-branch annotation. */
	private static int prerequisiteId(String value) {
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
			throw new IllegalStateException("NATIVE_TABLE_PARSE_FAILED: bad finished_quest_cond '" + value + "'");
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
		refreshController(player);
		return new StartResult(Outcome.STARTED, "START");
	}

	private static void refreshController(Player player) {
		try {
			if (player.getController() != null) {
				player.getController().updateZone();
				player.getController().updateNearbyQuests();
			}
		} catch (RuntimeException ignored) {
			// 单测/无控制器环境：状态已落库，刷新为尽力而为。
			// Headless environments: the state is committed; the refresh is best effort.
		}
	}

	private static int intOr(NativeQuestXmlTable.QuestRow row, String tag, int fallback) {
		Integer value = row.integer(tag);
		return value == null ? fallback : value;
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
