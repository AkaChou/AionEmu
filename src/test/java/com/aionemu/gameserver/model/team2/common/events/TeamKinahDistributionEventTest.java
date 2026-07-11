package com.aionemu.gameserver.model.team2.common.events;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class TeamKinahDistributionEventTest {

	@Test
	void paymentSucceedsBeforeDistributionAndIsNotRepeatedPerMember() throws Exception {
		String source = Files.readString(Path.of(
				"src/main/java/com/aionemu/gameserver/model/team2/common/events/TeamKinahDistributionEvent.java"));
		String handle = source.substring(source.indexOf("public void handleEvent()"), source.indexOf("public boolean apply"));
		String apply = source.substring(source.indexOf("public boolean apply"));

		assertTrue(handle.indexOf("tryDecreaseKinah") < handle.indexOf("applyOnMembers"));
		assertTrue(handle.contains("STR_NOT_ENOUGH_MONEY"));
		assertFalse(apply.contains("decreaseKinah"));
	}
}
