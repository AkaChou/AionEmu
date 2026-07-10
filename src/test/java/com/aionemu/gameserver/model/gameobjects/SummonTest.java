package com.aionemu.gameserver.model.gameobjects;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.aionemu.gameserver.model.SkillElement;
import org.junit.jupiter.api.Test;

class SummonTest {

	@Test
	void mapsSpiritNamesToAlwaysResistedElements() {
		assertEquals(SkillElement.EARTH, Summon.getAlwaysResistElement("earth spirit"));
		assertEquals(SkillElement.FIRE, Summon.getAlwaysResistElement("fire spirit"));
		assertEquals(SkillElement.WATER, Summon.getAlwaysResistElement("water spirit"));
		assertEquals(SkillElement.WIND, Summon.getAlwaysResistElement("wind spirit"));
		assertEquals(SkillElement.NONE, Summon.getAlwaysResistElement("tempest spirit"));
	}
}
