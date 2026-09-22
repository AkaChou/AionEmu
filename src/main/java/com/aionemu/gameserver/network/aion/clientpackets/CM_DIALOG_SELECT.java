package com.aionemu.gameserver.network.aion.clientpackets;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.aionemu.boot.i18n.I18n;
import com.aionemu.gameserver.configs.main.LoggingConfig;
import com.aionemu.gameserver.lifecycle.GameEngineServices;

import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.DialogSelectRepeat;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.AionClientPacket;
import com.aionemu.gameserver.network.aion.AionConnection.State;
import com.aionemu.gameserver.network.aion.serverpackets.SM_DIALOG_WINDOW;
import com.aionemu.gameserver.questEngine.QuestEngine;
import com.aionemu.gameserver.questEngine.definition.QuestDialogAction;
import com.aionemu.gameserver.questEngine.definition.QuestDialogPage;
import com.aionemu.gameserver.questEngine.definition.QuestEvent;
import com.aionemu.gameserver.questEngine.definition.QuestMetadata;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.services.ClassChangeService;
import com.aionemu.gameserver.services.QuestService;
import com.aionemu.gameserver.utils.PacketSendUtility;
/**
 * 选择 NPC/任务对话选项的客户端包。
 * Client packet selecting an NPC or quest dialog option.
 */
@Slf4j

public class CM_DIALOG_SELECT extends AionClientPacket {
	/** 连续相同对话选择达到该次数即判定为客户端重发死循环。 / Consecutive identical selections that mark a client resend loop. */
	static final int MAX_IDENTICAL_DIALOG_SELECTS = 4;
	/** 相邻相同对话选择的最大间隔；超过后重新计数。 / Maximum gap between identical selections before the counter restarts. */
	static final long DIALOG_SELECT_REPEAT_WINDOW_MILLIS = 5_000;
	/**
	 * 计入死循环的最小重发间隔：更快的重复视为人类连点或客户端重放，不计入循环计数。
	 * Minimum resend gap that counts towards a loop: faster repeats are human rapid clicks or client
	 * replays and restart the counter.
	 */
	static final long MIN_DIALOG_SELECT_RESEND_GAP_MILLIS = 800;

	/** 任务追踪日志出口，路由到 logback 的 quest logger。 / Quest trace sink for logback quest logger. */
	private static final Logger QUEST_TRACE_LOG = LoggerFactory.getLogger("quest");

	private int targetObjectId;
	private int dialogId;
	private int extendedRewardIndex;
	private int lastPage;
	private int questId;


	/**
	 * 构造客户端包实例。
	 * Constructs a new client packet instance.
	 * packet opcode
	 * @param state 连接状态 / connection state
	 * @param restStates 其余允许状态 / additional allowed states
	 */
	public CM_DIALOG_SELECT(int opcode, State state, State... restStates) {
		super(opcode, state, restStates);
	}

	@Override
	protected void readImpl() {
		targetObjectId = readD();
		dialogId = readH();
		extendedRewardIndex = readH();
		readH();
		lastPage = readH();
		questId = readD();
		readH();
	}

	/**
	 * 判断客户端是否从通用任务选择页发起了非任务行动作。
	 * Determines whether the client sent a non-quest-row action from the generic quest-selection page.
	 * <p>关闭 {@code show_acquirable_normal_quest} 后，5.8 客户端可能携带或不携带候选任务 ID；
	 * 第 10 页中的非任务行动作不能建立任务上下文，只有携带 questId&gt;0 的任务行选择（31）可以。</p>
	 * <p>When {@code show_acquirable_normal_quest} is disabled, the 5.8 client may or may not attach a candidate
	 * quest id; only a quest-row selection (31) carrying questId&gt;0 can establish quest context on page 10.</p>
	 * @param dialogId 对话动作 ID / dialog action id
	 * @param lastPage 客户端发包前所在页面 / page shown by the client before sending the packet
	 * @return 是否为通用页中的非任务行动作 / whether this is a non-quest-row action from the generic page
	 */
	static boolean isGenericQuestSelectionPage(int dialogId, int lastPage) {
		return lastPage == QuestDialogPage.SELECT_QUEST.id()
			&& dialogId != QuestDialogAction.QUEST_SELECT.id();
	}

