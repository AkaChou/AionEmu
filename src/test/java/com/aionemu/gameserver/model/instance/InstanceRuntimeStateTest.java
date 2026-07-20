package com.aionemu.gameserver.model.instance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

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

	@Test
	void batchMutationPublishesOneCompleteSnapshot() {
		InstanceRuntimeState state = new InstanceRuntimeState();
		AtomicInteger changes = new AtomicInteger();
		AtomicReference<Map<String, String>> published = new AtomicReference<>();
		state.onChange(() -> {
			changes.incrementAndGet();
			published.set(state.snapshot());
		});

		state.mutate(values -> {
			values.put("deadline.test.at", "123");
			values.put("deadline.test.completed", "false");
		});

		assertEquals(1, changes.get());
		assertEquals(2, state.snapshot("deadline.test.").size());
		assertEquals(2, published.get().size());
		assertThrows(IllegalStateException.class, () -> state.mutate(values -> {
			values.put("partial", "value");
			throw new IllegalStateException("abort");
		}));
		assertEquals(null, state.get("partial"));
		assertEquals(1, changes.get());
	}
}
