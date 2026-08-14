package com.aionemu.gameserver.ai.instance.sauroSupplyBase;

import com.aionemu.gameserver.lifecycle.GameEngineServices;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import com.aionemu.gameserver.ai.AggressiveNpcAI2;
import com.aionemu.commons.network.util.ThreadPoolManager;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.skillengine.SkillEngine;

/**
 * Sauro Supply Base 副本 NPC AI：Commander Ranodim（@AIName "commander_ranodim"），继承 AggressiveNpcAI2。
 * Sauro Supply Base instance NPC AI: Commander Ranodim (@AIName "commander_ranodim"), extends AggressiveNpcAI2.
 *
 * @author Encom
 */
@AIName("commander_ranodim")
public class Commander_RanodimAI2 extends AggressiveNpcAI2
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
		if (hpPercentage <= 80 && stage < 1) {
			stage1();
			stage = 1;
		} if (hpPercentage <= 50 && stage < 2) {
			stage2();
			stage = 2;
		} if (hpPercentage <= 20 && stage < 3) {
			stage3();
			stage = 3;
		}
	}
	
	private void stage1() {
		int delay = 25000;
		if (isAlreadyDead() || !isStart) {
			return;
		} else {
			GameEngineServices.skillEngine().getSkill(getOwner(), 20702, 60, getOwner()).useNoAnimationSkill(); // 范围吸血 / Area Blood Sucking.
			scheduleDelayStage1(delay);
		}
	}
	
	private void stage2() {
		int delay = 25000;
		if (isAlreadyDead() || !isStart) {
			return;
		} else {
			GameEngineServices.skillEngine().getSkill(getOwner(), 20703, 60, getOwner()).useNoAnimationSkill(); // 吸血 / Blood Sucking.
			scheduleDelayStage2(delay);
		}
	}
	
	private void stage3() {
		int delay = 25000;
		if (isAlreadyDead() || !isStart) {
			return;
		} else {
			GameEngineServices.skillEngine().getSkill(getOwner(), 20704, 60, getOwner()).useNoAnimationSkill(); // 范围压制 / Area Press.
			scheduleDelayStage3(delay);
		}
	}
	
	private void scheduleDelayStage1(int delay) {
		if (!isStart && !isAlreadyDead()) {
			return;
		} else {
			GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
				@Override
				public void run() {
					stage1();
				}
			}, delay);
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
	
	private void scheduleDelayStage3(int delay) {
		if (!isStart && !isAlreadyDead()) {
			return;
		} else {
			GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
				@Override
				public void run() {
					stage3();
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
