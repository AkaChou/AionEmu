package com.aionemu.gameserver.network.aion.clientpackets;

import lombok.extern.slf4j.Slf4j;
import com.aionemu.gameserver.lifecycle.GameEngineServices;

import com.aionemu.gameserver.model.actions.PlayerMode;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.AionClientPacket;
import com.aionemu.gameserver.network.aion.AionConnection.State;
import com.aionemu.gameserver.questEngine.QuestEngine;
import com.aionemu.gameserver.questEngine.definition.QuestDialogAction;
import com.aionemu.gameserver.questEngine.definition.QuestDialogPage;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.services.ClassChangeService;
import com.aionemu.gameserver.services.QuestService;
/**
 * 选择 NPC/任务对话选项的客户端包。
 * Client packet selecting an NPC or quest dialog option.
 */
@Slf4j

public class CM_DIALOG_SELECT extends AionClientPacket {
	private int targetObjectId;
	private int dialogId;
	private int extendedRewardIndex;
	private int lastPage;
	private int questId;


	/**
	 * 构造客户端包实例。
	 * Constructs a new client packet instance.
	 *
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
	 * 判断 NPC 是否从客户端通用任务选择页发起了无任务上下文的简单对话。
	 * Determines whether an NPC selection came from the client's generic quest-selection page without quest context.
	 *
	 * <p>关闭 {@code show_acquirable_normal_quest} 后，5.8 客户端可能携带或不携带候选任务 ID；
	 * 从 10 页选择任务（31）之外的动作都属于简单对话。</p>
	 * <p>When {@code show_acquirable_normal_quest} is disabled, the 5.8 client may or may not attach a candidate
	 * quest id; actions other than quest selection (31) from page 10 are simple dialogs.</p>
	 *
	 * @param targetObjectId NPC 对象 ID / NPC object id
	 * @param dialogId 对话动作 ID / dialog action id
	 * @param lastPage 客户端发包前所在页面 / page shown by the client before sending the packet
	 * @param questId 客户端可能携带的候选任务 ID，不参与页面上下文判定 /
	 *                candidate quest id the client may carry; not part of the page-context decision
	 * @return 是否应按简单 NPC 对话处理 / whether to process as a simple NPC dialog
	 */
	static boolean isSimpleNpcDialogSelection(int targetObjectId, int dialogId, int lastPage, int questId) {
		return targetObjectId > 0 && lastPage == QuestDialogPage.SELECT_QUEST.id()
			&& dialogId != QuestDialogAction.QUEST_SELECT.id();
	}

	@Override
	protected void runImpl() {
		final Player player = getConnection().getActivePlayer();
		var metadata = GameEngineServices.questEngine().questCatalog().findMetadata(questId).orElse(null);
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
					QuestEngine questEngine = GameEngineServices.questEngine();
					if (questEngine.questCatalog().findExecutable(questId).isPresent()) {
						questEngine.onSharedQuestDialog(new QuestEnv(null, player, questId, dialogId));
					} else {
						QuestService.startQuest(env);
					}
					return;
				}
			}
			if (GameEngineServices.questEngine().onDialog(new QuestEnv(null, player, questId, dialogId))) {
				return;
			}
			ClassChangeService.changeClassToSelection(player, questId, dialogId);
			return;
		}
		VisibleObject obj = player.getKnownList().getObject(targetObjectId);
		if (obj != null && obj instanceof Creature) {
			Creature creature = (Creature) obj;
			if (isSimpleNpcDialogSelection(targetObjectId, dialogId, lastPage, questId) && obj instanceof Npc) {
				creature.getController().onSimpleDialogSelect(dialogId, player, extendedRewardIndex);
			} else {
				creature.getController().onDialogSelect(dialogId, player, questId, extendedRewardIndex);
			}
		}
	}
}
