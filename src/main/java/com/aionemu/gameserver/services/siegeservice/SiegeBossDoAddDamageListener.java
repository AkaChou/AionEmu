package com.aionemu.gameserver.services.siegeservice;

import com.aionemu.gameserver.controllers.attack.AggroList.AddDamageValueCallback;
import com.aionemu.gameserver.model.gameobjects.Creature;

/**
 * 攻城 BOSS 伤害监听器，累计玩家/种族伤害。
 * Siege boss damage listener accumulating player/race damage.
 */
public class SiegeBossDoAddDamageListener extends AddDamageValueCallback {

	private final Siege siege;

	public SiegeBossDoAddDamageListener(Siege siege) {
		this.siege = siege;
	}

	@Override
	/**
	 * 伤害累计回调。
	 * Callback when damage is added.
	 *
	 * @param creature 攻击者 / attacker
	 * @param hate 伤害值 / damage amount
	 */
	public void onDamageAdded(Creature creature, int hate) {
		siege.addBossDamage(creature, hate);
	}
}