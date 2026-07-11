package com.aionemu.gameserver.ai.instance.dragonLordRefuge;

import com.aionemu.gameserver.lifecycle.GameFeatureServices;

import com.aionemu.gameserver.lifecycle.GameEngineServices;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import com.aionemu.gameserver.ai.AggressiveNpcAI2;
import com.aionemu.commons.network.util.ThreadPoolManager;
import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.services.NpcShoutsService;
import com.aionemu.gameserver.skillengine.SkillEngine;

/**
 * Dragon Lord Refuge 副本 NPC AI：Tiamat Woman Form（@AIName "tiamat_woman_form"），继承 AggressiveNpcAI2。
 * Dragon Lord Refuge instance NPC AI: Tiamat Woman Form (@AIName "tiamat_woman_form"), extends AggressiveNpcAI2.
 *
 * @author Encom
 */
@AIName("tiamat_woman_form")
public class Tiamat_Woman_FormAI2 extends AggressiveNpcAI2
{
	@Override
	public void think() {
	}
	
	@Override
	protected void handleSpawned() {
		super.handleSpawned();
		GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			@Override
			public void run() {
				startLifeTask();
				// 你……在威胁我？你们就是这样开玩笑的吗？ / Are you... threatening me? Is this what passes for a joke among you people?.
				sendMsg(1500613, getObjectId(), false, 3000);
				// 别担心。悲剧全是你们的。 / Don't worry. The tragedy will be all yours.
				sendMsg(1500614, getObjectId(), false, 9000);
				// 你将感受到前所未有的绝望！ / You will feel despair such as you have never felt!
				sendMsg(1500615, getObjectId(), false, 15000);
				// 在让卡林迪摧毁你之前，先让你瞥见族人的末日。 / Before I let Calindi destroy you, I will show you a glimpse of your people's ruin.
				sendMsg(1500616, getObjectId(), false, 21000);
				// 我……是的。我们很感激。没有你的帮助不知该怎么办…… / I... yes. We are grateful. I don't know what we'd have done without your help...
				sendMsg(1500617, getObjectId(), false, 27000);
				GameEngineServices.skillEngine().getSkill(getOwner(), 20917, 1, getOwner()).useNoAnimationSkill(); //Charge Siel's Relics.
			}
		}, 1000);
	}
	
	private void startLifeTask() {
		GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			@Override
			public void run() {
				spawn(219359, 470.5909f, 515.02856f, 417.40436f, (byte) 119); //Calindi.
				spawn(283174, 457.7215f, 514.4464f, 417.53998f, (byte) 0);
				AI2Actions.deleteOwner(Tiamat_Woman_FormAI2.this);
			}
		}, 35000);
	}
	
	private void sendMsg(int msg, int Obj, boolean isShout, int time) {
		GameFeatureServices.npcShoutsService().sendMsg(getPosition().getWorldMapInstance(), msg, Obj, isShout, 0, time);
	}
	
	@Override
	public boolean isMoveSupported() {
		return false;
	}
}
