package com.aionemu.gameserver.ai2.handler;

import com.aionemu.gameserver.lifecycle.GameHousingServices;

import com.aionemu.gameserver.lifecycle.GameEngineServices;

import com.aionemu.gameserver.ai2.AIState;
import com.aionemu.gameserver.ai2.AISubState;
import com.aionemu.gameserver.ai2.NpcAI2;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_DIALOG_WINDOW;
import com.aionemu.gameserver.questEngine.QuestEngine;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.services.TownService;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * 对话事件处理器，负责 NPC 与玩家的对话、城镇校验及结束对话清理。
 * Handles talk events: NPC-player dialogs, town residence checks, and finish-talk cleanup.
 */
public class TalkEventHandler {

	/**
	 * 处理完整对话：进入对话子状态，处理任务对话框，并按标题 / 城镇发送对话窗口。
	 * town. / town.
	 *
	 * NPC AI instance
	 * talking creature
	 */
	public static void onTalk(NpcAI2 npcAI, Creature creature) {
		onSimpleTalk(npcAI, creature);
		if (creature instanceof Player) {
			Player player = (Player) creature;
			if (GameEngineServices.questEngine().onDialog(new QuestEnv(npcAI.getOwner(), player, 0, -1))) {
				return;
			}
			switch (npcAI.getOwner().getObjectTemplate().getTitleId()) {
			case 462877: // Village Trade Broker.
			case 462878: // Village Guestbloom.
				// case 462881: // 村庄任务公告板。 / Village Quest Board.
				// 奥利尔。 / Oriel.
			case 730677:
			case 831198:
			case 831199:
			case 831200:
			case 831201:
			case 831202:
			case 831203:
			case 831204:
			case 831205:
			case 831206:
			case 831207:
			case 831208:
			case 831209:
			case 831211:
			case 831212:
				// 佩尔农。 / Pernon.
			case 730679:
			case 831223:
			case 831224:
			case 831225:
			case 831226:
			case 831227:
			case 831228:
			case 831229:
			case 831230:
			case 831231:
			case 831232:
			case 831233:
			case 831234:
			case 831236:
			case 831237:
				int playerTownId = GameHousingServices.townService().getTownResidence(player);
				int currentTownId = GameHousingServices.townService().getTownIdByPosition(player);
				if (playerTownId != currentTownId) {
					PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(npcAI.getOwner().getObjectId(), 44));
					return;
				} else {
					PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(npcAI.getOwner().getObjectId(), 10));
					return;
				}
			default:
				PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(npcAI.getOwner().getObjectId(), 10));
				break;
			}
		}
	}

	/**
	 * 简化对话：对话框 NPC 进入 TALK 子状态并设定目标。
	 * Simple talk: dialog NPCs enter TALK sub-state and set the target.
	 *
	 * NPC AI instance
	 * talking creature
	 */
	public static void onSimpleTalk(NpcAI2 npcAI, Creature creature) {
		if (npcAI.getOwner().getObjectTemplate().isDialogNpc()) {
			npcAI.setSubStateIfNot(AISubState.TALK);
			npcAI.getOwner().setTarget(creature);
		}
	}

	/**
	 * 结束对话：清空目标（非跟随状态）并触发思考。
	 * Finishes talk: clears target (unless following) and triggers think.
	 *
	 * NPC AI instance
	 * @param creature 结束对话的对象 / creature finishing talk
	 */
	public static void onFinishTalk(NpcAI2 npcAI, Creature creature) {
		Npc owner = npcAI.getOwner();
		if (owner.isTargeting(creature.getObjectId())) {
			if (npcAI.getState() != AIState.FOLLOWING) {
				owner.setTarget(null);
			}
			npcAI.think();
		}
	}

	/**
	 * 简化结束对话：清除 TALK 子状态并清空目标。
	 * Simple finish talk: clears TALK sub-state and target.
	 *
	 * NPC AI instance
	 * @param creature 结束对话的对象 / creature finishing talk
	 */
	public static void onSimpleFinishTalk(NpcAI2 npcAI, Creature creature) {
		Npc owner = npcAI.getOwner();
		if (owner.isTargeting(creature.getObjectId()) && npcAI.setSubStateIfNot(AISubState.NONE)) {
			owner.setTarget(null);
		}
	}
}
