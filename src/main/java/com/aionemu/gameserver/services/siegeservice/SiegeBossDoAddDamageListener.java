package com.aionemu.gameserver.services.siegeservice;

import com.aionemu.gameserver.controllers.attack.AggroList;
import com.aionemu.gameserver.model.gameobjects.Creature;
import lombok.AllArgsConstructor;

/**
 * 攻城 BOSS 伤害监听器，累计玩家/种族伤害。
 * Siege boss damage listener accumulating player/race damage.
 */
@AllArgsConstructor
public class SiegeBossDoAddDamageListener implements AggroList.DamageListener {

	private final Siege siege;

	/**
	 * 伤害累计回调。
	 * Callback when damage is added.
	 *
	 * @param creature 攻击者 / attacker
	 * @param hate 伤害值 / damage amount
	 */
	@Override
	public void onDamageAdded(Creature creature, int hate) {
		siege.addBossDamage(creature, hate);
	}
}
