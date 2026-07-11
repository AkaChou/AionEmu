package com.aionemu.gameserver.ai.worlds.cygnea;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import com.aionemu.gameserver.ai.AggressiveNpcAI2;
import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.utils.MathUtil;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Cygnea 区域 NPC AI：Stygian Mist Warrior（@AIName "lizardman_1"），继承 AggressiveNpcAI2。
 * Cygnea zone NPC AI: Stygian Mist Warrior (@AIName "lizardman_1"), extends AggressiveNpcAI2.
 *
 * @author Encom
 */
@AIName("lizardman_1")
public class Stygian_Mist_WarriorAI2 extends AggressiveNpcAI2
{
	@Override
	public void think() {
	}
	
	private AtomicBoolean startedEvent = new AtomicBoolean(false);
	
	@Override
	protected void handleCreatureMoved(Creature creature) {
		if (creature instanceof Player) {
			final Player player = (Player) creature;
			if (MathUtil.getDistance(getOwner(), player) <= 10) {
				if (startedEvent.compareAndSet(false, true)) {
					spawn(235912, getOwner().getX(), getOwner().getY(), getOwner().getZ(), (byte) getOwner().getHeading());
					AI2Actions.deleteOwner(Stygian_Mist_WarriorAI2.this);
					AI2Actions.scheduleRespawn(this);
					GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
					    @Override
					    public void run() {
						    despawnNpc(235912);
				        }
			        }, 300000); //5 分钟。 / 5 Minutes.
				}
			}
		}
	}
	
	@Override
	public boolean isMoveSupported() {
		return false;
	}
	
	private void despawnNpc(int npcId) {
		if (getPosition().getWorldMapInstance().getNpcs(npcId) != null) {
			List<Npc> npcs = getPosition().getWorldMapInstance().getNpcs(npcId);
			for (Npc npc: npcs) {
				npc.getController().onDelete();
			}
		}
	}
}
