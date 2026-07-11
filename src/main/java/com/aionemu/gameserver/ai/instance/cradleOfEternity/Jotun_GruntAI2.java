package com.aionemu.gameserver.ai.instance.cradleOfEternity;

import com.aionemu.gameserver.lifecycle.GameEngineServices;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import com.aionemu.gameserver.ai.AggressiveNpcAI2;
import com.aionemu.commons.network.util.ThreadPoolManager;
import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.skillengine.SkillEngine;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * Cradle Of Eternity 副本 NPC AI：Jotun Grunt（@AIName "Jotun_Grunt"），继承 AggressiveNpcAI2。
 * Cradle Of Eternity instance NPC AI: Jotun Grunt (@AIName "Jotun_Grunt"), extends AggressiveNpcAI2.
 *
 * @author Encom
 */
@AIName("Jotun_Grunt")
public class Jotun_GruntAI2 extends AggressiveNpcAI2
{
	@Override
	public void think() {
	}
	
	@Override
	protected void handleSpawned() {
		super.handleSpawned();
		GameEngineServices.skillEngine().getSkill(getOwner(), 23014, 60, getOwner()).useNoAnimationSkill(); //Sacrificial Rite.
		startLifeTask();
	}
	
	private void startLifeTask() {
		GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			@Override
			public void run() {
				// 尤顿战斗人员的支援被推迟。 / The support of the Jotun combatants was delayed.
				PacketSendUtility.npcSendPacketTime(getOwner(), SM_SYSTEM_MESSAGE.STR_IDEternity_02_Nepilim_Summon_MSG_02, 10000);
				AI2Actions.deleteOwner(Jotun_GruntAI2.this);
			}
		}, 10000);
	}
	
	@Override
	public boolean isMoveSupported() {
		return false;
	}
	
	@Override
	protected void handleDied() {
		super.handleDied();
		AI2Actions.deleteOwner(this);
	}
}
