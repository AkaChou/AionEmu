package com.aionemu.gameserver.services.drop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.configs.main.DropConfig;

class DropRegistrationServiceTest {

	@Test
	void reductionHonorsGlobalMapAndLevelOneChestExemptions() {
		boolean originalDisabled = DropConfig.DISABLE_DROP_REDUCTION;
		String originalMaps = DropConfig.DISABLE_DROP_REDUCTION_IN_ZONES;
		try {
			DropConfig.DISABLE_DROP_REDUCTION = false;
			DropConfig.DISABLE_DROP_REDUCTION_IN_ZONES = "0, 42,,";
			DropRegistrationService service = new DropRegistrationService();

			assertNull(service.getReductionDropRate(1, 20, 42, false));
			assertNull(service.getReductionDropRate(1, 20, 100, true));
			assertEquals(0f, service.getReductionDropRate(1, 20, 100, false));

			DropConfig.DISABLE_DROP_REDUCTION = true;
			assertNull(service.getReductionDropRate(1, 20, 100, false));
		} finally {
			DropConfig.DISABLE_DROP_REDUCTION = originalDisabled;
			DropConfig.DISABLE_DROP_REDUCTION_IN_ZONES = originalMaps;
		}
	}
}
