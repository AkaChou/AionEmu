package com.aionemu.gameserver.ai.rvr.elyosWarshipInvasion;

import com.aionemu.gameserver.lifecycle.GameLocationBootstrapServices;

import com.aionemu.gameserver.lifecycle.GameFeatureServices;

import com.aionemu.gameserver.ai.AggressiveNpcAI2;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.services.NpcShoutsService;
import com.aionemu.gameserver.services.RvrService;
import com.aionemu.gameserver.utils.MathUtil;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * RvR 相关 NPC AI：Nanabel Comander（@AIName "nanabel_comander"），继承 AggressiveNpcAI2。
 * RvR-related NPC AI: Nanabel Comander (@AIName "nanabel_comander"), extends AggressiveNpcAI2.
 *
 * @author Encom
 */
@AIName("nanabel_comander")
public class Nanabel_ComanderAI2 extends AggressiveNpcAI2
{
	private AtomicBoolean startedEvent = new AtomicBoolean(false);
	
	@Override
	protected void handleCreatureMoved(Creature creature) {
		if (creature instanceof Player) {
			final Player player = (Player) creature;
			if (MathUtil.getDistance(getOwner(), player) <= 15) {
				if (startedEvent.compareAndSet(false, true)) {
					// 天族必胜，你们的挣扎终是徒劳。 / Victory is a certainty for us Elyos, yet you continue your futile struggles.
				    sendMsg(1501537, getObjectId(), false, 3000);
					// 你们不傻。难道还没意识到这不是能赢的战斗？ / You're no fools. Haven't you yet realized that this isn't a battle you can win?
				    sendMsg(1501538, getObjectId(), false, 9000);
					// 真有这么多不知死活的魔族吗……？ / Are there really this many Asmodians ignorant of their fate…?
					// 那我就在此立刻教训你。 / Then I'll teach you here and now.
					// 今天就是你的末日！ / That today is your last day alive!
				    sendMsg(1501539, getObjectId(), false, 15000);
					// 艾瑞尔大人！请向那魔族展示您的力量！ / Lord Ariel! Please show your power to that Asmodian!
				    sendMsg(1501541, getObjectId(), false, 21000);
				}
			}
		}
	}
	
	@Override
	protected void handleDied() {
		GameLocationBootstrapServices.rvrService().stopRvr(4);
		spawn(833766, getOwner().getX(), getOwner().getY(), getOwner().getZ(), (byte) 0); // 次元漩涡 / Dimensional Vortex.
		super.handleDied();
	}
	
	private void sendMsg(int msg, int Obj, boolean isShout, int time) {
		GameFeatureServices.npcShoutsService().sendMsg(getPosition().getWorldMapInstance(), msg, Obj, isShout, 0, time);
	}
}
