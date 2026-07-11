package com.aionemu.gameserver.ai;

import com.aionemu.gameserver.ai2.AIName;

/**
 * 每次命中固定 1 点伤害的 NPC AI（通常用于练习/特殊目标）。
 * NPC AI that always deals 1 damage per hit (practice/special targets).
 *
 * @author Encom
 */
@AIName("onedmgperhit")
public class OneDmgPerHitAI2 extends NoActionAI2
{
	@Override
	public int modifyDamage(int damage) {
		return 1;
	}
	
	@Override
	public int modifyOwnerDamage(int damage) {
		return 1;
	}
}
