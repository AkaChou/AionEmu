package com.aionemu.gameserver.ai.instance.shugoImperialTomb;


import com.aionemu.boot.i18n.I18n;
import com.aionemu.gameserver.ai2.AbstractAI;
import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.ai2.AIState;
import com.aionemu.gameserver.ai2.NpcAI2;
import com.aionemu.gameserver.instance.handlers.scripts.ShugoImperialTombInstance;
import com.aionemu.gameserver.model.EmotionType;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_DIALOG_WINDOW;
import com.aionemu.gameserver.network.aion.serverpackets.SM_EMOTION;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.utils.PacketSendUtility;

import java.util.List;

/**
 * Shugo Imperial Tomb 副本 NPC AI：Crown Prince Admirer（@AIName "Crown_Prince_Admirer"），继承 NpcAI2。
 * Shugo Imperial Tomb instance NPC AI: Crown Prince Admirer (@AIName "Crown_Prince_Admirer"), extends NpcAI2.
 * 波次任务由副本持有，开战删除本 NPC 不会取消波次，销毁副本则会取消。
 * Wave tasks belong to the instance: deleting this NPC starts the battle, while instance teardown cancels its waves.
 * @author Encom
 */
@AIName("Crown_Prince_Admirer")
public class Crown_Prince_AdmirerAI2 extends NpcAI2 {
	/** 防止重复对话重复开战 / Guards against duplicate battle start. */
	private boolean tombBattleStarted;

	@Override
	protected void handleDialogStart(Player player) {
		PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(), 1011));
	}

	/**
	 * 仅在合法开战请求且参与者均已变身时启动一次战斗；拒绝时保留入口和变身装置。
	 * Starts one battle only for a valid request with transformed participants; rejection preserves the entry and device.
	 */
	@Override
	public synchronized boolean onDialogSelect(Player player, int dialogId, int questId, int extendedRewardIndex) {
		if (dialogId != 10000 || getNpcId() != 831110 || player == null) {
			sendDialogClose(player);
			return true;
		}
		if (!canStartBattle(player)) {
			PacketSendUtility.sendMessage(player, I18n.get("instance.shugo_tomb.transform_required"));
			sendDialogClose(player);
			return true;
		}
		if (!tombBattleStarted) {
			tombBattleStarted = true;
			startTombWaveA1();
			despawnNpc(831095); //Shugo Warrior Transformation Device.
			// 掠夺者来袭。守卫皇太子纪念碑！ / Pillagers incoming. Guard the Crown Prince's Monument!
			PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(1401582));
			spawn(831130, 174.11159f, 226.21033f, 536.16974f, (byte) 20);
			spawn(831250, 184.92825f, 229.44f, 536.16974f, (byte) 54);
			spawn(831251, 170.58969f, 237.13005f, 536.16974f, (byte) 108);
		}
		sendDialogClose(player);
		deleteOwner();
		return true;
	}

	protected boolean canStartBattle(Player player) {
		ShugoImperialTombInstance handler =
			(ShugoImperialTombInstance) getPosition().getWorldMapInstance().getInstanceHandler();
		return handler != null && handler.canStartTombBattle(player);
	}

	protected void sendDialogClose(Player player) {
		PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(), 0));
	}

	protected void deleteOwner() {
		AI2Actions.deleteOwner(this);
	}

	private void attackEvent(final Npc npc, float x, float y, float z, boolean despawn) {
		if (npc == null) {
			return;
		}
		((AbstractAI) npc.getAi2()).setStateIfNot(AIState.WALKING);
		npc.setState(1);
		npc.getMoveController().moveToPoint(x, y, z);
		PacketSendUtility.broadcastPacket(npc, new SM_EMOTION(npc, EmotionType.START_EMOTE2, 0, npc.getObjectId()));
	}

	protected void despawnNpc(int npcId) {
		if (getPosition().getWorldMapInstance().getNpcs(npcId) != null) {
			List<Npc> npcs = getPosition().getWorldMapInstance().getNpcs(npcId);
			for (Npc npc: npcs) {
				npc.getController().onDelete();
			}
		}
	}

	/**
	 * 将开场波次交由副本调度，使其在本 NPC 删除后继续、在副本销毁时取消。
	 * Schedules opening waves on the instance so they survive this NPC's deletion but not instance teardown.
	 */
	protected void startTombWaveA1() {
		ShugoImperialTombInstance handler =
			(ShugoImperialTombInstance) getPosition().getWorldMapInstance().getInstanceHandler();
		handler.scheduleTask(() -> {
			attackEvent((Npc)spawn(219508, 201.46498f, 279.1183f, 550.49426f, (byte) 78), 167.5225f, 239.59024f, 535.81213f, false);
			attackEvent((Npc)spawn(219508, 220.23207f, 268.67975f, 550.4942f, (byte) 78), 189.45801f, 229.36856f, 535.81213f, false);
		}, 10000);
		handler.scheduleTask(() -> {
			attackEvent((Npc)spawn(219509, 201.46498f, 279.1183f, 550.49426f, (byte) 78), 167.5225f, 239.59024f, 535.81213f, false);
			attackEvent((Npc)spawn(219509, 220.23207f, 268.67975f, 550.4942f, (byte) 78), 189.45801f, 229.36856f, 535.81213f, false);
		}, 30000);
		handler.scheduleTask(() -> {
			attackEvent((Npc)spawn(219510, 201.46498f, 279.1183f, 550.49426f, (byte) 78), 167.5225f, 239.59024f, 535.81213f, false);
			attackEvent((Npc)spawn(219510, 220.23207f, 268.67975f, 550.4942f, (byte) 78), 189.45801f, 229.36856f, 535.81213f, false);
		}, 50000);
		handler.scheduleTask(() -> {
			attackEvent((Npc)spawn(219508, 201.46498f, 279.1183f, 550.49426f, (byte) 78), 167.5225f, 239.59024f, 535.81213f, false);
			attackEvent((Npc)spawn(219508, 220.23207f, 268.67975f, 550.4942f, (byte) 78), 189.45801f, 229.36856f, 535.81213f, false);
		}, 70000);
		handler.scheduleTask(() -> {
			attackEvent((Npc)spawn(219509, 201.46498f, 279.1183f, 550.49426f, (byte) 78), 167.5225f, 239.59024f, 535.81213f, false);
			attackEvent((Npc)spawn(219509, 220.23207f, 268.67975f, 550.4942f, (byte) 78), 189.45801f, 229.36856f, 535.81213f, false);
		}, 90000);
		handler.scheduleTask(() -> {
			attackEvent((Npc)spawn(219510, 201.46498f, 279.1183f, 550.49426f, (byte) 78), 167.5225f, 239.59024f, 535.81213f, false);
			attackEvent((Npc)spawn(219510, 220.23207f, 268.67975f, 550.4942f, (byte) 78), 189.45801f, 229.36856f, 535.81213f, false);
		}, 110000);
		handler.scheduleTask(() -> {
			attackEvent((Npc)spawn(219508, 201.46498f, 279.1183f, 550.49426f, (byte) 78), 167.5225f, 239.59024f, 535.81213f, false);
			attackEvent((Npc)spawn(219508, 220.23207f, 268.67975f, 550.4942f, (byte) 78), 189.45801f, 229.36856f, 535.81213f, false);
		}, 130000);
	}

	@Override
	public boolean isMoveSupported() {
		return false;
	}
}
