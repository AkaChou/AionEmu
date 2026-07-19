package com.aionemu.gameserver.model.autogroup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class RetailMatchSessionTest {

	@Test
	void preservesReadyEnterActiveAndCancellationState() {
		long now = 1_000_000;
		RetailMatchSession session = new RetailMatchSession(107, 42, now, now + 120_000, now + 420_000,
				List.of(new RetailMatchSession.Member(1, "A", (byte) 10, 75, (byte) 0, (byte) 0, 11, 101,
						now - 5_000, false, false, true)));

		assertTrue(session.pressEnter(1));
		assertTrue(session.markEntered(1));
		assertTrue(session.acceptsLateEntry(now + 300_000));
		assertTrue(session.add(new RetailMatchSession.Member(2, "B", (byte) 2, 74, (byte) 1, (byte) 1, 12, 102,
				now, false, false, true), now + 300_000));

		RetailMatchSession restored = RetailMatchSession.decode(session.encode());
		assertEquals(RetailMatchSession.State.ACTIVE, restored.state());
		assertEquals(2, restored.members().size());
		assertTrue(restored.member(1).entered());
		assertEquals(101, restored.member(1).instanceGroupEntryId());
		assertEquals(75, restored.member(1).level());
		assertFalse(restored.acceptsLateEntry(now + 420_000));
		assertTrue(restored.remove(2, "READY_TIMEOUT"));
		assertEquals("READY_TIMEOUT", restored.cancelReason());
	}

	@Test
	void resetsOnlyUnfinishedAdmissionsAfterRestoreAndTracksLeave() {
		RetailMatchSession session = new RetailMatchSession(40, 7, 100, 220, 400, List.of(
				new RetailMatchSession.Member(1, "entered", (byte) 1, 75, (byte) 0, (byte) 0, 0, 0, 100,
						true, true, true),
				new RetailMatchSession.Member(2, "pending", (byte) 5, 74, (byte) 1, (byte) 0, 0, 0, 100,
						true, false, true)));

		session.resetPendingEntries();

		assertTrue(session.member(1).pressedEnter());
		assertFalse(session.member(2).pressedEnter());
		assertTrue(session.leave(1));
		assertEquals(List.of(2), session.members().stream().map(RetailMatchSession.Member::playerId).toList());
	}
}
