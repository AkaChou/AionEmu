package com.aionemu.gameserver.model.stats.container;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * 主人物品加成的主人侧属性映射回归闸门。
 * Regression gate for the master-side stat mapping of master-item inheritance.
 */
class MasterItemBonusApplierTest {

	@Test
	void handSpecificStatsReadMainHandBonuses() {
		assertEquals(StatEnum.MAIN_HAND_ACCURACY, MasterItemBonusApplier.masterStat(StatEnum.PHYSICAL_ACCURACY));
		assertEquals(StatEnum.MAIN_HAND_CRITICAL, MasterItemBonusApplier.masterStat(StatEnum.PHYSICAL_CRITICAL));
		assertEquals(StatEnum.MAIN_HAND_POWER, MasterItemBonusApplier.masterStat(StatEnum.PHYSICAL_ATTACK));
	}

	@Test
	void otherStatsKeepTheirIdentity() {
		assertEquals(StatEnum.MAXHP, MasterItemBonusApplier.masterStat(StatEnum.MAXHP));
		assertEquals(StatEnum.BOOST_MAGICAL_SKILL, MasterItemBonusApplier.masterStat(StatEnum.BOOST_MAGICAL_SKILL));
		assertEquals(StatEnum.FIRE_RESISTANCE, MasterItemBonusApplier.masterStat(StatEnum.FIRE_RESISTANCE));
	}
}
