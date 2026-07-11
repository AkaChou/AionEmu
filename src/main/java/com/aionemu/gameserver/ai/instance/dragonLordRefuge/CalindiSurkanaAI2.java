package com.aionemu.gameserver.ai.instance.dragonLordRefuge;

import com.aionemu.gameserver.lifecycle.GameEngineServices;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.ai2.NpcAI2;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.skillengine.SkillEngine;

import java.util.concurrent.Future;

/**
 * Dragon Lord Refuge 副本 NPC AI：Calindi Surkana（@AIName "calindi_surkana"），继承 NpcAI2。
 * Dragon Lord Refuge instance NPC AI: Calindi Surkana (@AIName "calindi_surkana"), extends NpcAI2.
 *
 * @author Encom
 */
@AIName("calindi_surkana")
public class CalindiSurkanaAI2 extends NpcAI2
{
	private Future<?> skillTask;
	Npc calindi;
	
	@Override
	protected void handleSpawned() {
		super.handleSpawned();
		calindi = getPosition().getWorldMapInstance().getNpc(219359);
		reflect();
	}
	
	private void reflect() {
		skillTask = GameThreadPoolServices.threadPoolManager().scheduleAtFixedRate(new Runnable() {
			@Override
			public void run() {
			    GameEngineServices.skillEngine().applyEffectDirectly(20891, getOwner(), calindi, 0);
			}
		}, 3000, 10000);
	}
	
	@Override
	protected void handleDied() {
		super.handleDied();
		if (skillTask != null && !skillTask.isCancelled()) {
			skillTask.cancel(true);
		}
	}
}
