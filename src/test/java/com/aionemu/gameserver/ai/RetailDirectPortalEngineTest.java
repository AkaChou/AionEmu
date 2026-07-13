package com.aionemu.gameserver.ai;

import com.aionemu.gameserver.dataholders.RetailAiData.DirectPortalEndpoint;
import com.aionemu.gameserver.dataholders.RetailAiData.DirectPortalGroup;
import com.aionemu.gameserver.dataholders.RetailAiData.DirectPortalPoint;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RetailDirectPortalEngineTest {

	@Test
	void selectsTheOnlyCompleteDestinationPoint() {
		DirectPortalPoint point = new DirectPortalPoint(1, 2, 3, 90);
		DirectPortalEndpoint endpoint = new DirectPortalEndpoint(210010000, 700138,
			List.of(new DirectPortalGroup(100, List.of(point))));

		assertEquals(point, RetailDirectPortalEngine.selectPoint(endpoint));
	}
}
