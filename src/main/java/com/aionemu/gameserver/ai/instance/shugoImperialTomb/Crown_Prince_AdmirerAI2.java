package com.aionemu.gameserver.ai.instance.shugoImperialTomb;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import com.aionemu.commons.network.util.ThreadPoolManager;
import com.aionemu.gameserver.ai2.*;
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
 *
 * @author Encom
 */
@AIName("Crown_Prince_Admirer")
public class Crown_Prince_AdmirerAI2 extends NpcAI2
{
	private boolean isInstanceDestroyed;
	
	@Override
	protected void handleDialogStart(Player player) {
		PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(), 1011));
	}
	
	@Override
	public boolean onDialogSelect(Player player, int dialogId, int questId, int extendedRewardIndex) {
		if (dialogId == 10000) {
			switch (getNpcId()) {
				case 831110: //Crown Prince's Admirer.
					startTombWaveA1();
					despawnNpc(831095); //Shugo Warrior Transformation Device.
					// 掠夺者来袭。守卫皇太子纪念碑！ / Pillagers incoming. Guard the Crown Prince's Monument!
					PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(1401582));
					spawn(831130, 174.11159f, 226.21033f, 536.16974f, (byte) 20); //Crown Prince's Monument.
					spawn(831250, 184.92825f, 229.44f, 536.16974f, (byte) 54); //Imperial Obelisk.
					spawn(831251, 170.58969f, 237.13005f, 536.16974f, (byte) 108); //Imperial Obelisk.
				break;
			}
		}
		PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(), 0));
		AI2Actions.deleteOwner(this);
		return true;
	}
	
	private void attackEvent(final Npc npc, float x, float y, float z, boolean despawn) {
		((AbstractAI) npc.getAi2()).setStateIfNot(AIState.WALKING);
		npc.setState(1);
		npc.getMoveController().moveToPoint(x, y, z);
		PacketSendUtility.broadcastPacket(npc, new SM_EMOTION(npc, EmotionType.START_EMOTE2, 0, npc.getObjectId()));
	}
	
	private void despawnNpc(int npcId) {
		if (getPosition().getWorldMapInstance().getNpcs(npcId) != null) {
			List<Npc> npcs = getPosition().getWorldMapInstance().getNpcs(npcId);
			for (Npc npc: npcs) {
				npc.getController().onDelete();
			}
		}
	}
	
	private void startTombWaveA1() {
		GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			@Override
			public void run() {
				attackEvent((Npc)spawn(219508, 201.46498f, 279.1183f, 550.49426f, (byte) 78), 167.5225f, 239.59024f, 535.81213f, false);
				attackEvent((Npc)spawn(219508, 220.23207f, 268.67975f, 550.4942f, (byte) 78), 189.45801f, 229.36856f, 535.81213f, false);
			}
		}, 10000);
		GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			@Override
			public void run() {
				attackEvent((Npc)spawn(219509, 201.46498f, 279.1183f, 550.49426f, (byte) 78), 167.5225f, 239.59024f, 535.81213f, false);
				attackEvent((Npc)spawn(219509, 220.23207f, 268.67975f, 550.4942f, (byte) 78), 189.45801f, 229.36856f, 535.81213f, false);
			}
		}, 30000);
		GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			@Override
			public void run() {
				attackEvent((Npc)spawn(219510, 201.46498f, 279.1183f, 550.49426f, (byte) 78), 167.5225f, 239.59024f, 535.81213f, false);
				attackEvent((Npc)spawn(219510, 220.23207f, 268.67975f, 550.4942f, (byte) 78), 189.45801f, 229.36856f, 535.81213f, false);
			}
		}, 50000);
		GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			@Override
			public void run() {
				attackEvent((Npc)spawn(219508, 201.46498f, 279.1183f, 550.49426f, (byte) 78), 167.5225f, 239.59024f, 535.81213f, false);
				attackEvent((Npc)spawn(219508, 220.23207f, 268.67975f, 550.4942f, (byte) 78), 189.45801f, 229.36856f, 535.81213f, false);
			}
		}, 70000);
		GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			@Override
			public void run() {
				attackEvent((Npc)spawn(219509, 201.46498f, 279.1183f, 550.49426f, (byte) 78), 167.5225f, 239.59024f, 535.81213f, false);
				attackEvent((Npc)spawn(219509, 220.23207f, 268.67975f, 550.4942f, (byte) 78), 189.45801f, 229.36856f, 535.81213f, false);
			}
		}, 90000);
		GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			@Override
			public void run() {
				attackEvent((Npc)spawn(219510, 201.46498f, 279.1183f, 550.49426f, (byte) 78), 167.5225f, 239.59024f, 535.81213f, false);
				attackEvent((Npc)spawn(219510, 220.23207f, 268.67975f, 550.4942f, (byte) 78), 189.45801f, 229.36856f, 535.81213f, false);
			}
		}, 110000);
		GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			@Override
			public void run() {
				attackEvent((Npc)spawn(219508, 201.46498f, 279.1183f, 550.49426f, (byte) 78), 167.5225f, 239.59024f, 535.81213f, false);
				attackEvent((Npc)spawn(219508, 220.23207f, 268.67975f, 550.4942f, (byte) 78), 189.45801f, 229.36856f, 535.81213f, false);
			}
		}, 130000);
	}
	
	public void onInstanceDestroy() {
		isInstanceDestroyed = true;
	}
	
	@Override
	public boolean isMoveSupported() {
		return false;
	}
}
