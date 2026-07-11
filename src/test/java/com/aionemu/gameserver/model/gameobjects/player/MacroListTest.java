package com.aionemu.gameserver.model.gameobjects.player;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

class MacroListTest {

	@Test
	void preservesSlotsAndOmitsDeletedMacrosFromPacketPart() {
		MacroList macros = new MacroList(new LinkedHashMap<>(Map.of(1, "one", 3, "three", 7, "seven")));

		assertEquals(Map.of(1, "one", 3, "three"), macros.getMarcosPart(1));
		assertEquals(Map.of(7, "seven"), macros.getMarcosPart(2));

		macros.removeMacro(3);
		assertEquals(Map.of(1, "one"), macros.getMarcosPart(1));
	}
}
