package com.aionemu.gameserver.ai.instance.draupnirCave;

import com.aionemu.gameserver.lifecycle.GameFeatureServices;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.ai2.NpcAI2;
import com.aionemu.gameserver.services.NpcShoutsService;

/**
 * Draupnir Cave 副本 NPC AI：Akhal Phantasm（@AIName "akhal_phantasm"），继承 NpcAI2。
 * Draupnir Cave instance NPC AI: Akhal Phantasm (@AIName "akhal_phantasm"), extends NpcAI2.
 *
 * @author Encom
 */
@AIName("akhal_phantasm")
public class Akhal_PhantasmAI2 extends NpcAI2
{
	@Override
	protected void handleSpawned() {
		super.handleSpawned();
		startEvent();
	}
	
	private void startEvent() {
		switch (getNpcId()) {
			case 237276: //Akhal's Phantasm.
				// 你让我们蒙羞，巴卡尔玛！竟败给一群软弱的守护者？你的荣誉尽毁。 / You shame us, Bakarma! Fallen to a flock of weak-fleshed Daevas? Your honor is in shambles.
				sendMsg(1403082, getObjectId(), false, 2000);
				// 贝里特拉神谕室的德劳普尼尔防御火焰已激活。靠近火焰区域会灼伤。 / The Draupnir defense flame at Beritra's Oracle Chamber has been activated. If you approach the flame area, prepare to burn.
				sendMsg(1403084, getObjectId(), false, 6000);
				// 见敌人尽灭，龙族退出防御姿态。 / Seeing all enemies slain, the Balaur exits defensive stance.
				sendMsg(1403085, getObjectId(), false, 10000);
				// 我们的命运计划遭破坏了吗？阻截入侵者，巴卡尔玛。吾主全靠你了！ / Hath our fated plans fallen prey to sabatoge? Impede the intruders, Bakarma. Our Lord depends upon it!
				sendMsg(1403067, getObjectId(), false, 14000);
				GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
					@Override
					public void run() {
						AI2Actions.deleteOwner(Akhal_PhantasmAI2.this);
				    }
			    }, 18000);
			break;
		}
	}
	
	private void sendMsg(int msg, int Obj, boolean isShout, int time) {
		GameFeatureServices.npcShoutsService().sendMsg(getPosition().getWorldMapInstance(), msg, Obj, isShout, 0, time);
	}
}
