package com.aionemu.gameserver.model.instance;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

class InstanceRuntimeStateTest {

	@Test
	void roundTripsTypedStateAndOnlySignalsRealChanges() {
		InstanceRuntimeState state = new InstanceRuntimeState();
		AtomicInteger changes = new AtomicInteger();
		state.onChange(changes::incrementAndGet);
		state.put("stage", 3);
		state.put("door.main", true);
		state.put("deadline", 123456789L);
		state.put("stage", 3);

		InstanceRuntimeState restored = InstanceRuntimeState.decode(state.encode());
		assertEquals(3, restored.getInt("stage", 0));
		assertEquals(true, restored.getBoolean("door.main", false));
		assertEquals(123456789L, restored.getLong("deadline", 0));
		assertEquals(3, changes.get());
	}

	@Test
	void snapshotsAndRemovesNamespacesAtomically() {
		InstanceRuntimeState state = new InstanceRuntimeState();
		AtomicInteger changes = new AtomicInteger();
		state.onChange(changes::incrementAndGet);
		state.put("area.one", true);
		state.put("area.two", false);
		state.put("score", 7);

		assertEquals(2, state.snapshot("area.").size());
		state.removePrefix("area.");
		assertEquals(7, state.getInt("score", 0));
		assertEquals(4, changes.get());
	}
}
