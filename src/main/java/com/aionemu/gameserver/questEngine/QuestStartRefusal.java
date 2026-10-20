package com.aionemu.gameserver.questEngine;

import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.DescriptionId;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.TitleTemplate;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.questEngine.definition.QuestMetadata;
import com.aionemu.gameserver.questEngine.definition.QuestStartCondition;
import com.aionemu.gameserver.questEngine.definition.QuestStartConditionGroup;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.questEngine.runtime.QuestStartEligibility;
import com.aionemu.gameserver.questEngine.tablelane.NativeQuestStartPort;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.stats.AbyssRankEnum;

import java.util.List;
import java.util.Locale;

/**
 * 接取被拒时下发给玩家的系统消息（原版 {@code STR_QUEST_ACQUIRE_ERROR_*} 族）。
 * <p>
 * 三条接取车道（typed 目录、native 表车道、legacy 模板车道）在拒绝面共用本入口：先定位唯一命中的
 * 拒绝轴，再按原版 ResourceId 规则填占位符并下发：
 * <ul>
 *   <li>等级：任务声明的下限/上限（十进制数）；</li>
 *   <li>军衔：玩家当前军衔的 {@link DescriptionId}（与 legacy 车道同源，客户端本地化）；</li>
 *   <li>制作技能：任务要求的熟练度档位（39/49/399/499）；</li>
 *   <li>称号：任务要求的称号 {@link DescriptionId}；</li>
 *   <li>前置任务：真正未满足的那一条的任务名。</li>
 * </ul>
 * 没有任何轴命中（页状态机拒绝、任务栏已满、限次来源耗尽等服务端状态）时不下发——**绝不臆造
 * 通用文案**，原版资源里并不存在「无法接取该任务」这类通用句（2026-10-19 实机 19679 此前正是
 * 「点了接受、窗一关、任务没接上、零反馈」）。
 * <p>
 * Player-facing messages for refused quest acquisition, filled with the retail placeholder shapes
 * (level as a decimal, rank/title as {@link DescriptionId}, production grade, prerequisite quest name).
 * A refusal without a retail-mapped axis stays silent: no generic text is invented.
 */
public final class QuestStartRefusal {

	private QuestStartRefusal() {
	}

	/**
	 * 按已命中的 native 判定结果下发拒绝提示；不改变任何任务状态。
	 * Sends the refusal message for an already adjudicated native verdict, without touching quest state.
	 * @param player 玩家 / the player
	 * @param questId 任务 ID / quest id
	 * @param verdict 已命中的 native 判定 / the hit native verdict
	 * @return 是否下发了提示 / whether a message was sent
	 */
	public static boolean notifyIfRefused(Player player, int questId, NativeQuestStartPort.AcquireVerdict verdict) {
		if (player == null || questId <= 0 || verdict == null || verdict.started()) {
			return false;
		}
		SM_SYSTEM_MESSAGE message = messageFor(verdict, questId);
		return message != null && send(player, message);
	}

	/**
	 * 为 typed owner 定位拒绝轴并下发提示（没有资格投影时才回落到 native 判定）。
	 * Locates the refusal axis of a typed owner and sends its message; without an eligibility projection
	 * the native verdict is used instead.
	 * @param player 玩家 / the player
	 * @param questId 任务 ID / quest id
	 * @param eligibility 该 owner 的资格投影（由 typed dispatcher 在只读快照上取得）
	 *                    / the owner's eligibility projection from the read-only typed snapshot
	 * @return 是否下发了提示 / whether a message was sent
	 */
	public static boolean notifyTypedRefused(Player player, int questId, QuestStartEligibility eligibility) {
		if (player == null || questId <= 0) {
			return false;
		}
		if (eligibility == null) {
			return notifyIfRefused(player, questId,
				NativeQuestStartPort.instance().evaluateAcquire(player, questId));
		}
		if (eligibility.eligible()) {
			return false;
		}
		SM_SYSTEM_MESSAGE message = messageFor(eligibility.reason(), player, questId);
		return message != null && send(player, message);
	}

