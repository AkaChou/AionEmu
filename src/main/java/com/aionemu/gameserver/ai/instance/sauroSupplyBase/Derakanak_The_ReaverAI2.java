package com.aionemu.gameserver.ai.instance.sauroSupplyBase;

import com.aionemu.gameserver.lifecycle.GameEngineServices;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import com.aionemu.gameserver.ai.AggressiveNpcAI2;
import com.aionemu.commons.network.util.ThreadPoolManager;
import com.aionemu.commons.utils.Rnd;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.skillengine.SkillEngine;

/**
 * Sauro Supply Base 副本 NPC AI：Derakanak The Reaver（@AIName "derakanak_the_reaver"），继承 AggressiveNpcAI2。
 * Sauro Supply Base instance NPC AI: Derakanak The Reaver (@AIName "derakanak_the_reaver"), extends AggressiveNpcAI2.
 *
 * @author Encom
 */
@AIName("derakanak_the_reaver")
public class Derakanak_The_ReaverAI2 extends AggressiveNpcAI2
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
		} if (hpPercentage <= 40 && stage < 2) {
			stage1();
			stage = 2;
		}
	}
	
	private void stage1() {
		int delay = 45000;
		if (isAlreadyDead() || !isStart) {
			return;
		} else {
			GameEngineServices.skillEngine().getSkill(getOwner(), 17888, 60, getOwner()).useNoAnimationSkill(); // 恐惧施法 / Fear Casting.
			scheduleDelayStage1(delay);
		}
	}
	
	private void stage2() {
		int delay = 15000;
		if (isAlreadyDead() || !isStart) {
			return;
		} else { // 16918: 火焰喷射 与 16881: 魔法飞弹 / 16918: Flame Spurt & 16881: Magic Missile.
			GameEngineServices.skillEngine().getSkill(getOwner(), Rnd.get(2) == 0 ? 16918 : 16881, 60, getTarget()).useNoAnimationSkill();
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
