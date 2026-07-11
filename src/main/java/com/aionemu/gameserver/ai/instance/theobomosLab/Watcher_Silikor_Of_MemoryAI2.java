package com.aionemu.gameserver.ai.instance.theobomosLab;

import com.aionemu.gameserver.lifecycle.GameEngineServices;

import com.aionemu.gameserver.ai.AggressiveNpcAI2;
import com.aionemu.commons.utils.Rnd;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.skillengine.SkillEngine;
import com.aionemu.gameserver.world.WorldMapInstance;
import com.aionemu.gameserver.world.WorldPosition;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Theobomos Lab 副本 NPC AI：Watcher Silikor Of Memory（@AIName "watcher_silikor_of_memory"），继承 AggressiveNpcAI2。
 * Theobomos Lab instance NPC AI: Watcher Silikor Of Memory (@AIName "watcher_silikor_of_memory"), extends AggressiveNpcAI2.
 *
 * @author Encom
 */
@AIName("watcher_silikor_of_memory")
public class Watcher_Silikor_Of_MemoryAI2 extends AggressiveNpcAI2
{
	private boolean canThink = true;
	private List<Integer> percents = new ArrayList<Integer>();
	
	@Override
	public boolean canThink() {
		return canThink;
	}
	
	@Override
	protected void handleAttack(Creature creature) {
		super.handleAttack(creature);
		checkPercentage(getLifeStats().getHpPercentage());
	}
	
	private void addPercent() {
		percents.clear();
		Collections.addAll(percents, new Integer[]{50, 25, 10});
	}
	
	private void checkPercentage(int hpPercentage) {
		for (Integer percent: percents) {
			if (hpPercentage <= percent) {
				switch (percent) {
					case 50:
						sp(281054);
						sp(281053);
					break;
					case 25:
						sp(281054);
						sp(281053);
					break;
					case 10:
						sp(281054);
						sp(281053);
					break;
				}
			}
		}
	}
	
	private void sp(int npcId) {
		float direction = Rnd.get(0, 199) / 100f;
		int distance = Rnd.get(0, 2);
		float x1 = (float) (Math.cos(Math.PI * direction) * distance);
		float y1 = (float) (Math.sin(Math.PI * direction) * distance);
		WorldPosition p = getPosition();
		spawn(npcId, p.getX() + x1, p.getY() + y1, p.getZ(), p.getHeading());
	}
	
	private void deleteNpcs(List<Npc> npcs) {
		for (Npc npc: npcs) {
			if (npc != null) {
				npc.getController().onDelete();
			}
		}
	}
	
	@Override
	protected void handleSpawned() {
		super.handleSpawned();
		switch (this.getNpcId()) {
			case 237248: //Watcher Silikor Of Memory.
				GameEngineServices.skillEngine().getSkill(getOwner(), 18481, 1, getOwner()).useSkill();
			break;
		}
	}
	
	@Override
	protected void handleBackHome() {
		addPercent();
		super.handleBackHome();
	}
	
	@Override
	protected void handleDespawned() {
		percents.clear();
		super.handleDespawned();
	}
	
	@Override
	protected void handleDied() {
		WorldMapInstance instance = getPosition().getWorldMapInstance();
		if (instance != null) {
			deleteNpcs(instance.getNpcs(281054));
			deleteNpcs(instance.getNpcs(281053));
		}
		percents.clear();
		super.handleDied();
	}
}