	/**
	 * 返回 native 表车道拒绝结论对应的原版消息；{@code null} 表示该轴在原版不报错。
	 * Returns the retail message for an already adjudicated native verdict, or {@code null} when silent.
	 */
	static SM_SYSTEM_MESSAGE messageFor(NativeQuestStartPort.AcquireVerdict verdict, int questId) {
		return switch (verdict.result().outcome()) {
			// 下限取判定结果本身（native 行已把它解析出来），上限取该行的原版列；
			// 元数据/表都缺时资格端口不会以等级轴拒绝，因此这里无需再兜 0。
			// The lower bound comes from the verdict itself; the upper bound from the row.
			case LEVEL_BLOCKED -> verdict.levelTooLow()
				? SM_SYSTEM_MESSAGE.STR_QUEST_ACQUIRE_ERROR_MIN_LEVEL(Integer.toString(verdict.minLevel()))
				: SM_SYSTEM_MESSAGE.STR_QUEST_ACQUIRE_ERROR_MAX_LEVEL(
					Integer.toString(nativeLevel(questId, "maxlevel_permitted")));
			case RACE_BLOCKED -> SM_SYSTEM_MESSAGE.STR_QUEST_ACQUIRE_ERROR_RACE;
			case CLASS_BLOCKED -> SM_SYSTEM_MESSAGE.STR_QUEST_ACQUIRE_ERROR_CLASS;
			case GENDER_BLOCKED -> SM_SYSTEM_MESSAGE.STR_QUEST_ACQUIRE_ERROR_GENDER;
			// native 前置轴 detail 形如 "unfinished prerequisites [1131]"，取出任务 id 换成任务名。
			// The native prerequisite detail lists quest ids; the message carries the quest name.
			case PREREQUISITE_MISSING -> SM_SYSTEM_MESSAGE.STR_QUEST_ACQUIRE_ERROR_FINISHED_QUEST(
				nativePrerequisiteName(questId, verdict.result().detail()));
			// 重复上限 / 进行中 / 缺行 / 账号限制位：原版在这些面上本就不报此句，保持静默。
			// Repeat limit, in progress, missing row, account restriction: retail stays silent.
			case REPEAT_LIMIT, ALREADY_RUNNING, MISSING_ROW, BM_RESTRICT_BLOCKED, STARTED -> null;
		};
	}

	/**
	 * 返回资格端口拒绝原因对应的原版消息；{@code null} 表示该轴在原版不报错。
	 * Returns the retail message for an eligibility rejection reason, or {@code null} when silent.
	 */
	static SM_SYSTEM_MESSAGE messageFor(String reason, Player player, int questId) {
		if (reason == null) {
			return null;
		}
		QuestMetadata metadata = metadata(questId);
		return switch (reason) {
			// typed owner 的等级轴来自目录元数据；取不到元数据时回落到原版表的同名列，再不行取玩家的
			// 实际等级（提示的是「至少要几级」的最低可解释值），绝不下发 0。
			// The typed level axis comes from the catalog metadata; without it the retail column is used,
			// then the player's actual level, so the message never claims a bound of 0.
			case "MIN_LEVEL_NOT_MET" -> SM_SYSTEM_MESSAGE.STR_QUEST_ACQUIRE_ERROR_MIN_LEVEL(
				Integer.toString(metadata != null ? metadata.minLevel()
					: levelOrNative(questId, player, "minlevel_permitted")));
			case "MAX_LEVEL_EXCEEDED" -> SM_SYSTEM_MESSAGE.STR_QUEST_ACQUIRE_ERROR_MAX_LEVEL(
				Integer.toString(metadata != null ? metadata.maxLevel()
					: levelOrNative(questId, player, "maxlevel_permitted")));
			case "RACE_NOT_PERMITTED" -> SM_SYSTEM_MESSAGE.STR_QUEST_ACQUIRE_ERROR_RACE;
			case "CLASS_NOT_PERMITTED" -> SM_SYSTEM_MESSAGE.STR_QUEST_ACQUIRE_ERROR_CLASS;
			case "GENDER_NOT_PERMITTED" -> SM_SYSTEM_MESSAGE.STR_QUEST_ACQUIRE_ERROR_GENDER;
			// 军衔占位符 = 军衔名（DescriptionId），与 legacy 车道同源。
			// The rank placeholder is the rank name (DescriptionId), the same source the legacy lane uses.
			case "RANK_NOT_MET" -> player.getAbyssRank() == null ? null
				: SM_SYSTEM_MESSAGE.STR_QUEST_ACQUIRE_ERROR_MIN_RANK(
					AbyssRankEnum.getRankDescriptionId(player).getValue());
			case "CRAFT_QUALIFICATION_NOT_MET" -> SM_SYSTEM_MESSAGE.STR_QUEST_ACQUIRE_ERROR_TS_RANK(
				Integer.toString(metadata == null || metadata.combineSkillPoint() == null
					? 0 : metadata.combineSkillPoint()));
			// 称号占位符 = 称号名的客户端本地化串（静态表 {@code nameId} → DescriptionId，与
			// {@code STR_QUEST_GET_REWARD_TITLE(nameId)} 同型）；原消息模板收字符串，故按串 id 下发。
			// The title placeholder is the client-localized title name (the template's nameId).
			case "TITLE_MISSING" -> metadata == null ? null
				: SM_SYSTEM_MESSAGE.STR_QUEST_ACQUIRE_ERROR_TITLE(
					Integer.toString(titleNameId(metadata.titleId())));
			case "START_CONDITION_REJECTED" -> SM_SYSTEM_MESSAGE.STR_QUEST_ACQUIRE_ERROR_FINISHED_QUEST(
				unmetPrerequisiteName(metadata, player));
			// 任务栏满：原版有专属句（1300622，legacy 车道同样下发）；玩家可自行清理任务后重试，
			// 不属于「服务端状态」静默面。
			// Full journal: retail has its own line (1300622, the legacy lane sends it too); the player
			// can clear a slot and retry, so it is not a silent server-state axis.
			case "QUEST_LIST_FULL" -> SM_SYSTEM_MESSAGE.STR_QUEST_ACQUIRE_ERROR_MAX_NORMAL;
			// 限次来源耗尽 / 势力轮换 / 进行中 / 缺元数据：服务端状态，原版不在此面报错。
			// Limited supply, faction rotation, in progress, missing metadata: silent.
			default -> null;
		};
	}

