package com.aionemu.gameserver.ai.rvr.asmodianWarshipInvasion;

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
 * RvR 相关 NPC AI：Taina Comander（@AIName "taina_comander"），继承 AggressiveNpcAI2。
 * RvR-related NPC AI: Taina Comander (@AIName "taina_comander"), extends AggressiveNpcAI2.
 *
 * @author Encom
 */
@AIName("taina_comander")
public class Taina_ComanderAI2 extends AggressiveNpcAI2
{
	private AtomicBoolean startedEvent = new AtomicBoolean(false);
	
	@Override
	protected void handleCreatureMoved(Creature creature) {
		if (creature instanceof Player) {
			final Player player = (Player) creature;
			if (MathUtil.getDistance(getOwner(), player) <= 15) {
				if (startedEvent.compareAndSet(false, true)) {
					// 卑鄙的家伙！！你们的抵抗终将徒劳。 / Wretches!! Your resistance shall be futile.
				    sendMsg(1501534, getObjectId(), false, 3000);
					// 让我们向这些懦弱的天族展示魔族的力量！ / Let's show these cowardly Elyos the might of the Asmodians!
				    sendMsg(1501535, getObjectId(), false, 9000);
					// 不要放弃！主神阿兹菲尔的意志与我们同在。 / Don't give up! The will of Empyrean Lord Azphel is with us.
				    sendMsg(1501536, getObjectId(), false, 15000);
					// 主神阿兹菲尔！请赐予我力量。 / Empyrean Lord Azphel! Please give me strength.
				    sendMsg(1501540, getObjectId(), false, 21000);
				}
			}
		}
	}
	
	@Override
	protected void handleDied() {
		GameLocationBootstrapServices.rvrService().stopRvr(3);
		spawn(833766, getOwner().getX(), getOwner().getY(), getOwner().getZ(), (byte) 0); //Dimensional Vortex.
		super.handleDied();
	}
	
	private void sendMsg(int msg, int Obj, boolean isShout, int time) {
		GameFeatureServices.npcShoutsService().sendMsg(getPosition().getWorldMapInstance(), msg, Obj, isShout, 0, time);
	}
}
