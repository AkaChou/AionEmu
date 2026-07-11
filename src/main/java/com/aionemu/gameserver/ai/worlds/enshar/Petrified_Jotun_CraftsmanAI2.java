package com.aionemu.gameserver.ai.worlds.enshar;

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
 * Enshar 区域 NPC AI：Petrified Jotun Craftsman（@AIName "nepilim_2"），继承 AggressiveNpcAI2。
 * Enshar zone NPC AI: Petrified Jotun Craftsman (@AIName "nepilim_2"), extends AggressiveNpcAI2.
 *
 * @author Encom
 */
@AIName("nepilim_2")
public class Petrified_Jotun_CraftsmanAI2 extends AggressiveNpcAI2
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
					spawn(219777, getOwner().getX(), getOwner().getY(), getOwner().getZ(), (byte) getOwner().getHeading());
					AI2Actions.deleteOwner(Petrified_Jotun_CraftsmanAI2.this);
					AI2Actions.scheduleRespawn(this);
					GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
					    @Override
					    public void run() {
						    despawnNpc(219777);
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
