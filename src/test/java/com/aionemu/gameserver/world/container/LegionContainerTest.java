package com.aionemu.gameserver.world.container;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.aionemu.gameserver.model.team.legion.Legion;
import java.util.Iterator;
import org.junit.jupiter.api.Test;

class LegionContainerTest {

	@Test
	void iteratorReturnsSnapshotSafeForRemovalDuringIteration() {
		LegionContainer legions = new LegionContainer();
		legions.add(new Legion(1, "first"));
		legions.add(new Legion(2, "second"));
		legions.add(new Legion(3, "third"));
		Iterator<Legion> iterator = legions.iterator();

		assertDoesNotThrow(() -> {
			while (iterator.hasNext()) {
				legions.remove(iterator.next());
			}
		});
		assertTrue(legions.getAllLegions().isEmpty());
	}
}