	/**
	 * 判断是否是从 NPC 任务列表点击任务行进入任务上下文的唯一入口。
	 * Determines whether this is the only quest-context entry from an NPC quest-list row click.
	 */
	static boolean isNpcQuestRowSelection(int targetObjectId, int dialogId, int lastPage, int questId) {
		return targetObjectId > 0 && questId > 0
			&& lastPage == QuestDialogPage.SELECT_QUEST.id()
			&& dialogId == QuestDialogAction.QUEST_SELECT.id();
	}

	/**
	 * 判断 NPC 选择是否携带任务上下文；没有上下文的 NPC 选项按普通对话处理。
	 * Determines whether an NPC selection carries quest context; NPC options without it are plain dialogs.
	 * @param npcTarget 目标是否为 NPC / whether the target is an NPC
	 * @param routedQuestId 客户端携带或已记住的任务 ID / quest id from the client or remembered selection
	 * @return 是否进入任务引擎 / whether to enter the quest engine
	 */
	static boolean hasQuestDialogContext(boolean npcTarget, int routedQuestId) {
		return !npcTarget || routedQuestId != 0;
	}

	/**
	 * 解析 NPC 对话应携带的任务上下文。
	 * Resolves the quest context carried by an NPC dialog selection.
	 * <p>通用任务选择页上的非任务行动作永远不能建立任务上下文；其他页面优先使用客户端携带的任务
	 * ID，客户端未携带时再使用同一 NPC 的任务行记忆。</p>
	 * <p>A non-quest-row action from the generic quest-selection page can never establish quest context;
	 * other pages prefer the client-provided quest id and fall back to the remembered quest-row selection.</p>
	 * @param clientQuestId 客户端携带的任务 ID / client-provided quest id
	 * @param rememberedQuestId 同一 NPC 的任务行记忆 / remembered quest-row selection for the same NPC
	 * @param genericQuestPage 是否为通用任务选择页上的非任务行动作 /
	 *                         whether this is a non-quest-row action from the generic quest-selection page
	 * @return 路由任务 ID，0 表示无任务上下文 / routed quest id; 0 means no quest context
	 */
	static int resolveRoutedQuestId(int clientQuestId, int rememberedQuestId, boolean genericQuestPage) {
		if (genericQuestPage) {
			return 0;
		}
		return clientQuestId > 0 ? clientQuestId : rememberedQuestId;
	}

	/**
	 * 判断普通任务是否不在玩家当前进行中/待领奖阶段。
	 * Determines whether a normal quest is outside the player's active or reward-pending progress.
	 */
	static boolean isNormalQuestOutsideActiveProgress(QuestMetadata metadata, QuestState questState) {
		if (metadata == null || !"QUEST".equals(metadata.category())) {
			return false;
		}
		return questState == null || (questState.getStatus() != QuestStatus.START
			&& questState.getStatus() != QuestStatus.REWARD);
	}

