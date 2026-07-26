package com.aionemu.gameserver.ai;

import com.aionemu.gameserver.dataholders.RetailAiData.DirectPortalEndpoint;
import com.aionemu.gameserver.dataholders.RetailAiData.DirectPortalGroup;
import com.aionemu.gameserver.dataholders.RetailAiData.DirectPortalPoint;
import com.aionemu.gameserver.model.TeleportAnimation;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RetailDirectPortalEngineTest {

	@Test
	void selectsTheOnlyCompleteDestinationPoint() {
		DirectPortalPoint point = new DirectPortalPoint(1, 2, 3, 90);
		DirectPortalEndpoint endpoint = new DirectPortalEndpoint(210010000, 700138,
			List.of(new DirectPortalGroup(100, List.of(point))));

		assertEquals(point, RetailDirectPortalEngine.selectPoint(endpoint));
	}

	@Test
	void followsRetailPortalTypeSemantics() {
		assertEquals(0x0E, TeleportAnimation.DIRECT_PORTAL.getStartAnimationId());
		assertEquals(0x0D, TeleportAnimation.DIRECT_PORTAL.getEndAnimationId());
		assertEquals(0x0E, TeleportAnimation.INVASION_PORTAL.getStartAnimationId());
		assertEquals(0x12, TeleportAnimation.INVASION_PORTAL.getEndAnimationId());
		for (int type : new int[] { 0, 4, 5 }) {
			assertEquals(TeleportAnimation.DIRECT_PORTAL, RetailDirectPortalEngine.animationFor(type));
				assertTrue(RetailDirectPortalEngine.closesWhenExhausted(type, false));
			assertNull(RetailDirectPortalEngine.noticeFor(type));
		}
		for (int type : new int[] { 1, 2, 3, 6 }) {
			assertEquals(TeleportAnimation.INVASION_PORTAL, RetailDirectPortalEngine.animationFor(type));
		}
		assertSame(SM_SYSTEM_MESSAGE.STR_MSG_INVADE_DIRECT_PORTAL_OPEN_NOTICE,
			RetailDirectPortalEngine.noticeFor(1));
		assertSame(SM_SYSTEM_MESSAGE.STR_MSG_EVENT_DIRECT_PORTAL_OPEN_NOTICE,
			RetailDirectPortalEngine.noticeFor(2));
		assertSame(SM_SYSTEM_MESSAGE.STR_MSG_SVS_DIRECT_PORTAL_CLOSE_TIMER_5S,
			RetailDirectPortalEngine.noticeFor(3));
		assertSame(SM_SYSTEM_MESSAGE.STR_MSG_RVR_DIRECT_PORTAL_OPEN_NOTICE,
			RetailDirectPortalEngine.noticeFor(6));
			assertFalse(RetailDirectPortalEngine.closesWhenExhausted(6, false));
			assertFalse(RetailDirectPortalEngine.closesWhenExhausted(1, true));
			assertFalse(RetailDirectPortalEngine.closesWhenExhausted(3, true));
	}

	@Test
	void followsRetailHourlyScheduleSemantics() {
		assertEquals(0, RetailDirectPortalEngine.scheduleIndex(DayOfWeek.MONDAY, 0));
		assertEquals(167, RetailDirectPortalEngine.scheduleIndex(DayOfWeek.SUNDAY, 23));
		assertFalse(RetailDirectPortalEngine.shouldOpen(0, 0));
		assertTrue(RetailDirectPortalEngine.shouldOpen(1, 0));
		assertFalse(RetailDirectPortalEngine.shouldOpen(1, 1));
		assertTrue(RetailDirectPortalEngine.shouldOpen(100, 99));
	}
}
