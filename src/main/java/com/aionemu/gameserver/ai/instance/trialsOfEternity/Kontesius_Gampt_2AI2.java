package com.aionemu.gameserver.ai.instance.trialsOfEternity;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import com.aionemu.gameserver.ai.AggressiveNpcAI2;
import com.aionemu.commons.network.util.ThreadPoolManager;
import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.ai2.manager.WalkManager;
import com.aionemu.gameserver.model.EmotionType;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_EMOTION;
import com.aionemu.gameserver.utils.MathUtil;
import com.aionemu.gameserver.utils.PacketSendUtility;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Trials Of Eternity 副本 NPC AI：Kontesius Gampt 2（@AIName "IDEternity_03_Event_Guard_02"），继承 AggressiveNpcAI2。
 * Trials Of Eternity instance NPC AI: Kontesius Gampt 2 (@AIName "IDEternity_03_Event_Guard_02"), extends AggressiveNpcAI2.
 *
 * @author Encom
 */
@AIName("IDEternity_03_Event_Guard_02")
public class Kontesius_Gampt_2AI2 extends AggressiveNpcAI2
{
	private boolean canThink = true;
	private AtomicBoolean startedEvent = new AtomicBoolean(false);
	
	@Override
	public boolean canThink() {
		return canThink;
	}
	
	/**
	 * 玩家靠近 10 米内时触发一次散步表演：沿巡逻路径行走并在 9.5 秒后消失。
	 * When a player comes within 10 meters, triggers a one-time walking show: walks the patrol path and despawns after 9.5 seconds.
	 */
	@Override
	protected void handleCreatureMoved(Creature creature) {
		if (creature instanceof Player) {
			final Player player = (Player) creature;
			if (MathUtil.getDistance(getOwner(), player) <= 10) {
				if (startedEvent.compareAndSet(false, true)) {
					canThink = false;
					getSpawnTemplate().setWalkerId("3015600002");
					WalkManager.startWalking(this);
					getOwner().setState(1);
					PacketSendUtility.broadcastPacket(getOwner(), new SM_EMOTION(getOwner(), EmotionType.START_EMOTE2, 0, getObjectId()));
					GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
						@Override
						public void run() {
							if (!isAlreadyDead()) {
								despawn();
							}
						}
					}, 9500);
				}
			}
		}
	}
	
	private void despawn() {
		AI2Actions.deleteOwner(this);
	}
}
