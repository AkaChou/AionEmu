package com.aionemu.gameserver.ai.instance.trialsOfEternity;

import com.aionemu.gameserver.lifecycle.GameEngineServices;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import com.aionemu.commons.network.util.ThreadPoolManager;
import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.ai2.NpcAI2;
import com.aionemu.gameserver.skillengine.SkillEngine;

/**
 * Trials Of Eternity 副本 NPC AI：Obelisk Of Ganesh（@AIName "IDEternity_03_Def_Boss_Energy"），继承 NpcAI2。
 * Trials Of Eternity instance NPC AI: Obelisk Of Ganesh (@AIName "IDEternity_03_Def_Boss_Energy"), extends NpcAI2.
 *
 * @author Encom
 */
@AIName("IDEternity_03_Def_Boss_Energy")
public class Obelisk_Of_GaneshAI2 extends NpcAI2
{
	@Override
	public void think() {
	}
	
	@Override
	protected void handleSpawned() {
		super.handleSpawned();
		GameEngineServices.skillEngine().getSkill(getOwner(), 17746, 60, getOwner()).useNoAnimationSkill();
	}
	
	/**
	 * 方尖碑被摧毁后，于 120 秒后在原位置重生，随后移除自身。
	 * When the obelisk is destroyed, it respawns at its original position after 120 seconds, then removes itself.
	 */
	@Override
	protected void handleDied() {
		switch (getNpcId()) {
			case 246421: //Obelisk Of Ganesh.
			    GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
					@Override
					public void run() {
						spawn(246421, 408.66196f, 1013.1304f, 711.93115f, (byte) 0, 177); //Obelisk Of Ganesh.
					}
				}, 120000);
			break;
			case 246422: //Obelisk Of Ganesh.
			    GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
					@Override
					public void run() {
						spawn(246422, 408.77426f, 1037.3873f, 711.90881f, (byte) 0, 179); //Obelisk Of Ganesh.
					}
				}, 120000);
			break;
			case 246423: //Obelisk Of Ganesh.
			    GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
					@Override
					public void run() {
						spawn(246423, 386.68903f, 1037.3842f, 711.95770f, (byte) 0, 181); //Obelisk Of Ganesh.
					}
				}, 120000);
			break;
			case 246424: //Obelisk Of Ganesh.
				GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
					@Override
					public void run() {
						spawn(246424, 386.68146f, 1013.2744f, 711.93091f, (byte) 0, 183); //Obelisk Of Ganesh.
					}
				}, 120000);
			break;
		}
		super.handleDied();
		AI2Actions.deleteOwner(this);
	}
	
	@Override
	public boolean isMoveSupported() {
		return false;
	}
}