	private static boolean send(Player player, SM_SYSTEM_MESSAGE message) {
		try {
			PacketSendUtility.sendPacket(player, message);
			return true;
		} catch (RuntimeException failure) {
			// 提示是尽力而为的附加面：拒绝结论已定，提示失败绝不能反过来改变接取行为。
			// The message is a best-effort side face; a failure here must never change the refusal.
			return false;
		}
	}

	/**
	 * 返回真正未满足的那一条前置任务名。
	 * <p>
	 * 起始条件分组之间是「或」：任一整组成立即通过，因此先找**整组都不成立**的分组，再取组内第一条
	 * 未满足的引用；分组全部成立时再看 {@code prerequisites} 轴。条件求值与资格端口逐条对齐（状态、
	 * 奖励档、完成计数），报出的前置就是资格端口真正拒绝的那一条。
	 * <p>
	 * Returns the name of the prerequisite that actually fails. Condition groups are alternatives, so a
	 * group that holds entirely passes; otherwise the first unmet reference inside a failing group is
	 * reported, and the plain {@code prerequisites} axis is checked when every group holds.
	 */
	static String unmetPrerequisiteName(QuestMetadata metadata, Player player) {
		if (metadata == null) {
			return "";
		}
		List<QuestStartConditionGroup> groups = metadata.startConditionGroups();
		if (!groups.isEmpty()) {
			for (QuestStartConditionGroup group : groups) {
				if (group.conditions().stream().allMatch(condition -> satisfied(condition, player))) {
					return "";
				}
			}
			for (QuestStartConditionGroup group : groups) {
				for (QuestStartCondition condition : group.conditions()) {
					if (!satisfied(condition, player)) {
						return questName(condition.questId());
					}
				}
			}
		}
		for (int prerequisiteId : metadata.prerequisites()) {
			QuestState state = player.getQuestStateList() == null
				? null : player.getQuestStateList().getQuestState(prerequisiteId);
			if (state == null || state.getStatus() != QuestStatus.COMPLETE) {
				return questName(prerequisiteId);
			}
		}
		return "";
	}

