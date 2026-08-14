package com.aionemu.gameserver.ai.event;

import com.aionemu.gameserver.ai.AggressiveNpcAI2;
import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.world.WorldPosition;

/**
 * 活动事件 NPC AI：War Goods（@AIName "war_goods"），继承 AggressiveNpcAI2。
 * Event NPC AI: War Goods (@AIName "war_goods"), extends AggressiveNpcAI2.
 *
 * @author Encom
 */
@AIName("war_goods")
public class War_GoodsAI2 extends AggressiveNpcAI2
{
	@Override
	protected void handleDied() {
		final WorldPosition p = getPosition();
		if (p != null) {
			spawn(832261, p.getX(), p.getY(), p.getZ(), (byte) 0); // 伊迪安深渊宝箱 / Idian Dephs Treasure Chest.
		}
		super.handleDied();
		AI2Actions.deleteOwner(this);
		AI2Actions.scheduleRespawn(this);
	}
}
