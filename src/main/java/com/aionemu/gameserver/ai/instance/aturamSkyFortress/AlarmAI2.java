package com.aionemu.gameserver.ai.instance.aturamSkyFortress;

import com.aionemu.gameserver.lifecycle.GameFeatureServices;

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
import com.aionemu.gameserver.services.NpcShoutsService;
import com.aionemu.gameserver.utils.MathUtil;
import com.aionemu.gameserver.utils.PacketSendUtility;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Aturam Sky Fortress 副本 NPC AI：Alarm（@AIName "alarm"），继承 AggressiveNpcAI2。
 * Aturam Sky Fortress instance NPC AI: Alarm (@AIName "alarm"), extends AggressiveNpcAI2.
 *
 * @author Encom
 */
@AIName("alarm")
public class AlarmAI2 extends AggressiveNpcAI2
{
	private boolean canThink = true;
	private AtomicBoolean startedEvent = new AtomicBoolean(false);
	
	@Override
	public boolean canThink() {
		return canThink;
	}
	
	@Override
	protected void handleCreatureMoved(Creature creature) {
		if (creature instanceof Player) {
			final Player player = (Player) creature;
			if (MathUtil.getDistance(getOwner(), player) <= 15) {
				if (startedEvent.compareAndSet(false, true)) {
					canThink = false;
					GameFeatureServices.npcShoutsService().sendMsg(getOwner(), 1500379, getObjectId(), 0, 0);
					GameFeatureServices.npcShoutsService().sendMsg(getOwner(), 1401350, 0);
					getSpawnTemplate().setWalkerId("3002400002");
					WalkManager.startWalking(this);
					getOwner().setState(1);
					PacketSendUtility.broadcastPacket(getOwner(), new SM_EMOTION(getOwner(), EmotionType.START_EMOTE2, 0, getObjectId()));
					getPosition().getWorldMapInstance().getDoors().get(128).setOpen(true);
					getPosition().getWorldMapInstance().getDoors().get(138).setOpen(true);
					GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
						@Override
						public void run() {
							if (!isAlreadyDead()) {
								despawn();
							}
						}
					}, 3000);
				}
			}
		}
	}
	
	private void despawn() {
		AI2Actions.deleteOwner(this);
	}
}
