package com.aionemu.gameserver.ai.worlds.iluma;

import com.aionemu.gameserver.lifecycle.GameFeatureServices;

import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.ai2.NpcAI2;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.services.NpcShoutsService;
import com.aionemu.gameserver.utils.MathUtil;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Iluma 区域 NPC AI：Carniverous Vines（@AIName "carniverous_vines"），继承 NpcAI2。
 * Iluma zone NPC AI: Carniverous Vines (@AIName "carniverous_vines"), extends NpcAI2.
 *
 * @author Encom
 */
@AIName("carniverous_vines")
public class Carniverous_VinesAI2 extends NpcAI2
{
	private AtomicBoolean startedEvent = new AtomicBoolean(false);
	
	@Override
	public void think() {
	}
	
	@Override
	protected void handleCreatureMoved(Creature creature) {
		if (creature instanceof Player) {
			final Player player = (Player) creature;
			if (MathUtil.getDistance(getOwner(), player) <= 10) {
				if (startedEvent.compareAndSet(false, true)) {
					// 谢谢你救了我！ / Thank you for saving me!
					sendMsg(1501531, getObjectId(), false, 0);
				}
			}
		}
	}
	
	@Override
	protected void handleDied() {
		spawn(806236, getOwner().getX(), getOwner().getY(), getOwner().getZ(), (byte) 0); //Tunes Of Splendor Scout.
		super.handleDied();
		AI2Actions.deleteOwner(this);
		AI2Actions.scheduleRespawn(this);
	}
	
	@Override
	public boolean isMoveSupported() {
		return false;
	}
	
	private void sendMsg(int msg, int Obj, boolean isShout, int time) {
		GameFeatureServices.npcShoutsService().sendMsg(getPosition().getWorldMapInstance(), msg, Obj, isShout, 0, time);
	}
}
