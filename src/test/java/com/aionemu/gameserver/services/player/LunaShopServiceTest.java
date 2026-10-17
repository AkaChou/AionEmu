package com.aionemu.gameserver.services.player;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class LunaShopServiceTest {

	@Test
	void lunaPricesMatchChinaClientAndNeverAllowOverdraft() {
		assertEquals(2, LunaShopService.MATERIAL_BOX_PRICE);
		assertEquals(12, LunaShopService.WARDROBE_APPEARANCE_PRICE);
		assertEquals(20, LunaShopService.LUNA_INSTANCE_PRICE);
		assertEquals(5, LunaShopService.TREASURE_CHEST_PRICE);
		assertEquals(10, LunaShopService.wardrobePrice(3));
		assertEquals(12, LunaShopService.wardrobePrice(8));
		assertEquals(-1, LunaShopService.wardrobePrice(9));
		// 真端无每日掷骰次数上限（User::StartRollDice 只查格子段位），露娜余额即闸门。
		// The retail server has no daily roll cap; the Luna balance is the only gate.
		assertEquals(3, LunaShopService.LUNA_DICE_PRICE);
		// 黄金骰子：真端 client_luna_cost.xml golddice price01=200；掷满 5 次进黄金阶段。
		// Gold dice: retail client_luna_cost.xml golddice price01=200; gold stage starts after 5 rolls.
		assertEquals(200, LunaShopService.LUNA_GOLD_DICE_PRICE);
		assertEquals(5, LunaShopService.DICE_GOLD_STAGE_TRIES);
		for (int i = 0; i < 1000; i++) {
			int grade = LunaShopService.rollGoldGrade();
			assertTrue(grade >= 1 && grade <= 5);
		}
		assertTrue(LunaShopService.canSpendLuna(2, 2));
		assertFalse(LunaShopService.canSpendLuna(1, 2));
		assertFalse(LunaShopService.canSpendLuna(10, -1));
		assertFalse(LunaShopService.canClaimDiceReward(0));
		assertTrue(LunaShopService.canClaimDiceReward(1));
	}
}
