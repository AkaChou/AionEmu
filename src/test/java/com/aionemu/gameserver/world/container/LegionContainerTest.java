package com.aionemu.gameserver.world.container;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.aionemu.gameserver.model.team.legion.Legion;
import com.aionemu.gameserver.world.exceptions.DuplicateAionObjectException;
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

	@Test
	void duplicateNameDoesNotLeaveLegionIndexedById() {
		LegionContainer legions = new LegionContainer();
		Legion existing = new Legion(1, "same");
		Legion duplicateName = new Legion(2, "SAME");
		legions.add(existing);

		assertThrows(DuplicateAionObjectException.class, () -> legions.add(duplicateName));

		assertSame(existing, legions.get(1));
		assertSame(existing, legions.get("same"));
		assertNull(legions.get(2));
	}

	@Test
	void duplicateIdDoesNotReplaceExistingLegion() {
		LegionContainer legions = new LegionContainer();
		Legion existing = new Legion(1, "existing");
		Legion duplicateId = new Legion(1, "other");
		legions.add(existing);

		assertThrows(DuplicateAionObjectException.class, () -> legions.add(duplicateId));

		assertSame(existing, legions.get(1));
		assertSame(existing, legions.get("existing"));
		assertNull(legions.get("other"));
	}
}
