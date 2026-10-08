package com.aionemu.gameserver.ai;

import com.aionemu.gameserver.lifecycle.GameCoreGameplayServices;

import com.aionemu.gameserver.lifecycle.GameEngineServices;

import com.aionemu.gameserver.ai.ActionItemNpcAI2;

import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AI2Actions.SelectDialogResult;
import com.aionemu.gameserver.ai2.handler.CreatureEventHandler;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_DIALOG_WINDOW;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.questEngine.QuestEngine;
import com.aionemu.gameserver.questEngine.definition.QuestDialogAction;
import com.aionemu.gameserver.questEngine.definition.QuestDialogPage;
import com.aionemu.gameserver.questEngine.model.QuestActionType;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.tablelane.DataDrivenNativeRuntime;
import com.aionemu.gameserver.questEngine.tablelane.SimpleCollectItemHandler;
import com.aionemu.gameserver.model.templates.quest.QuestNpc;
import com.aionemu.gameserver.services.QuestService;
import com.aionemu.gameserver.utils.PacketSendUtility;
import java.util.ArrayList;
import java.util.List;

/**
 * 任务交互物 AI：玩家使用后触发任务相关逻辑。
 * Quest interaction-item AI that runs quest logic when a player uses the object.
 * @author Rinzler (Encom)
 */
@AIName("quest_use_item")
public class QuestItemNpcAI2 extends ActionItemNpcAI2
{
	private List<Player> registeredPlayers = new ArrayList<>();

	/**
	 * 玩家开始与本 NPC 对话/交互。
	 * Player starts dialog/interaction with this NPC.
	 * @param player 玩家 / player
	 */
	@Override
	protected void handleDialogStart(Player player) {
		QuestEngine questEngine = GameEngineServices.questEngine();
		boolean actionAllowed = questEngine.onCanAct(new QuestEnv(getOwner(), player, 0, 0),
			getObjectTemplate().getTemplateId(), QuestActionType.ACTION_ITEM_USE);
		QuestNpc questNpc = questEngine.getQuestNpc(getObjectTemplate().getTemplateId());
		if (!canStartInteraction(actionAllowed, !questNpc.getOnTalkEvent().isEmpty(),
				!questNpc.getOnQuestStart().isEmpty())) {
			return;
		}
		RetailPatternAI2.runQuestItemTalkedByUser(getOwner(), player);
		super.handleDialogStart(player);
	}

	static boolean canStartInteraction(boolean actionAllowed, boolean hasTalkRoute, boolean hasQuestStartRoute) {
		return actionAllowed || hasTalkRoute || hasQuestStartRoute;
	}

	static List<Integer> dialogIds() {
		return List.of(QuestDialogAction.USE_OBJECT.id(), QuestDialogAction.QUEST_SELECT.id());
	}

	/**
	 * 使用交互物完成时的逻辑。
	 * Logic when action-item use finishes.
	 * @param player 玩家 / player
	 */
	@Override
	protected void handleUseItemFinish(Player player) {
		SelectDialogResult dialogResult = null;
		for (int dialogId : dialogIds()) {
			dialogResult = AI2Actions.selectDialog(this, player, 0, dialogId);
			if (dialogResult.isSuccess()) {
				break;
			}
		}
		if (dialogResult == null || !dialogResult.isSuccess()) {
			switch (failedInteractionReply(isDialogNpc(),
					!SimpleCollectItemHandler.instance().targetsForNpc(getNpcId()).isEmpty()
						|| DataDrivenNativeRuntime.instance().isCollectObject(getNpcId()))) {
				case START_DIALOG -> PacketSendUtility.sendPacket(player,
					new SM_DIALOG_WINDOW(getObjectId(), QuestDialogPage.SELECT1.id()));
				case REMIND_UNFINISHED_QUEST -> PacketSendUtility.sendPacket(player,
					SM_SYSTEM_MESSAGE.STR_CANNOT_MOVE_TO_AIRPORT_NEED_FINISH_QUEST);
				case SILENT_COLLECT -> {
				}
			}
			return;
		}
		QuestEnv questEnv = dialogResult.getEnv();
		if (QuestService.getQuestDrop(getNpcId()).isEmpty()) {
			return;
		} if (registeredPlayers.isEmpty()) {
			AI2Actions.scheduleRespawn(this);
			if (player.isInGroup2()) {
				registeredPlayers = QuestService.getEachDropMembersGroup(player.getPlayerGroup2(), getNpcId(), questEnv.getQuestId());
				if (registeredPlayers.isEmpty()) {
					registeredPlayers.add(player);
				}
			} else if (player.isInAlliance2()) {
				registeredPlayers = QuestService.getEachDropMembersAlliance(player.getPlayerAlliance2(), getNpcId(), questEnv.getQuestId());
				if (registeredPlayers.isEmpty()) {
					registeredPlayers.add(player);
				}
			} else {
				registeredPlayers.add(player);
			}
			AI2Actions.registerDrop(this, player, registeredPlayers);
			GameCoreGameplayServices.dropService().requestDropList(player, getObjectId());
		} else if (registeredPlayers.contains(player)) {
			GameCoreGameplayServices.dropService().requestDropList(player, getObjectId());
		}
	}

