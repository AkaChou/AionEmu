package com.aionemu.gameserver.network.aion.clientpackets;

import lombok.extern.slf4j.Slf4j;
import com.aionemu.gameserver.lifecycle.GameEngineServices;

import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.actions.PlayerMode;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.QuestTemplate;
import com.aionemu.gameserver.network.aion.AionClientPacket;
import com.aionemu.gameserver.network.aion.AionConnection.State;
import com.aionemu.gameserver.questEngine.QuestEngine;
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
	@SuppressWarnings("unused")
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

	@Override
	protected void runImpl() {
		final Player player = getConnection().getActivePlayer();
		QuestTemplate questTemplate = DataManager.QUEST_DATA.getQuestById(questId);
		QuestEnv env = new QuestEnv(null, player, questId, 0);

        /* 	if (player.isInPlayerMode(PlayerMode.RIDE)) { - dismount player with pet when interact with npc.
			 player.unsetPlayerMode(PlayerMode.RIDE);
        } */

		if (player.isTrading()) {
			return;
		}
		if (targetObjectId == 0 || targetObjectId == player.getObjectId()) {
			if (questTemplate != null && !questTemplate.isCannotShare() && (dialogId == 1002 || dialogId == 20000)) {
				QuestService.startQuest(env);
				return;
			}
			if (questTemplate != null && QuestService.isReportedRewardAction(dialogId)) {
				QuestService.finishReportedQuest(player, questId, dialogId);
				return;
			}
			if (GameEngineServices.questEngine().onDialog(new QuestEnv(null, player, questId, dialogId))) {
				return;
			}
			ClassChangeService.changeClassToSelection(player, dialogId);
			return;
		}
		VisibleObject obj = player.getKnownList().getObject(targetObjectId);
		if (obj != null && obj instanceof Creature) {
			Creature creature = (Creature) obj;
			creature.getController().onDialogSelect(dialogId, player, questId, extendedRewardIndex);
		}
	}
}
