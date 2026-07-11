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

/**
 * Shugo Imperial Tomb 副本 NPC AI：Emperor Admirer（@AIName "Emperor_Admirer"），继承 NpcAI2。
 * Shugo Imperial Tomb instance NPC AI: Emperor Admirer (@AIName "Emperor_Admirer"), extends NpcAI2.
 *
 * @author Encom
 */
@AIName("Emperor_Admirer")
public class Emperor_AdmirerAI2 extends NpcAI2
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
				case 831112: //Emperor's Admirer.
					startTombWaveC1();
					// 掠夺者来袭。守卫皇帝纪念碑！ / Pillagers incoming. Guard the Emperor's Monument!
					PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(1401584));
					spawn(831130, 452.43765f, 106.14462f, 212.20023f, (byte) 68); //Crown Prince's Monument.
					spawn(831250, 452.92874f, 85.73192f, 214.3359f, (byte) 75); //Imperial Obelisk.
					spawn(831251, 435.34854f, 120.64044f, 214.336f, (byte) 62); //Imperial Obelisk.
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
	
	private void startTombWaveC1() {
		GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			@Override
			public void run() {
				attackEvent((Npc)spawn(219521, 425.89014f, 58.281246f, 222.14124f, (byte) 15), 451.03403f, 84.020836f, 214.33578f, false);
				attackEvent((Npc)spawn(219521, 419.153f, 91.08885f, 214.33856f, (byte) 9), 449.21487f, 104.769394f, 212.20023f, false);
				attackEvent((Npc)spawn(219521, 395.60565f, 117.61483f, 222.1441f, (byte) 1), 433.06375f, 120.66558f, 214.33475f, false);
			}
		}, 10000);
		GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			@Override
			public void run() {
				attackEvent((Npc)spawn(219522, 425.89014f, 58.281246f, 222.14124f, (byte) 15), 451.03403f, 84.020836f, 214.33578f, false);
				attackEvent((Npc)spawn(219522, 419.153f, 91.08885f, 214.33856f, (byte) 9), 449.21487f, 104.769394f, 212.20023f, false);
				attackEvent((Npc)spawn(219522, 395.60565f, 117.61483f, 222.1441f, (byte) 1), 433.06375f, 120.66558f, 214.33475f, false);
			}
		}, 30000);
		GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			@Override
			public void run() {
				attackEvent((Npc)spawn(219523, 425.89014f, 58.281246f, 222.14124f, (byte) 15), 451.03403f, 84.020836f, 214.33578f, false);
				attackEvent((Npc)spawn(219523, 419.153f, 91.08885f, 214.33856f, (byte) 9), 449.21487f, 104.769394f, 212.20023f, false);
				attackEvent((Npc)spawn(219523, 395.60565f, 117.61483f, 222.1441f, (byte) 1), 433.06375f, 120.66558f, 214.33475f, false);
			}
		}, 50000);
		GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			@Override
			public void run() {
				attackEvent((Npc)spawn(219521, 425.89014f, 58.281246f, 222.14124f, (byte) 15), 451.03403f, 84.020836f, 214.33578f, false);
				attackEvent((Npc)spawn(219521, 419.153f, 91.08885f, 214.33856f, (byte) 9), 449.21487f, 104.769394f, 212.20023f, false);
				attackEvent((Npc)spawn(219521, 395.60565f, 117.61483f, 222.1441f, (byte) 1), 433.06375f, 120.66558f, 214.33475f, false);
			}
		}, 70000);
		GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			@Override
			public void run() {
				attackEvent((Npc)spawn(219522, 425.89014f, 58.281246f, 222.14124f, (byte) 15), 451.03403f, 84.020836f, 214.33578f, false);
				attackEvent((Npc)spawn(219522, 419.153f, 91.08885f, 214.33856f, (byte) 9), 449.21487f, 104.769394f, 212.20023f, false);
				attackEvent((Npc)spawn(219522, 395.60565f, 117.61483f, 222.1441f, (byte) 1), 433.06375f, 120.66558f, 214.33475f, false);
			}
		}, 90000);
		GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			@Override
			public void run() {
				attackEvent((Npc)spawn(219523, 425.89014f, 58.281246f, 222.14124f, (byte) 15), 451.03403f, 84.020836f, 214.33578f, false);
				attackEvent((Npc)spawn(219523, 419.153f, 91.08885f, 214.33856f, (byte) 9), 449.21487f, 104.769394f, 212.20023f, false);
				attackEvent((Npc)spawn(219523, 395.60565f, 117.61483f, 222.1441f, (byte) 1), 433.06375f, 120.66558f, 214.33475f, false);
			}
		}, 110000);
	}
	
	public void onInstanceDestroy() {
		isInstanceDestroyed = true;
	}
	
	@Override
	public boolean isMoveSupported() {
		return false;
	}
}
