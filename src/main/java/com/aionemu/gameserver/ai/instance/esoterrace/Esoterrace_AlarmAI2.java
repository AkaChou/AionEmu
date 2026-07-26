package com.aionemu.gameserver.ai.instance.esoterrace;

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
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.services.NpcShoutsService;
import com.aionemu.gameserver.utils.MathUtil;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.knownlist.Visitor;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Esoterrace 副本 NPC AI：Esoterrace Alarm（@AIName "esoterracealarm"），继承 AggressiveNpcAI2。
 * Esoterrace instance NPC AI: Esoterrace Alarm (@AIName "esoterracealarm"), extends AggressiveNpcAI2.
 *
 * @author Encom
 */
@AIName("esoterracealarm")
public class Esoterrace_AlarmAI2 extends AggressiveNpcAI2
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
			if (MathUtil.getDistance(getOwner(), player) <= 13) {
				if (startedEvent.compareAndSet(false, true)) {
					canThink = false;
					// 入侵警报。入侵警报。封锁所有关键系统。 / INTRUDER ALERT. INTRUDER ALERT. SEAL OFF ALL VITAL SYSTEMS.
					sendMsg(1500379, getObjectId(), false, 0);
					// 入侵警报。入侵警报。封锁所有关键系统。 / INTRUDER ALERT. INTRUDER ALERT. SEAL OFF ALL VITAL SYSTEMS.
					sendMsg(1500379, getObjectId(), false, 5000);
					// 入侵警报。入侵警报。封锁所有关键系统。 / INTRUDER ALERT. INTRUDER ALERT. SEAL OFF ALL VITAL SYSTEMS.
					sendMsg(1500379, getObjectId(), false, 10000);
					getSpawnTemplate().setWalkerId("3002500003");
					WalkManager.startWalking(this);
					getOwner().setState(1);
					PacketSendUtility.broadcastPacket(getOwner(), new SM_EMOTION(getOwner(), EmotionType.START_EMOTE2, 0, getObjectId()));
					GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
						@Override
						public void run() {
							if (!isAlreadyDead()) {
								despawn();
								announceBridgeRaised();
								getPosition().getWorldMapInstance().getInstanceHandler().setDoorState(69, true);
								getPosition().getWorldMapInstance().getInstanceHandler().setDoorState(367, true);
							}
						}
					}, 12000);
				}
			}
		}
	}
	
	private void announceBridgeRaised() {
		getPosition().getWorldMapInstance().doOnAllPlayers(new Visitor<Player>() {
			@Override
			public void visit(Player player) {
				if (player.isOnline()) {
					// 通往德拉纳生产实验室的桥已升起。 / The Bridge to the Drana Production Lab has been raised.
					PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_IDF4Re_Drana_01);
				}
			}
		});
	}
	
	private void despawn() {
		AI2Actions.deleteOwner(this);
	}
	
	private void sendMsg(int msg, int Obj, boolean isShout, int time) {
		GameFeatureServices.npcShoutsService().sendMsg(getPosition().getWorldMapInstance(), msg, Obj, isShout, 0, time);
	}
}
