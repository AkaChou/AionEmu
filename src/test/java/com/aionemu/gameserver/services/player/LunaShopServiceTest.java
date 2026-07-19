package com.aionemu.gameserver.services.player;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class LunaShopServiceTest {

	@Test
	void shopPricesMatchChinaClientAndNeverAllowOverdraft() {
		assertEquals(2, LunaShopService.MATERIAL_BOX_PRICE);
		assertEquals(12, LunaShopService.WARDROBE_APPEARANCE_PRICE);
		assertEquals(5, LunaShopService.TREASURE_CHEST_PRICE);
		assertEquals(10, LunaShopService.wardrobePrice(3));
		assertEquals(12, LunaShopService.wardrobePrice(8));
		assertEquals(1, LunaShopService.lunaConsumeRewardId(25));
		assertEquals(7, LunaShopService.lunaConsumeRewardId(1000));
		assertEquals(0, LunaShopService.lunaConsumeRewardId(30));
		assertEquals(-1, LunaShopService.wardrobePrice(9));
		assertEquals(3, LunaShopService.lunaDicePrice(0));
		assertEquals(3, LunaShopService.lunaDicePrice(10));
		assertEquals(-1, LunaShopService.lunaDicePrice(11));
		assertTrue(LunaShopService.canSpendLuna(2, 2));
		assertFalse(LunaShopService.canSpendLuna(1, 2));
		assertFalse(LunaShopService.canSpendLuna(10, -1));
		assertFalse(LunaShopService.canClaimDiceReward(0));
		assertTrue(LunaShopService.canClaimDiceReward(1));
	}
}
