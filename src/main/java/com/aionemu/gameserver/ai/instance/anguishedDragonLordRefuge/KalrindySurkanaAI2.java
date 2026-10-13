package com.aionemu.gameserver.ai.instance.anguishedDragonLordRefuge;



import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.ai2.NpcAI2;
import com.aionemu.gameserver.model.gameobjects.Npc;

import java.util.concurrent.Future;
import com.aionemu.gameserver.skillengine.SkillEngine;
import com.aionemu.gameserver.utils.ThreadPoolManager;

/**
 * Anguished Dragon Lord Refuge 副本 NPC AI：Kalrindy Surkana（@AIName "kalrindy_surkana"），继承 NpcAI2。
 * Anguished Dragon Lord Refuge instance NPC AI: Kalrindy Surkana (@AIName "kalrindy_surkana"), extends NpcAI2.
 * @author Encom
 */
@AIName("kalrindy_surkana")
public class KalrindySurkanaAI2 extends NpcAI2
{
	private Future<?> skillTask;
	Npc kalrindy;

	@Override
	protected void handleSpawned() {
		super.handleSpawned();
		kalrindy = getPosition().getWorldMapInstance().getNpc(236274);
		reflect();
	}

	private void reflect() {
		skillTask = ThreadPoolManager.getInstance().scheduleAtFixedRate(() -> SkillEngine.getInstance().applyEffectDirectly(20891, getOwner(), kalrindy, 0), 3000, 10000);
	}

	@Override
	protected void handleDied() {
		super.handleDied();
		if (skillTask != null && !skillTask.isCancelled()) {
			skillTask.cancel(true);
		}
	}
}
