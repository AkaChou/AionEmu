package com.aionemu.gameserver.ai.instance.elementisForest;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import com.aionemu.gameserver.ai.SummonerAI2;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.templates.spawns.SpawnTemplate;
import com.aionemu.gameserver.spawnengine.SpawnEngine;
import com.aionemu.gameserver.world.WorldMapInstance;
import com.aionemu.gameserver.world.WorldPosition;
import java.util.concurrent.Future;

/**
 * Elementis Forest 副本 NPC AI：Jurdin The Cursed（@AIName "jurdin"），继承 SummonerAI2。
 * Elementis Forest instance NPC AI: Jurdin The Cursed (@AIName "jurdin"), extends SummonerAI2.
 * @author Luzien
 */
@AIName("jurdin")
public class JurdinTheCursedAI2 extends SummonerAI2 {

	private boolean isStart;
	private Future<?> task;

	@Override
	public void handleAttack(Creature creature) {
		super.handleAttack(creature);
		if (getLifeStats().getHpPercentage() <= 90 && !isStart) {
			isStart = true;
			startTask();
		}
	}

	@Override
	protected void handleDespawned() {
		cancelTask();
		super.handleDespawned();
	}

	@Override
	public void handleBackHome() {
		super.handleBackHome();
		isStart = false;
		cancelTask();
	}

	@Override
	public void handleDied() {
		super.handleDied();
		cancelTask();
	}

	private void startTask() {

		task = GameThreadPoolServices.threadPoolManager().scheduleAtFixedRate(() -> {
			if (isAlreadyDead()) {
				cancelTask();
			}
			else {
				spawnShadows();
			}
		},0, 60000);
	}

	private void spawnShadows() {
		spawnFlowers();
		GameThreadPoolServices.threadPoolManager().schedule(() -> {
			if (isAlreadyDead() || !isStart) {
				return;
			}
			WorldPosition p = getPosition();
			if (p != null) {
				WorldMapInstance instance = p.getWorldMapInstance();
				if (instance != null) {
					for (int i = 0; i < 7; i++) {
						SpawnTemplate temp = rndSpawnInRange(282201, 10);
						VisibleObject o = SpawnEngine.spawnObject(temp, getPosition().getInstanceId());
						addHelpersSpawn(o.getObjectId());
					}
				}
			}
		}, 5000);
	}

	private void cancelTask() {
		if (task != null && !task.isCancelled()) {
			task.cancel(true);
		}
	}

	private void spawnFlowers() {
		WorldPosition p = getPosition();
		if (p != null) {
			WorldMapInstance instance = p.getWorldMapInstance();
			if (instance != null) {
				spawn(282440, 460.795f, 801.471f, 130.759f, (byte) 0);
				spawn(282440, 485.118f, 790.683f, 129.668f, (byte) 0);
				spawn(282440, 474.227f, 777.928f, 128.875f, (byte) 0);
				spawn(282440, 475.009f, 783.226f, 128.875f, (byte) 0);
				spawn(282440, 459.194f, 797.047f, 130.491f, (byte) 0);
				spawn(282440, 484.584f, 791.999f, 129.750f, (byte) 0);
				spawn(282440, 485.502f, 803.050f, 130.472f, (byte) 0);
				spawn(282440, 486.006f, 804.645f, 130.540f, (byte) 0);
				spawn(282440, 412.354f, 800.439f, 131.176f, (byte) 0);
				spawn(282440, 474.676f, 816.744f, 131.281f, (byte) 0);
				spawn(282440, 412.354f, 800.439f, 131.176f, (byte) 0);
				spawn(282440, 467.355f, 811.444f, 131.261f, (byte) 0);
				spawn(282440, 463.336f, 798.282f, 130.309f, (byte) 0);
			}
		}
	}
}
