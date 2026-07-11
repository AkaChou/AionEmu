package com.aionemu.gameserver.ai.instance.empyreanCrucible;

import com.aionemu.gameserver.lifecycle.GameEngineServices;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import com.aionemu.gameserver.ai.GeneralNpcAI2;
import com.aionemu.commons.network.util.ThreadPoolManager;
import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.skillengine.SkillEngine;

/**
 * Empyrean Crucible 副本 NPC AI：Spark Of Darkness（@AIName "spark_of_darkness"），继承 GeneralNpcAI2。
 * Empyrean Crucible instance NPC AI: Spark Of Darkness (@AIName "spark_of_darkness"), extends GeneralNpcAI2.
 *
 * @author Encom
 */
@AIName("spark_of_darkness")
public class SparkOfDarknessAI2 extends GeneralNpcAI2
{
	@Override
	public void think() {
	}
	
	@Override
	protected void handleSpawned() {
		super.handleSpawned();
		startEventTask();
		startLifeTask();
	}
	
	private void startEventTask() {
		GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			@Override
			public void run() {
				if (!isAlreadyDead()) {
					GameEngineServices.skillEngine().getSkill(getOwner(), 19554, 1, getOwner()).useNoAnimationSkill();
				}
			}
		}, 500);
	}
	
	private void startLifeTask() {
		GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			@Override
			public void run() {
				AI2Actions.deleteOwner(SparkOfDarknessAI2.this);
			}
		}, 6500);
	}
	
	@Override
	public boolean isMoveSupported() {
		return false;
	}
}
