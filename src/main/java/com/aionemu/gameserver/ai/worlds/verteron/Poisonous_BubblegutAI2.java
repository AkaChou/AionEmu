package com.aionemu.gameserver.ai.worlds.verteron;

import com.aionemu.gameserver.lifecycle.GameEngineServices;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import com.aionemu.gameserver.ai.AggressiveNpcAI2;
import com.aionemu.commons.network.util.ThreadPoolManager;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.skillengine.SkillEngine;
import com.aionemu.gameserver.world.WorldPosition;

import java.util.List;

/**
 * Verteron 区域 NPC AI：Poisonous Bubblegut（@AIName "poisonous_bubblegut"），继承 AggressiveNpcAI2。
 * Verteron zone NPC AI: Poisonous Bubblegut (@AIName "poisonous_bubblegut"), extends AggressiveNpcAI2.
 *
 * @author Encom
 */
@AIName("poisonous_bubblegut")
public class Poisonous_BubblegutAI2 extends AggressiveNpcAI2
{
	@Override
	protected void handleSpawned() {
		protectionFluid();
		super.handleSpawned();
	}

	private void protectionFluid() {
		GameEngineServices.skillEngine().getSkill(getOwner(), 16447, 1, getOwner()).useNoAnimationSkill(); //Spout Sticky Protection Fluid.
	}

	@Override
	protected void handleDied() {
		switch (getNpcId()) {
		    case 210318: //Poisonous Bubblegut.
			    spawn(203195, getOwner().getX(), getOwner().getY(), getOwner().getZ(), (byte) 0); //Kato.
			    GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
				    @Override
				    public void run() {
				        despawnNpc(203195); //Kato.
				    }
			    }, 60000);
			break;
		}
		super.handleDied();
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