	/** 起始条件判定：与 {@code PlayerQuestStartEligibilityPort#startConditionMet} 逐条对齐。 / Mirrors the port's rule. */
	private static boolean satisfied(QuestStartCondition condition, Player player) {
		QuestState state = player.getQuestStateList() == null
			? null : player.getQuestStateList().getQuestState(condition.questId());
		return switch (condition.type().toLowerCase(Locale.ROOT)) {
			case "finished" -> state != null && state.getStatus() == QuestStatus.COMPLETE
				&& condition.rewardMode() == rewardOf(state) && repeatCompletionMatches(condition, state);
			case "unfinished" -> state == null || state.getStatus() != QuestStatus.COMPLETE;
			case "noacquired" -> state == null || (state.getStatus() != QuestStatus.START
				&& state.getStatus() != QuestStatus.REWARD && state.getStatus() != QuestStatus.COMPLETE);
			case "acquired" -> state != null && state.getStatus() != QuestStatus.NONE
				&& state.getStatus() != QuestStatus.LOCKED;
			case "equipped" -> player.getEquipment() != null
				&& player.getEquipment().getEquippedItemIds().contains(condition.questId());
			default -> false;
		};
	}

	/**
	 * 完成计数轴：原版「完成计数 >= 前置行 max_repeat_count」，1 与 255 不附加计数要求（同资格端口）。
	 * The completion-count axis; 1 and 255 (unlimited) add no requirement, matching the eligibility port.
	 */
	private static boolean repeatCompletionMatches(QuestStartCondition condition, QuestState state) {
		QuestMetadata prerequisite = metadata(condition.questId());
		int maxRepeat = prerequisite == null ? 0 : prerequisite.repeatPolicy().maxRepeatCount();
		return maxRepeat == 1 || maxRepeat == 255 || state.getCompleteCount() >= maxRepeat;
	}

	/**
	 * 已存档奖励档；未选过奖励的存档没有档位，按 0 处理而不触发 {@code getReward()} 的告警。
	 * The stored reward slot; an unset slot counts as 0 without triggering {@code getReward()}'s warning.
	 */
	private static int rewardOf(QuestState state) {
		return state.getRewardOrNull() == null ? 0 : state.getRewardOrNull();
	}

	/**
	 * 称号模板的 {@code nameId}（客户端本地化名的串 id）；模板解不出时退回称号 id。
	 * Returns the title template's name id (the client string id of the localized name).
	 */
	private static int titleNameId(int titleId) {
		try {
			TitleTemplate title = DataManager.TITLE_DATA.getTitleTemplate(titleId);
			if (title != null && title.getNameId() > 0) {
				return title.getNameId();
			}
		} catch (RuntimeException unavailable) {
			// 静态数据未装载（单测/启动早期）：退回称号 id。
			// Static data not loaded (unit tests, early boot): fall back to the title id.
		}
		return titleId;
	}

	private static String questName(int questId) {
		QuestMetadata referenced = metadata(questId);
		return referenced == null ? Integer.toString(questId) : referenced.name();
	}

	/**
	 * native 前置轴的任务名：优先取 detail 里列出的第一条未完成 id（换成任务名），解不出再用原版列。
	 * The first unmet id listed in the native detail, as its quest name; the retail column as fallback.
	 */
	private static String nativePrerequisiteName(int questId, String detail) {
		int open = detail == null ? -1 : detail.indexOf('[');
		int close = open < 0 ? -1 : detail.indexOf(']', open);
		if (open >= 0 && close > open) {
			for (String token : detail.substring(open + 1, close).split("[\\s,]+")) {
				try {
					int unmet = Integer.parseInt(token.trim());
					return unmet <= 0 ? Integer.toString(unmet) : questName(unmet);
				} catch (NumberFormatException ignored) {
					// 非数字 token 跳过，继续找下一个。
					// Skip non-numeric tokens and keep looking.
				}
			}
		}
		return NativeQuestStartPort.instance().displayText(questId, "finished_quest_cond1");
	}

	private static int nativeLevel(int questId, String tag) {
		return NativeQuestStartPort.instance().displayInteger(questId, tag, 0);
	}

	/**
	 * 元数据缺失时的等级展示值：原版表同名列优先，表也没有则退回玩家实际等级。
	 * Level shown when metadata is missing: the retail column, else the player's own level.
	 */
	private static int levelOrNative(int questId, Player player, String tag) {
		int declared = nativeLevel(questId, tag);
		return declared > 0 ? declared : player.getLevel();
	}

	private static QuestMetadata metadata(int questId) {
		if (questId <= 0) {
			return null;
		}
		try {
			return QuestEngine.getInstance().questCatalog().findMetadata(questId).orElse(null);
		} catch (RuntimeException unavailable) {
			return null;
		}
	}
}