	/**
	 * 失败交互（任务引擎未认领任何动作）的应答形态。
	 * The reply shape of a finished interaction that no lane claimed.
	 */
	enum FailedInteractionReply {
		/** 对话物件：开首对话页（既有行为）。 / A dialog npc: open the start dialog page (existing behavior). */
		START_DIALOG,
		/** 采集对象：零发包（真端超杀/条件不满足零副作用口径，QE-137）。 /
		 * A collect object: zero packets (the retail zero-side-effect shape, QE-137). */
		SILENT_COLLECT,
		/** 其余任务物件：任务未完成提醒。 / Any other quest object: the unfinished-quest reminder. */
		REMIND_UNFINISHED_QUEST
	}

	/**
	 * 按物件类别分类失败交互的应答（USE_OBJECT / QUEST_SELECT 全部未被任务引擎认领时）。
	 * <p>
	 * 采集族物件保持真端零包口径（QE-137：条件不满足零副作用、不补发任何对话窗）；其余任务物件在
	 * 任务条件不满足（未接取/步骤不符）时给系统消息提醒而不是静默——形态同 {@code CM_USE_ITEM} 的
	 * 欧比斯入场拦截（{@code meetsAbyssEntryRequirement} 失败 → {@code STR_MSG_CANNOT_TELEPORT_TO_ABYSS}）。
	 * <p>
	 * Classifies the reply of an interaction that every lane left unclaimed. Collect objects keep the
	 * retail zero-packet shape (QE-137: no side effects when the condition is not met); other quest
	 * objects get a system-message reminder instead of staying silent, mirroring the Abyss-entry
	 * interception in {@code CM_USE_ITEM}.
	 * @param dialogNpc 是否为对话物件 / whether the object is a dialog npc
	 * @param collectObject 是否为采集对象 / whether the object is a collect object
	 * @return 应答形态 / the reply shape
	 */
	static FailedInteractionReply failedInteractionReply(boolean dialogNpc, boolean collectObject) {
		if (dialogNpc) {
			return FailedInteractionReply.START_DIALOG;
		}
		return collectObject ? FailedInteractionReply.SILENT_COLLECT
			: FailedInteractionReply.REMIND_UNFINISHED_QUEST;
	}

	private boolean isDialogNpc() {
		return getObjectTemplate().isDialogNpc();
	}

	/**
	 * 处理消失事件。
	 * Handle despawn.
	 */
	@Override
	protected void handleDespawned() {
		super.handleDespawned();
		registeredPlayers.clear();
	}

	/**
	 * 处理看见生物事件。
	 * Handle seeing a creature.
	 * @param creature 生物 / creature
	 */
	@Override
	protected void handleCreatureSee(Creature creature) {
		CreatureEventHandler.onCreatureSee(this, creature);
	}

	/**
	 * 处理生物移动事件。
	 * Handle creature-moved.
	 * @param creature 移动的生物 / moved creature
	 */
	@Override
	protected void handleCreatureMoved(Creature creature) {
		CreatureEventHandler.onCreatureMoved(this, creature);
	}
}
