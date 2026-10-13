package com.aionemu.gameserver.ai.instance.dragonLordRefuge;



import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.ai2.NpcAI2;
import com.aionemu.gameserver.model.gameobjects.Npc;

import java.util.concurrent.Future;
import com.aionemu.gameserver.skillengine.SkillEngine;
import com.aionemu.gameserver.utils.ThreadPoolManager;

/**
 * Dragon Lord Refuge 副本 NPC AI：Calindi Surkana（@AIName "calindi_surkana"），继承 NpcAI2。
 * Dragon Lord Refuge instance NPC AI: Calindi Surkana (@AIName "calindi_surkana"), extends NpcAI2.
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
		skillTask = ThreadPoolManager.getInstance().scheduleAtFixedRate(() -> SkillEngine.getInstance().applyEffectDirectly(20891, getOwner(), calindi, 0), 3000, 10000);
	}

	@Override
	protected void handleDied() {
		super.handleDied();
		if (skillTask != null && !skillTask.isCancelled()) {
			skillTask.cancel(true);
		}
	}
}
