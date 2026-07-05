package com.aionemu.gameserver.model.templates.item.actions;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class AssemblyItemActionBatchTest {

	@Test
	void countsAllAvailableDustAssemblies() {
		assertEquals(3, AssemblyItemAction.getAssemblyCount(3000, 1000));
		assertEquals(3, AssemblyItemAction.getAssemblyCount(3500, 1000));
		assertEquals(0, AssemblyItemAction.getAssemblyCount(999, 1000));
	}

	@Test
	void treatsMissingPartsNumAsOnePartPerAssembly() {
		assertEquals(7, AssemblyItemAction.getAssemblyCount(7, 0));
		assertEquals(7, AssemblyItemAction.getAssemblyCount(7, -1));
	}

	@Test
	void calculatesRequiredPartsForBatch() {
		assertEquals(3000, AssemblyItemAction.getRequiredPartsCount(1000, 3));
		assertEquals(7, AssemblyItemAction.getRequiredPartsCount(0, 7));
	}
}
