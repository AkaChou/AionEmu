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
import com.aionemu.gameserver.questEngine.QuestEngine;
import com.aionemu.gameserver.questEngine.model.QuestActionType;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.services.QuestService;
import com.aionemu.gameserver.services.drop.DropService;
import com.aionemu.gameserver.utils.PacketSendUtility;
import java.util.ArrayList;
import java.util.List;

/**
 * 任务交互物 AI：玩家使用后触发任务相关逻辑。
 * Quest interaction-item AI that runs quest logic when a player uses the object.
 *
 * @author Rinzler (Encom)
 */
@AIName("quest_use_item")
public class QuestItemNpcAI2 extends ActionItemNpcAI2
{
	private List<Player> registeredPlayers = new ArrayList<Player>();
	
	/**
	 * 玩家开始与本 NPC 对话/交互。
	 * Player starts dialog/interaction with this NPC.
	 *
	 * 玩家 / player
	 */
	@Override
	protected void handleDialogStart(Player player) {
		if (!(GameEngineServices.questEngine().onCanAct(new QuestEnv(getOwner(), player, 0, 0),
			getObjectTemplate().getTemplateId(), QuestActionType.ACTION_ITEM_USE))) {
			return;
		}
		super.handleDialogStart(player);
	}
	
	/**
	 * 使用交互物完成时的逻辑。
	 * Logic when action-item use finishes.
	 *
	 * @param player 玩家 / player
	 */
	@Override
	protected void handleUseItemFinish(Player player) {
		SelectDialogResult dialogResult = AI2Actions.selectDialog(this, player, 0, -1);
		if (!dialogResult.isSuccess()) {
			if (isDialogNpc()) {
				PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(), QuestDialog.SELECT_ACTION_1011.id()));
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
	 *
	 * creature
	 */
	@Override
	protected void handleCreatureSee(Creature creature) {
		CreatureEventHandler.onCreatureSee(this, creature);
	}
	
	/**
	 * 处理生物移动事件。
	 * Handle creature-moved.
	 *
	 * creature
	 */
	@Override
	protected void handleCreatureMoved(Creature creature) {
		CreatureEventHandler.onCreatureMoved(this, creature);
	}
}