	@Override
	protected void runImpl() {
		final Player player = getConnection().getActivePlayer();
		if (LoggingConfig.LOG_QUEST_TRACE || (player != null && player.isQuestTraceEnabled())) {
			int routedQuestIdTrace = questId > 0 ? questId
				: targetObjectId > 0 && player != null && player.getKnownList().getObject(targetObjectId) instanceof Npc npc
					? player.getNpcQuestDialogSelectionQuestId(npc.getObjectId()) : 0;
			int traceNpcId = targetObjectId > 0 && player != null && player.getKnownList().getObject(targetObjectId) instanceof Npc npc
				? npc.getNpcId() : 0;
			QUEST_TRACE_LOG.info(I18n.get("log.quest_trace.dialog_select",
				player != null ? player.getName() : "unknown",
				traceNpcId,
				targetObjectId,
				routedQuestIdTrace,
				lastPage,
				dialogId));
		}
		if (player != null && breakDialogSelectLoop(player, targetObjectId, dialogId, lastPage, questId)) {
			return;
		}
		QuestEngine questEngine = GameEngineServices.questEngine();
		var metadata = questEngine.questCatalog().findMetadata(questId).orElse(null);
		QuestEnv env = new QuestEnv(null, player, questId, 0);

        /* 	if (player.isInPlayerMode(PlayerMode.RIDE)) { - dismount player with pet when interact with npc.
			 player.unsetPlayerMode(PlayerMode.RIDE);
        } */

		if (player.isTrading()) {
			return;
		}
		if (targetObjectId == 0 || targetObjectId == player.getObjectId()) {
			if (metadata != null && !metadata.cannotShare() && (dialogId == 1002 || dialogId == 20000)) {
				if (player.consumePendingQuestShare(questId)) {
					if (questEngine.questCatalog().findExecutable(questId).isPresent()) {
						questEngine.onSharedQuestDialog(new QuestEnv(null, player, questId, dialogId));
					} else {
						QuestService.startQuest(env);
					}
					return;
				}
			}
			// Page 10 only accepts the quest-row action (31). A different action must not
			// enter QuestEngine through the targetless/class-change branch, even when the
			// client attaches a candidate quest id while normal-quest markers are hidden.
			// 第 10 页只允许任务行动作（31）；即使客户端在关闭普通任务标记时附带候选任务 ID，
			// 其他动作也不能通过无目标/玩家对象分支进入任务引擎。
			if (isGenericQuestSelectionPage(dialogId, lastPage)) {
				player.clearNpcQuestDialogSelection();
				return;
			}
			if (questId > 0 && isNormalQuestOutsideActiveProgress(metadata,
				player.getQuestStateList().getQuestState(questId))
				&& !questEngine.hasMatchingRoutes(new QuestEvent.QuestDialog(dialogId), questId)) {
				player.clearNpcQuestDialogSelection();
				return;
			}
			if (questEngine.onDialog(new QuestEnv(null, player, questId, dialogId))) {
				return;
			}
			ClassChangeService.changeClassToSelection(player, questId, dialogId);
			return;
		}
		VisibleObject obj = player.getKnownList().getObject(targetObjectId);
		if (obj instanceof Creature creature) {
			boolean npcTarget = obj instanceof Npc;
			boolean genericQuestPage = npcTarget && isGenericQuestSelectionPage(dialogId, lastPage);
			if (genericQuestPage) {
				player.clearNpcQuestDialogSelection();
			}
			if (obj instanceof Npc && isNpcQuestRowSelection(targetObjectId, dialogId, lastPage, questId)) {
				player.rememberNpcQuestDialogSelection(targetObjectId, questId);
			}
			int rememberedQuestId = obj instanceof Npc npc
				? player.getNpcQuestDialogSelectionQuestId(npc.getObjectId()) : 0;
			int routedQuestId = resolveRoutedQuestId(questId, rememberedQuestId, genericQuestPage);
			// 没有任务上下文的 NPC 选项只是普通对话：关闭“未满65级普通任务标记”后，5.8 客户端
			// 仍可能把 NPC 对话页上的任务按钮发回来；若绑定未接取任务 owner，服务端会下发带 questId
			// 的任务页并触发 load fail。与“该 NPC 任务已全部完成”一致，跳过任务引擎，直接交给
			// NPC AI / DialogService 做普通页面导航；有任务上下文时保持原有任务路由。
			// An NPC option without quest context is a plain dialog: after the normal-quest marker is
			// disabled, the 5.8 client can still send quest-page buttons with questId==0. Binding an
			// unaccepted owner would send a quest-id-tagged page and make the client fail to load it.
			// Match the "all quests on this NPC are done" behavior by skipping the quest engine and
			// letting the NPC AI / DialogService navigate the page. A real quest context keeps the
			// original quest routing.
			if (hasQuestDialogContext(npcTarget, routedQuestId)) {
				creature.getController().onDialogSelect(dialogId, player, routedQuestId, extendedRewardIndex);
			} else {
				creature.getController().onSimpleDialogSelect(dialogId, player, extendedRewardIndex);
			}
		}
	}

