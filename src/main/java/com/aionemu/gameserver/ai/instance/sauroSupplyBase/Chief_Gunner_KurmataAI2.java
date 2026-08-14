package com.aionemu.gameserver.ai.instance.sauroSupplyBase;

import com.aionemu.gameserver.lifecycle.GameEngineServices;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import com.aionemu.gameserver.ai.AggressiveNpcAI2;
import com.aionemu.commons.network.util.ThreadPoolManager;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.skillengine.SkillEngine;

/**
 * Sauro Supply Base 副本 NPC AI：Chief Gunner Kurmata（@AIName "chief_gunner_kurmata"），继承 AggressiveNpcAI2。
 * Sauro Supply Base instance NPC AI: Chief Gunner Kurmata (@AIName "chief_gunner_kurmata"), extends AggressiveNpcAI2.
 *
 * @author Encom
 */
@AIName("chief_gunner_kurmata")
public class Chief_Gunner_KurmataAI2 extends AggressiveNpcAI2
{
	private int stage = 0;
	private boolean isStart = false;
	
	@Override
	protected void handleCreatureAggro(Creature creature) {
		super.handleCreatureAggro(creature);
		wakeUp();
	}
	
	@Override
	protected void handleSpawned() {
		super.handleSpawned();
		beritraFavor();
	}
	
	private void beritraFavor() {
	    GameEngineServices.skillEngine().getSkill(getOwner(), 21194, 1, getOwner()).useNoAnimationSkill(); // 钢铁守护 / Iron Guardian.
		GameEngineServices.skillEngine().getSkill(getOwner(), 21135, 1, getOwner()).useNoAnimationSkill(); // 布里特拉之加护 / Beritra's Favor.
	}
	
	@Override
	protected void handleAttack(Creature creature) {
		super.handleAttack(creature);
		checkPercentage(getLifeStats().getHpPercentage());
		wakeUp();
	}
	
	private void wakeUp() {
		isStart = true;
	}
	
	private void checkPercentage(int hpPercentage) {
		if (hpPercentage <= 90 && stage < 1) {
			stage1();
			stage = 1;
		} if (hpPercentage <= 50 && stage < 2) {
			stage2();
			stage = 2;
		}
	}
	
	private void stage1() {
		int delay = 0;
		if (isAlreadyDead() || !isStart) {
			return;
		} else {
			GameEngineServices.skillEngine().getSkill(getOwner(), 20701, 60, getOwner()).useNoAnimationSkill(); // 鲜血祝福 / Blessing of Blood.
		}
	}
	
	private void stage2() {
		int delay = 20000;
		if (isAlreadyDead() || !isStart) {
			return;
		} else {
			GameEngineServices.skillEngine().getSkill(getOwner(), 20858, 60, getOwner()).useNoAnimationSkill(); // 雷霆冲击 / Thunder Crash Fallout.
			scheduleDelayStage2(delay);
		}
	}
	
	private void scheduleDelayStage2(int delay) {
		if (!isStart && !isAlreadyDead()) {
			return;
		} else {
			GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
				@Override
				public void run() {
					stage2();
				}
			}, delay);
		}
	}
	
	@Override
	protected void handleBackHome() {
        super.handleBackHome();
		isStart = false;
		stage = 0;
	}
	
	@Override
	protected void handleDied() {
		super.handleDied();
		isStart = false;
		stage = 0;
	}
}
