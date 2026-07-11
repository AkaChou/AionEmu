package com.aionemu.gameserver.ai.rvr;

import com.aionemu.gameserver.lifecycle.GameFeatureServices;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import com.aionemu.commons.network.util.ThreadPoolManager;

import com.aionemu.gameserver.ai.AggressiveNpcAI2;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.services.NpcShoutsService;

/**
 * RvR 相关 NPC AI：Guardian Scout（@AIName "guardian_scout"），继承 AggressiveNpcAI2。
 * RvR-related NPC AI: Guardian Scout (@AIName "guardian_scout"), extends AggressiveNpcAI2.
 *
 * @author Encom
 */
@AIName("guardian_scout")
public class Guardian_ScoutAI2 extends AggressiveNpcAI2
{
	@Override
	protected void handleSpawned() {
		super.handleSpawned();
		switch (getNpcId()) {
			case 240971: //Guardian Warrior.
			case 240972: //Guardian Mage.
			case 240973: //Guardian Scout.
			case 240974: //Guardian Marksman.
				startLifeTask();
			break;
        }
		// 天族必胜，你们的挣扎终是徒劳。 / Victory is a certainty for us Elyos, yet you continue your futile struggles.
		sendMsg(1501537, getObjectId(), false, 5000);
		// 你们不傻。难道还没意识到这不是能赢的战斗？ / You're no fools. Haven't you yet realized that this isn't a battle you can win ?
		sendMsg(1501538, getObjectId(), false, 8000);
		// 真有这么多不知死活的魔族吗……？ / Are there really this many Asmodians ignorant of their fate… ?
		// 那我就在此立刻教训你。 / Then I'll teach you here and now. That today is your last day alive!
		sendMsg(1501539, getObjectId(), false, 11000);
		// 艾瑞尔大人！请向那魔族展示您的力量！ / Lord Ariel! Please show your power to that Asmodian!
		sendMsg(1501541, getObjectId(), false, 14000);
	}
	
	private void startLifeTask() {
		GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			@Override
			public void run() {
				AI2Actions.deleteOwner(Guardian_ScoutAI2.this);
			}
		}, 300000); //5 分钟。 / 5 Minutes.
	}
	
	private void sendMsg(int msg, int Obj, boolean isShout, int time) {
		GameFeatureServices.npcShoutsService().sendMsg(getPosition().getWorldMapInstance(), msg, Obj, isShout, 0, time);
	}
}