	/**
	 * 跟踪同一个对话选择的连续重发；达到阈值时记录告警、清除对话上下文并关闭客户端窗口，打断死循环。
	 * Tracks consecutive resends of the same dialog selection; on reaching the threshold it logs a
	 * warning, clears the dialog context, and closes the client window to break the loop.
	 * <p>触发条件是「同一目标、同一上一页、同一动作、同一任务」按客户端重发节奏（间隔在
	 * {@link #MIN_DIALOG_SELECT_RESEND_GAP_MILLIS} 与 {@link #DIALOG_SELECT_REPEAT_WINDOW_MILLIS} 之间）
	 * 连续出现：这种情况下服务端仍然没有下发后续页，客户端会一直重发 {@code CM_DIALOG_SELECT}
	 * （例如 movie self-loop 缺少目标页）。人类连点比该节奏更快，会被排除，避免误伤。</p>
	 * <p>The trigger is the same target, previous page, action, and quest id repeating at the client resend
	 * cadence (gap between {@link #MIN_DIALOG_SELECT_RESEND_GAP_MILLIS} and
	 * {@link #DIALOG_SELECT_REPEAT_WINDOW_MILLIS}), which means the server still produced no continuation
	 * and the client keeps resending {@code CM_DIALOG_SELECT} (for example a movie self-loop that lacks its
	 * target page). Human rapid clicks are faster than that cadence and are excluded.</p>
	 * @param player 发包玩家 / sending player
	 * @param targetObjectId 交互目标对象 ID / interaction target object id
	 * @param dialogId 对话动作 ID / dialog action id
	 * @param lastPage 客户端发包前所在页面 / page shown by the client before sending the packet
	 * @param questId 客户端携带或记忆的任务 ID / quest id carried or remembered for this selection
	 * @return 是否已判定并打断循环 / whether the loop was detected and broken
	 */
	static boolean breakDialogSelectLoop(Player player, int targetObjectId, int dialogId, int lastPage,
			int questId) {
		DialogSelectRepeat repeat = nextDialogSelectRepeat(player.getDialogSelectRepeat(), targetObjectId,
			lastPage, dialogId, questId, System.currentTimeMillis());
		player.setDialogSelectRepeat(repeat);
		if (repeat.count() < MAX_IDENTICAL_DIALOG_SELECTS) {
			return false;
		}
		player.clearDialogSelectRepeat();
		player.clearNpcQuestDialogSelection();
		VisibleObject target = targetObjectId > 0 ? player.getKnownList().getObject(targetObjectId) : null;
		int npcId = target instanceof Npc npc ? npc.getNpcId() : 0;
		log.warn(I18n.get("log.quest_dialog_select_loop", player.getName(), npcId, targetObjectId, dialogId,
			lastPage, questId, repeat.count()));
		PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(0, 0));
		return true;
	}

	/**
	 * 计算本次对话选择的重发跟踪快照：签名相同且间隔落在死循环节奏内则计数加一，否则从 1 重新开始。
	 * Computes the repeat-tracking snapshot for the current dialog selection: identical signatures whose
	 * gap matches the loop cadence increment the counter and anything else restarts it at one.
	 */
	static DialogSelectRepeat nextDialogSelectRepeat(DialogSelectRepeat previous, int targetObjectId,
			int lastPage, int dialogId, int questId, long nowMillis) {
		if (previous != null && previous.matches(targetObjectId, lastPage, dialogId, questId)
				&& isLoopResendGap(nowMillis - previous.lastMillis())) {
			return previous.incremented(nowMillis);
		}
		return new DialogSelectRepeat(targetObjectId, lastPage, dialogId, questId, 1, nowMillis);
	}

	/**
	 * 判断重发间隔是否落在死循环节奏内：太快（人类连点）与太慢（间隔超过窗口）都会重新计数。
	 * Checks whether the resend gap matches the loop cadence: both rapid human clicks and slow gaps
	 * restart the counter.
	 */
	private static boolean isLoopResendGap(long gapMillis) {
		return gapMillis >= MIN_DIALOG_SELECT_RESEND_GAP_MILLIS
			&& gapMillis <= DIALOG_SELECT_REPEAT_WINDOW_MILLIS;
	}
}
