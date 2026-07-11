package com.aionemu.gameserver.ai.instance.rentusBase;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import com.aionemu.gameserver.ai.AggressiveNpcAI2;
import com.aionemu.commons.network.util.ThreadPoolManager;
import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.utils.MathUtil;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Rentus Base 副本 NPC AI：Broken Barrel（@AIName "broken_barrel"），继承 AggressiveNpcAI2。
 * Rentus Base instance NPC AI: Broken Barrel (@AIName "broken_barrel"), extends AggressiveNpcAI2.
 *
 * @author Encom
 */
@AIName("broken_barrel")
public class Broken_BarrelAI2 extends AggressiveNpcAI2
{
	private AtomicBoolean startedEvent = new AtomicBoolean(false);
	
	@Override
	public void think() {
	}
	
	@Override
	protected void handleCreatureMoved(Creature creature) {
		if (creature instanceof Player) {
			final Player player = (Player) creature;
			if (MathUtil.getDistance(getOwner(), player) <= 15) {
				if (startedEvent.compareAndSet(false, true)) {
					getPosition().getWorldMapInstance().getDoors().get(54).setOpen(true);
					GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
					    @Override
					    public void run() {
						    spawn(282626, 167.56618f, 341.45828f, 207.60175f, (byte) 0, 229);
				        }
			        }, 5000);
					AI2Actions.deleteOwner(Broken_BarrelAI2.this);
				}
			}
		}
	}
	
	@Override
	public boolean isMoveSupported() {
		return false;
	}
}
